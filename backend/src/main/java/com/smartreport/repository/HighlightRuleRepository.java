package com.smartreport.repository;

import com.smartreport.models.entity.HighlightRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HighlightRuleRepository extends JpaRepository<HighlightRule, String> {

    List<HighlightRule> findByEnabledOrderByPriorityAsc(Integer enabled);
}
