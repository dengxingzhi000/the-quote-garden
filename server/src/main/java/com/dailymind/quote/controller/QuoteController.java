package com.dailymind.quote.controller;

import com.dailymind.quote.domain.Quote;
import com.dailymind.quote.infrastructure.QuoteRepository;
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
