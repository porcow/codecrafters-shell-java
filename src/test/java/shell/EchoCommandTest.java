package shell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class EchoCommandTest {
    @Test
    void run_printsJoinedArgs() {
        Command command = Command.build("echo", "");
        command.setArgList(List.of("hello", "world"));

        String output = TestUtils.captureStdout(() -> EchoCommand.getInstance().run(command));

        assertEquals("hello world" + System.lineSeparator(), output);
    }

    @Test
    void run_printsEmptyLineWhenNoArgs() {
        Command command = Command.build("echo", "");
        command.setArgList(List.of());

        String output = TestUtils.captureStdout(() -> EchoCommand.getInstance().run(command));

        assertEquals(System.lineSeparator(), output);
    }

    @Test
    void run_preservesWhitespaceInsideArgument() {
        Command command = Command.build("echo", "");
        command.setArgList(List.of("a  b"));

        String output = TestUtils.captureStdout(() -> EchoCommand.getInstance().run(command));

        assertEquals("a  b" + System.lineSeparator(), output);
    }
}
