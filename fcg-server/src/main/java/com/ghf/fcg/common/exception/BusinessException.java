package com.ghf.fcg.common.exception;

import lombok.Getter;
import com.ghf.fcg.common.constant.ResultCode;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.PARAM_ERROR;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
