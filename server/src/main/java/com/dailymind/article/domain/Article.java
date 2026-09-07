package com.dailymind.article.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "article")
public class Article {
    @Id public String id;
    public String content;
    public String translation;
    public String author;
    public String category;
    public Long updatedAt;
    public Long deletedAt;
}
