package org.example.passport_scanner_api.controller;

import org.example.passport_scanner_api.service.FaceDetectorService;
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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api")
public class PassportController {

    private final PassportService service;

    public PassportController(PassportService service) {
        this.service = service;
    }

    @PostMapping(value = "/upload-file", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (!isSupportedFile(file)) {
                return ResponseEntity.badRequest()
                        .header("X-Error", "Unsupported file type")
                        .body(("Error: Unsupported file type - " + file.getContentType()).getBytes());
            }

            boolean isPdf = "application/pdf".equals(file.getContentType());
            List<FaceDetectorService.ProcessedImage> results = service.processFile(
                    file.getBytes(),
                    file.getOriginalFilename(),
                    isPdf
            );

            if (results.isEmpty()) {
                return ResponseEntity.badRequest()
                        .header("X-Error", "No faces detected")
                        .body("Error: No faces detected in the document".getBytes());
            }

            for (FaceDetectorService.ProcessedImage result : results) {
                if (!result.filename.startsWith("error_")) {
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + result.filename + "\"")
                            .body(result.content);
                }
            }

            return ResponseEntity.badRequest()
                    .header("X-Error", "All pages processing failed")
                    .body("Error: Failed to process all pages".getBytes());

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .header("X-Error", "File processing error")
                    .body(("Error: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .header("X-Error", "Unexpected error")
                    .body(("Unexpected error: " + e.getMessage()).getBytes());
        }
    }

    private boolean isSupportedFile(MultipartFile file) {
        String type = file.getContentType();
        return type != null && (type.startsWith("image/") || type.equals("application/pdf"));
    }
}
