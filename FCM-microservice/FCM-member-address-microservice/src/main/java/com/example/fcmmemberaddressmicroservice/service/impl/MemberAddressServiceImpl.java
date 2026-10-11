package com.example.fcmmemberaddressmicroservice.service.impl;

import com.example.fcmcommon.constant.ResponseCode;
import com.example.fcmcommon.exception.BizException;
import com.example.fcmcommon.util.XssUtil;
import com.example.fcmmemberaddressmicroservice.mapper.MemberAddressMapper;
import com.example.fcmspi.model.MemberAddressEntity;
import com.example.fcmspi.service.MemberAddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberAddressServiceImpl implements MemberAddressService {

    private final MemberAddressMapper memberAddressMapper;

    public MemberAddressServiceImpl(MemberAddressMapper memberAddressMapper) {
        this.memberAddressMapper = memberAddressMapper;
    }

    @Override
    public List<MemberAddressEntity> listByMemberAccount(String memberAccount) {
        return memberAddressMapper.findByMemberAccount(memberAccount);
    }

    @Override
    public MemberAddressEntity getById(Long id) {
        return memberAddressMapper.findById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(MemberAddressEntity address) {
        sanitizeTextFields(address);
        // member_address 表无审计字段，仅维护业务列
        boolean asDefault = Boolean.TRUE.equals(address.getIsDefault());
        if (asDefault) {
            memberAddressMapper.clearDefault(address.getMemberAccount(), null);
        } else if (address.getIsDefault() == null) {
            address.setIsDefault(false);
        }
        return memberAddressMapper.save(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(MemberAddressEntity address) {
        sanitizeTextFields(address);
        // 置默认时先清除同会员其他默认地址
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            memberAddressMapper.clearDefault(address.getMemberAccount(), address.getId());
        }
        return memberAddressMapper.update(address);
    }

    /** 收货人/详细地址为会员输入的纯文本字段，剥离 HTML 标签防 XSS（详细设计 8.2） */
    private void sanitizeTextFields(MemberAddressEntity address) {
        address.setReceiver(XssUtil.stripTags(address.getReceiver()));
        address.setAddrDetail(XssUtil.stripTags(address.getAddrDetail()));
    }

    @Override
    public int deleteById(Long id) {
        return memberAddressMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int setDefault(Long id, String memberAccount) {
        MemberAddressEntity address = memberAddressMapper.findById(id);
        if (address == null) {
            throw new BizException(ResponseCode.NOT_FOUND, "地址不存在");
        }
        if (!address.getMemberAccount().equals(memberAccount)) {
            throw new BizException(ResponseCode.FORBIDDEN, "无权操作他人地址");
        }
        memberAddressMapper.clearDefault(memberAccount, id);
        return memberAddressMapper.setDefaultById(id);
    }
}
