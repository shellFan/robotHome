package com.robot.home.qa.service;

import com.robot.home.common.PageResult;
import com.robot.home.qa.dto.AnswerDTO;
import com.robot.home.qa.dto.QuestionDTO;
import com.robot.home.qa.vo.AnswerVO;
import com.robot.home.qa.vo.QuestionVO;

import java.util.Map;

/**
 * 机器人问答服务
 */
public interface QaService {

    /** 提问 */
    Long ask(Long userId, QuestionDTO dto);

    /** 回答 */
    Long answer(Long userId, Long questionId, AnswerDTO dto);

    /** 问题列表(按robot/全部) */
    PageResult<QuestionVO> questions(Long robotId, String sort, Integer pageNum, Integer pageSize);

    /** 问题详情 */
    QuestionVO questionDetail(Long id, Long currentUserId);

    /** 回答列表 */
    PageResult<AnswerVO> answers(Long questionId, Integer pageNum, Integer pageSize);

    /** 关注问题 */
    void follow(Long userId, Long questionId);

    /** 取消关注 */
    void unfollow(Long userId, Long questionId);

    /** 回答有帮助 */
    void helpful(Long userId, Long answerId);

    /** 取消有帮助 */
    void unhelpful(Long userId, Long answerId);

    /** 采纳回答(仅提问者或Admin) */
    void accept(Long userId, Long answerId, boolean isAdmin);

    /** 删除问题 */
    void deleteQuestion(Long userId, Long questionId, boolean isAdmin);

    /** 删除回答 */
    void deleteAnswer(Long userId, Long answerId, boolean isAdmin);

    /** Admin: 问题列表 */
    PageResult<QuestionVO> adminQuestions(Integer status, Integer pageNum, Integer pageSize);

    /** Admin: 更新问题状态 */
    void updateQuestionStatus(Long questionId, Integer status);

    /** Admin: 回答列表 */
    PageResult<AnswerVO> adminAnswers(Long questionId, Integer pageNum, Integer pageSize);

    /** Admin: 更新回答状态 */
    void updateAnswerStatus(Long answerId, Integer status);
}