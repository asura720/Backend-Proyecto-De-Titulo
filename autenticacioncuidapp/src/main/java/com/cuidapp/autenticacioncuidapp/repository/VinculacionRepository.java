package com.cuidapp.autenticacioncuidapp.repository;

import com.cuidapp.autenticacioncuidapp.model.VinculacionCuidador;
import com.cuidapp.autenticacioncuidapp.model.VinculacionCuidador.EstadoVinculacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VinculacionRepository extends JpaRepository<VinculacionCuidador, Long> {

    Optional<VinculacionCuidador> findByToken(String token);

    List<VinculacionCuidador> findByTitularIdAndEstado(Long titularId, EstadoVinculacion estado);

    Optional<VinculacionCuidador> findByPacienteIdAndEstado(Long pacienteId, EstadoVinculacion estado);

    boolean existsByTitularIdAndPacienteIdAndEstado(Long titularId, Long pacienteId, EstadoVinculacion estado);
}
