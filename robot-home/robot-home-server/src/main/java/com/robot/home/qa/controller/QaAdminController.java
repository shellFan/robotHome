package com.robot.home.qa.controller;

import com.robot.home.common.PageResult;
import com.robot.home.qa.service.QaService;
import com.robot.home.qa.vo.AnswerVO;
import com.robot.home.qa.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Q&A管理API — 路径 /api/admin/qa/** 由AdminInterceptor保护
 */
@RestController
@RequestMapping("/api/admin/qa")
@RequiredArgsConstructor
public class QaAdminController {

    private final QaService qaService;

    /** 管理端: 问题列表 */
    @GetMapping("/questions")
    public PageResult<QuestionVO> listQuestions(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return qaService.adminQuestions(status, pageNum, pageSize);
    }

    /** 管理端: 删除问题 */
    @DeleteMapping("/questions/{id}")
    public void deleteQuestion(@PathVariable Long id) {
        qaService.deleteQuestion(null, id, true);
    }

    /** 管理端: 更新问题状态 */
    @PutMapping("/questions/{id}/status")
    public void updateQuestionStatus(@PathVariable Long id, @RequestParam Integer status) {
        qaService.updateQuestionStatus(id, status);
    }

    /** 管理端: 回答列表 */
    @GetMapping("/answers")
    public PageResult<AnswerVO> listAnswers(
            @RequestParam Long questionId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return qaService.adminAnswers(questionId, pageNum, pageSize);
    }

    /** 管理端: 删除回答 */
    @DeleteMapping("/answers/{id}")
    public void deleteAnswer(@PathVariable Long id) {
        qaService.deleteAnswer(null, id, true);
    }

    /** 管理端: 更新回答状态 */
    @PutMapping("/answers/{id}/status")
    public void updateAnswerStatus(@PathVariable Long id, @RequestParam Integer status) {
        qaService.updateAnswerStatus(id, status);
    }
}