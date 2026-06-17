package com.cuidapp.autenticacioncuidapp.service;

import com.cuidapp.autenticacioncuidapp.model.User;
import com.cuidapp.autenticacioncuidapp.model.UserRole;
import com.cuidapp.autenticacioncuidapp.repository.UserRepository;
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
    private final PasswordEncoder passwordEncoder;

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

        // 4. Guardar y retornar el usuario creado
        return userRepository.save(user);
    }

    /**
     * Busca un usuario para el proceso de login.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Restablece la contraseña de un usuario dado su correo.
     * Devuelve true si el usuario existía y se actualizó.
     */
    @Transactional
    public boolean resetPassword(String email, String newPassword) {
        return userRepository.findByEmail(email).map(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
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