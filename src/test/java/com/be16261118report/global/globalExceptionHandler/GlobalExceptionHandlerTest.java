package com.be16261118report.global.globalExceptionHandler;

import com.be16261118report.global.exception.ServiceException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 모든 예외가 RsData로 감싸져 반환되고, 후처리로 resultCode에 맞는 HTTP 상태 코드가 지정되는지 검증
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Import(GlobalExceptionHandlerTest.ExceptionTestController.class)
class GlobalExceptionHandlerTest {

    record TestReqBody(@NotBlank String title) {
    }

    // 테스트 전용 컨트롤러 (테스트 클래스의 중첩 클래스는 컴포넌트 스캔에서 제외되므로 @Import로 등록)
    @RestController
    @RequestMapping("/test/exception")
    static class ExceptionTestController {

        @GetMapping("/service")
        public void service() {
            throw new ServiceException("409-1", "이미 처리되었습니다.");
        }

        @GetMapping("/noSuchElement")
        public void noSuchElement() {
            throw new NoSuchElementException();
        }

        @PostMapping("/valid")
        public void valid(@Valid @RequestBody TestReqBody reqBody) {
        }

        @GetMapping("/typeMismatch/{id}")
        public void typeMismatch(@PathVariable long id) {
        }

        @GetMapping("/unknown")
        public void unknown() {
            throw new IllegalStateException("예상하지 못한 오류");
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("ServiceException은 지정한 resultCode와 메시지로 응답한다")
    void serviceException() throws Exception {
        mvc.perform(get("/test/exception/service"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("409-1"))
                .andExpect(jsonPath("$.msg").value("이미 처리되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("NoSuchElementException은 404-1로 응답한다")
    void noSuchElement() throws Exception {
        mvc.perform(get("/test/exception/noSuchElement"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("해당 데이터가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("요청 본문 검증 실패는 400-1로 응답하고 실패한 필드를 메시지에 담는다")
    void validation() throws Exception {
        mvc.perform(post("/test/exception/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": " "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value(containsString("title-NotBlank")));
    }

    @Test
    @DisplayName("요청 본문 JSON이 올바르지 않으면 400-2로 응답한다")
    void notReadable() throws Exception {
        mvc.perform(post("/test/exception/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ 잘못된 json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"));
    }

    @Test
    @DisplayName("경로 변수 타입이 맞지 않으면 400-3으로 응답한다")
    void typeMismatch() throws Exception {
        mvc.perform(get("/test/exception/typeMismatch/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"));
    }

    @Test
    @DisplayName("존재하지 않는 경로는 404-2로 응답한다")
    void noResource() throws Exception {
        mvc.perform(get("/test/exception/not-exists-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-2"));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 405-1로 응답한다")
    void methodNotSupported() throws Exception {
        mvc.perform(post("/test/exception/service"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.resultCode").value("405-1"));
    }

    @Test
    @DisplayName("그 외 처리하지 못한 예외는 500-1로 응답한다")
    void unknown() throws Exception {
        mvc.perform(get("/test/exception/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.resultCode").value("500-1"))
                .andExpect(jsonPath("$.msg").value("서버 오류가 발생했습니다."));
    }
}
