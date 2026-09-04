package com.robot.home.search.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.robot.home.article.entity.Article;
import com.robot.home.article.mapper.ArticleMapper;
import com.robot.home.brand.entity.Brand;
import com.robot.home.brand.mapper.BrandMapper;
import com.robot.home.common.PageResult;
import com.robot.home.common.util.PageUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.community.entity.CommunityPost;
import com.robot.home.community.mapper.CommunityPostMapper;
import com.robot.home.robot.entity.Robot;
import com.robot.home.robot.mapper.RobotMapper;
import com.robot.home.search.entity.HotSearch;
import com.robot.home.search.entity.SearchHistory;
import com.robot.home.search.mapper.HotSearchMapper;
import com.robot.home.search.mapper.SearchHistoryMapper;
import com.robot.home.search.service.SearchService;
import com.robot.home.search.vo.HotSearchVO;
import com.robot.home.search.vo.SearchItemVO;
import com.robot.home.search.vo.SearchResultVO;
import com.robot.home.tutorial.entity.Tutorial;
import com.robot.home.tutorial.mapper.TutorialMapper;
import com.robot.home.video.entity.Video;
import com.robot.home.video.mapper.VideoMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于数据库的搜索实现（第一版）
 * 后续切换 Elasticsearch 时，只需新增实现类并替换 @Service 注解
 */
@Service
public class DatabaseSearchServiceImpl implements SearchService {

    private static final int MAX_LIMIT_PER_TYPE = 20;

    @Resource
    private RobotMapper robotMapper;
    @Resource
    private BrandMapper brandMapper;
    @Resource
    private CompanyMapper companyMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private VideoMapper videoMapper;
    @Resource
    private TutorialMapper tutorialMapper;
    @Resource
    private CommunityPostMapper postMapper;
    @Resource
    private HotSearchMapper hotSearchMapper;
    @Resource
    private SearchHistoryMapper searchHistoryMapper;

    @Override
    public SearchResultVO search(String keyword, Integer limitPerType) {
        if (StrUtil.isBlank(keyword)) {
            throw new com.robot.home.common.exception.BusinessException("搜索关键词不能为空");
        }
        String kw = StrUtil.trim(keyword);
        int limit = limitPerType == null ? 5 : Math.max(1, Math.min(limitPerType, MAX_LIMIT_PER_TYPE));

        SearchResultVO vo = new SearchResultVO();
        vo.setKeyword(kw);
        vo.setRobots(searchRobots(kw, limit));
        vo.setBrands(searchBrands(kw, limit));
        vo.setCompanies(searchCompanies(kw, limit));
        vo.setArticles(searchArticles(kw, limit));
        vo.setVideos(searchVideos(kw, limit));
        vo.setTutorials(searchTutorials(kw, limit));
        vo.setPosts(searchPosts(kw, limit));

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("robot", countRobots(kw));
        counts.put("brand", countBrands(kw));
        counts.put("company", countCompanies(kw));
        counts.put("article", countArticles(kw));
        counts.put("video", countVideos(kw));
        counts.put("tutorial", countTutorials(kw));
        counts.put("post", countPosts(kw));
        vo.setCounts(counts);
        return vo;
    }

    @Override
    public PageResult<SearchItemVO> searchByType(String type, String keyword, Integer pageNum, Integer pageSize) {
        if (StrUtil.isBlank(keyword)) {
            return PageResult.of(1, PageUtils.normalizePageSize(pageSize), 0, new ArrayList<>());
        }
        String kw = StrUtil.trim(keyword);
        int pn = PageUtils.normalizePageNum(pageNum);
        int ps = PageUtils.normalizePageSize(pageSize);
        Page<?> page = new Page<>(pn, ps);

        IPage<?> result;
        List<SearchItemVO> items;
        switch (type) {
            case "robot": {
                IPage<Robot> p = robotMapper.selectPage((Page<Robot>) page, Wrappers.<Robot>lambdaQuery()
                        .and(w -> w.like(Robot::getName, kw).or().like(Robot::getModel, kw).or().like(Robot::getSubtitle, kw))
                        .orderByDesc(Robot::getHotScore));
                items = p.getRecords().stream().map(this::robotItem).collect(Collectors.toList());
                result = p;
                break;
            }
            case "brand": {
                IPage<Brand> p = brandMapper.selectPage((Page<Brand>) page, Wrappers.<Brand>lambdaQuery()
                        .like(Brand::getName, kw)
                        .orderByDesc(Brand::getHotScore));
                items = p.getRecords().stream().map(this::brandItem).collect(Collectors.toList());
                result = p;
                break;
            }
            case "company": {
                IPage<Company> p = companyMapper.selectPage((Page<Company>) page, Wrappers.<Company>lambdaQuery()
                        .and(w -> w.like(Company::getName, kw).or().like(Company::getIntro, kw))
                        .orderByDesc(Company::getHotScore));
                items = p.getRecords().stream().map(this::companyItem).collect(Collectors.toList());
                result = p;
                break;
            }
            case "article": {
                IPage<Article> p = articleMapper.selectPage((Page<Article>) page, Wrappers.<Article>lambdaQuery()
                        .eq(Article::getStatus, 1)
                        .and(w -> w.like(Article::getTitle, kw).or().like(Article::getSummary, kw))
                        .orderByDesc(Article::getPublishTime));
                items = p.getRecords().stream().map(this::articleItem).collect(Collectors.toList());
                result = p;
                break;
            }
            case "video": {
                IPage<Video> p = videoMapper.selectPage((Page<Video>) page, Wrappers.<Video>lambdaQuery()
                        .eq(Video::getStatus, 1)
                        .and(w -> w.like(Video::getTitle, kw).or().like(Video::getSummary, kw))
                        .orderByDesc(Video::getPublishTime));
                items = p.getRecords().stream().map(this::videoItem).collect(Collectors.toList());
                result = p;
                break;
            }
            case "tutorial": {
                IPage<Tutorial> p = tutorialMapper.selectPage((Page<Tutorial>) page, Wrappers.<Tutorial>lambdaQuery()
                        .eq(Tutorial::getStatus, 1)
                        .and(w -> w.like(Tutorial::getTitle, kw).or().like(Tutorial::getSummary, kw))
                        .orderByDesc(Tutorial::getPublishTime));
                items = p.getRecords().stream().map(this::tutorialItem).collect(Collectors.toList());
                result = p;
                break;
            }
            case "post": {
                IPage<CommunityPost> p = postMapper.selectPage((Page<CommunityPost>) page, Wrappers.<CommunityPost>lambdaQuery()
                        .eq(CommunityPost::getStatus, 1)
                        .and(w -> w.like(CommunityPost::getTitle, kw).or().like(CommunityPost::getContent, kw))
                        .orderByDesc(CommunityPost::getCreateTime));
                items = p.getRecords().stream().map(this::postItem).collect(Collectors.toList());
                result = p;
                break;
            }
            default:
                throw new com.robot.home.common.exception.BusinessException("不支持的搜索类型: " + type);
        }
        return PageResult.of(pn, ps, result.getTotal(), items);
    }

    @Override
    public List<String> suggest(String keyword, int limit) {
        if (StrUtil.isBlank(keyword)) {
            return new ArrayList<>();
        }
        String kw = StrUtil.trim(keyword);
        int size = Math.max(1, Math.min(limit, 20));
        List<Robot> robots = robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .like(Robot::getName, kw)
                .orderByDesc(Robot::getHotScore)
                .last("LIMIT " + size));
        List<String> result = robots.stream().map(Robot::getName).collect(Collectors.toList());
        if (result.size() < size) {
            List<HotSearch> hots = hotSearchMapper.selectList(Wrappers.<HotSearch>lambdaQuery()
                    .like(HotSearch::getKeyword, kw)
                    .orderByDesc(HotSearch::getSearchCount)
                    .last("LIMIT " + (size - result.size())));
            for (HotSearch h : hots) {
                if (!result.contains(h.getKeyword())) {
                    result.add(h.getKeyword());
                }
            }
        }
        return result;
    }

    @Override
    public List<HotSearchVO> hotSearches(int limit) {
        List<HotSearch> list = hotSearchMapper.selectList(Wrappers.<HotSearch>lambdaQuery()
                .orderByDesc(HotSearch::getSearchCount)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
        List<HotSearchVO> vos = new ArrayList<>();
        int rank = 1;
        for (HotSearch h : list) {
            HotSearchVO vo = new HotSearchVO();
            vo.setKeyword(h.getKeyword());
            vo.setSearchCount(h.getSearchCount());
            vo.setRank(rank++);
            vos.add(vo);
        }
        return vos;
    }

    @Override
    public List<String> history(Long userId, int limit) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<SearchHistory> list = searchHistoryMapper.selectList(Wrappers.<SearchHistory>lambdaQuery()
                .eq(SearchHistory::getUserId, userId)
                .orderByDesc(SearchHistory::getCreateTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 50))));
        // 去重，保留最近搜索
        List<String> result = new ArrayList<>();
        for (SearchHistory h : list) {
            if (!result.contains(h.getKeyword())) {
                result.add(h.getKeyword());
            }
        }
        return result;
    }

    @Override
    public void clearHistory(Long userId) {
        if (userId == null) {
            return;
        }
        searchHistoryMapper.delete(Wrappers.<SearchHistory>lambdaQuery().eq(SearchHistory::getUserId, userId));
    }

    @Override
    public void record(String keyword, Long userId) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }
        String kw = StrUtil.trim(keyword);
        if (kw.length() > 64) {
            kw = kw.substring(0, 64);
        }
        // 个人搜索历史
        if (userId != null) {
            SearchHistory history = new SearchHistory();
            history.setUserId(userId);
            history.setKeyword(kw);
            searchHistoryMapper.insert(history);
        }
        // 热搜计数
        HotSearch hot = hotSearchMapper.selectOne(Wrappers.<HotSearch>lambdaQuery()
                .eq(HotSearch::getKeyword, kw).last("LIMIT 1"));
        if (hot != null) {
            hotSearchMapper.update(null, Wrappers.<HotSearch>lambdaUpdate()
                    .eq(HotSearch::getId, hot.getId())
                    .setSql("search_count = search_count + 1"));
        } else {
            HotSearch insert = new HotSearch();
            insert.setKeyword(kw);
            insert.setSearchCount(1);
            insert.setSort(0);
            hotSearchMapper.insert(insert);
        }
    }

    // ---------- 各类型限量搜索 ----------

    private List<SearchItemVO> searchRobots(String kw, int limit) {
        return robotMapper.selectList(Wrappers.<Robot>lambdaQuery()
                .and(w -> w.like(Robot::getName, kw).or().like(Robot::getModel, kw).or().like(Robot::getSubtitle, kw))
                .orderByDesc(Robot::getHotScore)
                .last("LIMIT " + limit))
                .stream().map(this::robotItem).collect(Collectors.toList());
    }

    private List<SearchItemVO> searchBrands(String kw, int limit) {
        return brandMapper.selectList(Wrappers.<Brand>lambdaQuery()
                .like(Brand::getName, kw)
                .orderByDesc(Brand::getHotScore)
                .last("LIMIT " + limit))
                .stream().map(this::brandItem).collect(Collectors.toList());
    }

    private List<SearchItemVO> searchCompanies(String kw, int limit) {
        return companyMapper.selectList(Wrappers.<Company>lambdaQuery()
                .and(w -> w.like(Company::getName, kw).or().like(Company::getIntro, kw))
                .orderByDesc(Company::getHotScore)
                .last("LIMIT " + limit))
                .stream().map(this::companyItem).collect(Collectors.toList());
    }

    private List<SearchItemVO> searchArticles(String kw, int limit) {
        return articleMapper.selectList(Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, 1)
                .and(w -> w.like(Article::getTitle, kw).or().like(Article::getSummary, kw))
                .orderByDesc(Article::getPublishTime)
                .last("LIMIT " + limit))
                .stream().map(this::articleItem).collect(Collectors.toList());
    }

    private List<SearchItemVO> searchVideos(String kw, int limit) {
        return videoMapper.selectList(Wrappers.<Video>lambdaQuery()
                .eq(Video::getStatus, 1)
                .and(w -> w.like(Video::getTitle, kw).or().like(Video::getSummary, kw))
                .orderByDesc(Video::getPublishTime)
                .last("LIMIT " + limit))
                .stream().map(this::videoItem).collect(Collectors.toList());
    }

    private List<SearchItemVO> searchTutorials(String kw, int limit) {
        return tutorialMapper.selectList(Wrappers.<Tutorial>lambdaQuery()
                .eq(Tutorial::getStatus, 1)
                .and(w -> w.like(Tutorial::getTitle, kw).or().like(Tutorial::getSummary, kw))
                .orderByDesc(Tutorial::getPublishTime)
                .last("LIMIT " + limit))
                .stream().map(this::tutorialItem).collect(Collectors.toList());
    }

    private List<SearchItemVO> searchPosts(String kw, int limit) {
        return postMapper.selectList(Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getStatus, 1)
                .and(w -> w.like(CommunityPost::getTitle, kw).or().like(CommunityPost::getContent, kw))
                .orderByDesc(CommunityPost::getCreateTime)
                .last("LIMIT " + limit))
                .stream().map(this::postItem).collect(Collectors.toList());
    }

    private long countRobots(String kw) {
        return robotMapper.selectCount(Wrappers.<Robot>lambdaQuery()
                .and(w -> w.like(Robot::getName, kw).or().like(Robot::getModel, kw).or().like(Robot::getSubtitle, kw)));
    }

    private long countBrands(String kw) {
        return brandMapper.selectCount(Wrappers.<Brand>lambdaQuery().like(Brand::getName, kw));
    }

    private long countCompanies(String kw) {
        return companyMapper.selectCount(Wrappers.<Company>lambdaQuery()
                .and(w -> w.like(Company::getName, kw).or().like(Company::getIntro, kw)));
    }

    private long countArticles(String kw) {
        return articleMapper.selectCount(Wrappers.<Article>lambdaQuery()
                .eq(Article::getStatus, 1)
                .and(w -> w.like(Article::getTitle, kw).or().like(Article::getSummary, kw)));
    }

    private long countVideos(String kw) {
        return videoMapper.selectCount(Wrappers.<Video>lambdaQuery()
                .eq(Video::getStatus, 1)
                .and(w -> w.like(Video::getTitle, kw).or().like(Video::getSummary, kw)));
    }

    private long countTutorials(String kw) {
        return tutorialMapper.selectCount(Wrappers.<Tutorial>lambdaQuery()
                .eq(Tutorial::getStatus, 1)
                .and(w -> w.like(Tutorial::getTitle, kw).or().like(Tutorial::getSummary, kw)));
    }

    private long countPosts(String kw) {
        return postMapper.selectCount(Wrappers.<CommunityPost>lambdaQuery()
                .eq(CommunityPost::getStatus, 1)
                .and(w -> w.like(CommunityPost::getTitle, kw).or().like(CommunityPost::getContent, kw)));
    }

    // ---------- 结果项转换 ----------

    private SearchItemVO robotItem(Robot r) {
        SearchItemVO vo = new SearchItemVO();
        vo.setType("robot");
        vo.setId(r.getId());
        vo.setTitle(r.getName());
        vo.setImage(r.getCoverImage());
        vo.setSummary(r.getSubtitle());
        vo.setUrl("/robot/" + r.getId());
        vo.setExtra(formatPrice(r.getGuidePrice()));
        return vo;
    }

    private SearchItemVO brandItem(Brand b) {
        SearchItemVO vo = new SearchItemVO();
        vo.setType("brand");
        vo.setId(b.getId());
        vo.setTitle(b.getName());
        vo.setImage(b.getLogo());
        vo.setSummary(b.getIntro());
        vo.setUrl("/brand/" + b.getId());
        return vo;
    }

    private SearchItemVO companyItem(Company c) {
        SearchItemVO vo = new SearchItemVO();
        vo.setType("company");
        vo.setId(c.getId());
        vo.setTitle(c.getName());
        vo.setImage(c.getLogo());
        vo.setSummary(c.getIntro());
        vo.setUrl("/company/" + c.getId());
        return vo;
    }

    private SearchItemVO articleItem(Article a) {
        SearchItemVO vo = new SearchItemVO();
        vo.setType("article");
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setImage(a.getCover());
        vo.setSummary(a.getSummary());
        vo.setUrl("/article/" + a.getId());
        return vo;
    }

    private SearchItemVO videoItem(Video v) {
        SearchItemVO vo = new SearchItemVO();
        vo.setType("video");
        vo.setId(v.getId());
        vo.setTitle(v.getTitle());
        vo.setImage(v.getCover());
        vo.setSummary(v.getSummary());
        vo.setUrl("/video/" + v.getId());
        return vo;
    }

    private SearchItemVO tutorialItem(Tutorial t) {
        SearchItemVO vo = new SearchItemVO();
        vo.setType("tutorial");
        vo.setId(t.getId());
        vo.setTitle(t.getTitle());
        vo.setImage(t.getCover());
        vo.setSummary(t.getSummary());
        vo.setUrl("/tutorial/" + t.getId());
        return vo;
    }

    private SearchItemVO postItem(CommunityPost p) {
        SearchItemVO vo = new SearchItemVO();
        vo.setType("post");
        vo.setId(p.getId());
        vo.setTitle(StrUtil.isNotBlank(p.getTitle()) ? p.getTitle() : excerpt(p.getContent()));
        vo.setSummary(excerpt(p.getContent()));
        vo.setUrl("/community/" + p.getId());
        return vo;
    }

    private String excerpt(String content) {
        if (content == null) {
            return null;
        }
        return content.length() > 80 ? content.substring(0, 80) + "..." : content;
    }

    private String formatPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return "暂无报价";
        }
        return "¥" + price.stripTrailingZeros().toPlainString();
    }
}
