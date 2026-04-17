package gui;


import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

import javafx.scene.control.Button;


public class MainWindow extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a");

        GridPane silos = new GridPane();
        silos.setAlignment(Pos.CENTER);
        silos.setPadding(new Insets(10, 0, 0, 30));

        silos.setHgap(60);
        silos.setVgap(60);
        for (int r = 0; r < 3; r++){
            for (int c = 0; c < 4; c++){
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


                Label instr = new Label("MOVE UP");
                instr.getStyleClass().add("instr-text");
                instructionsBox.getChildren().add(instr);

                // create register labels
                Label accLabel = new Label("ACC:" + "\n" + "0");
                accLabel.getStyleClass().add("silo-register");

                Label bakLabel = new Label("BAK:" + "\n" + "0");
                bakLabel.getStyleClass().add("silo-register");

                Label pcLabel = new Label("PC:" + "\n" + "0");
                pcLabel.getStyleClass().add("silo-register");

                Label stateLabel = new Label("STATE:" + "\n" + "IDLE");
                stateLabel.getStyleClass().add("silo-register");

               registerBox.getChildren().addAll(accLabel, bakLabel, pcLabel, stateLabel);

                silo.getChildren().addAll(registerBox, instructionsBox);
                silo.setAlignment(Pos.TOP_LEFT);
                silos.add(silo,c, r);
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

        Button pause = new Button("PAUSE ⏸");
        pause.getStyleClass().add("control-button");

        Button stop = new Button("STOP ⃠");
        stop.getStyleClass().add("control-button");

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
}
