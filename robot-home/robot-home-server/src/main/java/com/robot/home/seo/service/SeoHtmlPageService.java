package com.robot.home.seo.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robot.home.article.entity.Article;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.ranking.service.RankingService;
import com.robot.home.ranking.vo.RankingTypeVO;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.entity.RobotCategory;
import com.robot.home.robot.mapper.RobotCategoryMapper;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.robot.vo.RobotListVO;
import com.robot.home.seo.util.SlugUtils;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Builds small, indexable HTML documents for public SEO routes.
 *
 * The renderer reads the same public records as the JSON APIs, but does not
 * record views or mutate counters when a crawler fetches a page.
 */
@Service
public class SeoHtmlPageService {

    private static final String SITE_NAME = "机器人之家";
    private static final String DEFAULT_CANONICAL_BASE = "https://www.example.com";
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,159}");

    private final RobotMapper robotMapper;
    private final BrandMapper brandMapper;
    private final CompanyMapper companyMapper;
    private final ArticleMapper articleMapper;
    private final RobotCategoryMapper categoryMapper;
    private final RankingService rankingService;
    private final ObjectMapper objectMapper;
    private final String canonicalBaseUrl;

    public SeoHtmlPageService(RobotMapper robotMapper,
                              BrandMapper brandMapper,
                              CompanyMapper companyMapper,
                              ArticleMapper articleMapper,
                              RobotCategoryMapper categoryMapper,
                              RankingService rankingService,
                              ObjectMapper objectMapper,
                              @Value("${seo.canonical-base-url:https://www.example.com}") String canonicalBaseUrl) {
        this.robotMapper = robotMapper;
        this.brandMapper = brandMapper;
        this.companyMapper = companyMapper;
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.rankingService = rankingService;
        this.objectMapper = objectMapper;
        this.canonicalBaseUrl = normalizeCanonicalBase(canonicalBaseUrl);
    }

    public Optional<String> renderRobot(String identifier) {
        Robot robot = findRobot(identifier);
        if (!isPublic(robot)) {
            return Optional.empty();
        }
        Brand brand = robot.getBrandId() == null ? null : brandMapper.selectById(robot.getBrandId());
        RobotCategory category = robot.getCategoryId() == null ? null : categoryMapper.selectById(robot.getCategoryId());
        String slug = canonicalSlug(robot.getSlug(), "robot", robot.getId());
        String title = first(robot.getSeoTitle(), robot.getName() + " - " + SITE_NAME);
        String description = description(first(robot.getSeoDescription(), robot.getSubtitle(), robot.getDetail()),
                robot.getName() + "机器人产品详情、参数与参考价格。机器人之家提供可信的机器人产品信息。");

        StringBuilder body = new StringBuilder();
        body.append("<article><h1>").append(escape(robot.getName())).append("</h1>");
        appendParagraph(body, robot.getSubtitle());
        body.append("<dl>");
        appendDefinition(body, "型号", robot.getModel());
        appendDefinition(body, "品牌", isPublic(brand) ? brand.getName() : null);
        appendDefinition(body, "分类", isPublic(category) ? category.getName() : null);
        appendDefinition(body, "参考价格", robot.getGuidePrice() == null ? null : robot.getGuidePrice().toPlainString());
        appendDefinition(body, "主要参数", robot.getMainParams());
        body.append("</dl>");
        appendParagraph(body, plainText(robot.getDetail()));
        body.append("</article>");

        Map<String, Object> jsonLd = baseStructuredData("Product", robot.getName(), description,
                canonical("/robots/" + slug));
        jsonLd.put("sku", first(robot.getModel(), String.valueOf(robot.getId())));
        if (isPublic(brand)) {
            Map<String, Object> brandData = new LinkedHashMap<>();
            brandData.put("@type", "Brand");
            brandData.put("name", brand.getName());
            jsonLd.put("brand", brandData);
        }
        if (robot.getGuidePrice() != null) {
            Map<String, Object> offer = new LinkedHashMap<>();
            offer.put("@type", "Offer");
            offer.put("priceCurrency", "CNY");
            offer.put("price", robot.getGuidePrice().toPlainString());
            offer.put("availability", "https://schema.org/InStock");
            jsonLd.put("offers", offer);
        }
        return Optional.of(page(title, description, robot.getSeoKeywords(), "/robots/" + slug,
                "product", robot.getCoverImage(), body.toString(), jsonLd));
    }

    public Optional<String> renderBrand(String identifier) {
        Brand brand = findBrand(identifier);
        if (!isPublic(brand)) {
            return Optional.empty();
        }
        String slug = canonicalSlug(brand.getSlug(), "brand", brand.getId());
        String title = first(brand.getSeoTitle(), brand.getName() + "品牌 - " + SITE_NAME);
        String description = description(first(brand.getSeoDescription(), brand.getIntro(), brand.getDetail()),
                brand.getName() + "品牌介绍与机器人产品信息。");
        StringBuilder body = new StringBuilder("<article><h1>").append(escape(brand.getName())).append("</h1><dl>");
        appendDefinition(body, "英文名", brand.getBrandNameEn());
        appendDefinition(body, "国家或地区", brand.getCountry());
        appendDefinition(body, "成立年份", brand.getFoundYear() == null ? null : String.valueOf(brand.getFoundYear()));
        appendDefinition(body, "产品数量", brand.getRobotCount() == null ? null : String.valueOf(brand.getRobotCount()));
        body.append("</dl>");
        appendParagraph(body, first(plainText(brand.getDetail()), brand.getIntro()));
        body.append("</article>");

        Map<String, Object> jsonLd = baseStructuredData("Brand", brand.getName(), description,
                canonical("/brands/" + slug));
        return Optional.of(page(title, description, brand.getSeoKeywords(), "/brands/" + slug,
                "website", brand.getLogo(), body.toString(), jsonLd));
    }

    public Optional<String> renderCompany(String identifier) {
        Company company = findCompany(identifier);
        if (!isPublic(company)) {
            return Optional.empty();
        }
        String slug = canonicalSlug(company.getSlug(), "company", company.getId());
        String title = first(company.getSeoTitle(), company.getName() + "企业 - " + SITE_NAME);
        String description = description(first(company.getSeoDescription(), company.getIntro(), company.getDetail()),
                company.getName() + "企业介绍、品牌与机器人产品信息。");
        StringBuilder body = new StringBuilder("<article><h1>").append(escape(company.getName())).append("</h1><dl>");
        appendDefinition(body, "英文名", company.getCompanyNameEn());
        appendDefinition(body, "地区", joinLocation(company.getProvince(), company.getCity(), company.getRegion()));
        appendDefinition(body, "成立年份", company.getFoundYear() == null ? null : String.valueOf(company.getFoundYear()));
        appendDefinition(body, "经营范围", company.getBusinessScope());
        appendDefinition(body, "认证状态", company.getVerificationStatus());
        body.append("</dl>");
        appendParagraph(body, first(plainText(company.getDetail()), company.getIntro()));
        body.append("</article>");

        Map<String, Object> jsonLd = baseStructuredData("Organization", company.getName(), description,
                canonical("/companies/" + slug));
        return Optional.of(page(title, description, company.getSeoKeywords(), "/companies/" + slug,
                "website", company.getLogo(), body.toString(), jsonLd));
    }

    public Optional<String> renderCategory(String identifier) {
        RobotCategory category = findCategory(identifier);
        if (!isPublic(category)) {
            return Optional.empty();
        }
        String slug = canonicalSlug(null, "category", category.getId(), category.getName());
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getCategoryId, category.getId())
                .eq(Robot::getStatus, 1)
                .orderByDesc(Robot::getHotScore)
                .last("LIMIT 20"));
        if (robots == null) {
            robots = Collections.emptyList();
        }
        String title = category.getName() + "分类 - " + SITE_NAME;
        String description = description(category.getName() + "机器人产品分类，包含热门型号、品牌与产品资料。", null);
        StringBuilder body = new StringBuilder("<section><h1>").append(escape(category.getName())).append("</h1>");
        body.append("<p>").append(escape(description)).append("</p>");
        appendRobotList(body, robotsToListItems(robots));
        body.append("</section>");
        Map<String, Object> jsonLd = itemListStructuredData(category.getName(), description,
                canonical("/categories/" + slug), robotsToListItems(robots));
        return Optional.of(page(title, description, null, "/categories/" + slug,
                "website", null, body.toString(), jsonLd));
    }

    public Optional<String> renderRanking(String identifier) {
        if (!valid(identifier)) {
            return Optional.empty();
        }
        RankingTypeVO selected = null;
        List<RankingTypeVO> types = rankingService.types();
        if (types != null) {
            for (RankingTypeVO type : types) {
                if (type != null && identifier.equals(type.getCode())) {
                    selected = type;
                    break;
                }
            }
        }
        if (selected == null) {
            return Optional.empty();
        }
        List<RobotListVO> robots = rankingService.rank(identifier, 20, null, "all");
        if (robots == null) {
            robots = Collections.emptyList();
        }
        List<RobotListVO> publicRobots = new ArrayList<>();
        for (RobotListVO robot : robots) {
            if (robot != null && Integer.valueOf(1).equals(robot.getStatus())) {
                publicRobots.add(robot);
            }
        }
        String title = selected.getName() + " - " + SITE_NAME;
        String description = selected.getName() + "，查看机器人之家基于公开产品数据整理的机器人排行。";
        StringBuilder body = new StringBuilder("<section><h1>").append(escape(selected.getName())).append("</h1>");
        body.append("<p>").append(escape(description)).append("</p>");
        appendRobotList(body, publicRobots);
        body.append("</section>");
        Map<String, Object> jsonLd = itemListStructuredData(selected.getName(), description,
                canonical("/rankings/" + identifier), publicRobots);
        return Optional.of(page(title, description, null, "/rankings/" + identifier,
                "website", null, body.toString(), jsonLd));
    }

    public Optional<String> renderArticle(String identifier) {
        Article article = findArticle(identifier);
        if (!isPublic(article)) {
            return Optional.empty();
        }
        String slug = canonicalSlug(article.getSlug(), "article", article.getId());
        String description = description(first(article.getSummary(), plainText(article.getContent())),
                article.getTitle() + "，机器人行业资讯与观点。");
        StringBuilder body = new StringBuilder("<article><h1>").append(escape(article.getTitle())).append("</h1>");
        body.append("<p class=\"byline\">");
        if (article.getAuthor() != null) {
            body.append(escape(article.getAuthor()));
        }
        if (article.getPublishTime() != null) {
            body.append(" <time datetime=\"").append(escape(article.getPublishTime().toString())).append("\">")
                    .append(escape(article.getPublishTime().toLocalDate().toString())).append("</time>");
        }
        body.append("</p>");
        appendParagraph(body, first(plainText(article.getContent()), article.getSummary()));
        body.append("</article>");

        Map<String, Object> jsonLd = baseStructuredData("Article", article.getTitle(), description,
                canonical("/articles/" + slug));
        jsonLd.put("headline", article.getTitle());
        if (article.getAuthor() != null) {
            Map<String, Object> author = new LinkedHashMap<>();
            author.put("@type", "Person");
            author.put("name", article.getAuthor());
            jsonLd.put("author", author);
        }
        if (article.getPublishTime() != null) {
            jsonLd.put("datePublished", article.getPublishTime().toString());
        }
        return Optional.of(page(article.getTitle() + " - " + SITE_NAME, description, article.getTags(),
                "/articles/" + slug, "article", article.getCover(), body.toString(), jsonLd));
    }

    private Robot findRobot(String identifier) {
        if (!valid(identifier)) return null;
        Long id = idFrom(identifier, "robot");
        return id == null ? robotMapper.selectOne(Wrappers.<Robot>lambdaQuery()
                .eq(Robot::getSlug, identifier).eq(Robot::getStatus, 1).last("LIMIT 1")) : robotMapper.selectById(id);
    }

    private Brand findBrand(String identifier) {
        if (!valid(identifier)) return null;
        Long id = idFrom(identifier, "brand");
        return id == null ? brandMapper.selectOne(Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getSlug, identifier).eq(Brand::getStatus, 1).last("LIMIT 1")) : brandMapper.selectById(id);
    }

    private Company findCompany(String identifier) {
        if (!valid(identifier)) return null;
        Long id = idFrom(identifier, "company");
        return id == null ? companyMapper.selectOne(Wrappers.<Company>lambdaQuery()
                .eq(Company::getSlug, identifier).eq(Company::getStatus, 1).last("LIMIT 1")) : companyMapper.selectById(id);
    }

    private Article findArticle(String identifier) {
        if (!valid(identifier)) return null;
        Long id = idFrom(identifier, "article");
        return id == null ? articleMapper.selectOne(Wrappers.<Article>lambdaQuery()
                .eq(Article::getSlug, identifier).eq(Article::getStatus, 1).last("LIMIT 1")) : articleMapper.selectById(id);
    }

    private RobotCategory findCategory(String identifier) {
        if (!valid(identifier)) return null;
        Long id = idFrom(identifier, "category");
        if (id != null) {
            return categoryMapper.selectById(id);
        }
        List<RobotCategory> categories = categoryMapper.selectList(Wrappers.<RobotCategory>lambdaQuery()
                .eq(RobotCategory::getStatus, 1));
        if (categories != null) {
            for (RobotCategory category : categories) {
                if (identifier.equals(canonicalSlug(null, "category", category.getId(), category.getName()))) {
                    return category;
                }
            }
        }
        return null;
    }

    private String page(String title, String description, String keywords, String path, String ogType,
                        String image, String body, Map<String, Object> jsonLd) {
        String canonical = canonical(path);
        String safeImage = safeImage(image);
        StringBuilder html = new StringBuilder(2048);
        html.append("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>").append(escape(title)).append("</title>")
                .append("<meta name=\"description\" content=\"").append(escape(description)).append("\">");
        if (keywords != null && !keywords.trim().isEmpty()) {
            html.append("<meta name=\"keywords\" content=\"").append(escape(keywords)).append("\">");
        }
        html.append("<link rel=\"canonical\" href=\"").append(escape(canonical)).append("\">")
                .append("<meta property=\"og:site_name\" content=\"").append(SITE_NAME).append("\">")
                .append("<meta property=\"og:locale\" content=\"zh_CN\">")
                .append("<meta property=\"og:type\" content=\"").append(escape(ogType)).append("\">")
                .append("<meta property=\"og:title\" content=\"").append(escape(title)).append("\">")
                .append("<meta property=\"og:description\" content=\"").append(escape(description)).append("\">")
                .append("<meta property=\"og:url\" content=\"").append(escape(canonical)).append("\">");
        if (safeImage != null) {
            html.append("<meta property=\"og:image\" content=\"").append(escape(safeImage)).append("\">");
        }
        html.append("<script type=\"application/ld+json\">").append(toSafeJson(jsonLd)).append("</script>")
                .append("<style>body{font-family:system-ui,sans-serif;max-width:960px;margin:auto;padding:24px;line-height:1.7;color:#222}a{color:#1769aa}dl{display:grid;grid-template-columns:max-content 1fr;gap:8px 20px}dt{font-weight:700}ol{padding-left:28px}</style>")
                .append("</head><body><header><a href=\"/\">").append(SITE_NAME).append("</a></header><main>")
                .append(body).append("</main></body></html>");
        return html.toString();
    }

    private Map<String, Object> baseStructuredData(String type, String name, String description, String url) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("@context", "https://schema.org");
        data.put("@type", type);
        data.put("name", name);
        data.put("description", description);
        data.put("url", url);
        return data;
    }

    private Map<String, Object> itemListStructuredData(String name, String description, String url,
                                                       List<RobotListVO> robots) {
        Map<String, Object> data = baseStructuredData("ItemList", name, description, url);
        List<Map<String, Object>> elements = new ArrayList<>();
        int position = 1;
        for (RobotListVO robot : robots) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("@type", "ListItem");
            item.put("position", position++);
            item.put("name", robot.getName());
            item.put("url", canonical("/robots/robot-" + robot.getId()));
            elements.add(item);
        }
        data.put("itemListElement", elements);
        return data;
    }

    private List<RobotListVO> robotsToListItems(List<Robot> robots) {
        List<RobotListVO> items = new ArrayList<>();
        for (Robot robot : robots) {
            if (!isPublic(robot)) continue;
            RobotListVO item = new RobotListVO();
            item.setId(robot.getId());
            item.setName(robot.getName());
            item.setModel(robot.getModel());
            item.setStatus(robot.getStatus());
            items.add(item);
        }
        return items;
    }

    private void appendRobotList(StringBuilder body, List<RobotListVO> robots) {
        body.append("<ol>");
        for (RobotListVO robot : robots) {
            body.append("<li><a href=\"/robots/robot-").append(robot.getId()).append("\">")
                    .append(escape(robot.getName())).append("</a>");
            if (robot.getModel() != null) {
                body.append(" <span>").append(escape(robot.getModel())).append("</span>");
            }
            body.append("</li>");
        }
        body.append("</ol>");
    }

    private void appendParagraph(StringBuilder body, String value) {
        String text = plainText(value);
        if (text != null && !text.isEmpty()) {
            body.append("<p>").append(escape(text)).append("</p>");
        }
    }

    private void appendDefinition(StringBuilder body, String label, String value) {
        String text = plainText(value);
        if (text != null && !text.isEmpty()) {
            body.append("<dt>").append(escape(label)).append("</dt><dd>").append(escape(text)).append("</dd>");
        }
    }

    private boolean isPublic(Robot value) { return value != null && Integer.valueOf(1).equals(value.getStatus()); }
    private boolean isPublic(Brand value) { return value != null && Integer.valueOf(1).equals(value.getStatus()); }
    private boolean isPublic(Company value) { return value != null && Integer.valueOf(1).equals(value.getStatus()); }
    private boolean isPublic(Article value) { return value != null && Integer.valueOf(1).equals(value.getStatus()); }
    private boolean isPublic(RobotCategory value) { return value != null && Integer.valueOf(1).equals(value.getStatus()); }

    private boolean valid(String identifier) {
        return identifier != null && IDENTIFIER.matcher(identifier).matches();
    }

    private Long idFrom(String identifier, String prefix) {
        String value = identifier;
        if (identifier.startsWith(prefix + "-")) {
            value = identifier.substring(prefix.length() + 1);
        }
        if (!value.matches("[1-9][0-9]{0,18}")) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String canonicalSlug(String stored, String type, Long id) {
        return canonicalSlug(stored, type, id, null);
    }

    private String canonicalSlug(String stored, String type, Long id, String name) {
        if (valid(stored)) return stored;
        String generated = SlugUtils.generate(name, type, id);
        return valid(generated) ? generated : type + "-" + id;
    }

    private String canonical(String path) {
        return canonicalBaseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private String safeImage(String image) {
        if (image == null || image.trim().isEmpty()) return null;
        String value = image.trim();
        if (value.startsWith("/")) return canonicalBaseUrl + value;
        try {
            URI uri = URI.create(value);
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null) {
                return value;
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid image URLs are omitted from metadata.
        }
        return null;
    }

    private String toSafeJson(Map<String, Object> jsonLd) {
        try {
            return objectMapper.writeValueAsString(jsonLd)
                    .replace("&", "\\u0026")
                    .replace("<", "\\u003c")
                    .replace(">", "\\u003e");
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String description(String preferred, String fallback) {
        String value = first(plainText(preferred), plainText(fallback), SITE_NAME);
        return value.length() > 180 ? value.substring(0, 177) + "..." : value;
    }

    private String plainText(String value) {
        if (value == null) return null;
        String text = Jsoup.parse(value).text().trim();
        return text.isEmpty() ? null : text;
    }

    private String first(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private String joinLocation(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty() && result.indexOf(value.trim()) < 0) {
                if (result.length() > 0) result.append(' ');
                result.append(value.trim());
            }
        }
        return result.toString();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String normalizeCanonicalBase(String configured) {
        String candidate = configured == null ? "" : configured.trim();
        try {
            URI uri = URI.create(candidate);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                return DEFAULT_CANONICAL_BASE;
            }
            StringBuilder normalized = new StringBuilder(uri.getScheme().toLowerCase()).append("://")
                    .append(uri.getHost().toLowerCase());
            if (uri.getPort() >= 0) normalized.append(':').append(uri.getPort());
            String path = uri.getPath();
            if (path != null && !path.isEmpty() && !"/".equals(path)) {
                normalized.append(path.endsWith("/") ? path.substring(0, path.length() - 1) : path);
            }
            return normalized.toString();
        } catch (IllegalArgumentException ignored) {
            return DEFAULT_CANONICAL_BASE;
        }
    }
}
