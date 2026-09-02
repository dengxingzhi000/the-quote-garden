package com.dailymind.quote;

import com.dailymind.quote.controller.QuoteController;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class QuoteControllerTest {
    @Test void contextLoads() { assertThat(QuoteController.class).isNotNull(); }
}
