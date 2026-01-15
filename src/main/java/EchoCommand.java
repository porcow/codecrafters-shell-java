public class EchoCommand implements CCRunnable {
    private static EchoCommand instance;

    private EchoCommand() {
    }

    public static EchoCommand getInstance() {
        if (instance == null) {
            instance = new EchoCommand();
        }
        return instance;
    }

    @Override
    public void run(Command cmd) {
        if (cmd.getArgList() == null) {
            System.out.println();
            return;
        }

        System.out.println(String.join(" ", cmd.getArgList()));
    }
}
