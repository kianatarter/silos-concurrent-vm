package logic;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Nevaeh Martinez
 *
 * Holds the full puzzle state: the silo grid, input/output streams,
 * and the synchronization barrier that keeps all silos in lock-step.
 *
 * Also owns the pause/step/stop controls used by the GUI buttons.
 */
public class Puzzle {

    // ---------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------

    /** Puzzle metadata parsed from the input file. */
    public String title;
    public String description;

    /** The grid of silos, indexed as grid[row][col]. */
    public Silo[][] grid;

    /** Input streams keyed by stream name (e.g. "INPUT.X"). */
    public Map<String, Stream> inputs;

    /** Output streams keyed by stream name (e.g. "OUTPUT.A"). */
    public Map<String, Stream> outputs;

    /**
     * GUI callback. Set via setSimulator() before start() is called.
     * Null-checked before every call so the puzzle works headlessly too.
     */
    private Simulator simulator;
    private int delayMS = 1000;
    private volatile boolean paused = false;


    /**
     * Number of silos currently "active" — i.e. not blocked on a port transfer.
     * The barrier uses this count to know when all active silos have checked in.
     * Decremented when a silo blocks on a port, incremented when it unblocks.
     */
    private AtomicInteger activeSilos;

    // --- barrier state ---

    /** Monitor object for all barrier and pause/step/stop coordination. */
    private final Object barrierLock = new Object();

    /** How many active silos have arrived at the barrier this generation. */
    private int silosWaiting = 0;

    /**
     * Incremented each time the barrier trips.
     * Silos waiting on an old generation detect the trip by comparing
     * their snapshot of generation to the current value.
     */
    private int generation = 0;

    // --- pause / step / stop flags ---

    /**
     * When true, awaitBarrier() throws InterruptedException to exit every
     * silo's run loop cleanly.
     */
    private volatile boolean stopped = false;

    /**
     * When true, the barrier allows exactly one generation through,
     * then sets paused = true again before releasing silos.
     * Reset to false automatically after each use.
     */
    private volatile boolean stepOnce = false;


    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------

    /**
     * Creates a Puzzle and immediately wires neighbors and streams.
     * Call start() separately when ready to launch silo threads.
     */
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

    public void setDelay(int millis){
        this.delayMS = millis;
    }

    public int getDelayMS(){
        return delayMS;
    }

    /**
     * Registers the GUI callback. Call this before start() so the
     * feeder and collector threads can push stream updates to the GUI.
     *
     * @param simulator the MainWindow (or any Simulator implementation)
     */
    public void setSimulator(Simulator simulator) {
        this.simulator = simulator;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }



    /**
     * Initializes the barrier counter and launches each silo on its own
     * daemon thread. Silos will immediately begin executing and hitting
     * the barrier. If pause() was called before start(), they will block
     * at the first barrier until resume() is called.
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

    public void initBarrier() {
        activeSilos = new AtomicInteger(grid.length * grid[0].length);
    }

    // Getters for activeSilos used by Silo.java
    public void setActiveSilos(AtomicInteger a) { this.activeSilos = a; }
    public AtomicInteger getActiveSilos() { return activeSilos; }


    // ---------------------------------------------------------------
    // Barrier
    // ---------------------------------------------------------------

    /**
     * Called by every active silo after executing one instruction.
     * Blocks until all other active silos have also called awaitBarrier(),
     * then releases them all together for the next instruction.
     *
     * Also handles pause and stop:
     * - If stopped, throws InterruptedException so the silo's run() loop exits.
     * - If paused, blocks after the barrier trips until resume() or step() wakes it.
     *
     * @throws InterruptedException if stop() has been called
     */
    public void awaitBarrier() throws InterruptedException {
        synchronized (barrierLock) {

            // Exit immediately if the puzzle has been stopped
            if (stopped) throw new InterruptedException("stopped");

            int gen = generation;

            int needed = activeSilos.get();
            silosWaiting++;

            if (silosWaiting >= needed) {
                // All active silos have arrived — trip the barrier.
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
     * Each shared channel replaces two separate queues - one silo's RIGHT output
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
     * Connects input and output streams to their edge silos.
     * Each stream's out-of-bounds coordinate tells us which silo and
     * which port direction it attaches to.
     *
     * Input streams get a feeder thread that puts values into the queue.
     * Output streams get a collector thread that takes values and checks
     * them against the expected list.
     *
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

            final String feederName = stream.name();
            Thread feeder = new Thread(() -> {
                try {
                    List<Integer> vals = stream.values();
                    for (int i = 0; i < vals.size(); i++) {
                        queue.put(vals.get(i));
                        // Notify GUI that this value was consumed by a silo
                        if (simulator != null) {
                            final int idx = i;
                            simulator.onInputConsumed(feederName, idx);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            feeder.setDaemon(true);
            feeder.start();
        }

        // --- Output streams: one collector thread per stream ---
        // Each stream drains independently so fast streams don't
        // block slow ones (fixes the ordering bug from the single-collector approach).
        for (Stream stream : outputs.values()) {
            SynchronousQueue<Integer> queue = resolveQueue(stream);

            final String collectorName = stream.name();
            Thread collector = new Thread(() -> {
                try {
                    List<Integer> vals = stream.values();
                    for (int i = 0; i < vals.size(); i++) {
                        int actual   = queue.take();
                        int expected = vals.get(i);
                        boolean correct = (actual == expected);

                        if (correct) {
                            System.out.println("[" + collectorName + "][" + i + "] correct: " + actual);
                        } else {
                            System.out.println("[" + collectorName + "][" + i + "] MISMATCH expected="
                                  + expected + " got=" + actual);
                        }

                        // Notify GUI with the result so it can update the stream display
                        if (simulator != null) {
                            final int idx = i;
                            simulator.onOutputProduced(collectorName, idx, correct);
                        }
                    }
                    System.out.println("[" + collectorName + "] complete");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            collector.setDaemon(true);
            collector.start();
        }
    }

    /**
     * Resolves which SynchronousQueue a stream connects to, based on its
     * out-of-bounds coordinate.
     *
     * Row == -1 → above the grid → silo (0, col)'s UP port
     * Row == grid.length → below the grid → silo (last row, col)'s DOWN port
     * Col == -1 → left of the grid → silo (row, 0)'s LEFT port
     * Col >= grid width → right of grid → silo (row, last col)'s RIGHT port
     *
     * @param stream the stream to resolve
     * @return the SynchronousQueue for the corresponding silo port
     */
    private SynchronousQueue<Integer> resolveQueue(Stream stream) {
        int r = stream.row();
        int c = stream.col();

        Silo   silo;
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

        return silo.getQueue(direction);
    }
}