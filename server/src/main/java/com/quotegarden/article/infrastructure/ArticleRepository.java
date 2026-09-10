package com.quotegarden.article.infrastructure;

import com.quotegarden.article.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, String> {
}
