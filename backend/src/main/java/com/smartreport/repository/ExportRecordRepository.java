package com.smartreport.repository;

import com.smartreport.models.entity.ExportRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExportRecordRepository extends JpaRepository<ExportRecord, String> {

    List<ExportRecord> findByStatusAndCreatedAtBefore(String status, LocalDateTime cutoff);
}
