public class PwdCommand implements Runnable {
    private static PwdCommand instance;

    private PwdCommand() {
    }

    public static PwdCommand getInstance() {
        if (instance == null) {
            instance = new PwdCommand();
        }
        return instance;
    }

    @Override
    public void run(Command cmd) {
        String workspace = cmd.getWorkspace();
        if (workspace == null || workspace.isBlank()) {
            workspace = System.getProperty("user.dir");
        }
        System.out.println(workspace);
    }
}
