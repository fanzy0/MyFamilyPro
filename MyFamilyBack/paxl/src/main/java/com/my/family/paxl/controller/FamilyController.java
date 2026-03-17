package com.my.family.paxl.controller;

import com.my.family.paxl.context.UserContext;
import com.my.family.paxl.domain.entity.FamilyDO;
import com.my.family.paxl.domain.entity.FamilyMemberDO;
import com.my.family.paxl.domain.vo.FamilyBriefVO;
import com.my.family.paxl.service.FamilyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 家庭控制器
 * 提供创建家庭、加入家庭、审批成员、查询家庭列表等接口
 *
 * @author ai
 * @date 2026/03/13
 */
@RestController
@RequestMapping("/api/family")
@Slf4j
public class FamilyController {

    private static final String MSG_UNAUTHORIZED = "未登录或身份失效";

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    /**
     * 创建家庭
     *
     * @param request 请求体，包含 familyName
     * @return 创建结果
     */
    @PostMapping("/create")
    public ResponseEntity<FamilyDO> createFamily(@RequestBody CreateFamilyRequest request) {
        try {
            if (request == null || !StringUtils.hasText(request.getFamilyName())) {
                return ResponseEntity.badRequest().build();
            }

            Long currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null) {
                log.warn("[CreateFamily] UserContext 为空，返回 401");
                return ResponseEntity.status(401).build();
            }

            FamilyDO family = familyService.createFamily(currentUserId, request.getFamilyName(), request.getMeetDate());
            log.info("[CreateFamily] 创建家庭成功, familyId={}, userId={}", family.getId(), currentUserId);
            return ResponseEntity.ok(family);

        } catch (IllegalArgumentException e) {
            log.warn("[CreateFamily] 参数校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[CreateFamily] 创建家庭异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 按家庭编号查询家庭（加入前预览）
     *
     * @param familyCode 家庭编号
     * @return 家庭基础信息
     */
    @GetMapping("/by-code")
    public ResponseEntity<FamilyDO> getFamilyByCode(@RequestParam("familyCode") String familyCode) {
        try {
            if (!StringUtils.hasText(familyCode)) {
                return ResponseEntity.badRequest().build();
            }

            FamilyDO family = familyService.getByFamilyCode(familyCode);
            if (family == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(family);

        } catch (IllegalArgumentException e) {
            log.warn("[GetFamilyByCode] 参数校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[GetFamilyByCode] 查询家庭异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 申请加入家庭
     *
     * @param request 请求体，包含 familyCode
     * @return 操作结果
     */
    @PostMapping("/join")
    public ResponseEntity<String> applyToJoin(@RequestBody JoinFamilyRequest request) {
        try {
            if (request == null || !StringUtils.hasText(request.getFamilyCode())) {
                return ResponseEntity.badRequest().body("家庭编号不能为空");
            }

            Long currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null) {
                log.warn("[ApplyToJoin] UserContext 为空，返回 401");
                return ResponseEntity.status(401).body(MSG_UNAUTHORIZED);
            }

            familyService.applyToJoin(currentUserId, request.getFamilyCode());
            log.info("[ApplyToJoin] 提交加入申请成功, userId={}", currentUserId);
            return ResponseEntity.ok("已提交加入申请，请等待户主审批");

        } catch (IllegalArgumentException e) {
            log.warn("[ApplyToJoin] 参数或业务校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("[ApplyToJoin] 申请加入家庭异常", e);
            return ResponseEntity.internalServerError().body("申请加入家庭失败: " + e.getMessage());
        }
    }

    /**
     * 查询当前户主的待审批成员列表
     *
     * @param familyId 家庭ID
     * @return 待审批成员列表
     */
    @GetMapping("/pending-members")
    public ResponseEntity<List<FamilyMemberDO>> listPendingMembers(@RequestParam("familyId") Long familyId) {
        try {
            Long currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null) {
                log.warn("[ListPendingMembers] UserContext 为空，返回 401");
                return ResponseEntity.status(401).build();
            }

            List<FamilyMemberDO> pendingList = familyService.listPendingMembers(currentUserId, familyId);
            log.info("[ListPendingMembers] 查询待审批成员成功, familyId={}, count={}", familyId, pendingList.size());
            return ResponseEntity.ok(pendingList);

        } catch (IllegalArgumentException e) {
            log.warn("[ListPendingMembers] 参数或业务校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[ListPendingMembers] 查询待审批成员异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 户主审批通过成员
     *
     * @param request 审批请求
     * @return 操作结果
     */
    @PostMapping("/approve")
    public ResponseEntity<String> approveMember(@RequestBody ApproveMemberRequest request) {
        try {
            if (request == null || request.getFamilyId() == null || request.getFamilyMemberId() == null) {
                return ResponseEntity.badRequest().body("familyId 和 familyMemberId 不能为空");
            }

            Long currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null) {
                log.warn("[ApproveMember] UserContext 为空，返回 401");
                return ResponseEntity.status(401).body(MSG_UNAUTHORIZED);
            }

            familyService.approveMember(currentUserId, request.getFamilyId(), request.getFamilyMemberId());
            log.info("[ApproveMember] 审批通过成功, familyId={}, familyMemberId={}",
                    request.getFamilyId(), request.getFamilyMemberId());
            return ResponseEntity.ok("审批通过成功");

        } catch (IllegalArgumentException e) {
            log.warn("[ApproveMember] 参数或业务校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("[ApproveMember] 审批通过异常", e);
            return ResponseEntity.internalServerError().body("审批通过失败: " + e.getMessage());
        }
    }

    /**
     * 户主审批拒绝成员
     *
     * @param request 审批请求
     * @return 操作结果
     */
    @PostMapping("/reject")
    public ResponseEntity<String> rejectMember(@RequestBody ApproveMemberRequest request) {
        try {
            if (request == null || request.getFamilyId() == null || request.getFamilyMemberId() == null) {
                return ResponseEntity.badRequest().body("familyId 和 familyMemberId 不能为空");
            }

            Long currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null) {
                log.warn("[RejectMember] UserContext 为空，返回 401");
                return ResponseEntity.status(401).body(MSG_UNAUTHORIZED);
            }

            familyService.rejectMember(currentUserId, request.getFamilyId(), request.getFamilyMemberId());
            log.info("[RejectMember] 审批拒绝成功, familyId={}, familyMemberId={}",
                    request.getFamilyId(), request.getFamilyMemberId());
            return ResponseEntity.ok("审批拒绝成功");

        } catch (IllegalArgumentException e) {
            log.warn("[RejectMember] 参数或业务校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("[RejectMember] 审批拒绝异常", e);
            return ResponseEntity.internalServerError().body("审批拒绝失败: " + e.getMessage());
        }
    }

    /**
     * 查询当前用户已加入的家庭列表
     *
     * @return 家庭列表
     */
    @GetMapping("/my-families")
    public ResponseEntity<List<FamilyBriefVO>> listMyFamilies() {
        try {
            Long currentUserId = UserContext.getCurrentUserId();
            if (currentUserId == null) {
                log.warn("[ListMyFamilies] UserContext 为空，返回 401");
                return ResponseEntity.status(401).build();
            }

            List<FamilyBriefVO> families = familyService.listMyFamilies(currentUserId);
            log.info("[ListMyFamilies] 查询家庭列表成功, userId={}, count={}", currentUserId, families.size());
            return ResponseEntity.ok(families);

        } catch (IllegalArgumentException e) {
            log.warn("[ListMyFamilies] 参数或业务校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[ListMyFamilies] 查询家庭列表异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 创建家庭请求体
     */
    public static class CreateFamilyRequest {

        private String familyName;

        /**
         * 相识相遇日期，可为空，格式 YYYY-MM-DD
         */
        private String meetDate;

        public String getFamilyName() {
            return familyName;
        }

        public void setFamilyName(String familyName) {
            this.familyName = familyName;
        }

        public String getMeetDate() {
            return meetDate;
        }

        public void setMeetDate(String meetDate) {
            this.meetDate = meetDate;
        }
    }

    /**
     * 申请加入家庭请求体
     */
    public static class JoinFamilyRequest {
        private String familyCode;

        public String getFamilyCode() {
            return familyCode;
        }

        public void setFamilyCode(String familyCode) {
            this.familyCode = familyCode;
        }
    }

    /**
     * 审批成员请求体
     */
    public static class ApproveMemberRequest {
        private Long familyId;
        private Long familyMemberId;

        public Long getFamilyId() {
            return familyId;
        }

        public void setFamilyId(Long familyId) {
            this.familyId = familyId;
        }

        public Long getFamilyMemberId() {
            return familyMemberId;
        }

        public void setFamilyMemberId(Long familyMemberId) {
            this.familyMemberId = familyMemberId;
        }
    }
}

