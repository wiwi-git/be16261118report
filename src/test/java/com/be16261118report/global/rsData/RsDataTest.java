package com.be16261118report.global.rsData;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RsDataTest {

    @Test
    @DisplayName("resultCode의 '-' 앞 숫자를 HTTP 상태 코드로 파싱한다")
    void parseStatusCode() {
        assertThat(new RsData<>("404-1", "없음").statusCode()).isEqualTo(404);
        assertThat(new RsData<>("201-1", "생성", 1L).statusCode()).isEqualTo(201);
        assertThat(new RsData<>("200-12", "성공").statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("데이터 없이 만들면 data는 null이다")
    void withoutData() {
        RsData<Void> rsData = new RsData<>("200-1", "성공");

        assertThat(rsData.resultCode()).isEqualTo("200-1");
        assertThat(rsData.msg()).isEqualTo("성공");
        assertThat(rsData.data()).isNull();
    }
}
