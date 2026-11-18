package com.be16261118report.global.globalExceptionHandler;

import com.be16261118report.global.exception.ServiceException;
import com.be16261118report.global.rsData.RsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

// 모든 예외를 RsData로 감싸 응답한다.
// HTTP 상태 코드는 여기서 지정하지 않고, RsDataAspect가 resultCode 앞 숫자로 후처리한다.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public RsData<Void> handle(ServiceException e) {
        return new RsData<>(e.getResultCode(), e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public RsData<Void> handle(NoSuchElementException e) {
        return new RsData<>("404-1", "해당 데이터가 존재하지 않습니다.");
    }

    // 실패한 필드를 "필드명-검증어노테이션-메시지" 형태로 정렬해 줄바꿈으로 이어 붙인다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RsData<Void> handle(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + "-" + error.getCode() + "-" + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("\n"));

        return new RsData<>("400-1", msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public RsData<Void> handle(HttpMessageNotReadableException e) {
        return new RsData<>("400-2", "요청 본문이 올바르지 않습니다.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public RsData<Void> handle(MethodArgumentTypeMismatchException e) {
        return new RsData<>("400-3", "%s의 형식이 올바르지 않습니다.".formatted(e.getName()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public RsData<Void> handle(NoResourceFoundException e) {
        return new RsData<>("404-2", "요청한 경로가 존재하지 않습니다.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public RsData<Void> handle(HttpRequestMethodNotSupportedException e) {
        return new RsData<>("405-1", "지원하지 않는 요청 방식입니다.");
    }

    @ExceptionHandler(Exception.class)
    public RsData<Void> handle(Exception e) {
        log.error("처리되지 않은 예외", e);
        return new RsData<>("500-1", "서버 오류가 발생했습니다.");
    }
}
