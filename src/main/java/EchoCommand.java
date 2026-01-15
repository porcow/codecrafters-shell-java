public class EchoCommand implements Runnable {
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
        System.out.println(cmd.getArgString());
    }
}
