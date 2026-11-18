package com.be16261118report.global.aspect;

import com.be16261118report.global.rsData.RsData;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// 응답 후처리: RsData를 반환하는 메서드가 끝나면 resultCode 앞 숫자(예: "404-1" → 404)를
// HTTP 응답 상태 코드로 지정한다.
// 적용 대상
// - @RestController의 public 메서드 중 RsData를 반환하는 것 (작성/수정/삭제 API)
// - @RestControllerAdvice의 public 메서드 중 RsData를 반환하는 것 (글로벌 예외 처리)
// 조회 API처럼 RsData가 아닌 값을 반환하는 메서드는 대상이 아니므로 상태 코드를 건드리지 않는다.
@Aspect
@Component
public class RsDataAspect {

    @Around("""
            (
                within(@org.springframework.web.bind.annotation.RestController *)
                || within(@org.springframework.web.bind.annotation.RestControllerAdvice *)
            )
            && execution(public com.be16261118report.global.rsData.RsData *(..))
            """)
    public Object setResponseStatus(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        if (result instanceof RsData<?> rsData
                && RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletResponse response = attributes.getResponse();

            if (response != null) {
                response.setStatus(rsData.statusCode());
            }
        }

        return result;
    }
}
