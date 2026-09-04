package com.robot.home.inquiry.service;

import com.robot.home.common.PageResult;
import com.robot.home.inquiry.dto.InquiryDTO;
import com.robot.home.inquiry.vo.InquiryVO;

/**
 * 询价服务
 */
public interface InquiryService {

    /**
     * 提交询价，返回询价 id
     */
    Long submit(Long userId, InquiryDTO dto);

    PageResult<InquiryVO> my(Long userId, Integer pageNum, Integer pageSize);

    InquiryVO myDetail(Long userId, Long id);
}
