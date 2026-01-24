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
 * Verwaltet die Visualisierung des Nachrichten-Feeds in der Benutzeroberfläche.
 * Steuert sowohl den horizontalen Ticker als auch das vertikale Ereignis-Log.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class FeedManager {

    // ========================================
    // Statische Variablen
    // ========================================

    private static final double TICKER_CARD_WIDTH = 450.0;
    private static final int ANIMATION_DURATION_MS = 600;
    private static final double CRITICAL_THRESHOLD = 0.5;

    // ========================================
    // Instanzvariablen
    // ========================================

    private final HBox tickerBox;
    private final ScrollPane tickerScroll;
    private final Pane eventFeedPane;

    // ========================================
    // Konstruktoren
    // ========================================

    /**
     * Initialisiert den Manager mit den notwendigen UI-Komponenten.
     *
     * @param tickerBox Horizontale Box für Ticker-Karten.
     * @param tickerScroll ScrollPane für den Ticker.
     * @param eventFeedPane Pane für das vertikale Log.
     */
    public FeedManager(HBox tickerBox, ScrollPane tickerScroll, Pane eventFeedPane) {
        this.tickerBox = tickerBox;
        this.tickerScroll = tickerScroll;
        this.eventFeedPane = eventFeedPane;
        configureTickerArea();
    }

    // ========================================
    // Business-Logik-Methoden
    // ========================================

    public void clear() {
        if (eventFeedPane != null) eventFeedPane.getChildren().clear();
        if (tickerBox != null) tickerBox.getChildren().clear();
    }

    /**
     * Verarbeitet ein Skandal-Ereignis und fügt es den sichtbaren Feeds hinzu.
     *
     * @param scandal Das zu visualisierende Skandal-Ereignis.
     * @param step Der aktuelle Simulationsschritt.
     */
    public void processScandal(ScandalEvent scandal, int step) {
        if (scandal == null) return;
        if (eventFeedPane != null) addToVerticalFeed(scandal, step);
        if (tickerBox != null) addScandalCardToTicker(scandal);
    }

    // ========================================
    // Hilfsmethoden (Utility)
    // ========================================

    private void configureTickerArea() {
        if (this.tickerBox != null) {
            this.tickerBox.setAlignment(Pos.CENTER_LEFT);
            this.tickerBox.setSpacing(10);
            this.tickerBox.setPadding(new Insets(0, 10, 0, 10));
            this.tickerBox.getStyleClass().add("ticker-box");
        }
        if (this.tickerScroll != null) {
            this.tickerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            this.tickerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            this.tickerScroll.setFitToHeight(true);
            this.tickerScroll.getStyleClass().add("ticker-scroll");
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
        alertPanel.setPrefWidth(TICKER_CARD_WIDTH - 10);
        alertPanel.getStyleClass().add("scandal-card");

        boolean isCritical = scandal.scandal().strength() > CRITICAL_THRESHOLD;
        if (isCritical) alertPanel.getStyleClass().add("critical");

        Label warningBadge = new Label(isCritical ? bundle.getString("feed.alert") : bundle.getString("feed.warning"));
        warningBadge.getStyleClass().add("scandal-badge");
        if (isCritical) warningBadge.getStyleClass().add("critical");

        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLabel = new Label(scandal.scandal().title());
        titleLabel.getStyleClass().add("scandal-title");
        if (isCritical) titleLabel.getStyleClass().add("critical");

        Label descLabel = new Label(bundle.getString("feed.target") + " " + scandal.affectedParty().getAbbreviation());
        descLabel.getStyleClass().add("scandal-desc");
        textBox.getChildren().addAll(titleLabel, descLabel);

        VBox impactBox = new VBox(2);
        impactBox.setAlignment(Pos.CENTER_RIGHT);
        Label impactTitle = new Label("-" + (int)(scandal.scandal().strength() * 100) + "%");
        impactTitle.getStyleClass().add("scandal-impact-text");
        if (isCritical) impactTitle.getStyleClass().add("critical");

        ProgressBar impactBar = new ProgressBar(scandal.scandal().strength());
        impactBar.setPrefWidth(60);
        impactBar.getStyleClass().add("scandal-impact-bar");
        if (isCritical) impactBar.getStyleClass().add("critical");

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
        scrollWrapper.getStyleClass().add("ticker-scroll");
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
        entry.getStyleClass().add("log-entry");

        HBox header = new HBox(10);
        Label idLbl = new Label(String.format(bundle.getString("feed.log_prefix"), String.format("%04d", ThreadLocalRandom.current().nextInt(9999))));
        idLbl.getStyleClass().add("log-id");

        Label timeLbl = new Label(bundle.getString("feed.tick") + " " + step);
        timeLbl.getStyleClass().add("log-time");
        header.getChildren().addAll(idLbl, timeLbl);

        Label msg = new Label();
        msg.getStyleClass().add("log-message");
        VisualFX.playTypewriterAnimation(msg, event.scandal().title(), 15);

        Label target = new Label(">>> " + bundle.getString("feed.target") + " " + event.affectedParty().getName());
        target.getStyleClass().add("log-target");

        Label impact = new Label(bundle.getString("feed.impact") + " -" + (int)(event.scandal().strength() * 100) + "% " + bundle.getString("feed.stability"));
        impact.getStyleClass().add("log-impact");
        if (event.scandal().strength() > CRITICAL_THRESHOLD) impact.getStyleClass().add("critical");

        entry.getChildren().addAll(header, msg, target, impact);
        return entry;
    }
}