package com.robot.home;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robot.home.common.util.RedisUtils;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 端到端接口联调测试：
 * 基于真实 SQL 脚本（H2 兼容模式执行）+ 真实 Controller / Service / Mapper，
 * 覆盖 PC / 小程序 / 后台三端的核心业务链路。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataSourceInitializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RobotHomeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Redis 未在本地启动，测试中以内存 Map 打桩，
     * 保证验证码、权限缓存、榜单缓存等依赖 Redis 的流程可被真实执行
     */
    @MockBean
    private RedisUtils redisUtils;

    private static final Map<String, String> REDIS_STORE = new ConcurrentHashMap<>();

    private static String userToken;
    private static String adminToken;
    private static Long robotId;
    private static Long articleId;

    private JsonNode data(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        assertEquals(200, root.path("code").asInt(), "接口返回异常: " + json);
        return root.path("data");
    }

    private JsonNode getJson(String url, Object... uriVars) throws Exception {
        return data(exec(get(url, uriVars)));
    }

    // ---------------- 首页与基础数据 ----------------

    @BeforeEach
    void setUpRedisStub() {
        REDIS_STORE.clear();
        given(redisUtils.get(anyString())).willAnswer(inv -> REDIS_STORE.get(inv.getArgument(0)));
        given(redisUtils.hasKey(anyString())).willAnswer(inv -> REDIS_STORE.containsKey(inv.getArgument(0)));
        willAnswer(inv -> {
            REDIS_STORE.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).given(redisUtils).set(anyString(), anyString());
        willAnswer(inv -> {
            REDIS_STORE.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).given(redisUtils).set(anyString(), anyString(), anyLong(), any());
        willAnswer(inv -> REDIS_STORE.remove(inv.getArgument(0)) != null)
                .given(redisUtils).delete(anyString());
        willAnswer(inv -> {
            REDIS_STORE.clear();
            return null;
        }).given(redisUtils).delete(any(java.util.Collection.class));
        given(redisUtils.keys(anyString())).willAnswer(inv -> {
            String prefix = inv.getArgument(0);
            String p = prefix.replace("*", "");
            Set<String> matched = new java.util.HashSet<>();
            for (String k : REDIS_STORE.keySet()) {
                if (k.startsWith(p)) {
                    matched.add(k);
                }
            }
            return matched;
        });
    }

    @Test
    @Order(1)
    void test01_homeIndex() throws Exception {
        JsonNode d = getJson("/api/home/index?position=pc");
        assertTrue(d.path("banners").isArray(), "首页 Banner 缺失");
        assertTrue(d.path("quickNav").isArray() && d.path("quickNav").size() > 0, "首页快捷导航缺失");
        assertTrue(d.path("hotRobots").size() > 0, "热门机器人为空");
        assertTrue(d.path("newRobots").size() > 0, "新品机器人为空");
        assertTrue(d.path("hotBrands").size() >= 10, "热门品牌不足 10 个");
        assertTrue(d.path("articles").size() > 0, "首页资讯为空");
        assertTrue(d.path("videos").size() > 0, "首页视频为空");
        assertTrue(d.path("companies").size() > 0, "推荐企业为空");
        assertTrue(d.path("rankings").size() >= 4, "首页榜单区块缺失");
        robotId = d.path("hotRobots").get(0).path("id").asLong();
    }

    @Test
    @Order(2)
    void test02_robotLibrary() throws Exception {
        JsonNode page = getJson("/api/robots?pageNum=1&pageSize=10");
        assertTrue(page.path("total").asInt() >= 20, "机器人数量不足 20 台，实际 " + page.path("total").asInt());
        assertEquals(10, page.path("list").size());

        // 分类筛选（一级分类应包含子分类）
        JsonNode byCategory = getJson("/api/robots?categoryId=1&pageSize=50");
        assertTrue(byCategory.path("total").asInt() > 0, "人形机器人分类筛选无结果");

        // 场景 / 开发 / AI 多维筛选
        JsonNode byScene = getJson("/api/robots?scenes=科研教育&pageSize=50");
        assertTrue(byScene.path("total").asInt() > 0, "使用场景筛选无结果");
        JsonNode byDev = getJson("/api/robots?devs=ROS2&pageSize=50");
        assertTrue(byDev.path("total").asInt() > 0, "开发能力筛选无结果");
        JsonNode byAi = getJson("/api/robots?ais=大模型&pageSize=50");
        assertTrue(byAi.path("total").asInt() > 0, "AI 能力筛选无结果");

        // 价格与排序
        JsonNode byPrice = getJson("/api/robots?minPrice=1000&maxPrice=100000&sort=price_asc&pageSize=50");
        assertTrue(byPrice.path("total").asInt() > 0, "价格区间筛选无结果");

        // 关键词
        JsonNode byKeyword = getJson("/api/robots?keyword=宇树&pageSize=50");
        assertTrue(byKeyword.path("total").asInt() > 0, "关键词筛选无结果");
    }

    @Test
    @Order(3)
    void test03_robotDetail() throws Exception {
        JsonNode d = getJson("/api/robots/{id}", robotId);
        assertNotNull(d.path("robot").path("name").asText());
        assertTrue(d.path("robot").path("name").asText().length() > 0, "机器人名称为空");
        assertTrue(d.path("brandName").asText().length() > 0, "品牌名缺失");
        assertTrue(d.path("paramGroups").isArray() && d.path("paramGroups").size() > 0, "参数分组为空");
        assertTrue(d.path("images").isArray(), "图片列表缺失");

        // 参数分组接口
        JsonNode params = getJson("/api/robots/{id}/params", robotId);
        assertTrue(params.size() > 0, "参数接口无数据");

        // 图片 / 视频 / 资讯
        assertNotNull(getJson("/api/robots/{id}/images", robotId));
        assertNotNull(getJson("/api/robots/{id}/videos", robotId));
        assertNotNull(getJson("/api/robots/{id}/articles", robotId));
    }

    @Test
    @Order(4)
    void test04_compare() throws Exception {
        JsonNode d = getJson("/api/robots/compare?ids=1,2,3,7");
        assertEquals(4, d.path("robots").size(), "对比应返回 4 台机器人");
        assertTrue(d.path("groups").size() > 0, "对比参数分组为空");
        // 超过 4 台应被拒绝
        String r = exec(get("/api/robots/compare?ids=1,2,3,4,5"));
        JsonNode root = objectMapper.readTree(r);
        assertEquals(500, root.path("code").asInt(), "超过 4 台对比时应报错");
    }

    @Test
    @Order(5)
    void test05_filtersAndRankings() throws Exception {
        JsonNode f = getJson("/api/robots/filters");
        assertTrue(f.path("categories").size() >= 9, "一级分类不足 9 个");
        assertTrue(f.path("scenes").size() > 0, "场景筛选项为空");
        assertTrue(f.path("devs").size() > 0, "开发能力筛选项为空");
        assertTrue(f.path("priceRanges").size() == 5, "价格区间应为 5 档");

        JsonNode types = getJson("/api/rankings/types");
        assertEquals(7, types.size(), "榜单类型应为 7 个");
        JsonNode hot = getJson("/api/rankings?type=hot&limit=10");
        assertTrue(hot.size() > 0, "热门榜为空");
        JsonNode humanoid = getJson("/api/rankings?type=humanoid&limit=10");
        assertTrue(humanoid.size() > 0, "人形机器人榜为空");
    }

    @Test
    @Order(6)
    void test06_brandAndCompany() throws Exception {
        JsonNode brands = getJson("/api/brands?pageSize=50");
        assertTrue(brands.path("total").asInt() >= 10, "品牌数量不足 10 个");
        JsonNode letters = getJson("/api/brands/letters");
        assertTrue(letters.size() > 0, "品牌字母索引为空");
        JsonNode brandDetail = getJson("/api/brands/{id}", 1L);
        assertTrue(brandDetail.path("brand").path("name").asText().length() > 0);
        assertTrue(brandDetail.path("products").size() > 0, "品牌下产品为空");

        JsonNode companies = getJson("/api/companies?pageSize=50");
        assertTrue(companies.path("total").asInt() >= 10, "企业数量不足 10 个");
        JsonNode companyDetail = getJson("/api/companies/{id}", 1L);
        assertTrue(companyDetail.path("company").path("name").asText().length() > 0);
        assertTrue(companyDetail.path("brandList").size() > 0, "企业下品牌为空");
    }

    @Test
    @Order(7)
    void test07_content() throws Exception {
        JsonNode articles = getJson("/api/articles?pageSize=50");
        assertTrue(articles.path("total").asInt() >= 20, "资讯数量不足 20 篇");
        articleId = articles.path("list").get(0).path("id").asLong();
        JsonNode detail = getJson("/api/articles/{id}", articleId);
        assertTrue(detail.path("title").asText().length() > 0, "资讯标题为空");
        assertTrue(detail.path("content").asText().length() > 0, "资讯正文为空");
        assertTrue(getJson("/api/articles/categories").size() > 0, "资讯栏目为空");

        JsonNode videos = getJson("/api/videos?pageSize=50");
        assertTrue(videos.path("total").asInt() >= 10, "视频数量不足 10 个");
        assertNotNull(getJson("/api/videos/categories"));

        JsonNode tutorials = getJson("/api/tutorials?pageSize=50");
        assertTrue(tutorials.path("total").asInt() >= 10, "教程数量不足 10 篇");
        assertNotNull(getJson("/api/tutorials/categories"));
    }

    @Test
    @Order(8)
    void test08_communityAndSearch() throws Exception {
        JsonNode circles = getJson("/api/community/circles");
        assertTrue(circles.size() >= 9, "圈子数量不足 9 个");
        JsonNode posts = getJson("/api/community/posts?pageSize=50");
        assertTrue(posts.path("total").asInt() >= 20, "帖子数量不足 20 个");
        JsonNode post = getJson("/api/community/posts/{id}", posts.path("list").get(0).path("id").asLong());
        assertTrue(post.path("content").asText().length() > 0, "帖子内容为空");

        JsonNode search = getJson("/api/search?keyword=宇树&limit=5");
        assertTrue(search.path("robots").size() > 0, "搜索机器人无结果");
        assertTrue(search.path("counts").path("robot").asInt() > 0, "搜索统计异常");
        assertTrue(getJson("/api/search/robot?keyword=宇树&pageSize=20").path("total").asInt() > 0, "分类搜索无结果");
        assertTrue(getJson("/api/search/hot").size() > 0, "热搜为空");
    }

    // ---------------- 用户链路 ----------------

    @Test
    @Order(10)
    void test10_registerAndLogin() throws Exception {
        String phone = "138" + String.format("%08d", System.currentTimeMillis() % 100000000L);
        mockMvc.perform(post("/api/auth/register")
                .param("username", "tester" + phone)
                .param("phone", phone)
                .param("password", "test123456")
                .param("nickname", "测试用户"));
        String r = exec(post("/api/auth/login")
                .param("username", "tester" + phone)
                .param("password", "test123456"));
        JsonNode root = objectMapper.readTree(r);
        assertEquals(200, root.path("code").asInt(), "登录失败: " + r);
        userToken = root.path("data").path("token").asText();
        assertTrue(userToken.length() > 20, "未返回 token");

        // 手机号 + 验证码登录（从 Redis mock 获取验证码）
        exec(post("/api/auth/send-sms-code").param("phone", phone));
        String code = REDIS_STORE.get("robot:sms:code:" + phone);
        String smsLogin = exec(post("/api/auth/sms-login").param("phone", phone).param("code", code));
        assertEquals(200, objectMapper.readTree(smsLogin).path("code").asInt(),
                "短信登录失败");

        // 微信登录（小程序）
        String wx = exec(post("/api/auth/wx-login").param("openid", "wx-openid-" + phone));
        assertEquals(200, objectMapper.readTree(wx).path("code").asInt(), "微信登录失败");
    }

    @Test
    @Order(11)
    void test11_userInteractions() throws Exception {
        String auth = "Bearer " + userToken;

        // 收藏切换
        String fav = exec(post("/api/favorites").header("Authorization", auth)
                .param("bizType", "robot").param("bizId", String.valueOf(robotId)));
        assertTrue(objectMapper.readTree(fav)
                .path("data").path("favorited").asBoolean(), "收藏失败，响应: " + fav);
        JsonNode favList = getJson2("/api/favorites?pageSize=20", auth);
        assertTrue(favList.path("total").asInt() > 0, "我的收藏为空");

        // 点赞
        String like = exec(post("/api/likes").header("Authorization", auth)
                .param("bizType", "robot").param("bizId", String.valueOf(robotId)));
        assertTrue(objectMapper.readTree(like)
                .path("data").path("liked").asBoolean(), "点赞失败");

        // 关注
        String follow = exec(post("/api/follows").header("Authorization", auth)
                .param("followType", "robot").param("followId", String.valueOf(robotId)));
        assertTrue(objectMapper.readTree(follow)
                .path("data").path("followed").asBoolean(), "关注失败");

        // 评论（一级）
        String body = "{\"bizType\":\"robot\",\"bizId\":" + robotId + ",\"content\":\"这是一条集成测试评论\"}";
        String comment = exec(post("/api/comments").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body));
        JsonNode cRoot = objectMapper.readTree(comment);
        assertEquals(200, cRoot.path("code").asInt(), "评论失败: " + comment);
        long commentId = cRoot.path("data").path("id").asLong();

        // 回复评论
        String reply = "{\"bizType\":\"robot\",\"bizId\":" + robotId + ",\"content\":\"回复内容\","
                + "\"parentId\":" + commentId + "}";
        String replyResult = exec(post("/api/comments").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(reply));
        assertEquals(200, objectMapper.readTree(replyResult).path("code").asInt());

        JsonNode comments = getJson2("/api/comments?bizType=robot&bizId=" + robotId, auth);
        assertTrue(comments.path("total").asInt() > 0, "评论列表为空");

        // 浏览历史：登录态下访问详情才会写入
        mockMvc.perform(get("/api/robots/{id}", robotId).header("Authorization", auth)
                .characterEncoding(StandardCharsets.UTF_8.name()));
        JsonNode history = getJson2("/api/history?pageSize=20", auth);
        assertTrue(history.path("total").asInt() > 0, "浏览历史为空，响应: " + history.toString());

        // 用户中心
        JsonNode me = getJson2("/api/users/me", auth);
        assertTrue(me.path("id").asLong() > 0, "用户中心返回异常");
        JsonNode stats = getJson2("/api/users/me/stats", auth);
        assertTrue(stats.path("favoriteCount").asInt() > 0, "用户统计异常");

        // 消息
        assertNotNull(getJson2("/api/messages/unread-count", auth));
    }

    @Test
    @Order(12)
    void test12_inquiry() throws Exception {
        String auth = "Bearer " + userToken;
        String body = "{\"robotId\":" + robotId + ",\"name\":\"张三\",\"phone\":\"13900001111\","
                + "\"region\":\"北京\",\"customerType\":2,\"companyName\":\"某某科技\",\"quantity\":5,"
                + "\"budget\":\"50万\",\"remark\":\"希望尽快联系\"}";
        String r = exec(post("/api/inquiries").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body));
        assertEquals(200, objectMapper.readTree(r).path("code").asInt(),
                "提交询价失败: " + r);

        JsonNode my = getJson2("/api/inquiries/my?pageSize=20", auth);
        assertTrue(my.path("total").asInt() > 0, "我的询价为空");
        // 手机号应脱敏
        assertTrue(my.path("list").get(0).path("phone").asText().contains("****"), "手机号未脱敏");
    }

    @Test
    @Order(13)
    void test13_postCreate() throws Exception {
        String auth = "Bearer " + userToken;
        String body = "{\"circleId\":1,\"title\":\"集成测试帖子\",\"content\":\"帖子正文内容\","
                + "\"robotId\":" + robotId + ",\"topic\":\"测试\"}";
        String r = exec(post("/api/community/posts").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body));
        assertEquals(200, objectMapper.readTree(r).path("code").asInt(),
                "发帖失败: " + r);

        JsonNode myPosts = getJson2("/api/community/my-posts?pageSize=20", auth);
        assertTrue(myPosts.path("total").asInt() > 0, "我的帖子为空");
    }

    // ---------------- 后台链路 ----------------

    @Test
    @Order(20)
    void test20_adminLogin() throws Exception {
        String r = exec(post("/api/admin/auth/login")
                .param("username", "admin").param("password", "admin123"));
        JsonNode root = objectMapper.readTree(r);
        assertEquals(200, root.path("code").asInt(), "后台登录失败: " + r);
        adminToken = root.path("data").path("token").asText();
        assertTrue(adminToken.length() > 20, "后台未返回 token");

        // 未登录访问后台接口应被拦截
        String denied = exec(get("/api/admin/dashboard/stats"));
        assertFalse(denied.contains("\"code\":200"), "未登录不应访问后台接口");
    }

    @Test
    @Order(21)
    void test21_adminInfo() throws Exception {
        String auth = "Bearer " + adminToken;
        JsonNode info = getJson2("/api/admin/auth/info", auth);
        assertTrue(info.path("permissions").size() > 0, "后台权限为空");
        assertTrue(info.path("menus").size() > 0, "后台菜单为空");
        assertTrue(info.path("roles").toString().contains("ADMIN"), "角色信息异常");
    }

    @Test
    @Order(22)
    void test22_adminDashboard() throws Exception {
        String auth = "Bearer " + adminToken;
        JsonNode stats = getJson2("/api/admin/dashboard/stats", auth);
        assertTrue(stats.path("robotCount").asInt() >= 20, "后台统计机器人数量异常");
        assertTrue(stats.path("brandCount").asInt() >= 10, "后台统计品牌数量异常");
        assertTrue(stats.path("companyCount").asInt() >= 10, "后台统计企业数量异常");
        assertTrue(stats.path("articleCount").asInt() >= 20, "后台统计资讯数量异常");
        assertTrue(stats.path("userCount").asInt() >= 0, "后台统计用户数量异常");
        assertNotNull(stats.path("todayPv"), "PV 统计缺失");
        assertNotNull(stats.path("todayUv"), "UV 统计缺失");

        JsonNode trend = getJson2("/api/admin/dashboard/trend?days=7", auth);
        assertEquals(7, trend.size(), "趋势数据应为 7 天");
    }

    @Test
    @Order(23)
    void test23_adminRobotCrud() throws Exception {
        String auth = "Bearer " + adminToken;
        String body = "{\"name\":\"集成测试机器人\",\"model\":\"IT-001\",\"categoryId\":1,\"brandId\":1,"
                + "\"guidePrice\":19999.00,\"status\":1,\"subtitle\":\"自动化测试创建\"}";
        String r = exec(post("/api/admin/robots").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body));
        JsonNode root = objectMapper.readTree(r);
        assertEquals(200, root.path("code").asInt(), "后台新增机器人失败: " + r);
        long newId = root.path("data").asLong();

        // 编辑
        String update = "{\"id\":" + newId + ",\"name\":\"集成测试机器人-改\",\"model\":\"IT-002\","
                + "\"categoryId\":1,\"brandId\":1,\"guidePrice\":29999.00,\"status\":1}";
        String u = exec(post("/api/admin/robots").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(update));
        assertEquals(200, objectMapper.readTree(u).path("code").asInt());
        JsonNode detail = getJson2("/api/admin/robots/" + newId, auth);
        assertEquals("集成测试机器人-改", detail.path("name").asText(), "编辑未生效");

        // 保存参数值
        String params = "{\"robotId\":" + newId + ",\"items\":[{\"defId\":1001,\"value\":\"1700\"},"
                + "{\"defId\":1002,\"value\":\"55\"}]}";
        String p = exec(post("/api/admin/robots/params").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(params));
        assertEquals(200, objectMapper.readTree(p).path("code").asInt(),
                "参数保存失败");
        JsonNode saved = getJson2("/api/admin/robots/" + newId + "/params", auth);
        assertTrue(saved.size() >= 2, "参数未保存成功");

        // 保存标签
        String t = exec(post("/api/admin/robots/{id}/tags", newId).header("Authorization", auth)
                .param("tagType", "scene").contentType(MediaType.APPLICATION_JSON).content("[\"科研教育\"]"));
        assertEquals(200, objectMapper.readTree(t).path("code").asInt());

        // 删除
        String d = exec(delete("/api/admin/robots/" + newId).header("Authorization", auth));
        assertEquals(200, objectMapper.readTree(d).path("code").asInt());
    }

    @Test
    @Order(24)
    void test24_adminArticleAndInquiry() throws Exception {
        String auth = "Bearer " + adminToken;

        // 文章 CRUD
        String body = "{\"title\":\"后台集成测试文章\",\"categoryId\":1,\"summary\":\"摘要\","
                + "\"content\":\"<p>正文内容</p>\",\"status\":1}";
        String r = exec(post("/api/admin/articles").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(body));
        JsonNode root = objectMapper.readTree(r);
        assertEquals(200, root.path("code").asInt(), "后台新增文章失败: " + r);
        long newArticleId = root.path("data").asLong();

        String s = exec(post("/api/admin/articles/{id}/status", newArticleId)
                .header("Authorization", auth).param("status", "0"));
        assertEquals(200, objectMapper.readTree(s).path("code").asInt());

        String del = exec(delete("/api/admin/articles/" + newArticleId).header("Authorization", auth));
        assertEquals(200, objectMapper.readTree(del).path("code").asInt());

        // 询价查看 + 跟进
        JsonNode inquiries = getJson2("/api/admin/inquiries?pageSize=20", auth);
        assertTrue(inquiries.path("total").asInt() > 0, "后台询价列表为空");
        long inqId = inquiries.path("list").get(0).path("id").asLong();

        String st = exec(post("/api/admin/inquiries/{id}/status", inqId)
                .header("Authorization", auth).param("status", "2").param("handleNote", "已电话联系"));
        assertEquals(200, objectMapper.readTree(st).path("code").asInt());

        String rec = exec(post("/api/admin/inquiries/{id}/records", inqId)
                .header("Authorization", auth).param("content", "客户确认需求").param("operator", "admin"));
        assertEquals(200, objectMapper.readTree(rec).path("code").asInt(),
                "跟进记录写入失败");
        JsonNode detail = getJson2("/api/admin/inquiries/" + inqId, auth);
        assertTrue(detail.path("handleRecords").asText().contains("客户确认需求"), "跟进记录未保存");
    }

    @Test
    @Order(25)
    void test25_adminRbacAndOperation() throws Exception {
        String auth = "Bearer " + adminToken;
        assertTrue(getJson2("/api/admin/system/admins", auth).path("total").asInt() >= 1, "管理员列表为空");
        assertTrue(getJson2("/api/admin/system/roles", auth).size() >= 2, "角色列表异常");
        assertTrue(getJson2("/api/admin/system/menus/tree", auth).size() > 0, "菜单树为空");
        assertTrue(getJson2("/api/admin/system/permissions", auth).size() >= 30, "权限列表异常");
        assertNotNull(getJson2("/api/admin/system/logs/login", auth), "登录日志接口异常");
        assertNotNull(getJson2("/api/admin/system/logs/oper", auth), "操作日志接口异常");
        assertTrue(getJson2("/api/admin/system/dicts", auth).size() > 0, "字典为空");
        assertTrue(getJson2("/api/admin/system/configs", auth).size() > 0, "系统配置为空");

        assertTrue(getJson2("/api/admin/users?pageSize=20", auth).path("total").asInt() >= 1, "前台用户列表为空");
        assertTrue(getJson2("/api/admin/banners", auth).size() > 0, "Banner 列表为空");
        assertTrue(getJson2("/api/admin/recommends/positions", auth).size() > 0, "推荐位为空");
        assertTrue(getJson2("/api/admin/community/circles", auth).size() > 0, "后台圈子为空");
        assertTrue(getJson2("/api/admin/robots/categories", auth).size() > 0, "后台分类为空");
        assertTrue(getJson2("/api/admin/robots/templates", auth).size() > 0, "参数模板为空");
    }

    private JsonNode getJson2(String url, String authorization) throws Exception {
        return data(exec(get(url).header("Authorization", authorization)));
    }

    /**
     * 统一执行 MockMvc 请求：强制 UTF-8，避免平台默认编码（Windows 下为 GBK）导致中文乱码
     */
    private String exec(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder.characterEncoding(StandardCharsets.UTF_8.name())).andReturn();
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
