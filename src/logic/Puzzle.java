package logic;

import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Neveah Martinez
 */
public class Puzzle {
    public String title;
    public String description;
    public Silo[][] grid;
    public Map<String, Stream> inputs;
    public Map<String, Stream> outputs;

    private AtomicInteger activeSilos;

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
        initBarrier();
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                grid[r][c].setPuzzle(this);
                Thread t = new Thread(grid[r][c]);
                t.setDaemon(true);
                t.start();
            }
        }
    }

    public void setActiveSilos(AtomicInteger activeSilos) { this.activeSilos = activeSilos; }
    public AtomicInteger getActiveSilos() { return activeSilos; }

    private final Object barrierLock = new Object();
    private int silosWaiting = 0;
    private int generation = 0; // increments each time barrier trips

    public void initBarrier() {
        activeSilos = new AtomicInteger(grid.length * grid[0].length);
    }

    public void awaitBarrier() throws InterruptedException {
        synchronized (barrierLock) {
            int gen = generation;

            // snapshot active count BEFORE incrementing silosWaiting —
            // if we read it after, another thread can decrement activeSilos
            // in the window between the two lines and cause the barrier to
            // trip too early, stranding silos that haven't arrived yet.
            int needed = activeSilos.get();
            silosWaiting++;

            if (silosWaiting >= needed) {
                // all active silos are here — trip the barrier
                silosWaiting = 0;
                generation++;
                barrierLock.notifyAll();
            } else {
                // wait until barrier trips
                while (gen == generation) {
                    barrierLock.wait();
                }
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
     * creates and starts a thread to feed or consume values through that port's queue.
     * Must be called after wireNeighbors().
     */
    public void wireStreams() {
        // handle input streams — one feeder thread per stream
        for (Stream stream : inputs.values()) {
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

            Thread feeder = new Thread(() -> {
                try {
                    for (int value : stream.values()) {
                        queue.put(value);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            feeder.setDaemon(true);
            feeder.start();
        }

        // handle output streams — ONE shared collector thread reads all streams in lockstep
        Thread collector = new Thread(() -> {
            try {
                List<Stream> outputList = new ArrayList<>(outputs.values());

                int maxValues = outputList.stream()
                      .mapToInt(s -> s.values().size())
                      .max()
                      .orElse(0);

                for (int i = 0; i < maxValues; i++) {
                    for (Stream stream : outputList) {
                        if (i >= stream.values().size()) continue;

                        int r = stream.row();
                        int c = stream.col();
                        Silo silo;
                        String direction;

                        if (r == -1) { silo = grid[0][c]; direction = "UP"; }
                        else if (r == grid.length) { silo = grid[grid.length-1][c]; direction = "DOWN"; }
                        else if (c == -1) { silo = grid[r][0]; direction = "LEFT"; }
                        else { silo = grid[r][grid[0].length-1]; direction = "RIGHT"; }

                        int actual = silo.getQueue(direction).take();
                        int expected = stream.values().get(i);
                        if (actual == expected) {
                            System.out.println("[" + stream.name() + "] correct: " + actual);
                        } else {
                            System.out.println("[" + stream.name() + "] MISMATCH expected=" + expected + " got=" + actual);
                        }
                    }
                }
                System.out.println("All streams complete");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        collector.setDaemon(true);
        collector.start();
    }
}