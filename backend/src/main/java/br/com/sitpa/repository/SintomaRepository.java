package br.com.sitpa.repository;

import br.com.sitpa.domain.Sintoma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SintomaRepository extends JpaRepository<Sintoma, Long> {

    Optional<Sintoma> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    List<Sintoma> findAllByOrderByNomeAsc();

    List<Sintoma> findByAtivoTrueOrderByNomeAsc();

    boolean existsByClassificacaoId(Long classificacaoId);
}
