package com.xxl.job.executor.core.config;

public enum DBTypeEnum {
    db70("db70"), db74("db74");
    private String value;

    DBTypeEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
