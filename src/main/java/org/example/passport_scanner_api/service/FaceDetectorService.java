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
    private CascadeClassifier faceDetector;
    private final int desiredOutputWidth = 300;
    private final double desiredAspectRatio = 3.0 / 4.0;
    private final int pdfRenderDPI = 300;

    @PostConstruct
    public void init() throws IOException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        try (InputStream is = getClass().getResourceAsStream("/haarcascade/haarcascade_frontalface_default.xml")) {
            Path tempFile = Files.createTempFile("cascade", ".xml");
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);

            faceDetector = new CascadeClassifier(tempFile.toString());
            if (faceDetector.empty()) {
                throw new IOException("Не удалось загрузить каскадный классификатор");
            }
            Files.delete(tempFile);
        }
    }

    public List<ProcessedImage> processFile(byte[] fileData, String originalFilename, boolean isPdf) throws IOException {
        List<ProcessedImage> results = new ArrayList<>();

        if (isPdf) {
            try (PDDocument document = PDDocument.load(new ByteArrayInputStream(fileData))) {
                PDFRenderer renderer = new PDFRenderer(document);
                for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                    BufferedImage pageImage = renderer.renderImageWithDPI(pageNum, pdfRenderDPI);
                    Mat imageMat = bufferedImageToMat(pageImage);

                    String pageName = originalFilename.replace(".pdf", "") + "_page_" + (pageNum + 1);
                    byte[] processedImage = processImage(imageMat, pageName);
                    results.add(new ProcessedImage("passport_" + pageName + ".png", processedImage));

                    imageMat.release();
                }
            }
        } else {
            Mat imageMat = Imgcodecs.imdecode(new MatOfByte(fileData), Imgcodecs.IMREAD_COLOR);
            byte[] processedImage = processImage(imageMat, originalFilename);
            results.add(new ProcessedImage("passport_" + originalFilename, processedImage));
            imageMat.release();
        }

        return results;
    }

    private byte[] processImage(Mat imageMat, String imageName) {
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(imageMat, faceDetections, 1.1, 10, 0,
                new Size(40, 30), new Size());

        Rect[] faces = faceDetections.toArray();
        if (faces.length == 0) {
            throw new IllegalArgumentException("Лица не обнаружены в " + imageName);
        }

        Rect faceRect = calculateFaceRect(faces[0], imageMat);
        Mat croppedFace = new Mat(imageMat, faceRect);
        Mat resizedFace = new Mat();

        Size outputSize = new Size(desiredOutputWidth, (int)(desiredOutputWidth / desiredAspectRatio));
        Imgproc.resize(croppedFace, resizedFace, outputSize, 0, 0, Imgproc.INTER_LINEAR);

        MatOfByte result = new MatOfByte();
        Imgcodecs.imencode(".png", resizedFace, result);

        croppedFace.release();
        resizedFace.release();
        faceDetections.release();

        return result.toArray();
    }

    private Rect calculateFaceRect(Rect face, Mat image) {
        int width = (int)(face.width * 1.4);
        int height = (int)(face.height * 1.8);

        if ((double)width / height > desiredAspectRatio) {
            width = (int)(height * desiredAspectRatio);
        } else {
            height = (int)(width / desiredAspectRatio);
        }

        int x = Math.max(0, face.x - (width - face.width) / 2);
        int y = Math.max(0, face.y - (height - face.height) / 2);

        width = Math.min(width, image.cols() - x);
        height = Math.min(height, image.rows() - y);

        return new Rect(x, y, width, height);
    }

    private Mat bufferedImageToMat(BufferedImage bi) {
        BufferedImage converted = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        converted.getGraphics().drawImage(bi, 0, 0, null);
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
}

