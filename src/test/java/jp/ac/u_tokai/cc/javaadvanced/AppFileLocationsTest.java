package jp.ac.u_tokai.cc.javaadvanced;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** 入出力先が作業ディレクトリに依存しないことを確認します。 */
public class AppFileLocationsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createsApplicationOutputDirectoryUnderStableDefault() throws Exception {
        File documents = temporaryFolder.newFolder("Documents");
        AppFileLocations locations = new AppFileLocations(documents);

        File outputDirectory = locations.outputDirectory();

        assertEquals(documents.getAbsoluteFile(), locations.inputDirectory());
        assertEquals(
                new File(documents, AppFileLocations.OUTPUT_DIRECTORY_NAME).getAbsoluteFile(),
                outputDirectory);
        assertTrue(outputDirectory.isDirectory());
        assertEquals(
                new File(outputDirectory, "setlist-project.xlsx"),
                locations.defaultOutputFile("setlist-project.xlsx"));
    }

    @Test
    public void remembersInputAndOutputDirectoriesSeparately() throws Exception {
        File defaultDirectory = temporaryFolder.newFolder("default");
        File inputDirectory = temporaryFolder.newFolder("input");
        File outputDirectory = temporaryFolder.newFolder("output");
        File inputFile = new File(inputDirectory, "source.xlsx");
        File outputFile = new File(outputDirectory, "result.xlsx");
        assertTrue(inputFile.createNewFile());
        assertTrue(outputFile.createNewFile());
        AppFileLocations locations = new AppFileLocations(defaultDirectory);

        locations.rememberInputFile(inputFile);
        locations.rememberOutputFile(outputFile);

        assertEquals(inputDirectory.getAbsoluteFile(), locations.inputDirectory());
        assertEquals(outputDirectory.getAbsoluteFile(), locations.outputDirectory());
    }

    @Test
    public void rejectsMissingDefaultDirectory() throws Exception {
        File missingDirectory = new File(temporaryFolder.getRoot(), "missing");

        try {
            new AppFileLocations(missingDirectory);
            fail("存在しない初期フォルダは受け付けない必要があります。");
        } catch (IllegalArgumentException expected) {
            assertEquals(
                    "defaultDirectory must be an existing directory",
                    expected.getMessage());
        }
    }
}
