package com.be16261118report.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

// dev 환경 설정이 .env 파일의 DB 정보를 읽어 datasource를 구성하는지 검증
// 실제 DB 연결 없이 설정 값만 확인하기 위해 자동 설정 없는 빈 컨텍스트로 띄운다.
// ENV_FILE로 테스트 전용 .env(src/test/resources/env/dev-test.env)를 지정한다.
class DevEnvConfigTest {

    @Configuration
    static class EmptyConfig {
    }

    @Test
    @DisplayName("dev 프로필은 .env 파일의 DB 정보로 datasource 설정을 만든다")
    void readsDbInfoFromEnvFile() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(EmptyConfig.class)
                .web(WebApplicationType.NONE)
                .profiles("dev")
                .properties("ENV_FILE=src/test/resources/env/dev-test.env")
                .run()) {

            Environment env = context.getEnvironment();

            assertThat(env.getProperty("spring.datasource.url"))
                    .startsWith("jdbc:mysql://test-host:3307/test_db");
            assertThat(env.getProperty("spring.datasource.username")).isEqualTo("test_user");
            assertThat(env.getProperty("spring.datasource.password")).isEqualTo("test_password");
            assertThat(env.getProperty("spring.datasource.driver-class-name")).isEqualTo("com.mysql.cj.jdbc.Driver");
        }
    }
}
