import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PipelineTest {
    @TempDir
    Path tempDir;

    @Test
    void eval_redirectsStdoutToRunnableCommand() {
        Main.ParsedLine parsed = Main.parseLine("echo hello world > echo");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertEquals("hello world" + System.lineSeparator(), output);
    }

    @Test
    void eval_splitsOnOneGreaterThan() {
        Main.ParsedLine parsed = Main.parseLine("echo hello 1> echo");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertEquals("hello" + System.lineSeparator(), output);
    }

    @Test
    void eval_ignoresEscapedGreaterThan() {
        Main.ParsedLine parsed = Main.parseLine("echo a\\>b > echo");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertEquals("a>b" + System.lineSeparator(), output);
    }

    @Test
    void eval_ignoresQuotedGreaterThan() {
        Main.ParsedLine parsed = Main.parseLine("echo \"a>b\" > echo");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertEquals("a>b" + System.lineSeparator(), output);
    }

    @Test
    void eval_redirectsStdoutToFile() throws Exception {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());
        Main.ParsedLine parsed = Main.parseLine("echo hello > output.txt");

        Main.eval(parsed);

        String content = Files.readString(tempDir.resolve("output.txt"));
        assertEquals("hello" + System.lineSeparator(), content);
    }

    @Test
    void eval_overwritesFileOnRedirect() throws Exception {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());
        Path output = tempDir.resolve("output.txt");
        Files.writeString(output, "old" + System.lineSeparator());

        Main.ParsedLine parsed = Main.parseLine("echo new 1> output.txt");
        Main.eval(parsed);

        String content = Files.readString(output);
        assertEquals("new" + System.lineSeparator(), content);
    }

    @Test
    void eval_redirectsStderrToFile() throws Exception {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());
        Files.writeString(tempDir.resolve("existing.txt"), "contents" + System.lineSeparator());
        Main.ParsedLine parsed = Main.parseLine("cat existing.txt missing.txt 2> errors.txt");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertEquals("contents" + System.lineSeparator(), output);
        String errorContent = Files.readString(tempDir.resolve("errors.txt"));
        assertTrue(errorContent.contains("missing.txt"));
    }


    @Test
    void eval_redirectsStderrToRunnableCommand() {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());
        Main.ParsedLine parsed = Main.parseLine("cat missing.txt 2> echo");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertTrue(output.contains("cat:"));
        assertTrue(output.contains("No such file or directory"));
    }
}
