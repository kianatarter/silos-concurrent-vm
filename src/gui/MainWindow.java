package gui;


import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.geometry.Pos;

import javafx.util.Duration;
import logic.*;
import parser.Parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class MainWindow extends Application implements Simulator {
    private Label[][] accLabels;
    private Label[][] bakLabels;
    private Label[][] pcLabels;
    private Label[][] stateLabels;
    private List<Label>[][] instructionLabels; // Store instruction labels for highlighting
    private Puzzle puzzle;

    private volatile boolean running = false;
    private volatile boolean paused = false;

    private Label[][][] horizontalArrows;
    private Label[][][] verticalArrows;


    private Timeline updateTimer;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Scanner scanner = new Scanner(new File("test_input/simple_example2.txt"));
        this.puzzle = Parser.parse(scanner);


        BorderPane root = new BorderPane();
        primaryStage.setTitle("CS 351 Project 4: Silos");
        root.setStyle("-fx-background-color: #1a1a1a");

        GridPane silos = new GridPane();
        silos.setAlignment(Pos.CENTER);
        silos.setPadding(new Insets(10, 0, 0, 30));

        silos.setHgap(20);
        silos.setVgap(20);


        int rows = puzzle.grid.length;
        int cols = puzzle.grid[0].length;

        accLabels = new Label[rows][cols];
        bakLabels = new Label[rows][cols];
        pcLabels = new Label[rows][cols];
        stateLabels = new Label[rows][cols];
        instructionLabels = new List[rows][cols];

        horizontalArrows = new Label[rows][cols][3];
        verticalArrows = new Label[rows][cols][3];


        for (int r = 0; r < puzzle.grid.length; r++){
            for (int c = 0; c < puzzle.grid[0].length; c++){
                puzzle.grid[r][c].setCallback(this,r,c);

                // create silos
                VBox silo = new VBox(10);
                silo.getStyleClass().add("silo-box");

                // create column to hold registers
                HBox registerBox = new HBox(12);
                registerBox.getStyleClass().add("registers-section");
                //  registerBox.setMaxWidth(Double.MAX_VALUE);
                registerBox.setAlignment(Pos.CENTER);

                // create column to hold instructions
                VBox instructionsBox = new VBox(3);
                instructionsBox.getStyleClass().add("instructions-box");

                // create a reference to store silo data
                Silo siloData = puzzle.grid[r][c];

                // Initialize instruction labels list for this silo
                instructionLabels[r][c] = new ArrayList<>();

                // add instructions for each silo
                for (Instruction instr : siloData.getInstructions()){
                    String instructionText = formatInstruction(instr);
                    Label instrLabel = new Label(instructionText);
                    instrLabel.getStyleClass().add("instr-text");
                    instructionsBox.getChildren().add(instrLabel);
                    instructionLabels[r][c].add(instrLabel); // Store for highlighting
                }

                ScrollPane instructionsScroll = new ScrollPane(instructionsBox);
                instructionsScroll.setFitToWidth(true);
                instructionsScroll.setMaxHeight(100);
                instructionsScroll.getStyleClass().add("instructions-scroll");

                // create register labels
                Label accLabel = new Label("ACC:" + "\n" + siloData.getACC());
                accLabel.getStyleClass().add("silo-register");


                Label bakLabel = new Label("BAK:" + "\n" + siloData.getBAK());
                bakLabel.getStyleClass().add("silo-register");

                Label pcLabel = new Label("PC:" + "\n" + siloData.getPC());
                pcLabel.getStyleClass().add("silo-register");



                accLabels[r][c] = accLabel;
                bakLabels[r][c] = bakLabel;
                pcLabels[r][c] = pcLabel;

                registerBox.getChildren().addAll(accLabel, bakLabel, pcLabel);

                silo.getChildren().addAll(registerBox, instructionsScroll);
                silo.setAlignment(Pos.TOP_LEFT);
                silos.add(silo,c * 2, r * 2);
                // pulseAnimation(silo);


                // RIGHT ARROW
                if (c < cols - 1){
                    HBox arrowBox = new HBox(2);
                    arrowBox.setAlignment(Pos.CENTER);
                    arrowBox.setMinWidth(20);

                    Label arrowSymbol = new Label("↔");
                    arrowSymbol.getStyleClass().add("arrow-symbol");

                    Label leftValue = new Label("");
                    leftValue.getStyleClass().add("arrow-value");

                    Label rightValue = new Label("");
                    rightValue.getStyleClass().add("arrow-value");

                    arrowBox.getChildren().addAll(leftValue, arrowSymbol, rightValue);

                    silos.add(arrowBox, (c * 2) + 1, r * 2);

                    horizontalArrows[r][c][0] = arrowSymbol;
                    horizontalArrows[r][c][1] = rightValue;
                    horizontalArrows[r][c][2] = leftValue;
                }


                // DOWN ARROW
                if (r < rows - 1){
                    VBox arrowBox = new VBox(2);
                    arrowBox.setAlignment(Pos.CENTER);
                    arrowBox.setMinHeight(20);

                    Label arrowSymbol = new Label("↕");
                    arrowSymbol.getStyleClass().add("arrow-symbol");

                    Label upValue = new Label("");
                    upValue.getStyleClass().add("arrow-value");

                    Label downValue = new Label("");
                    downValue.getStyleClass().add("arrow-value");

                    arrowBox.getChildren().addAll(upValue, arrowSymbol, downValue);

                    // Add arrow at ODD row position (r*2 + 1)
                    silos.add(arrowBox, c * 2, (r * 2) + 1);

                    verticalArrows[r][c][0] = arrowSymbol;
                    verticalArrows[r][c][1] = downValue;
                    verticalArrows[r][c][2] = upValue;
                }

            }
        }



        VBox controls = new VBox(15);
        Label controlsTitle = new Label("CONTROLS");
        controlsTitle.getStyleClass().add("instr-text");
        controls.setAlignment(Pos.BOTTOM_CENTER);
        controls.setPadding(new Insets(20));

        TextField delayField = new TextField("1");
        delayField.getStyleClass().add("delay-field");
        delayField.setMaxWidth(160);
        delayField.setPromptText("0-10");

        Button start = new Button("START ▶");
        start.getStyleClass().add("control-button");

        // TODO: add start button implementation
        start.setOnAction(event -> {
            if (!running) {
                running = true;

                try {
                    double delaySeconds = Double.parseDouble(delayField.getText());

                    // Clamp to 0-10 seconds
                    if (delaySeconds < 0) delaySeconds = 0;
                    if (delaySeconds > 10) delaySeconds = 10;

                    int delayMillis = (int)(delaySeconds * 1000);
                    puzzle.setDelay(delayMillis);

                    System.out.println("Delay set to " + delaySeconds + " seconds (" + delayMillis + "ms)");

                } catch (NumberFormatException e) {
                    System.out.println("Invalid delay, using default 1 second");
                    puzzle.setDelay(1000);
                }

                // START AUTOMATIC DISPLAY UPDATES
                updateTimer = new Timeline(new KeyFrame(Duration.millis(100), e -> {
                    updateDisplay();
                }));
                updateTimer.setCycleCount(Timeline.INDEFINITE);
                updateTimer.play();

                // START SIMULATION
                puzzle.start();
                System.out.println("Simulation started");
            }
        });

        Button pause = new Button("PAUSE ⏸");
        pause.getStyleClass().add("control-button");

        pause.setOnAction(event -> {
            if (!running) {
                return; // simulation not started yet
            }

            paused = !paused;
            puzzle.setPaused(paused);

            if (paused) {
                pause.setText("STEP ≫");
                System.out.println("Simulation PAUSED");
            } else {
                pause.setText("PAUSE ⏸");
                System.out.println("Simulation RESUMED");
            }
        });

        Button stop = new Button("STOP ⃠");
        stop.getStyleClass().add("control-button");
        // TODO: add stop button implementation
        stop.setOnAction(event -> {
            if (running) {
                running = false;
                paused = false;

                if (updateTimer != null) {
                    updateTimer.stop();
                }

                pause.setText("PAUSE ⏸");  // Reset pause button text
                System.out.println("Simulation stopped - restart app to run again");
            }
        });

        Label delayLabel = new Label("Delay (sec):");
        delayLabel.getStyleClass().add("instr-text");

        Button step = new Button("STEP ≫");
        step.getStyleClass().add("control-button");

        step.setOnAction(event -> {
            if (!running) {
                System.out.println("Start simulation first!");
                return;
            }

            if (!paused) {
                System.out.println("Pause first to use step mode");
                return;
            }

            // Unpause briefly to let ONE barrier cycle complete
            puzzle.setPaused(false);

            // Re-pause after a short delay
            PauseTransition stepDelay = new PauseTransition(Duration.millis(50));
            stepDelay.setOnFinished(e -> {
                puzzle.setPaused(true);
                System.out.println("Stepped one instruction");
            });
            stepDelay.play();
        });

        controls.getStyleClass().add("controls");
        controls.getChildren().addAll(controlsTitle,
                start,
                pause,
                stop,
                step,
                delayLabel,
                delayField);

        // CREATE I/O STREAMS PANEL
        VBox streamsPanel = new VBox(5);
        streamsPanel.getStyleClass().add("controls");
        streamsPanel.setPadding(new Insets(20));
        streamsPanel.setAlignment(Pos.TOP_CENTER);

        Label streamsTitle = new Label("I/O STREAMS");
        streamsTitle.getStyleClass().add("silo-register");


        // Add inputs
        VBox inputs = new VBox(8);
        for (String streamName : puzzle.inputs.keySet()){
            Stream stream = puzzle.inputs.get(streamName);
            VBox streamDisplay = createStreams(stream.name(),
                    stream.values().stream().mapToInt(i -> i).toArray(),
                    0);
            inputs.getChildren().add(streamDisplay);
        }

        VBox output = new VBox(8);
        for (String streamName : puzzle.outputs.keySet()){
            Stream stream = puzzle.outputs.get(streamName);
            VBox streamDisplay = createStreams(stream.name(),
                    stream.values().stream().mapToInt(i -> i).toArray(),
                    0);
            output.getChildren().add(streamDisplay);
        }

        streamsPanel.getChildren().addAll(
                streamsTitle,
                inputs,
                output);

        ScrollPane streamsScroll = new ScrollPane(streamsPanel);
        streamsScroll.setFitToWidth(true);
        streamsScroll.setMaxHeight(550);
        streamsScroll.getStyleClass().add("streams-scroll");


        VBox left = new VBox(15);
        left.getChildren().addAll(controls, streamsScroll);

        root.setLeft(left);
        streamsPanel.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        BorderPane.setMargin(left, new Insets(20));

        root.setCenter(silos);

        BorderPane.setAlignment(silos, Pos.CENTER);

        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(
                getClass().getResource("styles.css").toExternalForm()
        );

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void pulseAnimation(VBox silo){
        FadeTransition pulse = new FadeTransition(Duration.seconds(0.8), silo);
        pulse.setFromValue(1.0);
        pulse.setToValue(0.6);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    private String formatInstruction(Instruction instr){
        if (instr.type().startsWith(":") && instr.type().endsWith(":")){
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(instr.type());
        if (instr.arg1() != null){
            sb.append(" ").append(instr.arg1());
        }
        if (instr.arg2() != null){
            sb.append(" ").append(instr.arg2());
        }
        return sb.toString();
    }

    private VBox createStreams(String name, int[] values, int currentIndex){
        VBox streamBox = new VBox();
        streamBox.getStyleClass().add("stream-box");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("stream-value");

        VBox valuesList = new VBox(2);

        for (int i =0; i < values.length; i++){
            Label valueLabel = new Label(String.valueOf(values[i]));
            valueLabel.getStyleClass().add("stream-value");

            if (i == currentIndex){
                valueLabel.getStyleClass().add("stream-current");
            }
            valuesList.getChildren().add(valueLabel);
        }
        streamBox.getChildren().addAll(nameLabel, valuesList);
        return streamBox;
    }

    private void showTransfer(int fromRow, int fromCol, String dir, int val){
        // TODO: call when one silo is transferring data to another

        Label arrow =null;
        Label value = null;

        switch (dir) {
            case "RIGHT":
                if (fromCol < horizontalArrows[0].length - 1){
                    arrow = horizontalArrows[fromRow][fromCol][0];
                    value = horizontalArrows[fromRow][fromCol][1];
                }
                break;
            case "DOWN":
                if (fromRow < verticalArrows.length - 1) {
                    arrow = verticalArrows[fromRow][fromCol][0];
                    value = verticalArrows[fromRow][fromCol][1];
                }
                break;
            case "LEFT":
                if (fromCol > 0) {
                    arrow = horizontalArrows[fromRow][fromCol - 1][0];
                    value = horizontalArrows[fromRow][fromCol - 1][2];
                }
                break;
            case "UP":
                if (fromRow > 0) {
                    arrow = verticalArrows[fromRow - 1][fromCol][0];
                    value = verticalArrows[fromRow- 1][fromCol][2];
                }
                break;
        }

        if (arrow != null && value != null) {
            // Show the transfer
            arrow.getStyleClass().add("arrow-active");
            value.setText(String.valueOf(val));

            // Hide after 500ms
            PauseTransition pause = new PauseTransition(Duration.millis(500));
            final Label finalArrow = arrow;
            final Label finalVal = value;

            pause.setOnFinished(e -> {
                finalArrow.getStyleClass().remove("arrow-active");
                finalVal.setText("");
            });
            pause.play();
        }

    }

    private void updateDisplay(){
        Platform.runLater(() -> {
            int rows = puzzle.grid.length;
            int cols = puzzle.grid[0].length;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Silo silo = puzzle.grid[r][c];

                    accLabels[r][c].setText("ACC:\n" + silo.getACC());
                    bakLabels[r][c].setText("BAK:\n" + silo.getBAK());
                    pcLabels[r][c].setText("PC:\n" + silo.getPC());
                    // stateLabels[r][c].setText(silo.getState()); // TODO: when partner adds this
                }
            }
        });
    }

    @Override
    public void onRegisterChange(int row, int col, int acc, int bak, int pc) {
        // System.out.println("[GUI] Silo (" + row + "," + col + ") - ACC=" + acc + " BAK=" + bak + " PC=" + pc);
        Platform.runLater(() -> {
            accLabels[row][col].setText("ACC:\n" + acc);
            bakLabels[row][col].setText("BAK:\n" + bak);
            pcLabels[row][col].setText("PC:\n" + pc);

            // Highlight current instruction
            List<Label> instructions = instructionLabels[row][col];
            for (int i = 0; i < instructions.size(); i++) {
                if (i == pc) {
                    instructions.get(i).setStyle("-fx-background-color: #ffeb3b; -fx-text-fill: #000;");
                } else {
                    instructions.get(i).setStyle("");
                }
            }
        });
    }

    @Override
    public void onPortTransfer(int fromRow, int fromCol, String direction, int value) {
        System.out.println("[GUI] Transfer from (" + fromRow + "," + fromCol + ") " + direction + " = " + value);

        Platform.runLater(() -> {
            showTransfer(fromRow, fromCol, direction, value);
        });
    }


}


