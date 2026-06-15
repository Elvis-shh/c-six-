package com.smartreport.repository;

import com.smartreport.models.entity.IndicatorDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndicatorDefinitionRepository extends JpaRepository<IndicatorDefinition, String> {

    List<IndicatorDefinition> findAllByOrderBySortOrderAsc();
}
