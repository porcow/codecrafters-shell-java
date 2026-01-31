package shell;

import java.util.Scanner;
import org.jline.reader.LineReader;

public class Main {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final String PROMPT = "$ ";
    private static final ShellContext CONTEXT = new ShellContext();
    private static final AutoCompleter AUTO_COMPLETER = new AutoCompleter(PROMPT, CONTEXT);
    private static final LineReader LINE_READER = AUTO_COMPLETER.buildLineReader();
    private static final Shell SHELL = new Shell(CONTEXT);
    public static void main(String[] args) throws Exception {
        HistoryCommand.initializeFromEnv(CONTEXT);

        while (true) {
            String input = read();
            SHELL.evalInput(input);
        }
    }

    private static String read() {
        if (LINE_READER != null) {
            return AUTO_COMPLETER.readLine(LINE_READER);
        }

        System.out.print(PROMPT);
        System.out.flush();
        return SCANNER.nextLine();
    }
}
