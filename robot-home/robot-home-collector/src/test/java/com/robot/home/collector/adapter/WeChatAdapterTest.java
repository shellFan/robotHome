package com.robot.home.collector.adapter;

import com.robot.home.collector.fetcher.FetchResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeChatAdapterTest {

    private final WeChatAdapter adapter = new WeChatAdapter();

    @Test
    void acceptsWechatArticleSeedButRejectsOtherHosts() {
        assertTrue(adapter.shouldFollow("https://mp.weixin.qq.com/s/article-id", 0, 10, Collections.emptyMap()));
        assertTrue(adapter.shouldFollow("https://w.weixin.qq.com/s/article-id", 0, 10, Collections.emptyMap()));
        assertFalse(adapter.shouldFollow("https://example.com/article", 0, 10, Collections.emptyMap()));
    }

    @Test
    void parsedWechatArticleUsesCrawlerContentType() {
        String html = "<html><head><title>机器人技术动态</title></head><body>"
                + "<h1 class='rich_media_title'>机器人技术动态</h1>"
                + "<div id='js_content'><p>一段可供审核的文章正文。</p></div></body></html>";
        FetchResult result = FetchResult.success("https://mp.weixin.qq.com/s/article-id", 200, html);

        ParsedData parsed = adapter.parse(result, Collections.emptyMap());

        assertTrue(parsed.isSuccess());
        assertEquals("article", parsed.getType());
        assertEquals("机器人技术动态", parsed.getTitle());
    }
}
