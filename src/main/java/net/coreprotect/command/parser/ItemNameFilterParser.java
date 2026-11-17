package net.coreprotect.command.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Parses n:/name: item name filters out of lookup arguments.
 *
 * The value runs until the next lookup parameter, so names with spaces work
 * (n:Grave digger t:30d). Commas separate alternatives and a leading "-"
 * excludes. The n: arguments are removed from the returned arguments, so the
 * other parsers never see them.
 */
public final class ItemNameFilterParser {
    public static final int MINIMUM_FILTER_CODE_POINTS = 3;

    private ItemNameFilterParser() {
        throw new IllegalStateException("Parser class");
    }

    public static ParseResult parse(String[] inputArguments) {
        if (inputArguments == null || inputArguments.length == 0) {
            return new ParseResult(new String[0], Collections.emptyList(), false);
        }

        List<String> arguments = new ArrayList<>(inputArguments.length);
        Set<String> filters = new LinkedHashSet<>();
        boolean specified = false;

        for (int index = 0; index < inputArguments.length; index++) {
            String argument = Objects.toString(inputArguments[index], "").trim();
            if (!isNameParameter(argument)) {
                arguments.add(inputArguments[index]);
                continue;
            }

            specified = true;
            StringBuilder collected = new StringBuilder(value(argument));
            while (index + 1 < inputArguments.length && !MessageFilterParser.isLookupTerminator(inputArguments[index + 1])) {
                String next = Objects.toString(inputArguments[++index], "").trim();
                if (!next.isEmpty()) {
                    collected.append(' ').append(next);
                }
            }

            addFilters(filters, collected.toString());
        }

        return new ParseResult(arguments.toArray(new String[0]), new ArrayList<>(filters), specified);
    }

    /**
     * The first n:/name: argument at or after firstIndex, or null. Used by commands that must refuse it.
     */
    public static String findNameParameter(String[] arguments, int firstIndex) {
        if (arguments == null) {
            return null;
        }
        for (int index = Math.max(firstIndex, 0); index < arguments.length; index++) {
            String argument = Objects.toString(arguments[index], "").trim();
            if (isNameParameter(argument)) {
                return argument;
            }
        }
        return null;
    }

    static boolean isNameParameter(String raw) {
        String normalized = Objects.toString(raw, "").trim().toLowerCase(Locale.ROOT).replace("\\", "").replace("'", "");
        return normalized.startsWith("n:") || normalized.startsWith("name:");
    }

    private static String value(String argument) {
        int separator = argument.indexOf(':');
        return separator < 0 ? "" : argument.substring(separator + 1);
    }

    private static void addFilters(Set<String> filters, String value) {
        for (String part : value.split(",", -1)) {
            String filter = part.trim();
            if (!filter.isEmpty()) {
                filters.add(filter);
            }
        }
    }

    public static final class ParseResult {
        private final String[] arguments;
        private final List<String> filters;
        private final boolean specified;

        private ParseResult(String[] arguments, List<String> filters, boolean specified) {
            this.arguments = arguments;
            this.filters = Collections.unmodifiableList(filters);
            this.specified = specified;
        }

        public String[] getArguments() {
            return arguments.clone();
        }

        public List<String> getFilters() {
            return filters;
        }

        public boolean isSpecified() {
            return specified;
        }

        public boolean hasInvalidLength() {
            if (filters.isEmpty()) {
                return true;
            }
            for (String filter : filters) {
                int start = filter.startsWith("-") ? 1 : 0;
                if (filter.codePointCount(start, filter.length()) < MINIMUM_FILTER_CODE_POINTS) {
                    return true;
                }
            }
            return false;
        }
    }
}
