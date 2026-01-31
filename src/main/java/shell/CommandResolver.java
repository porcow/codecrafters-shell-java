package shell;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class CommandResolver {
    private static final Map<String, CommandRunner> BUILTIN_MAP = new HashMap<>() {{
        put("echo", EchoCommand.getInstance());
        put("exit", ExitCommand.getInstance());
        put("type", TypeCommand.getInstance());
        put("pwd", PwdCommand.getInstance());
        put("cd", CdCommand.getInstance());
        put("history", HistoryCommand.getInstance());
    }};

    private CommandResolver() {
    }

    public static Map<String, CommandRunner> getBuiltinMap() {
        return BUILTIN_MAP;
    }

    public static boolean isBuiltin(String name) {
        return name != null && BUILTIN_MAP.containsKey(name);
    }

    public static String findExecutable(String name) {
        return findExecutable(null, name);
    }

    public static String findExecutable(ShellContext context, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        File direct = new File(name);
        if (direct.isAbsolute() && direct.isFile() && direct.canExecute()) {
            return direct.getAbsolutePath();
        }

        String pathEnv = context != null ? context.getEnv("PATH") : System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return null;
        }

        return findExecutableInPath(name, pathEnv);
    }

    private static String findExecutableInPath(String name, String pathEnv) {
        String[] dirs = pathEnv.split(File.pathSeparator);
        for (String dir : dirs) {
            String dirPath = dir.isEmpty() ? "." : dir;
            File candidate = new File(dirPath, name);
            if (candidate.isFile() && candidate.canExecute()) {
                return candidate.getAbsolutePath();
            }
        }
        return null;
    }
}
