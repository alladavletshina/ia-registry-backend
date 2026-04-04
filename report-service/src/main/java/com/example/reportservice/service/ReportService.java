package com.example.reportservice.service;

import com.example.reportservice.client.*;
import com.example.reportservice.config.PerformanceProperties;
import com.example.reportservice.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
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
    private final RestTemplate restTemplate;
    private final PerformanceProperties performanceProperties;

    @Cacheable(value = "overview", key = "#period")
    public OverviewReportDTO getOverviewReport(String period) {
        log.info("Generating overview report for period: {}", period);
        List<AssetDTO> assets = Collections.emptyList();
        List<UserDTO> users = Collections.emptyList();
        TaskStatsDTO taskStats = new TaskStatsDTO();
        try {
            assets = assetClient.getAllAssets();
        } catch (Exception e) {
            log.error("Failed to fetch assets: {}", e.getMessage(), e);
        }
        try {
            users = userClient.getAllUsers();
        } catch (Exception e) {
            log.error("Failed to fetch users: {}", e.getMessage(), e);
        }
        try {
            taskStats = taskClient.getTaskStats();
        } catch (Exception e) {
            log.error("Failed to fetch task stats: {}", e.getMessage(), e);
        }

        OverviewReportDTO report = new OverviewReportDTO();
        report.setTotalAssets(assets.size());
        report.setTotalUsers(users.size());
        report.setPendingReviews((int) taskStats.getPending());
        report.setHighRiskAssets(calculateHighRiskAssets(assets));
        report.setCategoryDistribution(buildGroupDistribution(assets));
        report.setCiaDistribution(buildCiaDistribution(assets));
        return report;
    }

    @Cacheable(value = "assets", key = "#period")
    public AssetsReportDTO getAssetsReport(String period) {
        List<AssetDTO> assets = Collections.emptyList();
        try {
            assets = assetClient.getAllAssets();
        } catch (Exception e) {
            log.error("Failed to fetch assets for report: {}", e.getMessage(), e);
        }
        AssetsReportDTO report = new AssetsReportDTO();
        report.setByCategory(groupByGroupName(assets));
        report.setByStatus(groupByStatus(assets));
        report.setByConfidentiality(groupByConfidentiality(assets));
        report.setGrowthTrend(buildGrowthTrend(assets, period));
        return report;
    }

    @Cacheable(value = "users", key = "#period")
    public UsersReportDTO getUsersReport(String period) {
        List<AuditEventDTO> events = Collections.emptyList();
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = calculateStartDate(period, endDate);
            events = auditClient.getReportData(startDate.toString(), endDate.toString());
            log.info("Fetched {} audit events for users report", events.size());
        } catch (Exception e) {
            log.error("Failed to fetch audit events for users report: {}", e.getMessage(), e);
        }

        UsersReportDTO report = new UsersReportDTO();
        report.setActivityByRole(buildRoleActivity(events));
        report.setDailyActivity(buildDailyActivity(events));
        report.setTopUsers(buildTopUsers(events));
        return report;
    }

    @Cacheable(value = "security", key = "#period")
    public SecurityReportDTO getSecurityReport(String period) {
        log.info("Generating security report for period: {}", period);
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = calculateStartDate(period, endDate);

            log.debug("Calling audit-service with dates: {} - {}", startDate, endDate);
            List<AuditEventDTO> events = auditClient.getReportData(startDate.toString(), endDate.toString());
            log.debug("Received {} events from audit-service", events.size());

            SecurityReportDTO report = new SecurityReportDTO();
            report.setRiskDistribution(buildRiskDistribution(events));
            report.setAuditEvents(buildAuditEventsTimeline(events));
            report.setComplianceStatus(buildComplianceStatus());
            return report;
        } catch (Exception e) {
            log.error("Error in getSecurityReport", e);
            SecurityReportDTO fallback = new SecurityReportDTO();
            fallback.setRiskDistribution(List.of(
                    createRiskCount("Высокий риск", 0, "#ef4444"),
                    createRiskCount("Средний риск", 0, "#f59e0b"),
                    createRiskCount("Низкий риск", 0, "#10b981"),
                    createRiskCount("Информационный", 0, "#3b82f6")
            ));
            fallback.setAuditEvents(Collections.emptyList());
            fallback.setComplianceStatus(buildComplianceStatus());
            return fallback;
        }
    }

    private LocalDate calculateStartDate(String period, LocalDate endDate) {
        return switch (period) {
            case "week" -> endDate.minusWeeks(1);
            case "month" -> endDate.minusMonths(1);
            case "quarter" -> endDate.minusMonths(3);
            case "year" -> endDate.minusYears(1);
            default -> endDate.minusMonths(1);
        };
    }

    // ================== Методы для отчёта Performance (реальные метрики) ==================

    @Cacheable(value = "performance", key = "#period")
    public PerformanceReportDTO getPerformanceReport(String period) {
        log.info("Generating performance report for period: {}", period);

        List<AuditEventDTO> events = Collections.emptyList();
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = calculateStartDate(period, endDate);
            events = auditClient.getReportData(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch audit events for performance report: {}", e.getMessage(), e);
        }

        List<ServiceHealthMetric> metrics = collectServiceMetrics();
        double uptime = calculateUptime(metrics);
        double avgResponseTime = calculateAverageResponseTime(metrics);
        Map<String, Integer> errors = collectErrorsFromAudit(events);

        PerformanceReportDTO report = new PerformanceReportDTO();
        report.setUptime(uptime);
        report.setAvgResponseTime((int) avgResponseTime);
        report.setErrors(errors);
        return report;
    }

    private List<ServiceHealthMetric> collectServiceMetrics() {
        List<ServiceHealthMetric> metrics = new ArrayList<>();
        for (PerformanceProperties.ServiceConfig service : performanceProperties.getServices()) {
            long startTime = System.currentTimeMillis();
            boolean up = false;
            try {
                var response = restTemplate.getForEntity(service.getUrl(), String.class);
                up = response.getStatusCode().is2xxSuccessful();
            } catch (Exception e) {
                log.warn("Service {} is not reachable: {}", service.getName(), e.getMessage());
            }
            long responseTime = System.currentTimeMillis() - startTime;
            metrics.add(new ServiceHealthMetric(service.getName(), up, responseTime));
        }
        return metrics;
    }

    private double calculateUptime(List<ServiceHealthMetric> metrics) {
        if (metrics.isEmpty()) return 0.0;
        long upCount = metrics.stream().filter(ServiceHealthMetric::isUp).count();
        return (double) upCount / metrics.size() * 100;
    }

    private double calculateAverageResponseTime(List<ServiceHealthMetric> metrics) {
        return metrics.stream()
                .filter(ServiceHealthMetric::isUp)
                .mapToLong(ServiceHealthMetric::getResponseTime)
                .average()
                .orElse(0);
    }

    private Map<String, Integer> collectErrorsFromAudit(List<AuditEventDTO> events) {
        Map<String, Integer> errorCounts = new HashMap<>();
        for (AuditEventDTO event : events) {
            if ("DANGER".equals(event.getSeverity())) {
                String key = event.getAction();
                errorCounts.merge(key, 1, Integer::sum);
            }
        }
        return errorCounts;
    }

    private static class ServiceHealthMetric {
        private final String name;
        private final boolean up;
        private final long responseTime;

        public ServiceHealthMetric(String name, boolean up, long responseTime) {
            this.name = name;
            this.up = up;
            this.responseTime = responseTime;
        }
        public boolean isUp() { return up; }
        public long getResponseTime() { return responseTime; }
    }

    // ================== Остальные вспомогательные методы ==================

    private int calculateHighRiskAssets(List<AssetDTO> assets) {
        return (int) assets.stream()
                .filter(a -> "HIGH".equalsIgnoreCase(a.getConfidentiality()) ||
                        "HIGH".equalsIgnoreCase(a.getIntegrity()) ||
                        "HIGH".equalsIgnoreCase(a.getAvailability()))
                .count();
    }

    private List<OverviewReportDTO.CategoryCount> buildGroupDistribution(List<AssetDTO> assets) {
        Map<String, Long> counts = assets.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getGroupName() != null ? a.getGroupName() : "Без группы",
                        Collectors.counting()
                ));
        List<OverviewReportDTO.CategoryCount> result = new ArrayList<>();
        counts.forEach((name, count) -> {
            OverviewReportDTO.CategoryCount cc = new OverviewReportDTO.CategoryCount();
            cc.setName(name);
            cc.setValue(count.intValue());
            cc.setColor(getColorForGroupName(name));
            result.add(cc);
        });
        return result;
    }

    private List<AssetsReportDTO.CategoryCount> groupByGroupName(List<AssetDTO> assets) {
        Map<String, Long> map = assets.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getGroupName() != null ? a.getGroupName() : "Без группы",
                        Collectors.counting()
                ));
        List<AssetsReportDTO.CategoryCount> list = new ArrayList<>();
        map.forEach((name, count) -> {
            AssetsReportDTO.CategoryCount cc = new AssetsReportDTO.CategoryCount();
            cc.setName(name);
            cc.setValue(count.intValue());
            cc.setColor(getColorForGroupName(name));
            list.add(cc);
        });
        return list;
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
        if (assets == null || assets.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(period, endDate);
        log.debug("Growth trend period: {} - {}", startDate, endDate);

        Map<YearMonth, Long> groupedByMonth = assets.stream()
                .filter(asset -> asset.getCreatedAt() != null)
                .map(asset -> asset.getCreatedAt().toLocalDate())
                .filter(date -> !date.isBefore(startDate) && !date.isAfter(endDate))
                .collect(Collectors.groupingBy(YearMonth::from, Collectors.counting()));

        List<YearMonth> sortedMonths = groupedByMonth.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", new Locale("ru"));
        List<AssetsReportDTO.MonthValue> trend = new ArrayList<>();
        for (YearMonth ym : sortedMonths) {
            AssetsReportDTO.MonthValue mv = new AssetsReportDTO.MonthValue();
            mv.setMonth(ym.format(monthFormatter));
            mv.setValue(groupedByMonth.get(ym).intValue());
            trend.add(mv);
        }

        return trend;
    }

    // ================== Методы для отчёта Users (на основе аудита) ==================

    private List<UsersReportDTO.RoleActivity> buildRoleActivity(List<AuditEventDTO> events) {
        if (events == null || events.isEmpty()) {
            return List.of(
                    createRoleActivity("Администраторы", 0, 0),
                    createRoleActivity("Пользователи", 0, 0)
            );
        }

        long adminLogins = 0;
        long adminActions = 0;
        long userLogins = 0;
        long userActions = 0;

        for (AuditEventDTO event : events) {
            boolean isAdmin = "admin".equals(event.getUsername());
            if ("LOGIN".equals(event.getAction())) {
                if (isAdmin) adminLogins++;
                else userLogins++;
            } else {
                if (isAdmin) adminActions++;
                else userActions++;
            }
        }

        return List.of(
                createRoleActivity("Администраторы", (int) adminLogins, (int) adminActions),
                createRoleActivity("Пользователи", (int) userLogins, (int) userActions)
        );
    }

    private List<UsersReportDTO.DayActivity> buildDailyActivity(List<AuditEventDTO> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, int[]> dayStats = new HashMap<>();
        for (int i = 1; i <= 7; i++) {
            dayStats.put(i, new int[]{0, 0});
        }

        for (AuditEventDTO event : events) {
            if (event.getTimestamp() == null) continue;
            LocalDate date = LocalDate.parse(event.getTimestamp().split("T")[0]);
            int dayOfWeek = date.getDayOfWeek().getValue();
            int[] stats = dayStats.get(dayOfWeek);
            if ("LOGIN".equals(event.getAction())) {
                stats[0]++;
            } else {
                stats[1]++;
            }
        }

        Map<Integer, String> dayNames = Map.of(
                1, "Пн", 2, "Вт", 3, "Ср", 4, "Чт", 5, "Пт", 6, "Сб", 7, "Вс"
        );
        List<UsersReportDTO.DayActivity> result = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            int[] stats = dayStats.get(i);
            result.add(createDayActivity(dayNames.get(i), stats[0], stats[1]));
        }
        return result;
    }

    private List<UsersReportDTO.UserActivity> buildTopUsers(List<AuditEventDTO> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> userActions = new HashMap<>();
        for (AuditEventDTO event : events) {
            String username = event.getUsername();
            if (username == null) continue;
            userActions.merge(username, 1, Integer::sum);
        }

        return userActions.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> createUserActivity(entry.getKey(), entry.getValue(), null))
                .collect(Collectors.toList());
    }

    // ================== Вспомогательные методы для создания DTO ==================

    private UsersReportDTO.RoleActivity createRoleActivity(String name, int logins, int actions) {
        UsersReportDTO.RoleActivity ra = new UsersReportDTO.RoleActivity();
        ra.setName(name);
        ra.setLogins(logins);
        ra.setActions(actions);
        return ra;
    }

    private UsersReportDTO.DayActivity createDayActivity(String day, int logins, int actions) {
        UsersReportDTO.DayActivity da = new UsersReportDTO.DayActivity();
        da.setDay(day);
        da.setLogins(logins);
        da.setActions(actions);
        return da;
    }

    private UsersReportDTO.UserActivity createUserActivity(String name, int actions, String lastLogin) {
        UsersReportDTO.UserActivity ua = new UsersReportDTO.UserActivity();
        ua.setName(name);
        ua.setActions(actions);
        ua.setLastLogin(lastLogin != null ? lastLogin : "");
        return ua;
    }

    // ================== Методы для отчёта Security ==================

    private List<SecurityReportDTO.RiskCount> buildRiskDistribution(List<AuditEventDTO> events) {
        Map<String, Long> counts = events.stream()
                .collect(Collectors.groupingBy(AuditEventDTO::getSeverity, Collectors.counting()));

        List<SecurityReportDTO.RiskCount> list = new ArrayList<>();

        Map<String, String[]> severityMapping = Map.of(
                "DANGER", new String[]{"Высокий риск", "#ef4444"},
                "WARNING", new String[]{"Средний риск", "#f59e0b"},
                "INFO", new String[]{"Низкий риск", "#10b981"},
                "SUCCESS", new String[]{"Информационный", "#3b82f6"}
        );

        severityMapping.forEach((sev, props) -> {
            long count = counts.getOrDefault(sev, 0L);
            SecurityReportDTO.RiskCount rc = new SecurityReportDTO.RiskCount();
            rc.setName(props[0]);
            rc.setValue((int) count);
            rc.setColor(props[1]);
            list.add(rc);
        });

        return list;
    }

    private SecurityReportDTO.RiskCount createRiskCount(String name, int value, String color) {
        SecurityReportDTO.RiskCount rc = new SecurityReportDTO.RiskCount();
        rc.setName(name);
        rc.setValue(value);
        rc.setColor(color);
        return rc;
    }

    private List<SecurityReportDTO.DateCount> buildAuditEventsTimeline(List<AuditEventDTO> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Long> groupedByDate = events.stream()
                .filter(e -> e.getTimestamp() != null && !e.getTimestamp().isEmpty())
                .collect(Collectors.groupingBy(
                        e -> e.getTimestamp().split("T")[0],
                        Collectors.counting()
                ));

        return groupedByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    SecurityReportDTO.DateCount dc = new SecurityReportDTO.DateCount();
                    dc.setDate(entry.getKey());
                    dc.setValue(entry.getValue().intValue());
                    return dc;
                })
                .collect(Collectors.toList());
    }

    private SecurityReportDTO.DateCount createDateCount(String date, int value) {
        SecurityReportDTO.DateCount dc = new SecurityReportDTO.DateCount();
        dc.setDate(date);
        dc.setValue(value);
        return dc;
    }

    private Map<String, Integer> buildComplianceStatus() {
        return Collections.emptyMap(); // больше не мок, возвращаем пустую мапу
    }

    // ================== Цветовые схемы ==================

    private String getColorForGroupName(String groupName) {
        if (groupName == null) return "#8884d8";
        return switch (groupName) {
            case "Аппаратные активы" -> "#8884d8";
            case "Программные активы" -> "#82ca9d";
            case "Информационные активы" -> "#ffc658";
            case "Сервисы" -> "#3b82f6";
            case "Без группы" -> "#94a3b8";
            default -> "#8884d8";
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