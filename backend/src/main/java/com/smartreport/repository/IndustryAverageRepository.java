package com.smartreport.repository;

import com.smartreport.models.entity.IndustryAverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndustryAverageRepository extends JpaRepository<IndustryAverage, Long> {

    List<IndustryAverage> findByIndustryAndYear(String industry, Integer year);

    List<IndustryAverage> findByIndustryAndIndicatorKeyIn(String industry, List<String> indicatorKeys);
}
