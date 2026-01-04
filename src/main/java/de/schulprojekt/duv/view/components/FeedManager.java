package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.scandal.ScandalEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class FeedManager {
    private final HBox tickerBox;
    private final ScrollPane tickerScroll;
    private final Pane eventFeedPane;

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
        if (tickerBox != null) addScandalToTicker(scandal, step);
    }

    private void addToVerticalFeed(ScandalEvent event, int step) {
        VBox fb;
        if (eventFeedPane.getChildren().isEmpty()) {
            fb = new VBox(); fb.getStyleClass().add("event-feed-container");
            fb.prefWidthProperty().bind(eventFeedPane.widthProperty());
            eventFeedPane.getChildren().add(fb);
        } else fb = (VBox) eventFeedPane.getChildren().get(0);
        fb.getChildren().clear();
        fb.getChildren().add(createVerticalEventCard(event, step));
    }

    private HBox createVerticalEventCard(ScandalEvent event, int step) {
        VBox lc = new VBox(); lc.getStyleClass().add("event-timeline-col"); lc.setAlignment(Pos.TOP_CENTER); lc.setMinWidth(40);
        Circle bg = new Circle(14); bg.getStyleClass().addAll("event-icon-bg", getStyle(event));
        Text sym = new Text(getSymbol(event)); sym.getStyleClass().add("event-icon-symbol");
        lc.getChildren().add(new StackPane(bg, sym));
        VBox rc = new VBox(2); rc.getStyleClass().add("event-content-col"); HBox.setHgrow(rc, Priority.ALWAYS);
        Label t = new Label("Tick: " + step); t.getStyleClass().add("event-time");
        Label ti = new Label(event.getScandal().getTitle() + " (" + event.getAffectedParty().getAbbreviation() + ")"); ti.getStyleClass().add("event-title");
        Label d = new Label("Prognose: -" + (int)(event.getScandal().getStrength() * 50) + "% Beliebtheit."); d.getStyleClass().add("event-desc");
        rc.getChildren().addAll(t, ti, d);
        HBox card = new HBox(0); card.getStyleClass().add("event-card"); card.getChildren().addAll(lc, rc);
        return card;
    }

    private void addScandalToTicker(ScandalEvent event, int step) {
        tickerBox.setAlignment(Pos.TOP_LEFT);
        if (!tickerBox.getChildren().isEmpty()) {
            Line conn = new Line(0, 0, 50, 0); conn.getStyleClass().add("ticker-connector"); conn.setTranslateY(16);
            tickerBox.getChildren().add(conn);
        }

        Circle bg = new Circle(16);
        bg.getStyleClass().addAll("event-icon-bg", getStyle(event));

        Text sym = new Text(getSymbol(event));
        sym.getStyleClass().add("event-icon-symbol");

        StackPane st = new StackPane(bg, sym);
        st.getStyleClass().add("ticker-item");

        // FIX: Erzwingen einer quadratischen Form, damit der Kreis rund bleibt!
        st.setMinWidth(32);
        st.setMinHeight(32);
        st.setPrefSize(32, 32);
        st.setMaxSize(32, 32);

        Label l = new Label("Tick " + step);
        l.getStyleClass().add("ticker-time");

        VBox box = new VBox(st, l);
        box.getStyleClass().add("ticker-box");

        Tooltip tt = new Tooltip(String.format("TICK: %d\nPARTEI: %s\nTYP: %s\n\n%s\n\nAUSWIRKUNG: -%.0f%%", step, event.getAffectedParty().getAbbreviation(), event.getScandal().getType(), event.getScandal().getTitle(), event.getScandal().getStrength() * 50));
        tt.getStyleClass().add("scandal-tooltip"); tt.setShowDelay(Duration.ZERO); Tooltip.install(st, tt);

        tickerBox.getChildren().add(box);
        tickerBox.applyCss(); tickerBox.layout(); tickerScroll.layout(); tickerScroll.setHvalue(1.0);
    }

    private String getStyle(ScandalEvent e) {
        String t = e.getScandal().getType();
        return t.equals("CORRUPTION") ? "type-corruption" : t.equals("FINANCIAL") ? "type-financial" : t.equals("POLITICAL") ? "type-political" : t.equals("PERSONAL") ? "type-personal" : "type-scandal";
    }

    private String getSymbol(ScandalEvent e) {
        String t = e.getScandal().getType();
        return t.equals("CORRUPTION") ? "⚖" : t.equals("FINANCIAL") ? "$" : t.equals("POLITICAL") ? "♟" : t.equals("PERSONAL") ? "☹" : "⚠";
    }
}