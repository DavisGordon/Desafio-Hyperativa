package com.hyperativa.desafio.repository;

import com.hyperativa.desafio.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    boolean existsByNumberHash(String numberHash);

    Optional<Card> findByNumberHash(String numberHash);
}
