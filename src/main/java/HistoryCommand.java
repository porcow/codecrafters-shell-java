import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class HistoryCommand implements CCRunnable {
    private static final List<String> HISTORY = new ArrayList<>();
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

    static void clearHistory() {
        HISTORY.clear();
    }

    @Override
    public void run(Command cmd) {
        runWithStreams(cmd, System.in, System.out, System.err);
    }

    @Override
    public void runWithStreams(Command cmd, InputStream in, OutputStream out, OutputStream err) {
        PrintStream stdout = CCRunnable.toPrintStream(out);
        List<String> args = cmd.getArgList();
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
}
