import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Command {
    final static String[] BUILTINS = {"exit", "echo", "type", "pwd", "cd", "history"};

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

    private static boolean isBuiltin(String name) {
        for (String builtin : BUILTINS) {
            if (builtin.equals(name)) {
                return true;
            }
        }
        return false;
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

        String escaped = escapeUnescapedSingleQuotes(name);
        if (!escaped.equals(name)) {
            found = findExecutableInPath(escaped, pathEnv);
            if (found != null) {
                return found;
            }
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

    private static String escapeUnescapedSingleQuotes(String name) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '\'' && (i == 0 || name.charAt(i - 1) != '\\')) {
                escaped.append('\\');
            }
            escaped.append(ch);
        }
        return escaped.toString();
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
