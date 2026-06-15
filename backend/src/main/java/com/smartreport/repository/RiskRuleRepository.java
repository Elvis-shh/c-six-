package com.smartreport.repository;

import com.smartreport.models.entity.RiskRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskRuleRepository extends JpaRepository<RiskRule, String> {

    List<RiskRule> findByEnabledOrderByPriorityAsc(Integer enabled);
}
