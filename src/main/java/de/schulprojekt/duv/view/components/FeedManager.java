package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.scandal.ScandalEvent;
import de.schulprojekt.duv.view.util.VisualFX; // Import added
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages the news feed visualizations for scandal events.
 * Controls the horizontal ticker (bottom) and the vertical history feed (right side).
 *
 * <p>This class handles the processing of scandal events and displays them
 * through animated cards in the ticker area and log entries in the vertical feed.</p>
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class FeedManager {

    // --- Constants: Layout & Animation ---
    private static final double TICKER_CARD_WIDTH = 450.0;
    private static final int ANIMATION_DURATION_MS = 600;

    // --- Constants: Colors & Styles ---
    private static final double CRITICAL_THRESHOLD = 0.5;

    private static final String COLOR_CRITICAL_RED = "#FF3333";
    private static final String COLOR_WARNING_ORANGE = "#FFA500";
    private static final String COLOR_BG_CRITICAL = "rgba(20, 0, 0, 0.9)";
    private static final String COLOR_BG_WARNING = "rgba(20, 15, 0, 0.9)";

    private static final String FONT_CONSOLAS = "Consolas";

    // --- Fields ---
    private final HBox tickerBox;      // Bottom area (Horizontal stream)
    private final ScrollPane tickerScroll;
    private final Pane eventFeedPane;  // Right area (Vertical list)

    // --- Constructor ---

    public FeedManager(HBox tickerBox, ScrollPane tickerScroll, Pane eventFeedPane) {
        this.tickerBox = tickerBox;
        this.tickerScroll = tickerScroll;
        this.eventFeedPane = eventFeedPane;

        configureTickerArea();
    }

    // --- Public API ---

    public void clear() {
        if (eventFeedPane != null) {
            eventFeedPane.getChildren().clear();
        }
        if (tickerBox != null) {
            tickerBox.getChildren().clear();
        }
    }

    public void processScandal(ScandalEvent scandal, int step) {
        if (scandal == null) return;

        // 1. Right: Vertical History
        if (eventFeedPane != null) {
            addToVerticalFeed(scandal, step);
        }

        // 2. Bottom: Horizontal Live Ticker
        if (tickerBox != null) {
            addScandalCardToTicker(scandal);
        }
    }

    // --- Private Helper: Initialization ---

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

    // --- Section 1: Bottom Ticker (Horizontal) ---

    private void addScandalCardToTicker(ScandalEvent scandal) {
        // A. Build Panel
        HBox card = createTickerCard(scandal);

        // B. Animation (Slide-In)
        StackPane wrapper = new StackPane(card);
        wrapper.setAlignment(Pos.CENTER_LEFT);

        // Start with 0 width for animation
        wrapper.setMinWidth(0);
        wrapper.setPrefWidth(0);

        // Clip content during animation
        Rectangle clip = new Rectangle(0, 1000);
        clip.widthProperty().bind(wrapper.widthProperty());
        wrapper.setClip(clip);

        // Add to front (Left side)
        tickerBox.getChildren().addFirst(wrapper);

        // Animate Width
        Timeline slideIn = new Timeline();
        KeyValue kvWidth = new KeyValue(wrapper.prefWidthProperty(), TICKER_CARD_WIDTH, Interpolator.EASE_OUT);
        KeyValue kvMin = new KeyValue(wrapper.minWidthProperty(), TICKER_CARD_WIDTH, Interpolator.EASE_OUT);

        slideIn.getKeyFrames().add(new KeyFrame(Duration.millis(ANIMATION_DURATION_MS), kvWidth, kvMin));
        slideIn.play();

        // Reset Scroll to see new item
        if (tickerScroll != null) {
            tickerScroll.setHvalue(0);
        }
    }

    private HBox createTickerCard(ScandalEvent scandal) {
        HBox alertPanel = new HBox(15);
        alertPanel.setAlignment(Pos.CENTER_LEFT);
        alertPanel.setPadding(new Insets(10, 15, 10, 15));
        alertPanel.setPrefWidth(TICKER_CARD_WIDTH - 10);
        alertPanel.setMinWidth(TICKER_CARD_WIDTH - 10);

        // --- Color Logic ---
        double strength = scandal.scandal().strength();
        boolean isCritical = strength > CRITICAL_THRESHOLD;

        String mainColor = isCritical ? COLOR_CRITICAL_RED : COLOR_WARNING_ORANGE;
        String badgeBg   = isCritical ? "#FF0000" : "#CC7700";
        String badgeText = isCritical ? "⚠ ALERT" : "⚠ WARNING";
        String bgRgba    = isCritical ? COLOR_BG_CRITICAL : COLOR_BG_WARNING;

        // Apply Style
        alertPanel.setStyle(String.format(
                "-fx-background-color: %s; -fx-border-color: %s; -fx-border-width: 1px; " +
                        "-fx-background-radius: 4; -fx-border-radius: 4; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 0);",
                bgRgba, mainColor
        ));

        // 1. Badge
        Label warningBadge = new Label(badgeText);
        warningBadge.setStyle(String.format(
                "-fx-text-fill: white; -fx-background-color: %s; -fx-font-weight: bold; " +
                        "-fx-padding: 2 5 2 5; -fx-font-family: '%s'; -fx-font-size: 10px;",
                badgeBg, FONT_CONSOLAS
        ));

        // 2. Text Content
        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLabel = new Label(scandal.scandal().title());
        titleLabel.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 13px; -fx-font-family: '%s';",
                mainColor, FONT_CONSOLAS
        ));
        titleLabel.setWrapText(false);

        Label descLabel = new Label("TARGET: " + scandal.affectedParty().getAbbreviation());
        descLabel.setStyle(String.format(
                "-fx-text-fill: #aaa; -fx-font-size: 11px; -fx-font-family: '%s';",
                FONT_CONSOLAS
        ));

        textBox.getChildren().addAll(titleLabel, descLabel);

        // 3. Impact Bar
        VBox impactBox = new VBox(2);
        impactBox.setAlignment(Pos.CENTER_RIGHT);

        Label impactTitle = new Label("-" + (int)(strength * 100) + "%");
        impactTitle.setStyle(String.format(
                "-fx-text-fill: %s; -fx-font-weight: bold; -fx-font-size: 12px;",
                mainColor
        ));

        ProgressBar impactBar = new ProgressBar(strength);
        impactBar.setPrefWidth(60);
        impactBar.setStyle(String.format(
                "-fx-accent: %s; -fx-control-inner-background: #222; -fx-text-box-border: transparent;",
                mainColor
        ));

        impactBox.getChildren().addAll(impactTitle, impactBar);

        alertPanel.getChildren().addAll(warningBadge, textBox, impactBox);
        return alertPanel;
    }

    // --- Section 2: Vertical Feed (Right Side) ---

    private void addToVerticalFeed(ScandalEvent event, int step) {
        VBox contentBox;

        // Initialize ScrollPane/VBox on first usage if needed
        if (eventFeedPane.getChildren().isEmpty()) {
            contentBox = initializeVerticalFeedStructure();
        } else {
            ScrollPane scrollWrapper = (ScrollPane) eventFeedPane.getChildren().getFirst();
            contentBox = (VBox) scrollWrapper.getContent();
        }

        VBox newEntry = createLogEntry(event, step);
        contentBox.getChildren().addFirst(newEntry);

        // Animation: Fade In + Slide Down
        newEntry.setOpacity(0);
        newEntry.setTranslateY(-20);

        FadeTransition ft = new FadeTransition(Duration.millis(400), newEntry);
        ft.setFromValue(0);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(400), newEntry);
        tt.setFromY(-20);
        tt.setToY(0);

        ParallelTransition entryAnim = new ParallelTransition(ft, tt);
        entryAnim.play();
    }

    private VBox initializeVerticalFeedStructure() {
        ScrollPane scrollWrapper = new ScrollPane();
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollWrapper.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");

        scrollWrapper.prefWidthProperty().bind(eventFeedPane.widthProperty());
        scrollWrapper.prefHeightProperty().bind(eventFeedPane.heightProperty());

        VBox contentBox = new VBox();
        contentBox.getStyleClass().add("event-feed-container");
        contentBox.setPadding(new Insets(0, 10, 0, 0));

        scrollWrapper.setContent(contentBox);
        eventFeedPane.getChildren().add(scrollWrapper);

        return contentBox;
    }

    private VBox createLogEntry(ScandalEvent event, int step) {
        VBox entry = new VBox(2);
        entry.setStyle("-fx-padding: 5 0 10 0; -fx-border-color: #444; -fx-border-width: 0 0 1 0; -fx-border-style: dashed;");

        double strength = event.scandal().strength();
        String impactColor = strength > CRITICAL_THRESHOLD ? COLOR_CRITICAL_RED : COLOR_WARNING_ORANGE;

        // Header (ID + Timestamp)
        HBox header = new HBox(10);
        String id = String.format("LOG: %04d", ThreadLocalRandom.current().nextInt(9999));

        Label idLbl = new Label("[" + id + "]");
        idLbl.setStyle("-fx-text-fill: #555; -fx-font-family: Consolas; -fx-font-size: 10px;");

        Label timeLbl = new Label("TICK: " + step);
        timeLbl.setStyle("-fx-text-fill: #888; -fx-font-family: Consolas; -fx-font-size: 10px;");
        header.getChildren().addAll(idLbl, timeLbl);

        // Message
        Label msg = new Label(); // Empty initially for typewriter
        msg.setStyle("-fx-text-fill: #e0e0e0; -fx-font-family: Consolas; -fx-font-weight: bold; -fx-font-size: 12px;");
        msg.setWrapText(true);

        // --- APPLIED EFFECT: TYPEWRITER ---
        VisualFX.playTypewriterAnimation(msg, event.scandal().title(), 15);

        // Details
        Label target = new Label(">>> TARGET: " + event.affectedParty().getName());
        target.setStyle("-fx-text-fill: #D4AF37; -fx-font-family: Consolas; -fx-font-size: 11px;");

        Label impact = new Label("IMPACT: -" + (int)(strength * 100) + "% STABILITY");
        impact.setStyle("-fx-text-fill: " + impactColor + "; -fx-font-family: Consolas; -fx-font-weight: bold; -fx-font-size: 11px;");

        entry.getChildren().addAll(header, msg, target, impact);
        return entry;
    }
}