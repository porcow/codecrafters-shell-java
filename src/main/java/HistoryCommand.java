import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class HistoryCommand implements CCRunnable {
    private static final List<String> HISTORY = new ArrayList<>();
    private static String historyFilePath;
    private static int lastAppendIndex = 0;
    private static HistoryCommand instance;

    private HistoryCommand() {
    }

    public static HistoryCommand getInstance() {
        if (instance == null) {
            instance = new HistoryCommand();
        }
        return instance;
    }

    public static void record(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        HISTORY.add(line);
    }

    public static void initializeFromEnv() {
        String histFile = System.getenv("HISTFILE");
        if (histFile == null || histFile.isBlank()) {
            return;
        }
        historyFilePath = histFile;
        record("history -r " + histFile);
        readFromFile(histFile);
        lastAppendIndex = HISTORY.size();
    }

    static void clearHistory() {
        HISTORY.clear();
        historyFilePath = null;
        lastAppendIndex = 0;
    }

    static void writeOnExit() {
        if (historyFilePath == null || historyFilePath.isBlank()) {
            return;
        }
        appendToFile(historyFilePath);
    }

    @Override
    public void run(Command cmd) {
        runWithStreams(cmd, System.in, System.out, System.err);
    }

    @Override
    public void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err) {
        PrintStream stdout = CCRunnable.toPrintStream(out);
        List<String> args = cmd.getArgList();
        if (args != null && !args.isEmpty()) {
            String option = args.get(0);
            if ("-w".equals(option)) {
                if (args.size() >= 2) {
                    String path = args.get(1);
                    historyFilePath = path;
                    writeToFile(path);
                    lastAppendIndex = HISTORY.size();
                }
                return;
            }
            if ("-r".equals(option)) {
                if (args.size() >= 2) {
                    String path = args.get(1);
                    historyFilePath = path;
                    readFromFile(path);
                }
                return;
            }
            if ("-a".equals(option)) {
                if (args.size() >= 2) {
                    String path = args.get(1);
                    historyFilePath = path;
                    appendToFile(path);
                }
                return;
            }
        }

        int limit = HISTORY.size();
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

        int start = Math.max(0, HISTORY.size() - limit);
        for (int i = start; i < HISTORY.size(); i++) {
            String entry = HISTORY.get(i);
            stdout.printf("%5d  %s%n", i + 1, entry);
        }
    }

    private static void readFromFile(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            Path file = Path.of(path);
            if (!Files.exists(file)) {
                return;
            }
            List<String> lines = Files.readAllLines(file);
            for (String line : lines) {
                if (line != null && !line.isBlank()) {
                    HISTORY.add(line);
                }
            }
        } catch (Exception e) {
            // Ignore history file read failures.
        }
    }

    private static void appendToFile(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        int start = Math.min(Math.max(lastAppendIndex, 0), HISTORY.size());
        if (start >= HISTORY.size()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < HISTORY.size(); i++) {
            builder.append(HISTORY.get(i)).append(System.lineSeparator());
        }
        try {
            Path file = Path.of(path);
            Files.writeString(file,
                    builder.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            lastAppendIndex = HISTORY.size();
        } catch (Exception e) {
            // Ignore history file append failures.
        }
    }

    private static void writeToFile(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (String entry : HISTORY) {
            builder.append(entry).append(System.lineSeparator());
        }
        try {
            Path file = Path.of(path);
            Files.writeString(file,
                    builder.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            // Ignore history file write failures.
        }
    }
}
