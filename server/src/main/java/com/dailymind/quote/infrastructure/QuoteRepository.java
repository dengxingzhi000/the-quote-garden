package com.dailymind.quote.infrastructure;

import com.dailymind.quote.domain.Quote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface QuoteRepository extends JpaRepository<Quote, String> {
    Optional<Quote> findFirstByDeletedAtIsNullOrderByUpdatedAtDesc();
    @Query(value = "SELECT * FROM quote WHERE deleted_at IS NULL ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Quote> findRandom();
    List<Quote> findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(Long updatedAfter, Pageable pageable);
    @Query("SELECT MAX(q.updatedAt) FROM Quote q")
    Long findMaxUpdatedAt();
}
