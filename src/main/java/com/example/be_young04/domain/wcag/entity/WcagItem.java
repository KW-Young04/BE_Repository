package com.example.be_young04.domain.wcag.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "WCAG_ITEMS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WcagItem {

    @Id
    @Column(name = "WCAG_ITEM_ID")
    private Long wcagItemId;

    @Column(name = "SC", nullable = false, length = 20)
    private String sc;

    @Column(name = "TITLE", nullable = false)
    private String title;

    @Column(name = "LEVEL_TYPE", nullable = false, length = 5)
    private String levelType; // A, AA, AAA

    @Column(name = "CATEGORY", nullable = false, length = 30)
    private String category; // VISUAL, INTERACTION, UX

    @Column(name = "DESCRIPTION", nullable = false, columnDefinition = "TEXT")
    private String description;
}