import java.util.List;
import java.util.Map;

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
     * Constructor for creating a Silo with instructions
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
        return "Silo[" + instructions.size() + " instructions]";
    }
}
