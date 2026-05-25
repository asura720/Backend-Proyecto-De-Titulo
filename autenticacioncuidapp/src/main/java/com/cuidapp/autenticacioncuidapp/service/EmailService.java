package com.cuidapp.autenticacioncuidapp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void enviarInvitacionVinculacion(
            String destinatario,
            String nombreTitular,
            String nombrePaciente,
            String token) {

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("CuidApp — " + nombreTitular + " quiere ser tu cuidador");

            String urlAceptar = baseUrl + "/api/auth/vincular/aceptar/" + token;
            String urlRechazar = baseUrl + "/api/auth/vincular/rechazar/" + token;

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px;">
                    <div style="background-color: #1A56DB; padding: 20px; border-radius: 12px 12px 0 0; text-align: center;">
                        <h1 style="color: white; margin: 0;">❤️ CuidApp</h1>
                        <p style="color: #c7d8ff; margin: 4px 0 0;">Tu salud en tus manos</p>
                    </div>
                    <div style="background: #f9f9ff; padding: 28px; border-radius: 0 0 12px 12px; border: 1px solid #e0e7ff;">
                        <h2 style="color: #030213;">Solicitud de vinculación</h2>
                        <p style="color: #444; font-size: 15px;">
                            <strong>%s</strong> quiere vincularse contigo como tu cuidador en CuidApp.
                        </p>
                        <p style="color: #444; font-size: 14px;">
                            Si aceptas, <strong>%s</strong> podrá ver y gestionar tus medicamentos y recordatorios.
                        </p>
                        <div style="margin: 28px 0; text-align: center;">
                            <a href="%s"
                               style="background-color: #10B981; color: white; padding: 14px 32px;
                                      text-decoration: none; border-radius: 8px; font-size: 15px;
                                      font-weight: bold; margin-right: 12px; display: inline-block;">
                                ✅ Aceptar
                            </a>
                            <a href="%s"
                               style="background-color: #d4183d; color: white; padding: 14px 32px;
                                      text-decoration: none; border-radius: 8px; font-size: 15px;
                                      font-weight: bold; display: inline-block;">
                                ❌ Rechazar
                            </a>
                        </div>
                        <p style="color: #888; font-size: 12px; text-align: center;">
                            Este enlace expira en 48 horas. Si no conoces a esta persona, ignora este mensaje.
                        </p>
                    </div>
                </div>
                """.formatted(nombreTitular, nombreTitular, urlAceptar, urlRechazar);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Error al enviar el email de invitación: " + e.getMessage());
        }
    }

    public void enviarConfirmacionVinculacion(String destinatario, String nombrePaciente) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(destinatario);
            helper.setSubject("CuidApp — Vinculación aceptada");

            String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px;">
                    <div style="background-color: #1A56DB; padding: 20px; border-radius: 12px 12px 0 0; text-align: center;">
                        <h1 style="color: white; margin: 0;">❤️ CuidApp</h1>
                    </div>
                    <div style="background: #f9f9ff; padding: 28px; border-radius: 0 0 12px 12px; border: 1px solid #e0e7ff;">
                        <h2 style="color: #10B981;">✅ ¡Vinculación exitosa!</h2>
                        <p style="color: #444; font-size: 15px;">
                            <strong>%s</strong> ha aceptado tu solicitud. Ya puedes ver y gestionar sus medicamentos desde CuidApp.
                        </p>
                    </div>
                </div>
                """.formatted(nombrePaciente);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            System.err.println("No se pudo enviar email de confirmación: " + e.getMessage());
        }
    }
}
