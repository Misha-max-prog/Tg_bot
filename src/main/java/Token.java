import java.io.*;
import java.util.Scanner;

public class Token {
    public static String ReadToken() {
        File tokenFile = new File("token.txt");
        try {
            Scanner scanner = new Scanner(tokenFile);
            String name = scanner.nextLine();
            scanner.close();
            return name;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
