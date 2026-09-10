package com.quotegarden.sync.dto;

import com.quotegarden.quote.domain.Quote;
import java.util.List;

public record SyncResponse(List<Quote> items, String nextCursor) {}
