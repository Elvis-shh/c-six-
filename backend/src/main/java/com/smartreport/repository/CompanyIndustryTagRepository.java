package com.smartreport.repository;

import com.smartreport.models.entity.CompanyIndustryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyIndustryTagRepository extends JpaRepository<CompanyIndustryTag, Long> {
    @Query("SELECT t.companyCode FROM CompanyIndustryTag t WHERE t.tag = :tag")
    List<String> findCompanyCodesByTag(@Param("tag") String tag);

    boolean existsByCompanyCodeAndTag(String companyCode, String tag);
}
