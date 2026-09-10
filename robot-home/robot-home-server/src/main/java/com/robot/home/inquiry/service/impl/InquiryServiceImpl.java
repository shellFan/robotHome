package com.robot.home.inquiry.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.common.PageResult;
import com.robot.home.common.Constants;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.service.BizCounter;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.SensitiveUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.behavior.dto.BehaviorEventDTO;
import com.robot.home.behavior.service.BehaviorEventService;
import com.robot.home.inquiry.dto.InquiryDTO;
import com.robot.home.inquiry.entity.Inquiry;
import com.robot.home.inquiry.mapper.InquiryMapper;
import com.robot.home.inquiry.service.InquiryService;
import com.robot.home.inquiry.vo.InquiryVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * 询价服务实现
 */
@Service
public class InquiryServiceImpl extends ServiceImpl<InquiryMapper, Inquiry> implements InquiryService {

    private static final Logger log = LoggerFactory.getLogger(InquiryServiceImpl.class);

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private BizCounter bizCounter;
    @Resource
    private BehaviorEventService behaviorEventService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(Long userId, InquiryDTO dto) {
        Inquiry inquiry = new Inquiry();
        inquiry.setRobotId(dto.getRobotId());
        inquiry.setUserId(userId);
        inquiry.setName(XssUtils.escapeText(StrUtil.trim(dto.getName())));
        inquiry.setPhone(StrUtil.trim(dto.getPhone()));
        inquiry.setRegion(XssUtils.escapeText(dto.getRegion()));
        inquiry.setCustomerType(dto.getCustomerType() == null ? 1 : dto.getCustomerType());
        inquiry.setCompanyName(XssUtils.escapeText(dto.getCompanyName()));
        inquiry.setQuantity(dto.getQuantity() == null || dto.getQuantity() < 1 ? 1 : dto.getQuantity());
        inquiry.setBudget(dto.getBudget());
        inquiry.setRemark(XssUtils.escapeText(dto.getRemark()));
        inquiry.setStatus(Constants.INQUIRY_PENDING);

        if (dto.getRobotId() != null) {
            Robot robot = robotMapper.selectById(dto.getRobotId());
            if (robot == null) {
                throw new BusinessException("机器人不存在");
            }
            inquiry.setRobotName(robot.getName());
        }
        save(inquiry);

        if (dto.getRobotId() != null) {
            bizCounter.incr("robot", dto.getRobotId(), BizCounter.Field.INQUIRY);
        }

        // 询价成功后触发INQUIRY行为事件（服务端受信，更新热度排行）
        if (dto.getRobotId() != null) {
            try {
                BehaviorEventDTO eventDto = new BehaviorEventDTO();
                eventDto.setEventType("INQUIRY");
                eventDto.setBizType("robot");
                eventDto.setBizId(dto.getRobotId());
                behaviorEventService.recordTrusted(userId, eventDto, null, null, null);
            } catch (Exception e) {
                // 行为事件记录失败不影响询价主流程
                log.warn("触发INQUIRY行为事件失败: robotId={}, error={}", dto.getRobotId(), e.getMessage());
            }
        }

        return inquiry.getId();
    }

    @Override
    public PageResult<InquiryVO> my(Long userId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Inquiry> page = new Page<>(pn, ps);
        IPage<Inquiry> result = page(page, Wrappers.<Inquiry>lambdaQuery()
                .eq(Inquiry::getUserId, userId)
                .orderByDesc(Inquiry::getCreateTime));
        return PageResult.of(pn, ps, result.getTotal(),
                result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    @Override
    public InquiryVO myDetail(Long userId, Long id) {
        Inquiry inquiry = getOne(Wrappers.<Inquiry>lambdaQuery()
                .eq(Inquiry::getId, id)
                .eq(Inquiry::getUserId, userId));
        if (inquiry == null) {
            throw new BusinessException("询价记录不存在");
        }
        return toVO(inquiry);
    }

    private InquiryVO toVO(Inquiry i) {
        InquiryVO vo = new InquiryVO();
        vo.setId(i.getId());
        vo.setRobotId(i.getRobotId());
        vo.setRobotName(i.getRobotName());
        vo.setUserId(i.getUserId());
        vo.setName(i.getName());
        vo.setPhone(SensitiveUtils.maskPhone(i.getPhone()));
        vo.setRegion(i.getRegion());
        vo.setCustomerType(i.getCustomerType());
        vo.setCustomerTypeName(Integer.valueOf(2).equals(i.getCustomerType()) ? "企业" : "个人");
        vo.setCompanyName(i.getCompanyName());
        vo.setQuantity(i.getQuantity());
        vo.setBudget(i.getBudget());
        vo.setRemark(i.getRemark());
        vo.setStatus(i.getStatus());
        vo.setStatusName(statusName(i.getStatus()));
        vo.setHandleNote(i.getHandleNote());
        vo.setCreateTime(i.getCreateTime());
        return vo;
    }

    private String statusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case Constants.INQUIRY_PENDING:
                return "待处理";
            case Constants.INQUIRY_PROCESSING:
                return "处理中";
            case Constants.INQUIRY_CONTACTED:
                return "已联系";
            case Constants.INQUIRY_DEAL:
                return "已成交";
            case Constants.INQUIRY_CLOSED:
                return "已关闭";
            default:
                return "未知";
        }
    }
}
