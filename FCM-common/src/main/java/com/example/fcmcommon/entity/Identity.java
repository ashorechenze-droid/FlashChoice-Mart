package com.example.fcmcommon.entity;

/**
 * 实体主键接口（统一主键为 Long）
 */
public interface Identity<T> {

    T getId();

    void setId(T id);
}
