package com.robot.home.collector.parser;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * robots.txt解析器
 * 解析robots.txt规则，判断URL是否允许抓取
 */
@Component
public class RobotsTxtParser {

    private static final Logger log = LoggerFactory.getLogger(RobotsTxtParser.class);

    /** 缓存已解析的robots规则 */
    private final Map<String, RobotsRules> cache = new ConcurrentHashMap<>();

    /** 默认User-Agent */
    private static final String DEFAULT_USER_AGENT = "RobotHomeCollector";

    /**
     * 解析robots.txt内容
     */
    public RobotsRules parse(String domain, String robotsTxtContent) {
        if (StringUtils.isBlank(robotsTxtContent)) {
            return new RobotsRules(domain, Collections.emptyList(), Collections.emptyList(), 0);
        }

        List<Rule> allowRules = new ArrayList<>();
        List<Rule> disallowRules = new ArrayList<>();
        int crawlDelay = 0;

        String currentUserAgent = null;
        boolean matchOurBot = false;

        String[] lines = robotsTxtContent.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int colonIdx = line.indexOf(':');
            if (colonIdx < 0) continue;

            String directive = line.substring(0, colonIdx).trim().toLowerCase();
            String value = line.substring(colonIdx + 1).trim();

            // 移除行内注释
            int commentIdx = value.indexOf('#');
            if (commentIdx >= 0) {
                value = value.substring(0, commentIdx).trim();
            }

            switch (directive) {
                case "user-agent":
                    currentUserAgent = value;
                    matchOurBot = "*".equals(value) ||
                            value.toLowerCase().contains("robothome") ||
                            value.toLowerCase().contains("robot");
                    break;

                case "disallow":
                    if (matchOurBot && StringUtils.isNotBlank(value)) {
                        disallowRules.add(new Rule(value, wildcardToRegex(value)));
                    }
                    break;

                case "allow":
                    if (matchOurBot && StringUtils.isNotBlank(value)) {
                        allowRules.add(new Rule(value, wildcardToRegex(value)));
                    }
                    break;

                case "crawl-delay":
                    if (matchOurBot) {
                        try {
                            crawlDelay = Integer.parseInt(value);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    break;

                case "sitemap":
                    // Sitemap URL可以单独处理
                    break;
            }
        }

        // 按路径长度排序，更具体的规则优先
        allowRules.sort(Comparator.comparingInt(r -> -r.path.length()));
        disallowRules.sort(Comparator.comparingInt(r -> -r.path.length()));

        RobotsRules rules = new RobotsRules(domain, allowRules, disallowRules, crawlDelay);
        cache.put(domain, rules);
        return rules;
    }

    /**
     * 判断URL是否允许抓取
     */
    public boolean isAllowed(String url, String userAgent) {
        try {
            URL parsed = new URL(url);
            String domain = parsed.getHost().toLowerCase();
            String path = parsed.getPath();
            if (parsed.getQuery() != null) {
                path += "?" + parsed.getQuery();
            }

            RobotsRules rules = cache.get(domain);
            if (rules == null) {
                // 没有缓存规则，默认允许
                return true;
            }

            return rules.isAllowed(path, userAgent);
        } catch (Exception e) {
            log.warn("Failed to check robots.txt for URL: {}", url, e);
            return true;
        }
    }

    /**
     * 获取Crawl-Delay
     */
    public int getCrawlDelay(String domain) {
        RobotsRules rules = cache.get(domain);
        return rules != null ? rules.crawlDelay : 0;
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * 清除指定域名的缓存
     */
    public void clearCache(String domain) {
        cache.remove(domain);
    }

    /**
     * 将robots.txt通配符转为正则
     * * → .*  $ → \Z
     */
    private String wildcardToRegex(String pattern) {
        if (StringUtils.isBlank(pattern)) {
            return ".*";
        }
        String regex = Pattern.quote(pattern);
        regex = regex.replace("*", "\\E.*\\Q");
        regex = regex.replace("\\$", "\\Z");
        return "^" + regex + ".*$";
    }

    /**
     * robots.txt规则集
     */
    public static class RobotsRules {
        private final String domain;
        private final List<Rule> allowRules;
        private final List<Rule> disallowRules;
        private final int crawlDelay;

        public RobotsRules(String domain, List<Rule> allowRules, List<Rule> disallowRules, int crawlDelay) {
            this.domain = domain;
            this.allowRules = allowRules;
            this.disallowRules = disallowRules;
            this.crawlDelay = crawlDelay;
        }

        public boolean isAllowed(String path, String userAgent) {
            // 检查Allow规则
            for (Rule rule : allowRules) {
                if (rule.matches(path)) {
                    return true;
                }
            }
            // 检查Disallow规则
            for (Rule rule : disallowRules) {
                if (rule.matches(path)) {
                    return false;
                }
            }
            return true;
        }

        public int getCrawlDelay() {
            return crawlDelay;
        }

        public String getDomain() {
            return domain;
        }
    }

    /**
     * 单条规则
     */
    private static class Rule {
        final String path;
        final Pattern pattern;

        Rule(String path, String regex) {
            this.path = path;
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }

        boolean matches(String urlPath) {
            return pattern.matcher(urlPath).matches();
        }
    }
}