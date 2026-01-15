public class ExitCommand implements CCRunnable {
    private static ExitCommand instance;

    private ExitCommand() {
    }

    public static ExitCommand getInstance() {
        if (instance == null) {
            instance = new ExitCommand();
        }
        return instance;
    }

    @Override
    public void run(Command cmd) {
        System.exit(0);
    }
}
