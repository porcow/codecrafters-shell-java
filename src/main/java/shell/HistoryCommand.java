package shell;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;

public class HistoryCommand implements CommandRunner {
    private static HistoryCommand instance;

    private HistoryCommand() {
    }

    public static HistoryCommand getInstance() {
        if (instance == null) {
            instance = new HistoryCommand();
        }
        return instance;
    }

    public static void record(ShellContext context, String line) {
        if (context == null || line == null || line.isBlank()) {
            return;
        }
        context.getHistory().add(line);
    }

    public static void initializeFromEnv(ShellContext context) {
        if (context == null) {
            return;
        }
        String histFile = context.getEnv("HISTFILE");
        if (histFile == null || histFile.isBlank()) {
            return;
        }
        context.setHistoryFilePath(histFile);
        readFromFile(context, histFile);
        context.setLastAppendIndex(context.getHistory().size());
    }

    static void clearHistory(ShellContext context) {
        if (context == null) {
            return;
        }
        context.getHistory().clear();
        context.setHistoryFilePath(null);
        context.setLastAppendIndex(0);
    }

    static void writeOnExit(ShellContext context) {
        if (context == null) {
            return;
        }
        String path = context.getHistoryFilePath();
        if (path == null || path.isBlank()) {
            return;
        }
        appendToFile(context, path);
    }

    @Override
    public void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err) {
        PrintStream stdout = CommandRunner.toPrintStream(out);
        ShellContext context = cmd.getContext();
        if (context == null) {
            return;
        }
        List<String> history = context.getHistory();
        List<String> args = cmd.getArgList();
        if (args != null && !args.isEmpty()) {
            String option = args.get(0);
            if ("-w".equals(option)) {
                if (args.size() >= 2) {
                    String path = args.get(1);
                    context.setHistoryFilePath(path);
                    writeToFile(context, path);
                    context.setLastAppendIndex(history.size());
                }
                return;
            }
            if ("-r".equals(option)) {
                if (args.size() >= 2) {
                    String path = args.get(1);
                    context.setHistoryFilePath(path);
                    readFromFile(context, path);
                }
                return;
            }
            if ("-a".equals(option)) {
                if (args.size() >= 2) {
                    String path = args.get(1);
                    context.setHistoryFilePath(path);
                    appendToFile(context, path);
                }
                return;
            }
        }

        int limit = history.size();
        if (args != null && !args.isEmpty()) {
            try {
                int value = Integer.parseInt(args.get(0));
                if (value >= 0) {
                    limit = Math.min(limit, value);
                } else {
                    limit = 0;
                }
            } catch (NumberFormatException e) {
                limit = 0;
            }
        }

        int start = Math.max(0, history.size() - limit);
        for (int i = start; i < history.size(); i++) {
            String entry = history.get(i);
            stdout.printf("%5d  %s%n", i + 1, entry);
        }
    }

    private static void readFromFile(ShellContext context, String path) {
        if (context == null || path == null || path.isBlank()) {
            return;
        }
        try {
            Path file = Path.of(path);
            if (!Files.exists(file)) {
                return;
            }
            try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
                lines.filter(line -> line != null && !line.isBlank())
                        .forEach(context.getHistory()::add);
            }
        } catch (Exception e) {
            // Ignore history file read failures.
        }
    }

    private static void appendToFile(ShellContext context, String path) {
        if (context == null || path == null || path.isBlank()) {
            return;
        }
        List<String> history = context.getHistory();
        int start = Math.min(Math.max(context.getLastAppendIndex(), 0), history.size());
        if (start >= history.size()) {
            return;
        }
        try {
            Path file = Path.of(path);
            try (BufferedWriter writer = Files.newBufferedWriter(file,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                for (int i = start; i < history.size(); i++) {
                    writer.write(history.get(i));
                    writer.newLine();
                }
            }
            context.setLastAppendIndex(history.size());
        } catch (Exception e) {
            // Ignore history file append failures.
        }
    }

    private static void writeToFile(ShellContext context, String path) {
        if (context == null || path == null || path.isBlank()) {
            return;
        }
        try {
            Path file = Path.of(path);
            try (BufferedWriter writer = Files.newBufferedWriter(file,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                for (String entry : context.getHistory()) {
                    writer.write(entry);
                    writer.newLine();
                }
            }
        } catch (Exception e) {
            // Ignore history file write failures.
        }
    }
}
