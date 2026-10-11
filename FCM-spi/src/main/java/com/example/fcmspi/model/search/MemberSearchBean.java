package com.example.fcmspi.model.search;

import com.example.fcmspi.model.MemberEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员查询条件：支持关键字（账号/姓名/手机号模糊）与 enabled 等继承字段过滤
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberSearchBean extends MemberEntity {

    /** 关键字：匹配 account / name / phone */
    private String keyword;
}
