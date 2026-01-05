package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.scandal.ScandalEvent;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.Random;

public class FeedManager {
    private final HBox tickerBox;      // Unterer Bereich (Horizontaler Stream)
    private final ScrollPane tickerScroll;
    private final Pane eventFeedPane;  // Rechter Bereich (Vertikale Liste)
    private final Random rng = new Random();

    // Animationen speichern
    private Timeline typewriterTimeline;
    private FadeTransition pulseAnimation;

    public FeedManager(HBox tickerBox, ScrollPane tickerScroll, Pane eventFeedPane) {
        this.tickerBox = tickerBox;
        this.tickerScroll = tickerScroll;
        this.eventFeedPane = eventFeedPane;

        // --- KONFIGURATION UNTEN (Horizontaler Stream) ---
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

    public void clear() {
        if (eventFeedPane != null) eventFeedPane.getChildren().clear();
        if (tickerBox != null) {
            tickerBox.getChildren().clear();
            stopAnimations();
        }
    }

    private void stopAnimations() {
        if (typewriterTimeline != null) typewriterTimeline.stop();
        if (pulseAnimation != null) pulseAnimation.stop();
    }

    public void processScandal(ScandalEvent scandal, int step) {
        if (scandal == null) return;

        // 1. Rechts: Vertikale Historie
        if (eventFeedPane != null) addToVerticalFeed(scandal, step);

        // 2. Unten: Horizontaler Live-Stream (Karte)
        if (tickerBox != null) {
            addScandalCardToTicker(scandal);
        }

        // HINWEIS: Falls du das große HUD in der Mitte AUCH willst, müsstest du
        // hier noch eine Logikweiche einbauen oder ein separates Pane übergeben.
        // Aktuell nutzen wir tickerBox als Stream-Container.
    }

    // --- TEIL 1: UNTEN (Horizontaler Stream mit Farb-Logik) ---
    private void addScandalCardToTicker(ScandalEvent scandal) {
        // A. Das Panel bauen
        HBox card = createTickerCard(scandal);

        // B. Animation (Slide-In)
        StackPane wrapper = new StackPane(card);
        wrapper.setAlignment(Pos.CENTER_LEFT);

        wrapper.setMinWidth(0);
        wrapper.setPrefWidth(0);
        Rectangle clip = new Rectangle(0, 1000);
        clip.widthProperty().bind(wrapper.widthProperty());
        wrapper.setClip(clip);

        tickerBox.getChildren().add(0, wrapper);

        double targetWidth = 450;

        Timeline slideIn = new Timeline();
        KeyValue kvWidth = new KeyValue(wrapper.prefWidthProperty(), targetWidth, Interpolator.EASE_OUT);
        KeyValue kvMin = new KeyValue(wrapper.minWidthProperty(), targetWidth, Interpolator.EASE_OUT);

        slideIn.getKeyFrames().add(new KeyFrame(Duration.millis(600), kvWidth, kvMin));
        slideIn.play();

        if (tickerScroll != null) {
            tickerScroll.setHvalue(0);
        }
    }

    private HBox createTickerCard(ScandalEvent scandal) {
        HBox alertPanel = new HBox(15);
        alertPanel.setAlignment(Pos.CENTER_LEFT);
        alertPanel.setPadding(new Insets(10, 15, 10, 15));

        alertPanel.setPrefWidth(440);
        alertPanel.setMinWidth(440);

        // --- FARB-LOGIK ---
        double strength = scandal.getScandal().getStrength(); // 0.0 bis 1.0
        boolean isCritical = strength > 0.5; // Schwelle für Rot

        String mainColor = isCritical ? "#FF3333" : "#FFA500"; // Rot oder Orange
        String badgeBg   = isCritical ? "#FF0000" : "#CC7700"; // Dunkleres Rot oder Orange für Badge
        String badgeText = isCritical ? "⚠ ALERT" : "⚠ WARNING";

        // Hintergrund leicht rötlich oder gelblich
        String bgRgba = isCritical ? "rgba(20, 0, 0, 0.9)" : "rgba(20, 15, 0, 0.9)";

        // Style anwenden
        alertPanel.setStyle("-fx-background-color: " + bgRgba + "; -fx-border-color: " + mainColor + "; -fx-border-width: 1px; -fx-background-radius: 4; -fx-border-radius: 4; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 0);");

        // 1. Badge
        Label warningBadge = new Label(badgeText);
        warningBadge.setStyle("-fx-text-fill: white; -fx-background-color: " + badgeBg + "; -fx-font-weight: bold; -fx-padding: 2 5 2 5; -fx-font-family: 'Consolas'; -fx-font-size: 10px;");

        // 2. Text
        VBox textBox = new VBox(2);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Label titleLabel = new Label(scandal.getScandal().getTitle());
        titleLabel.setStyle("-fx-text-fill: " + mainColor + "; -fx-font-weight: bold; -fx-font-size: 13px; -fx-font-family: 'Consolas';");
        titleLabel.setWrapText(false);

        Label descLabel = new Label("TARGET: " + scandal.getAffectedParty().getAbbreviation());
        descLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px; -fx-font-family: 'Consolas';");

        textBox.getChildren().addAll(titleLabel, descLabel);

        // 3. Impact Bar
        VBox impactBox = new VBox(2);
        impactBox.setAlignment(Pos.CENTER_RIGHT);
        Label impactTitle = new Label("-" + (int)(strength * 100) + "%");
        impactTitle.setStyle("-fx-text-fill: " + mainColor + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        ProgressBar impactBar = new ProgressBar(strength);
        impactBar.setPrefWidth(60);
        // Bar Farbe dynamisch
        impactBar.setStyle("-fx-accent: " + mainColor + "; -fx-control-inner-background: #222; -fx-text-box-border: transparent;");

        impactBox.getChildren().addAll(impactTitle, impactBar);

        alertPanel.getChildren().addAll(warningBadge, textBox, impactBox);

        return alertPanel;
    }

    // --- TEIL 2: RECHTS (Vertikale Historie) ---
    private void addToVerticalFeed(ScandalEvent event, int step) {
        VBox contentBox;
        ScrollPane scrollWrapper;

        if (eventFeedPane.getChildren().isEmpty()) {
            scrollWrapper = new ScrollPane();
            scrollWrapper.setFitToWidth(true);
            scrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollWrapper.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
            scrollWrapper.prefWidthProperty().bind(eventFeedPane.widthProperty());
            scrollWrapper.prefHeightProperty().bind(eventFeedPane.heightProperty());

            contentBox = new VBox();
            contentBox.getStyleClass().add("event-feed-container");
            contentBox.setPadding(new Insets(0, 10, 0, 0));
            scrollWrapper.setContent(contentBox);
            eventFeedPane.getChildren().add(scrollWrapper);
        } else {
            scrollWrapper = (ScrollPane) eventFeedPane.getChildren().get(0);
            contentBox = (VBox) scrollWrapper.getContent();
        }

        VBox newEntry = createLogEntry(event, step);
        contentBox.getChildren().add(0, newEntry);

        // Animation
        newEntry.setOpacity(0);
        newEntry.setTranslateY(-20);

        FadeTransition ft = new FadeTransition(Duration.millis(400), newEntry);
        ft.setFromValue(0); ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(400), newEntry);
        tt.setFromY(-20); tt.setToY(0);

        ParallelTransition entryAnim = new ParallelTransition();
        entryAnim.getChildren().addAll(ft, tt);
        entryAnim.play();
    }

    private VBox createLogEntry(ScandalEvent event, int step) {
        VBox entry = new VBox(2);
        entry.setStyle("-fx-padding: 5 0 10 0; -fx-border-color: #444; -fx-border-width: 0 0 1 0; -fx-border-style: dashed;");

        // Farb-Logik auch hier anwenden? Optional. Hier nutzen wir dezente Farben.
        // Falls du auch hier Rot/Orange willst, sag Bescheid. Aktuell:

        double strength = event.getScandal().getStrength();
        String impactColor = strength > 0.5 ? "#ff3333" : "#ffa500"; // Rot/Orange

        HBox header = new HBox(10);
        String id = String.format("LOG: %04d", rng.nextInt(9999));
        Label idLbl = new Label("[" + id + "]");
        idLbl.setStyle("-fx-text-fill: #555; -fx-font-family: Consolas; -fx-font-size: 10px;");
        Label timeLbl = new Label("TICK: " + step);
        timeLbl.setStyle("-fx-text-fill: #888; -fx-font-family: Consolas; -fx-font-size: 10px;");
        header.getChildren().addAll(idLbl, timeLbl);

        Label msg = new Label(event.getScandal().getTitle());
        msg.setStyle("-fx-text-fill: #e0e0e0; -fx-font-family: Consolas; -fx-font-weight: bold; -fx-font-size: 12px;");
        msg.setWrapText(true);

        Label target = new Label(">>> TARGET: " + event.getAffectedParty().getName());
        target.setStyle("-fx-text-fill: #D4AF37; -fx-font-family: Consolas; -fx-font-size: 11px;");

        Label impact = new Label("IMPACT: -" + (int)(strength * 100) + "% STABILITY");
        impact.setStyle("-fx-text-fill: " + impactColor + "; -fx-font-family: Consolas; -fx-font-weight: bold; -fx-font-size: 11px;");

        entry.getChildren().addAll(header, msg, target, impact);
        return entry;
    }
}