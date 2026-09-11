package com.robot.home.inquiry.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.Constants;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.inquiry.dto.InquiryFollowDTO;
import com.robot.home.inquiry.entity.Inquiry;
import com.robot.home.inquiry.entity.InquiryFollow;
import com.robot.home.inquiry.mapper.InquiryFollowMapper;
import com.robot.home.inquiry.mapper.InquiryMapper;
import com.robot.home.inquiry.service.InquiryFollowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 询价跟进服务实现
 * <p>
 * 状态流转规则:
 * CONTACTED → inquiry.status = 3 (已联系)
 * FOLLOWING → inquiry.status = 2 (处理中)
 * CLOSED    → inquiry.status = 5 (已关闭)
 * INVALID   → inquiry.status = 5 (已关闭)
 * NOTE      → 不改变状态（纯备注）
 */
@Service
public class InquiryFollowServiceImpl implements InquiryFollowService {

    /** 跟进类型 → 询价状态映射 */
    private static final java.util.Map<String, Integer> FOLLOW_TYPE_TO_STATUS = new java.util.HashMap<>();

    static {
        FOLLOW_TYPE_TO_STATUS.put("CONTACTED", Constants.INQUIRY_CONTACTED);  // 3
        FOLLOW_TYPE_TO_STATUS.put("FOLLOWING", Constants.INQUIRY_PROCESSING); // 2
        FOLLOW_TYPE_TO_STATUS.put("CLOSED", Constants.INQUIRY_CLOSED);        // 5
        FOLLOW_TYPE_TO_STATUS.put("INVALID", Constants.INQUIRY_CLOSED);       // 5
        // NOTE 不映射，不改变状态
    }

    @Resource
    private InquiryFollowMapper inquiryFollowMapper;
    @Resource
    private InquiryMapper inquiryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InquiryFollow addFollow(Long adminUserId, InquiryFollowDTO dto) {
        // 1. 校验跟进类型
        if (!VALID_FOLLOW_TYPES.contains(dto.getFollowType())) {
            throw new BusinessException("不支持的跟进类型: " + dto.getFollowType());
        }

        // 2. 校验询价存在
        Inquiry inquiry = inquiryMapper.selectById(dto.getInquiryId());
        if (inquiry == null) {
            throw new BusinessException("询价记录不存在");
        }

        // 3. 创建跟进记录
        InquiryFollow follow = new InquiryFollow();
        follow.setInquiryId(dto.getInquiryId());
        follow.setFollowUserId(adminUserId);
        follow.setFollowType(dto.getFollowType());
        follow.setContent(dto.getContent());
        follow.setCreateTime(new Date());

        // 4. 根据跟进类型更新询价状态
        Integer newStatus = FOLLOW_TYPE_TO_STATUS.get(dto.getFollowType());
        if (newStatus != null) {
            follow.setAfterStatus(newStatus);
            Inquiry update = new Inquiry();
            update.setId(dto.getInquiryId());
            update.setStatus(newStatus);
            inquiryMapper.updateById(update);
        } else {
            // NOTE 类型，保持原状态
            follow.setAfterStatus(inquiry.getStatus());
        }

        inquiryFollowMapper.insert(follow);
        return follow;
    }

    @Override
    public List<InquiryFollow> listByInquiryId(Long inquiryId) {
        return inquiryFollowMapper.selectList(Wrappers.<InquiryFollow>lambdaQuery()
                .eq(InquiryFollow::getInquiryId, inquiryId)
                .orderByDesc(InquiryFollow::getCreateTime));
    }
}