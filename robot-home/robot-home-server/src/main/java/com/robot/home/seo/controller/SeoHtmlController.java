package com.robot.home.seo.controller;

import com.robot.home.seo.service.SeoHtmlPageService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** Serves indexable HTML for public detail and landing-page URLs. */
@RestController
public class SeoHtmlController {

    private static final MediaType HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);
    private final SeoHtmlPageService pageService;

    public SeoHtmlController(SeoHtmlPageService pageService) {
        this.pageService = pageService;
    }

    @GetMapping(value = "/robots/{slug}", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> robot(@PathVariable String slug) {
        return response(pageService.renderRobot(slug));
    }

    @GetMapping(value = "/brands/{slug}", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> brand(@PathVariable String slug) {
        return response(pageService.renderBrand(slug));
    }

    @GetMapping(value = "/companies/{slug}", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> company(@PathVariable String slug) {
        return response(pageService.renderCompany(slug));
    }

    @GetMapping(value = "/categories/{slug}", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> category(@PathVariable String slug) {
        return response(pageService.renderCategory(slug));
    }

    @GetMapping(value = "/rankings/{slug}", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> ranking(@PathVariable String slug) {
        return response(pageService.renderRanking(slug));
    }

    @GetMapping(value = "/articles/{slug}", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> article(@PathVariable String slug) {
        return response(pageService.renderArticle(slug));
    }

    private ResponseEntity<String> response(Optional<String> html) {
        if (!html.isPresent()) {
            return ResponseEntity.status(404)
                    .contentType(HTML_UTF8)
                    .header("X-Robots-Tag", "noindex")
                    .body("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\"><title>页面不存在</title></head><body><h1>页面不存在</h1></body></html>");
        }
        return ResponseEntity.ok()
                .contentType(HTML_UTF8)
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .header("X-Robots-Tag", "index, follow")
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING)
                .body(html.get());
    }
}
