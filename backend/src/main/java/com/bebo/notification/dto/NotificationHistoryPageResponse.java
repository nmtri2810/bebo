package com.bebo.notification.dto;

import com.bebo.notification.NotificationLog;
import java.util.List;
import org.springframework.data.domain.Page;

public record NotificationHistoryPageResponse(
    List<NotificationHistoryItemResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last) {

  public static NotificationHistoryPageResponse from(Page<NotificationLog> result) {
    List<NotificationHistoryItemResponse> items =
        result.getContent().stream().map(NotificationHistoryItemResponse::from).toList();

    return new NotificationHistoryPageResponse(
        items,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.isFirst(),
        result.isLast());
  }
}
