package com.example.assetservice.service.fstec;

import com.example.assetservice.model.entity.Threat;
import lombok.extern.slf4j.Slf4j;
import org.odftoolkit.odfdom.doc.OdfSpreadsheetDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("odsThreatParser")
public class OdsThreatParser implements FstecThreatParser {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public List<Threat> parse(InputStream inputStream) throws Exception {
        List<Threat> threats = new ArrayList<>();
        OdfSpreadsheetDocument document = (OdfSpreadsheetDocument) OdfSpreadsheetDocument.loadDocument(inputStream);
        OdfTable table = document.getTableList().get(0); // первый лист

        // Пропускаем заголовок (первая строка)
        for (int i = 2; i < table.getRowCount(); i++) {
            OdfTableRow row = table.getRowByIndex(i);
            if (row == null) continue;

            Threat threat = new Threat();
            threat.setId(getCellValue(row.getCellByIndex(0)));
            threat.setName(getCellValue(row.getCellByIndex(1)));
            threat.setDescription(getCellValue(row.getCellByIndex(2)));
            threat.setSource(getCellValue(row.getCellByIndex(3)));
            threat.setObjectAffected(getCellValue(row.getCellByIndex(4)));
            threat.setConfidentiality(getBooleanCellValue(row.getCellByIndex(5)));
            threat.setIntegrity(getBooleanCellValue(row.getCellByIndex(6)));
            threat.setAvailability(getBooleanCellValue(row.getCellByIndex(7)));
            threat.setInclusionDate(getDateCellValue(row.getCellByIndex(8)));
            threat.setLastModified(getDateCellValue(row.getCellByIndex(9)));
            threat.setStatus(getCellValue(row.getCellByIndex(10)));
            threat.setNotes(getCellValue(row.getCellByIndex(11)));
            threat.setSyncedAt(LocalDate.now());

            threats.add(threat);
        }
        document.close();
        log.info("ODS парсер завершил работу, найдено {} угроз", threats.size());
        return threats;
    }

    private String getCellValue(OdfTableCell cell) {
        if (cell == null) return "";
        return cell.getStringValue().trim();
    }

    private boolean getBooleanCellValue(OdfTableCell cell) {
        String val = getCellValue(cell);
        return "1".equals(val) || "да".equalsIgnoreCase(val);
    }

    private LocalDate getDateCellValue(OdfTableCell cell) {
        String val = getCellValue(cell);
        if (val.isEmpty()) return null;
        try {
            return LocalDate.parse(val, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("Не удалось распарсить дату: {}", val);
            return null;
        }
    }
}