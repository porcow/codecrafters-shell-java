import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TestUtils {
    private TestUtils() {
    }

    public static String captureStdout(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        System.setOut(capture);
        try {
            action.run();
        } finally {
            System.out.flush();
            System.setOut(originalOut);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    public static Command commandWithArgs(String name, String... args) {
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        Command command = Command.build(name, String.join(" ", argList));
        command.setArgList(argList);
        return command;
    }
}
