package parser;

import logic.Instruction;
import logic.Puzzle;
import logic.Silo;
import logic.Stream;

import java.util.*;

/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Nevaeh Martinez
 *
 * Parses a puzzle input file into a Puzzle object.
 *
 * Silos are listed row by row, left to right: (0,0), (0,1), ..., (1,0), ...
 * A silo with no instructions is represented by an immediate END.
 * Stream coordinates use one out-of-bounds value to indicate which edge
 * the stream attaches to (see Stream.java for details).
 */
public class Parser {

    /**
     * Parses the full puzzle file from the given scanner and returns
     * a fully constructed Puzzle object ready to be started.
     *
     * @param scanner a Scanner opened on the puzzle input file
     * @return the parsed Puzzle
     */
    public static Puzzle parse(Scanner scanner) {

        // --- Title and description ---
        // Title is always the first line.
        // Description continues until the GRID keyword is found.
        String title = scanner.nextLine().trim();
        String description = scanner.nextLine().trim();

        // Consume any remaining description lines until we hit GRID
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.equals("GRID")) break;
        }

        // --- Grid dimensions ---
        int rows = scanner.nextInt();
        int cols = scanner.nextInt();
        Silo[][] grid = new Silo[rows][cols];
        scanner.nextLine(); // consume the newline after the dimensions

        // --- Silo programs ---
        // Read one silo per (row, col) position, row-major order.
        // Each silo's instructions are terminated by a bare END line.
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = parseSilo(scanner);
            }
        }

        // Consume the END that closes the entire grid section
        scanner.nextLine();

        // --- Input and output streams ---
        Map<String, Stream> inputMap = new HashMap<>();
        Map<String, Stream> outputMap = new HashMap<>();

        while (scanner.hasNextLine()) {
            String next = scanner.nextLine().trim();

            if (next.isEmpty()) continue; // skip blank lines
            if (next.equals("END")) break; // end of file

            if (next.startsWith("INPUT")) {
                Stream stream = parseStream(next, scanner);
                inputMap.put(stream.name(), stream);

            } else if (next.startsWith("OUTPUT")) {
                Stream stream = parseStream(next, scanner);
                outputMap.put(stream.name(), stream);
            }
        }

        return new Puzzle(title, description, grid, inputMap, outputMap);
    }


    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Parses one silo's instruction list from the scanner.
     * Reads lines until a bare END is encountered.
     * Empty lines and inline comments (after a semicolon) are skipped.
     *
     * Each non-empty line is split into up to three tokens:
     *   words[0] = instruction type (e.g. "MOVE", ":START:")
     *   words[1] = arg1 (e.g. "ACC", "DOWN", "42")
     *   words[2] = arg2 (e.g. "RIGHT")
     *
     * @param scanner the scanner positioned at the first line of a silo program
     * @return the constructed Silo
     */
    private static Silo parseSilo(Scanner scanner) {
        List<Instruction> instructions = new ArrayList<>();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            if (line.equals("END")) break; // end of this silo's program
            if (line.isEmpty()) continue; // skip blank lines

            // Strip inline comments (anything after a semicolon)
            int commentIdx = line.indexOf(';');
            if (commentIdx >= 0) {
                line = line.substring(0, commentIdx).trim();
            }
            if (line.isEmpty()) continue; // line was only a comment

            // Split into tokens
            String[] words = line.split("\\s+");

            String type = words[0];
            String arg1 = words.length >= 2 ? words[1] : null;
            String arg2 = words.length >= 3 ? words[2] : null;

            instructions.add(new Instruction(type, arg1, arg2));
        }

        return new Silo(instructions);
    }

    /**
     * Parses one input or output stream from the scanner.
     * The stream header line (e.g. "INPUT.X") has already been read
     * by the caller and is passed in as the name parameter.
     *
     * Reads the row/col position, then reads integer values one per line
     * until a bare END is encountered.
     *
     * @param name the stream name line already read (e.g. "INPUT.X")
     * @param scanner the scanner positioned just after the name line
     * @return the constructed Stream
     */
    private static Stream parseStream(String name, Scanner scanner) {
        int row = scanner.nextInt();
        int col = scanner.nextInt();
        scanner.nextLine(); // consume the newline after row/col

        List<Integer> values = new ArrayList<>();
        while (scanner.hasNextLine()) {
            String value = scanner.nextLine().trim();
            if (value.equals("END")) break;
            if (value.isEmpty())     continue;
            values.add(Integer.parseInt(value));
        }

        return new Stream(name, row, col, values);
    }
}