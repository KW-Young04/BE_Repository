package com.example.be_young04.domain.repository.service;

import com.example.be_young04.domain.repository.exception.InvalidGithubUrlException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GithubUrlParserTest {

    private final GithubUrlParser parser = new GithubUrlParser();

    @Test
    void parseRemovesGitSuffixAndTrailingSlash() {
        var info = parser.parse("https://github.com/mdn/beginner-html-site-styled.git/");

        assertEquals("mdn", info.getOwner());
        assertEquals("beginner-html-site-styled", info.getRepo());
    }

    @Test
    void parseRejectsInvalidGithubUrl() {
        assertThrows(InvalidGithubUrlException.class, () -> parser.parse("https://example.com/foo/bar"));
        assertThrows(InvalidGithubUrlException.class, () -> parser.parse("https://github.com/foo/bar/issues"));
        assertThrows(InvalidGithubUrlException.class, () -> parser.parse("https://github.com/foo/../bar"));
    }
}
