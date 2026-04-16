import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Neveah Martinez
 *
 *
 */
public class Main {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(new File("test_input/simple_example1.txt"));
            Parser.parse(scanner);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }


    }

}
