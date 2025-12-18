package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.scandal.ScandalEvent;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class FeedManager {
    private final Pane eventFeedPane; // Parent Container für vertical box
    private final HBox scandalTickerBox;
    private final ScrollPane scandalTickerScroll;
    private final VBox verticalFeedBox; // Interne Box für die vertikale Liste

    public FeedManager(Pane eventFeedPane, HBox scandalTickerBox, ScrollPane scandalTickerScroll) {
        this.eventFeedPane = eventFeedPane;
        this.scandalTickerBox = scandalTickerBox;
        this.scandalTickerScroll = scandalTickerScroll;

        // Initialisiere die VBox für den vertikalen Feed
        this.verticalFeedBox = new VBox(10);
        this.eventFeedPane.getChildren().add(verticalFeedBox);
    }

    public void addScandal(ScandalEvent event, int step) {
        // 1. Vertikaler Feed (Karte)
        VBox card = createVerticalEventCard(event, step);
        // Neue Events oben einfügen
        verticalFeedBox.getChildren().add(0, card);

        // Begrenzen auf z.B. 10 Einträge, um Speicher zu sparen
        if (verticalFeedBox.getChildren().size() > 20) {
            verticalFeedBox.getChildren().remove(20);
        }

        // 2. Horizontaler Ticker (Laufschrift-Element)
        HBox tickerItem = createTickerItem(event);
        scandalTickerBox.getChildren().add(tickerItem);

        // Auto-Scroll nach rechts
        startAutoScroll();
    }

    public void clear() {
        verticalFeedBox.getChildren().clear();
        scandalTickerBox.getChildren().clear();
    }

    private VBox createVerticalEventCard(ScandalEvent event, int step) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");

        Label title = new Label(getScandalSymbol(event) + " " + event.getScandal().getTitle());
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setTextFill(Color.web(getScandalColor(event)));

        Label partyLabel = new Label("Betrifft: " + event.getAffectedParty().getName());
        Label stepLabel = new Label("Zeitpunkt: Woche " + step);
        stepLabel.setTextFill(Color.GRAY);
        stepLabel.setFont(Font.font(10));

        card.getChildren().addAll(title, partyLabel, stepLabel);
        return card;
    }

    private HBox createTickerItem(ScandalEvent event) {
        HBox item = new HBox(5);
        item.setStyle("-fx-padding: 5 15 5 15; -fx-background-color: " + getScandalColor(event) + "; -fx-background-radius: 15;");

        Label text = new Label(getScandalSymbol(event) + " EILMELDUNG: " + event.getScandal().getTitle() + " (" + event.getAffectedParty().getName() + ")");
        text.setTextFill(Color.WHITE);
        text.setFont(Font.font("System", FontWeight.BOLD, 12));

        item.getChildren().add(text);
        return item;
    }

    private void startAutoScroll() {
        // Simples Auto-Scroll für den Ticker
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            scandalTickerScroll.setHvalue(scandalTickerScroll.getHvalue() + 0.001);
            if (scandalTickerScroll.getHvalue() >= 1.0) {
                // Reset optional, hier lassen wir es einfach laufen
            }
        }));
        timeline.setCycleCount(1);
        timeline.play();
    }

    // Hilfsmethoden für Styling
    private String getScandalColor(ScandalEvent event) {
        double sev = event.getScandal().getStrength();
        if (sev > 0.8) return "#e74c3c"; // Rot (Kritisch)
        if (sev > 0.5) return "#f39c12"; // Orange (Mittel)
        return "#3498db"; // Blau (Gering)
    }

    private String getScandalSymbol(ScandalEvent event) {
        double sev = event.getScandal().getStrength();
        if (sev > 0.8) return "⚡";
        if (sev > 0.5) return "⚠";
        return "ℹ";
    }
}