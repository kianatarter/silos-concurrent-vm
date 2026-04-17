package logic;

public class Interpreter {
   private Silo silo;

   public Interpreter(Silo silo){
      this.silo = silo;
   }

   public void executeOneInstruction(Instruction instruction) {
      switch (instruction.type()) {
         case "NOOP" -> {}
         case "ADD" -> silo.setACC(silo.getACC() + resolve(instruction.arg1()));
         case "SUB" -> silo.setACC(silo.getACC() - resolve(instruction.arg1()));
         case "NEGATE" -> silo.setACC(-silo.getACC());
         case "SAVE" -> silo.setBAK(silo.getACC());
         case "SWAP" -> {
            int tmp = silo.getACC();
            silo.setACC(silo.getBAK());
            silo.setBAK(tmp);
         }
         case "MOVE" -> {
            int val = resolve(instruction.arg1());
            write(instruction.arg2(), val);
         }
         case "JUMP" -> { silo.setPC(silo.labelIndex(instruction.arg1())); return; }
         case "JEZ"  -> { if (silo.getACC() == 0) { silo.setPC(silo.labelIndex(instruction.arg1())); return; } }
         case "JNZ"  -> { if (silo.getACC() != 0) { silo.setPC(silo.labelIndex(instruction.arg1())); return; } }
         case "JGZ"  -> { if (silo.getACC() >  0) { silo.setPC(silo.labelIndex(instruction.arg1())); return; } }
         case "JLZ"  -> { if (silo.getACC() <  0) { silo.setPC(silo.labelIndex(instruction.arg1())); return; } }
         case "JRO"  -> {
            int offset = resolve(instruction.arg1());
            int next = Math.floorMod(silo.getPC() + offset, silo.executableCount());
            silo.setPC(next);
            return;
         }
         case "SLEEP" -> {
            int ms = Integer.parseInt(instruction.arg1()) * 1000;
            try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
         }
      }
      silo.setPC(silo.getPC() + 1);
   }

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

   private void write(String dst, int value) {
      switch (dst) {
         case "ACC" -> silo.setACC(value);
         case "NIL" -> {}  // discard
         case "BAK" -> {}  // invalid direct write, silently ignore
         case "UP" -> silo.writePort("UP", value);
         case "DOWN" -> silo.writePort("DOWN", value);
         case "LEFT" -> silo.writePort("LEFT", value);
         case "RIGHT" -> silo.writePort("RIGHT", value);
      }
   }
}
