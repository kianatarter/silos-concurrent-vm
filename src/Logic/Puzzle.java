package Logic;

import java.util.Map;

/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Neveah Martinez
 *
 *
 */
public class Puzzle {
    public String title;
    public String description;
    public Silo[][] grid;
    public Map<String, Stream> inputs;
    public Map<String, Stream> outputs;

    public Puzzle(String title, String description, Silo[][] grid,
                  Map<String, Stream> inputs, Map<String, Stream> outputs) {
        this.title = title;
        this.description = description;
        this.grid = grid;
        this.outputs = outputs;
        this.inputs = inputs;
    }
}
