package net.coreprotect.command.lookup;

import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import net.coreprotect.config.ConfigHandler;
import net.coreprotect.database.Lookup;
import net.coreprotect.model.action.EntityActionFilter;
import net.coreprotect.model.action.LookupActions;
import net.coreprotect.model.lookup.EntityLookupContext;
import net.coreprotect.model.lookup.LookupRollbackState;
import net.coreprotect.utility.ItemNameMatcher;
import net.coreprotect.utility.ItemUtils;

/**
 * Runs a lookup with an n: item name filter.
 *
 * Item names live inside the serialized metadata blob, so SQL can't match
 * them. This runs the normal lookup query with every other filter, reads the
 * rows newest first in chunks, deserializes each item's name and keeps the
 * rows that match. It stops after scanLimit rows so an unfiltered lookup
 * can't read a whole table.
 */
final class ItemNameLookup {
    private static final int CHUNK_SIZE = 20000;

    private ItemNameLookup() {
        throw new IllegalStateException("Lookup class");
    }

    static Result scan(Statement statement, CommandSender player, List<String> uuidList, List<String> userList, List<Object> blockList, Map<Object, Boolean> excludedBlocks, List<String> excludedUsers, List<Integer> actions, EntityActionFilter entityActionFilter, List<String> messageFilters, EntityLookupContext entityContext, Location location, Integer[] radius, long timeStart, long timeEnd, boolean restrictWorld, Integer entityContainerId, LookupRollbackState rollbackState, ItemNameMatcher matcher, int scanLimit) {
        // Freeze the upper time bound so rows logged during the scan don't shift the chunk offsets.
        long upperTime = timeEnd > 0 ? timeEnd : System.currentTimeMillis() / 1000L;
        boolean columnar = ConfigHandler.databaseType.isColumnar();
        long knownRows = -1L;
        if (columnar && ConfigHandler.databaseType.isClickHouse()) {
            knownRows = Lookup.countLookupRows(statement, player, uuidList, userList, blockList, excludedBlocks, excludedUsers, actions, entityActionFilter, messageFilters, entityContext, location, radius, newRowData(), timeStart, upperTime, restrictWorld, true, entityContainerId, rollbackState);
            if (knownRows <= 0L) {
                return new Result(Collections.emptyList(), 0, false);
            }
        }

        List<String[]> matched = new ArrayList<>();
        int scanned = 0;
        while (true) {
            int chunkSize = Math.min(CHUNK_SIZE, scanLimit - scanned + 1);
            List<String[]> chunk;
            if (columnar) {
                chunk = Lookup.performLookupPage(statement, player, uuidList, userList, blockList, excludedBlocks, excludedUsers, actions, entityActionFilter, messageFilters, entityContext, location, radius, newRowData(), timeStart, upperTime, scanned, chunkSize, knownRows, null, restrictWorld, true, entityContainerId, rollbackState).getRows();
            }
            else {
                chunk = Lookup.performPartialLookup(statement, player, uuidList, userList, blockList, excludedBlocks, excludedUsers, actions, entityActionFilter, messageFilters, entityContext, location, radius, newRowData(), timeStart, upperTime, scanned, chunkSize, restrictWorld, true, entityContainerId, rollbackState);
            }
            if (chunk == null || chunk.isEmpty()) {
                break;
            }

            for (String[] row : chunk) {
                if (scanned == scanLimit) {
                    return new Result(matched, scanned, true);
                }
                scanned++;
                if (matcher.matches(names(row, actions))) {
                    matched.add(row);
                }
            }

            if (chunk.size() < chunkSize) {
                break;
            }
        }

        return new Result(matched, scanned, false);
    }

    private static List<String> names(String[] row, List<Integer> actions) {
        int amount = parseInt(row.length > 10 ? row[10] : null, -1);
        boolean itemRow = actions.contains(LookupActions.CONTAINER) || actions.contains(LookupActions.ITEM) || actions.contains(5) || amount > -1;
        if (!itemRow || row.length <= 11 || row[11] == null) {
            return Collections.emptyList();
        }
        int type = parseInt(row[5], 0);
        byte[] metadata = row[11].getBytes(StandardCharsets.ISO_8859_1);
        return ItemUtils.getItemNames(metadata, type, Math.max(amount, 1));
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Long[] newRowData() {
        return new Long[] { 0L, 0L, 0L, 0L, 0L };
    }

    static final class Result {
        private final List<String[]> rows;
        private final int scanned;
        private final boolean limitReached;

        private Result(List<String[]> rows, int scanned, boolean limitReached) {
            this.rows = rows;
            this.scanned = scanned;
            this.limitReached = limitReached;
        }

        List<String[]> getRows() {
            return rows;
        }

        int getScanned() {
            return scanned;
        }

        boolean isLimitReached() {
            return limitReached;
        }
    }
}
