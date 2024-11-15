package com.tms.sportlight.controller;

import com.tms.sportlight.domain.NotiGrade;
import com.tms.sportlight.domain.NotiType;
import com.tms.sportlight.domain.Notification;
import com.tms.sportlight.dto.NotificationDTO;
import com.tms.sportlight.service.NotificationService;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

  private final NotificationService notificationService;
  private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  /**
   * SSE 구독
   * @return
   */
  @GetMapping("/subcribe")
  public SseEmitter subscribe() {
    SseEmitter emitter = new SseEmitter(0L);
    emitters.add(emitter);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    return emitter;
  }

  /**
   * 전달받은 메시지를 SseEmitter에 전송
   */
  private void sendNotification(Notification notification) {
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().name("notification").data(notification));
      } catch (IOException e) {
        emitters.remove(emitter);
      }
    }
  }

  /**
   * 알림 메시지 생성
   * @param userid  유저id
   * @param title 알림 제목
   * @param content 알림 내용
   * @param type 알림타입
   * @param target_grade 알림 대상 등급
   */
  @PostMapping("/")
  public void createNotification(long userid, String title, String content, NotiType type, NotiGrade target_grade){
    NotificationDTO notificationDTO = NotificationDTO.builder()
        .userId(userid)
        .notiTitle(title)
        .notiContent(content)
        .notiType(type)
        .notiGrade(target_grade)
        .build();

    Notification notification = notificationService.insertNotification(notificationDTO);
    sendNotification(notification);
  }

  @PatchMapping("/{id}")
  public void updateNotification(long id) {
      Notification notification = notificationService.modifyNotification(id);
      sendNotification(notification);
  }

  @DeleteMapping("/{id}")
  public void deleteNotification(long id) {
    notificationService.removeNotification(id);
  }

  @DeleteMapping("/")
  public void deleteAllNotification() {
    notificationService.removeAllNotification();
  }

}
