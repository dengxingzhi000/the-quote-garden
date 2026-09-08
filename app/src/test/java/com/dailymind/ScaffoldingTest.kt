package com.dailymind
import org.junit.Test
import org.junit.Assert.*
import java.io.File
class ScaffoldingTest {
    @Test fun `version catalog exists`() {
        val found = generateSequence(File("").absoluteFile) { it.parentFile }
            .any { File(it, "gradle/libs.versions.toml").exists() }
        assertTrue(found)
    }
}
