package jp.ac.u_tokai.cc.javaadvanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
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
    private static final int FIRST_PERFORMER_COLUMN = 3;

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
    private final List<String> performerNames;
    private Function<PerformerEditRequest, PerformerChangeScope> performerScopeSelector;
    private BiConsumer<UUID, List<String>> allSessionsPerformerUpdater;
    private Consumer<String> validationErrorHandler;
    private Runnable changedHandler;

    /** 演目から演者カラムを導出してモデルを作成します。 */
    public SetlistEntryTableModel(List<SetlistEntry> entries) {
        this(entries, List.of());
    }

    /** 指定された公演内の演者順を維持してモデルを作成します。 */
    public SetlistEntryTableModel(List<SetlistEntry> entries, List<String> performerNames) {
        this.entries = Objects.requireNonNull(entries, "entries must not be null").stream()
                .map(this::normalizeFixedPosition)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        this.performerNames = new ArrayList<>();
        ensurePerformerNames(Objects.requireNonNull(performerNames, "performerNames must not be null"));
        for (SetlistEntry entry : this.entries) {
            ensurePerformerNames(entry.performers());
        }
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
        this.performerScopeSelector = Objects.requireNonNull(
                performerScopeSelector, "performerScopeSelector must not be null");
        this.allSessionsPerformerUpdater = Objects.requireNonNull(
                allSessionsPerformerUpdater, "allSessionsPerformerUpdater must not be null");
    }

    public void setValidationErrorHandler(Consumer<String> validationErrorHandler) {
        this.validationErrorHandler = Objects.requireNonNull(
                validationErrorHandler, "validationErrorHandler must not be null");
    }

    public void setChangedHandler(Runnable changedHandler) {
        this.changedHandler = Objects.requireNonNull(changedHandler, "changedHandler must not be null");
    }

    public List<SetlistEntry> entries() {
        return List.copyOf(entries);
    }

    /** 現在の公演に表示する演者名を、カラム順で返します。 */
    public List<String> performerNames() {
        return List.copyOf(performerNames);
    }

    /** 指定した演者のカラム番号を返します。 */
    public int performerColumnIndex(String performerName) {
        int index = performerNames.indexOf(performerName);
        return index < 0 ? -1 : FIRST_PERFORMER_COLUMN + index;
    }

    /** 固定チェックボックスのカラム番号を返します。 */
    public int fixedColumnIndex() {
        return FIRST_PERFORMER_COLUMN + performerNames.size();
    }

    /** 公演に演者カラムを追加します。 */
    public void addPerformer(String performerName) {
        String normalizedName = normalizePerformerName(performerName);
        if (performerNames.contains(normalizedName)) {
            throw new IllegalArgumentException("演者「" + normalizedName + "」はすでに登録されています。");
        }
        performerNames.add(normalizedName);
        fireTableStructureChanged();
        changedHandler.run();
    }

    /** 演者カラム名と、その演者が出演する全演目の名前を変更します。 */
    public void renamePerformer(String currentName, String newName) {
        String normalizedCurrentName = normalizePerformerName(currentName);
        String normalizedNewName = normalizePerformerName(newName);
        int performerIndex = performerNames.indexOf(normalizedCurrentName);
        if (performerIndex < 0) {
            throw new IllegalArgumentException("演者「" + normalizedCurrentName + "」は登録されていません。");
        }
        if (normalizedCurrentName.equals(normalizedNewName)) {
            return;
        }
        if (performerNames.contains(normalizedNewName)) {
            throw new IllegalArgumentException("演者「" + normalizedNewName + "」はすでに登録されています。");
        }

        performerNames.set(performerIndex, normalizedNewName);
        for (int rowIndex = 0; rowIndex < entries.size(); rowIndex++) {
            SetlistEntry entry = entries.get(rowIndex);
            if (!entry.performers().contains(normalizedCurrentName)) {
                continue;
            }
            List<String> renamedPerformers = entry.performers().stream()
                    .map(name -> normalizedCurrentName.equals(name) ? normalizedNewName : name)
                    .toList();
            entries.set(rowIndex, copy(
                    entry,
                    entry.title(),
                    entry.durationSeconds(),
                    renamedPerformers,
                    entry.fixed(),
                    entry.fixedPosition(),
                    entry.fixedIndex()));
        }
        fireTableStructureChanged();
        changedHandler.run();
    }

    /** 出演中の演目がない演者カラムを削除します。 */
    public void removePerformer(String performerName) {
        String normalizedName = normalizePerformerName(performerName);
        if (!performerNames.contains(normalizedName)) {
            throw new IllegalArgumentException("演者「" + normalizedName + "」は登録されていません。");
        }
        if (entries.stream().anyMatch(entry -> entry.performers().contains(normalizedName))) {
            throw new IllegalArgumentException(
                    "演者「" + normalizedName + "」は出演中です。先にすべての◯を外してください。");
        }
        performerNames.remove(normalizedName);
        fireTableStructureChanged();
        changedHandler.run();
    }

    @Override
    public int getRowCount() {
        return entries.size();
    }

    @Override
    public int getColumnCount() {
        return FIRST_PERFORMER_COLUMN + performerNames.size() + 1;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> "順番";
            case 1 -> "曲名";
            case 2 -> "時間";
            default -> {
                if (isPerformerColumn(column)) {
                    yield performerNames.get(column - FIRST_PERFORMER_COLUMN);
                }
                if (column == fixedColumnIndex()) {
                    yield "固定";
                }
                throw new IllegalArgumentException("Unknown column: " + column);
            }
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Integer.class;
        }
        if (isPerformerColumn(columnIndex) || columnIndex == fixedColumnIndex()) {
            return Boolean.class;
        }
        return String.class;
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
            default -> {
                if (isPerformerColumn(columnIndex)) {
                    String performerName = performerNames.get(columnIndex - FIRST_PERFORMER_COLUMN);
                    yield entry.performers().contains(performerName);
                }
                if (columnIndex == fixedColumnIndex()) {
                    yield entry.fixed();
                }
                throw new IllegalArgumentException("Unknown column: " + columnIndex);
            }
        };
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        SetlistEntry entry = entries.get(rowIndex);
        try {
            if (columnIndex == 1) {
                replace(rowIndex, copy(entry, String.valueOf(value), entry.durationSeconds(),
                        entry.performers(), entry.fixed(), entry.fixedPosition(), entry.fixedIndex()));
            } else if (columnIndex == 2) {
                replace(rowIndex, copy(entry, entry.title(), parseDuration(String.valueOf(value)),
                        entry.performers(), entry.fixed(), entry.fixedPosition(), entry.fixedIndex()));
            } else if (isPerformerColumn(columnIndex)) {
                updatePerformerSelection(rowIndex, columnIndex, Boolean.TRUE.equals(value));
            } else if (columnIndex == fixedColumnIndex()) {
                updateFixed(rowIndex, Boolean.TRUE.equals(value));
            } else {
                throw new IllegalArgumentException("編集できない列です。");
            }
        } catch (IllegalArgumentException exception) {
            fireTableCellUpdated(rowIndex, columnIndex);
            validationErrorHandler.accept(exception.getMessage());
        }
    }

    public void addEntry() {
        entries.add(new SetlistEntry(
                UUID.randomUUID(), UUID.randomUUID(), "新しい演目", 0,
                List.of(Performance.NO_PERFORMER), false, FixedPosition.NONE, -1));
        fireTableRowsInserted(entries.size() - 1, entries.size() - 1);
        changedHandler.run();
    }

    public void removeEntry(int rowIndex) {
        removeEntryAndReturn(rowIndex);
    }

    /**
     * 演目を取り出します。他の公演タブへ移動するときに使用します。
     *
     * @param rowIndex 取り出す行番号
     * @return 取り出した演目
     */
    public SetlistEntry removeEntryAndReturn(int rowIndex) {
        SetlistEntry removedEntry = entries.remove(rowIndex);
        fireTableDataChanged();
        changedHandler.run();
        return removedEntry;
    }

    /**
     * 他の公演タブから移動した演目を末尾へ追加します。
     *
     * @param entry 追加する演目
     */
    public void appendEntry(SetlistEntry entry) {
        SetlistEntry normalizedEntry = normalizeFixedPosition(
                Objects.requireNonNull(entry, "entry must not be null"));
        boolean structureChanged = ensurePerformerNames(normalizedEntry.performers());
        entries.add(normalizedEntry);
        if (structureChanged) {
            fireTableStructureChanged();
        } else {
            fireTableRowsInserted(entries.size() - 1, entries.size() - 1);
        }
        changedHandler.run();
    }

    public void moveEntry(int rowIndex, int destinationIndex) {
        if (destinationIndex < 0 || destinationIndex >= entries.size() || rowIndex == destinationIndex) {
            return;
        }
        Collections.swap(entries, rowIndex, destinationIndex);
        fireTableDataChanged();
        changedHandler.run();
    }

    public void updatePerformersBySourceId(UUID sourceId, List<String> performers) {
        boolean containsSourceEntry = entries.stream()
                .anyMatch(entry -> entry.sourcePerformanceId().equals(sourceId));
        if (!containsSourceEntry) {
            return;
        }
        List<String> normalizedPerformers = normalizePerformers(performers);
        boolean structureChanged = ensurePerformerNames(normalizedPerformers);
        for (int rowIndex = 0; rowIndex < entries.size(); rowIndex++) {
            SetlistEntry entry = entries.get(rowIndex);
            if (entry.sourcePerformanceId().equals(sourceId)) {
                entries.set(rowIndex, copy(entry, entry.title(), entry.durationSeconds(),
                        normalizedPerformers, entry.fixed(), entry.fixedPosition(), entry.fixedIndex()));
            }
        }
        if (structureChanged) {
            fireTableStructureChanged();
        } else {
            fireTableDataChanged();
        }
        changedHandler.run();
    }

    private void updatePerformerSelection(int rowIndex, int columnIndex, boolean selected) {
        SetlistEntry entry = entries.get(rowIndex);
        String performerName = performerNames.get(columnIndex - FIRST_PERFORMER_COLUMN);
        LinkedHashSet<String> selectedPerformers = new LinkedHashSet<>();
        entry.performers().stream()
                .filter(value -> !Performance.NO_PERFORMER.equals(value))
                .forEach(selectedPerformers::add);
        if (selected) {
            selectedPerformers.add(performerName);
        } else {
            selectedPerformers.remove(performerName);
        }

        List<String> orderedPerformers = performerNames.stream()
                .filter(selectedPerformers::contains)
                .toList();
        updatePerformers(rowIndex, normalizePerformers(orderedPerformers), columnIndex);
    }

    private void updatePerformers(int rowIndex, List<String> performers, int editedColumn) {
        SetlistEntry entry = entries.get(rowIndex);
        PerformerChangeScope scope = performerScopeSelector.apply(new PerformerEditRequest(entry, performers));
        if (scope == PerformerChangeScope.CANCEL) {
            fireTableCellUpdated(rowIndex, editedColumn);
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
        replace(rowIndex, copy(entry, entry.title(), entry.durationSeconds(), entry.performers(),
                fixed, FixedPosition.NONE, -1));
    }

    private void replace(int rowIndex, SetlistEntry entry) {
        entries.set(rowIndex, entry);
        fireTableRowsUpdated(rowIndex, rowIndex);
        changedHandler.run();
    }

    private SetlistEntry normalizeFixedPosition(SetlistEntry entry) {
        return copy(entry, entry.title(), entry.durationSeconds(), entry.performers(),
                entry.fixed(), FixedPosition.NONE, -1);
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

    private List<String> normalizePerformers(List<String> performers) {
        Objects.requireNonNull(performers, "performers must not be null");
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        performers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .filter(value -> !Performance.NO_PERFORMER.equals(value))
                .forEach(normalized::add);
        if (normalized.isEmpty()) {
            return List.of(Performance.NO_PERFORMER);
        }
        return List.copyOf(normalized);
    }

    private String normalizePerformerName(String performerName) {
        if (performerName == null || performerName.trim().isEmpty()) {
            throw new IllegalArgumentException("演者名は空欄にできません。");
        }
        String normalizedName = performerName.trim();
        if (Performance.NO_PERFORMER.equals(normalizedName)) {
            throw new IllegalArgumentException("この演者名は内部処理で予約されています。");
        }
        return normalizedName;
    }

    private boolean ensurePerformerNames(List<String> names) {
        boolean changed = false;
        for (String name : names) {
            if (name == null || name.trim().isEmpty() || Performance.NO_PERFORMER.equals(name.trim())) {
                continue;
            }
            String normalizedName = name.trim();
            if (!performerNames.contains(normalizedName)) {
                performerNames.add(normalizedName);
                changed = true;
            }
        }
        return changed;
    }

    private boolean isPerformerColumn(int columnIndex) {
        return columnIndex >= FIRST_PERFORMER_COLUMN && columnIndex < fixedColumnIndex();
    }

    private String formatDuration(int durationSeconds) {
        return String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60);
    }
}
