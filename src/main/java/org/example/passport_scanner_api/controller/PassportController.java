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

    @PostMapping(value = "/upload-files", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> uploadFiles(@RequestParam("files") MultipartFile[] files) throws IOException {
        ByteArrayOutputStream zipStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOut = new ZipOutputStream(zipStream)) {
            int totalPages = 0;
            int processedPages = 0;

            for (MultipartFile file : files) {
                if (!isSupportedFile(file)) continue;

                boolean isPdf = file.getContentType().equals("application/pdf");
                String originalName = file.getOriginalFilename();

                try {
                    List<FaceDetectorService.ProcessedImage> results =
                            service.processFile(file.getBytes(), originalName, isPdf);

                    if (isPdf) {
                        totalPages += results.size();
                    }

                    for (FaceDetectorService.ProcessedImage result : results) {
                        zipOut.putNextEntry(new ZipEntry(result.filename));
                        zipOut.write(result.content);
                        zipOut.closeEntry();
                        processedPages++;
                    }
                } catch (Exception e) {
                    zipOut.putNextEntry(new ZipEntry("error_" + originalName + ".txt"));
                    zipOut.write(("Ошибка обработки файла: " + e.getMessage()).getBytes());
                    zipOut.closeEntry();
                }
            }

            // Добавляем отчет в архив
            zipOut.putNextEntry(new ZipEntry("processing_report.txt"));
            String report = String.format(
                    "Обработано файлов: %d\nОбработано страниц: %d\n",
                    files.length, processedPages
            );
            zipOut.write(report.getBytes());
            zipOut.closeEntry();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"passport_results.zip\"")
                .body(zipStream.toByteArray());
    }

    private boolean isSupportedFile(MultipartFile file) {
        String type = file.getContentType();
        return type != null && (type.startsWith("image/") || type.equals("application/pdf"));
    }
}
