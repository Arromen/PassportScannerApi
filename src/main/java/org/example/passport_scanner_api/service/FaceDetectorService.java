package org.example.passport_scanner_api.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class FaceDetectorService {
    private CascadeClassifier faceDetector;
    private final int desiredOutputWidth = 300;
    private final double desiredAspectRatio = 3.0 / 4.0;
    private final int desiredOutputHeight = (int) (desiredOutputWidth / desiredAspectRatio);
    private final Size outputSize = new Size(desiredOutputWidth, desiredOutputHeight);
    private final double paddingFactorWidth = 1.4;
    private final double paddingFactorHeight = 1.8;

    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Не удалось загрузить нативную библиотеку OpenCV", e);
        }
    }

    @PostConstruct
    public void init() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/haarcascade/haarcascade_frontalface_default.xml")) {
            if (is == null) {
                throw new FileNotFoundException("Не найден файл каскада Хаара");
            }

            Path tempFile = Files.createTempFile("cascade", ".xml");
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);

            faceDetector = new CascadeClassifier(tempFile.toString());

            if (faceDetector.empty()) {
                Files.delete(tempFile);
                throw new IOException("Не удалось загрузить каскад для определения лица");
            }

            Files.delete(tempFile);
        }
    }

    public byte[] detectAndCropFace(byte[] imageData) {
        Mat originalImage = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
        if (originalImage.empty()) {
            throw new IllegalArgumentException("Не удалось загрузить изображение");
        }

        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(originalImage, faceDetections, 1.1, 10, 0,
                new Size(40, 30), new Size());

        Rect[] facesArray = faceDetections.toArray();
        if (facesArray.length == 0) {
            throw new IllegalArgumentException("Лицо не обнаружено на изображении");
        }

        Rect faceRect = facesArray[0];

        int centeredWidth = (int) (faceRect.width * paddingFactorWidth);
        int centeredHeight = (int) (faceRect.height * paddingFactorHeight);

        if ((double) centeredWidth / centeredHeight > desiredAspectRatio) {
            centeredWidth = (int) (centeredHeight * desiredAspectRatio);
        } else {
            centeredHeight = (int) (centeredWidth / desiredAspectRatio);
        }

        int x = Math.max(0, faceRect.x - (centeredWidth - faceRect.width) / 2);
        int y = Math.max(0, faceRect.y - (centeredHeight - faceRect.height) / 2);

        centeredWidth = Math.min(centeredWidth, originalImage.cols() - x);
        centeredHeight = Math.min(centeredHeight, originalImage.rows() - y);

        Rect finalCutRect = new Rect(x, y, centeredWidth, centeredHeight);
        Mat croppedFaceRegion = new Mat(originalImage, finalCutRect);

        Mat finalPassportFace = new Mat();
        Imgproc.resize(croppedFaceRegion, finalPassportFace, outputSize, 0, 0, Imgproc.INTER_LINEAR);

        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".png", finalPassportFace, mob);
        return mob.toArray();
    }
}

