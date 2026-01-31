package shell;

import java.util.ArrayList;
import java.util.List;

public class Command {
    private boolean runable;
    private String name;
    private boolean builtin;
    private String path;
    private String argString;
    private List<String> argList;
    private String workspace;
    private ShellContext context;

    public Command() {
        this.argList = new ArrayList<>();
    }

    public Command(boolean runable,
                   String name,
                   boolean builtin,
                   String path,
                   String argString,
                   String workspace,
                   ShellContext context) {
        this.runable = runable;
        this.name = name;
        this.builtin = builtin;
        this.path = path;
        this.argString = argString;
        this.workspace = workspace;
        this.context = context;
        this.argList = new ArrayList<>();
    }

    public static Command build(String name, String argString) {
        return build(null, name, argString);
    }

    public static Command build(ShellContext context, String name, String argString) {
        Command command = new Command();
        command.name = name;
        command.argString = argString;
        command.context = context;
        if (context != null && context.getWorkspace() != null && !context.getWorkspace().isBlank()) {
            command.workspace = context.getWorkspace();
        } else {
            command.workspace = System.getProperty("user.dir");
        }

        if (name == null) {
            return command;
        }

        String execPath = CommandResolver.findExecutable(context, name);
        if (execPath != null) {
            command.path = execPath;
            command.runable = true;
        } else {
            command.runable = false;
        }

        if (CommandResolver.isBuiltin(name)) {
            command.builtin = true;
            command.runable = true;
            return command;
        }

        return command;
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

    public List<String> toCommandLine() {
        List<String> commandLine = new ArrayList<>();
        String execName = name;
        String execPath = path;
        if (execName == null || execName.isBlank()) {
            commandLine.add(execPath);
        } else {
            commandLine.add(execName);
        }
        if (argList != null && !argList.isEmpty()) {
            commandLine.addAll(argList);
        }
        return commandLine;
    }

    public String getWorkspace() {
        if (workspace != null && !workspace.isBlank()) {
            return workspace;
        }
        if (context != null) {
            return context.getWorkspace();
        }
        return System.getProperty("user.dir");
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public ShellContext getContext() {
        return context;
    }

    public void setContext(ShellContext context) {
        this.context = context;
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
