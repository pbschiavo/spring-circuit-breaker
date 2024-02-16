package com.example.dominio.adaptadores.services;

import com.example.dominio.adaptadores.model.Solicitacao;
import com.example.dominio.portas.interfaces.CacheCleanerPort;
import com.example.dominio.portas.interfaces.SolicitacaoServicePort;
import com.example.infrastructure.adaptadores.entidades.SolicitacaoEntity;
import com.example.infrastructure.adaptadores.repositories.SolicitacaoRepository;
import com.example.infrastructure.exception.BadRequestException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class SolicitacaoServiceImp implements SolicitacaoServicePort {

    private final SolicitacaoRepository solicitacaoRepository;
    private final CacheCleanerPort cacheCleanerPort;

    @Override
    @CircuitBreaker(name = "solicitacao", fallbackMethod = "solicCircuitBreaker")
    public void solicitacao(Solicitacao solicitacao) {
        log.error("erro para simular o circuit breaker");
        throw new BadRequestException("erro para simular o circuit breaker");
    }

    public void solicCircuitBreaker(Solicitacao solicitacao, Throwable e) {
        log.info("Solicitando pelo circuit breaker");
        SolicitacaoEntity solicitacaoEntity;
        if (Objects.isNull(solicitacao.codigo()))
            solicitacaoEntity = new SolicitacaoEntity(solicitacao);
        else {
            solicitacaoEntity = this.solicitacaoRepository.findById(solicitacao.codigo()).get();
            solicitacaoEntity.atualizar(solicitacao);
        }

        this.solicitacaoRepository.save(solicitacaoEntity);
        cacheCleanerPort.limpaCache();
        cacheCleanerPort.atualizarCacheSolicitacoes();
    }

    @Override
    @Cacheable(value = "solicitacoes")
    public List<Solicitacao> buscarTodasSolicitacoes() {
        log.info("consultando na base e criando cache pela primeira vez");
        List<SolicitacaoEntity> produtoEntities = this.solicitacaoRepository.findAll();
        return produtoEntities.stream().map(SolicitacaoEntity::toProduto).collect(Collectors.toList());
    }
}
