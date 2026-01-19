import java.util.Scanner;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.File;
import java.nio.charset.StandardCharsets;
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
    private static final String[] AUTOCOMPLETE_BUILTINS = {"echo", "exit"};
    private static final LineReader LINE_READER = buildLineReader();
    private static String lastTabBuffer = null;
    final static Map<String, CCRunnable> builtinMap = new HashMap<String, CCRunnable>() {{
            put("echo", EchoCommand.getInstance());
            put("exit", ExitCommand.getInstance());
            put("type", TypeCommand.getInstance());
            put("pwd", PwdCommand.getInstance());
            put("cd", CdCommand.getInstance());
        }};

    public static void main(String[] args) throws Exception {

        while (true) {
            String input = read();
            ParsedLine parsed = parseLine(input);
            eval(parsed);
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
        for (String builtin : AUTOCOMPLETE_BUILTINS) {
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

        String buffer = reader.getBuffer().toString();
        if (buffer.equals(lastTabBuffer)) {
            reader.printAbove(String.join("  ", matches));
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
        java.io.Writer writer = reader.getTerminal().writer();
        try {
            writer.write("\007");
            writer.flush();
        } catch (java.io.IOException e) {
            // Ignore bell failures in non-interactive terminals.
        }
    }

    public static Command parse(String inputString) {
        return parseTokens(tokenize(inputString));
    }

    public static List<String> parseArguments(String inputString) {
        return tokenize(inputString);
    }

    public static ParsedLine parseLine(String inputString) {
        SplitResult split = splitRedirection(inputString);
        Command command = parse(split.left);
        return new ParsedLine(command, split.right, split.type);
    }

    private static SplitResult splitRedirection(String inputString) {
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < inputString.length(); i++) {
            char ch = inputString.charAt(i);

            // Only split on >, 1>, 2>, >>, 1>>, or 2>> when we're not inside quotes or escapes.
            if (!inSingleQuotes && !inDoubleQuotes && ch == '\\') {
                if (i + 1 < inputString.length()) {
                    i++;
                }
                continue;
            }

            if (inDoubleQuotes && ch == '\\') {
                if (i + 1 < inputString.length()) {
                    char next = inputString.charAt(i + 1);
                    if (next == '"' || next == '\\') {
                        i++;
                    }
                }
                continue;
            }

            if (ch == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                continue;
            }

            if (ch == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                continue;
            }

            if (!inSingleQuotes && !inDoubleQuotes) {
                if (ch == '2' && i + 1 < inputString.length() && inputString.charAt(i + 1) == '>') {
                    if (i + 2 < inputString.length() && inputString.charAt(i + 2) == '>') {
                        return new SplitResult(inputString.substring(0, i),
                                               inputString.substring(i + 3),
                                               RedirectType.STDERR_APPEND);
                    }
                    return new SplitResult(inputString.substring(0, i),
                                           inputString.substring(i + 2),
                                           RedirectType.STDERR);
                }
                if (ch == '1' && i + 1 < inputString.length() && inputString.charAt(i + 1) == '>') {
                    if (i + 2 < inputString.length() && inputString.charAt(i + 2) == '>') {
                        return new SplitResult(inputString.substring(0, i),
                                               inputString.substring(i + 3),
                                               RedirectType.STDOUT_APPEND);
                    }
                    return new SplitResult(inputString.substring(0, i),
                                           inputString.substring(i + 2),
                                           RedirectType.STDOUT);
                }
                if (ch == '>') {
                    if (i + 1 < inputString.length() && inputString.charAt(i + 1) == '>') {
                        return new SplitResult(inputString.substring(0, i),
                                               inputString.substring(i + 2),
                                               RedirectType.STDOUT_APPEND);
                    }
                    return new SplitResult(inputString.substring(0, i),
                                           inputString.substring(i + 1),
                                           RedirectType.STDOUT);
                }
            }
        }

        return new SplitResult(inputString, null, null);
    }

    private static List<String> tokenize(String inputString) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean tokenStarted = false;

        for (int i = 0; i < inputString.length(); i++) {
            char ch = inputString.charAt(i);

            // Outside quotes, backslash escapes the next character (including whitespace and quotes).
            if (!inSingleQuotes && !inDoubleQuotes && ch == '\\') {
                if (i + 1 < inputString.length()) {
                    current.append(inputString.charAt(i + 1));
                    tokenStarted = true;
                    i++;
                } else {
                    current.append(ch);
                    tokenStarted = true;
                }
                continue;
            }

            if (inDoubleQuotes && ch == '\\') {
                if (i + 1 < inputString.length()) {
                    char next = inputString.charAt(i + 1);
                    if (next == '"' || next == '\\') {
                        current.append(next);
                        tokenStarted = true;
                        i++;
                        continue;
                    }
                }
                current.append(ch);
                tokenStarted = true;
                continue;
            }

            if (ch == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                tokenStarted = true;
                continue;
            }

            if (ch == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                tokenStarted = true;
                continue;
            }

            if (!inSingleQuotes && !inDoubleQuotes && Character.isWhitespace(ch)) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
                continue;
            }

            current.append(ch);
            tokenStarted = true;
        }

        if (tokenStarted) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    public static void eval(ParsedLine parsed) {
        if (parsed == null || parsed.command == null) {
            return;
        }

        if (parsed.redirectPart == null || parsed.redirectPart.isBlank()) {
            runCommand(parsed.command);
            return;
        }

        List<String> redirectTokens = tokenize(parsed.redirectPart);
        if (redirectTokens.isEmpty()) {
            runCommand(parsed.command);
            return;
        }

        Command rightCommand = parseTokens(redirectTokens);
        if ((parsed.redirectType == RedirectType.STDOUT
                || parsed.redirectType == RedirectType.STDERR)
                && isRunnableCommand(rightCommand)) {
            CCRunnable runner = resolveRunner(parsed.command);
            if (runner == null) {
                System.out.println(parsed.command.getName() + ": command not found");
                return;
            }
            try {
                if (parsed.redirectType == RedirectType.STDERR) {
                    runner.stderr(parsed.command, rightCommand);
                } else {
                    runner.stdout(parsed.command, rightCommand);
                }
            } catch (RuntimeException e) {
                reportRunError(parsed.command, e);
                return;
            }
            runCommand(rightCommand);
            return;
        }

        CCRunnable runner = resolveRunner(parsed.command);
        if (runner == null) {
            System.out.println(parsed.command.getName() + ": command not found");
            return;
        }
        Command sink = new Command();
        try {
            if (parsed.redirectType == RedirectType.STDERR
                    || parsed.redirectType == RedirectType.STDERR_APPEND) {
                runner.stderr(parsed.command, sink);
            } else {
                runner.stdout(parsed.command, sink);
            }
            writeRedirectOutput(parsed.command,
                    redirectTokens.get(0),
                    sink.getArgString(),
                    parsed.redirectType == RedirectType.STDOUT_APPEND
                            || parsed.redirectType == RedirectType.STDERR_APPEND);
        } catch (RuntimeException e) {
            reportRunError(parsed.command, e);
        }
    }

    private static Command parseTokens(List<String> tokens) {
        String commandName = tokens.isEmpty() ? null : tokens.get(0);
        List<String> argList = tokens.size() > 1
                ? new ArrayList<>(tokens.subList(1, tokens.size()))
                : new ArrayList<>();

        String home = System.getenv("HOME");
        if (home != null && !home.isBlank()) {
            for (int i = 0; i < argList.size(); i++) {
                String arg = argList.get(i);
                if (arg != null && arg.startsWith("~")) {
                    argList.set(i, home + arg.substring(1));
                }
            }
        }

        String commandArgs = String.join(" ", argList);

        Command command = Command.build(commandName, commandArgs);
        command.setArgList(argList);
        return command;
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
            return builtinMap.get(command.getName());
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

    private static boolean isRunnableCommand(Command command) {
        return command != null && command.getName() != null
                && (command.isBuiltin() || command.isRunable());
    }

    private static void writeRedirectOutput(Command command,
                                            String redirectPath,
                                            String output,
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
            String content = output == null ? "" : output;
            if (append) {
                Files.writeString(path.normalize(), content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(path.normalize(), content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public record ParsedLine(Command command, String redirectPart, RedirectType redirectType) {}

    private record SplitResult(String left, String right, RedirectType type) {}

    private enum RedirectType {
        STDOUT,
        STDERR,
        STDOUT_APPEND,
        STDERR_APPEND
    }
}
