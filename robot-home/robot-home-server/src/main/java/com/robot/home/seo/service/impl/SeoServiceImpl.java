package com.robot.home.seo.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.robot.home.article.entity.Article;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.seo.entity.SeoPageView;
import com.robot.home.seo.mapper.SeoPageViewMapper;
import com.robot.home.seo.service.SeoService;
import com.robot.home.seo.util.SlugUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SEO服务实现
 */
@Slf4j
@Service
public class SeoServiceImpl implements SeoService {

    private static final DateTimeFormatter W3C_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int SITEMAP_LIMIT = 5000;

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private SeoPageViewMapper seoPageViewMapper;

    @Override
    public String generateSitemap(String baseUrl) {
        String base = baseUrl == null || baseUrl.isEmpty() ? "https://www.example.com" : baseUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 首页
        appendUrl(sb, base + "/", LocalDateTime.now().format(W3C_DATE), "daily", "1.0");

        int count = 1;

        // 机器人详情页
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getStatus, 1)
                .orderByDesc(Robot::getUpdateTime)
                .last("LIMIT " + SITEMAP_LIMIT));
        for (Robot robot : robots) {
            if (count >= SITEMAP_LIMIT) break;
            String slug = robot.getSlug() != null ? robot.getSlug() : ("robot-" + robot.getId());
            appendUrl(sb, base + "/robot/" + slug,
                    formatDate(robot.getUpdateTime()), "weekly", "0.8");
            count++;
        }

        // 品牌页
        List<Brand> brands = brandMapper.selectList(Wrappers.<Brand>lambdaQuery()
                .orderByDesc(Brand::getUpdateTime)
                .last("LIMIT " + (SITEMAP_LIMIT - count)));
        for (Brand brand : brands) {
            if (count >= SITEMAP_LIMIT) break;
            String slug = brand.getSlug() != null ? brand.getSlug() : ("brand-" + brand.getId());
            appendUrl(sb, base + "/brand/" + slug,
                    formatDate(brand.getUpdateTime()), "weekly", "0.7");
            count++;
        }

        // 企业页
        List<Company> companies = companyMapper.selectList(Wrappers.<Company>lambdaQuery()
                .eq(Company::getStatus, 1)
                .orderByDesc(Company::getUpdateTime)
                .last("LIMIT " + (SITEMAP_LIMIT - count)));
        for (Company company : companies) {
            if (count >= SITEMAP_LIMIT) break;
            String slug = company.getSlug() != null ? company.getSlug() : ("company-" + company.getId());
            appendUrl(sb, base + "/company/" + slug,
                    formatDate(company.getUpdateTime()), "weekly", "0.6");
            count++;
        }

        // 文章页
        List<Article> articles = articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getUpdateTime)
                .last("LIMIT " + (SITEMAP_LIMIT - count)));
        for (Article article : articles) {
            if (count >= SITEMAP_LIMIT) break;
            String slug = article.getSlug() != null ? article.getSlug() : ("article-" + article.getId());
            appendUrl(sb, base + "/article/" + slug,
                    formatDate(article.getUpdateTime()), "daily", "0.6");
            count++;
        }

        sb.append("</urlset>");
        log.info("sitemap生成完成: {}个URL", count);
        return sb.toString();
    }

    @Override
    public String generateRobotsTxt(String baseUrl) {
        String base = baseUrl == null || baseUrl.isEmpty() ? "https://www.example.com" : baseUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("User-agent: *\n");
        sb.append("Allow: /\n");
        sb.append("Disallow: /api/\n");
        sb.append("Disallow: /admin/\n");
        sb.append("Disallow: /user/\n");
        sb.append("\n");
        sb.append("Sitemap: ").append(base).append("/sitemap.xml\n");
        return sb.toString();
    }

    @Override
    public void recordPageView(String pageType, String pageSlug, String pagePath, Long entityId, Long userId,
                               String visitorId, String ipHash, String userAgent,
                               String refererSource, String refererUrl) {
        try {
            SeoPageView view = new SeoPageView();
            view.setPageType(pageType);
            view.setPageSlug(pageSlug);
            view.setPagePath(pagePath);
            view.setEntityId(entityId);
            view.setUserId(userId);
            view.setVisitorId(visitorId);
            view.setIpHash(ipHash);
            // User-Agent截断, 防止超长
            if (userAgent != null && userAgent.length() > 500) {
                userAgent = userAgent.substring(0, 500);
            }
            view.setUserAgent(userAgent);
            view.setRefererSource(refererSource);
            if (refererUrl != null && refererUrl.length() > 500) {
                refererUrl = refererUrl.substring(0, 500);
            }
            view.setRefererUrl(refererUrl);
            view.setViewTime(LocalDateTime.now());
            seoPageViewMapper.insert(view);
        } catch (Exception e) {
            // 访问记录失败不影响主流程
            log.warn("记录页面访问失败: pageType={}, error={}", pageType, e.getMessage());
        }
    }

    @Override
    public Map<String, Integer> backfillSlugs() {
        Map<String, Integer> stats = new HashMap<>();

        // Robot slug回填
        int robotUpdated = 0;
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .isNull(Robot::getSlug)
                .last("LIMIT 500"));
        for (Robot robot : robots) {
            String slug = generateUniqueRobotSlug(robot);
            if (slug != null) {
                Robot update = new Robot();
                update.setId(robot.getId());
                update.setSlug(slug);
                robotMapper.updateById(update);
                robotUpdated++;
            }
        }
        stats.put("robotUpdated", robotUpdated);

        // Brand slug回填
        int brandUpdated = 0;
        List<Brand> brands = brandMapper.selectList(Wrappers.<Brand>lambdaQuery()
                .isNull(Brand::getSlug)
                .last("LIMIT 500"));
        for (Brand brand : brands) {
            String slug = generateUniqueBrandSlug(brand);
            if (slug != null) {
                Brand update = new Brand();
                update.setId(brand.getId());
                update.setSlug(slug);
                brandMapper.updateById(update);
                brandUpdated++;
            }
        }
        stats.put("brandUpdated", brandUpdated);

        // Company slug回填
        int companyUpdated = 0;
        List<Company> companies = companyMapper.selectList(Wrappers.<Company>lambdaQuery()
                .isNull(Company::getSlug)
                .last("LIMIT 500"));
        for (Company company : companies) {
            String slug = generateUniqueCompanySlug(company);
            if (slug != null) {
                Company update = new Company();
                update.setId(company.getId());
                update.setSlug(slug);
                companyMapper.updateById(update);
                companyUpdated++;
            }
        }
        stats.put("companyUpdated", companyUpdated);

        // Article slug回填
        int articleUpdated = 0;
        List<Article> articles = articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                .isNull(Article::getSlug)
                .eq(Article::getStatus, 1)
                .last("LIMIT 500"));
        for (Article article : articles) {
            String slug = generateUniqueArticleSlug(article);
            if (slug != null) {
                Article update = new Article();
                update.setId(article.getId());
                update.setSlug(slug);
                articleMapper.updateById(update);
                articleUpdated++;
            }
        }
        stats.put("articleUpdated", articleUpdated);

        log.info("slug回填完成: {}", stats);
        return stats;
    }

    @Override
    public Map<String, Object> pageViewStats(int days) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        // 总访问量
        Long totalViews = seoPageViewMapper.selectCount(Wrappers.<SeoPageView>lambdaQuery()
                .ge(SeoPageView::getCreateTime, since));
        result.put("totalViews", totalViews != null ? totalViews : 0);

        // 按页面类型统计
        Map<String, Object> byPageType = new HashMap<>();
        List<SeoPageView> recentViews = seoPageViewMapper.selectList(Wrappers.<SeoPageView>lambdaQuery()
                .ge(SeoPageView::getCreateTime, since)
                .select(SeoPageView::getPageType, SeoPageView::getVisitorId)
                .last("LIMIT 10000"));
        Map<String, Integer> typeCounts = new HashMap<>();
        for (SeoPageView v : recentViews) {
            String type = v.getPageType() != null ? v.getPageType() : "unknown";
            typeCounts.merge(type, 1, Integer::sum);
        }
        byPageType.put("counts", typeCounts);
        // 独立访客数(按visitorId去重)
        long uniqueVisitors = recentViews.stream()
                .map(SeoPageView::getVisitorId)
                .filter(vid -> vid != null && !vid.isEmpty())
                .distinct()
                .count();
        byPageType.put("uniqueVisitors", uniqueVisitors);
        result.put("byPageType", byPageType);

        return result;
    }

    // ==================== 私有方法 ====================

    private void appendUrl(StringBuilder sb, String loc, String lastmod, String changefreq, String priority) {
        sb.append("  <url>\n");
        sb.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        if (lastmod != null) {
            sb.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        if (changefreq != null) {
            sb.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        }
        if (priority != null) {
            sb.append("    <priority>").append(priority).append("</priority>\n");
        }
        sb.append("  </url>\n");
    }

    private String formatDate(LocalDateTime time) {
        if (time == null) {
            return LocalDateTime.now().format(W3C_DATE);
        }
        return time.format(W3C_DATE);
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * 生成唯一robot slug, 冲突时追加-ID
     */
    private String generateUniqueRobotSlug(Robot robot) {
        String slug = SlugUtils.generate(robot.getName(), "robot", robot.getId());
        return ensureUnique(slug, "robot", robot.getId());
    }

    private String generateUniqueBrandSlug(Brand brand) {
        String slug = SlugUtils.generate(brand.getName(), "brand", brand.getId());
        return ensureUnique(slug, "brand", brand.getId());
    }

    private String generateUniqueCompanySlug(Company company) {
        String slug = SlugUtils.generate(company.getName(), "company", company.getId());
        return ensureUnique(slug, "company", company.getId());
    }

    private String generateUniqueArticleSlug(Article article) {
        String slug = SlugUtils.generate(article.getTitle(), "article", article.getId());
        return ensureUnique(slug, "article", article.getId());
    }

    /**
     * 确保slug唯一: 查库检查冲突, 冲突则追加实体ID
     */
    private String ensureUnique(String slug, String type, Long id) {
        if (slug == null || id == null) {
            return null;
        }
        // 检查同类型下是否有其他实体使用相同slug
        Long conflictCount;
        switch (type) {
            case "robot":
                conflictCount = robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                        .eq(Robot::getSlug, slug).ne(Robot::getId, id));
                break;
            case "brand":
                conflictCount = brandMapper.selectCount(Wrappers.<Brand>lambdaQuery()
                        .eq(Brand::getSlug, slug).ne(Brand::getId, id));
                break;
            case "company":
                conflictCount = companyMapper.selectCount(Wrappers.<Company>lambdaQuery()
                        .eq(Company::getSlug, slug).ne(Company::getId, id));
                break;
            case "article":
                conflictCount = articleMapper.selectCount(Wrappers.<Article>lambdaQuery()
                        .eq(Article::getSlug, slug).ne(Article::getId, id));
                break;
            default:
                conflictCount = 0L;
        }
        if (conflictCount != null && conflictCount > 0) {
            return slug + "-" + id;
        }
        return slug;
    }
}
