package logic;

import java.util.Map;
import java.util.concurrent.SynchronousQueue;

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


    /**
     * Connects neighboring silos by sharing a single SynchronousQueue between them.
     * Each shared channel replaces two separate queues — one silo's RIGHT output
     * becomes the same queue as its neighbor's LEFT input, and so on.
     * Must be called after the grid is built (i.e. after parsing).
     */
    public void wireNeighbors() {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                Silo current = grid[r][c];

                // If there's a silo to the right, create one shared channel
                // between current's RIGHT port and that silo's LEFT port.
                // Only one queue needed — both silos block on the same object.
                if (c + 1 < grid[r].length) {
                    Silo right = grid[r][c + 1];
                    SynchronousQueue<Integer> channel = new SynchronousQueue<>();
                    current.setRightQueue(channel);
                    right.setLeftQueue(channel);
                }

                // If there's a silo below, create one shared channel
                // between current's DOWN port and that silo's UP port.
                if (r + 1 < grid.length) {
                    Silo below = grid[r + 1][c];
                    SynchronousQueue<Integer> channel = new SynchronousQueue<>();
                    current.setDownQueue(channel);
                    below.setUpQueue(channel);
                }
            }
        }
    }
}
