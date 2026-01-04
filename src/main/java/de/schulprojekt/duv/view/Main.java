package de.schulprojekt.duv.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    // Startpunkt ist jetzt der StartView, nicht mehr das Dashboard
    private static final String START_VIEW_FXML = "/de/schulprojekt/duv/view/StartView.fxml";
    private static final String CSS_PATH = "/de/schulprojekt/duv/style.css";
    private static final String APP_TITLE = "Das Unberechenbare Volk";

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL fxmlURL = getClass().getResource(START_VIEW_FXML);
        if (fxmlURL == null) {
            throw new IOException("FXML resource not found: " + START_VIEW_FXML);
        }

        FXMLLoader loader = new FXMLLoader(fxmlURL);
        Parent root = loader.load();

        // Fenstergröße initial festlegen
        Scene scene = new Scene(root, 1200, 750);

        URL cssURL = getClass().getResource(CSS_PATH);
        if (cssURL != null) {
            scene.getStylesheets().add(cssURL.toExternalForm());
        }

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true); // Responsive Design zulassen

        // Sauberer Shutdown
        primaryStage.setOnCloseRequest(e -> {
            System.exit(0);
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}