package br.com.sitpa.repository;

import br.com.sitpa.domain.RegraProtocolo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegraProtocoloRepository extends JpaRepository<RegraProtocolo, Long> {

    List<RegraProtocolo> findByAtivoTrueOrderByNomeAsc();

    List<RegraProtocolo> findAllByOrderByNomeAsc();

    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, Long id);
}
