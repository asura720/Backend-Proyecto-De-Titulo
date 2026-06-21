package com.cuidapp.autenticacioncuidapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envío de correos de la app. Si el envío real está deshabilitado
 * (app.mail.enabled=false o sin credenciales), solo registra el código en el log,
 * útil para pruebas sin SMTP configurado.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:CuidApp <no-reply@cuidapp.com>}")
    private String from;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    // JavaMailSender puede ser null si no hay autoconfiguración; lo recibimos opcional.
    public EmailService(org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    /** Envía un código de seguridad para una acción sensible (recuperar/cambiar
     *  contraseña o eliminar cuenta). */
    public void sendSecurityCode(String to, String name, String code, String accion) {
        String saludo = (name != null && !name.isBlank()) ? name : "";
        String subject = "Código de seguridad de CuidApp";
        String body = "Hola " + saludo + ",\n\n"
                + "Tu código para " + accion + " es:\n\n"
                + "    " + code + "\n\n"
                + "Ingresa este código en la app para continuar. "
                + "El código vence en 30 minutos.\n\n"
                + "Si no solicitaste esto, ignora este correo y tu cuenta seguirá segura.\n\n"
                + "— Equipo CuidApp";

        if (!mailEnabled || mailSender == null) {
            log.warn("[Email] Envío deshabilitado. Código de seguridad para {}: {}", to, code);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("[Email] Código de seguridad enviado a {}", to);
        } catch (Exception e) {
            log.error("[Email] No se pudo enviar el correo a {}: {} (código: {})", to, e.getMessage(), code);
        }
    }

    /** Envía el código de verificación de cuenta. */
    public void sendVerificationCode(String to, String name, String code) {
        String saludo = (name != null && !name.isBlank()) ? name : "";
        String subject = "Verifica tu cuenta de CuidApp";
        String body = "Hola " + saludo + ",\n\n"
                + "Tu código de verificación de CuidApp es:\n\n"
                + "    " + code + "\n\n"
                + "Ingresa este código en la app para activar tu cuenta. "
                + "El código vence en 30 minutos.\n\n"
                + "Si no creaste esta cuenta, ignora este correo.\n\n"
                + "— Equipo CuidApp";

        if (!mailEnabled || mailSender == null) {
            log.warn("[Email] Envío deshabilitado. Código de verificación para {}: {}", to, code);
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("[Email] Código de verificación enviado a {}", to);
        } catch (Exception e) {
            // No interrumpimos el registro si el correo falla; el código queda en log.
            log.error("[Email] No se pudo enviar el correo a {}: {} (código: {})", to, e.getMessage(), code);
        }
    }
}
