package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class SetlistEntryTableModelTest {

    @Test
    public void performerMarkCanRequestAllSessionsUpdate() {
        UUID sourceId = UUID.randomUUID();
        SetlistEntryTableModel model = new SetlistEntryTableModel(
                List.of(entry(sourceId)), List.of("出演者A", "出演者B"));
        AtomicReference<UUID> updatedSourceId = new AtomicReference<>();
        AtomicReference<List<String>> updatedPerformers = new AtomicReference<>();
        model.setPerformerChangeCallbacks(
                request -> SetlistEntryTableModel.PerformerChangeScope.ALL_SESSIONS,
                (id, performers) -> {
                    updatedSourceId.set(id);
                    updatedPerformers.set(performers);
                });

        model.setValueAt(Boolean.TRUE, 0, model.performerColumnIndex("出演者B"));

        assertEquals(List.of("出演者A", "出演者B"), model.entries().get(0).performers());
        assertEquals(sourceId, updatedSourceId.get());
        assertEquals(List.of("出演者A", "出演者B"), updatedPerformers.get());
    }

    @Test
    public void performerMarkDefaultsToTheCurrentSessionOnly() {
        UUID sourceId = UUID.randomUUID();
        List<String> roster = List.of("出演者A", "代理出演者");
        SetlistEntryTableModel firstSession = new SetlistEntryTableModel(List.of(entry(sourceId)), roster);
        SetlistEntryTableModel secondSession = new SetlistEntryTableModel(List.of(entry(sourceId)), roster);

        firstSession.setValueAt(Boolean.TRUE, 0, firstSession.performerColumnIndex("代理出演者"));

        assertEquals(List.of("出演者A", "代理出演者"), firstSession.entries().get(0).performers());
        assertEquals(List.of("出演者A"), secondSession.entries().get(0).performers());
    }

    @Test
    public void allSessionsUpdateDoesNotAddPerformersToAnUnrelatedSession() {
        SetlistEntryTableModel unrelatedSession = new SetlistEntryTableModel(
                List.of(entry(UUID.randomUUID())), List.of("出演者A"));

        unrelatedSession.updatePerformersBySourceId(
                UUID.randomUUID(), List.of("出演者A", "別公演の出演者"));

        assertEquals(List.of("出演者A"), unrelatedSession.performerNames());
        assertEquals(List.of("出演者A"), unrelatedSession.entries().getFirst().performers());
    }

    @Test
    public void removingTheLastMarkStoresNoPerformerWithoutShowingAMark() {
        SetlistEntryTableModel model = new SetlistEntryTableModel(
                List.of(entry(UUID.randomUUID())), List.of("出演者A"));

        model.setValueAt(Boolean.FALSE, 0, model.performerColumnIndex("出演者A"));

        assertEquals(List.of(Performance.NO_PERFORMER), model.entries().get(0).performers());
        assertEquals(Boolean.FALSE, model.getValueAt(0, model.performerColumnIndex("出演者A")));
    }

    @Test
    public void performerColumnsCanBeAddedButUsedColumnsCannotBeRemoved() {
        SetlistEntryTableModel model = new SetlistEntryTableModel(
                List.of(entry(UUID.randomUUID())), List.of("出演者A"));

        model.addPerformer("出演者B");

        assertEquals(List.of("出演者A", "出演者B"), model.performerNames());
        assertEquals(Boolean.FALSE, model.getValueAt(0, model.performerColumnIndex("出演者B")));
        try {
            model.removePerformer("出演者A");
            fail("出演中の演者カラムを削除できてしまいました。");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("出演中"));
        }

        model.removePerformer("出演者B");
        assertEquals(List.of("出演者A"), model.performerNames());
    }

    @Test
    public void renamingPerformerUpdatesRosterAndEveryParticipatingEntry() {
        SetlistEntry participating = entry(UUID.randomUUID());
        SetlistEntry notParticipating = new SetlistEntry(
                UUID.randomUUID(), UUID.randomUUID(), "別の演目", 120,
                List.of("出演者B"), false, FixedPosition.NONE, -1);
        SetlistEntryTableModel model = new SetlistEntryTableModel(
                List.of(participating, notParticipating), List.of("出演者A", "出演者B"));

        model.renamePerformer("出演者A", "出演者A 改名後");

        assertEquals(List.of("出演者A 改名後", "出演者B"), model.performerNames());
        assertEquals(List.of("出演者A 改名後"), model.entries().get(0).performers());
        assertEquals(List.of("出演者B"), model.entries().get(1).performers());
        assertEquals(-1, model.performerColumnIndex("出演者A"));
        assertTrue(model.performerColumnIndex("出演者A 改名後") >= 3);
    }

    @Test
    public void renamingPerformerRejectsDuplicateNameWithoutChangingEntries() {
        SetlistEntryTableModel model = new SetlistEntryTableModel(
                List.of(entry(UUID.randomUUID())), List.of("出演者A", "出演者B"));

        try {
            model.renamePerformer("出演者A", "出演者B");
            fail("既存の演者名へ変更できてしまいました。");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("すでに登録"));
        }

        assertEquals(List.of("出演者A", "出演者B"), model.performerNames());
        assertEquals(List.of("出演者A"), model.entries().getFirst().performers());
    }

    @Test
    public void invalidDurationDoesNotOverwriteExistingValue() {
        SetlistEntryTableModel model = new SetlistEntryTableModel(List.of(entry(UUID.randomUUID())));
        AtomicReference<String> error = new AtomicReference<>();
        model.setValidationErrorHandler(error::set);

        model.setValueAt("3:60", 0, 2);

        assertEquals(180, model.entries().get(0).durationSeconds());
        assertTrue(error.get().contains("0〜59"));
    }

    @Test
    public void fixedCheckboxKeepsFixedEntryWhenItIsMoved() {
        SetlistEntry first = entry(UUID.randomUUID());
        SetlistEntry second = entry(UUID.randomUUID());
        SetlistEntryTableModel model = new SetlistEntryTableModel(List.of(first, second));

        model.setValueAt(Boolean.TRUE, 0, model.fixedColumnIndex());
        model.moveEntry(0, 1);

        SetlistEntry moved = model.entries().get(1);
        assertEquals(first.id(), moved.id());
        assertTrue(moved.fixed());
        assertEquals(FixedPosition.NONE, moved.fixedPosition());
        assertEquals(-1, moved.fixedIndex());
    }

    @Test
    public void editorModelNormalizesLegacyFixedPositionsToCheckboxBasedFixedEntries() {
        SetlistEntry legacyOpening = new SetlistEntry(
                UUID.randomUUID(), UUID.randomUUID(), "旧オープニング", 180,
                List.of("出演者A"), true, FixedPosition.OPENING, -1);

        SetlistEntry normalized = new SetlistEntryTableModel(List.of(legacyOpening)).entries().get(0);

        assertTrue(normalized.fixed());
        assertEquals(FixedPosition.NONE, normalized.fixedPosition());
        assertEquals(-1, normalized.fixedIndex());
    }

    @Test
    public void checkedFixedEntryRemainsFixedAfterRegeneration() {
        SetlistEntry first = entry(UUID.randomUUID());
        SetlistEntry second = entry(UUID.randomUUID());
        SetlistEntryTableModel model = new SetlistEntryTableModel(List.of(first, second));
        model.setValueAt(Boolean.TRUE, 1, model.fixedColumnIndex());
        SetlistProject project = new SetlistProject(List.of(new SetlistSession(
                "第1公演", model.entries(), model.performerNames())));

        SetlistProject regenerated = new SetlistRegenerator(new java.util.Random(1)).regenerate(project);
        SetlistEntry fixedEntry = regenerated.sessions().get(0).entries().get(1);

        assertEquals(second.id(), fixedEntry.id());
        assertTrue(fixedEntry.fixed());
        assertEquals(FixedPosition.NONE, fixedEntry.fixedPosition());
    }

    @Test
    public void entryCanMoveBetweenSessionModelsWithoutChangingItsIdentity() {
        SetlistEntry entry = entry(UUID.randomUUID());
        SetlistEntryTableModel source = new SetlistEntryTableModel(List.of(entry));
        SetlistEntryTableModel target = new SetlistEntryTableModel(List.of());

        target.appendEntry(source.removeEntryAndReturn(0));

        assertEquals(0, source.getRowCount());
        assertEquals(1, target.getRowCount());
        assertEquals(entry.id(), target.entries().get(0).id());
        assertTrue(target.performerNames().contains("出演者A"));
    }

    @Test
    public void newEntryStartsWithAllPerformerMarksBlank() {
        SetlistEntryTableModel model = new SetlistEntryTableModel(List.of(), List.of("出演者A"));

        model.addEntry();

        assertEquals(Boolean.FALSE, model.getValueAt(0, model.performerColumnIndex("出演者A")));
        assertFalse(model.entries().get(0).fixed());
    }

    private SetlistEntry entry(UUID sourceId) {
        return new SetlistEntry(
                UUID.randomUUID(), sourceId, "演目", 180,
                List.of("出演者A"), false, FixedPosition.NONE, -1);
    }
}
