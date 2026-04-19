package gui;


import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

import javafx.scene.control.Button;
import javafx.util.Duration;
import logic.Instruction;
import logic.Puzzle;
import logic.Silo;
import parser.Parser;

import java.io.File;
import java.util.Scanner;


public class MainWindow extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Scanner scanner = new Scanner(new File("test_input/simple_example1.txt"));

        Puzzle puzzle = Parser.parse(scanner);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a");

        GridPane silos = new GridPane();
        silos.setAlignment(Pos.CENTER);
        silos.setPadding(new Insets(10, 0, 0, 30));

        silos.setHgap(60);
        silos.setVgap(60);
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

                Silo siloData = puzzle.grid[r][c];



                for (Instruction instr : siloData.getInstructions()){
                    String instructionText = formatInstruction(instr);
                    Label instrLabel = new Label(instructionText);
                    instrLabel.getStyleClass().add("instr-text");
                    instructionsBox.getChildren().add(instrLabel);
                }

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

                silo.getChildren().addAll(registerBox, instructionsBox);
                silo.setAlignment(Pos.TOP_LEFT);
                silos.add(silo,c, r);
                pulseAnimation(silo);


                // TODO: add arrows to represent ports
            }
        }

        VBox streamsPanel = new VBox(5);
        streamsPanel.getStyleClass().add("controls");
        streamsPanel.setPadding(new Insets(20));
        streamsPanel.setAlignment(Pos.TOP_CENTER);

        Label streamsTitle = new Label("I/O STREAMS");
        streamsTitle.getStyleClass().add("silo-register");

        streamsPanel.getChildren().addAll(streamsTitle);
        root.setLeft(streamsPanel);
        BorderPane.setMargin(streamsPanel, new Insets(20));

        VBox controls = new VBox(15);
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

        controls.getStyleClass().add("controls");
        controls.getChildren().addAll(start, pause, stop);
        streamsPanel.getChildren().add(controls);

        root.setLeft(streamsPanel);
        root.setCenter(silos);

        BorderPane.setAlignment(silos, Pos.CENTER);

        Scene scene = new Scene(root, 1300, 900);
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

}
