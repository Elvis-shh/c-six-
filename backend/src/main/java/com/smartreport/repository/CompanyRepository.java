package com.smartreport.repository;

import com.smartreport.models.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, String> {

    @Query("SELECT c FROM Company c WHERE c.status = 1 AND " +
           "(c.name LIKE CONCAT('%', :keyword, '%') OR " +
           "c.code LIKE CONCAT('%', :keyword, '%') OR " +
           "c.shortName LIKE CONCAT('%', :keyword, '%'))")
    Page<Company> searchCompanies(@Param("keyword") String keyword, Pageable pageable);

    List<Company> findByCodeIn(List<String> codes);

    List<Company> findByStatus(Integer status);
}
