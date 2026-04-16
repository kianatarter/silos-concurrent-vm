import java.util.*;


/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Neveah Martinez
 *
 *
 */
public class Parser {


    public Parser() {

    }

    /**
     * Main method to parse the input files
     */
    public static Silo[][] parse(Scanner scanner){
        String title = scanner.nextLine().trim();
        String description = scanner.nextLine().trim();

        while (scanner.hasNextLine()){
            String line = scanner.nextLine().trim();

            if (line.equals("GRID")){
                break;
            }
        }

        int rows = scanner.nextInt();
        int cols = scanner.nextInt();
        Silo[][] grid = new Silo[rows][cols];

        scanner.nextLine();

        for (int r = 0; r < rows; r++){
            for (int c = 0; c < cols; c++){
                List<Instruction> instructions = new ArrayList<>();

                while (scanner.hasNextLine()){

                    String line = scanner.nextLine().trim();
                    String[] words = line.split("\\s+");

                    // move on to next silo
                    if (line.equals("END")){
                        break;
                    }

                    // skip empty lines
                    if (line.isEmpty()){
                        continue;
                    }

                    String type;
                    String arg1 = null;
                    String arg2 = null;

                    if (words.length == 1){
                        type = words[0];
                    } else if (words.length == 2){
                        type = words[0];
                        arg1 = words[1];
                    } else {
                        type = words[0];
                        arg1 = words[1];
                        arg2 = words[2];
                    }

                    Instruction instruction = new Instruction(type, arg1, arg2);

                    // add instruction to map with correct position
                    instructions.add(instruction);
                }
                Silo silo = new Silo(instructions);
                grid[r][c] = silo;
            }
        }
        scanner.nextLine();

        return grid;
    }

}
