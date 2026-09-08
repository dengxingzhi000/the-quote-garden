package com.dailymind.core.designsystem

/** 句中缩写里的句点占位符（私用区字符，断句后再还原）。 */
private const val DOT_PLACEHOLDER = ''

private val abbreviationTokens = listOf(
    "Mr.", "Mrs.", "Ms.", "Dr.", "Prof.", "Sr.", "Jr.",
    "St.", "Mt.", "vs.", "etc.", "e.g.", "i.e.",
    "a.m.", "p.m.", "No.", "Ph.D."
)

/** 句末标点（+ 可选右引号/右括号）后跟空白或结尾即断句。破折号不拆，括号内从句不断。 */
private val sentenceEnd = Regex("""([.?!;:…]+["'”’)}\]]?)(\s+|$)""")

/**
 * 把引文按句子切分为展示段落（纯展示用，不改数据）。
 *
 * - 在 `. ? ! ; : …`（+ 可选右引号/括号）后断句，标点保留在段尾
 * - 不拆单字母缩写（`H. G. Wells`）、常见缩写（`Mr./Dr./St./e.g.`）、小数（`3.14`）
 * - 空白归一；空输入返回空列表
 */
fun splitQuoteSegments(content: String): List<String> {
    if (content.isBlank()) return emptyList()
    var text = content.trim().replace(Regex("\\s+"), " ")
    text = text.replace(Regex("""\b([A-Za-z])\."""), "$1$DOT_PLACEHOLDER")
    for (token in abbreviationTokens) {
        text = text.replace(token, token.replace('.', DOT_PLACEHOLDER), ignoreCase = true)
    }
    val out = mutableListOf<String>()
    var start = 0
    for (m in sentenceEnd.findAll(text)) {
        val segment = text.substring(start, m.range.last + 1).trim()
        if (segment.isNotEmpty()) out.add(segment.replace(DOT_PLACEHOLDER, '.'))
        start = m.range.last + 1
    }
    val tail = text.substring(start).trim()
    if (tail.isNotEmpty()) out.add(tail.replace(DOT_PLACEHOLDER, '.'))
    return out
}
