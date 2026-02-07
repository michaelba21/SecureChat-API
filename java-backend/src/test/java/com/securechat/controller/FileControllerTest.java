

package com.securechat.controller;

import com.securechat.dto.FileUploadResponse;
import com.securechat.entity.File;
import com.securechat.repository.FileRepository;
import com.securechat.repository.UserRepository;
import com.securechat.service.FileService;
import com.securechat.service.LocalFileStorageService;
import com.securechat.util.AuthUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class) //  Testing only FileController layer with MVC support
@AutoConfigureMockMvc(addFilters = false) //  Disabling security filters for isolated controller testing
class FileControllerTest {

        @Autowired
        private MockMvc mockMvc; //  Main test utility for MVC endpoint testing

        @Autowired
        private ObjectMapper objectMapper; //  JSON serialization/deserialization helper

        @MockBean
        private FileService fileService; //  Mocking business logic layer

        @MockBean
        private FileRepository fileRepository; //  Mocking persistence layer (though unused in tests)

        @MockBean
        private UserRepository userRepository; //  Mocking user repository (currently unused in tests)

        @MockBean
        private LocalFileStorageService localFileStorageService; //  Mocking file storage service

        @MockBean
        private AuthUtil authUtil; //  Mocking authentication utility for user extraction

        private final UUID testUserId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"); //  Fixed test user ID for consistency
        private Authentication mockAuth;

        @BeforeEach
        void setUp() {
                //  Configuring AuthUtil mock to return test user ID for all authentication inputs
                when(authUtil.getCurrentUserId(any())).thenReturn(testUserId);

                //  Creating mock authentication with authenticated state and test user identity
                mockAuth = mock(Authentication.class);
                when(mockAuth.isAuthenticated()).thenReturn(true);
                when(mockAuth.getName()).thenReturn(testUserId.toString());
        }

        // Sample FileUploadResponse
        //  Factory method for creating consistent test response objects
        private FileUploadResponse createSampleUploadResponse() {
                return FileUploadResponse.builder()
                                .id(UUID.randomUUID())
                                .filename("document.pdf")
                                .downloadUrl("/api/files/download/" + UUID.randomUUID())
                                .size(2048L)
                                .uploadedAt(LocalDateTime.now())
                                .build();
        }

        // === Upload File Tests ===
        //  Test suite for file upload endpoint functionality

        @Test
        void uploadFile_successfulUpload_returnsCreated() throws Exception {
                //  Creating mock multipart file with PDF content for testing
                MockMultipartFile mockFile = new MockMultipartFile(
                                "file",
                                "document.pdf",
                                "application/pdf",
                                "PDF content".getBytes());

                FileUploadResponse response = createSampleUploadResponse();

                //  Mocking service layer to return success response
                when(fileService.uploadFile(eq(mockFile), eq(testUserId.toString())))
                                .thenReturn(response);

                mockMvc.perform(multipart("/api/files/upload")
                                .file(mockFile)
                                .principal(mockAuth))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.filename").value("document.pdf"))
                                .andExpect(jsonPath("$.size").value(2048));

                //  Verifying service interaction with exact parameter matching
                verify(fileService).uploadFile(mockFile, testUserId.toString());
        }

        @Test
        void uploadFile_noFileProvided_returnsBadRequest() throws Exception {
                //  Testing validation - empty multipart request should fail
                mockMvc.perform(multipart("/api/files/upload")
                                .principal(mockAuth))
                                .andExpect(status().isBadRequest());

                //  Ensuring service is not called when validation fails
                verifyNoInteractions(fileService);
        }

        @Test
        void uploadFile_serviceThrowsException_returnsInternalServerError() throws Exception {
                //  Testing error handling when service layer throws runtime exception
                MockMultipartFile mockFile = new MockMultipartFile(
                                "file", "bad.pdf", "application/pdf", "bad".getBytes());

                when(fileService.uploadFile(any(), anyString()))
                                .thenThrow(new RuntimeException("Storage failure"));

                mockMvc.perform(multipart("/api/files/upload")
                                .file(mockFile)
                                .principal(mockAuth))
                                .andExpect(status().isInternalServerError());

                //  Verifying service was called before exception propagation
                verify(fileService).uploadFile(mockFile, testUserId.toString());
        }

        // === Poll New Files Tests ===
        //  Test suite for polling new files in chat rooms

        @Test
        @WithMockUser(username = "550e8400-e29b-41d4-a716-446655440000") //  Annotation provides security context for test
        void pollNewFiles_validRequest_returnsFileList() throws Exception {
                //  Note: Parameters declared but unused - could indicate incomplete test setup
                UUID chatRoomId = UUID.randomUUID();
                Long lastMessageId = 100L;
                UUID userId = UUID.randomUUID();

                //  Building complete File entity with all required fields for realistic test
                File file = new File();
                file.setId(UUID.randomUUID());
                file.setFilename("test.txt");
                file.setFilePath("/path/to/test.txt");
                file.setFileSize(1024L);
                file.setMimeType("text/plain");
                file.setUploadedAt(LocalDateTime.now());
                file.setIsPublic(false);

                List<File> files = List.of(file);
                //  Mocking service to return file list based on room ID and timestamp
                when(fileService.getFilesSince(eq("room-123"), eq("2025-01-01T10:00:00")))
                                .thenReturn(files);

                mockMvc.perform(get("/api/files/poll")
                                .param("chatRoomId", "room-123")
                                .param("since", "2025-01-01T10:00:00")
                                .principal(mockAuth))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray()); //  Basic array verification - could be enhanced

                verify(fileService).getFilesSince("room-123", "2025-01-01T10:00:00");
        }

        @Test
        void pollNewFiles_invalidTimestampFormat_returnsBadRequest() throws Exception {
                //  Testing input validation - malformed timestamp should be rejected
                mockMvc.perform(get("/api/files/poll")
                                .param("chatRoomId", "room-123")
                                .param("since", "invalid-date")
                                .principal(mockAuth))
                                .andExpect(status().isBadRequest());

                //  Service should not be called with invalid input
                verifyNoInteractions(fileService);
        }

        // === Download File Tests ===
        //  Test suite for file download endpoint functionality

        @Test
        void downloadFile_successfulDownload_returnsFileResource() throws Exception {
                //  Creating ByteArrayResource with filename via anonymous class override
                ByteArrayResource resource = new ByteArrayResource("Sample file".getBytes()) {
                        @Override
                        public String getFilename() {
                                return "document.pdf";
                        }
                };

                when(fileService.downloadFile(any(UUID.class))).thenReturn(resource);

                mockMvc.perform(get("/api/files/download/{fileId}", UUID.randomUUID())
                                .principal(mockAuth))
                                .andExpect(status().isOk())
                                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"document.pdf\""))
                                .andExpect(content().string("Sample file"));

                verify(fileService).downloadFile(any(UUID.class));
        }
}