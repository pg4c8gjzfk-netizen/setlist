package jp.ac.u_tokai.cc.javaadvanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 編集済みの香盤表から、固定されていない演目だけを再配置するサービスです。
 *
 * <p>このクラスは入力プロジェクトを書き換えません。固定条件に矛盾がある場合は
 * {@link IllegalArgumentException} を送出するため、呼び出し側は編集画面の状態を
 * そのまま維持できます。</p>
 */
public final class SetlistRegenerator {

    private static final int ORDER_SEARCH_ATTEMPTS = 1_000;

    private final Random random;

    /**
     * 通常の乱数を使用する再生成器を作成します。
     */
    public SetlistRegenerator() {
        this(new Random());
    }

    /**
     * テスト時に並び順を再現できる再生成器を作成します。
     *
     * @param random 並び順の探索に使用する乱数
     */
    public SetlistRegenerator(Random random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /**
     * 固定演目を保持し、未固定の演目だけを再配置します。
     *
     * @param currentProject 編集画面の現在の状態
     * @return 再配置後の新しいプロジェクト
     * @throws IllegalArgumentException 固定条件または演目IDが矛盾している場合
     */
    public SetlistProject regenerate(SetlistProject currentProject) {
        Objects.requireNonNull(currentProject, "currentProject must not be null");
        validate(currentProject);

        List<SessionLayout> layouts = new ArrayList<>();
        List<SetlistEntry> movableEntries = new ArrayList<>();
        for (SetlistSession session : currentProject.sessions()) {
            layouts.add(createLayout(session, movableEntries));
        }

        int freeSlotCount = layouts.stream().mapToInt(SessionLayout::freeSlotCount).sum();
        if (freeSlotCount != movableEntries.size()) {
            throw new IllegalArgumentException("未固定演目の数と再配置先の数が一致しません。");
        }

        List<SetlistEntry> bestOrder = findBestMovableOrder(layouts, movableEntries);
        List<SetlistSession> regeneratedSessions = placeMovableEntries(layouts, bestOrder);
        SetlistProject regeneratedProject = new SetlistProject(regeneratedSessions);
        verifyEntriesPreserved(currentProject, regeneratedProject);
        return regeneratedProject;
    }

    /**
     * 現在の編集状態が再生成可能かを検証します。
     *
     * @param currentProject 検証対象のプロジェクト
     * @throws IllegalArgumentException 固定条件または演目IDが矛盾している場合
     */
    public void validate(SetlistProject currentProject) {
        Objects.requireNonNull(currentProject, "currentProject must not be null");
        validateUniqueEntryIds(currentProject);
        for (SetlistSession session : currentProject.sessions()) {
            createLayout(session, new ArrayList<>());
        }
    }

    private SessionLayout createLayout(SetlistSession session, List<SetlistEntry> movableEntries) {
        List<SetlistEntry> originalEntries = session.entries();
        List<SetlistEntry> slots = new ArrayList<>(Collections.nCopies(originalEntries.size(), null));
        int openingCount = 0;
        int closingCount = 0;

        for (int entryIndex = 0; entryIndex < originalEntries.size(); entryIndex++) {
            SetlistEntry entry = originalEntries.get(entryIndex);
            if (!entry.fixed()) {
                if (entry.fixedPosition() != FixedPosition.NONE) {
                    throw new IllegalArgumentException(
                            session.name() + " の「" + entry.title() + "」は固定位置が指定されていますが、固定されていません。");
                }
                movableEntries.add(entry);
                continue;
            }

            int targetIndex = determineFixedIndex(entry, entryIndex, originalEntries.size(), session.name());
            if (entry.fixedPosition() == FixedPosition.OPENING && ++openingCount > 1) {
                throw new IllegalArgumentException(session.name() + " ではオープニング固定は1件までです。");
            }
            if (entry.fixedPosition() == FixedPosition.CLOSING && ++closingCount > 1) {
                throw new IllegalArgumentException(session.name() + " ではトリ固定は1件までです。");
            }
            if (slots.get(targetIndex) != null) {
                throw new IllegalArgumentException(
                        session.name() + " の固定位置 " + (targetIndex + 1) + " が重複しています。");
            }
            slots.set(targetIndex, entry);
        }

        return new SessionLayout(session.name(), slots);
    }

    private int determineFixedIndex(
            SetlistEntry entry, int currentIndex, int entryCount, String sessionName) {
        return switch (entry.fixedPosition()) {
            case NONE -> currentIndex;
            case OPENING -> 0;
            case CLOSING -> entryCount - 1;
            case INDEX -> {
                if (entry.fixedIndex() >= entryCount) {
                    throw new IllegalArgumentException(
                            sessionName + " の「" + entry.title() + "」の固定位置が演目数を超えています。");
                }
                yield entry.fixedIndex();
            }
        };
    }

    private List<SetlistEntry> findBestMovableOrder(
            List<SessionLayout> layouts, List<SetlistEntry> movableEntries) {
        if (movableEntries.size() <= 1) {
            return List.copyOf(movableEntries);
        }

        List<SetlistEntry> bestOrder = new ArrayList<>(movableEntries);
        int bestViolations = Integer.MAX_VALUE;
        for (int attempt = 0; attempt < ORDER_SEARCH_ATTEMPTS; attempt++) {
            List<SetlistEntry> candidate = new ArrayList<>(movableEntries);
            Collections.shuffle(candidate, random);
            int violations = countConsecutivePerformerViolations(layouts, candidate);
            if (violations < bestViolations) {
                bestOrder = candidate;
                bestViolations = violations;
                if (violations == 0) {
                    break;
                }
            }
        }
        return bestOrder;
    }

    private int countConsecutivePerformerViolations(
            List<SessionLayout> layouts, List<SetlistEntry> movableEntries) {
        List<SetlistSession> sessions = placeMovableEntries(layouts, movableEntries);
        int violations = 0;
        for (SetlistSession session : sessions) {
            for (int i = 0; i < session.entries().size() - 1; i++) {
                if (sharesPerformer(session.entries().get(i), session.entries().get(i + 1))) {
                    violations++;
                }
            }
        }
        return violations;
    }

    private List<SetlistSession> placeMovableEntries(
            List<SessionLayout> layouts, List<SetlistEntry> movableEntries) {
        int movableIndex = 0;
        List<SetlistSession> sessions = new ArrayList<>();
        for (SessionLayout layout : layouts) {
            List<SetlistEntry> entries = new ArrayList<>(layout.slots());
            for (int index = 0; index < entries.size(); index++) {
                if (entries.get(index) == null) {
                    if (movableIndex >= movableEntries.size()) {
                        throw new IllegalArgumentException("未固定演目をすべて配置できません。");
                    }
                    entries.set(index, movableEntries.get(movableIndex++));
                }
            }
            sessions.add(new SetlistSession(layout.name(), entries));
        }
        if (movableIndex != movableEntries.size()) {
            throw new IllegalArgumentException("未配置の演目があります。");
        }
        return sessions;
    }

    private void validateUniqueEntryIds(SetlistProject project) {
        Set<UUID> ids = new HashSet<>();
        for (SetlistSession session : project.sessions()) {
            for (SetlistEntry entry : session.entries()) {
                if (!ids.add(entry.id())) {
                    throw new IllegalArgumentException("演目IDが重複しています: " + entry.id());
                }
            }
        }
    }

    private void verifyEntriesPreserved(SetlistProject before, SetlistProject after) {
        List<UUID> beforeIds = entryIds(before);
        List<UUID> afterIds = entryIds(after);
        if (beforeIds.size() != afterIds.size() || !new HashSet<>(beforeIds).equals(new HashSet<>(afterIds))) {
            throw new IllegalArgumentException("再生成後の演目に欠落または重複があります。");
        }
    }

    private List<UUID> entryIds(SetlistProject project) {
        return project.sessions().stream()
                .flatMap(session -> session.entries().stream())
                .map(SetlistEntry::id)
                .toList();
    }

    private boolean sharesPerformer(SetlistEntry first, SetlistEntry second) {
        for (String performer : first.performers()) {
            if (Performance.NO_PERFORMER.equals(performer)) {
                continue;
            }
            if (second.performers().contains(performer)) {
                return true;
            }
        }
        return false;
    }

    private record SessionLayout(String name, List<SetlistEntry> slots) {

        private SessionLayout {
            // 空きスロットを null で表現するため、null を許可しない List.copyOf は使わない。
            slots = Collections.unmodifiableList(new ArrayList<>(slots));
        }

        private int freeSlotCount() {
            return (int) slots.stream().filter(Objects::isNull).count();
        }
    }
}
