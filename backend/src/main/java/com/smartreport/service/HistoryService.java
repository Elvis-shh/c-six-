package com.smartreport.service;

import com.smartreport.models.entity.Company;
import com.smartreport.models.entity.UserSearchHistory;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.repository.UserSearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private static final int MAX_ITEMS = 50;
    private final UserSearchHistoryRepository historyRepo;
    private final CompanyRepository companyRepository;

    public void addHistory(Long userId, String companyCode, String companyName) {
        historyRepo.deleteByUserIdAndCompanyCode(userId, companyCode);
        UserSearchHistory history = UserSearchHistory.builder()
                .userId(userId)
                .companyCode(companyCode)
                .searchedAt(LocalDateTime.now())
                .build();
        historyRepo.save(history);

        long count = historyRepo.countByUserId(userId);
        if (count > MAX_ITEMS) {
            historyRepo.deleteOldestByUserId(userId, count - MAX_ITEMS);
        }
    }

    public List<Map<String, Object>> getHistory(Long userId) {
        List<UserSearchHistory> list = historyRepo.findRecentByUserId(userId, PageRequest.of(0, MAX_ITEMS));
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserSearchHistory h : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", h.getId());
            item.put("code", h.getCompanyCode());
            item.put("timestamp", h.getSearchedAt() != null
                    ? h.getSearchedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : System.currentTimeMillis());
            Company company = companyRepository.findById(h.getCompanyCode()).orElse(null);
            item.put("name", company != null ? company.getName() : h.getCompanyCode());
            result.add(item);
        }
        return result;
    }

    public void deleteHistory(Long userId, Long id) {
        historyRepo.findById(id).ifPresent(h -> {
            if (h.getUserId() != null && h.getUserId().equals(userId)) {
                historyRepo.delete(h);
            }
        });
    }

    public void clearHistory(Long userId) {
        historyRepo.deleteAllByUserId(userId);
    }

    public List<Map<String, Object>> syncHistory(Long userId, List<Map<String, String>> localItems) {
        for (Map<String, String> item : localItems) {
            String code = item.get("code");
            String name = item.get("name");
            if (code != null) {
                addHistory(userId, code, name);
            }
        }
        return getHistory(userId);
    }
}
