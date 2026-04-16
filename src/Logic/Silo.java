package Logic;

import java.util.List;

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

    // Temporary stubs for ports that will be implemented later
    public int readPort(String direction) { return 0; }
    public void writePort(String direction, int value) {}

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
