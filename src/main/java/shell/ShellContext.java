package shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShellContext {
    private String workspace;
    private final List<String> history;
    private final Map<String, String> env;

    public ShellContext() {
        this(System.getProperty("user.dir"), new ArrayList<>(), System.getenv());
    }

    public ShellContext(String workspace) {
        this(workspace, new ArrayList<>(), System.getenv());
    }

    public ShellContext(String workspace, List<String> history, Map<String, String> env) {
        this.workspace = workspace;
        this.history = history == null ? new ArrayList<>() : history;
        this.env = env == null ? System.getenv() : env;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        if (workspace != null && !workspace.isBlank()) {
            this.workspace = workspace;
        }
    }

    public List<String> getHistory() {
        return history;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public String getEnv(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return env.get(key);
    }
}
