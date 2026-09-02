package com.dailymind.sync

import org.junit.Assert.*
import org.junit.Test

class DailySyncWorkerTest {
    @Test fun `worker exists`() {
        assertNotNull(DailySyncWorker::class.java)
    }
}
