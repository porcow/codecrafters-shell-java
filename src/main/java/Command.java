import java.io.File;

public class Command {
    final static String[] BUILTINS = {"exit", "echo", "type", "pwd", "cd"};

    private boolean runable;
    private String name;
    private boolean builtin;
    private String path;
    private String argString;
    private String workspace;
    static String currentWorkspace = System.getProperty("user.dir");

    public Command() {
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

    public boolean isRunable() {
        return runable;
    }

    public void setRunable(boolean runable) {
        this.runable = runable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public void setBuiltin(boolean builtin) {
        this.builtin = builtin;
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
