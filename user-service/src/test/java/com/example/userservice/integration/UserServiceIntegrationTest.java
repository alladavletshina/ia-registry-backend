package com.example.userservice.integration;

import com.example.userservice.TestSecurityConfig;
import com.example.userservice.dto.request.RegisterRequestDto;
import com.example.userservice.dto.response.UserResponseDto;
import com.example.userservice.service.KeycloakAdminClient;
import com.example.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"
})
@Import(TestSecurityConfig.class)
class UserServiceIntegrationTest {

    @MockBean
    private KeycloakAdminClient keycloakAdminClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserService userService;

    @Test
    void register_shouldSaveUserInDatabase() {
        String keycloakUuid = java.util.UUID.randomUUID().toString();
        when(keycloakAdminClient.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(keycloakUuid);
        when(keycloakAdminClient.verifyPassword(anyString(), anyString())).thenReturn(true);

        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("integ@example.com");
        request.setPassword("Password123!");
        request.setFirstName("Integration");
        request.setLastName("Test");
        request.setPhone("123456789");
        request.setPosition("QA");
        request.setDepartment("Testing");

        UserResponseDto result = userService.register(request, "127.0.0.1");

        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo("integ@example.com");
        assertThat(result.getFirstName()).isEqualTo("Integration");
    }
}