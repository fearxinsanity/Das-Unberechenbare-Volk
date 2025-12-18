package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.scandal.ScandalEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class FeedManager {
    private final Pane eventFeedPane;
    private final HBox scandalTickerBox;
    private final ScrollPane scandalTickerScroll;

    public FeedManager(Pane eventFeedPane, HBox scandalTickerBox, ScrollPane scandalTickerScroll) {
        this.eventFeedPane = eventFeedPane;
        this.scandalTickerBox = scandalTickerBox;
        this.scandalTickerScroll = scandalTickerScroll;

        if (scandalTickerBox != null) {
            scandalTickerBox.getStyleClass().add("ticker-container");
            scandalTickerBox.setAlignment(Pos.TOP_LEFT);
        }
    }

    public void addScandal(ScandalEvent event, int step) {
        if (eventFeedPane != null) addToVerticalFeed(event, step);
        if (scandalTickerBox != null) addToTicker(event, step);
    }

    public void clear() {
        if (eventFeedPane != null) eventFeedPane.getChildren().clear();
        if (scandalTickerBox != null) scandalTickerBox.getChildren().clear();
    }

    private void addToVerticalFeed(ScandalEvent event, int step) {
        VBox feedBox;
        if (eventFeedPane.getChildren().isEmpty()) {
            feedBox = new VBox();
            feedBox.getStyleClass().add("event-feed-container");
            feedBox.prefWidthProperty().bind(eventFeedPane.widthProperty());
            eventFeedPane.getChildren().add(feedBox);
        } else {
            feedBox = (VBox) eventFeedPane.getChildren().get(0);
        }

        feedBox.getChildren().clear(); // Originalverhalten: Alte Karten löschen
        feedBox.getChildren().add(createVerticalEventCard(event, step));
    }

    private HBox createVerticalEventCard(ScandalEvent event, int step) {
        // Linke Spalte (Icon)
        VBox leftCol = new VBox();
        leftCol.getStyleClass().add("event-timeline-col");
        leftCol.setAlignment(Pos.TOP_CENTER);
        leftCol.setMinWidth(40);
        leftCol.setMaxWidth(40);

        StackPane iconStack = createIconStack(event, 14);
        leftCol.getChildren().add(iconStack);

        // Rechte Spalte (Content)
        VBox rightCol = new VBox(2);
        rightCol.getStyleClass().add("event-content-col");
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        Label timeLbl = new Label("Tick: " + step);
        timeLbl.getStyleClass().add("event-time");

        Label titleLbl = new Label(event.getScandal().getTitle() + " (" + event.getAffectedParty().getAbbreviation() + ")");
        titleLbl.getStyleClass().add("event-title");
        titleLbl.setWrapText(true);

        Label descLbl = new Label("Prognose: -" + (int)(event.getScandal().getStrength() * 50) + "% Beliebtheit.");
        descLbl.getStyleClass().add("event-desc");
        descLbl.setWrapText(true);

        rightCol.getChildren().addAll(timeLbl, titleLbl, descLbl);

        HBox card = new HBox(0);
        card.getStyleClass().add("event-card");
        card.getChildren().addAll(leftCol, rightCol);
        return card;
    }

    private void addToTicker(ScandalEvent event, int step) {
        // Verbindungslinie
        if (!scandalTickerBox.getChildren().isEmpty()) {
            Line connector = new Line(0, 0, 50, 0);
            connector.getStyleClass().add("ticker-connector");
            connector.setTranslateY(16); // Zentrieren
            scandalTickerBox.getChildren().add(connector);
        }

        StackPane iconStack = createIconStack(event, 16);
        iconStack.getStyleClass().add("ticker-item");
        iconStack.setMinSize(32, 32);
        iconStack.setMaxSize(32, 32);

        Label tickLabel = new Label("Tick " + step);
        tickLabel.getStyleClass().add("ticker-time");

        VBox tickerEntry = new VBox(iconStack, tickLabel);
        tickerEntry.getStyleClass().add("ticker-box");

        // Tooltip für Ticker
        String tooltipText = String.format("TICK: %d\nPARTEI: %s\n%s",
                step, event.getAffectedParty().getName(), event.getScandal().getTitle());
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.getStyleClass().add("scandal-tooltip");
        tooltip.setShowDelay(Duration.ZERO);
        Tooltip.install(iconStack, tooltip);

        scandalTickerBox.getChildren().add(tickerEntry);

        scandalTickerScroll.layout();
        scandalTickerScroll.setHvalue(1.0);
    }

    private StackPane createIconStack(ScandalEvent event, double radius) {
        String typeStyle = getScandalStyle(event);
        String symbol = getScandalSymbol(event);

        Circle iconBg = new Circle(radius);
        iconBg.getStyleClass().addAll("event-icon-bg", typeStyle);

        Text iconSymbol = new Text(symbol);
        iconSymbol.getStyleClass().add("event-icon-symbol");

        return new StackPane(iconBg, iconSymbol);
    }

    private String getScandalStyle(ScandalEvent event) {
        // Logik aus Main: Versuche Type zu matchen, sonst Fallback auf Severity
        try {
            // Falls dein Model im Restructure Branch kein getType() hat, nutzen wir die Severity Logik
            // (Im Main Branch gab es switch case auf Type Strings)
            double sev = event.getScandal().getStrength();
            if (sev > 0.8) return "type-corruption";
            if (sev > 0.6) return "type-financial";
            if (sev > 0.4) return "type-scandal";
            return "type-default";
        } catch (Exception e) {
            return "type-default";
        }
    }

    private String getScandalSymbol(ScandalEvent event) {
        double sev = event.getScandal().getStrength();
        if (sev > 0.8) return "⚖";
        if (sev > 0.6) return "$";
        if (sev > 0.4) return "⚠";
        return "!";
    }
}