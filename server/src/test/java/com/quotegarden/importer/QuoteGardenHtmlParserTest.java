package com.quotegarden.importer;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.assertThat;
public class QuoteGardenHtmlParserTest {
    private String fixture() throws Exception {
        return Files.readString(Path.of("src/test/resources/fixtures/happiness_sample.html"));
    }
    @Test void parsesHappinessFixture() throws Exception {
        var quotes = new QuoteGardenHtmlParser().parse(fixture(), "happiness");
        assertThat(quotes).hasSize(3);
        assertThat(quotes.get(0).content()).startsWith("Whenever you are sincerely pleased");
        assertThat(quotes.get(0).author()).contains("Ralph Waldo Emerson");
        assertThat(quotes.get(1).author()).isEqualTo("James Oppenheim");
        assertThat(quotes.stream().anyMatch(q -> q.author() != null && q.author().contains("Chinese proverb"))).isTrue();
    }
    @Test void skipsBlocksWithoutTilde() throws Exception {
        var quotes = new QuoteGardenHtmlParser().parse("<div class=\"quotes-section\">Hello no tilde<BR><BR><BR>Real quote here ok ~Anon<BR><BR><BR></div>", "test");
        assertThat(quotes).hasSize(1);
    }
}
