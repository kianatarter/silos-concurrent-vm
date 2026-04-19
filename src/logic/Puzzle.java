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
        wireNeighbors();
        wireStreams();
    }

    /**
     * Launches each silo on its own thread.
     * Call this after wireNeighbors() and wireStreams() are done.
     */
    public void start() {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                Thread t = new Thread(grid[r][c]);
                t.setDaemon(true);
                t.start();
            }
        }
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

    /**
     * Connects input and output streams to their edge silos by figuring out
     * which silo and which port direction the stream sits next to, then
     * create and start a thread to feed or consume values through that port's queue.
     * Must be called after wireNeighbors().
     */
    public void wireStreams() {
        // handle input streams
        for (Stream stream : inputs.values()) {
            int r = stream.row();
            int c = stream.col();

            // figure out which silo and direction this stream connects to
            Silo silo;
            String direction;

            if (r == -1) {
                // stream is above the grid, connects to top row silo's UP port
                silo = grid[0][c];
                direction = "UP";
            } else if (r == grid.length) {
                // stream is below the grid, connects to bottom row silo's DOWN port
                silo = grid[grid.length - 1][c];
                direction = "DOWN";
            } else if (c == -1) {
                // stream is left of the grid, connects to left column silo's LEFT port
                silo = grid[r][0];
                direction = "LEFT";
            } else {
                // stream is right of the grid, connects to right column silo's RIGHT port
                silo = grid[r][grid[0].length - 1];
                direction = "RIGHT";
            }

            SynchronousQueue<Integer> queue = silo.getQueue(direction);

            // create and start a thread that feeds values one at a time into the silo's queue
            // blocks between each value until the silo is ready to read
            Thread feeder = new Thread(() -> {
                try {
                    for (int value : stream.values()) {
                        queue.put(value);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            feeder.setDaemon(true); // dies when main program exits
            feeder.start();
        }

        // handle output streams
        for (Stream stream : outputs.values()) {
            int r = stream.row();
            int c = stream.col();

            Silo silo;
            String direction;

            if (r == -1) {
                silo = grid[0][c];
                direction = "UP";
            } else if (r == grid.length) {
                silo = grid[grid.length - 1][c];
                direction = "DOWN";
            } else if (c == -1) {
                silo = grid[r][0];
                direction = "LEFT";
            } else {
                silo = grid[r][grid[0].length - 1];
                direction = "RIGHT";
            }

            SynchronousQueue<Integer> queue = silo.getQueue(direction);

            // create and start a thread that reads values from the silo's queue
            // and compares them against the expected output list
            Thread collector = new Thread(() -> {
                try {
                    for (int expected : stream.values()) {
                        int actual = queue.take();
                        if (actual == expected) {
                            System.out.println("[" + stream.name() + "] correct: " + actual);
                        } else {
                            System.out.println("[" + stream.name() + "] MISMATCH expected=" + expected + " got=" + actual);
                        }
                    }
                    System.out.println("[" + stream.name() + "] stream complete");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            collector.setDaemon(true);
            collector.start();
        }
    }
}
