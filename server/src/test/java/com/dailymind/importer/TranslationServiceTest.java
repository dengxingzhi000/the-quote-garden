package com.dailymind.importer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
public class TranslationServiceTest {
    @Test void noopReturnsNull() {
        TranslationService svc = new NoopTranslationService();
        assertThat(svc.translate("Hello")).isNull();
    }
}
