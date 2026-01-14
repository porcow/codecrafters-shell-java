import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {

        while (true) {
            // TODO: Uncomment the code below to pass the first stage
            System.out.print("$ ");
            String input = read();
            eval(input);
        }
    }

    public static String read() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    public static void eval(String inputString) {
        String trimmed = inputString.trim();
        if (trimmed.equals("exit")) {
            System.exit(0);
        }

        System.out.println(inputString + ": command not found");
    }
}
