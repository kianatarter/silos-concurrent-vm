package logic;

public interface Simulator {
    void onRegisterChange(int row, int col, int acc, int bak, int pc);
    void onPortTransfer(int fromRow, int fromCol, String direction, int value);
    void onInputConsumed(String streamName, int index);
    void onOutputProduced(String streamName, int index, boolean correct);
}
