package com.example.be_young04.domain.realtime_analysis.service;

import com.example.be_young04.domain.wcag.entity.WcagItem;
import com.example.be_young04.domain.wcag.repository.WcagItemRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WcagItemMetadataServiceTest {

    @Test
    void getCachedWcagItemsIndexesItemsByPrimaryKey() {
        WcagItemRepository repository = mock(WcagItemRepository.class);
        WcagItem first = wcagItem(1L, "1.1.1");
        WcagItem second = wcagItem(2L, "1.1.1");
        when(repository.findAll()).thenReturn(List.of(first, second));

        Map<Long, WcagItem> result = new WcagItemMetadataService(repository).getCachedWcagItems();

        assertThat(result).containsEntry(1L, first).containsEntry(2L, second);
    }

    private WcagItem wcagItem(Long id, String sc) {
        return WcagItem.builder()
                .wcagItemId(id)
                .sc(sc)
                .title("title")
                .levelType("A")
                .category("VISUAL")
                .description("description")
                .build();
    }
}
