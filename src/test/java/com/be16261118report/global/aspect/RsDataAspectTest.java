package com.be16261118report.global.aspect;

import com.be16261118report.global.rsData.RsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RsDataAspect: 컨트롤러/예외 핸들러가 RsData를 반환하면
// resultCode 앞 숫자가 HTTP 응답 상태 코드로 지정되는지 검증
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Import({
        RsDataAspectTest.RsDataTestController.class,
        RsDataAspectTest.RsDataTestExceptionHandler.class
})
class RsDataAspectTest {

    static class RsDataTestException extends RuntimeException {
    }

    // 테스트 전용 컨트롤러 (테스트 클래스의 중첩 클래스는 컴포넌트 스캔에서 제외되므로 @Import로 등록)
    @RestController
    @RequestMapping("/test/rsData")
    static class RsDataTestController {

        @GetMapping("/created")
        public RsData<Map<String, Integer>> created() {
            return new RsData<>("201-1", "생성되었습니다.", Map.of("id", 1));
        }

        @GetMapping("/notFound")
        public RsData<Void> notFound() {
            return new RsData<>("404-1", "없습니다.");
        }

        @GetMapping("/plain")
        public Map<String, String> plain() {
            return Map.of("hello", "world");
        }

        @GetMapping("/exception")
        public RsData<Void> exception() {
            throw new RsDataTestException();
        }
    }

    // 테스트 전용 예외 핸들러: @RestControllerAdvice가 반환한 RsData에도 Aspect가 적용되는지 확인
    // 다른 전역 예외 핸들러(Exception 전체 처리)보다 먼저 선택되도록 우선순위를 가장 높게 둔다.
    @RestControllerAdvice
    @Order(Ordered.HIGHEST_PRECEDENCE)
    static class RsDataTestExceptionHandler {

        @ExceptionHandler(RsDataTestException.class)
        public RsData<Void> handle(RsDataTestException e) {
            return new RsData<>("409-1", "예외 핸들러 응답입니다.");
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("RsData의 resultCode가 201-1이면 응답 상태는 201이다")
    void created() throws Exception {
        mvc.perform(get("/test/rsData/created"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("생성되었습니다."))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.statusCode").doesNotExist());
    }

    @Test
    @DisplayName("RsData의 resultCode가 404-1이면 응답 상태는 404다")
    void notFound() throws Exception {
        mvc.perform(get("/test/rsData/notFound"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @DisplayName("RsData가 아닌 응답은 상태 코드를 건드리지 않는다")
    void plain() throws Exception {
        mvc.perform(get("/test/rsData/plain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hello").value("world"))
                .andExpect(jsonPath("$.resultCode").doesNotExist());
    }

    @Test
    @DisplayName("@RestControllerAdvice 예외 핸들러가 반환한 RsData도 resultCode로 상태가 지정된다")
    void exceptionHandler() throws Exception {
        mvc.perform(get("/test/rsData/exception"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"))
                .andExpect(jsonPath("$.msg").value("예외 핸들러 응답입니다."));
    }
}
