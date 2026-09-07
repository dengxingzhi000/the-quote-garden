package com.dailymind.importer;
import com.dailymind.quote.domain.Quote;
import com.dailymind.quote.infrastructure.QuoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
public class QuoteGardenImporterHtmlTest {
    @Test void importsParsedQuotesWithDedup() {
        FakeRepo repo = new FakeRepo(Set.of());
        var importer = new QuoteGardenImporter(repo, new Normalizer(), new QuoteGardenHtmlParser());
        int n = importer.importHtml("<div class=\"quotes-section\">Whenever you are sincerely pleased, you are nourished. ~Ralph Waldo Emerson, 1860<BR><BR><BR></div>", "happiness");
        assertThat(n).isEqualTo(1);
        int n2 = importer.importHtml("<div class=\"quotes-section\">Whenever you are sincerely pleased, you are nourished. ~Ralph Waldo Emerson, 1860<BR><BR><BR></div>", "happiness");
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
}
