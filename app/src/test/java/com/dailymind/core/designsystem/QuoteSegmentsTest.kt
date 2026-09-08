package com.dailymind.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class QuoteSegmentsTest {
    @Test fun `single sentence returns one segment`() {
        assertEquals(
            listOf("Happiness is the manifest rule of life."),
            splitQuoteSegments("Happiness is the manifest rule of life.")
        )
    }

    @Test fun `splits on period question exclamation`() {
        assertEquals(
            listOf("Go now.", "Wait here?", "Run fast!"),
            splitQuoteSegments("Go now. Wait here? Run fast!")
        )
    }

    @Test fun `splits on semicolon and colon`() {
        assertEquals(
            listOf("Glory's not otherwhere but here:", "yonder stars may fall."),
            splitQuoteSegments("Glory's not otherwhere but here: yonder stars may fall.")
        )
    }

    @Test fun `keeps closing quote with segment`() {
        assertEquals(
            listOf("\u201cBe kind.\u201d", "Stay humble."),
            splitQuoteSegments("\u201cBe kind.\u201d Stay humble.")
        )
    }

    @Test fun `does not split single letter initials`() {
        assertEquals(
            listOf("H. G. Wells wrote about happiness daily."),
            splitQuoteSegments("H. G. Wells wrote about happiness daily.")
        )
    }

    @Test fun `does not split common abbreviations`() {
        assertEquals(
            listOf("Mr. Smith met Dr. Jones on St. Lane."),
            splitQuoteSegments("Mr. Smith met Dr. Jones on St. Lane.")
        )
    }

    @Test fun `does not split decimals`() {
        assertEquals(
            listOf("Pi is about 3.14 in value."),
            splitQuoteSegments("Pi is about 3.14 in value.")
        )
    }

    @Test fun `blank returns empty`() {
        assertEquals(emptyList<String>(), splitQuoteSegments("   "))
    }

    @Test fun `collapses whitespace`() {
        assertEquals(
            listOf("First line.", "Second line."),
            splitQuoteSegments("First line.   Second line.")
        )
    }
}
