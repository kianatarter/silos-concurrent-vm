package logic;

import java.util.List;
import java.util.concurrent.SynchronousQueue;

/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Nevaeh Martinez
 *
 * Represents a single virtual machine in the silo grid.
 * Each silo runs on its own thread, executing its instruction list
 * in a loop and synchronizing with all other silos via the barrier
 * in Puzzle after every instruction.
 *
 * Silos communicate with neighbors through four directional ports
 * (UP, DOWN, LEFT, RIGHT), each backed by a SynchronousQueue.
 * Port transfers are blocking rendezvous — both sides must arrive
 * before either can proceed.
 */
public class Silo implements Runnable {

    // ---------------------------------------------------------------
    // Fields
    // ---------------------------------------------------------------

    /** The parsed instruction list for this silo's program. */
    private List<Instruction> instructions;

    // --- Registers ---

    /** Accumulator. Primary working register. Starts at 0. */
    private int ACC = 0;

    /** Backup register. Cannot be written directly — use SAVE or SWAP. Starts at 0. */
    private int BAK = 0;

    /** Program counter. Index of the next instruction to execute. Starts at 0. */
    private int PC = 0;
    private Simulator callback;
    private int row, col;

    private SynchronousQueue<Integer> upQueue = new SynchronousQueue<>();
    private SynchronousQueue<Integer> downQueue = new SynchronousQueue<>();
    private SynchronousQueue<Integer> leftQueue = new SynchronousQueue<>();
    private SynchronousQueue<Integer> rightQueue = new SynchronousQueue<>();

    // Replaces this silo's UP queue with a shared channel.
    // Called when the silo above wires its DOWN port to this silo's UP port.
    public void setUpQueue(SynchronousQueue<Integer> q) { upQueue = q; }

    // Replaces this silo's DOWN queue with a shared channel.
    // Called when wiring this silo's DOWN port to the silo below's UP port.
    public void setDownQueue(SynchronousQueue<Integer> q) { downQueue = q; }

    // Replaces this silo's LEFT queue with a shared channel.
    // Called when the silo to the left wires its RIGHT port to this silo's LEFT port.
    public void setLeftQueue(SynchronousQueue<Integer> q) { leftQueue = q; }

    // Replaces this silo's RIGHT queue with a shared channel.
    // Called when wiring this silo's RIGHT port to the silo to the right's LEFT port.
    public void setRightQueue(SynchronousQueue<Integer> q) { rightQueue = q; }

    // replace barrier field with puzzle reference
    private Puzzle puzzle;

    public void setPuzzle(Puzzle puzzle) { this.puzzle = puzzle; }

    private boolean justCompletedPortTransfer = false;

    public void setCallback(Simulator callback, int row, int col) {
        this.callback = callback;
        this.row = row;
        this.col = col;
    }

    @Override
    public void run() {
        if (instructions.isEmpty()) {
            puzzle.getActiveSilos().decrementAndGet();
            return;
        }

        Interpreter interpreter = new Interpreter(this);
        while (true) {
            while (puzzle.isPaused()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (PC >= instructions.size()) PC = 0;
            Instruction inst = instructions.get(PC);
            justCompletedPortTransfer = false;
            interpreter.executeOneInstruction(inst);

            if (callback != null) {
                callback.onRegisterChange(row, col, ACC, BAK, PC);
            }

            try {
                if (puzzle.getDelayMS() > 0) {
                    Thread.sleep(puzzle.getDelayMS());
                }

                if (justCompletedPortTransfer) {
                    // The silo blocked on a port and already decremented activeSilos.
                    // Wait at the barrier while still "inactive", then re-increment
                    // so the count is correct for the next round. This prevents the
                    // silo from being counted as active before it has re-synchronized.
                    puzzle.awaitBarrier();
                    puzzle.getActiveSilos().incrementAndGet();
                } else {
                    puzzle.awaitBarrier();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Reads a value from the given port. Blocks until the neighboring silo
     * (or input stream feeder) writes to the corresponding port.
     *
     * Decrements activeSilos before blocking so the barrier doesn't
     * wait for this silo while it's idle. The run() loop re-increments
     * activeSilos after the next barrier trip.
     *
     * @param direction "UP", "DOWN", "LEFT", or "RIGHT"
     * @return the value received, or 0 if interrupted
     */
    public Silo(List<Instruction> instructions){
        this.instructions = instructions;

    }

    public List<Instruction> getInstructions(){
        return instructions;
    }

    @Override
    public String toString(){
        return "Logic.Silo[" + instructions.size() + " instructions]";
    }


    public int getACC() { return ACC; }
    public int getBAK() { return BAK; }
    public int getPC() { return PC; }

    public void setACC(int val) { ACC = val; }
    public void setBAK(int val) { BAK = val; }
    public void setPC(int val) { PC = val; }

    public int readPort(String direction) {
        try {
            // Decrement before blocking. The run() loop will re-increment
            // after the barrier once this port transfer completes.
            puzzle.getActiveSilos().decrementAndGet();
            int value = switch(direction) {
                case "UP" -> upQueue.take();
                case "DOWN" -> downQueue.take();
                case "LEFT" -> leftQueue.take();
                case "RIGHT" -> rightQueue.take();
                default -> 0;
            };
            justCompletedPortTransfer = true;
            return value;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    /**
     * Writes a value to the given port. Blocks until the neighboring silo
     * (or output stream collector) reads from the corresponding port.
     *
     * Decrements activeSilos before blocking so the barrier doesn't
     * wait for this silo while it's idle. The run() loop re-increments
     * activeSilos after the next barrier trip.
     *
     * @param direction "UP", "DOWN", "LEFT", or "RIGHT"
     * @param value the value to send
     */
    public void writePort(String direction, int value) {
        try {
            // Signal that this silo is now idle (blocked on port)
            puzzle.getActiveSilos().decrementAndGet();

            if (callback != null) {
                callback.onPortTransfer(row, col, direction, value);
            }

            switch(direction) {
                case "UP" -> upQueue.put(value);
                case "DOWN" -> downQueue.put(value);
                case "LEFT" -> leftQueue.put(value);
                case "RIGHT" -> rightQueue.put(value);
            }
            justCompletedPortTransfer = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    // ---------------------------------------------------------------
    // Label and instruction utilities
    // ---------------------------------------------------------------

    /**
     * Finds the index of the instruction immediately after a label.
     * Labels are stored as instructions with type ":LABEL:", so this
     * scans the list for a matching type and returns i+1.
     *
     * Returns 0 (start of program) if the label is not found.
     *
     * @param label the label name without colons (e.g. "START")
     * @return the PC index to jump to
     */
    public int labelIndex(String label) {
        for (int i = 0; i < instructions.size(); i++) {
            // Only works for labels like :START: if you dont strip colons
            if (instructions.get(i).type().equals(":" + label + ":")) {
                return i + 1;
            }// instruction immediately after the label
        }
        return 0;
    }

    /**
     * Returns the number of executable instructions (excluding labels).
     * Used by JRO to compute wrap-around offsets over only real instructions.
     *
     * @return count of non-label instructions
     */
    public int executableCount() {
        return (int) instructions.stream()
              .filter(i -> !i.type().startsWith(":"))
              .count();
    }

    /**
     * Returns the SynchronousQueue for the given port direction.
     * Used by Puzzle.wireStreams() to connect input/output stream threads
     * directly to a silo's port queue.
     *
     * @param direction "UP", "DOWN", "LEFT", or "RIGHT"
     * @return the queue, or null for an unrecognized direction
     */
    public SynchronousQueue<Integer> getQueue(String direction) {
        return switch (direction) {
            case "UP" -> upQueue;
            case "DOWN" -> downQueue;
            case "LEFT" -> leftQueue;
            case "RIGHT" -> rightQueue;
            default -> null;
        };
    }

}