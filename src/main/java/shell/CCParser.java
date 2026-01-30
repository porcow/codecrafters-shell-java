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
        return parseTokens(context, extractWords(tokenizeWithOperators(inputString)));
    }

    public static List<String> parseArguments(String inputString) {
        return extractWords(tokenizeWithOperators(inputString));
    }

    public static ParsedLine parseLine(String inputString) {
        return parseLine(null, inputString);
    }

    public static ParsedLine parseLine(ShellContext context, String inputString) {
        List<Token> tokens = tokenizeWithOperators(inputString);
        RedirectType redirectType = null;
        List<String> commandTokens = new ArrayList<>();
        List<String> redirectTokens = null;
        boolean afterRedirect = false;

        for (Token token : tokens) {
            if (!afterRedirect && isRedirect(token.type)) {
                redirectType = toRedirectType(token.type);
                afterRedirect = true;
                redirectTokens = new ArrayList<>();
                continue;
            }

            if (token.type == TokenType.PIPE) {
                if (afterRedirect) {
                    break;
                }
                continue;
            }

            if (token.type == TokenType.WORD) {
                if (afterRedirect) {
                    redirectTokens.add(token.text);
                } else {
                    commandTokens.add(token.text);
                }
            }
        }

        Command command = parseTokens(context, commandTokens);
        if (redirectType == null) {
            return new ParsedLine(command, null, null);
        }
        return new ParsedLine(command, redirectTokens, redirectType);
    }

    public static List<List<String>> splitPipelineTokens(String inputString) {
        List<List<String>> parts = new ArrayList<>();
        if (inputString == null || inputString.isEmpty()) {
            parts.add(new ArrayList<>());
            return parts;
        }

        List<Token> tokens = tokenizeWithOperators(inputString);
        List<String> current = new ArrayList<>();
        for (Token token : tokens) {
            if (token.type == TokenType.PIPE) {
                parts.add(current);
                current = new ArrayList<>();
                continue;
            }
            if (token.type == TokenType.WORD) {
                current.add(token.text);
            }
        }
        parts.add(current);
        return parts;
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

    public record ParsedLine(Command command, List<String> redirectTokens, RedirectType redirectType) {}

    public enum RedirectType {
        STDOUT,
        STDERR,
        STDOUT_APPEND,
        STDERR_APPEND
    }

    private enum TokenType {
        WORD,
        PIPE,
        REDIRECT_STDOUT,
        REDIRECT_STDOUT_APPEND,
        REDIRECT_STDERR,
        REDIRECT_STDERR_APPEND
    }

    private record Token(TokenType type, String text) {}

    private static boolean isRedirect(TokenType type) {
        return type == TokenType.REDIRECT_STDOUT
                || type == TokenType.REDIRECT_STDOUT_APPEND
                || type == TokenType.REDIRECT_STDERR
                || type == TokenType.REDIRECT_STDERR_APPEND;
    }

    private static RedirectType toRedirectType(TokenType type) {
        return switch (type) {
            case REDIRECT_STDOUT -> RedirectType.STDOUT;
            case REDIRECT_STDOUT_APPEND -> RedirectType.STDOUT_APPEND;
            case REDIRECT_STDERR -> RedirectType.STDERR;
            case REDIRECT_STDERR_APPEND -> RedirectType.STDERR_APPEND;
            default -> null;
        };
    }

    private static List<String> extractWords(List<Token> tokens) {
        List<String> words = new ArrayList<>();
        for (Token token : tokens) {
            if (token.type == TokenType.WORD) {
                words.add(token.text);
            }
        }
        return words;
    }

    private static List<Token> tokenizeWithOperators(String inputString) {
        List<Token> tokens = new ArrayList<>();
        if (inputString == null || inputString.isEmpty()) {
            return tokens;
        }

        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean tokenStarted = false;

        for (int i = 0; i < inputString.length(); i++) {
            char ch = inputString.charAt(i);

            if (!inSingleQuotes && !inDoubleQuotes) {
                if (ch == '\\') {
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

                if (ch == '|') {
                    if (tokenStarted) {
                        tokens.add(new Token(TokenType.WORD, current.toString()));
                        current.setLength(0);
                        tokenStarted = false;
                    }
                    tokens.add(new Token(TokenType.PIPE, null));
                    continue;
                }

                if ((ch == '1' || ch == '2')
                        && i + 1 < inputString.length()
                        && inputString.charAt(i + 1) == '>') {
                    if (tokenStarted) {
                        tokens.add(new Token(TokenType.WORD, current.toString()));
                        current.setLength(0);
                        tokenStarted = false;
                    }
                    boolean append = i + 2 < inputString.length() && inputString.charAt(i + 2) == '>';
                    if (ch == '1') {
                        tokens.add(new Token(append
                                ? TokenType.REDIRECT_STDOUT_APPEND
                                : TokenType.REDIRECT_STDOUT, null));
                    } else {
                        tokens.add(new Token(append
                                ? TokenType.REDIRECT_STDERR_APPEND
                                : TokenType.REDIRECT_STDERR, null));
                    }
                    i += append ? 2 : 1;
                    continue;
                }

                if (ch == '>') {
                    if (tokenStarted) {
                        tokens.add(new Token(TokenType.WORD, current.toString()));
                        current.setLength(0);
                        tokenStarted = false;
                    }
                    boolean append = i + 1 < inputString.length() && inputString.charAt(i + 1) == '>';
                    tokens.add(new Token(append
                            ? TokenType.REDIRECT_STDOUT_APPEND
                            : TokenType.REDIRECT_STDOUT, null));
                    if (append) {
                        i++;
                    }
                    continue;
                }
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
                    tokens.add(new Token(TokenType.WORD, current.toString()));
                    current.setLength(0);
                    tokenStarted = false;
                }
                continue;
            }

            current.append(ch);
            tokenStarted = true;
        }

        if (tokenStarted) {
            tokens.add(new Token(TokenType.WORD, current.toString()));
        }

        return tokens;
    }
}
