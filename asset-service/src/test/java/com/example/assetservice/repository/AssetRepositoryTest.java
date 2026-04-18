package com.example.assetservice.repository;

import com.example.assetservice.model.Asset;
import com.example.assetservice.model.AssetStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=PostgreSQL;NON_KEYWORDS=VALUE"
})
class AssetRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AssetRepository assetRepository;

    @Test
    void shouldFindByNameContainingIgnoreCase() {
        Asset asset = new Asset();
        asset.setName("Server Database");
        asset.setStatus(AssetStatus.ACTIVE);
        entityManager.persist(asset);

        List<Asset> result = assetRepository.findByNameContainingIgnoreCase("server");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Server Database");
    }
}