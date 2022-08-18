package bh.bhback.global.error.advice.exception;

import bh.bhback.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@AllArgsConstructor
public class CUserNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;
    public CUserNotFoundException() {
        super();
        errorCode = ErrorCode.USER_NOT_FOUND;
    }
}
