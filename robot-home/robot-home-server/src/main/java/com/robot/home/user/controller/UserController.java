package com.robot.home.user.controller;

import com.robot.home.comment.service.CommentService;
import com.robot.home.common.PageResult;
import com.robot.home.common.Result;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.SecurityUtils;
import com.robot.home.common.util.SensitiveUtils;
import com.robot.home.community.service.CommunityService;
import com.robot.home.community.vo.PostVO;
import com.robot.home.comment.vo.CommentVO;
import com.robot.home.favorite.service.FavoriteService;
import com.robot.home.follow.service.FollowService;
import com.robot.home.history.service.HistoryService;
import com.robot.home.inquiry.service.InquiryService;
import com.robot.home.like.service.LikeService;
import com.robot.home.message.service.MessageService;
import com.robot.home.user.dto.UserProfileDTO;
import com.robot.home.user.entity.User;
import com.robot.home.user.service.UserService;
import com.robot.home.user.vo.UserProfileVO;
import com.robot.home.user.vo.UserStatsVO;
import com.robot.home.user.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 用户中心
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private FavoriteService favoriteService;
    @Resource
    private HistoryService historyService;
    @Resource
    private CommunityService communityService;
    @Resource
    private CommentService commentService;
    @Resource
    private LikeService likeService;
    @Resource
    private FollowService followService;
    @Resource
    private InquiryService inquiryService;
    @Resource
    private MessageService messageService;

    /**
     * 当前登录用户资料
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(toVO(userService.getById(userId), true));
    }

    /**
     * 更新个人资料
     */
    @PutMapping("/me")
    public Result<Void> update(@RequestBody @Valid UserProfileDTO dto) {
        Long userId = SecurityUtils.requireUserId();
        userService.updateProfile(userId, dto);
        return Result.success();
    }

    /**
     * 用户中心统计
     */
    @GetMapping("/me/stats")
    public Result<UserStatsVO> stats() {
        Long userId = SecurityUtils.requireUserId();
        User user = userService.getById(userId);
        UserStatsVO vo = new UserStatsVO();
        vo.setFavoriteCount(favoriteService.count(userId, null));
        vo.setHistoryCount(historyService.count(userId));
        vo.setPostCount(user == null || user.getPostCount() == null ? 0L : user.getPostCount().longValue());
        vo.setCommentCount(myCommentCount(userId));
        vo.setLikeCount(likeService.count(userId));
        vo.setFollowCount(followService.count(userId, null));
        vo.setInquiryCount(inquiryService.my(userId, 1, 1).getTotal());
        vo.setUnreadMessageCount(messageService.unreadCount(userId));
        vo.setFansCount(user == null || user.getFansCount() == null ? 0L : user.getFansCount().longValue());
        return Result.success(vo);
    }

    /**
     * 他人主页
     */
    @GetMapping("/{id}")
    public Result<UserProfileVO> profile(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(toVO(user, false), vo);
        vo.setIntro(user.getIntro());
        vo.setFansCount(user.getFansCount());
        vo.setFollowCount(user.getFollowCount());
        vo.setPostCount(user.getPostCount());
        return Result.success(vo);
    }

    /**
     * 我的帖子
     */
    @GetMapping("/me/posts")
    public Result<PageResult<PostVO>> myPosts(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(communityService.myPosts(userId, pageNum, pageSize));
    }

    /**
     * 我的评论
     */
    @GetMapping("/me/comments")
    public Result<PageResult<CommentVO>> myComments(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(commentService.myComments(userId, pageNum, pageSize));
    }

    /**
     * 我的收藏
     */
    @GetMapping("/me/favorites")
    public Result<PageResult<com.robot.home.favorite.vo.FavoriteItemVO>> myFavorites(
            @RequestParam(required = false) String bizType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(favoriteService.myFavorites(userId, bizType, pageNum, pageSize));
    }

    /**
     * 我的浏览历史
     */
    @GetMapping("/me/history")
    public Result<PageResult<com.robot.home.history.vo.HistoryItemVO>> myHistory(
            @RequestParam(required = false) String bizType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(historyService.myHistory(userId, bizType, pageNum, pageSize));
    }

    /**
     * 我的关注
     */
    @GetMapping("/me/follows")
    public Result<PageResult<com.robot.home.follow.vo.FollowItemVO>> myFollows(
            @RequestParam(required = false) String followType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(followService.myFollows(userId, followType, pageNum, pageSize));
    }

    /**
     * 我的点赞（评论点赞数，供用户中心展示）
     */
    @GetMapping("/me/likes")
    public Result<PageResult<CommentVO>> myLikes(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = SecurityUtils.requireUserId();
        return Result.success(commentService.myComments(userId, pageNum, pageSize));
    }

    private long myCommentCount(Long userId) {
        return commentService.myComments(userId, 1, 1).getTotal();
    }

    private UserVO toVO(User user, boolean self) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        // 密码等敏感字段 UserVO 本身不含，其余按场景脱敏
        if (!self) {
            vo.setPhone(SensitiveUtils.maskPhone(user.getPhone()));
            vo.setEmail(SensitiveUtils.maskEmail(user.getEmail()));
        }
        return vo;
    }
}
