package com.cuidapp.autenticacioncuidapp.service;

import com.cuidapp.autenticacioncuidapp.model.User;
import com.cuidapp.autenticacioncuidapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // 2. Encriptar la contraseña antes de guardar (Seguridad Obligatoria)
        // Esto asegura que nadie vea la clave real en la base de datos SQL.
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 3. Guardar y retornar el usuario creado
        return userRepository.save(user);
    }

    /**
     * Busca un usuario para el proceso de login.
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
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
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}