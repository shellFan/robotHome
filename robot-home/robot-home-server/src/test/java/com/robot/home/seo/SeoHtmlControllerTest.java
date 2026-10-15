package com.robot.home.seo;

import com.robot.home.seo.controller.SeoHtmlController;
import com.robot.home.seo.service.SeoHtmlPageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeoHtmlControllerTest {

    private SeoHtmlPageService pageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        pageService = mock(SeoHtmlPageService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SeoHtmlController(pageService)).build();
    }

    @Test
    void exposesAllSixPublicHtmlRoutes() throws Exception {
        when(pageService.renderRobot("unitree-go2")).thenReturn(Optional.of("<html>robot</html>"));
        when(pageService.renderBrand("unitree")).thenReturn(Optional.of("<html>brand</html>"));
        when(pageService.renderCompany("unitree-tech")).thenReturn(Optional.of("<html>company</html>"));
        when(pageService.renderCategory("category-1")).thenReturn(Optional.of("<html>category</html>"));
        when(pageService.renderRanking("hot")).thenReturn(Optional.of("<html>ranking</html>"));
        when(pageService.renderArticle("robot-news")).thenReturn(Optional.of("<html>article</html>"));

        assertHtmlOk("/robots/unitree-go2", "robot");
        assertHtmlOk("/brands/unitree", "brand");
        assertHtmlOk("/companies/unitree-tech", "company");
        assertHtmlOk("/categories/category-1", "category");
        assertHtmlOk("/rankings/hot", "ranking");
        assertHtmlOk("/articles/robot-news", "article");
    }

    @Test
    void returns404WhenRecordIsNotPublic() throws Exception {
        when(pageService.renderArticle("draft")).thenReturn(Optional.empty());

        mockMvc.perform(get("/articles/draft"))
                .andExpect(status().isNotFound());
    }

    private void assertHtmlOk(String path, String marker) throws Exception {
        mockMvc.perform(get(path).header("Host", "attacker.example"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(marker)));
    }
}
