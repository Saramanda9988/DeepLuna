package com.luna.deepluna.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CommonErrorEnum implements ErrorEnum {

    SYSTEM_ERROR(-1, "系统出小差了，请稍后再试哦~~"),
    PARAM_VALID(-2, "参数校验失败{0}"),
    DATABASE_ERROR(-5, "数据库异常"),
    PARAMS_ERROR(102, "参数有误"),
    USER_NOT_EXIST(410, "用户不存在"),;
    private final Integer code;
    private final String msg;

    @Override
    public Integer getErrorCode() {
        return this.code;
    }

    @Override
    public String getErrorMsg() {
        return this.msg;
    }
}
