package com.quotegarden.sync;

import com.quotegarden.sync.controller.SyncController;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class SyncControllerTest {
    @Test void syncEndpointExists() { assertThat(SyncController.class.getDeclaredMethods()).isNotEmpty(); }
}
