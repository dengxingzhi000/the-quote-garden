package com.quotegarden.importer;

import org.springframework.stereotype.Component;

public interface TranslationService {
    /** 返回译文；无译文/失败返回 null（调用方保留原文）�?*/
    String translate(String englishContent);
}

@Component
class NoopTranslationService implements TranslationService {
    public String translate(String englishContent) { return null; }
}
