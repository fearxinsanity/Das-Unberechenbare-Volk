package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

/**
 * Verwaltet Tooltips für BEIDE Ansichten:
 * 1. Dashboard: Dynamisch per Mouse-Hover (handleMouseMove)
 * 2. Parlament: Statisch per Klick (showStaticTooltip)
 */
public class TooltipManager {
    private final VBox tooltipBox;
    private final Label nameLabel, abbrLabel, votersLabel, posLabel, scandalsLabel;
    private final Pane overlayPane;
    private final Line connectionLine;
    private final Circle anchorPoint;

    private Party currentActiveParty = null;

    public TooltipManager(Pane animationPane) {
        // 1. Overlay Layer
        this.overlayPane = new Pane();
        this.overlayPane.setPickOnBounds(false);
        this.overlayPane.setMouseTransparent(true);

        animationPane.getChildren().add(overlayPane);
        overlayPane.prefWidthProperty().bind(animationPane.widthProperty());
        overlayPane.prefHeightProperty().bind(animationPane.heightProperty());

        // 2. Verbindungslinie
        this.connectionLine = new Line();
        this.connectionLine.setStrokeWidth(1.5);
        this.connectionLine.getStrokeDashArray().addAll(5d, 5d);
        this.connectionLine.setVisible(false);

        this.anchorPoint = new Circle(3);
        this.anchorPoint.setVisible(false);

        // 3. Tooltip Container
        this.tooltipBox = new VBox(0);
        this.tooltipBox.setPadding(Insets.EMPTY);
        this.tooltipBox.setVisible(false);
        this.tooltipBox.setEffect(new DropShadow(15, Color.BLACK));

        // 4. Inhalt bauen
        VBox headerBox = new VBox(2);
        headerBox.setPadding(new Insets(8, 10, 8, 10));
        headerBox.setId("headerBox"); // Für CSS-Zugriff

        nameLabel = new Label();
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-font-family: 'Consolas';");
        abbrLabel = new Label();
        abbrLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px; -fx-font-family: 'Consolas';");
        headerBox.getChildren().addAll(nameLabel, abbrLabel);

        VBox contentBox = new VBox(4);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle("-fx-background-color: rgba(20, 20, 25, 0.95);");

        String infoStyle = "-fx-text-fill: #e0e0e0; -fx-font-size: 12px; -fx-font-family: 'Consolas';";
        votersLabel = new Label(); votersLabel.setStyle(infoStyle);
        posLabel = new Label(); posLabel.setStyle(infoStyle);
        Separator sep = new Separator(); sep.setOpacity(0.3);
        scandalsLabel = new Label();
        scandalsLabel.setStyle("-fx-text-fill: #ff5555; -fx-font-size: 12px; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");

        contentBox.getChildren().addAll(votersLabel, posLabel, sep, scandalsLabel);
        tooltipBox.getChildren().addAll(headerBox, contentBox);

        overlayPane.getChildren().addAll(connectionLine, anchorPoint, tooltipBox);
    }

    // ============================================================================================
    // MODUS 1: Dashboard (Dynamischer Mouse-Hover)
    // ============================================================================================

    /**
     * Prüft, ob die Maus über einem Parteikreis ist (für Dashboard).
     */
    public void handleMouseMove(double mx, double my, List<Party> parties, Map<String, CanvasRenderer.Point> positions, int total) {
        if (parties == null || positions == null) return;

        boolean foundAny = false;

        for (Party p : parties) {
            CanvasRenderer.Point pt = positions.get(p.getName());
            if (pt != null) {
                // Radius-Check (etwas größer als der gezeichnete Kreis)
                double share = (double) p.getCurrentSupporterCount() / Math.max(1, total);
                double r = 40.0 + (share * 60.0);

                // Hit-Test
                if (Math.sqrt(Math.pow(mx - pt.x, 2) + Math.pow(my - pt.y, 2)) <= r) {
                    foundAny = true;

                    // Zielposition für die Box berechnen (leicht versetzt zur Maus)
                    double targetBoxX = mx + 30;
                    double targetBoxY = my - 20;

                    // Neue Partei oder nur Bewegung?
                    if (!tooltipBox.isVisible() || currentActiveParty != p) {
                        currentActiveParty = p;
                        // Animation starten
                        showCallout(p, pt.x, pt.y, targetBoxX, targetBoxY);
                    } else {
                        // Nur Position updaten (ohne Animation)
                        updateCalloutPosition(pt.x, pt.y, targetBoxX, targetBoxY);
                    }
                    break;
                }
            }
        }

        if (!foundAny) {
            hideTooltip();
        }
    }

    // ============================================================================================
    // MODUS 2: Parlament (Statischer Klick)
    // ============================================================================================

    /**
     * Zeigt den Tooltip statisch an einer festen Position (für Parlament).
     */
    public void showStaticTooltip(Party p, double anchorX, double anchorY) {
        currentActiveParty = p;

        // Simuliere eine "Box-Position" relativ zum Ankerpunkt
        double targetBoxX = anchorX + 40;
        double targetBoxY = anchorY - 60;

        showCallout(p, anchorX, anchorY, targetBoxX, targetBoxY);
    }

    // ============================================================================================
    // Gemeinsame Logik (Anzeige & Rendering)
    // ============================================================================================

    private void showCallout(Party p, double anchorX, double anchorY, double boxX, double boxY) {
        // 1. Styling
        Color pColor;
        try { pColor = Color.web(p.getColorCode()); } catch (Exception e) { pColor = Color.WHITE; }
        String hexColor = toHexString(pColor);

        tooltipBox.lookup("#headerBox").setStyle("-fx-background-color: " + hexColor + ";");
        tooltipBox.setStyle("-fx-border-color: " + hexColor + "; -fx-border-width: 1; -fx-background-radius: 3; -fx-border-radius: 3;");
        connectionLine.setStroke(pColor);
        anchorPoint.setFill(pColor);

        // 2. Inhalte
        nameLabel.setText(p.getName().toUpperCase());
        abbrLabel.setText(">> " + p.getAbbreviation());
        votersLabel.setText(String.format("STIMMEN: %,d", p.getCurrentSupporterCount()));
        posLabel.setText("POL. SPEKTRUM: " + p.getPoliticalOrientationName());

        int sCount = p.getScandalCount();
        if (sCount > 0) {
            scandalsLabel.setText("⚠ SKANDAL-LOG: " + sCount);
            scandalsLabel.setTextFill(Color.web("#ff5555"));
        } else {
            scandalsLabel.setText("✔ KEINE VORFÄLLE");
            scandalsLabel.setTextFill(Color.web("#55ff55"));
        }

        // 3. Sichtbar machen & Layout
        tooltipBox.setVisible(true);
        connectionLine.setVisible(true);
        anchorPoint.setVisible(true);
        tooltipBox.applyCss();
        tooltipBox.layout();

        // Position initial setzen
        updateCalloutPosition(anchorX, anchorY, boxX, boxY);

        // 4. "Tech Unfold" Animation
        ScaleTransition st = new ScaleTransition(Duration.millis(200), tooltipBox);
        st.setFromY(0); st.setToY(1);
        FadeTransition ft = new FadeTransition(Duration.millis(200), tooltipBox);
        ft.setFromValue(0); ft.setToValue(1);
        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.play();
    }

    private void updateCalloutPosition(double anchorX, double anchorY, double desiredBoxX, double desiredBoxY) {
        // Bildschirmgrenzen prüfen
        double finalBoxX = desiredBoxX;
        double finalBoxY = desiredBoxY;

        if (finalBoxX + 220 > overlayPane.getWidth()) finalBoxX = anchorX - 230;
        if (finalBoxY + 150 > overlayPane.getHeight()) finalBoxY = anchorY - 160;

        // Box setzen
        tooltipBox.setLayoutX(finalBoxX);
        tooltipBox.setLayoutY(finalBoxY);

        // Linie von Anker zur Box ziehen
        connectionLine.setStartX(anchorX);
        connectionLine.setStartY(anchorY);

        // Linie soll an der nächstgelegenen Ecke/Kante der Box enden
        connectionLine.setEndX(finalBoxX > anchorX ? finalBoxX : finalBoxX + tooltipBox.getWidth());
        connectionLine.setEndY(finalBoxY > anchorY ? finalBoxY : finalBoxY + tooltipBox.getHeight());

        // Punkt setzen
        anchorPoint.setCenterX(anchorX);
        anchorPoint.setCenterY(anchorY);
    }

    public void hideTooltip() {
        if (tooltipBox.isVisible()) {
            tooltipBox.setVisible(false);
            connectionLine.setVisible(false);
            anchorPoint.setVisible(false);
            currentActiveParty = null;
        }
    }

    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}