package com.novawallet.novawallet_api.notification.controller;

import com.novawallet.novawallet_api.config.PageRequests;
import com.novawallet.novawallet_api.common.dto.ApiResponse;
import com.novawallet.novawallet_api.common.dto.PagedResponse;
import com.novawallet.novawallet_api.notification.dto.NotificationResponse;
import com.novawallet.novawallet_api.notification.entity.Notification;
import com.novawallet.novawallet_api.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for the user-facing notification inbox.
 *
 * <p>Provides paginated notification history, unread count, and
 * mark-all-as-read functionality for the authenticated user.</p>
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "User notification inbox")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            summary = "List notifications",
            description = "Returns paginated notifications for the authenticated user, newest first."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Notifications retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized"
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = extractUserId(userDetails);
        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications = notificationService.getNotifications(userId, pageable);
        List<NotificationResponse> items = notifications.getContent().stream()
                .map(NotificationResponse::from)
                .toList();
        PagedResponse<NotificationResponse> response = new PagedResponse<>(
                items,
                notifications.getNumber(),
                notifications.getSize(),
                notifications.getTotalElements(),
                notifications.getTotalPages(),
                notifications.isLast()
        );
        return ResponseEntity.ok(ApiResponse.success(response, "Notifications retrieved"));
    }

    @Operation(
            summary = "Get unread count",
            description = "Returns the number of unread notifications for the badge indicator."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Unread count retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized"
            )
    })
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = extractUserId(userDetails);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(new UnreadCountResponse(count), "Unread count retrieved"));
    }

    @Operation(
            summary = "Mark all as read",
            description = "Marks all unread notifications as read for the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "All marked as read"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized"
            )
    })
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UUID userId = extractUserId(userDetails);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All notifications marked as read"));
    }

    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }

    public record UnreadCountResponse(long count) {}
}
