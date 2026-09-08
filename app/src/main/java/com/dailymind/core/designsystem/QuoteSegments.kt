package com.dailymind.core.designsystem

/**
 * PUA placeholder (U+E000..U+F8FF) for periods we want to keep inside
 * abbreviations while still letting `sentenceEnd` split on real sentence
 * terminators. We restore the original '.' at the end.
 */
private const val DOT_PLACEHOLDER = ''

private val abbreviationTokens = listOf(
    "Mr.", "Mrs.", "Ms.", "Dr.", "Prof.", "Sr.", "Jr.",
    "St.", "Mt.", "vs.", "etc.", "e.g.", "i.e.",
    "a.m.", "p.m.", "No.", "Ph.D."
)

/**
 * Sentence-end punctuation (with optional closing quote/bracket) followed by
 * whitespace or end-of-input. Em-dashes are not split, parenthetical clauses
 * stay inside their sentence.
 */
private val sentenceEnd = Regex("""([.?!;:…]+["'”’)}\]]?)(\s+|$)""")

/**
 * Split a quote into presentation paragraphs on sentence-ending punctuation
 * while preserving single-letter initials (e.g. `H. G. Wells`), common
 * abbreviations (`Mr./Dr./St./e.g./i.e./Ph.D.`), and decimals (`3.14`).
 *
 * - Punctuation is kept at the end of each segment.
 * - Whitespace is normalized; blank input returns an empty list.
 */
fun splitQuoteSegments(content: String): List<String> {
    if (content.isBlank()) return emptyList()
    var text = content.trim().replace(Regex("\\s+"), " ")
    text = text.replace(Regex("""\b([A-Za-z])\."""), "$1$DOT_PLACEHOLDER")
    for (token in abbreviationTokens) {
        text = text.replace(token, token.replace(".", DOT_PLACEHOLDER.toString()), ignoreCase = true)
    }
    val out = mutableListOf<String>()
    var start = 0
    for (m in sentenceEnd.findAll(text)) {
        val segment = text.substring(start, m.range.last + 1).trim()
        if (segment.isNotEmpty()) out.add(segment.replace(DOT_PLACEHOLDER.toString(), "."))
        start = m.range.last + 1
    }
    val tail = text.substring(start).trim()
    if (tail.isNotEmpty()) out.add(tail.replace(DOT_PLACEHOLDER.toString(), "."))
    return out
}
