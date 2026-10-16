package com.robot.home.seo;

import com.robot.home.TestDataSourceInitializer;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stage 5: SEO 真实 HTTP 验收
 *
 * 通过实际 HTTP 请求（非 MockMvc）验证 SEO 页面的可索引性。
 * 使用 WebEnvironment.RANDOM_PORT 启动真实嵌入式服务器。
 * 注意：不使用@Transactional，因为HTTP请求运行在独立线程/事务中，
 * 测试事务内的数据对请求处理线程不可见。
 *
 * Nginx 运行时验证为 NGINX_RUNTIME_BLOCKED（本地无 Nginx 环境），
 * 本测试仅覆盖 Spring Boot HTTP 层。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestDataSourceInitializer.class)
class SeoRealHttpVerificationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private RobotMapper robotMapper;
    @Autowired
    private BrandMapper brandMapper;
    @Autowired
    private CompanyMapper companyMapper;

    @MockBean
    private RedisUtils redisUtils;

    private static final String ROBOT_SLUG = "real-http-test-robot";
    private static final String BRAND_SLUG = "real-http-test-brand";
    private static final String COMPANY_SLUG = "real-http-test-company";

    @BeforeEach
    void seedSeoData() {
        // 清理可能残留的旧数据（无@Transactional，数据跨测试持久）
        cleanupSeoData();

        // 种子 Brand
        Brand brand = new Brand();
        brand.setName("真HTTP测试品牌");
        brand.setSlug(BRAND_SLUG);
        brand.setStatus(1);
        brandMapper.insert(brand);

        // 种子 Company
        Company company = new Company();
        company.setName("真HTTP测试企业");
        company.setSlug(COMPANY_SLUG);
        company.setStatus(1);
        company.setVerificationStatus("VERIFIED");
        companyMapper.insert(company);

        // 种子 Robot (公开可见)
        Robot robot = new Robot();
        robot.setName("真HTTP测试机器人");
        robot.setSlug(ROBOT_SLUG);
        robot.setStatus(1);
        robot.setBrandId(brand.getId());
        robot.setCategoryId(1L);
        robotMapper.insert(robot);
    }

    @AfterEach
    void cleanupSeoData() {
        // 清理本测试创建的数据（含各测试中动态创建的机器人）
        robotMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Robot>lambdaQuery()
                .likeRight(Robot::getSlug, "real-http-test-")
                .or().likeRight(Robot::getSlug, "offline-robot-")
                .or().likeRight(Robot::getSlug, "xss-escape-"));
        brandMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Brand>lambdaQuery()
                .eq(Brand::getSlug, BRAND_SLUG));
        companyMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.<Company>lambdaQuery()
                .eq(Company::getSlug, COMPANY_SLUG));
    }

    @Test
    void robotPageReturnsRealHtmlWithSeoMetadata() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/robots/" + ROBOT_SLUG, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        String html = resp.getBody();
        assertTrue(html != null && html.length() > 200, "HTML内容不应为空壳");
        assertTrue(html.contains("<title"), "必须包含title标签");
        assertTrue(html.contains("name=\"description\""), "必须包含description meta");
        assertTrue(html.contains("rel=\"canonical\""), "必须包含canonical");
        assertTrue(html.contains("og:"), "必须包含OpenGraph");
        assertTrue(html.contains("application/ld+json"), "必须包含JSON-LD");
        assertTrue(html.contains("真HTTP测试机器人"), "HTML必须包含真实业务内容");
        // SEO页面不应包含SPA JavaScript引用（不含src=的script标签）
        assertFalse(html.contains("<script src="), "SEO页面不应引用SPA JavaScript");
    }

    @Test
    void brandPageReturnsRealHtmlWithSeoMetadata() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/brands/" + BRAND_SLUG, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        String html = resp.getBody();
        assertTrue(html != null && html.contains("<title"), "品牌页必须包含title");
        assertTrue(html.contains("rel=\"canonical\""), "品牌页必须包含canonical");
        assertTrue(html.contains("真HTTP测试品牌"), "品牌页必须包含真实品牌名");
    }

    @Test
    void companyPageReturnsRealHtmlWithSeoMetadata() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/companies/" + COMPANY_SLUG, String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        String html = resp.getBody();
        assertTrue(html != null && html.contains("<title"), "企业页必须包含title");
        assertTrue(html.contains("rel=\"canonical\""), "企业页必须包含canonical");
        assertTrue(html.contains("真HTTP测试企业"), "企业页必须包含真实企业名");
    }

    @Test
    void nonExistentSlugReturns404() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/robots/this-slug-does-not-exist-999", String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void offlineRobotIsNotIndexable() {
        // 创建一个下架的机器人（status=0）
        Robot offline = new Robot();
        offline.setName("已下架机器人");
        offline.setSlug("offline-robot-not-indexable");
        offline.setStatus(0);
        robotMapper.insert(offline);

        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/robots/offline-robot-not-indexable", String.class);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode(),
                "下架内容不可索引，应返回404");
    }

    @Test
    void sitemapXmlReturnsXmlContent() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/seo/sitemap.xml", String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        String body = resp.getBody();
        assertTrue(body != null && body.contains("<urlset"), "sitemap.xml必须包含urlset");
        assertTrue(body.contains("<loc>"), "sitemap.xml必须包含URL条目");
    }

    @Test
    void robotsTxtReturnsPlainText() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/seo/robots.txt", String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        String body = resp.getBody();
        assertTrue(body != null && body.contains("User-agent"), "robots.txt必须包含User-agent");
    }

    @Test
    void htmlContentIsEscaped() {
        // 创建名称含HTML特殊字符的机器人
        Robot xssRobot = new Robot();
        xssRobot.setName("<script>alert(1)</script>转义测试");
        xssRobot.setSlug("xss-escape-test-robot");
        xssRobot.setStatus(1);
        robotMapper.insert(xssRobot);

        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/robots/xss-escape-test-robot", String.class);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        String html = resp.getBody();
        assertFalse(html.contains("<script>alert(1)</script>"),
                "原始HTML标签必须被转义，不允许XSS注入");
        assertTrue(html.contains("&lt;script&gt;"),
                "HTML特殊字符必须被转义输出");
    }
}