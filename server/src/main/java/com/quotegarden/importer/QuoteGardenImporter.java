package com.quotegarden.importer;
import com.quotegarden.article.domain.Article;
import com.quotegarden.article.infrastructure.ArticleRepository;
import com.quotegarden.quote.domain.Quote;
import com.quotegarden.quote.infrastructure.QuoteRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Component
public class QuoteGardenImporter {
    /** 名言与文章的长度分界（字符数），�?V2 迁移�?Python 爬虫保持一�? */
    public static final int ARTICLE_THRESHOLD = 800;
    private final QuoteRepository repo;
    private final Normalizer normalizer;
    private final QuoteGardenHtmlParser parser;
    private final ArticleRepository articles;
    public QuoteGardenImporter(QuoteRepository repo, Normalizer normalizer, QuoteGardenHtmlParser parser, ArticleRepository articles) {
        this.repo = repo; this.normalizer = normalizer; this.parser = parser; this.articles = articles;
    }
    /** HTML分类页导入（新主路径）：幂等，长文路由到 article 表，去重后返回新增数. */
    public int importHtml(String html, String category) {
        int count = 0;
        for (ParsedQuote p : parser.parse(html, category)) {
            String content = normalizer.normalize(p.content());
            if (content == null || content.isBlank()) continue;
            String hash = normalizer.hash(content);
            if (content.length() >= ARTICLE_THRESHOLD) {
                if (articles.existsById(hash)) continue;
                Article a = new Article();
                a.id = hash; a.content = content; a.author = p.author(); a.category = p.category();
                a.updatedAt = System.currentTimeMillis();
                articles.save(a); count++;
            } else {
                if (repo.existsById(hash)) continue;
                Quote q = new Quote();
                q.id = hash; q.content = content; q.author = p.author(); q.category = p.category();
                q.updatedAt = System.currentTimeMillis();
                repo.save(q); count++;
            }
        }
        return count;
    }
    /** 整文导入：单�?essay 页整体存�?article（标�?+ 来源链接），段落保留空行分隔. */
    public int importArticlePage(String html, String category, String url) {
        var doc = Jsoup.parse(html);
        var h1 = doc.selectFirst("h1");
        String title = h1 != null ? h1.text().strip() : null;
        if ((title == null || title.isBlank()) && doc.title() != null) {
            title = doc.title().split("\\|")[0].strip();
        }
        var body = doc.selectFirst("article");
        if (body == null) body = doc.selectFirst("div.quotes-section");
        if (body == null || title == null || title.isBlank()) return 0;
        List<String> paras = new ArrayList<>();
        if (!body.select("p").isEmpty()) {
            for (Node child : new ArrayList<>(body.childNodes())) {
                if (child instanceof Comment) continue;
                String t;
                if (child instanceof TextNode tn) t = tn.text();
                else if (child instanceof Element el) t = el.text();
                else continue;
                t = t.strip().replaceAll("\\s+", " ");
                if (!t.isEmpty()) paras.add(t);
            }
        } else {
            for (String chunk : body.wholeText().split("\\n\\s*\\n")) {
                String t = chunk.strip().replaceAll("\\s+", " ");
                if (!t.isEmpty()) paras.add(t);
            }
        }
        String content = String.join("\n\n", paras);
        if (content.length() < 100) return 0;
        String hash = normalizer.hash(url);
        if (articles.existsById(hash)) return 0;
        Article a = new Article();
        a.id = hash; a.title = title; a.sourceUrl = url;
        a.content = content; a.category = category;
        a.updatedAt = System.currentTimeMillis();
        articles.save(a);
        return 1;
    }
    /** 旧JSON API路径保留兼容，标记废�? */
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
