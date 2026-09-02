package com.dailymind
import org.junit.Test
import org.junit.Assert.*
import java.io.File
class ScaffoldingTest {
    @Test fun `version catalog exists`() {
        assertTrue(File("gradle/libs.versions.toml").exists())
    }
}
