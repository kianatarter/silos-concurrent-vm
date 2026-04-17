package Logic;

import java.util.List;
import java.util.concurrent.SynchronousQueue;

/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Neveah Martinez
 *
 *
 */
public class Silo {
    private List<Instruction> instructions;
    private int ACC = 0;
    private int BAK = 0;
    private int PC = 0;

    private SynchronousQueue<Integer> upQueue;
    private SynchronousQueue<Integer> downQueue;
    private SynchronousQueue<Integer> leftQueue;
    private SynchronousQueue<Integer> rightQueue;

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
            return switch(direction) {
                case "UP" -> upQueue.take();
                case "DOWN" -> downQueue.take();
                case "LEFT" -> leftQueue.take();
                case "RIGHT" -> rightQueue.take();
                default -> 0;
            };
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

}
