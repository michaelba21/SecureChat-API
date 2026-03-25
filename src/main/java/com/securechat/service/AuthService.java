package com.securechat.service;

import com.securechat.dto.AuthRequest;
import com.securechat.dto.AuthResponse;
import com.securechat.dto.LoginRequest;
import com.securechat.entity.RefreshToken;
import com.securechat.entity.User;
import com.securechat.exception.InvalidCredentialsException;
import com.securechat.exception.UserNotFoundException;
import com.securechat.exception.ValidationException;
import com.securechat.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service // Marks this class as a Spring service component (business logic layer)
public class AuthService {

  // Logger for tracking authentication events and errors
  private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

  @Autowired // Injects the UserRepository for database operations
  protected UserRepository userRepository;

  @Autowired // Injects the RefreshTokenService for refresh token management
  protected RefreshTokenService refreshTokenService;

  @Value("${jwt.secret:default-temp-secret-keycloak-migration}") // Injects JWT secret key from application.properties with default value
  protected String jwtSecret;

  @Value("${jwt.expiration:86400000}") // Injects JWT expiration time from application.properties with default value (24 hours)
  protected long jwtExpiration;

  // Password encoder with strength 12 (high security, slower computation)
  protected final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

  /**
   * Register a new user with email, username, and password
   * @param request 
   * @param ipAddress Client IP address for audit logging
   * @param userAgent 
   * @return AuthResponse containing JWT tokens and user information
   */
  public AuthResponse register(AuthRequest request, String ipAddress, String userAgent) {
    logger.info("Attempting to register user with username: {}", request.getUsername());

    validateRegistrationRequest(request); // Validate input data

    User user = createAndSaveUser(request); // Create and persist user entity
    logger.info("User registered successfully with ID: {}", user.getId());

    logger.info("Generating JWT for user: {}", user.getUsername());
    String accessToken = generateJWT(user); // Generate JWT access token
    RefreshToken refreshToken = refreshTokenService.createToken(user, ipAddress, userAgent); // Create refresh token

    return buildAuthResponse(accessToken, refreshToken.getToken(), user, "Registration successful");
  }

  // Validates registration request data
  private void validateRegistrationRequest(AuthRequest request) {
    validateEmail(request.getEmail(), request.getUsername());
    validateUsernameUniqueness(request.getUsername());
    validateEmailUniqueness(request.getEmail());
  }

  // Validates email format
  private void validateEmail(String email, String username) {
    if (email == null || email.isBlank()) {
      logger.warn("Registration failed: Email is required for username: {}", username);
      throw new ValidationException("Email is required");
    }
    if (!isValidEmail(email)) {
      logger.warn("Registration failed: Invalid email format for email: {}", email);
      throw new ValidationException("Invalid email format");
    }
  }

  // Checks if username is already taken
  private void validateUsernameUniqueness(String username) {
    if (userRepository.findByUsername(username).isPresent()) {
      logger.warn("Registration failed: Username already in use: {}", username);
      throw new com.securechat.exception.DuplicateResourceException("Username already in use");
    }
  }

  // Checks if email is already registered
  private void validateEmailUniqueness(String email) {
    if (userRepository.findByEmail(email).isPresent()) {
      logger.warn("Registration failed: Email already in use: {}", email);
      throw new com.securechat.exception.DuplicateResourceException("Email already in use");
    }
  }

  // Creates a new User entity and saves it to the database
  private User createAndSaveUser(AuthRequest request) {
    User user = new User();
    user.setEmail(request.getEmail());
    user.setUsername(request.getUsername());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // Hash password before storing
    user.setIsActive(true); // New users are active by default

    // Assign default role (ROLE_USER)
    Set<User.UserRole> roles = new HashSet<>();
    roles.add(User.UserRole.ROLE_USER);
    user.setRoles(roles);

    return userRepository.save(user); // Persist to database
  }

  // Basic email format validation using regex
  private boolean isValidEmail(String email) {
    return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  }

  /**
   * Login with LoginRequest DTO (typically from login form)
   * @param request 
   * @param ipAddress Client IP address for audit logging
   * @param userAgent 
   * @return AuthResponse containing JWT tokens and user information
   */
  public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
    // Convert LoginRequest to AuthRequest for unified processing
    AuthRequest proxy = new AuthRequest();
    proxy.setEmail(request.getEmail());
    proxy.setPassword(request.getPassword());
    return login(proxy, ipAddress, userAgent);
  }

  /**
   * Login with AuthRequest DTO
   * @param request 
   * @param ipAddress Client IP address for audit logging
   * @param userAgent 
   * @return AuthResponse containing JWT tokens and user information
   */
  public AuthResponse login(AuthRequest request, String ipAddress, String userAgent) {
    logger.info("Attempting login for email: {}", request.getEmail());

    // Validate email presence
    if (request.getEmail() == null || request.getEmail().isBlank()) {
      logger.warn("Login failed: Email must be provided");
      throw new ValidationException("Email must be provided");
    }

    // Find user by email
    User user = userRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> {
          logger.warn("Login failed: No user found with email: {}", request.getEmail());
          return new UserNotFoundException("No user found with the provided email");
        });

    // Verify password matches stored hash
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      logger.warn("Login failed: Invalid password for email: {}", request.getEmail());
      throw new InvalidCredentialsException("Invalid password");
    }

    // Check if user account is active
    if (user.getIsActive() == null || !user.getIsActive()) {
      logger.warn("Login failed: User is inactive: {}", request.getEmail());
      throw new InvalidCredentialsException("User account is inactive");
    }

    // Update last login timestamp
    user.setLastLogin(LocalDateTime.now());
    userRepository.save(user);
    logger.info("User logged in successfully: {}", user.getId());

    // Generate tokens
    logger.info("Generating JWT for user: {}", user.getUsername());
    String accessToken = generateJWT(user);
    RefreshToken refreshToken = refreshTokenService.createToken(user, ipAddress, userAgent);
    return buildAuthResponse(accessToken, refreshToken.getToken(), user, "Login successful");
  }

  /**
   * Generates a JWT (JSON Web Token) for user authentication
   * @param user 
   * @return Signed JWT string
   */
  private String generateJWT(User user) {
    return Jwts.builder()
        .setSubject(user.getId().toString()) 
        .claim("email", user.getEmail()) // Additional claim: email
        .claim("roles", user.getRoles().stream().map(Enum::name).toList()) // User roles as list
        .setAudience("securechat-api") 
        .setIssuedAt(new Date()) // Token creation time
        .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Token expiration
        .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes())) // Sign with secret key
        .compact(); // Build into string
  }

  // Builds authentication response with tokens and user info
  private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user, String message) {
    return new AuthResponse(
        accessToken,
        refreshToken,
        message,
        user.getUsername(),
        user.getEmail(),
        user.getId().toString());
  }

  /**
   * Refresh authentication tokens using a valid refresh token
   * @param refreshToken 
   * @param ipAddress Client IP address for audit logging
   * @param userAgent Client browser/device information for audit logging
   * @return 
   */
  public AuthResponse refresh(String refreshToken, String ipAddress, String userAgent) {
    // Validate refresh token and get associated user
    RefreshToken token = refreshTokenService.validateAndRefresh(refreshToken)
        .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired refresh token"));

    User user = token.getUser();
    // Verify user exists and is active
    if (user == null || (user.getIsActive() != null && !user.getIsActive())) {
      throw new InvalidCredentialsException("User is inactive or missing");
    }

    logger.info("Generating JWT for user: {}", user.getUsername());
    String newAccessToken = generateJWT(user);
    // Optional: rotate refresh token - for now we keep same and update lastUsedAt
    return buildAuthResponse(newAccessToken, token.getToken(), user, "Token refreshed");
  }
}