import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class AutocompleteTest {
    private static String uniqueCommandMatch(String token) throws Exception {
        Method method = Main.class.getDeclaredMethod("uniqueCommandMatch", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, token);
    }

    @Test
    void uniqueCommandMatch_completesEcho() throws Exception {
        assertEquals("echo", runWithPath("", "ech"));
    }

    @Test
    void uniqueCommandMatch_completesExit() throws Exception {
        assertEquals("exit", runWithPath("", "exi"));
    }

    @Test
    void uniqueCommandMatch_returnsNullWhenAmbiguousBuiltin() throws Exception {
        assertNull(runWithPath("", "e"));
    }

    @Test
    void uniqueCommandMatch_returnsNullWhenNoMatch() throws Exception {
        assertNull(runWithPath("", "nope"));
    }

    @Test
    void uniqueCommandMatch_findsExecutableInPath(@TempDir Path tempDir) throws Exception {
        Path exec = tempDir.resolve("custom_executable");
        Files.writeString(exec, "#!/bin/sh\necho ok\n");
        try {
            Files.setPosixFilePermissions(exec, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException e) {
            exec.toFile().setExecutable(true);
        }

        String output = runWithPath(tempDir.toString(), "custom");

        assertEquals("custom_executable", output);
    }

    @Test
    void uniqueCommandMatch_returnsNullWhenAmbiguousWithExecutable(@TempDir Path tempDir) throws Exception {
        Path first = tempDir.resolve("custom_one");
        Path second = tempDir.resolve("custom_two");
        Files.writeString(first, "#!/bin/sh\necho ok\n");
        Files.writeString(second, "#!/bin/sh\necho ok\n");
        try {
            Files.setPosixFilePermissions(first, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.setPosixFilePermissions(second, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException e) {
            first.toFile().setExecutable(true);
            second.toFile().setExecutable(true);
        }

        String output = runWithPath(tempDir.toString(), "custom");

        assertNull(output);
    }

    private String runWithPath(String pathValue, String token) throws Exception {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add(AutocompleteHarness.class.getName());
        command.add(pathValue);
        command.add(token);
        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> env = builder.environment();
        String originalPath = env.get("PATH");
        if (pathValue != null && !pathValue.isBlank()) {
            if (originalPath != null && !originalPath.isBlank()) {
                pathValue = pathValue + java.io.File.pathSeparator + originalPath;
            }
        } else {
            pathValue = originalPath == null ? "" : originalPath;
        }
        env.put("PATH", pathValue);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        process.waitFor();
        return output.isBlank() ? null : output.trim();
    }

    public static class AutocompleteHarness {
        public static void main(String[] args) throws Exception {
            String token = args.length > 1 ? args[1] : "";
            Method method = Main.class.getDeclaredMethod("uniqueCommandMatch", String.class);
            method.setAccessible(true);
            Object result = method.invoke(null, token);
            if (result != null) {
                System.out.print(result.toString());
            }
        }
    }

    
}
