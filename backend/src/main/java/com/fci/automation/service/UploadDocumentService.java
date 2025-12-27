package com.fci.automation.service;

import com.fci.automation.entity.UploadDocument;
import com.fci.automation.repository.UploadDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UploadDocumentService {

    @Autowired
    private UploadDocumentRepository repository;

    private final Path fileStorageLocation;

    public UploadDocumentService() {
        this.fileStorageLocation = Paths.get(System.getProperty("user.dir") + "/uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public UploadDocument uploadFile(MultipartFile file, String type, String subType, UUID periodId) {
        // Normalize file name
        String originalFileName = file.getOriginalFilename();
        // unique filename to prevent overwrites
        String fileName = System.currentTimeMillis() + "_" + originalFileName;

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation);

            // CHECK FOR EXISTING DOCUMENT (REPLACEMENT LOGIC)
            java.util.Optional<UploadDocument> existingOpt = repository.findByPeriodIdAndTypeAndSubType(periodId, type,
                    subType);

            if (existingOpt.isPresent()) {
                UploadDocument existingDoc = existingOpt.get();

                // Delete OLD file from disk
                try {
                    Files.deleteIfExists(Paths.get(existingDoc.getFilePath()));
                } catch (IOException e) {
                    System.err.println("Could not delete old file: " + e.getMessage());
                    // Continue anyway to update DB
                }

                // Update Existing Record
                existingDoc.setFileName(originalFileName);
                existingDoc.setFilePath(targetLocation.toString());
                existingDoc.setUploadDate(LocalDateTime.now());

                return repository.save(existingDoc);

            } else {
                // Create NEW Record
                UploadDocument doc = new UploadDocument(type, subType, originalFileName, targetLocation.toString(),
                        LocalDateTime.now(), periodId);
                return repository.save(doc);
            }

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public List<UploadDocument> getAllDocuments() {
        return repository.findAllByOrderByUploadDateDesc();
    }

    public List<UploadDocument> getDocumentsByPeriod(UUID periodId) {
        return repository.findByPeriodIdOrderByUploadDateDesc(periodId);
    }

    public UploadDocument getDocument(UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("File not found with id " + id));
    }

    public Resource loadFileAsResource(String filePath) {
        try {
            Path filePathPath = Paths.get(filePath);
            Resource resource = new UrlResource(filePathPath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + filePath);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + filePath, ex);
        }
    }
}
