import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Command {
    private static final Map<String, CCRunnable> BUILTIN_MAP = new HashMap<>() {{
        put("echo", EchoCommand.getInstance());
        put("exit", ExitCommand.getInstance());
        put("type", TypeCommand.getInstance());
        put("pwd", PwdCommand.getInstance());
        put("cd", CdCommand.getInstance());
        put("history", HistoryCommand.getInstance());
    }};

    private boolean runable;
    private String name;
    private boolean builtin;
    private String path;
    private String argString;
    private List<String> argList;
    private String workspace;
    static String currentWorkspace = System.getProperty("user.dir");

    public Command() {
        this.argList = new ArrayList<>();
    }

    public Command(boolean runable,
                   String name,
                   boolean builtin,
                   String path,
                   String argString,
                   String workspace) {
        this.runable = runable;
        this.name = name;
        this.builtin = builtin;
        this.path = path;
        this.argString = argString;
        this.workspace = workspace;
        this.argList = new ArrayList<>();
    }

    public static Command build(String name, String argString) {
        Command command = new Command();
        command.name = name;
        command.argString = argString;
        command.workspace = currentWorkspace;

        if (name == null) {
            return command;
        }

        String execPath = findExecutable(name);
        if (execPath != null) {
            command.path = execPath;
            command.runable = true;
        } else {
            command.runable = false;
        }

        if (isBuiltin(name)) {
            command.builtin = true;
            command.runable = true;
            return command;
        }

        return command;
    }

    public static Map<String, CCRunnable> getBuiltinMap() {
        return BUILTIN_MAP;
    }

    private static boolean isBuiltin(String name) {
        return BUILTIN_MAP.containsKey(name);
    }

    public static String findExecutable(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        File direct = new File(name);
        if (direct.isAbsolute() && direct.isFile() && direct.canExecute()) {
            return direct.getAbsolutePath();
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return null;
        }

        String found = findExecutableInPath(name, pathEnv);
        if (found != null) {
            return found;
        }

        return null;
    }

    private static String findExecutableInPath(String name, String pathEnv) {
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

    public boolean isRunable() {
        return runable;
    }

    public String getName() {
        return name;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getArgString() {
        return argString;
    }

    public void setArgString(String argString) {
        this.argString = argString;
    }

    public List<String> getArgList() {
        return argList;
    }

    public void setArgList(List<String> argList) {
        this.argList = argList;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public static String getCurrentWorkspace() {
        return currentWorkspace;
    }

    public static void setCurrentWorkspace(String workspace) {
        if (workspace != null && !workspace.isBlank()) {
            currentWorkspace = workspace;
        }
    }

    @Override
    public String toString() {
        return "Command{" +
                "runable=" + runable +
                ", name='" + name + '\'' +
                ", builtin=" + builtin +
                ", path='" + path + '\'' +
                '}';
    }
}
