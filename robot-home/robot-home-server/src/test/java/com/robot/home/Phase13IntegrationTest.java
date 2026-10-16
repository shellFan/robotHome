package com.robot.home;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.robot.home.company.entity.Company;
import com.robot.home.company.entity.CompanyClaim;
import com.robot.home.company.entity.CompanyProfileChange;
import com.robot.home.company.mapper.CompanyClaimMapper;
import com.robot.home.company.mapper.CompanyMapper;
import com.robot.home.company.mapper.CompanyProfileChangeMapper;
import com.robot.home.company.mapper.CompanyMemberMapper;
import com.robot.home.company.service.CompanyClaimService;
import com.robot.home.company.service.CompanyProfileChangeService;
import com.robot.home.common.util.RedisUtils;
import com.robot.home.common.util.JwtUtils;
import com.robot.home.company.entity.CompanyMember;
import com.robot.home.seo.entity.SeoPageView;
import com.robot.home.seo.mapper.SeoPageViewMapper;
import com.robot.home.seo.service.SeoService;
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
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDataSourceInitializer.class)
@Transactional
class Phase13IntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CompanyMapper companyMapper;
    @Autowired
    private CompanyClaimMapper claimMapper;
    @Autowired
    private CompanyProfileChangeMapper profileChangeMapper;
    @Autowired
    private SeoPageViewMapper pageViewMapper;
    @Autowired
    private CompanyClaimService claimService;
    @Autowired
    private CompanyProfileChangeService profileChangeService;
    @Autowired
    private CompanyMemberMapper companyMemberMapper;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private SeoService seoService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private RedisUtils redisUtils;

    @Test
    void anonymousLatestClaimDoesNotExposeApplicantOrProofMaterial() throws Exception {
        Company company = existingCompany();
        CompanyClaim claim = new CompanyClaim();
        claim.setCompanyId(company.getId());
        claim.setApplicantUserId(900001L);
        claim.setApplicantName("Private Applicant Name");
        claim.setApplicantPhone("13800001234");
        claim.setApplicantEmail("private@example.invalid");
        claim.setClaimReason("Private reason");
        claim.setProofMaterials("https://files.example.invalid/private-proof.pdf");
        claim.setStatus("PENDING");
        claimMapper.insert(claim);

        MvcResult result = mockMvc.perform(get("/api/company-claims/latest")
                        .param("companyId", String.valueOf(company.getId())))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String response = result.getResponse().getContentAsString();

        assertEquals(200, root.path("code").asInt());
        assertFalse(response.contains("Private Applicant Name"));
        assertFalse(response.contains("13800001234"));
        assertFalse(response.contains("private@example.invalid"));
        assertFalse(response.contains("private-proof.pdf"));
        assertNotNull(root.path("data").get("status"));
    }

    @Test
    void rejectedClaimCanBeResubmittedAndRejectedAgain() {
        Company company = existingCompany();
        CompanyClaim first = submitAndReject(company.getId(), 900002L);
        assertEquals("REJECTED", first.getStatus());

        CompanyClaim second = claimService.submitClaim(company.getId(), 900002L,
                "Applicant", "13800000002", null, null, "retry", null);
        assertEquals("PENDING", second.getStatus());

        CompanyClaim rejectedAgain = claimService.reviewClaim(second.getId(), 1L,
                "reviewer", false, "second rejection");
        assertEquals("REJECTED", rejectedAgain.getStatus());
    }

    @Test
    void pendingClaimReservesCompanyAndApplicantSlots() {
        Company company = existingCompany();
        claimService.submitClaim(company.getId(), 900004L,
                "Applicant", "13800000004", null, null, "claim", null);

        Integer reservations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM company_claim WHERE pending_company_id = ? AND pending_applicant_user_id = ?",
                Integer.class, company.getId(), 900004L);
        assertEquals(Integer.valueOf(1), reservations,
                "Pending claim must occupy both unique active slots to make concurrent submissions safe");
    }

    @Test
    void claimApprovalEnsuresApplicantIsActiveOwnerEvenWhenMembershipAlreadyExists() {
        Company company = existingCompany();
        CompanyMember existing = new CompanyMember();
        existing.setCompanyId(company.getId());
        existing.setUserId(900005L);
        existing.setRole("MEMBER");
        existing.setStatus(1);
        companyMemberMapper.insert(existing);

        CompanyClaim claim = claimService.submitClaim(company.getId(), 900005L,
                "Applicant", "13800000005", null, null, "claim", null);
        claimService.reviewClaim(claim.getId(), 1L, "reviewer", true, "approved");

        CompanyMember owner = companyMemberMapper.selectOne(Wrappers.<CompanyMember>lambdaQuery()
                .eq(CompanyMember::getCompanyId, company.getId())
                .eq(CompanyMember::getUserId, 900005L));
        assertEquals("OWNER", owner.getRole());
        assertEquals(Integer.valueOf(1), owner.getStatus());
    }

    @Test
    void profileChangeSubmissionPersistsAllMappedFields() {
        Company company = existingCompany();
        company.setVerificationStatus("VERIFIED");
        companyMapper.updateById(company);
        CompanyProfileChange saved = profileChangeService.submitChange(company.getId(), 900003L,
                "Applicant", "contactPhone", null, "old", "new", "verified contact");

        CompanyProfileChange loaded = profileChangeMapper.selectById(saved.getId());
        assertNotNull(loaded);
        assertEquals("Applicant", loaded.getApplicantName());
        assertEquals("contactPhone", loaded.getFieldName());
        assertEquals("verified contact", loaded.getChangeReason());
    }

    @Test
    void seoPageViewEventPersistsUsingTheDeclaredEntitySchema() {
        long before = pageViewMapper.selectCount(Wrappers.<SeoPageView>lambdaQuery());
        seoService.recordPageView("company_detail", "company/test", "/company/test", 1L, null,
                "anonymous-visitor", "ip-hash", "phase13-test-agent", "search", "https://example.invalid");

        long after = pageViewMapper.selectCount(Wrappers.<SeoPageView>lambdaQuery());
        assertEquals(before + 1, after, "SEO tracking must not report success while discarding an event");
    }

    @Test
    void anonymousSeoStatsRequestIsDenied() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/seo/stats")).andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotEquals(200, response.path("code").asInt(),
                "Internal SEO statistics must require the same permission as the admin stats endpoint");
    }

    @Test
    void unverifiedCompanyMemberCannotChangeWorkspaceProfile() throws Exception {
        Company company = existingCompany();
        CompanyMember member = companyMemberMapper.selectOne(Wrappers.<CompanyMember>lambdaQuery()
                .eq(CompanyMember::getCompanyId, company.getId())
                .eq(CompanyMember::getStatus, 1)
                .last("LIMIT 1"));
        assertNotNull(member, "Phase13 test requires a seeded active company member");
        String token = jwtUtils.generateToken(member.getUserId(), "phase13-member", "USER");

        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/workspace/profile-update")
                        .header("Authorization", "Bearer " + token)
                        .param("companyId", String.valueOf(company.getId()))
                        .param("fieldName", "intro")
                        .param("newValue", "unverified update"))
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotEquals(200, response.path("code").asInt(),
                "Membership alone must not grant write access before company verification");
    }

    @Test
    void companyMemberCannotChangeAnotherCompanyProfile() throws Exception {
        Company ownCompany = existingCompany();
        CompanyMember member = companyMemberMapper.selectOne(Wrappers.<CompanyMember>lambdaQuery()
                .eq(CompanyMember::getCompanyId, ownCompany.getId())
                .eq(CompanyMember::getStatus, 1)
                .last("LIMIT 1"));
        assertNotNull(member);

        Company other = new Company();
        other.setName("Other test company");
        other.setStatus(1);
        other.setVerificationStatus("VERIFIED");
        other.setIntro("original profile");
        companyMapper.insert(other);

        String token = jwtUtils.generateToken(member.getUserId(), "phase13-member", "USER");
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/workspace/profile-update")
                        .header("Authorization", "Bearer " + token)
                        .param("companyId", String.valueOf(other.getId()))
                        .param("fieldName", "intro")
                        .param("newValue", "cross-company tamper"))
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotEquals(200, response.path("code").asInt());
        assertEquals("original profile", companyMapper.selectById(other.getId()).getIntro());
    }

    private CompanyClaim submitAndReject(Long companyId, Long applicantId) {
        CompanyClaim claim = claimService.submitClaim(companyId, applicantId,
                "Applicant", "13800000001", null, null, "claim", null);
        return claimService.reviewClaim(claim.getId(), 1L, "reviewer", false, "rejected");
    }

    private Company existingCompany() {
        Company company = companyMapper.selectById(1L);
        assertTrue(company != null, "Phase13 integration test requires seeded company id 1");
        return company;
    }
}
