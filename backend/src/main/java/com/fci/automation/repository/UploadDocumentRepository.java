package com.fci.automation.repository;

import com.fci.automation.entity.UploadDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UploadDocumentRepository extends JpaRepository<UploadDocument, UUID> {
    List<UploadDocument> findByType(String type);

    List<UploadDocument> findAllByOrderByUploadDateDesc();

    List<UploadDocument> findByPeriodIdOrderByUploadDateDesc(UUID periodId);

    java.util.Optional<UploadDocument> findByPeriodIdAndTypeAndSubType(UUID periodId, String type, String subType);
}
