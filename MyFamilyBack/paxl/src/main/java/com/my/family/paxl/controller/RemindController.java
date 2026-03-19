package com.my.family.paxl.controller;

import com.my.family.paxl.context.UserContext;
import com.my.family.paxl.domain.vo.RemindActionRequest;
import com.my.family.paxl.domain.vo.RemindHistoryVO;
import com.my.family.paxl.domain.vo.RemindLogVO;
import com.my.family.paxl.service.RemindService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提醒记录控制器
 * 提供家庭主页浮层展示、用户操作（完成/关闭）、历年回溯等接口
 *
 * @author ai
 * @date 2026/03/18
 */
@RestController
@RequestMapping("/api/remind")
@Slf4j
public class RemindController {

    private final RemindService remindService;

    public RemindController(RemindService remindService) {
        this.remindService = remindService;
    }

    /**
     * 查询家庭主页红点数量（PENDING + READ）
     *
     * @param familyId 家庭ID
     * @return 活跃提醒数量
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> countActive(@RequestParam Long familyId) {
        try {
            if (familyId == null) {
                return ResponseEntity.badRequest().build();
            }
            Long currentUserId = UserContext.getCurrentUserId();
            log.info("[CountActive] userId={}, familyId={}", currentUserId, familyId);

            int count = remindService.countActiveReminds(currentUserId, familyId);
            return ResponseEntity.ok(count);

        } catch (SecurityException e) {
            log.warn("[CountActive] 无权访问, msg={}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            log.error("[CountActive] 查询失败, familyId={}", familyId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 查询活跃提醒列表，同时将 PENDING 批量标记为 READ（用于浮层展开）
     *
     * @param familyId 家庭ID
     * @return 活跃提醒 VO 列表
     */
    @GetMapping("/active")
    public ResponseEntity<List<RemindLogVO>> listActive(@RequestParam Long familyId) {
        try {
            if (familyId == null) {
                return ResponseEntity.badRequest().build();
            }
            Long currentUserId = UserContext.getCurrentUserId();
            log.info("[ListActive] userId={}, familyId={}", currentUserId, familyId);

            List<RemindLogVO> list = remindService.listActiveReminds(currentUserId, familyId);
            log.info("[ListActive] 返回 count={}, userId={}, familyId={}", list.size(), currentUserId, familyId);
            return ResponseEntity.ok(list);

        } catch (SecurityException e) {
            log.warn("[ListActive] 无权访问, msg={}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            log.error("[ListActive] 查询失败, familyId={}", familyId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 用户标记提醒为"已完成"
     *
     * @param request 请求体（包含 remindLogId）
     * @return "OK" 或错误信息
     */
    @PostMapping("/done")
    public ResponseEntity<String> done(@RequestBody RemindActionRequest request) {
        try {
            if (request == null || request.getRemindLogId() == null) {
                return ResponseEntity.badRequest().body("remindLogId 不能为空");
            }
            Long currentUserId = UserContext.getCurrentUserId();
            log.info("[Done] userId={}, remindLogId={}", currentUserId, request.getRemindLogId());

            remindService.doneRemind(currentUserId, request.getRemindLogId());
            return ResponseEntity.ok("OK");

        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("不存在")) {
                log.warn("[Done] 记录不存在, msg={}", msg);
                return ResponseEntity.status(404).body(msg);
            }
            log.warn("[Done] 终态冲突, msg={}", msg);
            return ResponseEntity.status(409).body(msg);
        } catch (SecurityException e) {
            log.warn("[Done] 无权操作, msg={}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            log.error("[Done] 操作失败, request={}", request, e);
            return ResponseEntity.internalServerError().body("操作失败: " + e.getMessage());
        }
    }

    /**
     * 用户关闭/忽略一条提醒
     *
     * @param request 请求体（包含 remindLogId）
     * @return "OK" 或错误信息
     */
    @PostMapping("/close")
    public ResponseEntity<String> close(@RequestBody RemindActionRequest request) {
        try {
            if (request == null || request.getRemindLogId() == null) {
                return ResponseEntity.badRequest().body("remindLogId 不能为空");
            }
            Long currentUserId = UserContext.getCurrentUserId();
            log.info("[Close] userId={}, remindLogId={}", currentUserId, request.getRemindLogId());

            remindService.closeRemind(currentUserId, request.getRemindLogId());
            return ResponseEntity.ok("OK");

        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("不存在")) {
                log.warn("[Close] 记录不存在, msg={}", msg);
                return ResponseEntity.status(404).body(msg);
            }
            log.warn("[Close] 终态冲突, msg={}", msg);
            return ResponseEntity.status(409).body(msg);
        } catch (SecurityException e) {
            log.warn("[Close] 无权操作, msg={}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            log.error("[Close] 操作失败, request={}", request, e);
            return ResponseEntity.internalServerError().body("操作失败: " + e.getMessage());
        }
    }

    /**
     * 查询某事项的历年提醒记录（含全部状态），用于重要事项详情页历年完成情况展示
     *
     * @param eventId 事项ID
     * @return 历年提醒记录 VO 列表，按 trigger_year DESC 排序
     */
    @GetMapping("/history")
    public ResponseEntity<List<RemindHistoryVO>> history(@RequestParam Long eventId) {
        try {
            if (eventId == null) {
                return ResponseEntity.badRequest().build();
            }
            Long currentUserId = UserContext.getCurrentUserId();
            log.info("[History] userId={}, eventId={}", currentUserId, eventId);

            List<RemindHistoryVO> list = remindService.listRemindHistory(currentUserId, eventId);
            log.info("[History] 返回 count={}, userId={}, eventId={}", list.size(), currentUserId, eventId);
            return ResponseEntity.ok(list);

        } catch (SecurityException e) {
            log.warn("[History] 无权访问, msg={}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            log.error("[History] 查询失败, eventId={}", eventId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
