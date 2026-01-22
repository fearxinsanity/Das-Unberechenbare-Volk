package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.view.Main;
import de.schulprojekt.duv.view.util.VisualFX;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ResourceBundle;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the news feed visualizations in the UI.
 * @author Nico Hoffmann
 * @version 1.1
 */
public class FeedManager {

    // ========================================
    // Static Variables
    // ========================================

    private static final double TICKER_CARD_WIDTH = 450.0;
    private static final int ANIMATION_DURATION_MS = 600;
    private static final double CRITICAL_THRESHOLD = 0.5;

    private static final String COLOR_CRITICAL_RED = "#FF3333";
    private static final String COLOR_WARNING_ORANGE = "#FFA500";
    private static final String COLOR_BG_CRITICAL = "rgba(20, 0, 0, 0.9)";
    private static final String COLOR_BG_WARNING = "rgba(20, 15, 0, 0.9)";
    private static final String FONT_CONSOLAS = "Consolas";

    // ========================================
    // Instance Variables
    // ========================================

    private final HBox tickerBox;
    private final ScrollPane tickerScroll;
    private final Pane eventFeedPane;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Initializes the manager with UI components.
     * @param tickerBox horizontal ticker box
     * @param tickerScroll ticker scroll pane
     * @param eventFeedPane vertical event feed pane
     */
    public FeedManager(HBox tickerBox, ScrollPane tickerScroll, Pane eventFeedPane) {
        this.tickerBox = tickerBox;
        this.tickerScroll = tickerScroll;
        this.eventFeedPane = eventFeedPane;
        configureTickerArea();
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    public void clear() {
        if (eventFeedPane != null) eventFeedPane.getChildren().clear();
        if (tickerBox != null) tickerBox.getChildren().clear();
    }

    public void processScandal(ScandalEvent scandal, int step) {
        if (scandal == null) return;
        if (eventFeedPane != null) addToVerticalFeed(scandal, step);
        if (tickerBox != null) addScandalCardToTicker(scandal);
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void configureTickerArea() {
        if (this.tickerBox != null) {
            this.tickerBox.setAlignment(Pos.CENTER_LEFT);
            this.tickerBox.setSpacing(10);
            this.tickerBox.setPadding(new Insets(0, 10, 0, 10));
            this.tickerBox.setStyle("-fx-background-color: transparent;");
        }
        if (this.tickerScroll != null) {
            this.tickerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            this.tickerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            this.tickerScroll.setFitToHeight(true);
            this.tickerScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        }
    }

    private void addScandalCardToTicker(ScandalEvent scandal) {
        HBox card = createTickerCard(scandal);
        StackPane wrapper = new StackPane(card);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setMinWidth(0);
        wrapper.setPrefWidth(0);

        Rectangle clip = new Rectangle(0, 1000);
        clip.widthProperty().bind(wrapper.widthProperty());
        wrapper.setClip(clip);

        tickerBox.getChildren().addFirst(wrapper);

        Timeline slideIn = new Timeline();
        slideIn.getKeyFrames().add(new KeyFrame(Duration.millis(ANIMATION_DURATION_MS),
                new KeyValue(wrapper.prefWidthProperty(), TICKER_CARD_WIDTH, Interpolator.EASE_OUT),
                new KeyValue(wrapper.minWidthProperty(), TICKER_CARD_WIDTH, Interpolator.EASE_OUT)));
        slideIn.play();

        if (tickerScroll != null) tickerScroll.setHvalue(0);
    }

    private HBox createTickerCard(ScandalEvent scandal) {
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());
        HBox alertPanel = new HBox(15);
        alertPanel.setAlignment(Pos.CENTER_LEFT);
        alertPanel.setPadding(new Insets(10, 15, 10, 15));
        alertPanel.setPrefWidth(TICKER_CARD_WIDTH - 10);
        alertPanel.setMinWidth(TICKER_CARD_WIDTH - 10);

        double strength = scandal.scandal().strength();
        boolean isCritical = strength > CRITICAL_THRESHOLD;
        String mainColor = isCritical ? COLOR_CRITICAL_RED : COLOR_WARNING_ORANGE;
        String badgeBg = isCritical ? "#FF0000" : "#CC7700";
        String bgRgba = isCritical ? COLOR_BG_CRITICAL : COLOR_BG_WARNING;

        alertPanel.setStyle(String.format("-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1px; -fx-background-radius: 4; -fx-border-radius: 4;", bgRgba, mainColor));

        Label warningBadge = new Label(isCritical ? bundle.getString("feed.alert") : bundle.getString("feed.warning"));
        warningBadge.setStyle(String.format("-fx-text-fill: white; -fx-background-color: %s; -fx-font-weight: bold; -fx-padding: 2 5 2 5; -fx-font-family: '%s'; -fx-font-size: 10px;", badgeBg, FONT_CONSOLAS));

        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLabel = new Label(scandal.scandal().title());
        titleLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 13px; -fx-font-family: '%s';", mainColor, FONT_CONSOLAS));
        Label descLabel = new Label(bundle.getString("feed.target") + " " + scandal.affectedParty().getAbbreviation());
        descLabel.setStyle(String.format("-fx-text-fill: #aaa; -fx-font-family: '%s';", FONT_CONSOLAS));
        textBox.getChildren().addAll(titleLabel, descLabel);

        VBox impactBox = new VBox(2);
        impactBox.setAlignment(Pos.CENTER_RIGHT);
        Label impactTitle = new Label("-" + (int)(strength * 100) + "%");
        impactTitle.setStyle(String.format("-fx-text-fill: %s; -fx-font-weight: bold;", mainColor));
        ProgressBar impactBar = new ProgressBar(strength);
        impactBar.setPrefWidth(60);
        impactBar.setStyle(String.format("-fx-accent: %s; -fx-control-inner-background: #222;", mainColor));
        impactBox.getChildren().addAll(impactTitle, impactBar);

        alertPanel.getChildren().addAll(warningBadge, textBox, impactBox);
        return alertPanel;
    }

    private void addToVerticalFeed(ScandalEvent event, int step) {
        VBox contentBox;
        if (eventFeedPane.getChildren().isEmpty()) {
            contentBox = initializeVerticalFeedStructure();
        } else {
            contentBox = (VBox) ((ScrollPane) eventFeedPane.getChildren().getFirst()).getContent();
        }

        VBox newEntry = createLogEntry(event, step);
        contentBox.getChildren().addFirst(newEntry);
        newEntry.setOpacity(0);
        newEntry.setTranslateY(-20);

        ParallelTransition anim = new ParallelTransition(
                new FadeTransition(Duration.millis(400), newEntry),
                new TranslateTransition(Duration.millis(400), newEntry)
        );
        ((FadeTransition)anim.getChildren().get(0)).setToValue(1);
        ((TranslateTransition)anim.getChildren().get(1)).setToY(0);
        anim.play();
    }

    private VBox initializeVerticalFeedStructure() {
        ScrollPane scrollWrapper = new ScrollPane();
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollWrapper.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollWrapper.prefWidthProperty().bind(eventFeedPane.widthProperty());
        scrollWrapper.prefHeightProperty().bind(eventFeedPane.heightProperty());

        VBox contentBox = new VBox();
        contentBox.setPadding(new Insets(0, 10, 0, 0));
        scrollWrapper.setContent(contentBox);
        eventFeedPane.getChildren().add(scrollWrapper);
        return contentBox;
    }

    private VBox createLogEntry(ScandalEvent event, int step) {
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());
        VBox entry = new VBox(2);
        entry.setStyle("-fx-padding: 5 0 10 0; -fx-border-color: #444; -fx-border-width: 0 0 1 0; -fx-border-style: dashed;");

        HBox header = new HBox(10);
        Label idLbl = new Label(String.format(bundle.getString("feed.log_prefix"), String.format("%04d", ThreadLocalRandom.current().nextInt(9999))));
        idLbl.setStyle("-fx-text-fill: #555; -fx-font-family: Consolas;");
        Label timeLbl = new Label(bundle.getString("feed.tick") + " " + step);
        timeLbl.setStyle("-fx-text-fill: #888; -fx-font-family: Consolas;");
        header.getChildren().addAll(idLbl, timeLbl);

        Label msg = new Label();
        msg.setStyle("-fx-text-fill: #e0e0e0; -fx-font-family: Consolas; -fx-font-weight: bold;");
        VisualFX.playTypewriterAnimation(msg, event.scandal().title(), 15);

        Label target = new Label(">>> " + bundle.getString("feed.target") + " " + event.affectedParty().getName());
        target.setStyle("-fx-text-fill: #D4AF37; -fx-font-family: Consolas;");
        Label impact = new Label(bundle.getString("feed.impact") + " -" + (int)(event.scandal().strength() * 100) + "% " + bundle.getString("feed.stability"));
        impact.setStyle(String.format("-fx-text-fill: %s; -fx-font-family: Consolas; -fx-font-weight: bold;", event.scandal().strength() > CRITICAL_THRESHOLD ? COLOR_CRITICAL_RED : COLOR_WARNING_ORANGE));

        entry.getChildren().addAll(header, msg, target, impact);
        return entry;
    }
}