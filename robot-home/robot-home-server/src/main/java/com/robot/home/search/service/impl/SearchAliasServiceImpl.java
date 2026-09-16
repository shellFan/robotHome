package com.robot.home.search.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.common.PageResult;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.PageUtils;
import com.robot.home.common.util.XssUtils;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.search.entity.SearchAlias;
import com.robot.home.search.mapper.SearchAliasMapper;
import com.robot.home.search.service.SearchAliasService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class SearchAliasServiceImpl extends ServiceImpl<SearchAliasMapper, SearchAlias> implements SearchAliasService {

    private static final int ALIAS_MAX_LENGTH = 100;

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private BrandMapper brandMapper;

    @Override
    public List<SearchAlias> findByAlias(String alias) {
        if (StrUtil.isBlank(alias)) {
            return java.util.Collections.emptyList();
        }
        // Normalize: trim + lowercase for consistent matching
        String normalized = alias.trim().toLowerCase();
        return list(Wrappers.<SearchAlias>lambdaQuery()
                .eq(SearchAlias::getAlias, normalized)
                .eq(SearchAlias::getStatus, 1));
    }

    @Override
    public PageResult<SearchAlias> adminList(String targetType, Integer pageNum, Integer pageSize) {
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<SearchAlias> page = new Page<>(pn, ps);
        IPage<SearchAlias> result = page(page, Wrappers.<SearchAlias>lambdaQuery()
                .eq(StrUtil.isNotBlank(targetType), SearchAlias::getTargetType, targetType)
                .orderByDesc(SearchAlias::getCreateTime));
        return PageResult.of(pn, ps, result.getTotal(), result.getRecords());
    }

    @Override
    public Long create(SearchAlias alias) {
        if (StrUtil.isBlank(alias.getAlias())) {
            throw new BusinessException("别名不能为空");
        }
        if (alias.getAlias().length() > ALIAS_MAX_LENGTH) {
            throw new BusinessException("别名长度不能超过" + ALIAS_MAX_LENGTH + "字");
        }
        if (StrUtil.isBlank(alias.getTargetType())) {
            throw new BusinessException("目标类型不能为空");
        }
        if (alias.getTargetId() == null) {
            throw new BusinessException("目标ID不能为空");
        }
        // 验证targetId存在性
        validateTargetExists(alias.getTargetType(), alias.getTargetId());
        // Normalize alias: trim + lowercase for consistent matching
        String normalizedAlias = StrUtil.trim(alias.getAlias()).toLowerCase();
        alias.setAlias(XssUtils.clean(normalizedAlias));
        // Self-reference prevention: alias must not equal targetName
        if (StrUtil.isNotBlank(alias.getTargetName())
                && alias.getAlias().equalsIgnoreCase(StrUtil.trim(alias.getTargetName()))) {
            throw new BusinessException("别名不能与目标名称相同");
        }
        alias.setStatus(1);
        save(alias);
        return alias.getId();
    }

    private void validateTargetExists(String targetType, Long targetId) {
        switch (targetType) {
            case "robot":
                if (robotMapper.selectById(targetId) == null) {
                    throw new BusinessException("目标机器人不存在");
                }
                break;
            case "brand":
                if (brandMapper.selectById(targetId) == null) {
                    throw new BusinessException("目标品牌不存在");
                }
                break;
            default:
                throw new BusinessException("不支持的目标类型: " + targetType);
        }
    }

    @Override
    public void update(Long id, SearchAlias alias) {
        SearchAlias existing = getById(id);
        if (existing == null) {
            throw new BusinessException("别名不存在");
        }
        if (StrUtil.isNotBlank(alias.getAlias())) {
            if (alias.getAlias().length() > ALIAS_MAX_LENGTH) {
                throw new BusinessException("别名长度不能超过" + ALIAS_MAX_LENGTH + "字");
            }
            existing.setAlias(XssUtils.clean(StrUtil.trim(alias.getAlias())));
        }
        if (StrUtil.isNotBlank(alias.getTargetName())) {
            existing.setTargetName(alias.getTargetName());
        }
        if (alias.getStatus() != null) {
            existing.setStatus(alias.getStatus());
        }
        updateById(existing);
    }

    @Override
    public void toggleStatus(Long id) {
        SearchAlias existing = getById(id);
        if (existing == null) {
            throw new BusinessException("别名不存在");
        }
        existing.setStatus(existing.getStatus() == 1 ? 0 : 1);
        updateById(existing);
    }
}