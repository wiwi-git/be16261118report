package com.be16261118report;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class Be16261118reportApplicationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("test 프로필로 애플리케이션 컨텍스트가 뜬다")
    void contextLoads() {
        assertThat(environment.getActiveProfiles()).containsExactly("test");
    }

    @Test
    @DisplayName("test 환경은 H2 인메모리 DB를 사용한다")
    void usesH2InMemoryDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            assertThat(url).startsWith("jdbc:h2:mem:");
        }
    }

    @Test
    @DisplayName("JPA가 설정되어 EntityManager를 사용할 수 있다")
    void jpaIsConfigured() {
        assertThat(entityManager).isNotNull();
        assertThat(entityManager.isOpen()).isTrue();
    }

    @Test
    @DisplayName("테스트는 트랜잭션 안에서 동작한다")
    void runsInTransaction() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
    }
}
