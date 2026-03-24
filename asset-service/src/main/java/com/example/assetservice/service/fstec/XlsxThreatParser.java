package com.example.assetservice.service.fstec;

import com.example.assetservice.model.entity.Threat;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("xlsxThreatParser")
public class XlsxThreatParser implements FstecThreatParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public List<Threat> parse(InputStream inputStream) throws Exception {
        List<Threat> threats = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Первая и 2-я строка — заголовок, данные начинаются с 3 строки (индекс 2)
            int rowStart = 2;
            for (int i = rowStart; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Threat threat = new Threat();
                Cell idCell = row.getCell(0);
                if (idCell != null) {
                    if (idCell.getCellType() == CellType.NUMERIC) {
                        threat.setId((long) idCell.getNumericCellValue());
                    } else {
                        String idStr = getStringCellValue(idCell);
                        threat.setId(Long.parseLong(idStr));
                    }
                } else {
                    threat.setId(null); // или пропустить строку
                }         // Идентификатор
                threat.setName(getStringCellValue(row.getCell(1)));        // Наименование
                threat.setDescription(getStringCellValue(row.getCell(2))); // Описание
                threat.setSource(getStringCellValue(row.getCell(3)));      // Источник
                threat.setObjectAffected(getStringCellValue(row.getCell(4))); // Объект воздействия
                threat.setConfidentiality(getBooleanCellValue(row.getCell(5))); // Конф.
                threat.setIntegrity(getBooleanCellValue(row.getCell(6)));  // Целостность
                threat.setAvailability(getBooleanCellValue(row.getCell(7))); // Доступность
                threat.setInclusionDate(getDateCellValue(row.getCell(8))); // Дата включения
                threat.setLastModified(getDateCellValue(row.getCell(9)));  // Дата изменения
                threat.setStatus(getStringCellValue(row.getCell(10)));     // Статус
                threat.setNotes(getStringCellValue(row.getCell(11)));      // Замечания
                threat.setSyncedAt(LocalDate.now());

                threats.add(threat);
            }
        }
        log.info("XLSX парсер завершил работу, найдено {} угроз", threats.size());
        return threats;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // если число записано как текст, оно может быть числовым
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return "";
        }
    }

    private boolean getBooleanCellValue(Cell cell) {
        if (cell == null) return false;
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue() != 0;
            case STRING:
                String val = cell.getStringCellValue().trim();
                return "1".equals(val) || "да".equalsIgnoreCase(val);
            default:
                return false;
        }
    }

    private LocalDate getDateCellValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            // если дата в виде строки
            String str = getStringCellValue(cell);
            if (!str.isEmpty()) {
                return LocalDate.parse(str, DATE_FORMATTER);
            }
        } catch (Exception e) {
            log.warn("Не удалось распарсить дату в ячейке: {}", e.getMessage());
        }
        return null;
    }
}
