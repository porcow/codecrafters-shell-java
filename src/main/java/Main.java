import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Main {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final String PROMPT = "$ ";
    private static final LineReader LINE_READER = buildLineReader();
    private static String lastTabBuffer = null;
    public static void main(String[] args) throws Exception {
        HistoryCommand.initializeFromEnv();

        while (true) {
            String input = read();
            evalInput(input);
        }
    }

    public static String read() {
        if (LINE_READER != null) {
            try {
                return LINE_READER.readLine(PROMPT);
            } catch (UserInterruptException e) {
                return "";
            } catch (EndOfFileException e) {
                System.out.println();
                HistoryCommand.writeOnExit();
                System.exit(0);
            }
        }

        System.out.print(PROMPT);
        System.out.flush();
        return SCANNER.nextLine();
    }

    private static String uniqueCommandMatch(String token) {
        List<String> matches = findCommandMatches(token);
        if (matches.size() == 1) {
            return matches.get(0);
        }
        return null;
    }

    private static List<String> findCommandMatches(String token) {
        List<String> matches = new ArrayList<>();
        if (token == null || token.isEmpty()) {
            return matches;
        }

        java.util.Set<String> unique = new java.util.TreeSet<>();
        for (String builtin : Command.getBuiltinMap().keySet()) {
            if (builtin.startsWith(token)) {
                unique.add(builtin);
            }
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null && !pathEnv.isBlank()) {
            String[] dirs = pathEnv.split(File.pathSeparator);
            for (String dir : dirs) {
                if (dir == null) {
                    continue;
                }
                String dirPath = dir.isEmpty() ? "." : dir;
                if (dirPath.isBlank()) {
                    continue;
                }
                File directory = new File(dirPath);
                File[] entries = directory.listFiles();
                if (entries == null) {
                    continue;
                }
                for (File entry : entries) {
                    String name = entry.getName();
                    if (entry.isFile() && entry.canExecute() && name.startsWith(token)) {
                        unique.add(name);
                    }
                }
            }
        }

        matches.addAll(unique);
        return matches;
    }

    private static LineReader buildLineReader() {
        if (System.console() == null) {
            return null;
        }
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
            DefaultParser parser = new DefaultParser();
            parser.setEscapeChars(new char[0]);
            Completer completer = new BuiltinCompleter();
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .parser(parser)
                    .completer(completer)
                    .build();
            reader.getWidgets().put("custom-tab", () -> handleTab(reader));
            reader.getKeyMaps().get(LineReader.MAIN).bind(new Reference("custom-tab"), "\t");
            return reader;
        } catch (Exception e) {
            return null;
        }
    }

    private static final class BuiltinCompleter implements Completer {
        @Override
        public void complete(LineReader reader,
                             org.jline.reader.ParsedLine line,
                             List<Candidate> candidates) {
            if (line.wordIndex() != 0) {
                return;
            }
            String match = uniqueCommandMatch(line.word());
            if (match == null) {
                return;
            }
            candidates.add(new Candidate(match, match, null, null, " ", null, true));
        }
    }

    private static boolean handleTab(LineReader reader) {
        String prefix = currentCommandPrefix(reader);
        if (prefix == null || prefix.isEmpty()) {
            lastTabBuffer = null;
            return true;
        }

        List<String> matches = findCommandMatches(prefix);
        if (matches.isEmpty()) {
            ringBell(reader);
            lastTabBuffer = null;
            return true;
        }

        if (matches.size() == 1) {
            String match = matches.get(0);
            if (!match.equals(prefix)) {
                reader.getBuffer().write(match.substring(prefix.length()) + " ");
            } else {
                reader.getBuffer().write(" ");
            }
            lastTabBuffer = null;
            return true;
        }

        String commonPrefix = longestCommonPrefix(matches);
        if (commonPrefix.length() > prefix.length()) {
            reader.getBuffer().write(commonPrefix.substring(prefix.length()));
            lastTabBuffer = null;
            return true;
        }

        String buffer = reader.getBuffer().toString();
        if (buffer.equals(lastTabBuffer)) {
            String terminalType = reader.getTerminal().getType();
            if (System.console() == null
                    || terminalType == null
                    || terminalType.startsWith("dumb")) {
                System.out.print("\n" + String.join("  ", matches) + "\n" + PROMPT + prefix);
                System.out.flush();
            } else {
                java.io.Writer writer = reader.getTerminal().writer();
                try {
                    writer.write("\n" + String.join("  ", matches) + "\n");
                    writer.flush();
                } catch (java.io.IOException e) {
                    // Ignore write failures in non-interactive terminals.
                }
                reader.getBuffer().cursor(prefix.length());
                reader.callWidget(LineReader.REDRAW_LINE);
            }
        } else {
            ringBell(reader);
        }
        lastTabBuffer = buffer;
        return true;
    }

    private static String currentCommandPrefix(LineReader reader) {
        String line = reader.getBuffer().toString();
        int cursor = reader.getBuffer().cursor();
        int start = 0;
        while (start < line.length() && Character.isWhitespace(line.charAt(start))) {
            start++;
        }
        if (cursor < start) {
            return "";
        }
        int end = start;
        while (end < line.length() && !Character.isWhitespace(line.charAt(end))) {
            end++;
        }
        if (cursor > end) {
            return null;
        }
        return line.substring(start, cursor);
    }

    private static void ringBell(LineReader reader) {
        String terminalType = reader.getTerminal().getType();
        if (System.console() == null
                || terminalType == null
                || terminalType.startsWith("dumb")) {
            System.out.print("\007");
            System.out.flush();
            return;
        }
        java.io.Writer writer = reader.getTerminal().writer();
        try {
            writer.write("\007");
            writer.flush();
        } catch (java.io.IOException e) {
            // Ignore bell failures in non-interactive terminals.
        }
    }

    private static String longestCommonPrefix(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        String prefix = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            String value = values.get(i);
            int max = Math.min(prefix.length(), value.length());
            int idx = 0;
            while (idx < max && prefix.charAt(idx) == value.charAt(idx)) {
                idx++;
            }
            prefix = prefix.substring(0, idx);
            if (prefix.isEmpty()) {
                break;
            }
        }
        return prefix;
    }

    public static void evalInput(String inputString) {
        if (inputString != null && !inputString.isBlank()) {
            HistoryCommand.record(inputString.trim());
        }
        List<String> pipelineParts = CCParser.splitPipeline(inputString);
        if (pipelineParts.size() > 1) {
            evalPipeline(pipelineParts);
            return;
        }
        CCParser.ParsedLine parsed = CCParser.parseLine(inputString);
        eval(parsed);
    }
    public static void eval(CCParser.ParsedLine parsed) {
        if (parsed == null || parsed.command() == null) {
            return;
        }

        if (parsed.redirectPart() == null || parsed.redirectPart().isBlank()) {
            runCommand(parsed.command());
            return;
        }

        List<String> redirectTokens = CCParser.parseArguments(parsed.redirectPart());
        if (redirectTokens.isEmpty()) {
            runCommand(parsed.command());
            return;
        }

        CCRunnable runner = resolveRunner(parsed.command());
        if (runner == null) {
            System.out.println(parsed.command().getName() + ": command not found");
            return;
        }
        boolean append = parsed.redirectType() == CCParser.RedirectType.STDOUT_APPEND
                || parsed.redirectType() == CCParser.RedirectType.STDERR_APPEND;
        try (OutputStream redirectStream = openRedirectStream(parsed.command(),
                redirectTokens.get(0),
                append)) {
            if (parsed.redirectType() == CCParser.RedirectType.STDERR
                    || parsed.redirectType() == CCParser.RedirectType.STDERR_APPEND) {
                runner.runWithStreams(parsed.command(), System.in, System.out, redirectStream);
            } else {
                runner.runWithStreams(parsed.command(), System.in, redirectStream, System.err);
            }
        } catch (RuntimeException e) {
            reportRunError(parsed.command(), e);
        } catch (Exception e) {
            reportRunError(parsed.command(), new RuntimeException(e.getMessage(), e));
        }
    }

    private static void evalPipeline(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            return;
        }

        List<Command> commands = new ArrayList<>();
        List<CCRunnable> runners = new ArrayList<>();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                return;
            }
            Command command = CCParser.parseTokens(CCParser.parseArguments(part));
            CCRunnable runner = resolveRunner(command);
            if (runner == null) {
                System.out.println(command.getName() + ": command not found");
                return;
            }
            commands.add(command);
            runners.add(runner);
        }

        boolean allExternal = true;
        for (Command command : commands) {
            if (command.isBuiltin()) {
                allExternal = false;
                break;
            }
        }
        if (allExternal) {
            // For external-only pipelines, use OS-level piping for correct stream behavior.
            evalExternalPipeline(commands);
            return;
        }

        List<Thread> threads = new ArrayList<>();
        InputStream nextInput = System.in;
        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            CCRunnable runner = runners.get(i);
            InputStream input = nextInput;
            OutputStream output = System.out;
            if (i < commands.size() - 1) {
                try {
                    PipedOutputStream pipeOut = new PipedOutputStream();
                    PipedInputStream pipeIn = new PipedInputStream(pipeOut);
                    output = pipeOut;
                    nextInput = pipeIn;
                } catch (Exception e) {
                    reportRunError(command, new RuntimeException(e.getMessage(), e));
                    return;
                }
            }

            OutputStream targetOut = output;
            InputStream targetIn = input;
            Thread thread = new Thread(() -> {
                try {
                    // Execute each stage in a thread and wire its output to the next stage.
                    runner.runWithStreams(command, targetIn, targetOut, System.err);
                } catch (RuntimeException e) {
                    reportRunError(command, e);
                } finally {
                    if (targetOut != System.out) {
                        try {
                            targetOut.close();
                        } catch (Exception e) {
                            // Ignore close failures.
                        }
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }

        Thread last = threads.get(threads.size() - 1);
        try {
            // Wait for the last stage before interrupting upstream stages.
            last.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        for (int i = 0; i < threads.size() - 1; i++) {
            Thread thread = threads.get(i);
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void evalExternalPipeline(List<Command> commands) {
        boolean interactive = System.console() != null;
        List<ProcessBuilder> builders = new ArrayList<>();
        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            List<String> commandLine = new ArrayList<>();
            String execName = command.getName();
            String execPath = command.getPath();
            if (execName == null || execName.isBlank()) {
                commandLine.add(execPath);
            } else {
                commandLine.add(execName);
            }
            List<String> args = command.getArgList();
            if (args != null && !args.isEmpty()) {
                commandLine.addAll(args);
            }

            String workspace = command.getWorkspace();
            if (workspace == null || workspace.isBlank()) {
                workspace = System.getProperty("user.dir");
            }
            ProcessBuilder builder = new ProcessBuilder(commandLine)
                    .directory(new File(workspace))
                    .redirectError(ProcessBuilder.Redirect.INHERIT);
            if (i == 0) {
                // Let the first process read from our stdin.
                builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            }
            if (i == commands.size() - 1) {
                if (interactive) {
                    // In interactive mode, inherit stdout so line-buffered tools flush immediately.
                    builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                } else {
                    // Pipe the last stdout into Java so tests can capture it.
                    builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
                }
            }
            builders.add(builder);
        }

        Thread stdoutThread = null;
        List<Process> processes = null;
        try {
            // Let the OS wire the pipeline between all external commands.
            processes = ProcessBuilder.startPipeline(builders);
            Process last = processes.get(processes.size() - 1);
            if (!interactive) {
                stdoutThread = new Thread(() -> {
                    try {
                        // Stream the last process output into System.out.
                        last.getInputStream().transferTo(System.out);
                    } catch (java.io.IOException e) {
                        // Ignore pipe failures while the last process is ending.
                    } finally {
                        try {
                            System.out.flush();
                        } catch (Exception e) {
                            // Ignore flush failures.
                        }
                    }
                });
                stdoutThread.start();
            }

            // Wait for the last stage, then tear down earlier stages (e.g., tail -f).
            last.waitFor();
            if (stdoutThread != null) {
                stdoutThread.join();
            }

            for (int i = 0; i < processes.size() - 1; i++) {
                Process process = processes.get(i);
                if (process.isAlive()) {
                    process.destroy();
                }
            }
            for (Process process : processes) {
                process.waitFor();
            }
        } catch (Exception e) {
            if (stdoutThread != null && stdoutThread.isAlive()) {
                stdoutThread.interrupt();
            }
            if (processes != null) {
                for (Process process : processes) {
                    process.destroy();
                }
            }
            if (!commands.isEmpty()) {
                reportRunError(commands.get(0), new RuntimeException(e.getMessage(), e));
            }
        }
    }

    private static void runCommand(Command command) {
        CCRunnable runner = resolveRunner(command);
        if (runner == null) {
            System.out.println(command.getName() + ": command not found");
            return;
        }
        try {
            runner.run(command);
        } catch (RuntimeException e) {
            reportRunError(command, e);
        }
    }

    private static CCRunnable resolveRunner(Command command) {
        if (command == null || command.getName() == null || command.getName().isBlank()) {
            return null;
        }
        if (command.isBuiltin()) {
            return Command.getBuiltinMap().get(command.getName());
        }
        if (command.isRunable()) {
            return ExternalCommand.getInstance();
        }
        return null;
    }

    private static void reportRunError(Command command, RuntimeException e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.toString();
        }

        System.err.println(command.getName() + ": " + message);
    }

    private static OutputStream openRedirectStream(Command command,
                                                   String redirectPath,
                                                   boolean append) {
        try {
            Path path = Path.of(redirectPath);
            if (!path.isAbsolute()) {
                String base = command.getWorkspace();
                if (base == null || base.isBlank()) {
                    base = System.getProperty("user.dir");
                }
                path = Path.of(base).resolve(redirectPath);
            }
            if (append) {
                return Files.newOutputStream(path.normalize(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } else {
                return Files.newOutputStream(path.normalize(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
