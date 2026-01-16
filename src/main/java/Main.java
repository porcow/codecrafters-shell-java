import java.util.Scanner;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Main {

    private static final Scanner SCANNER = new Scanner(System.in);
    final static Map<String, CCRunnable> builtinMap = new HashMap<String, CCRunnable>() {{
            put("echo", EchoCommand.getInstance());
            put("exit", ExitCommand.getInstance());
            put("type", TypeCommand.getInstance());
            put("pwd", PwdCommand.getInstance());
            put("cd", CdCommand.getInstance());
        }};

    public static void main(String[] args) throws Exception {

        while (true) {
            System.out.print("$ ");
            String input = read();
            ParsedLine parsed = parseLine(input);
            eval(parsed);
        }
    }

    public static String read() {
        return SCANNER.nextLine();
    }

    public static Command parse(String inputString) {
        List<String> tokens = tokenize(inputString);

        String commandName = tokens.isEmpty() ? null : tokens.get(0);
        List<String> argList = tokens.size() > 1
                ? new ArrayList<>(tokens.subList(1, tokens.size()))
                : new ArrayList<>();
        String commandArgs = String.join(" ", argList);

        Command command = Command.build(commandName, commandArgs);
        command.setArgList(argList);
        return command;
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

            // Only split on >, 1>, 2>, >>, or 1>> when we're not inside quotes or escapes.
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

        Command rightCommand = parse(parsed.redirectPart);
        if (parsed.redirectType != RedirectType.STDOUT_APPEND && isRunnableCommand(rightCommand)) {
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
            if (parsed.redirectType == RedirectType.STDERR) {
                runner.stderr(parsed.command, sink);
            } else {
                runner.stdout(parsed.command, sink);
            }
            writeRedirectOutput(parsed.command,
                    redirectTokens.get(0),
                    sink.getArgString(),
                    parsed.redirectType == RedirectType.STDOUT_APPEND);
        } catch (RuntimeException e) {
            reportRunError(parsed.command, e);
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
                    base = System.getenv("HOME");
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
        STDOUT_APPEND
    }
}
