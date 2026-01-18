import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ExternalCommandTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void resetWorkspace() {
        Command.setCurrentWorkspace(tempDir.toAbsolutePath().toString());
    }

    @Test
    void run_printsStderrWhenOnlyStderrOutput() {
        Assumptions.assumeTrue(Command.findExecutable("sh") != null);
        Command command = Command.build("sh", "");
        command.setArgList(List.of("-c", "echo boom 1>&2"));

        AtomicReference<String> capturedErr = new AtomicReference<>();
        String output = TestUtils.captureStdout(() -> {
            capturedErr.set(TestUtils.captureStderr(() -> ExternalCommand.getInstance().run(command)));
        });

        assertEquals("", output);
        assertEquals("boom" + System.lineSeparator(), capturedErr.get());
    }

    @Test
    void run_allowsNonZeroExitCodes() {
        Assumptions.assumeTrue(Command.findExecutable("sh") != null);
        Command command = Command.build("sh", "");
        command.setArgList(List.of("-c", "exit 7"));

        AtomicReference<String> capturedErr = new AtomicReference<>();
        String output = TestUtils.captureStdout(() -> {
            capturedErr.set(TestUtils.captureStderr(() -> ExternalCommand.getInstance().run(command)));
        });

        assertEquals("", output);
        assertEquals("", capturedErr.get());
    }

    @Test
    void run_throwsWhenWorkingDirectoryIsInvalid() {
        Assumptions.assumeTrue(Command.findExecutable("sh") != null);
        Command command = Command.build("sh", "");
        command.setArgList(List.of("-c", "echo ok"));
        command.setWorkspace(tempDir.resolve("missing").toString());

        assertThrows(RuntimeException.class, () -> ExternalCommand.getInstance().run(command));
    }
}
