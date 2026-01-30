package shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ExternalCommandTest {
    @TempDir
    Path tempDir;
    private ShellContext context;

    @BeforeEach
    void resetWorkspace() {
        context = new ShellContext(tempDir.toAbsolutePath().toString());
    }

    @Test
    void run_printsStderrWhenOnlyStderrOutput() {
        Assumptions.assumeTrue(Command.findExecutable("sh") != null);
        Command command = Command.build(context, "sh", "");
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
        Command command = Command.build(context, "sh", "");
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
        Command command = Command.build(context, "sh", "");
        command.setArgList(List.of("-c", "echo ok"));
        command.setWorkspace(tempDir.resolve("missing").toString());

        assertThrows(RuntimeException.class, () -> ExternalCommand.getInstance().run(command));
    }

    @Test
    void run_usesResolvedPathForQuotedExecutableWithBackslashNNoSpace() throws Exception {
        Path exec = writeEchoScript(tempDir.resolve("my \\necho"));
        Assumptions.assumeTrue(Files.isExecutable(exec));
        String output = runWithTempPath("'my \\necho' hello");

        assertEquals("hello" + System.lineSeparator(), output);
    }

    @Test
    void eval_passesProgramNameAsArgZeroFromPath() throws Exception {
        Path exec = writeArgListScript(tempDir.resolve("custom_exe_" + System.nanoTime()));
        Assumptions.assumeTrue(Files.isExecutable(exec));

        String output = runWithTempPath(exec.getFileName().toString() + " Alice Maria Alice");

        String name = exec.getFileName().toString();
        String expected = String.join(System.lineSeparator(),
                "Program was passed 4 args (including program name).",
                "Arg #0 (program name): " + name,
                "Arg #1: Alice",
                "Arg #2: Maria",
                "Arg #3: Alice",
                "");
        assertEquals(expected, output);
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

    private Path writeArgListScript(Path path) throws Exception {
        String script = "#!/bin/sh\n" +
                "count=$(($#+1))\n" +
                "name=$(basename \"$0\")\n" +
                "echo \"Program was passed $count args (including program name).\"\n" +
                "echo \"Arg #0 (program name): $name\"\n" +
                "i=1\n" +
                "for arg in \"$@\"; do\n" +
                "  echo \"Arg #$i: $arg\"\n" +
                "  i=$((i+1))\n" +
                "done\n";
        Files.writeString(path, script);
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException e) {
            path.toFile().setExecutable(true);
        }
        return path;
    }

    private String runWithTempPath(String line) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(EvalHarness.class.getName());
        command.add(line);

        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> env = builder.environment();
        env.put("PATH", tempDir + File.pathSeparator + env.getOrDefault("PATH", ""));
        builder.directory(tempDir.toFile());

        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        process.waitFor();
        return output;
    }

    public static class EvalHarness {
        public static void main(String[] args) {
            String line = String.join(" ", args);
            Shell shell = new Shell();
            shell.eval(CCParser.parseLine(shell.getContext(), line));
        }
    }
}
