package org.example.passport_scanner_api.controller;

import org.example.passport_scanner_api.service.PassportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class PassportController {

    private final PassportService service;

    public PassportController(PassportService service) {
        this.service = service;
    }

    @PostMapping(value = "/upload-files", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> uploadFiles(@RequestParam("file") MultipartFile file) throws IOException {

        if (!isSupportedFileType(file.getContentType())) {
            return ResponseEntity.badRequest().body(null);
        }

        byte[] passportImage = service.passportImageExtract(file.getBytes());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"passport_" + file.getOriginalFilename() + "\""
                )
                .body(passportImage);

    }

    private boolean isSupportedFileType(String contentType) {
        return contentType != null && (
                contentType.startsWith("image/") ||
                contentType.equals("application/pdf")
        );
    }
}
