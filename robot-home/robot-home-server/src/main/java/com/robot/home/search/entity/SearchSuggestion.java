package com.robot.home.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * 搜索建议
 * 对应表: search_suggestion
 * <p>
 * 管理员预设的搜索建议词，按权重排序展示
 */
@Getter
@Setter
@TableName("search_suggestion")
public class SearchSuggestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关键词 */
    private String keyword;

    /** 类型: robot/brand/company/article */
    private String type;

    /** 权重（越大越靠前） */
    private Integer weight;

    /** 是否启用: 1=启用, 0=禁用 */
    private Integer enabled;

    @Override
    public String toString() {
        return "SearchSuggestion{id=" + id + ", keyword=" + keyword + ", type=" + type +
               ", weight=" + weight + ", enabled=" + enabled + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchSuggestion that = (SearchSuggestion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}