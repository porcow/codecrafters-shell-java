package shell;

import java.util.ArrayList;
import java.util.List;

public final class CCParser {
    private CCParser() {
    }

    public static Command parse(String inputString) {
        return parse(null, inputString);
    }

    public static Command parse(ShellContext context, String inputString) {
        return parseTokens(context, tokenize(inputString));
    }

    public static List<String> parseArguments(String inputString) {
        return tokenize(inputString);
    }

    public static ParsedLine parseLine(String inputString) {
        return parseLine(null, inputString);
    }

    public static ParsedLine parseLine(ShellContext context, String inputString) {
        SplitResult split = splitRedirection(inputString);
        Command command = parse(context, split.left);
        return new ParsedLine(command, split.right, split.type);
    }

    public static List<String> splitPipeline(String inputString) {
        List<String> parts = new ArrayList<>();
        if (inputString == null || inputString.isEmpty()) {
            parts.add(inputString);
            return parts;
        }

        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < inputString.length(); i++) {
            char ch = inputString.charAt(i);

            if (!inSingleQuotes && !inDoubleQuotes && ch == '\\') {
                if (i + 1 < inputString.length()) {
                    current.append(ch);
                    current.append(inputString.charAt(i + 1));
                    i++;
                } else {
                    current.append(ch);
                }
                continue;
            }

            if (inDoubleQuotes && ch == '\\') {
                if (i + 1 < inputString.length()) {
                    char next = inputString.charAt(i + 1);
                    if (next == '"' || next == '\\') {
                        current.append(ch);
                        current.append(next);
                        i++;
                        continue;
                    }
                }
                current.append(ch);
                continue;
            }

            if (ch == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                current.append(ch);
                continue;
            }

            if (ch == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                current.append(ch);
                continue;
            }

            if (!inSingleQuotes && !inDoubleQuotes && ch == '|') {
                parts.add(current.toString().trim());
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        parts.add(current.toString().trim());
        return parts;
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

    static Command parseTokens(ShellContext context, List<String> tokens) {
        String commandName = tokens.isEmpty() ? null : tokens.get(0);
        List<String> argList = tokens.size() > 1
                ? new ArrayList<>(tokens.subList(1, tokens.size()))
                : new ArrayList<>();

        String home = null;
        if (context != null) {
            home = context.getEnv("HOME");
        }
        if (home == null || home.isBlank()) {
            home = System.getenv("HOME");
        }
        if (home != null && !home.isBlank()) {
            for (int i = 0; i < argList.size(); i++) {
                String arg = argList.get(i);
                if (arg != null && arg.startsWith("~")) {
                    argList.set(i, home + arg.substring(1));
                }
            }
        }

        String commandArgs = String.join(" ", argList);

        Command command = Command.build(context, commandName, commandArgs);
        command.setArgList(argList);
        return command;
    }

    public record ParsedLine(Command command, String redirectPart, RedirectType redirectType) {}

    private record SplitResult(String left, String right, RedirectType type) {}

    public enum RedirectType {
        STDOUT,
        STDERR,
        STDOUT_APPEND,
        STDERR_APPEND
    }
}
