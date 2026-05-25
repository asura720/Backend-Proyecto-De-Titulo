package com.cuidapp.notificacionescuidapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long userId;

	private Long medicationId;

	private String medicationName;

	private String title;

	@Column(length = 1000)
	private String message;

	private LocalDateTime scheduledAt;

	@Builder.Default
	private Boolean sent = false;

	@Builder.Default
	private LocalDateTime createdAt = LocalDateTime.now();
}
