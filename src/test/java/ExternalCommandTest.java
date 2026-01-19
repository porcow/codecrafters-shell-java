import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
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

    @Test
    void run_usesResolvedPathForQuotedExecutableName() throws Exception {
        Path exec = writeEchoScript(tempDir.resolve("my \\'echo\\'"));
        Assumptions.assumeTrue(Files.isExecutable(exec));
        Command command = Main.parse("\"my \\\\'echo\\\\'\" hello");
        command.setPath(exec.toString());

        String output = TestUtils.captureStdout(() -> ExternalCommand.getInstance().run(command));

        assertEquals("hello" + System.lineSeparator(), output);
    }

    private Path writeEchoScript(Path path) throws Exception {
        String script = "#!/bin/sh\n" +
                "echo \"$@\"\n";
        Files.writeString(path, script);
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException e) {
            path.toFile().setExecutable(true);
        }
        return path;
    }
}
