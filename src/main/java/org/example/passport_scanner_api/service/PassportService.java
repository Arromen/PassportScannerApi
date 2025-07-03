package org.example.passport_scanner_api.service;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public class PassportService {

    private final FaceDetectorService faceDetectorService;

    public PassportService(FaceDetectorService faceDetectorService) {
        this.faceDetectorService = faceDetectorService;
    }


    public byte[] passportImageExtract(byte[] imageData) {
        return faceDetectorService.detectAndCropFace(imageData);
    }
}
