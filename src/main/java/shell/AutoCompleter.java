package shell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

public class AutoCompleter {
    private final String prompt;
    private String lastTabBuffer = null;

    public AutoCompleter(String prompt) {
        this.prompt = prompt;
    }

    public LineReader buildLineReader() {
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

    public String readLine(LineReader reader) {
        try {
            return reader.readLine(prompt);
        } catch (UserInterruptException e) {
            return "";
        } catch (EndOfFileException e) {
            System.out.println();
            HistoryCommand.writeOnExit();
            System.exit(0);
        }
        return "";
    }

    private final class BuiltinCompleter implements Completer {
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

    String uniqueCommandMatch(String token) {
        List<String> matches = findCommandMatches(token);
        if (matches.size() == 1) {
            return matches.get(0);
        }
        return null;
    }

    private List<String> findCommandMatches(String token) {
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

    boolean handleTab(LineReader reader) {
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
                System.out.print("\n" + String.join("  ", matches) + "\n" + prompt + prefix);
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

    private String currentCommandPrefix(LineReader reader) {
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

    private void ringBell(LineReader reader) {
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

    private String longestCommonPrefix(List<String> values) {
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

    void resetTabState() {
        lastTabBuffer = null;
    }
}
