import java.util.Scanner;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
            List<Command> commands = parsePipeline(input);
            eval(commands);
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

    public static List<Command> parsePipeline(String inputString) {
        List<String> parts = splitRedirections(inputString);
        List<Command> commands = new ArrayList<>();
        for (String part : parts) {
            Command command = parse(part);
            if (command.getName() != null && !command.getName().isBlank()) {
                commands.add(command);
            }
        }
        return commands;
    }

    private static List<String> splitRedirections(String inputString) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < inputString.length(); i++) {
            char ch = inputString.charAt(i);

            // Only split on > or 1> when we're not inside quotes or escapes.
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
                    current.append(ch);
                    if (next == '"' || next == '\\') {
                        current.append(next);
                        i++;
                        continue;
                    }
                } else {
                    current.append(ch);
                }
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

            if (!inSingleQuotes && !inDoubleQuotes) {
                if (ch == '1' && i + 1 < inputString.length() && inputString.charAt(i + 1) == '>') {
                    parts.add(current.toString());
                    current.setLength(0);
                    i++;
                    continue;
                }
                if (ch == '>') {
                    parts.add(current.toString());
                    current.setLength(0);
                    continue;
                }
            }

            current.append(ch);
        }

        parts.add(current.toString());
        return parts;
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

    public static void eval(List<Command> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        for (int i = 0; i < commands.size() - 1; i++) {
            Command current = commands.get(i);
            Command next = commands.get(i + 1);
            CCRunnable runner = resolveRunner(current);
            if (runner == null) {
                System.out.println(current.getName() + ": command not found");
                return;
            }
            try {
                runner.stdout(current, next);
            } catch (RuntimeException e) {
                reportRunError(current, e);
                return;
            }
        }

        runCommand(commands.get(commands.size() - 1));
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
}
