package com.be16261118report.global.exception;

// 비즈니스 규칙 위반 등 의도적으로 던지는 예외
// GlobalExceptionHandler가 resultCode와 msg를 그대로 RsData로 감싸 응답한다.
public class ServiceException extends RuntimeException {

    private final String resultCode;

    public ServiceException(String resultCode, String msg) {
        super(msg);
        this.resultCode = resultCode;
    }

    public String getResultCode() {
        return resultCode;
    }
}
