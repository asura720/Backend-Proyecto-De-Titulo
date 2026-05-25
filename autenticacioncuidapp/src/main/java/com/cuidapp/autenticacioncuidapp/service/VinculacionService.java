package com.cuidapp.autenticacioncuidapp.service;

import com.cuidapp.autenticacioncuidapp.model.User;
import com.cuidapp.autenticacioncuidapp.model.UserRole;
import com.cuidapp.autenticacioncuidapp.model.VinculacionCuidador;
import com.cuidapp.autenticacioncuidapp.model.VinculacionCuidador.EstadoVinculacion;
import com.cuidapp.autenticacioncuidapp.repository.UserRepository;
import com.cuidapp.autenticacioncuidapp.repository.VinculacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VinculacionService {

    private final VinculacionRepository vinculacionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // El titular envía una invitación al email del paciente
    @Transactional
    public void invitar(Long titularId, String emailPaciente) {
        User titular = userRepository.findById(titularId)
            .orElseThrow(() -> new RuntimeException("Titular no encontrado"));

        User paciente = userRepository.findByEmail(emailPaciente)
            .orElseThrow(() -> new RuntimeException("No existe un usuario registrado con ese correo"));

        if (titular.getId().equals(paciente.getId())) {
            throw new RuntimeException("No puedes vincularte contigo mismo");
        }

        boolean yaVinculado = vinculacionRepository.existsByTitularIdAndPacienteIdAndEstado(
            titularId, paciente.getId(), EstadoVinculacion.ACEPTADA);
        if (yaVinculado) {
            throw new RuntimeException("Ya estás vinculado con este paciente");
        }

        String token = UUID.randomUUID().toString();

        VinculacionCuidador vinculacion = VinculacionCuidador.builder()
            .titular(titular)
            .paciente(paciente)
            .token(token)
            .tokenExpiry(LocalDateTime.now().plusHours(48))
            .build();

        vinculacionRepository.save(vinculacion);

        // Asignar rol TITULAR si aún no lo tiene
        if (titular.getRole() == UserRole.INDEPENDIENTE) {
            titular.setRole(UserRole.TITULAR);
            userRepository.save(titular);
        }

        emailService.enviarInvitacionVinculacion(
            paciente.getEmail(),
            titular.getName(),
            paciente.getName(),
            token
        );
    }

    // El paciente acepta la invitación desde el link del email
    @Transactional
    public String aceptar(String token) {
        VinculacionCuidador vinculacion = obtenerVinculacionValida(token);

        vinculacion.setEstado(EstadoVinculacion.ACEPTADA);
        vinculacion.setRespondidoAt(LocalDateTime.now());
        vinculacionRepository.save(vinculacion);

        // Asignar rol PACIENTE
        User paciente = vinculacion.getPaciente();
        paciente.setRole(UserRole.PACIENTE);
        userRepository.save(paciente);

        // Notificar al titular
        emailService.enviarConfirmacionVinculacion(
            vinculacion.getTitular().getEmail(),
            paciente.getName()
        );

        return "Vinculación aceptada. Ya puedes cerrar esta ventana y volver a CuidApp.";
    }

    // El paciente rechaza la invitación
    @Transactional
    public String rechazar(String token) {
        VinculacionCuidador vinculacion = obtenerVinculacionValida(token);

        vinculacion.setEstado(EstadoVinculacion.RECHAZADA);
        vinculacion.setRespondidoAt(LocalDateTime.now());
        vinculacionRepository.save(vinculacion);

        return "Invitación rechazada.";
    }

    // El titular consulta su lista de pacientes vinculados
    public List<User> getMisPacientes(Long titularId) {
        return vinculacionRepository
            .findByTitularIdAndEstado(titularId, EstadoVinculacion.ACEPTADA)
            .stream()
            .map(VinculacionCuidador::getPaciente)
            .toList();
    }

    // El paciente consulta quién es su titular
    public User getMiTitular(Long pacienteId) {
        return vinculacionRepository
            .findByPacienteIdAndEstado(pacienteId, EstadoVinculacion.ACEPTADA)
            .map(VinculacionCuidador::getTitular)
            .orElseThrow(() -> new RuntimeException("No tienes un cuidador vinculado"));
    }

    // El titular desvincula a un paciente
    @Transactional
    public void desvincular(Long titularId, Long pacienteId) {
        VinculacionCuidador vinculacion = vinculacionRepository
            .findByTitularIdAndEstado(titularId, EstadoVinculacion.ACEPTADA)
            .stream()
            .filter(v -> v.getPaciente().getId().equals(pacienteId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Vinculación no encontrada"));

        vinculacion.setEstado(EstadoVinculacion.RECHAZADA);
        vinculacionRepository.save(vinculacion);

        // Revertir rol del paciente a INDEPENDIENTE
        User paciente = vinculacion.getPaciente();
        paciente.setRole(UserRole.INDEPENDIENTE);
        userRepository.save(paciente);
    }

    private VinculacionCuidador obtenerVinculacionValida(String token) {
        VinculacionCuidador vinculacion = vinculacionRepository.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (!vinculacion.isTokenValido()) {
            throw new RuntimeException("El enlace ha expirado");
        }
        if (vinculacion.getEstado() != EstadoVinculacion.PENDIENTE) {
            throw new RuntimeException("Esta invitación ya fue respondida");
        }
        return vinculacion;
    }
}
