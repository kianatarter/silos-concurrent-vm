package logic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Neveah Martinez
 *
 *
 */
public class Silo implements Runnable {
    private List<Instruction> instructions;
    private int ACC = 0;
    private int BAK = 0;
    private int PC = 0;

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

    @Override
    public void run() {
        if (instructions.isEmpty()) {
            puzzle.getActiveSilos().decrementAndGet();
            return;
        }

        Interpreter interpreter = new Interpreter(this);
        while (true) {
            if (PC >= instructions.size()) PC = 0;
            Instruction inst = instructions.get(PC);
            justCompletedPortTransfer = false;
            interpreter.executeOneInstruction(inst);
            try {
                // only hit the barrier if we didn't block on a port this step
                if (!justCompletedPortTransfer) {
                    puzzle.awaitBarrier();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Constructor for creating a Logic.Silo with instructions
     * @param instructions the program instructions for a single silo
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
            puzzle.getActiveSilos().decrementAndGet();
            int value = switch(direction) {
                case "UP" -> upQueue.take();
                case "DOWN" -> downQueue.take();
                case "LEFT" -> leftQueue.take();
                case "RIGHT" -> rightQueue.take();
                default -> 0;
            };
            puzzle.getActiveSilos().incrementAndGet();
            justCompletedPortTransfer = true;
            return value;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    public void writePort(String direction, int value) {
        try {
            puzzle.getActiveSilos().decrementAndGet();
            switch(direction) {
                case "UP" -> upQueue.put(value);
                case "DOWN" -> downQueue.put(value);
                case "LEFT" -> leftQueue.put(value);
                case "RIGHT" -> rightQueue.put(value);
            }
            puzzle.getActiveSilos().incrementAndGet();
            justCompletedPortTransfer = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // returns index of the instruction after the label
    public int labelIndex(String label) {
        for (int i = 0; i < instructions.size(); i++) {
            // Only works for labels like :START: if you dont strip colons
            if (instructions.get(i).type().equals(":" + label + ":")) {
                return i + 1;
            }// instruction immediately after the label
        }
        return 0;
    }

    public int executableCount() {
        return (int) instructions.stream()
              .filter(i -> !i.type().startsWith(":"))
              .count();
    }

    // Returns the queue for a given direction so streams can wire into it directly
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
