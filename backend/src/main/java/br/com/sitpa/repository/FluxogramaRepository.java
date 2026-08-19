package br.com.sitpa.repository;

import br.com.sitpa.domain.Fluxograma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FluxogramaRepository extends JpaRepository<Fluxograma, Long> {

    Optional<Fluxograma> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    List<Fluxograma> findAllByOrderByNomeAsc();

    List<Fluxograma> findByGrupoIdOrderByNomeAsc(Long grupoId);

    List<Fluxograma> findByGrupoIdAndAtivoTrueOrderByNomeAsc(Long grupoId);

    boolean existsByGrupoId(Long grupoId);
}
