import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

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
}
