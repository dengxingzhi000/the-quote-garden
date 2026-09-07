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
    private final QuoteGardenHtmlParser parser;
    public QuoteGardenImporter(QuoteRepository repo, Normalizer normalizer, QuoteGardenHtmlParser parser) {
        this.repo = repo; this.normalizer = normalizer; this.parser = parser;
    }
    /** HTML分类页导入（新主路径）：幂等，去重后返回新增数. */
    public int importHtml(String html, String category) {
        int count = 0;
        for (ParsedQuote p : parser.parse(html, category)) {
            String content = normalizer.normalize(p.content());
            if (content == null || content.isBlank()) continue;
            String hash = normalizer.hash(content);
            if (repo.existsById(hash)) continue;
            Quote q = new Quote();
            q.id = hash; q.content = content; q.author = p.author(); q.category = p.category();
            q.updatedAt = System.currentTimeMillis();
            repo.save(q); count++;
        }
        return count;
    }
    /** 旧JSON API路径保留兼容，标记废弃. */
    @Deprecated
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
            q.id = hash; q.content = content; q.author = (String) m.get("quoteAuthor");
            q.updatedAt = System.currentTimeMillis();
            repo.save(q); count++;
        }
        return count;
    }
}
