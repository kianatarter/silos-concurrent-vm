package GUI;


import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Pos;

import java.awt.*;


public class MainWindow extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a1a");

        GridPane silos = new GridPane();
        silos.setAlignment(Pos.CENTER);
        silos.setPadding(new Insets(10, 0, 0, 200));

        silos.setHgap(60);
        silos.setVgap(60);
        for (int r = 0; r < 3; r++){
            for (int c = 0; c < 3; c++){
                VBox silo = new VBox(30);
                silo.getStyleClass().add("silo-box");
                silos.add(silo,r,c);
            }
        }

        VBox controls = new VBox(5);
        Label controlTitle = new Label("CONTROLS");
        controls.setAlignment(Pos.BASELINE_LEFT);
        controls.setPadding(new Insets(0, 0, 300, 0));
        controls.getStyleClass().add("controls");

        root.setLeft(controls);
        BorderPane.setAlignment(controls, Pos.CENTER);

        root.setCenter(silos);

        BorderPane.setAlignment(silos, Pos.CENTER);


        Scene scene = new Scene(root, 1300, 1000);
        scene.getStylesheets().add(
                getClass().getResource("styles.css").toExternalForm()
        );

        primaryStage.setScene(scene);
        primaryStage.show();









    }
}
