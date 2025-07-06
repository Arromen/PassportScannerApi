package org.example.passport_scanner_api.service;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class PassportService {

    private final FaceDetectorService faceDetectorService;

    public PassportService(FaceDetectorService faceDetectorService) {
        this.faceDetectorService = faceDetectorService;
    }

    public List<FaceDetectorService.ProcessedImage> processFile(byte[] fileData, String originalFilename, boolean isPdf)
            throws IOException {
        return faceDetectorService.processFile(fileData, originalFilename, isPdf);
    }
}
