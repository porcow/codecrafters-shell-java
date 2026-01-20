import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HistoryCommandTest {

    @Test
    void history_printsAllCommands() {
        HistoryCommand.clearHistory();

        TestUtils.captureStdout(() -> Main.evalInput("echo hello"));
        TestUtils.captureStdout(() -> Main.evalInput("echo world"));
        TestUtils.captureStdout(() -> Main.evalInput("invalid_command"));

        String output = TestUtils.captureStdout(() -> Main.evalInput("history"));

        String expected = String.join(System.lineSeparator(),
                "    1  echo hello",
                "    2  echo world",
                "    3  invalid_command",
                "    4  history",
                "");
        assertEquals(expected, output);
    }

    @Test
    void history_limitsToLastNCommands() {
        HistoryCommand.clearHistory();

        TestUtils.captureStdout(() -> Main.evalInput("echo one"));
        TestUtils.captureStdout(() -> Main.evalInput("echo two"));

        String output = TestUtils.captureStdout(() -> Main.evalInput("history 2"));

        String expected = String.join(System.lineSeparator(),
                "    2  echo two",
                "    3  history 2",
                "");
        assertEquals(expected, output);
    }

    @Test
    void history_readsFromFile(@TempDir Path tempDir) throws Exception {
        HistoryCommand.clearHistory();
        Path historyFile = tempDir.resolve("history.log");
        String fileContent = String.join(System.lineSeparator(),
                "echo hello",
                "echo world",
                "");
        Files.writeString(historyFile, fileContent);

        String output = TestUtils.captureStdout(() -> {
            Main.evalInput("history -r " + historyFile);
            Main.evalInput("history");
        });

        String expected = String.join(System.lineSeparator(),
                "    1  history -r " + historyFile,
                "    2  echo hello",
                "    3  echo world",
                "    4  history",
                "");
        assertEquals(expected, output);
    }

    @Test
    void history_writesToFile(@TempDir Path tempDir) throws Exception {
        HistoryCommand.clearHistory();
        Path historyFile = tempDir.resolve("history.log");

        TestUtils.captureStdout(() -> Main.evalInput("echo hello"));
        TestUtils.captureStdout(() -> Main.evalInput("echo world"));
        TestUtils.captureStdout(() -> Main.evalInput("history -w " + historyFile));

        String content = Files.readString(historyFile);
        String expected = String.join(System.lineSeparator(),
                "echo hello",
                "echo world",
                "history -w " + historyFile,
                "");
        assertEquals(expected, content);
    }

    @Test
    void history_appendsOnlyNewCommands(@TempDir Path tempDir) throws Exception {
        HistoryCommand.clearHistory();
        Path historyFile = tempDir.resolve("history.log");

        TestUtils.captureStdout(() -> Main.evalInput("echo initial_command_1"));
        TestUtils.captureStdout(() -> Main.evalInput("echo initial_command_2"));
        TestUtils.captureStdout(() -> Main.evalInput("history -w " + historyFile));

        TestUtils.captureStdout(() -> Main.evalInput("echo new_command"));
        TestUtils.captureStdout(() -> Main.evalInput("history -a " + historyFile));

        String content = Files.readString(historyFile);
        String expected = String.join(System.lineSeparator(),
                "echo initial_command_1",
                "echo initial_command_2",
                "history -w " + historyFile,
                "echo new_command",
                "history -a " + historyFile,
                "");
        assertEquals(expected, content);
    }
}
