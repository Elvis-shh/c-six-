package com.smartreport.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class FileValidationService {
    private static final long MAX_SIZE = 50L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("pdf", "doc", "docx", "xls", "xlsx", "txt", "jpg", "jpeg", "png");

    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 50MB");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = getExtension(name);
        if (!ALLOWED.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }
        return ext;
    }

    public String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }
}
