import java.util.Scanner;
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Main {

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
            Command cmd = parse(input);
            eval(cmd);
        }
    }

    public static String read() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    public static Command parse(String inputString) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean tokenStarted = false;

        for (int i = 0; i < inputString.length(); i++) {
            char ch = inputString.charAt(i);

            if (ch == '\'') {
                inSingleQuotes = !inSingleQuotes;
                tokenStarted = true;
                continue;
            }

            if (!inSingleQuotes && Character.isWhitespace(ch)) {
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

        String commandName = tokens.isEmpty() ? null : tokens.get(0);
        List<String> argList = tokens.size() > 1
                ? new ArrayList<>(tokens.subList(1, tokens.size()))
                : new ArrayList<>();
        String commandArgs = String.join(" ", argList);

        Command command = Command.build(commandName, commandArgs);
        command.setArgList(argList);
        return command;
    }

    public static void eval(Command command) {
        if (command.isBuiltin()) {
            CCRunnable builtin = builtinMap.get(command.getName());
            if (builtin != null) {
                builtin.run(command);
                return;
            }
        } else if (command.isRunable()) {
            List<String> cmd = new ArrayList<>();
            cmd.add(command.getName());

            List<String> args = command.getArgList();
            if (args != null && !args.isEmpty()) {
                cmd.addAll(args);
            }

            try {
                Process process = new ProcessBuilder(cmd)
                        .directory(new File(command.getWorkspace()))
                        .redirectErrorStream(true)
                        .start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                }
                process.waitFor();
            } catch (IOException e) {
                System.out.println(command.getName() + ": " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            System.out.println(command.getName() + ": command not found");
        }
    }
}
