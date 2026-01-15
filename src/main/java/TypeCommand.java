public class TypeCommand implements Runnable {
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
        String typeArgs = cmd.getArgString().trim();
        if (!typeArgs.isBlank()) {
            String[] args = typeArgs.split("\\s+");
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
}
