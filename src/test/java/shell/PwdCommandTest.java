package shell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class PwdCommandTest {
    @Test
    void run_printsCurrentWorkspace() throws Exception {
        Path tempDir = Files.createTempDirectory("pwd-test");
        ShellContext context = new ShellContext(tempDir.toAbsolutePath().toString());

        String output = TestUtils.captureStdout(() ->
                PwdCommand.getInstance().run(Command.build(context, "pwd", "")));

        assertEquals(tempDir.toAbsolutePath().toString() + System.lineSeparator(), output);
    }
}
