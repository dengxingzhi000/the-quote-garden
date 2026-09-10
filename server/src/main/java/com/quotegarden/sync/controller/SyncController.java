package com.quotegarden.sync.controller;

import com.quotegarden.quote.infrastructure.QuoteRepository;
import com.quotegarden.sync.dto.SyncResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final QuoteRepository repo;

    public SyncController(QuoteRepository repo) { this.repo = repo; }

    @GetMapping("/quotes")
    public SyncResponse sync(
            @RequestParam Long updatedAfter,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "100") int limit) {
        var page = PageRequest.of(cursor == null ? 0 : Integer.parseInt(cursor), limit);
        var items = repo.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(updatedAfter, page);
        String next = items.size() == limit ? String.valueOf(page.getPageNumber() + 1) : null;
        return new SyncResponse(items, next);
    }
}
