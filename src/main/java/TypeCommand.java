import java.util.List;

public class TypeCommand implements CCRunnable {
    private static TypeCommand instance;

    private TypeCommand() {
    }

    public static TypeCommand getInstance() {
        if (instance == null) {
            instance = new TypeCommand();
        }
        return instance;
    }

    @Override
    public void run(Command cmd) {
        List<String> args = cmd.getArgList();
        if (args == null || args.isEmpty()) {
            return;
        }

        for (String arg : args) {
            Command typeCommand = Command.build(arg, "");
            if (typeCommand.isBuiltin()) {
                System.out.println(arg + " is a shell builtin");
            } else if (typeCommand.isRunable() && typeCommand.getPath() != null) {
                System.out.println(arg + " is " + typeCommand.getPath());
            } else {
                System.out.println(arg + ": not found");
            }
        }
    }
}
