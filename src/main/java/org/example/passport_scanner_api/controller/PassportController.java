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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api")
public class PassportController {

    private final PassportService service;

    public PassportController(PassportService service) {
        this.service = service;
    }

    @PostMapping(value = "/upload-files", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> uploadFiles(@RequestParam("files") MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(null);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (MultipartFile file : files) {
                if (!isSupportedFileType(file.getContentType())) {
                    continue;
                }

                try {
                    byte[] passportImage = service.passportImageExtract(file.getBytes());

                    ZipEntry entry = new ZipEntry("passport_" + file.getOriginalFilename());
                    zos.putNextEntry(entry);
                    zos.write(passportImage);
                    zos.closeEntry();
                } catch (Exception e) {
                    System.err.println("Ошибка при обработке файла " + file.getOriginalFilename() + ": " + e.getMessage());
                }
            }
        }

        if (baos.size() == 0) {
            return ResponseEntity.badRequest().body("Не удалось обработать ни один файл".getBytes());
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"passport_images.zip\""
                )
                .body(baos.toByteArray());
    }

    private boolean isSupportedFileType(String contentType) {
        return contentType != null && (
                contentType.startsWith("image/") ||
                contentType.equals("application/pdf")
        );
    }
}
