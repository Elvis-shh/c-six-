package com.smartreport.controller;

import com.smartreport.common.ApiResponse;
import com.smartreport.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getHistory(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(historyService.getHistory(userId));
    }

    @PostMapping
    public ApiResponse<Void> addHistory(@AuthenticationPrincipal Long userId,
                                         @RequestBody Map<String, String> req) {
        historyService.addHistory(userId, req.get("companyCode"), req.get("companyName"));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteHistory(@AuthenticationPrincipal Long userId,
                                            @PathVariable Long id) {
        historyService.deleteHistory(userId, id);
        return ApiResponse.success(null);
    }

    @DeleteMapping
    public ApiResponse<Void> clearHistory(@AuthenticationPrincipal Long userId) {
        historyService.clearHistory(userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/sync")
    public ApiResponse<List<Map<String, Object>>> syncHistory(@AuthenticationPrincipal Long userId,
                                                                @RequestBody List<Map<String, String>> localItems) {
        return ApiResponse.success(historyService.syncHistory(userId, localItems));
    }
}
