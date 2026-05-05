package com.rb.feed_ask_ai.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "rags")
public class RagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    @Lob
    private String content;
    private Integer pageNumber;
    @Column(columnDefinition = "json")
    private String embedding;

}
