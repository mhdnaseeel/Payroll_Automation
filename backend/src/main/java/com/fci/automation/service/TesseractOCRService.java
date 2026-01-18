package com.fci.automation.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TesseractOCRService {

    private static final Logger logger = LoggerFactory.getLogger(TesseractOCRService.class);
    private final Tesseract tesseract;

    public TesseractOCRService() {
        this.tesseract = new Tesseract();
        // Assuming default datapath or setting it if necessary.
        // On Mac with Homebrew, it's often /opt/homebrew/share/tessdata or
        // /usr/local/share/tessdata
        // We can try to detect or let it fallback to environment variables.
        // tesseract.setDatapath("/opt/homebrew/share/tessdata");
        // Setting it to a common path if TESSDATA_PREFIX is not set might be safer,
        // but 'tesseract' command worked so enviroment might be set.
        // Let's rely on default behavior first or set a sensible default if needed.
        if (new File("/opt/homebrew/share/tessdata").exists()) {
            tesseract.setDatapath("/opt/homebrew/share/tessdata");
        } else if (new File("/usr/local/share/tessdata").exists()) {
            tesseract.setDatapath("/usr/local/share/tessdata");
        } else if (new File("/usr/share/tessdata").exists()) {
            tesseract.setDatapath("/usr/share/tessdata");
        } else if (new File("/usr/share/tesseract-ocr/4.00/tessdata").exists()) {
            tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
        }
        tesseract.setLanguage("eng");
    }

    public String extractRawText(MultipartFile file) throws IOException {
        logger.info("Starting Tesseract OCR extraction for file: {}", file.getOriginalFilename());

        // Use a temp file for the original
        Path originalTemp = Files.createTempFile("ocr_orig_", "_" + file.getOriginalFilename());
        file.transferTo(originalTemp.toFile());

        File processedFile = null;
        try {
            // Preprocessing: Scale & Binarize
            processedFile = preprocessImage(originalTemp.toFile());

            logger.info("Image pre-processed at: {}", processedFile.getAbsolutePath());

            String result = tesseract.doOCR(processedFile);

            logger.info("Tesseract OCR completed. Content length: {}", result.length());
            logger.debug("Tesseract Raw Output: {}", result);

            return result;
        } catch (TesseractException e) {
            logger.error("Tesseract OCR failed", e);
            throw new IOException("Tesseract OCR Extraction Failed: " + e.getMessage(), e);
        } finally {
            try {
                Files.deleteIfExists(originalTemp);
                if (processedFile != null)
                    processedFile.delete();
            } catch (IOException ignored) {
            }
        }
    }

    private File preprocessImage(File inputFile) throws IOException {
        java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(inputFile);

        // 1. Rescale (Upscale) - Tesseract likes 300 DPI.
        // Assuming typical 72 DPI input, 2x or 3x scaling helps.
        int targetWidth = original.getWidth() * 2;
        int targetHeight = original.getHeight() * 2;

        java.awt.image.BufferedImage resized = new java.awt.image.BufferedImage(
                targetWidth, targetHeight, java.awt.image.BufferedImage.TYPE_BYTE_GRAY);

        java.awt.Graphics2D g = resized.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        // 2. Binarization (Simple Thresholding) - Optional improvement
        // For now, Grayscale (TYPE_BYTE_GRAY) is often sufficient and safer than bad
        // thresholding.
        // Let's stick to High-Quality Grayscale Upscaling first, as that fixes the
        // "Resolution" warning best.

        File tempOut = File.createTempFile("ocr_proc_", ".png");
        javax.imageio.ImageIO.write(resized, "png", tempOut);

        return tempOut;
    }
}
