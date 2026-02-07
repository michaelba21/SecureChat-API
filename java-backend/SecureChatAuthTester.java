import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SecureChatAuthTester {
    
    // UPDATED URLs based on info.txt
    private static final String KEYCLOAK_URL = "http://localhost:9090";
    private static final String REALM = "SecureChat";
    private static final String CLIENT_ID = "securechat-backend";
    private static final String CLIENT_SECRET = "b90M2LWNz5H0rUx9JTmre1JXdrxm98b5";
    private static final String API_BASE_URL = "http://localhost:8081/api";
    
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    
    public static void main(String[] args) {
        SecureChatAuthTester tester = new SecureChatAuthTester();
        tester.runTests();
    }
    
    public void runTests() {
        System.out.println(" SecureChat OAuth 2.0 Authentication Tester");
        System.out.println("=============================================");
        
        try {
            // Test 1: Get tokens for admin user
            System.out.println("\n1. Testing Admin User Authentication...");
            Map<String, String> adminTokens = getTokens("admin@securechat.com", "admin123");
            System.out.println(" Admin Tokens Received");
            System.out.println("   Access Token: " + adminTokens.get("access_token").substring(0, 50) + "...");
            System.out.println("   Refresh Token: " + adminTokens.get("refresh_token").substring(0, 50) + "...");
            
            // Test 2: Get tokens for regular user
            System.out.println("\n2. Testing Regular User Authentication...");
            Map<String, String> userTokens = getTokens("user@securechat.com", "user123");
            
            // Test 3: Test token introspection
            System.out.println("\n3. Testing Token Introspection...");
            introspectToken(adminTokens.get("access_token"));
            
            // Test 4: Test refresh token flow
            System.out.println("\n4. Testing Refresh Token Flow...");
            Map<String, String> refreshedTokens = refreshToken(adminTokens.get("refresh_token"));
            
            // Test 5: Test role-based API access
            System.out.println("\n5. Testing Role-Based API Access...");
            testAdminEndpoint(adminTokens.get("access_token"));
            testUserEndpoint(userTokens.get("access_token"));
            
            System.out.println("\n All tests completed successfully!");
            
        } catch (Exception e) {
            System.err.println(" Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Map<String, String> getTokens(String username, String password) throws IOException {
        String url = KEYCLOAK_URL + "/realms/" + REALM + "/protocol/openid-connect/token";
        
        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "password")
                .add("username", username)
                .add("password", password)
                .add("client_id", CLIENT_ID)
                .add("scope", "openid profile email roles")
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Basic " + encodedCredentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(body)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get tokens: " + response.body().string());
            }
            
            String responseBody = response.body().string();
            return mapper.readValue(responseBody, Map.class);
        }
    }
    
    private void introspectToken(String token) throws IOException {
        String url = KEYCLOAK_URL + "/realms/" + REALM + "/protocol/openid-connect/token/introspect";
        
        RequestBody body = new FormBody.Builder()
                .add("token", token)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            Map<String, Object> introspect = mapper.readValue(responseBody, Map.class);
            
            System.out.println("   Token Active: " + introspect.get("active"));
            System.out.println("   Username: " + introspect.get("username"));
            System.out.println("   Client ID: " + introspect.get("client_id"));
            System.out.println("   Token Type: " + introspect.get("token_type"));
            System.out.println("   Expiration: " + introspect.get("exp"));
        }
    }
    
    private Map<String, String> refreshToken(String refreshToken) throws IOException {
        String url = KEYCLOAK_URL + "/realms/" + REALM + "/protocol/openid-connect/token";
        
        RequestBody body = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build();
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            return mapper.readValue(responseBody, Map.class);
        }
    }
    
    private void testAdminEndpoint(String accessToken) throws IOException {
        String url = API_BASE_URL + "/admin/users";
        
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            System.out.println("   Admin endpoint status: " + response.code());
            if (response.code() == 200) {
                System.out.println("   Admin endpoint accessible");
            } else if (response.code() == 403) {
                System.out.println("   Admin endpoint: Access forbidden (missing admin role)");
            } else if (response.code() == 401) {
                System.out.println("   Admin endpoint: Unauthorized (invalid token)");
            }
        }
    }
    
    private void testUserEndpoint(String accessToken) throws IOException {
        String url = API_BASE_URL + "/user/profile";
        
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            System.out.println("   User endpoint status: " + response.code());
        }
    }
}
