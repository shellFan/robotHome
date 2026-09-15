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
import com.robot.home.search.entity.SearchAlias;
import com.robot.home.search.mapper.SearchAliasMapper;
import com.robot.home.search.service.SearchAliasService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class SearchAliasServiceImpl extends ServiceImpl<SearchAliasMapper, SearchAlias> implements SearchAliasService {

    @Override
    public List<SearchAlias> findByAlias(String alias) {
        if (StrUtil.isBlank(alias)) {
            return java.util.Collections.emptyList();
        }
        return list(Wrappers.<SearchAlias>lambdaQuery()
                .eq(SearchAlias::getAlias, alias.trim())
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
        if (StrUtil.isBlank(alias.getTargetType())) {
            throw new BusinessException("目标类型不能为空");
        }
        if (alias.getTargetId() == null) {
            throw new BusinessException("目标ID不能为空");
        }
        alias.setAlias(XssUtils.clean(StrUtil.trim(alias.getAlias())));
        alias.setStatus(1);
        save(alias);
        return alias.getId();
    }

    @Override
    public void update(Long id, SearchAlias alias) {
        SearchAlias existing = getById(id);
        if (existing == null) {
            throw new BusinessException("别名不存在");
        }
        if (StrUtil.isNotBlank(alias.getAlias())) {
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