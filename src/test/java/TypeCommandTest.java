import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TypeCommandTest {
    @Test
    void run_reportsBuiltinAndMissing() {
        Command command = Command.build("type", "");
        String missing = "definitely-not-a-command-12345";
        command.setArgList(List.of("pwd", missing));

        String output = TestUtils.captureStdout(() -> TypeCommand.getInstance().run(command));

        String expected = String.join(System.lineSeparator(),
                "pwd is a shell builtin",
                missing + ": not found",
                "");
        assertEquals(expected, output);
    }
}
