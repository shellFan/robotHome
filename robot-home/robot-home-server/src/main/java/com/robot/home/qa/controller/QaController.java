package com.robot.home.qa.controller;

import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.qa.dto.AnswerDTO;
import com.robot.home.qa.dto.QuestionDTO;
import com.robot.home.qa.service.QaService;
import com.robot.home.qa.vo.AnswerVO;
import com.robot.home.qa.vo.QuestionVO;
import com.robot.home.ratelimit.annotation.RateLimit;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

/**
 * 机器人问答
 */
@RestController
@RequestMapping("/api/qa")
public class QaController {

    @Resource
    private QaService qaService;

    /** 问题列表 */
    @GetMapping("/questions")
    public Result<PageResult<QuestionVO>> questions(
            @RequestParam(required = false) Long robotId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(qaService.questions(robotId, sort, pageNum, pageSize));
    }

    /** 问题详情 */
    @GetMapping("/questions/{id}")
    public Result<QuestionVO> questionDetail(@PathVariable Long id) {
        return Result.success(qaService.questionDetail(id, SecurityUtils.currentUserId()));
    }

    /** 提问 */
    @PostMapping("/questions")
    @RateLimit(action = "question_create", windowSeconds = 60, maxRequests = 5, dimension = "IP_USER")
    public Result<Map<String, Object>> ask(@RequestBody @Valid QuestionDTO dto) {
        Long userId = SecurityUtils.requireUserId();
        Long id = qaService.ask(userId, dto);
        return Result.success(Collections.singletonMap("id", id));
    }

    /** 删除问题 */
    @DeleteMapping("/questions/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        qaService.deleteQuestion(userId, id, false);
        return Result.success();
    }

    /** 回答列表 */
    @GetMapping("/questions/{questionId}/answers")
    public Result<PageResult<AnswerVO>> answers(
            @PathVariable Long questionId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(qaService.answers(questionId, pageNum, pageSize));
    }

    /** 回答 */
    @PostMapping("/questions/{questionId}/answers")
    @RateLimit(action = "answer_create", windowSeconds = 60, maxRequests = 10, dimension = "IP_USER")
    public Result<Map<String, Object>> answer(@PathVariable Long questionId,
                                               @RequestBody @Valid AnswerDTO dto) {
        Long userId = SecurityUtils.requireUserId();
        Long id = qaService.answer(userId, questionId, dto);
        return Result.success(Collections.singletonMap("id", id));
    }

    /** 删除回答 */
    @DeleteMapping("/answers/{id}")
    public Result<Void> deleteAnswer(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        qaService.deleteAnswer(userId, id, false);
        return Result.success();
    }

    /** 关注问题 */
    @PostMapping("/questions/{id}/follow")
    @RateLimit(action = "question_follow", windowSeconds = 60, maxRequests = 20, dimension = "IP_USER")
    public Result<Void> follow(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        qaService.follow(userId, id);
        return Result.success();
    }

    /** 取消关注 */
    @DeleteMapping("/questions/{id}/follow")
    public Result<Void> unfollow(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        qaService.unfollow(userId, id);
        return Result.success();
    }

    /** 回答有帮助 */
    @PostMapping("/answers/{id}/helpful")
    @RateLimit(action = "answer_helpful", windowSeconds = 10, maxRequests = 3, dimension = "IP_USER")
    public Result<Void> helpful(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        qaService.helpful(userId, id);
        return Result.success();
    }

    /** 取消有帮助 */
    @DeleteMapping("/answers/{id}/helpful")
    public Result<Void> unhelpful(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        qaService.unhelpful(userId, id);
        return Result.success();
    }

    /** 采纳回答 */
    @PostMapping("/answers/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        Long userId = SecurityUtils.requireUserId();
        qaService.accept(userId, id, false);
        return Result.success();
    }
}