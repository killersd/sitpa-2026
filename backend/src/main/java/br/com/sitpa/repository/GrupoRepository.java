package br.com.sitpa.repository;

import br.com.sitpa.domain.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    Optional<Grupo> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    List<Grupo> findByAtivoTrueOrderByNomeAsc();

    List<Grupo> findAllByOrderByNomeAsc();
}
