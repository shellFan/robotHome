package com.robot.home;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robot.home.common.Constants;
import com.robot.home.common.exception.BusinessException;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.company.entity.Company;
import com.robot.home.company.entity.CompanyMember;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.company.mapper.CompanyMemberMapper;
import com.robot.home.inquiry.entity.Inquiry;
import com.robot.home.inquiry.mapper.InquiryMapper;
import com.robot.home.procurement.dto.ProcurementResponseDTO;
import com.robot.home.procurement.entity.ProcurementFollowRecord;
import com.robot.home.procurement.entity.ProcurementResponse;
import com.robot.home.procurement.mapper.ProcurementFollowRecordMapper;
import com.robot.home.procurement.mapper.ProcurementResponseMapper;
import com.robot.home.procurement.service.ProcurementCrmService;
import com.robot.home.procurement.service.ProcurementResponseService;
import com.robot.home.procurement.vo.ProcurementResponseVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataSourceInitializer.class)
@Transactional
class ProcurementLifecycleIntegrationTest {

    private static final Long COMPANY_MEMBER_USER_ID = 910001L;
    private static final Long OUTSIDE_COMPANY_USER_ID = 910002L;
    private static final Long BUYER_USER_ID = 910003L;

    @Autowired
    private ProcurementResponseService responseService;
    @Autowired
    private ProcurementCrmService crmService;
    @Autowired
    private ProcurementResponseMapper responseMapper;
    @Autowired
    private ProcurementFollowRecordMapper followRecordMapper;
    @Autowired
    private InquiryMapper inquiryMapper;
    @Autowired
    private CompanyMapper companyMapper;
    @Autowired
    private CompanyMemberMapper companyMemberMapper;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedisUtils redisUtils;

    @Test
    void responseSubmissionAdvancesLeadAndCreatesOneAuditRecord() {
        verifyCompany(1L);
        addActiveMember(1L, COMPANY_MEMBER_USER_ID);
        Inquiry inquiry = createInquiry(Constants.PIPELINE_NEW);

        ProcurementResponseVO created = responseService.submitResponse(
                COMPANY_MEMBER_USER_ID, 1L, responseDto(inquiry.getId()));

        Inquiry updated = inquiryMapper.selectById(inquiry.getId());
        List<ProcurementFollowRecord> records = followRecords(inquiry.getId());
        assertEquals(Constants.RESPONSE_SUBMITTED, created.getStatus());
        assertEquals(Constants.PIPELINE_RESPONDED, updated.getPipelineStatus());
        assertEquals(1, records.size());
        assertEquals(Constants.CRM_ACTION_STATUS_CHANGE, records.get(0).getAction());
        assertEquals(Constants.PIPELINE_NEW, records.get(0).getOldStatus());
        assertEquals(Constants.PIPELINE_RESPONDED, records.get(0).getNewStatus());
    }

    @Test
    void duplicateResponseSubmissionIsIdempotent() {
        verifyCompany(1L);
        addActiveMember(1L, COMPANY_MEMBER_USER_ID);
        Inquiry inquiry = createInquiry(Constants.PIPELINE_NEW);
        ProcurementResponseDTO dto = responseDto(inquiry.getId());

        ProcurementResponseVO first = responseService.submitResponse(COMPANY_MEMBER_USER_ID, 1L, dto);
        ProcurementResponseVO retry = responseService.submitResponse(COMPANY_MEMBER_USER_ID, 1L, dto);

        assertEquals(first.getId(), retry.getId());
        assertEquals(1L, responseMapper.selectCount(Wrappers.<ProcurementResponse>lambdaQuery()
                .eq(ProcurementResponse::getProcurementId, inquiry.getId())
                .eq(ProcurementResponse::getCompanyId, 1L)));
        assertEquals(1, followRecords(inquiry.getId()).size());
    }

    @Test
    void crossCompanyMemberCannotSubmitResponse() {
        verifyCompany(1L);
        verifyCompany(2L);
        addActiveMember(1L, OUTSIDE_COMPANY_USER_ID);
        Inquiry inquiry = createInquiry(Constants.PIPELINE_NEW);

        assertThrows(BusinessException.class, () -> responseService.submitResponse(
                OUTSIDE_COMPANY_USER_ID, 2L, responseDto(inquiry.getId())));

        assertEquals(0L, responseMapper.selectCount(Wrappers.<ProcurementResponse>lambdaQuery()
                .eq(ProcurementResponse::getProcurementId, inquiry.getId())));
    }

    @Test
    void unverifiedCompanyMemberCannotSubmitResponse() {
        setCompanyVerification(1L, "UNCLAIMED");
        addActiveMember(1L, COMPANY_MEMBER_USER_ID);
        Inquiry inquiry = createInquiry(Constants.PIPELINE_NEW);

        assertThrows(BusinessException.class, () -> responseService.submitResponse(
                COMPANY_MEMBER_USER_ID, 1L, responseDto(inquiry.getId())));
    }

    @Test
    void acceptedResponseCannotReturnToAnEarlierStatus() {
        verifyCompany(1L);
        addActiveMember(1L, COMPANY_MEMBER_USER_ID);
        Inquiry inquiry = createInquiry(Constants.PIPELINE_NEW);
        ProcurementResponseVO response = responseService.submitResponse(
                COMPANY_MEMBER_USER_ID, 1L, responseDto(inquiry.getId()));

        responseService.updateStatus(response.getId(), Constants.RESPONSE_ACCEPTED, BUYER_USER_ID);
        assertThrows(BusinessException.class, () -> responseService.updateStatus(
                response.getId(), Constants.RESPONSE_VIEWED, BUYER_USER_ID));

        assertEquals(Constants.RESPONSE_ACCEPTED,
                responseMapper.selectById(response.getId()).getStatus());
    }

    @Test
    void terminalPipelineCannotBeReopened() {
        Inquiry inquiry = createInquiry(Constants.PIPELINE_RESPONDED);

        crmService.updatePipelineStatus(inquiry.getId(), Constants.PIPELINE_WON, 1L, "admin");
        assertThrows(BusinessException.class, () -> crmService.updatePipelineStatus(
                inquiry.getId(), Constants.PIPELINE_NEGOTIATING, 1L, "admin"));

        assertEquals(Constants.PIPELINE_WON,
                inquiryMapper.selectById(inquiry.getId()).getPipelineStatus());
        assertEquals(1, followRecords(inquiry.getId()).size());
    }

    @Test
    void anonymousCallerCannotEnumerateResponseDetails() throws Exception {
        verifyCompany(1L);
        addActiveMember(1L, COMPANY_MEMBER_USER_ID);
        Inquiry inquiry = createInquiry(Constants.PIPELINE_NEW);
        ProcurementResponseVO response = responseService.submitResponse(
                COMPANY_MEMBER_USER_ID, 1L, responseDto(inquiry.getId()));

        MvcResult result = mockMvc.perform(get("/api/procurement-responses/procurement/{id}", inquiry.getId()))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(body);

        assertTrue(root.path("code").asInt() != 200, body);
        assertFalse(body.contains("secret-solution-" + inquiry.getId()), body);
        assertFalse(body.contains(response.getContactDescription()), body);
    }

    @Test
    void withdrawnResponseCanBeResubmittedWithNewContent() {
        verifyCompany(1L);
        addActiveMember(1L, COMPANY_MEMBER_USER_ID);
        Inquiry inquiry = createInquiry(Constants.PIPELINE_NEW);
        ProcurementResponseDTO dto = responseDto(inquiry.getId());

        // 1. 首次提交
        ProcurementResponseVO first = responseService.submitResponse(COMPANY_MEMBER_USER_ID, 1L, dto);
        assertEquals(Constants.RESPONSE_SUBMITTED, first.getStatus());

        // 2. 撤回
        responseService.withdrawResponse(first.getId(), COMPANY_MEMBER_USER_ID);
        assertEquals(Constants.RESPONSE_WITHDRAWN,
                responseMapper.selectById(first.getId()).getStatus());

        // 3. 撤回后重新提交（MEDIUM-1修复：不再返回旧撤回记录，而是重新激活）
        ProcurementResponseDTO newDto = responseDto(inquiry.getId());
        newDto.setSolution("updated-solution-after-withdraw");
        newDto.setPriceDescription("updated-price");
        ProcurementResponseVO resubmitted = responseService.submitResponse(COMPANY_MEMBER_USER_ID, 1L, newDto);

        // 4. 验证：同一记录被重新激活，内容已更新，状态恢复SUBMITTED
        assertEquals(first.getId(), resubmitted.getId());
        assertEquals(Constants.RESPONSE_SUBMITTED, resubmitted.getStatus());
        ProcurementResponse dbRecord = responseMapper.selectById(first.getId());
        assertEquals(Constants.RESPONSE_SUBMITTED, dbRecord.getStatus());
        assertEquals("updated-solution-after-withdraw", dbRecord.getSolution());
        assertEquals("updated-price", dbRecord.getPriceDescription());
    }

    @Test
    void listByCompanyRejectsNonMember() {
        verifyCompany(1L);
        addActiveMember(1L, COMPANY_MEMBER_USER_ID);
        Inquiry inquiry = createInquiry(Constants.PIPELINE_NEW);
        responseService.submitResponse(COMPANY_MEMBER_USER_ID, 1L, responseDto(inquiry.getId()));

        // 非企业成员调用listByCompany应被拒绝（LOW-4修复：IDOR防护）
        assertThrows(BusinessException.class, () -> responseService.listByCompany(
                OUTSIDE_COMPANY_USER_ID, 1L, 1, 10));

        // 企业成员可正常查看
        assertEquals(1, responseService.listByCompany(
                COMPANY_MEMBER_USER_ID, 1L, 1, 10).getList().size());
    }

    private Inquiry createInquiry(String pipelineStatus) {
        Inquiry inquiry = new Inquiry();
        inquiry.setUserId(BUYER_USER_ID);
        inquiry.setName("Procurement buyer");
        inquiry.setPhone("13800000000");
        inquiry.setQuantity(10);
        inquiry.setStatus(Constants.INQUIRY_PENDING);
        inquiry.setInquiryType("PURCHASE");
        inquiry.setRequirementType("OPEN");
        inquiry.setPipelineStatus(pipelineStatus);
        inquiryMapper.insert(inquiry);
        return inquiry;
    }

    private ProcurementResponseDTO responseDto(Long procurementId) {
        ProcurementResponseDTO dto = new ProcurementResponseDTO();
        dto.setProcurementId(procurementId);
        dto.setSolution("secret-solution-" + procurementId);
        dto.setRobotIds("[1]");
        dto.setPriceDescription("secret-price");
        dto.setDeliveryDescription("two weeks");
        dto.setContactDescription("secret-contact-13800000001");
        return dto;
    }

    private void verifyCompany(Long companyId) {
        setCompanyVerification(companyId, "VERIFIED");
    }

    private void setCompanyVerification(Long companyId, String status) {
        Company company = companyMapper.selectById(companyId);
        assertTrue(company != null, "Test requires seeded company " + companyId);
        company.setVerificationStatus(status);
        company.setStatus(1);
        companyMapper.updateById(company);
    }

    private void addActiveMember(Long companyId, Long userId) {
        CompanyMember member = new CompanyMember();
        member.setCompanyId(companyId);
        member.setUserId(userId);
        member.setRole("MEMBER");
        member.setStatus(1);
        companyMemberMapper.insert(member);
    }

    private List<ProcurementFollowRecord> followRecords(Long procurementId) {
        return followRecordMapper.selectList(Wrappers.<ProcurementFollowRecord>lambdaQuery()
                .eq(ProcurementFollowRecord::getProcurementId, procurementId)
                .orderByAsc(ProcurementFollowRecord::getCreateTime));
    }
}
