package gui;


import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
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
import logic.Instruction;
import logic.Puzzle;
import logic.Silo;
import logic.Stream;
import parser.Parser;

import java.io.File;
import java.util.List;
import java.util.Scanner;


public class MainWindow extends Application {
    private Label[][][] horizontalArrows;
    private Label[][][] verticalArrows;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Scanner scanner = new Scanner(new File("test_input/simple_example1.txt"));
        Puzzle puzzle = Parser.parse(scanner);


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

        horizontalArrows = new Label[rows][cols][3];
        verticalArrows = new Label[rows][cols][3];


        for (int r = 0; r < puzzle.grid.length; r++){
            for (int c = 0; c < puzzle.grid[0].length; c++){

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

                // add instructions for each silo
                for (Instruction instr : siloData.getInstructions()){
                    String instructionText = formatInstruction(instr);
                    Label instrLabel = new Label(instructionText);
                    instrLabel.getStyleClass().add("instr-text");
                    instructionsBox.getChildren().add(instrLabel);
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

                Label stateLabel = new Label("STATE:" + "\n" + "IDLE");
                stateLabel.getStyleClass().add("silo-register");

               registerBox.getChildren().addAll(accLabel, bakLabel, pcLabel, stateLabel);

                silo.getChildren().addAll(registerBox, instructionsScroll);
                silo.setAlignment(Pos.TOP_LEFT);
                silos.add(silo,c * 2, r * 2);
                pulseAnimation(silo);


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
                }


                // DOWN ARROW
                if (r < rows - 1){
                    VBox arrowBox = new VBox(2);
                    arrowBox.setAlignment(Pos.CENTER);
                    arrowBox.setMinHeight(20);

                    Label arrowSymbol = new Label("↕");
                    arrowSymbol.getStyleClass().add("arrow-symbol");

                    Label leftValue = new Label("");
                    leftValue.getStyleClass().add("arrow-value");

                    Label rightValue = new Label("");
                    rightValue.getStyleClass().add("arrow-value");

                    arrowBox.getChildren().addAll(leftValue, arrowSymbol, rightValue);

                    // Add arrow at ODD row position (r*2 + 1)
                    silos.add(arrowBox, c * 2, (r * 2) + 1);

                    verticalArrows[r][c][0] = arrowSymbol;
                    verticalArrows[r][c][1] = rightValue;
                }

            }
        }



        VBox controls = new VBox(15);
        Label controlsTitle = new Label("CONTROLS");
        controlsTitle.getStyleClass().add("instr-text");
        controls.setAlignment(Pos.BOTTOM_CENTER);
        controls.setPadding(new Insets(20));

        Button start = new Button("START ▶");
        start.getStyleClass().add("control-button");

        // TODO: add start button implementation
        start.setOnAction(event -> {
            System.out.println("START button pressed");
        });

        Button pause = new Button("PAUSE ⏸");
        pause.getStyleClass().add("control-button");
        // TODO: add pause button implementation
        pause.setOnAction(event -> {
            System.out.println("PAUSE button pressed");
        });

        Button stop = new Button("STOP ⃠");
        stop.getStyleClass().add("control-button");
        // TODO: add stop button implementation
        stop.setOnAction(event -> {
            System.out.println("STOP button pressed");
        });

        Label delayLabel = new Label("Delay (sec):");
        delayLabel.getStyleClass().add("instr-text");

        Button step = new Button("STEP ≫");
        step.getStyleClass().add("control-button");
        // TODO: add step button implementation
        step.setOnAction(event -> {
            System.out.println("STEP button pressed");
        });


        TextField delayField = new TextField("1");
        delayField.getStyleClass().add("delay-field");
        delayField.setMaxWidth(160);
        delayField.setPromptText("0-10");

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

        // TEST: Show arrow after 2 seconds
        PauseTransition test = new PauseTransition(Duration.seconds(2));
        test.setOnFinished(e -> {
            showTransfer(0, 0, "RIGHT", 42);  // Test horizontal arrow

            PauseTransition test2 = new PauseTransition(Duration.seconds(1));
            test2.setOnFinished(e2 -> showTransfer(0, 0, "DOWN", 99));  // Test vertical arrow
            test2.play();
        });
        test.play();

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
                    value = horizontalArrows[fromRow][fromCol - 1][1];
                }
                break;
            case "UP":
                if (fromRow > 0) {
                    arrow = verticalArrows[fromRow - 1][fromCol][0];
                    value = verticalArrows[fromRow- 1][fromCol][1];
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


}


