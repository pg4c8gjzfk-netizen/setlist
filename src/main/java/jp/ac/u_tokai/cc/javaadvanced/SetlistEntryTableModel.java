package jp.ac.u_tokai.cc.javaadvanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.table.AbstractTableModel;

/** 1公演分の演目を編集する JTable 用モデルです。 */
public final class SetlistEntryTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {
            "順番", "曲名", "時間", "出演者", "固定", "固定位置"
    };

    /** 出演者変更の反映範囲です。 */
    public enum PerformerChangeScope {
        CURRENT_SESSION,
        ALL_SESSIONS,
        CANCEL
    }

    /** 出演者セルが編集されたときに表示する選択肢のための情報です。 */
    public record PerformerEditRequest(SetlistEntry entry, List<String> performers) {
    }

    private final List<SetlistEntry> entries;
    private Function<PerformerEditRequest, PerformerChangeScope> performerScopeSelector;
    private BiConsumer<UUID, List<String>> allSessionsPerformerUpdater;
    private Consumer<String> validationErrorHandler;
    private Runnable changedHandler;

    public SetlistEntryTableModel(List<SetlistEntry> entries) {
        this.entries = new ArrayList<>(Objects.requireNonNull(entries, "entries must not be null"));
        this.performerScopeSelector = request -> PerformerChangeScope.CURRENT_SESSION;
        this.allSessionsPerformerUpdater = (sourceId, performers) -> {
        };
        this.validationErrorHandler = message -> {
        };
        this.changedHandler = () -> {
        };
    }

    public void setPerformerChangeCallbacks(
            Function<PerformerEditRequest, PerformerChangeScope> performerScopeSelector,
            BiConsumer<UUID, List<String>> allSessionsPerformerUpdater) {
        this.performerScopeSelector = Objects.requireNonNull(performerScopeSelector, "performerScopeSelector must not be null");
        this.allSessionsPerformerUpdater = Objects.requireNonNull(
                allSessionsPerformerUpdater, "allSessionsPerformerUpdater must not be null");
    }

    public void setValidationErrorHandler(Consumer<String> validationErrorHandler) {
        this.validationErrorHandler = Objects.requireNonNull(validationErrorHandler, "validationErrorHandler must not be null");
    }

    public void setChangedHandler(Runnable changedHandler) {
        this.changedHandler = Objects.requireNonNull(changedHandler, "changedHandler must not be null");
    }

    public List<SetlistEntry> entries() {
        return List.copyOf(entries);
    }

    @Override
    public int getRowCount() {
        return entries.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> Integer.class;
            case 4 -> Boolean.class;
            case 5 -> FixedPosition.class;
            default -> String.class;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SetlistEntry entry = entries.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> rowIndex + 1;
            case 1 -> entry.title();
            case 2 -> formatDuration(entry.durationSeconds());
            case 3 -> String.join(", ", entry.performers());
            case 4 -> entry.fixed();
            case 5 -> entry.fixedPosition();
            default -> throw new IllegalArgumentException("Unknown column: " + columnIndex);
        };
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        SetlistEntry entry = entries.get(rowIndex);
        try {
            switch (columnIndex) {
                case 1 -> replace(rowIndex, copy(entry, String.valueOf(value), entry.durationSeconds(),
                        entry.performers(), entry.fixed(), entry.fixedPosition(), entry.fixedIndex()));
                case 2 -> replace(rowIndex, copy(entry, entry.title(), parseDuration(String.valueOf(value)),
                        entry.performers(), entry.fixed(), entry.fixedPosition(), entry.fixedIndex()));
                case 3 -> updatePerformers(rowIndex, parsePerformers(String.valueOf(value)));
                case 4 -> updateFixed(rowIndex, Boolean.TRUE.equals(value));
                case 5 -> updateFixedPosition(rowIndex, (FixedPosition) value);
                default -> throw new IllegalArgumentException("編集できない列です。");
            }
        } catch (IllegalArgumentException exception) {
            fireTableCellUpdated(rowIndex, columnIndex);
            validationErrorHandler.accept(exception.getMessage());
        }
    }

    public void addEntry() {
        entries.add(new SetlistEntry(
                UUID.randomUUID(), UUID.randomUUID(), "新しい演目", 0,
                List.of("未設定"), false, FixedPosition.NONE, -1));
        fireTableRowsInserted(entries.size() - 1, entries.size() - 1);
        changedHandler.run();
    }

    public void removeEntry(int rowIndex) {
        entries.remove(rowIndex);
        normalizeIndexFixedEntries();
        fireTableDataChanged();
        changedHandler.run();
    }

    public void moveEntry(int rowIndex, int destinationIndex) {
        if (destinationIndex < 0 || destinationIndex >= entries.size() || rowIndex == destinationIndex) {
            return;
        }
        Collections.swap(entries, rowIndex, destinationIndex);
        normalizeIndexFixedEntries();
        fireTableDataChanged();
        changedHandler.run();
    }

    public void updatePerformersBySourceId(UUID sourceId, List<String> performers) {
        List<String> normalizedPerformers = normalizePerformers(performers);
        boolean updated = false;
        for (int rowIndex = 0; rowIndex < entries.size(); rowIndex++) {
            SetlistEntry entry = entries.get(rowIndex);
            if (entry.sourcePerformanceId().equals(sourceId)) {
                entries.set(rowIndex, copy(entry, entry.title(), entry.durationSeconds(),
                        normalizedPerformers, entry.fixed(), entry.fixedPosition(), entry.fixedIndex()));
                updated = true;
            }
        }
        if (updated) {
            fireTableDataChanged();
            changedHandler.run();
        }
    }

    private void updatePerformers(int rowIndex, List<String> performers) {
        SetlistEntry entry = entries.get(rowIndex);
        PerformerChangeScope scope = performerScopeSelector.apply(new PerformerEditRequest(entry, performers));
        if (scope == PerformerChangeScope.CANCEL) {
            fireTableCellUpdated(rowIndex, 3);
            return;
        }
        replace(rowIndex, copy(entry, entry.title(), entry.durationSeconds(), performers,
                entry.fixed(), entry.fixedPosition(), entry.fixedIndex()));
        if (scope == PerformerChangeScope.ALL_SESSIONS) {
            allSessionsPerformerUpdater.accept(entry.sourcePerformanceId(), performers);
        }
    }

    private void updateFixed(int rowIndex, boolean fixed) {
        SetlistEntry entry = entries.get(rowIndex);
        FixedPosition position = fixed ? entry.fixedPosition() : FixedPosition.NONE;
        int fixedIndex = fixed && position == FixedPosition.INDEX ? rowIndex : -1;
        replace(rowIndex, copy(entry, entry.title(), entry.durationSeconds(), entry.performers(),
                fixed, position, fixedIndex));
    }

    private void updateFixedPosition(int rowIndex, FixedPosition position) {
        SetlistEntry entry = entries.get(rowIndex);
        FixedPosition normalizedPosition = Objects.requireNonNull(position, "fixedPosition must not be null");
        int fixedIndex = normalizedPosition == FixedPosition.INDEX ? rowIndex : -1;
        replace(rowIndex, copy(entry, entry.title(), entry.durationSeconds(), entry.performers(),
                entry.fixed() || normalizedPosition != FixedPosition.NONE, normalizedPosition, fixedIndex));
    }

    private void replace(int rowIndex, SetlistEntry entry) {
        entries.set(rowIndex, entry);
        fireTableRowsUpdated(rowIndex, rowIndex);
        changedHandler.run();
    }

    private void normalizeIndexFixedEntries() {
        for (int rowIndex = 0; rowIndex < entries.size(); rowIndex++) {
            SetlistEntry entry = entries.get(rowIndex);
            if (entry.fixed() && entry.fixedPosition() == FixedPosition.INDEX && entry.fixedIndex() != rowIndex) {
                entries.set(rowIndex, copy(entry, entry.title(), entry.durationSeconds(), entry.performers(),
                        true, FixedPosition.INDEX, rowIndex));
            }
        }
    }

    private SetlistEntry copy(
            SetlistEntry source,
            String title,
            int durationSeconds,
            List<String> performers,
            boolean fixed,
            FixedPosition fixedPosition,
            int fixedIndex) {
        return new SetlistEntry(source.id(), source.sourcePerformanceId(), title, durationSeconds,
                performers, fixed, fixedPosition, fixedIndex);
    }

    private int parseDuration(String value) {
        if (value == null || !value.matches("\\d+:\\d{2}")) {
            throw new IllegalArgumentException("時間は m:ss 形式で入力してください。");
        }
        String[] values = value.split(":", -1);
        int minutes;
        int seconds;
        try {
            minutes = Integer.parseInt(values[0]);
            seconds = Integer.parseInt(values[1]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("時間は m:ss 形式で入力してください。");
        }
        if (seconds < 0 || seconds > 59) {
            throw new IllegalArgumentException("秒は 0〜59 の範囲で入力してください。");
        }
        try {
            return Math.addExact(Math.multiplyExact(minutes, 60), seconds);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("時間が大きすぎます。");
        }
    }

    private List<String> parsePerformers(String value) {
        if (value == null) {
            throw new IllegalArgumentException("出演者名は空欄にできません。");
        }
        return normalizePerformers(List.of(value.split("[,、]", -1)));
    }

    private List<String> normalizePerformers(List<String> performers) {
        List<String> normalized = performers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("出演者名は空欄にできません。");
        }
        return List.copyOf(normalized);
    }

    private String formatDuration(int durationSeconds) {
        return String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60);
    }
}
