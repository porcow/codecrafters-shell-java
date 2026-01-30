package shell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QuotedExecutableTest {

    private ShellContext context;
    private Shell shell;
    private Path testDir;

    @BeforeEach
    void resetWorkspace() throws Exception {
        Path base = Path.of(System.getProperty("user.dir"), "target");
        Files.createDirectories(base);
        testDir = Files.createTempDirectory(base, "quoted-exec-");
        context = new ShellContext(testDir.toAbsolutePath().toString());
        shell = new Shell(context);
    }

    @Test
    void eval_executesQuotedExecutableWithSpaces_singleQuotes() throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(Path.of("/bin/sh")));
        Path exec = writeCatScript(testDir.resolve("exe with space"));
        Assumptions.assumeTrue(Files.isExecutable(exec));
        Path input = testDir.resolve("input.txt");
        String content = "raspberry mango." + System.lineSeparator();
        Files.writeString(input, content);

        String commandLine = "'" + exec + "' " + input;
        String output = TestUtils.captureStdout(() ->
                shell.eval(CCParser.parseLine(context, commandLine)));

        assertEquals(content, output);
    }

    @Test
    void eval_executesQuotedExecutableWithSpaces_doubleQuotes() throws Exception {
        Assumptions.assumeTrue(Files.isExecutable(Path.of("/bin/sh")));
        Path exec = writeCatScript(testDir.resolve("exe with space"));
        Assumptions.assumeTrue(Files.isExecutable(exec));
        Path input = testDir.resolve("input.txt");
        String content = "banana strawberry." + System.lineSeparator();
        Files.writeString(input, content);

        String commandLine = "\"" + exec + "\" " + input;
        String output = TestUtils.captureStdout(() ->
                shell.eval(CCParser.parseLine(context, commandLine)));

        assertEquals(content, output);
    }

    private Path writeCatScript(Path path) throws Exception {
        String script = "#!/bin/sh\ncat \"$1\"\n";
        Files.writeString(path, script);
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException e) {
            path.toFile().setExecutable(true);
        }
        return path;
    }
}
