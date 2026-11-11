package com.example.be_young04.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SqlInitializationResourcesTest {

    @Test
    void wcagSeedUsesUpsertWithoutDeletingReferencedRows() throws IOException {
        String sql = readClasspathResource("wcag.sql");

        assertThat(sql)
                .doesNotContainIgnoringCase("DELETE FROM `WCAG_ITEMS`")
                .containsIgnoringCase("ON DUPLICATE KEY UPDATE");
    }

    @Test
    void schemaCreatesNamedForeignKeysOnlyWhenTablesAreCreated() throws IOException {
        String sql = readClasspathResource("schema.sql");

        assertThat(sql)
                .doesNotContainIgnoringCase("ALTER TABLE")
                .contains("CONSTRAINT `FK_ANALYSIS_WCAG_RESULTS_WCAG_ITEM`");
    }

    private String readClasspathResource(String name) throws IOException {
        return new ClassPathResource(name).getContentAsString(StandardCharsets.UTF_8);
    }
}
