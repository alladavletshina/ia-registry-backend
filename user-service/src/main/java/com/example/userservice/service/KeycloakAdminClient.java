package com.example.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminClient {

    private final Keycloak keycloak;

    @Value("${keycloak.target-realm}")
    private String targetRealm;

    public String createUser(String email, String password, String firstName, String lastName){
        try {
            RealmResource realmResource = keycloak.realm(targetRealm);
            UsersResource usersResource = realmResource.users();

            UserRepresentation user = new UserRepresentation();
            user.setUsername(email);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(true);

            //добавлю позже верификацию через почту
            //user.setRequiredActions(Collections.singletonList("VERIFY_EMAIL"));

            //Создаем пользователя
            ObjectMapper objectMapper = new ObjectMapper();
            log.info("Sending to Keycloak: {}", objectMapper.writeValueAsString(user));

            Response response = usersResource.create(user);

            log.info("Response status: {}, body: {}", response.getStatus(),
                    response.readEntity(String.class));

            if(response.getStatus() != 201) {
                String errorBody = response.readEntity(String.class);
                log.error("Ошибка создания пользователя в Keycloak. Статус: {}, Ответ: {}",
                        response.getStatus(), errorBody);
                throw new RuntimeException("Ошибка создания пользователя в Keycloak: " + response.getStatus());
            }

            //Получаем ID созданного пользователя из Location header
            String userId = extractUserId(response);
            log.info("Пользователь создан в Keycloak с ID: {}", userId);

            //Устанавливаем пароль
            setUserPassword(userId, password);

            //Отправляем письмо с подтверждением email
            //usersResource.get(userId).sendVerifyEmail();

            return userId;
        } catch (Exception e) {
            log.error("Ошибка при создании пользователя в Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось создать пользователя в Keycloak: " + e.getMessage());
        }
    }

    public void updateUser(String userId, String firstName, String lastName, String email) {
        try {
            RealmResource realmResource = keycloak.realm(targetRealm);
            UsersResource usersResource = realmResource.users();

            UserRepresentation user = usersResource.get(userId).toRepresentation();
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);

            usersResource.get(userId).update(user);
            log.info("Пользователь {} обновлен в Keycloak", userId);

        } catch (Exception e) {
            log.error("Ошибка обновления пользователя в Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обновить пользователя в Keycloak");
        }
    }

    public void setUserPassword(String userId, String password){
        try{
            RealmResource realmResource = keycloak.realm(targetRealm);
            UsersResource usersResource = realmResource.users();

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(password);
            credential.setTemporary(false);

            usersResource.get(userId).resetPassword(credential);
            log.info("Пароль установлен для пользователя {}", userId);
        } catch (Exception e) {
            log.error("Ошибка установки пароля в Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось установить пароль пользователя");
        }
    }

    public boolean userExists(String email) {
        try {
            RealmResource realmResource = keycloak.realm(targetRealm);
            UsersResource usersResource = realmResource.users();

            return usersResource.search(email, true).size() > 0;

        } catch (Exception e) {
            log.error("Ошибка проверки существования пользователя: {}", e.getMessage(), e);
            return false;
        }
    }

    private String extractUserId(Response response) {
        String location = response.getLocation().getPath();
        return location.substring(location.lastIndexOf('/') + 1);
    }

    public void deleteUser(String userId) {
        try {
            RealmResource realmResource = keycloak.realm(targetRealm);
            UsersResource usersResource = realmResource.users();

            usersResource.get(userId).remove();
            log.info("Пользователь {} удален из Keycloak", userId);

        } catch (Exception e) {
            log.error("Ошибка удаления пользователя из Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось удалить пользователя из Keycloak");
        }
    }

    /**
     * Создаёт роль в realm, если её ещё нет.
     */
    public void createRealmRole(String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(targetRealm);
            RoleRepresentation role = new RoleRepresentation(roleName, null, false);
            realmResource.roles().create(role);
            log.info("Роль '{}' создана в realm '{}'", roleName, targetRealm);
        } catch (Exception e) {
            log.warn("Роль '{}' уже существует или не может быть создана: {}", roleName, e.getMessage());
        }
    }

    /**
     * Назначает роль пользователю.
     */
    public void assignRealmRole(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(targetRealm);
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            realmResource.users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
            log.info("Роль '{}' назначена пользователю {}", roleName, userId);
        } catch (Exception e) {
            log.error("Ошибка назначения роли '{}' пользователю {}: {}", roleName, userId, e.getMessage(), e);
            throw new RuntimeException("Не удалось назначить роль пользователю");
        }
    }
}
