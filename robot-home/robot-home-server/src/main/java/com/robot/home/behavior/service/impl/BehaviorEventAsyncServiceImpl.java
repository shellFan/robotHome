package com.robot.home.behavior.service.impl;

import cn.hutool.core.util.StrUtil;
import com.robot.home.behavior.dto.BehaviorEventDTO;
import com.robot.home.behavior.entity.BehaviorEvent;
import com.robot.home.behavior.mapper.BehaviorEventMapper;
import com.robot.home.behavior.service.BehaviorEventAsyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 行为事件异步服务实现
 * <p>
 * 独立Bean，@Async通过Spring AOP代理生效。
 * 解决BehaviorEventServiceImpl内自调用@Async不生效的问题。
 */
@Service
public class BehaviorEventAsyncServiceImpl implements BehaviorEventAsyncService {

    private static final Logger log = LoggerFactory.getLogger(BehaviorEventAsyncServiceImpl.class);

    @Resource
    private BehaviorEventMapper behaviorEventMapper;

    @Override
    @Async("asyncExecutor")
    public void saveEvent(Long userId, BehaviorEventDTO dto, String ip, String ua) {
        try {
            BehaviorEvent event = new BehaviorEvent();
            event.setUserId(userId);
            event.setEventType(dto.getEventType());
            event.setBizType(dto.getBizType());
            event.setBizId(dto.getBizId());
            event.setExtra(StrUtil.isBlank(dto.getExtra()) ? null : dto.getExtra());
            event.setIp(ip != null && ip.length() > 64 ? ip.substring(0, 64) : ip);
            event.setUserAgent(ua != null && ua.length() > 512 ? ua.substring(0, 512) : ua);
            event.setCreateTime(new Date());
            behaviorEventMapper.insert(event);
        } catch (Exception e) {
            log.error("保存行为事件失败: eventType={}, bizType={}, bizId={}",
                    dto.getEventType(), dto.getBizType(), dto.getBizId(), e);
        }
    }
}