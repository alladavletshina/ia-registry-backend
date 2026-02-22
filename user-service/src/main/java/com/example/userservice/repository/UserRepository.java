package com.example.userservice.repository;

import com.example.userservice.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

}
