package com.be16261118report.global.rsData;

import com.fasterxml.jackson.annotation.JsonIgnore;

// 쓰기(작성/수정/삭제) API와 예외 응답을 감싸는 공통 형식 (조회 API에는 사용하지 않음)
// resultCode는 "HTTP상태코드-세부코드" 형식이다. (예: "201-1", "404-1")
// statusCode는 resultCode의 '-' 앞 숫자로, 응답 본문에는 내보내지 않고
// RsDataAspect가 HTTP 응답 상태 코드를 지정할 때 사용한다.
public record RsData<T>(
        String resultCode,
        @JsonIgnore int statusCode,
        String msg,
        T data
) {
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }

    public RsData(String resultCode, String msg, T data) {
        this(resultCode, parseStatusCode(resultCode), msg, data);
    }

    private static int parseStatusCode(String resultCode) {
        return Integer.parseInt(resultCode.split("-", 2)[0]);
    }
}
