package com.dailymind.importer;

import com.dailymind.quote.domain.Quote;
import com.dailymind.quote.infrastructure.QuoteRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Component
public class QuoteGardenImporter {
    private final QuoteRepository repo;
    private final Normalizer normalizer;

    public QuoteGardenImporter(QuoteRepository repo, Normalizer normalizer) {
        this.repo = repo;
        this.normalizer = normalizer;
    }

    @SuppressWarnings("unchecked")
    public int importFrom(String url) {
        var rt = new RestTemplate();
        var res = rt.getForObject(url, Map.class);
        List<Map> data = (List<Map>) res.get("data");
        int count = 0;
        for (Map m : data) {
            String content = normalizer.normalize((String) m.get("quoteText"));
            String hash = normalizer.hash(content);
            if (repo.existsById(hash)) continue;
            Quote q = new Quote();
            q.id = hash;
            q.content = content;
            q.author = (String) m.get("quoteAuthor");
            q.updatedAt = System.currentTimeMillis();
            repo.save(q);
            count++;
        }
        return count;
    }
}
