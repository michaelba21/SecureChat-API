package com.securechat.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;  
import org.mockito.InjectMocks;  // Mockito annotation for injecting mocks
import org.mockito.junit.jupiter.MockitoExtension;  // Mockito extension for JUnit 5
import org.springframework.http.MediaType; 
import org.springframework.web.bind.annotation.GetMapping;  

import static org.junit.jupiter.api.Assertions.*;  // JUnit 5 assertions

@ExtendWith(MockitoExtension.class)  
class HomeControllerTest {

    @InjectMocks  // Creates instance of HomeController and injects any mocks (none here)
    private HomeController homeController;

    @Test
    void home_ShouldReturnWelcomeMessage() {
        // Act: Call the home() method
        String result = homeController.home();

        // Assert: Verify the returned message
        assertEquals("SecureChat API is running", result);  // Expected welcome message
    }

    @Test
    void home_ShouldReturnPlainTextContent() throws Exception {
        // This test verifies the annotation configuration on the controller method
        HomeController controller = new HomeController();  // Create controller instance
        
        // Get the @GetMapping annotation from the home() method using reflection
        GetMapping mapping = controller.getClass()  
            .getMethod("home")  
            .getAnnotation(GetMapping.class);  
        
        // Verify the annotation's produces attribute is set to text/plain
        assertArrayEquals(new String[]{MediaType.TEXT_PLAIN_VALUE}, mapping.produces());

    }
} 