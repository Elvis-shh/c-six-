package com.smartreport.service;

import com.smartreport.models.entity.Company;
import com.smartreport.models.entity.ExportRecord;
import com.smartreport.models.entity.FinancialIndicator;
import com.smartreport.models.entity.FinancialReport;
import com.smartreport.repository.CompanyRepository;
import com.smartreport.repository.ExportRecordRepository;
import com.smartreport.repository.FinancialIndicatorRepository;
import com.smartreport.repository.FinancialReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ExportRecordRepository exportRecordRepository;
    private final CompanyRepository companyRepository;
    private final FinancialReportRepository reportRepository;
    private final FinancialIndicatorRepository indicatorRepository;
    private final Path exportDir = Paths.get(System.getProperty("java.io.tmpdir"), "smartreport-exports");

    private static final String DISCLAIMER_TEXT =
            "免责声明：本报告由 SmartReport 自动生成，数据基于公开财报信息。"
                    + "所有分析和预测仅供参考学习，不构成任何投资建议。投资有风险，入市需谨慎。";

    public String submit(String companyCode, String format) {
        String taskId = "export_" + UUID.randomUUID().toString().substring(0, 12);
        String date = LocalDateTime.now().toLocalDate().toString();
        String fileName = companyCode + "_财报分析报告_" + date + "." + format;

        ExportRecord record = ExportRecord.builder()
                .taskId(taskId)
                .companyCode(companyCode)
                .format(format)
                .status("pending")
                .fileName(fileName)
                .progress(0)
                .build();
        exportRecordRepository.save(record);

        processExport(record);
        return taskId;
    }

    @Async
    public void processExport(ExportRecord record) {
        try {
            record.setStatus("rendering");
            record.setProgress(10);
            exportRecordRepository.save(record);

            Files.createDirectories(exportDir);
            Path filePath = exportDir.resolve(record.getFileName());

            record.setProgress(30);
            exportRecordRepository.save(record);

            byte[] fileBytes;
            switch (record.getFormat()) {
                case "pdf" -> fileBytes = generatePdf(record);
                case "xlsx" -> fileBytes = generateExcel(record);
                default -> fileBytes = generatePdf(record); // fallback to PDF
            }

            record.setProgress(90);
            exportRecordRepository.save(record);

            Files.write(filePath, fileBytes);

            record.setStatus("ready");
            record.setProgress(100);
            record.setFileUrl(filePath.toAbsolutePath().toString());
            record.setFileSize((long) fileBytes.length);
            record.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Export failed: {}", record.getTaskId(), e);
            record.setStatus("failed");
            record.setErrorMsg(e.getMessage());
        }
        exportRecordRepository.save(record);
    }

    /**
     * 生成 PDF 报告（含"仅供参考"半透明水印）
     */
    private byte[] generatePdf(ExportRecord record) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDPageContentStream cs = new PDPageContentStream(document, page);

        String companyCode = record.getCompanyCode();
        Company company = companyRepository.findById(companyCode).orElse(null);
        String companyName = company != null ? company.getName() : companyCode;
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        float margin = 60;
        float width = PDRectangle.A4.getWidth() - 2 * margin;
        float y = PDRectangle.A4.getHeight() - margin;

        // Title
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
        cs.newLineAtOffset(margin, y);
        cs.showText("SmartReport 财报分析报告");
        cs.endText();
        y -= 35;

        // Company info
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
        cs.newLineAtOffset(margin, y);
        cs.showText(companyName + "（" + companyCode + "）");
        cs.endText();
        y -= 25;

        // Generation time
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        cs.newLineAtOffset(margin, y);
        cs.showText("生成时间：" + dateStr);
        cs.endText();
        y -= 30;

        // Divider line
        cs.setLineWidth(1);
        cs.moveTo(margin, y);
        cs.lineTo(margin + width, y);
        cs.stroke();
        y -= 20;

        // Financial data section
        List<FinancialReport> reports = reportRepository
                .findActiveByCompanyCodeOrderForDisplay(companyCode);
        List<FinancialReport> recent = reports.stream()
                .filter(r -> !indicatorRepository.findByReportId(r.getId()).isEmpty())
                .sorted(Comparator.comparing(FinancialReport::getReportYear))
                .collect(java.util.stream.Collectors.toList());

        if (!recent.isEmpty()) {
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 13);
            cs.newLineAtOffset(margin, y);
            cs.showText("核心财务指标");
            cs.endText();
            y -= 22;

            FinancialReport latest = recent.get(recent.size() - 1);
            List<FinancialIndicator> indicators = indicatorRepository.findByReportId(latest.getId());

            String[] keys = {"revenue", "profit", "grossMargin", "debtRatio", "cashFlow", "roe"};
            for (String key : keys) {
                FinancialIndicator fi = indicators.stream()
                        .filter(i -> key.equals(i.getIndicatorKey())).findFirst().orElse(null);
                if (fi != null && y > margin + 40) {
                    String line = String.format("• %s：%s %s",
                            getIndicatorName(key), fi.getValue().toPlainString(), getIndicatorUnit(key));
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                    cs.newLineAtOffset(margin + 10, y);
                    cs.showText(line);
                    cs.endText();
                    y -= 18;
                }
            }
        }

        // Disclaimer
        y -= 15;
        if (y > margin + 60) {
            cs.setLineWidth(0.5f);
            cs.moveTo(margin, y);
            cs.lineTo(margin + width, y);
            cs.stroke();
            y -= 20;

            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
            cs.newLineAtOffset(margin, y);
            cs.showText("⚠️ " + DISCLAIMER_TEXT);
            cs.endText();
        }

        cs.close();

        // Add watermark overlay (semi-transparent "仅供参考")
        addWatermark(document);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        document.close();
        return baos.toByteArray();
    }

    /**
     * 在 PDF 每一页上叠加"仅供参考"半透明水印
     */
    private void addWatermark(PDDocument document) throws IOException {
        for (PDPage page : document.getPages()) {
            PDPageContentStream wmStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true);

            PDRectangle pageSize = page.getMediaBox();
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();

            // 45-degree diagonal watermark text, repeated
            wmStream.beginText();
            wmStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 52);
            // Light gray for watermark
            wmStream.setNonStrokingColor(new Color(0.85f, 0.85f, 0.85f));

            float stepX = 220;
            float stepY = 180;
            for (float x = -pageWidth * 0.3f; x < pageWidth * 1.3f; x += stepX) {
                for (float y = -pageHeight * 0.3f; y < pageHeight * 1.3f; y += stepY) {
                    // Rotate text using Matrix
                    wmStream.setTextMatrix(Matrix.getRotateInstance(
                            Math.toRadians(-35), x, y));
                    wmStream.newLineAtOffset(x, y);
                    wmStream.showText("仅供参考");
                }
            }

            wmStream.endText();
            wmStream.close();
        }
    }

    /**
     * 生成 Excel 报告（含页脚免责声明）
     */
    private byte[] generateExcel(ExportRecord record) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        String companyCode = record.getCompanyCode();
        Company company = companyRepository.findById(companyCode).orElse(null);
        String companyName = company != null ? company.getName() : companyCode;
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        // Create styles
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);

        CellStyle disclaimerStyle = workbook.createCellStyle();
        Font disclaimerFont = workbook.createFont();
        disclaimerFont.setColor(IndexedColors.ORANGE.getIndex());
        disclaimerFont.setFontHeightInPoints((short) 9);
        disclaimerStyle.setFont(disclaimerFont);
        disclaimerStyle.setWrapText(true);

        // ========== Sheet 1: 报告概览 ==========
        Sheet overviewSheet = workbook.createSheet("报告概览");

        Row titleRow = overviewSheet.createRow(0);
        titleRow.createCell(0).setCellValue("SmartReport 财报分析报告");
        titleRow.getCell(0).setCellStyle(titleStyle);
        overviewSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

        Row infoRow = overviewSheet.createRow(2);
        infoRow.createCell(0).setCellValue("公司名称：");
        infoRow.createCell(1).setCellValue(companyName);
        Row codeRow = overviewSheet.createRow(3);
        codeRow.createCell(0).setCellValue("股票代码：");
        codeRow.createCell(1).setCellValue(companyCode);
        Row dateRow = overviewSheet.createRow(4);
        dateRow.createCell(0).setCellValue("生成时间：");
        dateRow.createCell(1).setCellValue(dateStr);

        // ========== Sheet 2: 财务指标数据 ==========
        Sheet dataSheet = workbook.createSheet("财务指标");

        Row headerRow = dataSheet.createRow(0);
        String[] headers = {"年份", "营业总收入(亿)", "归母净利润(亿)", "毛利率(%)", "资产负债率(%)", "经营现金流(亿)", "ROE(%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<FinancialReport> reports = reportRepository
                .findActiveByCompanyCodeOrderForDisplay(companyCode);
        List<FinancialReport> recent = reports.stream()
                .filter(r -> !indicatorRepository.findByReportId(r.getId()).isEmpty())
                .sorted(Comparator.comparing(FinancialReport::getReportYear))
                .collect(java.util.stream.Collectors.toList());

        int rowIdx = 1;
        String[] metricKeys = {"revenue", "profit", "grossMargin", "debtRatio", "cashFlow", "roe"};
        for (FinancialReport r : recent) {
            Row row = dataSheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(String.valueOf(r.getReportYear()));
            row.getCell(0).setCellStyle(dataStyle);

            List<FinancialIndicator> indicators = indicatorRepository.findByReportId(r.getId());
            Map<String, BigDecimal> map = new HashMap<>();
            for (FinancialIndicator fi : indicators) {
                map.put(fi.getIndicatorKey(), fi.getValue());
            }
            for (int j = 0; j < metricKeys.length; j++) {
                BigDecimal val = map.get(metricKeys[j]);
                Cell cell = row.createCell(j + 1);
                if (val != null) {
                    cell.setCellValue(val.doubleValue());
                }
                cell.setCellStyle(dataStyle);
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            dataSheet.autoSizeColumn(i);
        }

        // ========== Disclaimer footer ==========
        Row disclaimerRow = dataSheet.createRow(rowIdx + 2);
        Cell discCell = disclaimerRow.createCell(0);
        discCell.setCellValue("⚠️ " + DISCLAIMER_TEXT);
        discCell.setCellStyle(disclaimerStyle);
        dataSheet.addMergedRegion(new CellRangeAddress(rowIdx + 2, rowIdx + 2, 0, headers.length - 1));

        // Also add to overview sheet
        Row overviewDiscRow = overviewSheet.createRow(7);
        Cell overviewDiscCell = overviewDiscRow.createCell(0);
        overviewDiscCell.setCellValue("⚠️ " + DISCLAIMER_TEXT);
        overviewDiscCell.setCellStyle(disclaimerStyle);
        overviewSheet.addMergedRegion(new CellRangeAddress(7, 7, 0, 3));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    private String getIndicatorName(String key) {
        return switch (key) {
            case "revenue" -> "营业总收入";
            case "profit" -> "归母净利润";
            case "grossMargin" -> "毛利率";
            case "debtRatio" -> "资产负债率";
            case "cashFlow" -> "经营现金流";
            case "roe" -> "净资产收益率(ROE)";
            default -> key;
        };
    }

    private String getIndicatorUnit(String key) {
        return switch (key) {
            case "grossMargin", "debtRatio", "netMargin", "roe" -> "%";
            case "revenue", "profit", "cashFlow", "totalAssets", "totalLiabilities", "equity" -> "亿";
            default -> "";
        };
    }

    public ExportRecord getTaskStatus(String taskId) {
        return exportRecordRepository.findById(taskId)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + taskId));
    }

    public ExportRecord getCompletedTask(String taskId) {
        ExportRecord record = getTaskStatus(taskId);
        if (!"ready".equals(record.getStatus())) {
            throw new IllegalStateException("Export not ready, current status: " + record.getStatus());
        }
        return record;
    }

    public byte[] downloadBytes(String taskId) throws IOException {
        ExportRecord record = getCompletedTask(taskId);
        return Files.readAllBytes(Paths.get(record.getFileUrl()));
    }
}
