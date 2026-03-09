package com.example.reportservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class UsersReportDTO {
    private List<RoleActivity> activityByRole;
    private List<DayActivity> dailyActivity;
    private List<UserActivity> topUsers;

    @Data
    public static class RoleActivity {
        private String name;
        private int logins;
        private int actions;
    }

    @Data
    public static class DayActivity {
        private String day;
        private int logins;
        private int actions;
    }

    @Data
    public static class UserActivity {
        private String name;
        private int actions;
        private String lastLogin;
    }
}