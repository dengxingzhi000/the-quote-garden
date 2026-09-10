package com.quotegarden.quote.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "quote")
public class Quote {
    @Id public String id;
    public String content;
    public String translation;
    public String author;
    public String category;
    public Integer difficulty;
    public String audioUrl;
    public String imageUrl;
    public Long updatedAt;
    public Long deletedAt;
}
