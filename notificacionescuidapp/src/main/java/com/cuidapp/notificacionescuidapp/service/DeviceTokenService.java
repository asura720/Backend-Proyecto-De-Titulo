package com.cuidapp.notificacionescuidapp.service;

import com.cuidapp.notificacionescuidapp.model.DeviceToken;
import com.cuidapp.notificacionescuidapp.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository repository;

    /**
     * Registra (o reasigna) un token de dispositivo a un usuario.
     * Si el token ya existía, solo actualiza el userId; si no, lo crea.
     */
    public DeviceToken register(Long userId, String token) {
        DeviceToken saved = repository.findByToken(token)
                .map(dt -> {
                    dt.setUserId(userId);
                    return repository.save(dt);
                })
                .orElseGet(() -> repository.save(
                        DeviceToken.builder().userId(userId).token(token).build()));
        log.info("[DeviceToken] Registrado token para userId={}", userId);
        return saved;
    }

    public List<DeviceToken> forUser(Long userId) {
        return repository.findByUserId(userId);
    }
}
