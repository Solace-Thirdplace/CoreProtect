package net.coreprotect.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class ItemNameMatcherTest {

    // How Paper renders a per-letter gradient name such as the Nexo Gravedigger: hex colour then bold for each letter.
    private static final String GRADIENT_NAME = gradient("Gravedigger");

    private static String gradient(String text) {
        StringBuilder builder = new StringBuilder();
        for (char letter : text.toCharArray()) {
            builder.append("§x§f§f§e§0§a§3§l").append(letter);
        }
        return builder.toString();
    }

    @Test
    void gradientNameMatchesPlainText() {
        assertEquals("gravedigger", ItemNameMatcher.normalize(GRADIENT_NAME));
        assertTrue(ItemNameMatcher.of(List.of("Gravedigger")).matches(List.of(GRADIENT_NAME)));
    }

    @Test
    void matchesAnywhereIgnoringCase() {
        ItemNameMatcher matcher = ItemNameMatcher.of(List.of("DIGGER"));

        assertTrue(matcher.matches(List.of(GRADIENT_NAME)));
        assertFalse(matcher.matches(List.of("§6Soulreaper")));
    }

    @Test
    void anyIncludedTextIsEnough() {
        ItemNameMatcher matcher = ItemNameMatcher.of(List.of("reaper", "digger"));

        assertTrue(matcher.matches(List.of("Soulreaper")));
        assertTrue(matcher.matches(List.of(GRADIENT_NAME)));
        assertFalse(matcher.matches(List.of("Ghoulbane")));
    }

    @Test
    void exclusionsRemoveMatches() {
        ItemNameMatcher matcher = ItemNameMatcher.of(List.of("digger", "-fork"));

        assertTrue(matcher.matches(List.of(GRADIENT_NAME)));
        assertFalse(matcher.matches(List.of("Gravedigger Fork")));
    }

    @Test
    void exclusionsAloneKeepUnnamedItems() {
        ItemNameMatcher matcher = ItemNameMatcher.of(List.of("-Gravedigger"));

        assertTrue(matcher.matches(Collections.emptyList()));
        assertTrue(matcher.matches(List.of("Soulreaper")));
        assertFalse(matcher.matches(List.of(GRADIENT_NAME)));
    }

    @Test
    void unnamedItemsNeverMatchAnInclude() {
        ItemNameMatcher matcher = ItemNameMatcher.of(List.of("Gravedigger"));

        assertFalse(matcher.matches(Collections.emptyList()));
        assertFalse(matcher.matches(null));
    }

    @Test
    void eitherNameCanMatch() {
        // A renamed item keeps its item_name, so a search for either finds it.
        ItemNameMatcher matcher = ItemNameMatcher.of(List.of("Gravedigger"));

        assertTrue(matcher.matches(List.of("§oMy Shovel", GRADIENT_NAME)));
    }

    @Test
    void emptyFiltersAreIgnored() {
        assertTrue(ItemNameMatcher.of(List.of("-", "")).isEmpty());
        assertTrue(ItemNameMatcher.of(null).isEmpty());
    }
}
