package com.cuidapp.autenticacioncuidapp.repository;

import com.cuidapp.autenticacioncuidapp.model.VinculacionCuidador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VinculacionRepository extends JpaRepository<VinculacionCuidador, Long> {

    List<VinculacionCuidador> findByTitularId(Long titularId);

    Optional<VinculacionCuidador> findByPacienteId(Long pacienteId);

    boolean existsByTitularIdAndPacienteId(Long titularId, Long pacienteId);

    Optional<VinculacionCuidador> findByConfirmToken(String confirmToken);
}
