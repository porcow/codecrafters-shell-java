import java.io.File;
import java.util.Scanner;

public class Main {
    private static final String[] BUILTINS = {"exit", "echo", "type"};

    public static void main(String[] args) throws Exception {

        while (true) {
            // TODO: Uncomment the code below to pass the first stage
            System.out.print("$ ");
            String input = read();
            eval(input);
        }
    }

    public static String read() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    private static boolean isBuiltin(String name) {
        for (String builtin : BUILTINS) {
            if (builtin.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static String findExecutable(String name) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return null;
        }

        String[] dirs = pathEnv.split(":");
        for (String dir : dirs) {
            String dirPath = dir.isEmpty() ? "." : dir;
            File candidate = new File(dirPath, name);
            if (candidate.isFile() && candidate.canExecute()) {
                return candidate.getAbsolutePath();
            }
        }

        return null;
    }

    public static void eval(String inputString) {
        String trimmed = inputString.trim();

        // Implement exit
        if (trimmed.equals("exit")) {
            System.exit(0);
        }

        // Implement echo
        if (trimmed.startsWith("echo ")) {
            String echoArgs = trimmed.substring(5);
            System.out.println(echoArgs);
            return;
        }

        // Implement type
        if (trimmed.equals("type")) {
            return;
        }

        if (trimmed.startsWith("type ")) {
            String typeArgs = trimmed.substring(5).trim();
            if (!typeArgs.isBlank()) {
                String[] args = typeArgs.split("\s+");
                for (String arg : args) {
                    if (isBuiltin(arg)) {
                        System.out.println(arg + " is a shell builtin");
                        continue;
                    }

                    String execPath = findExecutable(arg);
                    if (execPath != null) {
                        System.out.println(arg + " is " + execPath);
                    } else {p
                        System.out.println(arg + ": not found");
                    }
                }
            }
            return;
        }
        System.out.println(inputString + ": command not found");
    }
}
