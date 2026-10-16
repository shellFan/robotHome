package com.robot.home.company.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** Public-safe claim status; applicant identity and proof materials are never included. */
@Data
public class CompanyClaimStatusVO {

    private Long companyId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime reviewTime;
}
