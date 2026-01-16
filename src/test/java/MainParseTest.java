import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class MainParseTest {
    @Test
    void parse_preservesWhitespaceInsideDoubleQuotes() {
        Command command = Main.parse("echo \"hello    world\"");

        assertEquals("echo", command.getName());
        assertEquals(List.of("hello    world"), command.getArgList());
    }

    @Test
    void parse_concatenatesAdjacentDoubleQuotedStrings() {
        Command command = Main.parse("echo \"hello\"\"world\"");

        assertEquals(List.of("helloworld"), command.getArgList());
    }

    @Test
    void parse_splitsOnWhitespaceOutsideDoubleQuotes() {
        Command command = Main.parse("echo \"hello\" \"world\"");

        assertEquals(List.of("hello", "world"), command.getArgList());
    }

    @Test
    void parse_treatsSingleQuotesLiterallyInsideDoubleQuotes() {
        Command command = Main.parse("echo \"shell's test\"");

        assertEquals(List.of("shell's test"), command.getArgList());
    }
}
