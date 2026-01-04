package de.schulprojekt.duv.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    private static final String START_VIEW_FXML = "/de/schulprojekt/duv/view/StartView.fxml";
    // NEU: Zwei CSS-Dateien statt einer
    private static final String COMMON_CSS = "/de/schulprojekt/duv/common.css";
    private static final String START_CSS = "/de/schulprojekt/duv/start.css";
    private static final String APP_TITLE = "Das Unberechenbare Volk";

    @Override
    public void start(Stage primaryStage) throws IOException {
        URL fxmlURL = getClass().getResource(START_VIEW_FXML);
        if (fxmlURL == null) {
            throw new IOException("FXML resource not found: " + START_VIEW_FXML);
        }

        FXMLLoader loader = new FXMLLoader(fxmlURL);
        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 750);

        // CSS: Common + Start Screen laden
        URL commonUrl = getClass().getResource(COMMON_CSS);
        URL startUrl = getClass().getResource(START_CSS);

        if (commonUrl != null) scene.getStylesheets().add(commonUrl.toExternalForm());
        if (startUrl != null) scene.getStylesheets().add(startUrl.toExternalForm());

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);

        primaryStage.setOnCloseRequest(e -> System.exit(0));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}