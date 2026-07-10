package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class SetlistEntryTableModelTest {

    @Test
    public void performerEditCanRequestAllSessionsUpdate() {
        UUID sourceId = UUID.randomUUID();
        SetlistEntryTableModel model = new SetlistEntryTableModel(List.of(entry(sourceId)));
        AtomicReference<UUID> updatedSourceId = new AtomicReference<>();
        AtomicReference<List<String>> updatedPerformers = new AtomicReference<>();
        model.setPerformerChangeCallbacks(
                request -> SetlistEntryTableModel.PerformerChangeScope.ALL_SESSIONS,
                (id, performers) -> {
                    updatedSourceId.set(id);
                    updatedPerformers.set(performers);
                });

        model.setValueAt("出演者B、出演者C", 0, 3);

        assertEquals(List.of("出演者B", "出演者C"), model.entries().get(0).performers());
        assertEquals(sourceId, updatedSourceId.get());
        assertEquals(List.of("出演者B", "出演者C"), updatedPerformers.get());
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

        model.setValueAt(Boolean.TRUE, 0, 4);
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
        model.setValueAt(Boolean.TRUE, 1, 4);
        SetlistProject project = new SetlistProject(List.of(new SetlistSession("第1公演", model.entries())));

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
    }

    private SetlistEntry entry(UUID sourceId) {
        return new SetlistEntry(
                UUID.randomUUID(), sourceId, "演目", 180,
                List.of("出演者A"), false, FixedPosition.NONE, -1);
    }
}
