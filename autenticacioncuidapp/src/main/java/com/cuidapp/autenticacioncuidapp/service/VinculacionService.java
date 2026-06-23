package com.cuidapp.autenticacioncuidapp.service;

import com.cuidapp.autenticacioncuidapp.model.User;
import com.cuidapp.autenticacioncuidapp.model.UserRole;
import com.cuidapp.autenticacioncuidapp.model.VinculacionCuidador;
import com.cuidapp.autenticacioncuidapp.repository.UserRepository;
import com.cuidapp.autenticacioncuidapp.repository.VinculacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VinculacionService {

    private final VinculacionRepository vinculacionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // URL pública del backend (gateway) para los enlaces del correo de confirmación.
    @Value("${app.public.url:http://20.119.249.151:8083}")
    private String publicUrl;

    // El titular crea directamente la cuenta del paciente bajo su cargo
    @Transactional
    public User crearPaciente(Long titularId, String name, String email, String password,
                              LocalDate birthDate, String bloodType) {
        User titular = userRepository.findById(titularId)
            .orElseThrow(() -> new RuntimeException("Titular no encontrado"));

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("El correo electrónico ya está registrado");
        }

        User paciente = new User();
        paciente.setName(name);
        paciente.setEmail(email);
        paciente.setPassword(passwordEncoder.encode(password));
        paciente.setBirthDate(birthDate);
        paciente.setBloodType(bloodType);
        paciente.setRole(UserRole.PACIENTE);
        paciente = userRepository.save(paciente);

        // Vínculo pendiente hasta que el paciente confirme por correo
        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        VinculacionCuidador vinculacion = VinculacionCuidador.builder()
            .titular(titular)
            .paciente(paciente)
            .confirmado(false)
            .confirmToken(token)
            .build();
        vinculacionRepository.save(vinculacion);

        // El titular pasa a serlo si aún era usuario independiente
        if (titular.getRole() == UserRole.INDEPENDIENTE) {
            titular.setRole(UserRole.TITULAR);
            userRepository.save(titular);
        }

        // Enviar correo al paciente con botones Confirmar / No confirmar
        String confirmUrl = publicUrl + "/api/auth/vincular/confirmar?token=" + token + "&accion=si";
        String rejectUrl = publicUrl + "/api/auth/vincular/confirmar?token=" + token + "&accion=no";
        emailService.sendVinculoConfirm(paciente.getEmail(), paciente.getName(),
                titular.getName(), confirmUrl, rejectUrl);

        return paciente;
    }

    /**
     * Confirma o rechaza un vínculo desde el correo. Devuelve un texto de estado.
     * accion="si" confirma; cualquier otro valor lo rechaza (borra el vínculo).
     */
    @Transactional
    public String confirmarVinculo(String token, String accion) {
        VinculacionCuidador v = vinculacionRepository.findByConfirmToken(token).orElse(null);
        if (v == null) {
            return "Este enlace ya no es válido o el vínculo ya fue procesado.";
        }
        if ("si".equalsIgnoreCase(accion)) {
            v.setConfirmado(true);
            v.setConfirmToken(null);
            vinculacionRepository.save(v);
            return "¡Vínculo confirmado! Ahora tu cuidador podrá ayudarte desde CuidApp.";
        } else {
            // Rechazado: se elimina el vínculo y el paciente queda independiente
            User paciente = v.getPaciente();
            vinculacionRepository.delete(v);
            paciente.setRole(UserRole.INDEPENDIENTE);
            userRepository.save(paciente);
            return "Rechazaste la vinculación. No se compartirá tu información con ese cuidador.";
        }
    }

    // El titular consulta su lista de pacientes con el estado de permiso/solicitud
    public List<Map<String, Object>> getMisPacientes(Long titularId) {
        return vinculacionRepository.findByTitularId(titularId)
            .stream()
            .map(v -> {
                User p = v.getPaciente();
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", p.getId());
                m.put("name", p.getName());
                m.put("email", p.getEmail());
                m.put("puedeGestionar", v.getPacientePuedeGestionar());
                m.put("solicitudPendiente", v.getSolicitudPendiente());
                m.put("confirmado", v.getConfirmado());
                return m;
            })
            .toList();
    }

    // El paciente solicita permiso para gestionar sus propios medicamentos
    @Transactional
    public void solicitarPermiso(Long pacienteId) {
        VinculacionCuidador vinculacion = vinculacionRepository.findByPacienteId(pacienteId)
            .orElseThrow(() -> new RuntimeException("No tienes un cuidador vinculado"));

        if (Boolean.TRUE.equals(vinculacion.getPacientePuedeGestionar())) {
            throw new RuntimeException("Ya tienes permiso para gestionar tus medicamentos");
        }

        vinculacion.setSolicitudPendiente(true);
        vinculacionRepository.save(vinculacion);
    }

    // El titular responde la solicitud del paciente (autoriza o rechaza)
    @Transactional
    public void responderSolicitud(Long titularId, Long pacienteId, boolean aprobar) {
        VinculacionCuidador vinculacion = vinculacionRepository.findByTitularId(titularId)
            .stream()
            .filter(v -> v.getPaciente().getId().equals(pacienteId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Vinculación no encontrada"));

        vinculacion.setPacientePuedeGestionar(aprobar);
        vinculacion.setSolicitudPendiente(false);
        vinculacionRepository.save(vinculacion);
    }

    // El paciente consulta si puede gestionar y si tiene una solicitud pendiente
    public Map<String, Object> getMiPermiso(Long pacienteId) {
        VinculacionCuidador vinculacion = vinculacionRepository.findByPacienteId(pacienteId)
            .orElseThrow(() -> new RuntimeException("No tienes un cuidador vinculado"));
        return Map.of(
            "puedeGestionar", vinculacion.getPacientePuedeGestionar(),
            "solicitudPendiente", vinculacion.getSolicitudPendiente()
        );
    }

    /**
     * Resuelve a quién avisar si un paciente no toma su medicamento.
     * La alerta llega SOLO a las personas del vínculo:
     *  - Siempre al propio paciente (self=true).
     *  - Y, si está vinculado, también a su cuidador titular (self=false).
     * Devuelve el nombre del paciente y la lista de destinatarios.
     */
    public Map<String, Object> resolveAlertTarget(Long pacienteId) {
        String patientName = userRepository.findById(pacienteId)
                .map(User::getName).orElse("");

        List<Map<String, Object>> recipients = new java.util.ArrayList<>();
        // El paciente siempre recibe el aviso
        recipients.add(Map.of("userId", pacienteId, "self", true));
        // El cuidador vinculado también, pero SOLO si el vínculo está confirmado
        vinculacionRepository.findByPacienteId(pacienteId).ifPresent(v -> {
            if (Boolean.TRUE.equals(v.getConfirmado())) {
                recipients.add(Map.of("userId", v.getTitular().getId(), "self", false));
            }
        });

        return Map.of(
                "patientName", patientName,
                "recipients", recipients);
    }

    // El paciente consulta quién es su titular
    public User getMiTitular(Long pacienteId) {
        return vinculacionRepository.findByPacienteId(pacienteId)
            .map(VinculacionCuidador::getTitular)
            .orElseThrow(() -> new RuntimeException("No tienes un cuidador vinculado"));
    }

    // El titular desvincula a un paciente: se elimina el vínculo y el paciente vuelve a ser independiente
    @Transactional
    public void desvincular(Long titularId, Long pacienteId) {
        VinculacionCuidador vinculacion = vinculacionRepository.findByTitularId(titularId)
            .stream()
            .filter(v -> v.getPaciente().getId().equals(pacienteId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Vinculación no encontrada"));

        User paciente = vinculacion.getPaciente();
        vinculacionRepository.delete(vinculacion);

        paciente.setRole(UserRole.INDEPENDIENTE);
        userRepository.save(paciente);
    }
}
