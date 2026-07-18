package com.bebo.cycle;

import com.bebo.cycle.dto.CreateCycleRecordRequest;
import com.bebo.cycle.dto.CyclePredictionResponse;
import com.bebo.cycle.dto.CycleRecordResponse;
import com.bebo.cycle.dto.UpdateCycleRecordRequest;
import com.bebo.security.CurrentUserService;
import com.bebo.user.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cycles")
public class CycleController {

  private final CurrentUserService currentUserService;

  private final CycleService cycleService;

  public CycleController(CurrentUserService currentUserService, CycleService cycleService) {
    this.currentUserService = currentUserService;

    this.cycleService = cycleService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CycleRecordResponse create(
      Authentication authentication, @Valid @RequestBody CreateCycleRecordRequest request) {

    User user = currentUserService.requireCurrentUser(authentication);

    return cycleService.create(user, request);
  }

  @GetMapping
  public List<CycleRecordResponse> getHistory(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return cycleService.getHistory(user);
  }

  @PatchMapping("/{recordId}")
  public CycleRecordResponse update(
      Authentication authentication,
      @PathVariable UUID recordId,
      @Valid @RequestBody UpdateCycleRecordRequest request) {
    User user = currentUserService.requireCurrentUser(authentication);

    return cycleService.update(user, recordId, request);
  }

  @DeleteMapping("/{recordId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(Authentication authentication, @PathVariable UUID recordId) {

    User user = currentUserService.requireCurrentUser(authentication);

    cycleService.delete(user, recordId);
  }

  @GetMapping("/prediction")
  public CyclePredictionResponse getPrediction(Authentication authentication) {
    User user = currentUserService.requireCurrentUser(authentication);

    return cycleService.getPrediction(user);
  }
}
