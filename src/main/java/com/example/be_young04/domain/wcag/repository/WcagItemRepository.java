package com.example.be_young04.domain.wcag.repository;

import com.example.be_young04.domain.wcag.entity.WcagItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WcagItemRepository extends JpaRepository<WcagItem, Long> {

    List<WcagItem> findBySc(String sc);
}