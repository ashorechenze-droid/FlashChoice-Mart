package com.example.fcmspi.service;

import com.example.fcmspi.model.MemberAddressEntity;

import java.util.List;

/**
 * 会员收货地址业务接口（member-address-api）：CRUD、默认地址唯一性
 */
public interface MemberAddressService {

    /** 当前会员地址列表 */
    List<MemberAddressEntity> listByMemberAccount(String memberAccount);

    MemberAddressEntity getById(Long id);

    int insert(MemberAddressEntity address);

    int update(MemberAddressEntity address);

    int deleteById(Long id);

    /** 设为默认（事务内清除该会员其他默认地址） */
    int setDefault(Long id, String memberAccount);
}
