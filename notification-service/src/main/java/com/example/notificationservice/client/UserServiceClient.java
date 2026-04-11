package com.example.notificationservice.client;

import com.example.notificationservice.config.FeignConfig;
import com.example.notificationservice.model.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url = "${services.user-service.url}", configuration = FeignConfig.class)
public interface UserServiceClient {
    @GetMapping("/by-keycloak-id")
    UserDto getUserByKeycloakId(@RequestParam("keycloakId") String keycloakId);
}