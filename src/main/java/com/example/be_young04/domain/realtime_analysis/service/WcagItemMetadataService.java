package com.example.be_young04.domain.realtime_analysis.service;

import com.example.be_young04.domain.wcag.entity.WcagItem;
import com.example.be_young04.domain.wcag.repository.WcagItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WcagItemMetadataService {

    private final WcagItemRepository wcagItemRepository;

    @Cacheable("wcagRules")
    public Map<Long, WcagItem> getCachedWcagItems() {
        return wcagItemRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        WcagItem::getWcagItemId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }
}
