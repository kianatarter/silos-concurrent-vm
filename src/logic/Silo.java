package logic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;

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

    /**
     * Main execution loop for this silo.
     * Runs on its own thread
     * Grabs and executes one instruction at a time infinitely.
     * PC wraps back to 0 when it reaches the end of the program.
     */
    @Override
    public void run() {
        Interpreter interpreter = new Interpreter(this);
        while (true) {
            // wrap PC if we've gone past the last instruction
            if (PC >= instructions.size()) {
                PC = 0;
            }
            Instruction inst = instructions.get(PC);
            interpreter.executeOneInstruction(inst);
            //todo
            // At this point, you need to wait for other non idle silos
            // I would recommend having a flag thats set to 0 at the start
            // of a loop iteration, and 1 after completing instruction.
            // Then you wait until all non idle silos have flag value 1
            // before continuing
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

    // Port communication
    // blocks this silo's thread until a value is available from its neighbor
    // called by resolve() when the interpreter sees UP, DOWN, LEFT, RIGHT as a source
    public int readPort(String direction) {
        try {
            //todo: set internal state to idle so other threads don't wait for you
            return switch(direction) {
                case "UP" -> upQueue.take();
                case "DOWN" -> downQueue.take();
                case "LEFT" -> leftQueue.take();
                case "RIGHT" -> rightQueue.take();
                default -> 0;
            };
            //todo: set internal state to non-idle to resynchronize
        } catch (InterruptedException e) {
            // if the thread is interrupted, restore the flag so the caller knows something happened.
            Thread.currentThread().interrupt();
            return 0;
        }
    }

    // Port communication
    // blocks this silo's thread until a neighbor reads the value from this port.
    // called by write() when the interpreter sees UP, DOWN, LEFT, RIGHT as a destination
    public void writePort(String direction, int value) {
        try {
            //todo: same as read port, set internal state to idle before, and non-idle after
            switch(direction) {
                // .put() blocks until .take() is called on the same queue
                case "UP" -> upQueue.put(value);
                case "DOWN" -> downQueue.put(value);
                case "LEFT" -> leftQueue.put(value);
                case "RIGHT" -> rightQueue.put(value);
            }
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
