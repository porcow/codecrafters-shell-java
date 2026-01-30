package shell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assumptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CdCommandTest {
    @TempDir
    Path tempDir;

    @Test
    void run_updatesWorkspaceForRelativePath() throws Exception {
        Path child = Files.createDirectory(tempDir.resolve("child"));
        ShellContext context = new ShellContext(tempDir.toAbsolutePath().toString());

        Command command = Command.build(context, "cd", "");
        command.setArgList(java.util.List.of("child"));
        CdCommand.getInstance().run(command);

        assertEquals(child.toAbsolutePath().toString(), context.getWorkspace());
    }

    @Test
    void run_printsErrorWhenTargetMissing() {
        ShellContext context = new ShellContext(tempDir.toAbsolutePath().toString());

        Command command = Command.build(context, "cd", "");
        command.setArgList(java.util.List.of("missing"));

        String output = TestUtils.captureStdout(() -> CdCommand.getInstance().run(command));

        assertEquals("cd: missing: No such file or directory" + System.lineSeparator(), output);
    }

    @Test
    void run_expandsTildeToHome() {
        String home = System.getenv("HOME");
        Assumptions.assumeTrue(home != null && !home.isBlank());
        ShellContext context = new ShellContext(tempDir.toAbsolutePath().toString());

        Command command = Command.build(context, "cd", "");
        command.setArgList(List.of("~"));
        CdCommand.getInstance().run(command);

        assertEquals(Path.of(home).normalize().toString(), context.getWorkspace());
    }

    @Test
    void run_defaultsToHomeWhenArgsEmpty() {
        String home = System.getenv("HOME");
        Assumptions.assumeTrue(home != null && !home.isBlank());
        ShellContext context = new ShellContext(tempDir.toAbsolutePath().toString());

        Command command = Command.build(context, "cd", "");
        command.setArgList(List.of());
        CdCommand.getInstance().run(command);

        assertEquals(Path.of(home).normalize().toString(), context.getWorkspace());
    }
}
