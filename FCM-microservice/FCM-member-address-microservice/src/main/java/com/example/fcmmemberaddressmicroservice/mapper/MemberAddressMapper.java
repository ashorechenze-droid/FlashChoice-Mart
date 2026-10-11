package com.example.fcmmemberaddressmicroservice.mapper;

import com.example.fcmspi.model.MemberAddressEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberAddressMapper {

    List<MemberAddressEntity> findByMemberAccount(String memberAccount);

    MemberAddressEntity findById(Long id);

    int save(MemberAddressEntity address);

    int update(MemberAddressEntity address);

    int deleteById(Long id);

    /** 清除会员其他默认地址（excludeId 为空则全部清除） */
    int clearDefault(@Param("memberAccount") String memberAccount, @Param("excludeId") Long excludeId);

    int setDefaultById(Long id);
}
