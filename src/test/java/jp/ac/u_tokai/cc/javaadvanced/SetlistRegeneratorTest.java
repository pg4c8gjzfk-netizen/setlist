package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.Test;

public class SetlistRegeneratorTest {

    @Test
    public void regenerateKeepsOpeningClosingAndIndexFixedEntries() {
        SetlistEntry opening = entry("Opening", true, FixedPosition.OPENING, -1);
        SetlistEntry indexed = entry("Indexed", true, FixedPosition.INDEX, 2);
        SetlistEntry closing = entry("Closing", true, FixedPosition.CLOSING, -1);
        SetlistProject project = project("1回目", List.of(
                entry("Movable A", false, FixedPosition.NONE, -1),
                opening,
                entry("Movable B", false, FixedPosition.NONE, -1),
                indexed,
                closing));

        SetlistProject regenerated = new SetlistRegenerator(new Random(1)).regenerate(project);
        List<SetlistEntry> entries = regenerated.sessions().get(0).entries();

        assertEquals(opening.id(), entries.get(0).id());
        assertEquals(indexed.id(), entries.get(2).id());
        assertEquals(closing.id(), entries.get(entries.size() - 1).id());
        assertSameEntryIds(project, regenerated);
    }

    @Test
    public void regenerateKeepsEditedPerformersAndDoesNotLoseEntries() {
        SetlistEntry edited = new SetlistEntry(
                UUID.randomUUID(), UUID.randomUUID(), "代理出演の曲", 180,
                List.of("代理出演者"), false, FixedPosition.NONE, -1);
        SetlistProject project = new SetlistProject(List.of(
                new SetlistSession("1回目", List.of(edited, entry("A", false, FixedPosition.NONE, -1))),
                new SetlistSession("2回目", List.of(entry("B", false, FixedPosition.NONE, -1)))));

        SetlistProject regenerated = new SetlistRegenerator(new Random(2)).regenerate(project);

        SetlistEntry regeneratedEdited = regenerated.sessions().stream()
                .flatMap(session -> session.entries().stream())
                .filter(candidate -> candidate.id().equals(edited.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("代理出演者"), regeneratedEdited.performers());
        assertSameEntryIds(project, regenerated);
    }

    @Test
    public void regenerateRejectsConflictingFixedPositionsWithoutChangingProject() {
        SetlistEntry opening = entry("Opening", true, FixedPosition.OPENING, -1);
        SetlistEntry firstIndex = entry("First index", true, FixedPosition.INDEX, 0);
        SetlistProject project = project("1回目", List.of(opening, firstIndex));

        try {
            new SetlistRegenerator(new Random(3)).regenerate(project);
            fail("重複する固定位置はエラーになるべきです。");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("重複"));
        }
        assertEquals(opening.id(), project.sessions().get(0).entries().get(0).id());
        assertEquals(firstIndex.id(), project.sessions().get(0).entries().get(1).id());
    }

    @Test
    public void regenerateRejectsDuplicateEntryIds() {
        UUID duplicateId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        SetlistEntry first = new SetlistEntry(
                duplicateId, sourceId, "A", 100, List.of("出演者A"), false, FixedPosition.NONE, -1);
        SetlistEntry second = new SetlistEntry(
                duplicateId, UUID.randomUUID(), "B", 100, List.of("出演者B"), false, FixedPosition.NONE, -1);
        SetlistProject project = project("1回目", List.of(first, second));

        try {
            new SetlistRegenerator(new Random(4)).regenerate(project);
            fail("重複IDはエラーになるべきです。");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("重複"));
        }
    }

    private SetlistProject project(String name, List<SetlistEntry> entries) {
        return new SetlistProject(List.of(new SetlistSession(name, entries)));
    }

    private SetlistEntry entry(String title, boolean fixed, FixedPosition position, int fixedIndex) {
        return new SetlistEntry(
                UUID.randomUUID(), UUID.randomUUID(), title, 180,
                List.of(title + "の出演者"), fixed, position, fixedIndex);
    }

    private void assertSameEntryIds(SetlistProject expected, SetlistProject actual) {
        List<UUID> expectedIds = expected.sessions().stream()
                .flatMap(session -> session.entries().stream())
                .map(SetlistEntry::id)
                .sorted()
                .toList();
        List<UUID> actualIds = actual.sessions().stream()
                .flatMap(session -> session.entries().stream())
                .map(SetlistEntry::id)
                .sorted()
                .toList();
        assertEquals(expectedIds, actualIds);
    }
}
