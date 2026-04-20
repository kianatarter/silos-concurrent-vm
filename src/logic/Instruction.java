package logic;

/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Nevaeh Martinez
 *
 * Represents a single parsed instruction in a silo's program.
 * Stored as a plain data record — the Interpreter is responsible
 * for executing it.
 *
 * Examples:
 *   MOVE ACC DOWN → type="MOVE",  arg1="ACC",  arg2="DOWN"
 *   ADD 10 → type="ADD", arg1="10", arg2=null
 *   JUMP START → type="JUMP", arg1="START",arg2=null
 *   NOOP → type="NOOP", arg1=null, arg2=null
 *   :START: → type=":START:", arg1=null, arg2=null (label, not executable)
 */
public record Instruction(String type, String arg1, String arg2) {
}