package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.scandal.ScandalEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import java.util.Random;

public class FeedManager {
    private final HBox tickerBox;
    private final ScrollPane tickerScroll;
    private final Pane eventFeedPane;
    private final Random rng = new Random();

    public FeedManager(HBox tickerBox, ScrollPane tickerScroll, Pane eventFeedPane) {
        this.tickerBox = tickerBox; this.tickerScroll = tickerScroll; this.eventFeedPane = eventFeedPane;
    }

    public void clear() {
        if (eventFeedPane != null) eventFeedPane.getChildren().clear();
        if (tickerBox != null) tickerBox.getChildren().clear();
    }

    public void processScandal(ScandalEvent scandal, int step) {
        if (scandal == null) return;
        if (eventFeedPane != null) addToVerticalFeed(scandal, step);
        // Ticker kann ähnlich angepasst oder weggelassen werden, hier Fokus auf Log
    }

    private void addToVerticalFeed(ScandalEvent event, int step) {
        VBox fb;
        if (eventFeedPane.getChildren().isEmpty()) {
            fb = new VBox(); fb.getStyleClass().add("event-feed-container");
            fb.prefWidthProperty().bind(eventFeedPane.widthProperty());
            eventFeedPane.getChildren().add(fb);
        } else fb = (VBox) eventFeedPane.getChildren().get(0);

        // Limit auf 3 Einträge für Terminal-Look
        if (fb.getChildren().size() > 2) fb.getChildren().remove(2);
        fb.getChildren().add(0, createLogEntry(event, step));
    }

    private VBox createLogEntry(ScandalEvent event, int step) {
        VBox entry = new VBox(2);
        entry.setStyle("-fx-padding: 0 0 10 0; -fx-border-color: #444; -fx-border-width: 0 0 1 0; -fx-border-style: dashed;");

        // 1. Header Zeile (ID + Time)
        HBox header = new HBox(10);
        String id = String.format("ID: %04d-%c", rng.nextInt(9999), (char)('A' + rng.nextInt(26)));
        Label idLbl = new Label("[" + id + "]"); idLbl.getStyleClass().add("terminal-label");
        Label timeLbl = new Label("TICK: " + step); timeLbl.setStyle("-fx-text-fill: #888; -fx-font-family: Consolas;");
        header.getChildren().addAll(idLbl, timeLbl);

        // 2. Info Zeile
        HBox info = new HBox(10);
        Label typeLbl = new Label("TYPE: " + event.getScandal().getType());
        typeLbl.getStyleClass().add("terminal-value-alert");

        Label locLbl = new Label(String.format("LOC: %03d.%02d", rng.nextInt(999), rng.nextInt(99)));
        locLbl.setStyle("-fx-text-fill: #888; -fx-font-family: Consolas;");
        info.getChildren().addAll(typeLbl, locLbl);

        // 3. Inhalt
        Label msg = new Label(">> " + event.getScandal().getTitle() + " // TARGET: " + event.getAffectedParty().getAbbreviation());
        msg.setStyle("-fx-text-fill: #e0e0e0; -fx-font-family: Consolas;");

        Label impact = new Label("IMPACT: -" + (int)(event.getScandal().getStrength() * 100) + "% STABILITY");
        impact.setStyle("-fx-text-fill: #ff3333; -fx-font-family: Consolas; -fx-font-weight: bold;");

        entry.getChildren().addAll(header, info, msg, impact);
        return entry;
    }
}