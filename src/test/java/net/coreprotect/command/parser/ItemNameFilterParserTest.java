package net.coreprotect.command.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ItemNameFilterParserTest {

    @Test
    void removesNameArgumentAndKeepsTheRest() {
        ItemNameFilterParser.ParseResult result = ItemNameFilterParser.parse(new String[] { "lookup", "u:LilPuppyyy", "n:Gravedigger", "t:30d" });

        assertTrue(result.isSpecified());
        assertEquals(List.of("Gravedigger"), result.getFilters());
        assertArrayEquals(new String[] { "lookup", "u:LilPuppyyy", "t:30d" }, result.getArguments());
    }

    @Test
    void collectsWordsUntilTheNextParameter() {
        ItemNameFilterParser.ParseResult result = ItemNameFilterParser.parse(new String[] { "l", "name:Grave", "digger", "a:container" });

        assertEquals(List.of("Grave digger"), result.getFilters());
        assertArrayEquals(new String[] { "l", "a:container" }, result.getArguments());
    }

    @Test
    void stopsAtControlArguments() {
        ItemNameFilterParser.ParseResult result = ItemNameFilterParser.parse(new String[] { "l", "n:Gravedigger", "#count" });

        assertEquals(List.of("Gravedigger"), result.getFilters());
        assertArrayEquals(new String[] { "l", "#count" }, result.getArguments());
    }

    @Test
    void splitsCommasAndKeepsExclusions() {
        ItemNameFilterParser.ParseResult result = ItemNameFilterParser.parse(new String[] { "l", "n:digger,-Fork" });

        assertEquals(List.of("digger", "-Fork"), result.getFilters());
        assertFalse(result.hasInvalidLength());
    }

    @Test
    void shortFiltersAreInvalid() {
        assertTrue(ItemNameFilterParser.parse(new String[] { "l", "n:ab" }).hasInvalidLength());
        assertTrue(ItemNameFilterParser.parse(new String[] { "l", "n:-ab" }).hasInvalidLength());
        assertTrue(ItemNameFilterParser.parse(new String[] { "l", "n:" }).hasInvalidLength());
    }

    @Test
    void noisyFlagIsNotANameFilter() {
        ItemNameFilterParser.ParseResult result = ItemNameFilterParser.parse(new String[] { "l", "u:Notch", "n" });

        assertFalse(result.isSpecified());
        assertArrayEquals(new String[] { "l", "u:Notch", "n" }, result.getArguments());
    }

    @Test
    void messageFilterStopsAtNameFilter() {
        MessageFilterParser.ParseResult result = MessageFilterParser.parse(new String[] { "l", "a:chat", "f:hello", "there", "n:Gravedigger" });

        assertEquals(List.of("hello there"), result.getFilters());
    }

    @Test
    void findsNameParameterForCommandsThatRefuseIt() {
        assertEquals("n:Gravedigger", ItemNameFilterParser.findNameParameter(new String[] { "rollback", "u:Notch", "n:Gravedigger" }, 1));
        assertEquals("NAME:x", ItemNameFilterParser.findNameParameter(new String[] { "purge", "NAME:x" }, 1));
        assertNull(ItemNameFilterParser.findNameParameter(new String[] { "rollback", "u:Notch", "t:1h", "n" }, 1));
        assertNull(ItemNameFilterParser.findNameParameter(new String[] { "n:subcommand-name" }, 1));
    }
}
