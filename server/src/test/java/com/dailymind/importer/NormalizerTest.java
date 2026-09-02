package com.dailymind.importer;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class NormalizerTest {
    @Test void deduplicatesByContentHash() {
        var n = new Normalizer();
        assertThat(n.normalize("  Hello World  ")).isEqualTo("Hello World");
        assertThat(n.hash("Hello World")).isEqualTo(n.hash("Hello World"));
    }
}
