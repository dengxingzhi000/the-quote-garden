package com.quotegarden.quote.controller;

import com.quotegarden.quote.domain.Quote;
import com.quotegarden.quote.infrastructure.QuoteRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quotes")
public class QuoteController {
    private final QuoteRepository repo;
    public QuoteController(QuoteRepository repo) { this.repo = repo; }

    @GetMapping("/daily")
    public Quote daily() { return repo.findFirstByDeletedAtIsNullOrderByUpdatedAtDesc().orElseThrow(); }

    @GetMapping("/random")
    public Quote random() { return repo.findRandom().orElseThrow(); }
}
