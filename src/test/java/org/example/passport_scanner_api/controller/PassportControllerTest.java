package org.example.passport_scanner_api.controller;

import org.example.passport_scanner_api.service.PassportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PassportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PassportService service;

    @Test
    public void testValidImageUpload() throws Exception {
        // Подготавливаем тестовые данные
        byte[] fakeImageBytes = new byte[]{1, 2, 3};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "passport.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                getClass().getResourceAsStream("/images/passport.jpg").readAllBytes()
        );

        when(service.passportImageExtract(any(byte[].class)))
                .thenReturn(fakeImageBytes);

        mockMvc.perform(multipart("/api/upload-files").file(file))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        "attachment; filename=\"passport_passport.jpg\""
                ))
                .andExpect(content().bytes(fakeImageBytes));
    }

    @Test
    public void testUnsupportedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.txt",
                "text/plain",
                "invalid content".getBytes()
        );

        mockMvc.perform(multipart("/api/upload-files").file(file))
                .andExpect(status().isBadRequest());
    }

}