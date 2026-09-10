package com.robot.home.collector.util;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * URL规范化工具
 * 处理：http/https、末尾/、utm参数、fragment、tracking参数、重复query参数
 */
public class UrlNormalizer {

    /** 需要移除的tracking参数 */
    private static final Set<String> TRACKING_PARAMS = new HashSet<>(Arrays.asList(
            // UTM系列
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "utm_id", "utm_cid", "utm_reader", "utm_name", "utm_pubreferrer", "utm_swu", "utm_viz_id",
            // Mailchimp
            "mc_eid", "mc_cid",
            // HubSpot
            "hsa_cam", "hsa_grp", "hsa_src", "hsa_ad", "hsa_ver", "hsa_la", "hsa_ol", "hsa_acc",
            "hubs_content", "hubs_content-type",
            // Marketo
            "mkt_tok", "mkt_n", "mkt_unsubscribe",
            // Pardot
            "pi_ad_id", "pi_ad_type", "pi_campaign_id", "pi_partner_id",
            // Google Ads
            "gclid", "gclsrc", "dclid", "gad_source",
            // Facebook
            "fbclid", "fb_action_ids", "fb_action_types", "fb_ref", "fb_source",
            // Twitter/X
            "twclid", "s_cid",
            // LinkedIn
            "li_fat_id", "trk", "trkContact", "li_share",
            // Adobe
            "ef_id", "s_kwcid",
            // 其他营销参数
            "spm", "from", "source", "share_token", "isShare", "share_url",
            "ref", "referrer", "affiliate", "aff_id", "affiliate_id",
            "campaign", "campaign_id", "campaign_name",
            "partner", "partner_id", "partner_name",
            "click_id", "clickid", "clickthrough",
            // Newsletter/Email
            "vero_id", "vero_conv", "oly_anon_id", "oly_enc_id",
            "_bta_tid", "_bta_c", "edr", "amp",
            // NS (NetSuite)
            "ns_m_channel", "ns_campaign", "ns_source", "ns_m_source", "ns_linkname",
            // Session/Cache
            "callback", "_", "timestamp", "rand", "random", "t", "ts", "v", "version",
            "session_id", "sid", "jsessionid",
            // 其他
            "wickedid", "icid", "ncid", "igshid", "wgu", "sl", "s_tft",
            "si", "sm", "s", "st", "st-",
            "sr", "sp", "sr_share", "share", "shared",
            "preview", "draft", "test", "debug", "dev", "env",
            "lang", "locale", "cc", "country"
    ));

    private static final Pattern DEFAULT_PORT_PATTERN = Pattern.compile(":(80|443)$");

    /**
     * 规范化URL
     */
    public static String normalize(String rawUrl) {
        if (StringUtils.isBlank(rawUrl)) {
            return rawUrl;
        }

        String url = rawUrl.trim();

        // 移除首尾空格和零宽字符
        url = url.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");

        // 补全协议
        if (url.startsWith("//")) {
            url = "http:" + url;
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        try {
            URL parsed = new URL(url);
            url = rebuildUrl(parsed);
        } catch (MalformedURLException e) {
            // 无法解析的URL返回原始（清理后）的
            return url;
        }

        return url;
    }

    /**
     * 计算URL的SHA-256哈希
     */
    public static String hash(String normalizedUrl) {
        return HashUtils.sha256(normalizedUrl);
    }

    /**
     * 判断URL是否同一域名
     */
    public static boolean isSameDomain(String url1, String url2) {
        try {
            String host1 = new URL(url1).getHost().toLowerCase();
            String host2 = new URL(url2).getHost().toLowerCase();
            return host1.equals(host2);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * 提取域名
     */
    public static String getDomain(String url) {
        try {
            return new URL(url).getHost().toLowerCase();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * 解析URL路径
     */
    public static String getPath(String url) {
        try {
            return new URL(url).getPath();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * 判断URL是否匹配包含规则
     */
    public static boolean matchesInclude(String url, String includeRules) {
        if (StringUtils.isBlank(includeRules)) {
            return true;
        }
        String[] rules = includeRules.split(",");
        for (String rule : rules) {
            if (StringUtils.isBlank(rule)) continue;
            try {
                if (Pattern.compile(rule.trim()).matcher(url).find()) {
                    return true;
                }
            } catch (Exception ignored) {
                // 无效正则跳过
            }
        }
        return false;
    }

    /**
     * 判断URL是否匹配排除规则
     */
    public static boolean matchesExclude(String url, String excludeRules) {
        if (StringUtils.isBlank(excludeRules)) {
            return false;
        }
        String[] rules = excludeRules.split(",");
        for (String rule : rules) {
            if (StringUtils.isBlank(rule)) continue;
            try {
                if (Pattern.compile(rule.trim()).matcher(url).find()) {
                    return true;
                }
            } catch (Exception ignored) {
                // 无效正则跳过
            }
        }
        return false;
    }

    /**
     * 将相对URL转为绝对URL
     */
    public static String resolve(String baseUrl, String relativeUrl) {
        if (StringUtils.isBlank(relativeUrl)) {
            return baseUrl;
        }
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return normalize(relativeUrl);
        }
        try {
            URL base = new URL(baseUrl);
            return normalize(new URL(base, relativeUrl).toString());
        } catch (MalformedURLException e) {
            return relativeUrl;
        }
    }

    /**
     * 重建规范化URL
     */
    private static String rebuildUrl(URL parsed) {
        String protocol = parsed.getProtocol().toLowerCase();
        String host = parsed.getHost().toLowerCase();
        int port = parsed.getPort();
        String path = parsed.getPath();
        String query = parsed.getQuery();

        // 移除默认端口
        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append("://").append(host);
        if (port > 0 && port != 80 && port != 443) {
            sb.append(":").append(port);
        }

        // 路径处理：空路径设为/，移除末尾/
        if (StringUtils.isBlank(path) || "/".equals(path)) {
            sb.append("/");
        } else {
            // 移除路径中的 // 
            path = path.replaceAll("/+", "/");
            // 移除末尾/
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }
            sb.append(path);
        }

        // 处理查询参数
        if (StringUtils.isNotBlank(query)) {
            String cleanedQuery = cleanQuery(query);
            if (StringUtils.isNotBlank(cleanedQuery)) {
                sb.append("?").append(cleanedQuery);
            }
        }

        // 不保留fragment
        return sb.toString();
    }

    /**
     * 清理查询参数：移除tracking参数，排序
     */
    private static String cleanQuery(String query) {
        if (StringUtils.isBlank(query)) {
            return null;
        }

        String[] pairs = query.split("&");
        List<String> kept = new ArrayList<>();

        for (String pair : pairs) {
            if (StringUtils.isBlank(pair)) continue;
            int idx = pair.indexOf('=');
            String key = idx > 0 ? pair.substring(0, idx) : pair;
            if (TRACKING_PARAMS.contains(key.toLowerCase())) {
                continue;
            }
            kept.add(pair);
        }

        if (kept.isEmpty()) {
            return null;
        }

        // 排序保证一致性
        Collections.sort(kept);
        return String.join("&", kept);
    }
}