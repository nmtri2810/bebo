package com.bebo.notification;

import com.bebo.common.exception.BadRequestException;
import com.bebo.notification.dto.NotificationHistoryPageResponse;
import com.bebo.user.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationHistoryService {

  private static final int MAX_PAGE_SIZE = 50;

  private static final List<NotificationStatus> VISIBLE_STATUSES =
      List.of(NotificationStatus.SENT, NotificationStatus.FAILED);

  private final NotificationLogRepository notificationLogRepository;

  public NotificationHistoryService(NotificationLogRepository notificationLogRepository) {
    this.notificationLogRepository = notificationLogRepository;
  }

  @Transactional(readOnly = true)
  public NotificationHistoryPageResponse getHistory(User currentUser, int page, int size) {
    validatePagination(page, size);

    PageRequest pageable =
        PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "scheduledFor").and(Sort.by(Sort.Direction.DESC, "id")));

    Page<NotificationLog> result =
        notificationLogRepository.findAllByUser_IdAndStatusIn(
            currentUser.getId(), VISIBLE_STATUSES, pageable);

    return NotificationHistoryPageResponse.from(result);
  }

  private void validatePagination(int page, int size) {
    if (page < 0) {
      throw new BadRequestException("Page must not be negative");
    }

    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new BadRequestException("Size must be between 1 and " + MAX_PAGE_SIZE);
    }
  }
}
