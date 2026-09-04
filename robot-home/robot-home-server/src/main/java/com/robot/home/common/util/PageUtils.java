package com.robot.home.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.robot.home.common.PageResult;

/**
 * 分页转换工具：保证所有分页接口返回同一种结构
 */
public final class PageUtils {

    private PageUtils() {
    }

    public static <T> PageResult<T> toResult(IPage<T> page) {
        return PageResult.of((int) page.getCurrent(), (int) page.getSize(), page.getTotal(), page.getRecords());
    }

    /**
     * 修正分页参数，避免越界导致的慢查询
     */
    public static int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : Math.min(pageNum, 1000);
    }

    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }
}
