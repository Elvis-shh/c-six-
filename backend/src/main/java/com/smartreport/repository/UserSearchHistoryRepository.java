package com.smartreport.repository;

import com.smartreport.models.entity.UserSearchHistory;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSearchHistoryRepository extends JpaRepository<UserSearchHistory, Long> {

    List<UserSearchHistory> findByUserIdOrderBySearchedAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);

    long countByUserId(Long userId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_search_history WHERE id IN (SELECT id FROM (SELECT id FROM user_search_history WHERE user_id = :userId ORDER BY searched_at ASC LIMIT :count) AS t)", nativeQuery = true)
    void deleteOldestByUserId(@Param("userId") Long userId, @Param("count") long count);

    @Modifying
    @Transactional
    void deleteByUserIdAndCompanyCode(Long userId, String companyCode);

    @Modifying
    @Transactional
    void deleteAllByUserId(Long userId);

    @Query("SELECT h FROM UserSearchHistory h WHERE h.userId = :userId ORDER BY h.searchedAt DESC")
    List<UserSearchHistory> findRecentByUserId(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);
}
