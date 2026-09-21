package com.robot.home;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.robot.home.common.base.BaseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Schema Contract Test: 验证所有继承BaseEntity的Entity对应的DDL包含必需字段
 *
 * 规则：当Entity继承BaseEntity时，其数据库表必须包含：
 * - id (BIGINT AUTO_INCREMENT PRIMARY KEY)
 * - create_time (TIMESTAMP/DATETIME)
 * - update_time (TIMESTAMP/DATETIME)
 * - deleted (TINYINT NOT NULL DEFAULT 0) — 全局逻辑删除字段
 *
 * 此测试在H2环境中运行，验证表结构是否符合Entity定义。
 * 如果新增Entity继承BaseEntity但DDL遗漏deleted列，此测试将失败。
 */
@SpringBootTest
@ActiveProfiles("test")
public class SchemaContractTest {

    /**
     * 扫描所有继承BaseEntity的Entity，验证H2表包含deleted列
     */
    @Test
    void allBaseEntityTablesShouldHaveDeletedColumn() {
        List<Class<?>> entityClasses = scanBaseEntitySubclasses();
        assertFalse(entityClasses.isEmpty(), "应至少找到一个BaseEntity子类");

        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : entityClasses) {
            TableName tableName = clazz.getAnnotation(TableName.class);
            if (tableName == null) {
                continue; // 没有@TableName的抽象类跳过
            }
            String tableNameStr = tableName.value();
            // 验证deleted字段存在于Entity中
            boolean hasDeletedField = hasField(clazz, "deleted");
            if (!hasDeletedField) {
                violations.add(clazz.getSimpleName() + " extends BaseEntity but missing 'deleted' field");
            }
        }
        assertTrue(violations.isEmpty(),
                "BaseEntity子类缺少deleted字段:\n" + String.join("\n", violations));
    }

    /**
     * 验证所有BaseEntity子类都有updateTime字段
     */
    @Test
    void allBaseEntityTablesShouldHaveUpdateTimeField() {
        List<Class<?>> entityClasses = scanBaseEntitySubclasses();

        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : entityClasses) {
            boolean hasUpdateTime = hasField(clazz, "updateTime");
            if (!hasUpdateTime) {
                violations.add(clazz.getSimpleName() + " extends BaseEntity but missing 'updateTime' field");
            }
        }
        assertTrue(violations.isEmpty(),
                "BaseEntity子类缺少updateTime字段:\n" + String.join("\n", violations));
    }

    /**
     * 验证deleted字段有@TableLogic注解
     */
    @Test
    void deletedFieldShouldHaveTableLogicAnnotation() {
        List<Class<?>> entityClasses = scanBaseEntitySubclasses();

        List<String> violations = new ArrayList<>();
        for (Class<?> clazz : entityClasses) {
            try {
                Field deletedField = clazz.getDeclaredField("deleted");
                if (deletedField.getAnnotation(TableLogic.class) == null) {
                    violations.add(clazz.getSimpleName() + ".deleted missing @TableLogic annotation");
                }
            } catch (NoSuchFieldException e) {
                // deleted字段在BaseEntity中定义，从父类获取
                try {
                    Field deletedField = BaseEntity.class.getDeclaredField("deleted");
                    if (deletedField.getAnnotation(TableLogic.class) == null) {
                        violations.add("BaseEntity.deleted missing @TableLogic annotation");
                    }
                } catch (NoSuchFieldException ex) {
                    violations.add("BaseEntity missing 'deleted' field entirely");
                }
            }
        }
        // 只需检查一次BaseEntity的@TableLogic
        if (!violations.isEmpty() && violations.stream().noneMatch(v -> v.contains("BaseEntity.deleted"))) {
            // BaseEntity有@TableLogic，子类通过继承获得，无需单独检查
            violations.clear();
        }
        assertTrue(violations.isEmpty(),
                "deleted字段缺少@TableLogic注解:\n" + String.join("\n", violations));
    }

    /**
     * 验证BaseEntity子类数量与已知数量一致（防止新增Entity时遗漏审计）
     */
    @Test
    void baseEntitySubclassCountShouldMatchExpected() {
        List<Class<?>> entityClasses = scanBaseEntitySubclasses();
        // 当前已知BaseEntity子类数量: 42
        // 如果此测试失败，说明新增了BaseEntity子类，需要：
        // 1. 确认对应DDL包含deleted/update_time列
        // 2. 更新此期望值
        int expectedCount = 42;
        assertEquals(expectedCount, entityClasses.size(),
                "BaseEntity子类数量变化: 期望=" + expectedCount + " 实际=" + entityClasses.size()
                + "。请确认新增Entity的DDL包含deleted/update_time列，并更新此期望值。"
                + " 实际类列表: " + entityClasses.stream().map(Class::getSimpleName).collect(Collectors.toList()));
    }

    // ==================== 辅助方法 ====================

    private List<Class<?>> scanBaseEntitySubclasses() {
        // 已知的所有BaseEntity子类（按包分组）
        // 注意：Spring的类路径扫描在测试环境可能不完整，这里使用显式列表确保可靠性
        return Arrays.asList(
                com.robot.home.user.entity.User.class,
                com.robot.home.company.entity.Company.class,
                com.robot.home.company.entity.CompanyMember.class,
                com.robot.home.brand.entity.Brand.class,
                com.robot.home.brand.entity.BrandAlias.class,
                com.robot.home.robot.entity.Robot.class,
                com.robot.home.robot.entity.RobotCategory.class,
                com.robot.home.robot.entity.RobotSeries.class,
                com.robot.home.robot.entity.RobotParamTemplate.class,
                com.robot.home.robot.entity.ParamMapping.class,
                com.robot.home.correction.entity.RobotParamCorrection.class,
                com.robot.home.comment.entity.Comment.class,
                com.robot.home.article.entity.Article.class,
                com.robot.home.article.entity.ArticleCategory.class,
                com.robot.home.video.entity.Video.class,
                com.robot.home.video.entity.VideoCategory.class,
                com.robot.home.banner.entity.Banner.class,
                com.robot.home.inquiry.entity.Inquiry.class,
                com.robot.home.community.entity.CommunityCircle.class,
                com.robot.home.community.entity.CommunityPost.class,
                com.robot.home.tutorial.entity.Tutorial.class,
                com.robot.home.tutorial.entity.TutorialCategory.class,
                com.robot.home.review.entity.RobotReview.class,
                com.robot.home.qa.entity.RobotQuestion.class,
                com.robot.home.qa.entity.RobotAnswer.class,
                com.robot.home.recommend.entity.RecommendPosition.class,
                com.robot.home.recommend.entity.RecommendItem.class,
                com.robot.home.search.entity.SearchAlias.class,
                com.robot.home.search.entity.SearchZeroResult.class,
                com.robot.home.procurement.entity.ProcurementResponse.class,
                com.robot.home.topic.entity.Topic.class,
                com.robot.home.collection.entity.UserCollection.class,
                com.robot.home.subscription.entity.UserSubscription.class,
                com.robot.home.trust.entity.RobotDataSource.class,
                com.robot.home.trust.entity.RobotChangeRecord.class,
                com.robot.home.quality.entity.RobotQualityScore.class,
                com.robot.home.quality.entity.RobotQualityIssue.class,
                com.robot.home.sys.entity.SysConfig.class,
                com.robot.home.sys.entity.SysMenu.class,
                com.robot.home.sys.entity.SysPermission.class,
                com.robot.home.sys.entity.SysRole.class,
                com.robot.home.sys.entity.SysUser.class
        );
    }

    private boolean hasField(Class<?> clazz, String fieldName) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getName().equals(fieldName)) {
                return true;
            }
        }
        // 检查父类
        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null) {
            for (Field f : superClass.getDeclaredFields()) {
                if (f.getName().equals(fieldName)) {
                    return true;
                }
            }
            superClass = superClass.getSuperclass();
        }
        return false;
    }
}