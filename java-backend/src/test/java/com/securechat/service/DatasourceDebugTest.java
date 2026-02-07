
package com.securechat.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

@SpringBootTest  // Full Spring Boot context initialization with all beans
@ActiveProfiles("test")  
class DatasourceDebugTest {  // Debug utility test, not a conventional unit test

    @Autowired
    private DataSource dataSource;  // Injected DataSource bean to inspect database configuration

    @Test
    void printUrl() throws Exception {
        // Debug method: prints database connection URL to console
       
        
        // Get database URL from connection metadata
        // This helps verify which database is being used (H2, PostgreSQL, etc.)
        System.out.println(dataSource.getConnection().getMetaData().getURL());
        
        
    }
}