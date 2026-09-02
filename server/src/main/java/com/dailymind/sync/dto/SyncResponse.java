package com.dailymind.sync.dto;

import com.dailymind.quote.domain.Quote;
import java.util.List;

public record SyncResponse(List<Quote> items, String nextCursor) {}
