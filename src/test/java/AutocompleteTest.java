import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class AutocompleteTest {
    private static String uniqueCommandMatch(String token) throws Exception {
        Method method = Main.class.getDeclaredMethod("uniqueCommandMatch", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, token);
    }

    @Test
    void uniqueCommandMatch_completesBuiltin() throws Exception {
        assertEquals("echo", runWithPath("", "ech"));
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
    void noMatchRingsBell() throws Exception {
        String output = runWithPathNoMatch("", "zzzz");
        assertEquals(true, output.contains("\u0007"));
        assertEquals(true, output.contains("BUFFER=zzzz"));
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

    @Test
    void multiMatchSecondTabPrintsListAndKeepsPrefix(@TempDir Path tempDir) throws Exception {
        Path expand = tempDir.resolve("expand");
        Path expect = tempDir.resolve("expect");
        Path expr = tempDir.resolve("expr");
        Files.writeString(expand, "#!/bin/sh\necho ok\n");
        Files.writeString(expect, "#!/bin/sh\necho ok\n");
        Files.writeString(expr, "#!/bin/sh\necho ok\n");
        try {
            Files.setPosixFilePermissions(expand, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.setPosixFilePermissions(expect, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.setPosixFilePermissions(expr, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException e) {
            expand.toFile().setExecutable(true);
            expect.toFile().setExecutable(true);
            expr.toFile().setExecutable(true);
        }

        String output = runWithPath(tempDir.toString(), "exp", true);
        String normalized = stripAnsi(output).replace("\r", "");
        String withoutBell = normalized.replace("\u0007", "");
        List<String> lines = withoutBell.lines()
                .map(line -> line.startsWith("$ ") ? line.substring(2) : line)
                .filter(line -> !line.isEmpty())
                .toList();

        assertEquals(true, output.contains("\u0007"));
        assertEquals(true,
                     containsLineSequence(lines, List.of("exp", "expand  expect  expr", "exp")));
        assertEquals(true, normalized.contains("BUFFER=exp"));
    }

    @Test
    void lcpCompletionExtendsPrefix(@TempDir Path tempDir) throws Exception {
        Path one = tempDir.resolve("xyz_foo");
        Path two = tempDir.resolve("xyz_foo_bar");
        Path three = tempDir.resolve("xyz_foo_bar_baz");
        Files.writeString(one, "#!/bin/sh\necho ok\n");
        Files.writeString(two, "#!/bin/sh\necho ok\n");
        Files.writeString(three, "#!/bin/sh\necho ok\n");
        try {
            Files.setPosixFilePermissions(one, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.setPosixFilePermissions(two, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.setPosixFilePermissions(three, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException e) {
            one.toFile().setExecutable(true);
            two.toFile().setExecutable(true);
            three.toFile().setExecutable(true);
        }

        String output = runWithPathLcp(tempDir.toString(), "xyz_");

        assertEquals(false, output.contains("\u0007"));
        assertEquals(true, output.contains("BUFFER=xyz_foo"));
    }

    @Test
    void lcpCompletionProgressesAndFinishes(@TempDir Path tempDir) throws Exception {
        Path one = tempDir.resolve("xyz_foo");
        Path two = tempDir.resolve("xyz_foo_bar");
        Path three = tempDir.resolve("xyz_foo_bar_baz");
        Files.writeString(one, "#!/bin/sh\necho ok\n");
        Files.writeString(two, "#!/bin/sh\necho ok\n");
        Files.writeString(three, "#!/bin/sh\necho ok\n");
        try {
            Files.setPosixFilePermissions(one, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.setPosixFilePermissions(two, PosixFilePermissions.fromString("rwxr-xr-x"));
            Files.setPosixFilePermissions(three, PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException e) {
            one.toFile().setExecutable(true);
            two.toFile().setExecutable(true);
            three.toFile().setExecutable(true);
        }

        String output = runWithPathLcp(tempDir.toString(), "xyz_", "_", "_");

        assertEquals(true, output.contains("BUFFER=xyz_foo_bar_baz "));
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
        String pathEnv = pathValue == null ? env.getOrDefault("PATH", "") : pathValue;
        env.put("PATH", pathEnv);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        process.waitFor();
        return output.isBlank() ? null : output.trim();
    }

    private String runWithPath(String pathValue, String token, boolean doubleTab) throws Exception {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add(DoubleTabHarness.class.getName());
        command.add(pathValue);
        command.add(token);

        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> env = builder.environment();
        String pathEnv = pathValue == null ? env.getOrDefault("PATH", "") : pathValue;
        env.put("PATH", pathEnv);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        process.waitFor();
        return output;
    }

    private String runWithPathNoMatch(String pathValue, String token) throws Exception {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add(NoMatchTabHarness.class.getName());
        command.add(pathValue);
        command.add(token);

        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> env = builder.environment();
        String pathEnv = pathValue == null ? env.getOrDefault("PATH", "") : pathValue;
        env.put("PATH", pathEnv);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        process.waitFor();
        return output;
    }

    private String runWithPathLcp(String pathValue, String token, String... appendAfterTabs) throws Exception {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");

        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(classpath);
        command.add(LcpHarness.class.getName());
        command.add(pathValue);
        command.add(token);
        for (String append : appendAfterTabs) {
            command.add(append);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> env = builder.environment();
        String pathEnv = pathValue == null ? env.getOrDefault("PATH", "") : pathValue;
        env.put("PATH", pathEnv);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        process.waitFor();
        return output;
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

    public static class NoMatchTabHarness {
        public static void main(String[] args) throws Exception {
            String token = args.length > 1 ? args[1] : "";
            java.lang.reflect.Field lastTab = Main.class.getDeclaredField("lastTabBuffer");
            lastTab.setAccessible(true);
            lastTab.set(null, null);

            Terminal terminal = TerminalBuilder.builder()
                    .system(false)
                    .dumb(true)
                    .type("dumb")
                    .build();
            DefaultParser parser = new DefaultParser();
            parser.setEscapeChars(new char[0]);
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(parser)
                    .build();
            reader.getBuffer().write(token);
            reader.getBuffer().cursor(reader.getBuffer().length());

            Method handleTab = Main.class.getDeclaredMethod("handleTab", LineReader.class);
            handleTab.setAccessible(true);
            handleTab.invoke(null, reader);

            System.out.print("BUFFER=" + reader.getBuffer().toString());
        }
    }

    public static class LcpHarness {
        public static void main(String[] args) throws Exception {
            String token = args.length > 1 ? args[1] : "";
            java.lang.reflect.Field lastTab = Main.class.getDeclaredField("lastTabBuffer");
            lastTab.setAccessible(true);
            lastTab.set(null, null);

            Terminal terminal = TerminalBuilder.builder()
                    .system(false)
                    .dumb(true)
                    .type("dumb")
                    .build();
            DefaultParser parser = new DefaultParser();
            parser.setEscapeChars(new char[0]);
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(parser)
                    .build();
            reader.getBuffer().write(token);
            reader.getBuffer().cursor(reader.getBuffer().length());

            Method handleTab = Main.class.getDeclaredMethod("handleTab", LineReader.class);
            handleTab.setAccessible(true);

            for (int i = 2; i < args.length; i++) {
                handleTab.invoke(null, reader);
                String append = args[i];
                if (append != null && !append.isEmpty()) {
                    reader.getBuffer().write(append);
                    reader.getBuffer().cursor(reader.getBuffer().length());
                }
            }

            handleTab.invoke(null, reader);

            System.out.print("BUFFER=" + reader.getBuffer().toString());
        }
    }

    public static class DoubleTabHarness {
        public static void main(String[] args) throws Exception {
            String token = args.length > 1 ? args[1] : "";
            java.lang.reflect.Field lastTab = Main.class.getDeclaredField("lastTabBuffer");
            lastTab.setAccessible(true);
            lastTab.set(null, null);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Terminal terminal = TerminalBuilder.builder()
                    .system(false)
                    .dumb(true)
                    .type("dumb")
                    .streams(new ByteArrayInputStream(new byte[0]), output)
                    .build();
            DefaultParser parser = new DefaultParser();
            parser.setEscapeChars(new char[0]);
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(parser)
                    .build();
            reader.getBuffer().write(token);
            reader.getBuffer().cursor(reader.getBuffer().length());
            System.out.print(token);

            Method handleTab = Main.class.getDeclaredMethod("handleTab", LineReader.class);
            handleTab.setAccessible(true);
            reader.getBuffer().cursor(reader.getBuffer().length());
            handleTab.invoke(null, reader);
            reader.getBuffer().cursor(reader.getBuffer().length());
            handleTab.invoke(null, reader);

            System.out.print("\nBUFFER=" + reader.getBuffer().toString());
        }
    }

    private static String stripAnsi(String value) {
        return value.replaceAll("\\u001B\\[[;?0-9]*[ -/]*[@-~]", "");
    }

    private static boolean containsLineSequence(List<String> lines, List<String> expected) {
        for (int i = 0; i <= lines.size() - expected.size(); i++) {
            boolean match = true;
            for (int j = 0; j < expected.size(); j++) {
                if (!lines.get(i + j).equals(expected.get(j))) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true;
            }
        }
        return false;
    }


}
