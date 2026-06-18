package com.smartreport.repository;

import com.smartreport.models.entity.MqTaskRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MqTaskRecordRepository extends JpaRepository<MqTaskRecord, Long> {
    Optional<MqTaskRecord> findByTaskId(String taskId);
}
