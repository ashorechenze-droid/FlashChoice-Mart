package com.example.fcmspi.model;

import com.example.fcmcommon.entity.AuditEntity;
import lombok.Data;

/**
 * 会员收货地址（member_address，member_account 关联会员账号）
 */
@Data
public class MemberAddressEntity extends AuditEntity {

    /** 会员账号 */
    private String memberAccount;
    /** 收货人 */
    private String receiver;
    /** 手机号 */
    private String phone;
    /** 所在地区 ID（省市区） */
    private Long addrId;
    /** 详细地址 */
    private String addrDetail;
    /** 是否默认（同一会员仅一条 is_default=1） */
    private Boolean isDefault;
}
