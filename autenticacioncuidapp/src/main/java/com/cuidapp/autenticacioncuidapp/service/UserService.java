package com.cuidapp.autenticacioncuidapp.service;

import com.cuidapp.autenticacioncuidapp.model.User;
import com.cuidapp.autenticacioncuidapp.model.UserRole;
import com.cuidapp.autenticacioncuidapp.repository.UserRepository;
import com.cuidapp.autenticacioncuidapp.repository.VinculacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final VinculacionRepository vinculacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    /** Genera un código de verificación de 6 dígitos (000000-999999). */
    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    /** True si el código coincide con el almacenado y no está vencido. */
    private boolean codeValid(User user, String code) {
        if (code == null || user.getVerificationCode() == null
                || !user.getVerificationCode().equals(code)) {
            return false;
        }
        return user.getVerificationExpiry() == null
                || user.getVerificationExpiry().isAfter(java.time.LocalDateTime.now());
    }

    /**
     * Genera y envía por correo un código de seguridad para una acción sensible
     * (recuperar/cambiar contraseña o eliminar cuenta). Devuelve true si el
     * usuario existe (y se envió el código).
     */
    @Transactional
    public boolean sendActionCode(String email, String accion) {
        return userRepository.findByEmail(email).map(user -> {
            String code = generateCode();
            user.setVerificationCode(code);
            user.setVerificationExpiry(java.time.LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            emailService.sendSecurityCode(user.getEmail(), user.getName(), code, accion);
            return true;
        }).orElse(false);
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * Sustituye la lógica de 'register' en tu auth_provider.dart.
     */
    @Transactional
    public User registerUser(User user) {
        // 1. Validar si el email ya existe (Integridad de Datos)
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("El correo electrónico ya está registrado");
        }

        // 2. Encriptar la contraseña antes de guardar
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 3. Rol por defecto y ajuste según edad
        user.setRole(UserRole.INDEPENDIENTE);
        if (user.getBirthDate() != null) {
            int edad = LocalDate.now().getYear() - user.getBirthDate().getYear();
            if (edad >= 55) {
                user.setRole(UserRole.PACIENTE);
            }
        }

        // 4. Cuenta sin verificar: generar código y enviarlo por correo
        user.setEmailVerified(false);
        String code = generateCode();
        user.setVerificationCode(code);
        user.setVerificationExpiry(java.time.LocalDateTime.now().plusMinutes(30));

        User saved = userRepository.save(user);
        emailService.sendVerificationCode(saved.getEmail(), saved.getName(), code);
        return saved;
    }

    /**
     * Verifica la cuenta con el código de 6 dígitos. Devuelve true si coincide y
     * no está vencido; marca la cuenta como verificada y limpia el código.
     */
    @Transactional
    public boolean verifyCode(String email, String code) {
        return userRepository.findByEmail(email).map(user -> {
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                return true; // ya verificada
            }
            if (user.getVerificationCode() == null
                    || !user.getVerificationCode().equals(code)) {
                return false;
            }
            if (user.getVerificationExpiry() != null
                    && user.getVerificationExpiry().isBefore(java.time.LocalDateTime.now())) {
                return false; // código vencido
            }
            user.setEmailVerified(true);
            user.setVerificationCode(null);
            user.setVerificationExpiry(null);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    /**
     * Reenvía un nuevo código de verificación. Devuelve false si la cuenta no
     * existe o ya está verificada.
     */
    @Transactional
    public boolean resendCode(String email) {
        return userRepository.findByEmail(email).map(user -> {
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                return false;
            }
            String code = generateCode();
            user.setVerificationCode(code);
            user.setVerificationExpiry(java.time.LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            emailService.sendVerificationCode(user.getEmail(), user.getName(), code);
            return true;
        }).orElse(false);
    }

    /**
     * Busca un usuario para el proceso de login.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Restablece la contraseña verificando el código enviado por correo.
     * Devuelve true si el código es válido y se actualizó.
     */
    @Transactional
    public boolean resetPassword(String email, String code, String newPassword) {
        return userRepository.findByEmail(email).map(user -> {
            if (!codeValid(user, code)) {
                return false;
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setVerificationCode(null);
            user.setVerificationExpiry(null);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Activa la función de cuidador verificando el código enviado por correo.
     * Devuelve true si el código es válido.
     */
    @Transactional
    public boolean enableCaregiver(Long id, String code) {
        return userRepository.findById(id).map(user -> {
            if (!codeValid(user, code)) {
                return false;
            }
            user.setCaregiverEnabled(true);
            user.setVerificationCode(null);
            user.setVerificationExpiry(null);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    /**
     * Cambia la contraseña verificando la contraseña actual y el código enviado
     * por correo. Devuelve true si ambos son válidos y se actualizó.
     */
    @Transactional
    public boolean changePassword(Long id, String oldPassword, String code, String newPassword) {
        return userRepository.findById(id).map(user -> {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                return false;
            }
            if (!codeValid(user, code)) {
                return false;
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setVerificationCode(null);
            user.setVerificationExpiry(null);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    /**
     * Elimina la cuenta tras verificar la contraseña y el código enviado por
     * correo. Borra primero las vinculaciones (FK).
     */
    @Transactional
    public boolean deleteAccount(Long id, String password, String code) {
        return userRepository.findById(id).map(user -> {
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return false;
            }
            if (!codeValid(user, code)) {
                return false;
            }
            // Eliminar vínculos donde es titular
            vinculacionRepository.deleteAll(vinculacionRepository.findByTitularId(id));
            // Eliminar vínculo donde es paciente (si existe)
            vinculacionRepository.findByPacienteId(id)
                    .ifPresent(vinculacionRepository::delete);
            userRepository.deleteById(id);
            return true;
        }).orElse(false);
    }

    /**
     * Actualiza el perfil del usuario.
     * Mapeado directamente con el método 'updateProfile' de tu Flutter.
     */
    @Transactional
    public User updateProfile(Long id, User updatedData) {
        return userRepository.findById(id).map(user -> {
            user.setName(updatedData.getName());
            user.setPhone(updatedData.getPhone());
            user.setBirthDate(updatedData.getBirthDate());
            user.setBloodType(updatedData.getBloodType());
            user.setEmergencyContact(updatedData.getEmergencyContact());
            user.setEmergencyPhone(updatedData.getEmergencyPhone());

            // Reasignar rol por edad solo si el usuario no es TITULAR
            // (TITULAR es asignado por el sistema de vinculación, no por edad)
            if (user.getRole() != UserRole.TITULAR && updatedData.getBirthDate() != null) {
                int edad = LocalDate.now().getYear() - updatedData.getBirthDate().getYear();
                user.setRole(edad >= 55 ? UserRole.PACIENTE : UserRole.INDEPENDIENTE);
            }

            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}