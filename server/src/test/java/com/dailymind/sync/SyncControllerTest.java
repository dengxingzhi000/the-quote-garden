package com.dailymind.sync;

import com.dailymind.sync.controller.SyncController;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class SyncControllerTest {
    @Test void syncEndpointExists() { assertThat(SyncController.class.getDeclaredMethods()).isNotEmpty(); }
}
