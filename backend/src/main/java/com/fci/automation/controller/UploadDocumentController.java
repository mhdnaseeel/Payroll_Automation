package com.fci.automation.controller;

import com.fci.automation.entity.UploadDocument;
import com.fci.automation.service.UploadDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "http://localhost:4200")
public class UploadDocumentController {

    @Autowired
    private UploadDocumentService service;

    @PostMapping
    public ResponseEntity<UploadDocument> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam("type") String type,
            @RequestParam("subType") String subType,
            @RequestParam("periodId") UUID periodId) {
        UploadDocument doc = service.uploadFile(file, type, subType, periodId);
        return ResponseEntity.ok(doc);
    }

    @GetMapping
    public ResponseEntity<List<UploadDocument>> listFiles(
            @RequestParam(value = "periodId", required = false) UUID periodId) {
        if (periodId != null) {
            return ResponseEntity.ok(service.getDocumentsByPeriod(periodId));
        }
        return ResponseEntity.ok(service.getAllDocuments());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID id) {
        UploadDocument doc = service.getDocument(id);
        Resource resource = service.loadFileAsResource(doc.getFilePath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
