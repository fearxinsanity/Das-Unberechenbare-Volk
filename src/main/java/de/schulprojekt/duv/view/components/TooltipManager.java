package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

public class TooltipManager {
    private final VBox tooltipBox;
    private final Label nameLabel, abbrLabel, votersLabel, posLabel, scandalsLabel;
    private final Pane overlayPane;
    private final Line connectionLine;
    private final Circle anchorPoint; // Kleiner Punkt am Ende der Linie

    private Party currentActiveParty = null;

    public TooltipManager(Pane animationPane) {
        // 1. Overlay Layer
        this.overlayPane = new Pane();
        this.overlayPane.setPickOnBounds(false);
        this.overlayPane.setMouseTransparent(true);

        animationPane.getChildren().add(overlayPane);
        overlayPane.prefWidthProperty().bind(animationPane.widthProperty());
        overlayPane.prefHeightProperty().bind(animationPane.heightProperty());

        // 2. Verbindungslinie & Ankerpunkt (Data Link Visuals)
        this.connectionLine = new Line();
        this.connectionLine.setStrokeWidth(1.5);
        this.connectionLine.getStrokeDashArray().addAll(5d, 5d); // Gestrichelt für Tech-Look
        this.connectionLine.setVisible(false);

        this.anchorPoint = new Circle(3);
        this.anchorPoint.setVisible(false);

        // 3. Tooltip Container
        this.tooltipBox = new VBox(0); // 0 Spacing, wir regeln das mit Padding
        this.tooltipBox.setPadding(Insets.EMPTY);
        this.tooltipBox.setVisible(false);

        // Schatten für Tiefe
        this.tooltipBox.setEffect(new DropShadow(15, Color.BLACK));

        // 4. Inhalt bauen
        // Header-Bereich (wird später eingefärbt)
        VBox headerBox = new VBox(2);
        headerBox.setPadding(new Insets(8, 10, 8, 10));
        headerBox.setId("headerBox"); // Für Zugriff später

        nameLabel = new Label();
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-font-family: 'Consolas';");

        abbrLabel = new Label();
        abbrLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px; -fx-font-family: 'Consolas';");

        headerBox.getChildren().addAll(nameLabel, abbrLabel);

        // Content-Bereich (Dunkel)
        VBox contentBox = new VBox(4);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle("-fx-background-color: rgba(20, 20, 25, 0.95);");

        String infoStyle = "-fx-text-fill: #e0e0e0; -fx-font-size: 12px; -fx-font-family: 'Consolas';";
        votersLabel = new Label(); votersLabel.setStyle(infoStyle);
        posLabel = new Label(); posLabel.setStyle(infoStyle);

        Separator sep = new Separator();
        sep.setOpacity(0.3);

        scandalsLabel = new Label();
        scandalsLabel.setStyle("-fx-text-fill: #ff5555; -fx-font-size: 12px; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");

        contentBox.getChildren().addAll(votersLabel, posLabel, sep, scandalsLabel);

        // Zusammenfügen
        tooltipBox.getChildren().addAll(headerBox, contentBox);

        // Alles zum Overlay hinzufügen (Reihenfolge wichtig: Linie unter Box)
        overlayPane.getChildren().addAll(connectionLine, anchorPoint, tooltipBox);
    }

    public void handleMouseMove(double mx, double my, List<Party> parties, Map<String, CanvasRenderer.Point> positions, int total) {
        if (parties == null) return;

        boolean foundAny = false;

        for (Party p : parties) {
            CanvasRenderer.Point pt = positions.get(p.getName());
            if (pt != null) {
                // Radius etwas großzügiger
                double share = (double) p.getCurrentSupporterCount() / Math.max(1, total);
                double r = 40.0 + (share * 60.0);

                // Hit-Test
                if (Math.sqrt(Math.pow(mx - pt.x, 2) + Math.pow(my - pt.y, 2)) <= r) {
                    foundAny = true;

                    // Neue Partei oder Update?
                    if (!tooltipBox.isVisible() || currentActiveParty != p) {
                        currentActiveParty = p;
                        showCallout(p, pt.x, pt.y, mx, my);
                    } else {
                        updateCalloutPosition(pt.x, pt.y, mx, my);
                    }
                    break;
                }
            }
        }

        if (!foundAny) {
            hideTooltip();
        }
    }

    private void updateCalloutPosition(double targetX, double targetY, double mouseX, double mouseY) {
        // Zielposition für die Box (leicht versetzt zur Maus)
        double boxX = mouseX + 30;
        double boxY = mouseY - 20;

        // Bildschirm-Grenzen Check
        if (boxX + 200 > overlayPane.getWidth()) boxX = mouseX - 210; // Links anzeigen
        if (boxY + 120 > overlayPane.getHeight()) boxY = mouseY - 130; // Oben anzeigen

        tooltipBox.setLayoutX(boxX);
        tooltipBox.setLayoutY(boxY);

        // Linie updaten: Von Partei-Zentrum -> Zur Box
        connectionLine.setStartX(targetX);
        connectionLine.setStartY(targetY);

        // Das Ende der Linie soll sich dynamisch an die Box-Ecke heften
        // Einfache Variante: Zur Box-Ecke, die dem Ziel am nächsten ist
        connectionLine.setEndX(boxX > targetX ? boxX : boxX + tooltipBox.getWidth());
        connectionLine.setEndY(boxY > targetY ? boxY : boxY + tooltipBox.getHeight());

        // Ankerpunkt auf Parteimitte
        anchorPoint.setCenterX(targetX);
        anchorPoint.setCenterY(targetY);
    }

    private void showCallout(Party p, double targetX, double targetY, double mouseX, double mouseY) {
        // 1. Styling basierend auf Partei-Farbe
        Color pColor;
        try {
            pColor = Color.web(p.getColorCode());
        } catch (Exception e) {
            pColor = Color.WHITE;
        }

        // Header Hintergrund in Parteifarbe
        String hexColor = toHexString(pColor);
        tooltipBox.lookup("#headerBox").setStyle("-fx-background-color: " + hexColor + ";");

        // Box Border in Parteifarbe
        tooltipBox.setStyle("-fx-border-color: " + hexColor + "; -fx-border-width: 1; -fx-background-radius: 3; -fx-border-radius: 3;");

        // Linie und Punkt einfärben
        connectionLine.setStroke(pColor);
        anchorPoint.setFill(pColor);

        // 2. Texte setzen
        nameLabel.setText(p.getName().toUpperCase());
        abbrLabel.setText(">> " + p.getAbbreviation());
        votersLabel.setText(String.format("STIMMEN: %,d", p.getCurrentSupporterCount()));
        posLabel.setText("POL. SPEKTRUM: " + p.getPoliticalOrientationName());

        int sCount = p.getScandalCount();
        if (sCount > 0) {
            scandalsLabel.setText("⚠ SKANDAL-LOG: " + sCount + " VERZEICHNET");
            scandalsLabel.setTextFill(Color.web("#ff5555"));
        } else {
            scandalsLabel.setText("✔ KEINE VORFÄLLE");
            scandalsLabel.setTextFill(Color.web("#55ff55"));
        }

        // 3. Sichtbar machen & Positionieren
        tooltipBox.setVisible(true);
        connectionLine.setVisible(true);
        anchorPoint.setVisible(true);

        // Layout erzwingen für korrekte Linien-Berechnung im ersten Frame
        tooltipBox.applyCss();
        tooltipBox.layout();

        updateCalloutPosition(targetX, targetY, mouseX, mouseY);

        // 4. "Tech Unfold" Animation
        // Box klappt vertikal auf
        ScaleTransition st = new ScaleTransition(Duration.millis(200), tooltipBox);
        st.setFromY(0);
        st.setToY(1);

        // Box blendet ein
        FadeTransition ft = new FadeTransition(Duration.millis(200), tooltipBox);
        ft.setFromValue(0);
        ft.setToValue(1);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.play();
    }

    public void hideTooltip() {
        if (tooltipBox.isVisible()) {
            tooltipBox.setVisible(false);
            connectionLine.setVisible(false);
            anchorPoint.setVisible(false);
            currentActiveParty = null;
        }
    }

    // Hilfsmethode für CSS Hex String
    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}