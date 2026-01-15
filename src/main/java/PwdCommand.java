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
        System.out.println(Command.currentWorkspace);
    }
}
