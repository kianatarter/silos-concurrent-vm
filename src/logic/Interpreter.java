package logic;

/**
 * CS 351L Project 4: Silos
 * @author Kiana Tarter and Nevaeh Martinez
 *
 * Executes one instruction at a time for a given Silo.
 * Called by Silo.run() on each iteration of the execution loop.
 *
 * The interpreter reads from and writes to the silo's registers (ACC, BAK)
 * and program counter (PC). Port reads and writes are delegated back to
 * Silo.readPort() and Silo.writePort(), which handle the blocking rendezvous.
 *
 * Jump instructions return early (before the PC++ at the bottom of
 * executeOneInstruction) so the PC is set exactly where they specify.
 * All other instructions fall through to the PC++ at the end.
 */
public class Interpreter {

   // ---------------------------------------------------------------
   // Fields
   // ---------------------------------------------------------------

   /** The silo this interpreter is executing instructions for. */
   private Silo silo;


   // ---------------------------------------------------------------
   // Constructor
   // ---------------------------------------------------------------

   /**
    * Creates an interpreter bound to the given silo.
    * One interpreter is created per silo in Silo.run().
    *
    * @param silo the silo whose registers and ports this interpreter operates on
    */
   public Interpreter(Silo silo) {
      this.silo = silo;
   }


   // ---------------------------------------------------------------
   // Instruction execution
   // ---------------------------------------------------------------

   /**
    * Executes a single instruction and advances the program counter.
    *
    * Jump instructions (JUMP, JEZ, JNZ, JGZ, JLZ, JRO) set the PC
    * themselves and return early — they do NOT fall through to the PC++
    * at the bottom, because that would skip the target instruction.
    *
    * MOVE may block if either src or dst is a port (see Silo.readPort /
    * Silo.writePort). The silo thread will stay inside this call until
    * the rendezvous completes.
    *
    * @param instruction the instruction to execute
    */
   public void executeOneInstruction(Instruction instruction) {
      switch (instruction.type()) {

         // --- No-op ---
         case "NOOP" -> {}
         // Does nothing. Consumes one step.

         // --- Arithmetic ---
         case "ADD" -> silo.setACC(silo.getACC() + resolve(instruction.arg1()));
         case "SUB" -> silo.setACC(silo.getACC() - resolve(instruction.arg1()));
         case "NEGATE" -> silo.setACC(-silo.getACC());

         // --- Register transfers ---
         case "SAVE" -> silo.setBAK(silo.getACC()); // BAK = ACC
         case "SWAP" -> {  // swap ACC <-> BAK
            int tmp = silo.getACC();
            silo.setACC(silo.getBAK());
            silo.setBAK(tmp);
         }

         // --- Data movement ---
         // resolve() handles port reads (blocking); write() handles port writes (blocking)
         case "MOVE" -> {
            int val = resolve(instruction.arg1());
            write(instruction.arg2(), val);
         }

         // --- Unconditional jump ---
         case "JUMP" -> {
            silo.setPC(silo.labelIndex(instruction.arg1()));
            return; // skip PC++ — PC is already set to the target
         }

         // --- Conditional jumps ---
         case "JEZ" -> {
            if (silo.getACC() == 0) { silo.setPC(silo.labelIndex(instruction.arg1())); return; }
         }
         case "JNZ" -> {
            if (silo.getACC() != 0) { silo.setPC(silo.labelIndex(instruction.arg1())); return; }
         }
         case "JGZ" -> {
            if (silo.getACC() > 0) { silo.setPC(silo.labelIndex(instruction.arg1())); return; }
         }
         case "JLZ" -> {
            if (silo.getACC() < 0) { silo.setPC(silo.labelIndex(instruction.arg1())); return; }
         }

         // --- Relative jump ---
         case "JRO" -> {
            int offset = resolve(instruction.arg1());
            // Wrap using the full instruction list size, NOT executableCount().
            // executableCount() skips label entries, making it smaller than
            // the real list. Using it here can land the PC on a label instruction,
            // causing the silo to silently spin in the wrong place.
            int next = Math.floorMod(
                  silo.getPC() + offset,
                  silo.getInstructions().size());
            silo.setPC(next);
            return; // skip PC++ — PC is already set to the target
         }

         // --- Sleep ---
         // Pauses this silo's thread for N seconds. Because every other silo
         // is blocked at the barrier waiting for this one, the whole simulation
         // effectively halts for the duration.
         case "SLEEP" -> {
            int ms = Integer.parseInt(instruction.arg1()) * 1000;
            try {
               Thread.sleep(ms);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
         }

         // --- Labels and unknown instructions ---
         // Label entries (e.g. ":START:") are stored in the instruction list
         // but are not executable. If the PC lands on one (e.g. after JRO),
         // we just fall through to the PC++ below and skip past it harmlessly.
         default -> {}
      }

      // Advance to the next instruction.
      // Jump instructions return early above and never reach this line.
      silo.setPC(silo.getPC() + 1);
   }


   // ---------------------------------------------------------------
   // Helpers
   // ---------------------------------------------------------------

   /**
    * Resolves a source argument to an integer value.
    *
    * Registers (ACC, BAK, NIL) are read directly.
    * Port names (UP, DOWN, LEFT, RIGHT) trigger a blocking read via
    * Silo.readPort() — the silo thread will block here until a neighbor
    * writes to the corresponding port.
    * Integer literals (e.g. "42", "-7") are parsed directly.
    *
    * @param arg the source argument string from the instruction
    * @return the resolved integer value
    */
   private int resolve(String arg) {
      return switch (arg) {
         case "ACC" -> silo.getACC();
         case "BAK" -> silo.getBAK();
         case "NIL" -> 0;
         case "UP" -> silo.readPort("UP");
         case "DOWN" -> silo.readPort("DOWN");
         case "LEFT" -> silo.readPort("LEFT");
         case "RIGHT" -> silo.readPort("RIGHT");
         default -> Integer.parseInt(arg);
      };
   }

   /**
    * Writes a value to a destination.
    *
    * ACC is written directly.
    * NIL silently discards the value (useful as a throwaway destination).
    * BAK cannot be written directly — SAVE must be used instead. Writes to
    * BAK are silently ignored per the spec.
    * Port names (UP, DOWN, LEFT, RIGHT) trigger a blocking write via
    * Silo.writePort() — the silo thread will block here until a neighbor
    * reads from the corresponding port.
    *
    * @param dst the destination argument string from the instruction
    * @param value the value to write
    */
   private void write(String dst, int value) {
      switch (dst) {
         case "ACC" -> silo.setACC(value);
         case "NIL" -> {}  // discard — writing to NIL is a valid no-op
         case "BAK" -> {}  // invalid direct write per spec — silently ignore
         case "UP" -> silo.writePort("UP",    value);
         case "DOWN" -> silo.writePort("DOWN",  value);
         case "LEFT" -> silo.writePort("LEFT",  value);
         case "RIGHT" -> silo.writePort("RIGHT", value);
      }
   }
}