package com.robot.home.message.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.PageUtils;
import com.robot.home.message.entity.Message;
import com.robot.home.message.mapper.MessageMapper;
import com.robot.home.message.service.MessageService;
import com.robot.home.message.vo.MessageVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 站内消息服务实现
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Override
    public PageResult<MessageVO> list(Long userId, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<Message> page = new Page<>(pn, ps);
        IPage<Message> result = page(page, Wrappers.<Message>lambdaQuery()
                .eq(Message::getUserId, userId)
                .orderByDesc(Message::getCreateTime));
        return PageResult.of(pn, ps, result.getTotal(),
                result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
    }

    @Override
    public long unreadCount(Long userId) {
        return count(Wrappers.<Message>lambdaQuery()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0));
    }

    @Override
    public void markRead(Long userId, Long id) {
        Message update = new Message();
        update.setId(id);
        update.setIsRead(1);
        update(null, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getId, id)
                .eq(Message::getUserId, userId)
                .set(Message::getIsRead, 1));
    }

    @Override
    public void markAllRead(Long userId) {
        update(null, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getUserId, userId)
                .set(Message::getIsRead, 1));
    }

    @Override
    public void send(Long userId, String type, String title, String content, Long relatedId) {
        Message message = new Message();
        message.setUserId(userId);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setRelatedId(relatedId);
        message.setIsRead(0);
        message.setCreateTime(LocalDateTime.now());
        baseMapper.insert(message);
    }

    private MessageVO toVO(Message m) {
        MessageVO vo = new MessageVO();
        vo.setId(m.getId());
        vo.setType(m.getType());
        vo.setTitle(m.getTitle());
        vo.setContent(m.getContent());
        vo.setRelatedId(m.getRelatedId());
        vo.setIsRead(m.getIsRead());
        vo.setCreateTime(m.getCreateTime());
        return vo;
    }
}
