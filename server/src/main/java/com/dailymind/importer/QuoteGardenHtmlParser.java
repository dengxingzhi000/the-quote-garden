package com.dailymind.importer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Node;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class QuoteGardenHtmlParser {
    public List<ParsedQuote> parse(String html, String category) {
        var doc = Jsoup.parse(html);
        var section = doc.selectFirst("div.quotes-section");
        if (section == null) return List.of();
        for (Node n : new ArrayList<>(section.childNodes()))
            if (n instanceof Comment) n.remove();
        String inner = section.html();
        String[] blocks = inner.split("(?i)(?:<br\\s*/?\\s*>\\s*){2,}");
        List<ParsedQuote> out = new ArrayList<>();
        for (String b : blocks) {
            String text = Jsoup.parse(b).wholeText().replace(' ', ' ').strip();
            if (text.isEmpty() || !text.contains("~")) continue;
            int idx = text.lastIndexOf('~');
            String content = text.substring(0, idx).replace('\n', ' ').trim().replaceAll("\\s+", " ");
            if (content.length() < 10) continue;
            String authorRaw = text.substring(idx + 1).split("\n")[0].trim();
            out.add(new ParsedQuote(content, cleanAuthor(authorRaw), category));
        }
        return out;
    }
    static String cleanAuthor(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String first = raw.split(",")[0].trim().replaceAll("\\s*\\(.*?\\)\\s*", "").trim().replaceAll("\\s*\\d{3,4}.*$", "").trim();
        return first.isEmpty() ? null : first;
    }
}
