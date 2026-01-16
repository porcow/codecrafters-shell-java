import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void eval_splitsOnBangGreaterThan() {
        Main.ParsedLine parsed = Main.parseLine("echo hello !> echo");

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

        Main.ParsedLine parsed = Main.parseLine("echo new !> output.txt");
        Main.eval(parsed);

        String content = Files.readString(output);
        assertEquals("new" + System.lineSeparator(), content);
    }
}
