package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class SetlistProjectModelTest {

    @Test
    public void copiesCollectionsToKeepProjectImmutable() {
        UUID id = UUID.randomUUID();
        List<String> performers = new java.util.ArrayList<>(List.of("さやか"));
        SetlistEntry entry = new SetlistEntry(
                id,
                id,
                "曲A",
                180,
                performers,
                false,
                FixedPosition.NONE,
                -1);
        List<SetlistEntry> entries = new java.util.ArrayList<>(List.of(entry));
        SetlistSession session = new SetlistSession("第1公演", entries);
        SetlistProject project = new SetlistProject(new java.util.ArrayList<>(List.of(session)));

        performers.add("だいご");
        entries.clear();

        assertEquals(List.of("さやか"), project.sessions().get(0).entries().get(0).performers());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidFixedIndex() {
        UUID id = UUID.randomUUID();

        new SetlistEntry(
                id,
                id,
                "曲A",
                180,
                List.of("さやか"),
                true,
                FixedPosition.INDEX,
                -1);
    }
}
