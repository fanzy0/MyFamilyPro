package com.my.family.paxl.controller;

import com.my.family.paxl.context.UserContext;
import com.my.family.paxl.domain.vo.CreateEventRequest;
import com.my.family.paxl.domain.vo.EventBriefVO;
import com.my.family.paxl.domain.vo.EventDetailVO;
import com.my.family.paxl.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 家庭重要事项控制器
 * 所有接口需鉴权（AuthInterceptor 自动拦截 /api/** 路径）
 *
 * @author ai
 * @date 2026/03/17
 */
@RestController
@RequestMapping("/api/event")
@Slf4j
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 创建重要事项
     * 当前用户必须是该家庭的正式成员
     *
     * @param request 创建请求体
     * @return 创建成功后的事项详情
     */
    @PostMapping("/create")
    public ResponseEntity<EventDetailVO> createEvent(@RequestBody CreateEventRequest request) {
        try {
            if (request == null) {
                log.warn("[CreateEvent] 请求体为空");
                return ResponseEntity.badRequest().build();
            }
            if (request.getFamilyId() == null) {
                log.warn("[CreateEvent] 家庭ID为空");
                return ResponseEntity.badRequest().build();
            }
            if (!StringUtils.hasText(request.getTitle())) {
                log.warn("[CreateEvent] 事项名称为空");
                return ResponseEntity.badRequest().build();
            }
            if (!StringUtils.hasText(request.getCategory())) {
                log.warn("[CreateEvent] 事项类别为空");
                return ResponseEntity.badRequest().build();
            }
            if (request.getMonth() == null || request.getDay() == null) {
                log.warn("[CreateEvent] 月或日为空");
                return ResponseEntity.badRequest().build();
            }

            Long currentUserId = UserContext.getCurrentUserId();
            log.info("[CreateEvent] 收到请求, userId={}, familyId={}, title={}", currentUserId, request.getFamilyId(), request.getTitle());

            EventDetailVO result = eventService.createEvent(currentUserId, request);
            log.info("[CreateEvent] 创建成功, userId={}, eventId={}", currentUserId, result != null ? result.getId() : null);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.warn("[CreateEvent] 参数校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            log.warn("[CreateEvent] 权限校验失败, msg={}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            log.error("[CreateEvent] 创建事项异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 查询家庭事项列表
     * 当前用户必须是该家庭的正式成员
     *
     * @param familyId 家庭ID
     * @return 事项列表（含 isOwner 标记，true=可删除）
     */
    @GetMapping("/list")
    public ResponseEntity<List<EventBriefVO>> listEvents(@RequestParam Long familyId) {
        try {
            if (familyId == null) {
                log.warn("[ListEvents] 家庭ID为空");
                return ResponseEntity.badRequest().build();
            }

            Long currentUserId = UserContext.getCurrentUserId();
            log.info("[ListEvents] 收到请求, userId={}, familyId={}", currentUserId, familyId);

            List<EventBriefVO> list = eventService.listEvents(currentUserId, familyId);
            log.info("[ListEvents] 查询成功, userId={}, familyId={}, count={}", currentUserId, familyId, list.size());
            return ResponseEntity.ok(list);

        } catch (IllegalArgumentException e) {
            log.warn("[ListEvents] 参数校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            log.warn("[ListEvents] 权限校验失败, msg={}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            log.error("[ListEvents] 查询事项列表异常, familyId={}", familyId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 查询事项详情
     * 当前用户必须是事项所属家庭的正式成员
     *
     * @param eventId 事项ID
     * @return 事项详情（含 isOwner 标记）
     */
    @GetMapping("/detail")
    public ResponseEntity<EventDetailVO> getEventDetail(@RequestParam Long eventId) {
        try {
            if (eventId == null) {
                log.warn("[GetEventDetail] 事项ID为空");
                return ResponseEntity.badRequest().build();
            }

            Long currentUserId = UserContext.getCurrentUserId();
            log.info("[GetEventDetail] 收到请求, userId={}, eventId={}", currentUserId, eventId);

            EventDetailVO detail = eventService.getEventDetail(currentUserId, eventId);
            log.info("[GetEventDetail] 查询成功, userId={}, eventId={}", currentUserId, eventId);
            return ResponseEntity.ok(detail);

        } catch (IllegalArgumentException e) {
            log.warn("[GetEventDetail] 参数校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            log.warn("[GetEventDetail] 事项不存在, eventId={}, msg={}", eventId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            log.warn("[GetEventDetail] 权限校验失败, msg={}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            log.error("[GetEventDetail] 查询事项详情异常, eventId={}", eventId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除重要事项（逻辑删除）
     * 仅事项创建人可操作，其他成员返回 403
     *
     * @param eventId 事项ID
     * @return 操作结果
     */
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteEvent(@RequestParam Long eventId) {
        try {
            if (eventId == null) {
                log.warn("[DeleteEvent] 事项ID为空");
                return ResponseEntity.badRequest().body("事项ID不能为空");
            }

            Long currentUserId = UserContext.getCurrentUserId();
            log.info("[DeleteEvent] 收到请求, userId={}, eventId={}", currentUserId, eventId);

            eventService.deleteEvent(currentUserId, eventId);
            log.info("[DeleteEvent] 删除成功, userId={}, eventId={}", currentUserId, eventId);
            return ResponseEntity.ok("删除成功");

        } catch (IllegalArgumentException e) {
            log.warn("[DeleteEvent] 参数校验失败, msg={}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("[DeleteEvent] 事项不存在, eventId={}, msg={}", eventId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            log.warn("[DeleteEvent] 权限校验失败, msg={}", e.getMessage());
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            log.error("[DeleteEvent] 删除事项异常, eventId={}", eventId, e);
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }
}
