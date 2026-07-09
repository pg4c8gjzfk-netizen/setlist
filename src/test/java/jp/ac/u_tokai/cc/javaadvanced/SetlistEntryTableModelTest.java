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
    public void indexFixedEntryTracksItsNewOrder() {
        SetlistEntry first = entry(UUID.randomUUID());
        SetlistEntry second = entry(UUID.randomUUID());
        SetlistEntryTableModel model = new SetlistEntryTableModel(List.of(first, second));

        model.setValueAt(FixedPosition.INDEX, 0, 5);
        model.moveEntry(0, 1);

        SetlistEntry moved = model.entries().get(1);
        assertEquals(first.id(), moved.id());
        assertTrue(moved.fixed());
        assertEquals(FixedPosition.INDEX, moved.fixedPosition());
        assertEquals(1, moved.fixedIndex());
    }

    private SetlistEntry entry(UUID sourceId) {
        return new SetlistEntry(
                UUID.randomUUID(), sourceId, "演目", 180,
                List.of("出演者A"), false, FixedPosition.NONE, -1);
    }
}
