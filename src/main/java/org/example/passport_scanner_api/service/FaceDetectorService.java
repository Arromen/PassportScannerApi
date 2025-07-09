package org.example.passport_scanner_api.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FaceDetectorService {
    private static final int MAX_PDF_PAGES = 20;
    private static final int MIN_FACE_SIZE = 40;
    private static final double SCALE_FACTOR = 1.1;
    private static final int MIN_NEIGHBORS = 3;

    private CascadeClassifier faceDetector;
    private final int desiredWidth = 300;
    private final double aspectRatio = 3.0 / 4.0;
    private final int pdfDpi = 300;

    @PostConstruct
    public void init() throws IOException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        try (InputStream is = getClass().getResourceAsStream("/haarcascade/haarcascade_frontalface_default.xml")) {
            if (is == null) {
                throw new IOException("Face detection model not found in resources");
            }

            Path tempFile = Files.createTempFile("cascade", ".xml");
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);

            faceDetector = new CascadeClassifier(tempFile.toString());
            if (faceDetector.empty()) {
                throw new IOException("Failed to load face detection model");
            }

            Files.deleteIfExists(tempFile);
        }
    }

    public List<ProcessedImage> processFile(byte[] fileData, String filename, boolean isPdf)
            throws IOException, ProcessingException {

        List<ProcessedImage> results = new ArrayList<>();

        if (isPdf) {
            processPdfFile(fileData, filename, results);
        } else {
            processImageFile(fileData, filename, results);
        }

        if (results.isEmpty()) {
            throw new ProcessingException("No valid content found in the file");
        }

        return results;
    }

    private void processPdfFile(byte[] fileData, String filename, List<ProcessedImage> results)
            throws IOException {

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(fileData))) {
            int pageCount = document.getNumberOfPages();

            if (pageCount > MAX_PDF_PAGES) {
                throw new ProcessingException(
                        String.format("PDF has too many pages (%d). Maximum allowed: %d",
                                pageCount, MAX_PDF_PAGES)
                );
            }

            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < pageCount; i++) {
                try {
                    BufferedImage pageImage = renderer.renderImageWithDPI(i, pdfDpi);
                    processPageImage(pageImage, filename + "_page_" + (i + 1), results);
                } catch (Exception e) {
                    results.add(new ProcessedImage(
                            "error_" + filename + "_page_" + (i + 1) + ".txt",
                            ("Page processing failed: " + e.getMessage()).getBytes()
                    ));
                }
            }
        } catch (ProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void processImageFile(byte[] fileData, String filename, List<ProcessedImage> results) {
        try {
            Mat image = Imgcodecs.imdecode(new MatOfByte(fileData), Imgcodecs.IMREAD_COLOR);
            if (image.empty()) {
                throw new ProcessingException("Failed to decode image");
            }

            processOpenCvImage(image, filename, results);
            image.release();
        } catch (Exception e) {
            results.add(new ProcessedImage(
                    "error_" + filename + ".txt",
                    ("Image processing failed: " + e.getMessage()).getBytes()
            ));
        }
    }

    private void processPageImage(BufferedImage image, String name, List<ProcessedImage> results) throws ProcessingException {
        try {
            Mat matImage = bufferedImageToMat(image);
            processOpenCvImage(matImage, name, results);
            matImage.release();
        } catch (Exception e) {
            throw new ProcessingException("Failed to process page: " + e.getMessage());
        }
    }

    private void processOpenCvImage(Mat image, String name, List<ProcessedImage> results) throws ProcessingException {
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(
                image,
                faceDetections,
                SCALE_FACTOR,
                MIN_NEIGHBORS,
                0,
                new Size(MIN_FACE_SIZE, MIN_FACE_SIZE),
                new Size()
        );

        Rect[] faces = faceDetections.toArray();
        if (faces.length == 0) {
            throw new ProcessingException("No faces detected in " + name);
        }

        Rect primaryFace = faces[0];
        Rect adjustedRect = adjustFaceRect(primaryFace, image.size());

        Mat croppedFace = new Mat(image, adjustedRect);
        Mat resizedFace = new Mat();
        Size targetSize = new Size(desiredWidth, (int)(desiredWidth / aspectRatio));

        Imgproc.resize(croppedFace, resizedFace, targetSize, 0, 0, Imgproc.INTER_LINEAR);

        MatOfByte resultData = new MatOfByte();
        if (!Imgcodecs.imencode(".png", resizedFace, resultData)) {
            throw new ProcessingException("Failed to encode face image");
        }

        results.add(new ProcessedImage("face_" + name + ".png", resultData.toArray()));

        croppedFace.release();
        resizedFace.release();
        faceDetections.release();
    }

    private Rect adjustFaceRect(Rect face, Size imageSize) {
        int width = (int)(face.width * 1.4);
        int height = (int)(face.height * 1.8);

        if ((double)width / height > aspectRatio) {
            width = (int)(height * aspectRatio);
        } else {
            height = (int)(width / aspectRatio);
        }

        int x = Math.max(0, face.x - (width - face.width) / 2);
        int y = Math.max(0, face.y - (height - face.height) / 2);

        width = Math.min(width, (int)imageSize.width - x);
        height = Math.min(height, (int)imageSize.height - y);

        return new Rect(x, y, width, height);
    }

    private Mat bufferedImageToMat(BufferedImage image) {
        BufferedImage converted = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        converted.getGraphics().drawImage(image, 0, 0, null);
        converted.getGraphics().dispose();

        byte[] pixels = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(converted.getHeight(), converted.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }

    public static class ProcessedImage {
        public final String filename;
        public final byte[] content;

        public ProcessedImage(String filename, byte[] content) {
            this.filename = filename;
            this.content = content;
        }
    }

    public static class ProcessingException extends Exception {
        public ProcessingException(String message) {
            super(message);
        }
    }
}

