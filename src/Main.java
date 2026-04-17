import Logic.*;
import Parser.Parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
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
            //Scanner scanner = new Scanner(new File("test_input/simple_example1.txt"));
            Scanner scanner = new Scanner(new File("test_input/test_input.txt"));
            Puzzle puzzle = Parser.parse(scanner);

            // Testing
            Silo silo = puzzle.grid[0][0];
            Interpreter interp = new Interpreter(silo);
            List<Instruction> instructions = silo.getInstructions();

            System.out.println("Starting: ACC=" + silo.getACC() + " BAK=" + silo.getBAK());

            for (int i = 0; i < instructions.size(); i++) {
                Instruction inst = instructions.get(silo.getPC());
                System.out.println("Executing: " + inst);
                interp.executeOneInstruction(inst);
                System.out.println("After: ACC=" + silo.getACC() + " BAK=" + silo.getBAK() + " PC=" + silo.getPC());
            }


            System.out.println("=== PUZZLE PARSED ===");
            System.out.println("Title: " + puzzle.title);
            System.out.println("Description: " + puzzle.description);
            System.out.println("Grid: " + puzzle.grid.length + "x" + puzzle.grid[0].length);

            System.out.println("\n=== ALL SILOS ===");
            for (int r = 0; r < puzzle.grid.length; r++) {
                for (int c = 0; c < puzzle.grid[r].length; c++) {
                    System.out.println("Logic.Silo (" + r + "," + c + "): " + puzzle.grid[r][c]);

                    // Print the actual instructions
                    List<Instruction> program = puzzle.grid[r][c].getInstructions();
                    for (Instruction inst : program) {
                        System.out.println("  " + inst);
                    }
                }
            }
            System.out.println("\n=== INPUT STREAMS ===");
            for (String name : puzzle.inputs.keySet()) {
                Stream stream = puzzle.inputs.get(name);
                System.out.println(name + " at (" + stream.row() + "," + stream.col() +
                        ") with " + stream.values().size() + " values");
            }

            System.out.println("\n=== OUTPUT STREAMS ===");
            for (String name : puzzle.outputs.keySet()) {
                Stream stream = puzzle.outputs.get(name);
                System.out.println(name + " at (" + stream.row() + "," + stream.col() +
                        ") expecting " + stream.values().size() + " values");
            }


            scanner.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

}
