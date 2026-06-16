package com.cuidapp.medicamentoscuidapp.repository;

import com.cuidapp.medicamentoscuidapp.model.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {

    /**
     * Busca todos los medicamentos asociados a un usuario específico.
     * Esto permitirá que al iniciar sesión en Flutter, la app recupere solo 
     * los remedios de esa persona.
     */
    List<Medication> findByUserId(Long userId);

    /**
     * Busca medicamentos cuyo nombre contenga el texto enviado.
     * IgnoreCase hace que no importe si escribes "Losartan" o "losartan".
     * Esto es vital para procesar lo que lea el OCR de Google ML Kit.
     */
    List<Medication> findByNameContainingIgnoreCase(String name);
}