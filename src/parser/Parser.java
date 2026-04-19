package parser;

import logic.Instruction;
import logic.Puzzle;
import logic.Silo;
import logic.Stream;

import java.util.*;


/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Neveah Martinez
 *
 *
 */
public class Parser {


    /**
     * Main method used to parse test files
     *
     * @param scanner scanner to get data from file
     * @return a Logic.Puzzle object with silo program and data, and input/output streams
     */
    public static Puzzle parse(Scanner scanner){
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
        // close grid parsing
        scanner.nextLine();

        // create maps for stream title + stream data
        Map<String, Stream> inputMap = new HashMap<>();
        Map<String, Stream> outputMap = new HashMap<>();




        while (scanner.hasNextLine()){
            String next = scanner.nextLine().trim();

            // skip empty lines
            if (next.isEmpty()){
                continue;
            }

            // move onto building output stream
            if (next.equals("END")){
                break;
            }



            if (next.startsWith("INPUT")){
                String inputTitle = next;
                int row = scanner.nextInt();
                int col = scanner.nextInt();
                scanner.nextLine();

                List<Integer> inputValues = new ArrayList<>();
                while(scanner.hasNextLine()){
                    String value = scanner.nextLine().trim();
                    if (value.equals("END")){
                        break; // end this stream
                    }

                    inputValues.add(Integer.parseInt(value));
                }
                Stream inputStream = new Stream(inputTitle, row, col, inputValues);
                inputMap.put(inputTitle, inputStream);


            } else if (next.startsWith("OUTPUT")){
                String outputTitle = next;
                int row = scanner.nextInt();
                int col = scanner.nextInt();
                scanner.nextLine();

                List<Integer> outputValues = new ArrayList<>();
                while(scanner.hasNextLine()){
                    String value = scanner.nextLine().trim();
                    if (value.equals("END")){
                        break; // end this stream
                    }
                    outputValues.add(Integer.parseInt(value));
                }
                Stream outputStream = new Stream(outputTitle, row, col, outputValues);
                outputMap.put(outputTitle, outputStream);

            }
        }
        Puzzle puzzle = new Puzzle(title, description, grid, inputMap, outputMap);
        return puzzle;
    }
}


