package com.example.reportservice.client;

import com.example.reportservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {
    @GetMapping
    List<UserDTO> getAllUsers();
}