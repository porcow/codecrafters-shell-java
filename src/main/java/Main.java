import java.util.Scanner;
import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main {

    final static Map<String, Runnable> builtinMap = new HashMap<String, Runnable>() {{
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
        String trimmed = inputString.trim();
        int spaceIndex = trimmed.indexOf(' ');
        String commandName = spaceIndex == -1 ? trimmed : trimmed.substring(0, spaceIndex);
        String commandArgs = spaceIndex == -1 ? "" : trimmed.substring(spaceIndex + 1);

        Command command = Command.build(commandName, commandArgs);
        return command;
    }

    public static void eval(Command command) {
        if (command.isBuiltin()) {
            Runnable builtin = builtinMap.get(command.getName());
            if (builtin != null) {
                builtin.run(command);
                return;
            }
        } else if (command.isRunable()) {
            List<String> cmd = new ArrayList<>();
            cmd.add(command.getName());

            if (!command.getArgString().isBlank()) {
                String[] args = command.getArgString().split("\\s+");
                cmd.addAll(Arrays.asList(args));
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
