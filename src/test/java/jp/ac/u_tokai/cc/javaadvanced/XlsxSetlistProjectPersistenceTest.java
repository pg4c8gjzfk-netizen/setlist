package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

public class XlsxSetlistProjectPersistenceTest {

    @Test
    public void writerAndReaderPreserveEditableRegenerationState() throws Exception {
        SetlistEntry opening = entry("開演曲", List.of("出演者A"), true, FixedPosition.OPENING, -1);
        SetlistEntry substitute = entry("代理出演曲", List.of("代理出演者"), false, FixedPosition.NONE, -1);
        SetlistEntry indexed = entry("位置固定曲", List.of("出演者B"), true, FixedPosition.INDEX, 1);
        SetlistProject project = new SetlistProject(List.of(
                new SetlistSession("第1公演", List.of(opening, indexed)),
                new SetlistSession("第2公演", List.of(substitute))));
        File output = Files.createTempFile("setlist-project-", ".xlsx").toFile();

        new XlsxSetlistProjectWriter().write(project, output);
        SetlistProject restored = new XlsxSetlistProjectReader().read(output);

        assertEquals(project, restored);
        try (Workbook workbook = new XSSFWorkbook(output)) {
            int metadataIndex = workbook.getSheetIndex(XlsxSetlistProjectWriter.META_SHEET_NAME);
            assertTrue(metadataIndex >= 0);
            assertTrue(workbook.isSheetHidden(metadataIndex));
            assertEquals(
                    XlsxSetlistProjectWriter.FORMAT_VERSION,
                    workbook.getSheet(XlsxSetlistProjectWriter.META_SHEET_NAME).getRow(0).getCell(1).getStringCellValue());
        }
    }

    @Test
    public void emptySessionCanBeSavedAndRestored() throws Exception {
        SetlistProject project = new SetlistProject(List.of(new SetlistSession("空の公演", List.of())));
        File output = Files.createTempFile("empty-setlist-project-", ".xlsx").toFile();

        new XlsxSetlistProjectWriter().write(project, output);

        assertEquals(project, new XlsxSetlistProjectReader().read(output));
    }

    private SetlistEntry entry(
            String title,
            List<String> performers,
            boolean fixed,
            FixedPosition fixedPosition,
            int fixedIndex) {
        return new SetlistEntry(
                UUID.randomUUID(), UUID.randomUUID(), title, 180,
                performers, fixed, fixedPosition, fixedIndex);
    }
}
