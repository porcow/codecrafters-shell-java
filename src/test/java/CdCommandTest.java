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
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());

        Command command = Command.build("cd", "");
        command.setArgList(java.util.List.of("child"));
        CdCommand.getInstance().run(command);

        assertEquals(child.toAbsolutePath().toString(), Command.getCurrentWorkspace());
    }

    @Test
    void run_printsErrorWhenTargetMissing() {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());

        Command command = Command.build("cd", "");
        command.setArgList(java.util.List.of("missing"));

        String output = TestUtils.captureStdout(() -> CdCommand.getInstance().run(command));

        assertEquals("cd: missing: No such file or directory" + System.lineSeparator(), output);
    }

    @Test
    void run_expandsTildeToHome() {
        String home = System.getenv("HOME");
        Assumptions.assumeTrue(home != null && !home.isBlank());
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());

        Command command = Command.build("cd", "");
        command.setArgList(List.of("~"));
        CdCommand.getInstance().run(command);

        assertEquals(Path.of(home).normalize().toString(), Command.getCurrentWorkspace());
    }

    @Test
    void run_defaultsToHomeWhenArgsEmpty() {
        String home = System.getenv("HOME");
        Assumptions.assumeTrue(home != null && !home.isBlank());
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());

        Command command = Command.build("cd", "");
        command.setArgList(List.of());
        CdCommand.getInstance().run(command);

        assertEquals(Path.of(home).normalize().toString(), Command.getCurrentWorkspace());
    }
}
