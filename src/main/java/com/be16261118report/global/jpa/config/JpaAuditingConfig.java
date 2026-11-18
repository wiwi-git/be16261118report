package com.be16261118report.global.jpa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// BaseEntity의 @CreatedDate, @LastModifiedDate를 동작시키기 위한 설정
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
