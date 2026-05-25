package com.cuidapp.notificacionescuidapp.repository;

import com.cuidapp.notificacionescuidapp.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findBySentFalseAndScheduledAtBefore(LocalDateTime time);

}
