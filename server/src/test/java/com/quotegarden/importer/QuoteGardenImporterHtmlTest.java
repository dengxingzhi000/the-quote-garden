package com.quotegarden.importer;
import com.quotegarden.article.domain.Article;
import com.quotegarden.article.infrastructure.ArticleRepository;
import com.quotegarden.quote.domain.Quote;
import com.quotegarden.quote.infrastructure.QuoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
public class QuoteGardenImporterHtmlTest {
    @Test void importsParsedQuotesWithDedup() {
        FakeRepo repo = new FakeRepo(Set.of());
        FakeArticleRepo articles = new FakeArticleRepo();
        var importer = new QuoteGardenImporter(repo, new Normalizer(), new QuoteGardenHtmlParser(), articles);
        int n = importer.importHtml("<div class=\"quotes-section\">Whenever you are sincerely pleased, you are nourished. ~Ralph Waldo Emerson, 1860<BR><BR><BR></div>", "happiness");
        assertThat(n).isEqualTo(1);
        int n2 = importer.importHtml("<div class=\"quotes-section\">Whenever you are sincerely pleased, you are nourished. ~Ralph Waldo Emerson, 1860<BR><BR><BR></div>", "happiness");
        assertThat(n2).isEqualTo(0); // 去重
    }
    @Test void routesLongContentToArticleTable() {        FakeRepo repo = new FakeRepo(Set.of());
        FakeArticleRepo articles = new FakeArticleRepo();
        var importer = new QuoteGardenImporter(repo, new Normalizer(), new QuoteGardenHtmlParser(), articles);
        String longContent = "Lorem ipsum dolor sit amet. ".repeat(40);
        int n = importer.importHtml("<div class=\"quotes-section\">" + longContent + " ~Some Author<BR><BR><BR></div>", "essays");
        assertThat(n).isEqualTo(1);
        assertThat(repo.count()).isEqualTo(0);
        assertThat(articles.count()).isEqualTo(1);
    }
    @Test void importsArticlePageAsWhole() {
        FakeRepo repo = new FakeRepo(Set.of());
        FakeArticleRepo articles = new FakeArticleRepo();
        var importer = new QuoteGardenImporter(repo, new Normalizer(), new QuoteGardenHtmlParser(), articles);
        String html = "<html><head><title>Site</title></head><body><h1>Essay Title</h1>"
            + "<article><p>Para one with enough words to be valid content here.</p>"
            + "<p>Para two with more words to be safe and sound.</p></article></body></html>";
        int n = importer.importArticlePage(html, "blog-test", "https://www.quotegarden.com/blog-test.html");
        assertThat(n).isEqualTo(1);
        assertThat(repo.count()).isEqualTo(0);
        assertThat(articles.count()).isEqualTo(1);
        int n2 = importer.importArticlePage(html, "blog-test", "https://www.quotegarden.com/blog-test.html");
        assertThat(n2).isEqualTo(0);
    }
    static class FakeRepo implements QuoteRepository {
        private final Set<String> ids; private final Map<String, Quote> store = new HashMap<>();
        FakeRepo(Set<String> ids) { this.ids = new HashSet<>(ids); }
        public boolean existsById(String id) { return ids.contains(id) || store.containsKey(id); }
        public <S extends Quote> S save(S e) { ids.add(e.id); store.put(e.id, e); return e; }
        public void flush() {}
        private static UnsupportedOperationException uo() { return new UnsupportedOperationException(); }
        public List<Quote> findAll() { throw uo(); }
        public List<Quote> findAllById(Iterable<String> ids) { throw uo(); }
        public Optional<Quote> findById(String id) { throw uo(); }
        public long count() { return store.size(); }
        public void deleteById(String id) { throw uo(); }
        public void delete(Quote e) { throw uo(); }
        public void deleteAllById(Iterable<? extends String> ids) { throw uo(); }
        public void deleteAll(Iterable<? extends Quote> e) { throw uo(); }
        public void deleteAll() { throw uo(); }
        public <S extends Quote> List<S> saveAll(Iterable<S> e) { throw uo(); }
        public List<Quote> findAll(Sort s) { throw uo(); }
        public Page<Quote> findAll(Pageable p) { throw uo(); }
        public <S extends Quote> Optional<S> findOne(Example<S> e) { throw uo(); }
        public <S extends Quote> List<S> findAll(Example<S> e) { throw uo(); }
        public <S extends Quote> List<S> findAll(Example<S> e, Sort s) { throw uo(); }
        public <S extends Quote> Page<S> findAll(Example<S> e, Pageable p) { throw uo(); }
        public <S extends Quote> long count(Example<S> e) { throw uo(); }
        public <S extends Quote> boolean exists(Example<S> e) { throw uo(); }
        public <S extends Quote, R> R findBy(Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { throw uo(); }
        public <S extends Quote> S saveAndFlush(S e) { throw uo(); }
        public <S extends Quote> List<S> saveAllAndFlush(Iterable<S> e) { throw uo(); }
        public void deleteAllInBatch(Iterable<Quote> e) { throw uo(); }
        public void deleteAllByIdInBatch(Iterable<String> ids) { throw uo(); }
        public void deleteAllInBatch() { throw uo(); }
        public Quote getOne(String id) { throw uo(); }
        public Quote getById(String id) { throw uo(); }
        public Quote getReferenceById(String id) { throw uo(); }
        public Optional<Quote> findFirstByDeletedAtIsNullOrderByUpdatedAtDesc() { return Optional.empty(); }
        public Optional<Quote> findRandom() { return Optional.empty(); }
        public List<Quote> findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(Long u, Pageable p) { return List.of(); }
        public Long findMaxUpdatedAt() { return null; }
    }
    static class FakeArticleRepo implements ArticleRepository {
        private final Map<String, Article> store = new HashMap<>();
        public boolean existsById(String id) { return store.containsKey(id); }
        public <S extends Article> S save(S e) { store.put(e.id, e); return e; }
        public void flush() {}
        private static UnsupportedOperationException uo() { return new UnsupportedOperationException(); }
        public List<Article> findAll() { throw uo(); }
        public List<Article> findAllById(Iterable<String> ids) { throw uo(); }
        public Optional<Article> findById(String id) { throw uo(); }
        public long count() { return store.size(); }
        public void deleteById(String id) { throw uo(); }
        public void delete(Article e) { throw uo(); }
        public void deleteAllById(Iterable<? extends String> ids) { throw uo(); }
        public void deleteAll(Iterable<? extends Article> e) { throw uo(); }
        public void deleteAll() { throw uo(); }
        public <S extends Article> List<S> saveAll(Iterable<S> e) { throw uo(); }
        public List<Article> findAll(Sort s) { throw uo(); }
        public Page<Article> findAll(Pageable p) { throw uo(); }
        public <S extends Article> Optional<S> findOne(Example<S> e) { throw uo(); }
        public <S extends Article> List<S> findAll(Example<S> e) { throw uo(); }
        public <S extends Article> List<S> findAll(Example<S> e, Sort s) { throw uo(); }
        public <S extends Article> Page<S> findAll(Example<S> e, Pageable p) { throw uo(); }
        public <S extends Article> long count(Example<S> e) { throw uo(); }
        public <S extends Article> boolean exists(Example<S> e) { throw uo(); }
        public <S extends Article, R> R findBy(Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> q) { throw uo(); }
        public <S extends Article> S saveAndFlush(S e) { throw uo(); }
        public <S extends Article> List<S> saveAllAndFlush(Iterable<S> e) { throw uo(); }
        public void deleteAllInBatch(Iterable<Article> e) { throw uo(); }
        public void deleteAllByIdInBatch(Iterable<String> ids) { throw uo(); }
        public void deleteAllInBatch() { throw uo(); }
        public Article getOne(String id) { throw uo(); }
        public Article getById(String id) { throw uo(); }
        public Article getReferenceById(String id) { throw uo(); }
    }
}
