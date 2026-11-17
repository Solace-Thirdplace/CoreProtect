package net.coreprotect.utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Matches item names against n: filters.
 *
 * Names come from ItemMeta as legacy text, where a gradient name is one colour
 * code per letter. Colour and format codes are stripped before comparing, so
 * "Gravedigger" matches however it was coloured. Matching is case-insensitive
 * and looks for the text anywhere in the name. An item matches when it
 * contains any included text (or there are no includes) and no excluded text.
 */
public final class ItemNameMatcher {
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9A-FK-ORX]");

    private final List<String> included;
    private final List<String> excluded;

    private ItemNameMatcher(List<String> included, List<String> excluded) {
        this.included = included;
        this.excluded = excluded;
    }

    public static ItemNameMatcher of(Collection<String> filters) {
        List<String> included = new ArrayList<>();
        List<String> excluded = new ArrayList<>();
        if (filters != null) {
            for (String filter : filters) {
                if (filter == null) {
                    continue;
                }
                boolean exclude = filter.startsWith("-");
                String text = normalize(exclude ? filter.substring(1) : filter);
                if (text.isEmpty()) {
                    continue;
                }
                (exclude ? excluded : included).add(text);
            }
        }
        return new ItemNameMatcher(Collections.unmodifiableList(included), Collections.unmodifiableList(excluded));
    }

    public boolean isEmpty() {
        return included.isEmpty() && excluded.isEmpty();
    }

    /**
     * @param names
     *            every name the item carries (custom name, item_name); empty for an item with no name
     */
    public boolean matches(Collection<String> names) {
        List<String> plainNames = new ArrayList<>();
        if (names != null) {
            for (String name : names) {
                String plain = normalize(name);
                if (!plain.isEmpty()) {
                    plainNames.add(plain);
                }
            }
        }

        for (String text : excluded) {
            for (String name : plainNames) {
                if (name.contains(text)) {
                    return false;
                }
            }
        }

        if (included.isEmpty()) {
            return true;
        }
        for (String text : included) {
            for (String name : plainNames) {
                if (name.contains(text)) {
                    return true;
                }
            }
        }
        return false;
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return FORMATTING.matcher(text).replaceAll("").trim().toLowerCase(Locale.ROOT);
    }
}
