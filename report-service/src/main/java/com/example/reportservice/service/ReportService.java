package com.example.reportservice.service;

import com.example.reportservice.client.*;
import com.example.reportservice.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserServiceClient userClient;
    private final AssetServiceClient assetClient;
    private final TaskServiceClient taskClient;
    private final AuditServiceClient auditClient;

    @Cacheable(value = "overview", key = "#period")
    public OverviewReportDTO getOverviewReport(String period) {
        log.info("Generating overview report for period: {}", period);
        List<AssetDTO> assets = assetClient.getAllAssets();
        List<UserDTO> users = userClient.getAllUsers();
        TaskStatsDTO taskStats = taskClient.getTaskStats();

        OverviewReportDTO report = new OverviewReportDTO();
        report.setTotalAssets(assets.size());
        report.setTotalUsers(users.size());
        report.setPendingReviews((int) taskStats.getPending());
        report.setHighRiskAssets(calculateHighRiskAssets(assets));

        report.setCategoryDistribution(buildCategoryDistribution(assets));
        report.setCiaDistribution(buildCiaDistribution(assets));
        return report;
    }

    @Cacheable(value = "assets", key = "#period")
    public AssetsReportDTO getAssetsReport(String period) {
        List<AssetDTO> assets = assetClient.getAllAssets();
        AssetsReportDTO report = new AssetsReportDTO();

        report.setByCategory(groupByCategory(assets));
        report.setByStatus(groupByStatus(assets));
        report.setByConfidentiality(groupByConfidentiality(assets));
        report.setGrowthTrend(buildGrowthTrend(assets, period));
        return report;
    }

    @Cacheable(value = "users", key = "#period")
    public UsersReportDTO getUsersReport(String period) {
        List<UserDTO> users = userClient.getAllUsers();
        // В реальности нужно больше данных (логины, действия). Для демо используем заглушки.
        UsersReportDTO report = new UsersReportDTO();
        report.setActivityByRole(buildRoleActivity(users));
        report.setDailyActivity(buildDailyActivity());
        report.setTopUsers(buildTopUsers(users));
        return report;
    }

    @Cacheable(value = "security", key = "#period")
    public SecurityReportDTO getSecurityReport(String period) {
        List<AuditEventDTO> events = auditClient.getAuditEvents(null, null);
        SecurityReportDTO report = new SecurityReportDTO();
        report.setRiskDistribution(buildRiskDistribution(events));
        report.setAuditEvents(buildAuditEventsTimeline(events));
        report.setComplianceStatus(buildComplianceStatus());
        return report;
    }

    @Cacheable(value = "performance", key = "#period")
    public PerformanceReportDTO getPerformanceReport(String period) {
        // Для демо возвращаем фиксированные данные
        PerformanceReportDTO report = new PerformanceReportDTO();
        report.setUptime(99.8);
        report.setAvgResponseTime(172);
        report.setPeakLoad(2450);
        report.setErrors(Map.of("400", 12, "401", 5, "404", 8, "500", 2));
        report.setResponseTimes(List.of(
                createHourValue("00:00", 142),
                createHourValue("04:00", 138),
                createHourValue("08:00", 156),
                createHourValue("12:00", 234),
                createHourValue("16:00", 198),
                createHourValue("20:00", 167)
        ));
        return report;
    }

    // ================== Вспомогательные методы ==================

    private int calculateHighRiskAssets(List<AssetDTO> assets) {
        return (int) assets.stream()
                .filter(a -> "HIGH".equalsIgnoreCase(a.getConfidentiality()) ||
                        "HIGH".equalsIgnoreCase(a.getIntegrity()) ||
                        "HIGH".equalsIgnoreCase(a.getAvailability()))
                .count();
    }

    private List<OverviewReportDTO.CategoryCount> buildCategoryDistribution(List<AssetDTO> assets) {
        Map<String, Long> counts = assets.stream()
                .collect(Collectors.groupingBy(AssetDTO::getCategory, Collectors.counting()));
        List<OverviewReportDTO.CategoryCount> result = new ArrayList<>();
        counts.forEach((name, count) -> {
            OverviewReportDTO.CategoryCount cc = new OverviewReportDTO.CategoryCount();
            cc.setName(name != null ? name : "Без категории");
            cc.setValue(count.intValue());
            cc.setColor(getColorForCategory(name));
            result.add(cc);
        });
        return result;
    }

    private List<OverviewReportDTO.CiaAvg> buildCiaDistribution(List<AssetDTO> assets) {
        double avgConf = assets.stream().mapToInt(a -> levelToInt(a.getConfidentiality())).average().orElse(0);
        double avgInt = assets.stream().mapToInt(a -> levelToInt(a.getIntegrity())).average().orElse(0);
        double avgAvail = assets.stream().mapToInt(a -> levelToInt(a.getAvailability())).average().orElse(0);

        return List.of(
                createCiaAvg("Конфиденциальность", avgConf, "#ef4444"),
                createCiaAvg("Целостность", avgInt, "#f59e0b"),
                createCiaAvg("Доступность", avgAvail, "#10b981")
        );
    }

    private int levelToInt(String level) {
        if (level == null) return 0;
        return switch (level.toUpperCase()) {
            case "LOW" -> 1;
            case "MEDIUM" -> 2;
            case "HIGH" -> 3;
            case "CRITICAL" -> 4;
            default -> 0;
        };
    }

    private OverviewReportDTO.CiaAvg createCiaAvg(String name, double value, String color) {
        OverviewReportDTO.CiaAvg cia = new OverviewReportDTO.CiaAvg();
        cia.setName(name);
        cia.setValue(Math.round(value * 10) / 10.0);
        cia.setColor(color);
        return cia;
    }

    private List<AssetsReportDTO.CategoryCount> groupByCategory(List<AssetDTO> assets) {
        Map<String, Long> map = assets.stream()
                .collect(Collectors.groupingBy(AssetDTO::getCategory, Collectors.counting()));
        List<AssetsReportDTO.CategoryCount> list = new ArrayList<>();
        map.forEach((name, count) -> {
            AssetsReportDTO.CategoryCount cc = new AssetsReportDTO.CategoryCount();
            cc.setName(name != null ? name : "Без категории");
            cc.setValue(count.intValue());
            cc.setColor(getColorForCategory(name));
            list.add(cc);
        });
        return list;
    }

    private List<AssetsReportDTO.StatusCount> groupByStatus(List<AssetDTO> assets) {
        Map<String, Long> map = assets.stream()
                .collect(Collectors.groupingBy(AssetDTO::getStatus, Collectors.counting()));
        List<AssetsReportDTO.StatusCount> list = new ArrayList<>();
        map.forEach((status, count) -> {
            AssetsReportDTO.StatusCount sc = new AssetsReportDTO.StatusCount();
            sc.setName(status != null ? status : "Неизвестно");
            sc.setValue(count.intValue());
            sc.setColor(getColorForStatus(status));
            list.add(sc);
        });
        return list;
    }

    private List<AssetsReportDTO.LevelCount> groupByConfidentiality(List<AssetDTO> assets) {
        Map<String, Long> map = assets.stream()
                .collect(Collectors.groupingBy(AssetDTO::getConfidentiality, Collectors.counting()));
        List<AssetsReportDTO.LevelCount> list = new ArrayList<>();
        map.forEach((level, count) -> {
            AssetsReportDTO.LevelCount lc = new AssetsReportDTO.LevelCount();
            lc.setName(level != null ? level : "Не указано");
            lc.setValue(count.intValue());
            lc.setColor(getColorForLevel(level));
            list.add(lc);
        });
        return list;
    }

    private List<AssetsReportDTO.MonthValue> buildGrowthTrend(List<AssetDTO> assets, String period) {
        // Для демо генерируем последние 7 месяцев
        List<AssetsReportDTO.MonthValue> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM", new Locale("ru"));
        for (int i = 6; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthName = month.format(fmt);
            int count = (int) (Math.random() * 30 + 100); // заглушка
            AssetsReportDTO.MonthValue mv = new AssetsReportDTO.MonthValue();
            mv.setMonth(monthName);
            mv.setValue(count);
            trend.add(mv);
        }
        return trend;
    }

    private List<UsersReportDTO.RoleActivity> buildRoleActivity(List<UserDTO> users) {
        // Заглушка
        return List.of(
                createRoleActivity("Администраторы", 245, 1204),
                createRoleActivity("Пользователи", 1542, 3248),
                createRoleActivity("Аудиторы", 86, 412)
        );
    }

    private UsersReportDTO.RoleActivity createRoleActivity(String name, int logins, int actions) {
        UsersReportDTO.RoleActivity ra = new UsersReportDTO.RoleActivity();
        ra.setName(name);
        ra.setLogins(logins);
        ra.setActions(actions);
        return ra;
    }

    private List<UsersReportDTO.DayActivity> buildDailyActivity() {
        return List.of(
                createDayActivity("Пн", 145, 324),
                createDayActivity("Вт", 162, 368),
                createDayActivity("Ср", 178, 412),
                createDayActivity("Чт", 154, 356),
                createDayActivity("Пт", 132, 298),
                createDayActivity("Сб", 48, 86),
                createDayActivity("Вс", 32, 54)
        );
    }

    private UsersReportDTO.DayActivity createDayActivity(String day, int logins, int actions) {
        UsersReportDTO.DayActivity da = new UsersReportDTO.DayActivity();
        da.setDay(day);
        da.setLogins(logins);
        da.setActions(actions);
        return da;
    }

    private List<UsersReportDTO.UserActivity> buildTopUsers(List<UserDTO> users) {
        // Заглушка
        return List.of(
                createUserActivity("Иванов И.И.", 324, "Сегодня"),
                createUserActivity("Петрова А.С.", 298, "Вчера"),
                createUserActivity("Сидоров В.П.", 256, "2 дня назад"),
                createUserActivity("Кузнецов Д.А.", 198, "Сегодня"),
                createUserActivity("Смирнова О.И.", 176, "3 дня назад")
        );
    }

    private UsersReportDTO.UserActivity createUserActivity(String name, int actions, String lastLogin) {
        UsersReportDTO.UserActivity ua = new UsersReportDTO.UserActivity();
        ua.setName(name);
        ua.setActions(actions);
        ua.setLastLogin(lastLogin);
        return ua;
    }

    private List<SecurityReportDTO.RiskCount> buildRiskDistribution(List<AuditEventDTO> events) {
        // Заглушка
        return List.of(
                createRiskCount("Высокий риск", 12, "#ef4444"),
                createRiskCount("Средний риск", 34, "#f59e0b"),
                createRiskCount("Низкий риск", 89, "#10b981"),
                createRiskCount("Информационный", 21, "#3b82f6")
        );
    }

    private SecurityReportDTO.RiskCount createRiskCount(String name, int value, String color) {
        SecurityReportDTO.RiskCount rc = new SecurityReportDTO.RiskCount();
        rc.setName(name);
        rc.setValue(value);
        rc.setColor(color);
        return rc;
    }

    private List<SecurityReportDTO.DateCount> buildAuditEventsTimeline(List<AuditEventDTO> events) {
        // Заглушка
        return List.of(
                createDateCount("01.01", 45),
                createDateCount("08.01", 52),
                createDateCount("15.01", 48),
                createDateCount("22.01", 56),
                createDateCount("29.01", 62)
        );
    }

    private SecurityReportDTO.DateCount createDateCount(String date, int value) {
        SecurityReportDTO.DateCount dc = new SecurityReportDTO.DateCount();
        dc.setDate(date);
        dc.setValue(value);
        return dc;
    }

    private Map<String, Integer> buildComplianceStatus() {
        return Map.of(
                "iso27001", 85,
                "gdpr", 92,
                "sox", 78,
                "pciDss", 65
        );
    }

    private PerformanceReportDTO.HourValue createHourValue(String hour, int value) {
        PerformanceReportDTO.HourValue hv = new PerformanceReportDTO.HourValue();
        hv.setHour(hour);
        hv.setValue(value);
        return hv;
    }

    private String getColorForCategory(String category) {
        if (category == null) return "#8884d8";
        return switch (category.toLowerCase()) {
            case "database" -> "#8884d8";
            case "documentation" -> "#82ca9d";
            case "software" -> "#ffc658";
            default -> "#3b82f6";
        };
    }

    private String getColorForStatus(String status) {
        if (status == null) return "#94a3b8";
        return switch (status.toUpperCase()) {
            case "ACTIVE" -> "#10b981";
            case "NEEDS_REVIEW" -> "#f59e0b";
            case "ARCHIVED" -> "#64748b";
            default -> "#3b82f6";
        };
    }

    private String getColorForLevel(String level) {
        if (level == null) return "#94a3b8";
        return switch (level.toUpperCase()) {
            case "CRITICAL" -> "#ef4444";
            case "HIGH" -> "#f97316";
            case "MEDIUM" -> "#eab308";
            case "LOW" -> "#22c55e";
            default -> "#94a3b8";
        };
    }
}