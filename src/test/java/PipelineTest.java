import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;

public class PipelineTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void resetWorkspace() {
        Command.setCurrentWorkspace(System.getProperty("user.dir"));
    }

    @Test
    void eval_redirectsStdoutToRunnableCommand() {
        CCParser.ParsedLine parsed = CCParser.parseLine("echo hello world > echo");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertEquals("hello world" + System.lineSeparator(), output);
    }

    @Test
    void eval_pipeIgnoresQuotedPipe() {
        String output = TestUtils.captureStdout(() -> Main.evalInput("/bin/echo \"a|b\" | /bin/cat"));

        assertEquals("a|b" + System.lineSeparator(), output);
    }

    @Test
    void eval_pipeStreamsStdoutIntoStdin(@TempDir Path tempDir) throws Exception {
        Path upper = tempDir.resolve("upper");
        Files.writeString(upper, "#!/bin/sh\ntr '[:lower:]' '[:upper:]'\n");
        try {
            Files.setPosixFilePermissions(upper, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException e) {
            upper.toFile().setExecutable(true);
        }

        String command = "/bin/echo hello | " + upper.toAbsolutePath();
        String output = TestUtils.captureStdout(() -> Main.evalInput(command));

        assertEquals("HELLO" + System.lineSeparator(), output);
    }

    @Test
    void eval_tailFollowPipesIntoHead(@TempDir Path tempDir) throws Exception {
        String tailPath = Command.findExecutable("tail");
        String headPath = Command.findExecutable("head");
        Assumptions.assumeTrue(tailPath != null && headPath != null);

        Path input = tempDir.resolve("lines.txt");
        String expected = String.join(System.lineSeparator(),
                "1. blueberry orange",
                "2. pear mango",
                "3. raspberry banana",
                "");
        String content = expected + System.lineSeparator() + System.lineSeparator();
        Files.writeString(input, content);

        String command = tailPath + " -f " + input + " | " + headPath + " -n 5";
        String output = TestUtils.captureStdout(() -> Main.evalInput(command));

        assertEquals(expected, output.stripTrailing() + System.lineSeparator());
    }

    @Test
    void eval_splitsOnOneGreaterThan() {
        CCParser.ParsedLine parsed = CCParser.parseLine("echo hello 1> echo");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertEquals("hello" + System.lineSeparator(), output);
    }

    @Test
    void eval_ignoresEscapedGreaterThan() {
        CCParser.ParsedLine parsed = CCParser.parseLine("echo a\\>b > echo");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertEquals("a>b" + System.lineSeparator(), output);
    }

    @Test
    void eval_ignoresQuotedGreaterThan() {
        CCParser.ParsedLine parsed = CCParser.parseLine("echo \"a>b\" > echo");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertEquals("a>b" + System.lineSeparator(), output);
    }

    @Test
    void eval_redirectsStdoutToFile() throws Exception {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());
        CCParser.ParsedLine parsed = CCParser.parseLine("echo hello > output.txt");

        Main.eval(parsed);

        String content = Files.readString(tempDir.resolve("output.txt"));
        assertEquals("hello" + System.lineSeparator(), content);
    }

    @Test
    void eval_overwritesFileOnRedirect() throws Exception {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());
        Path output = tempDir.resolve("output.txt");
        Files.writeString(output, "old" + System.lineSeparator());

        CCParser.ParsedLine parsed = CCParser.parseLine("echo new 1> output.txt");
        Main.eval(parsed);

        String content = Files.readString(output);
        assertEquals("new" + System.lineSeparator(), content);
    }

    @Test
    void eval_appendsWithDoubleGreaterThan() throws Exception {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());
        Path output = tempDir.resolve("append.txt");

        Main.eval(CCParser.parseLine("echo first >> append.txt"));
        Main.eval(CCParser.parseLine("echo second >> append.txt"));

        String content = Files.readString(output);
        assertEquals("first" + System.lineSeparator() + "second" + System.lineSeparator(), content);
    }

    @Test
    void eval_redirectsStderrToFile() throws Exception {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());
        Files.writeString(tempDir.resolve("existing.txt"), "contents" + System.lineSeparator());
        CCParser.ParsedLine parsed = CCParser.parseLine("cat existing.txt missing.txt 2> errors.txt");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertEquals("contents" + System.lineSeparator(), output);
        String errorContent = Files.readString(tempDir.resolve("errors.txt"));
        assertTrue(errorContent.contains("missing.txt"));
    }


    @Test
    void eval_redirectsStderrToRunnableCommand() {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());
        CCParser.ParsedLine parsed = CCParser.parseLine("cat missing.txt 2> echo");

        String output = TestUtils.captureStdout(() -> Main.eval(parsed));

        assertTrue(output.contains("cat:"));
        assertTrue(output.contains("No such file or directory"));
    }

    @Test
    void eval_appendsStderrWithTwoGreaterThan() throws Exception {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());
        Path errors = tempDir.resolve("errors.txt");

        Main.eval(CCParser.parseLine("cat missing1.txt 2>> errors.txt"));
        Main.eval(CCParser.parseLine("cat missing2.txt 2>> errors.txt"));

        String content = Files.readString(errors);
        int first = content.indexOf("missing1.txt");
        int second = content.indexOf("missing2.txt");
        assertTrue(first >= 0);
        assertTrue(second > first);
    }
}
