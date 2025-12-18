package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.Map;

public class TooltipManager {
    private final VBox tooltipBox;
    private final Label name, abbr, voters, pos, scandals;
    private final Pane animationPane;
    private final Pane overlayPane;

    public TooltipManager(Pane animationPane) {
        this.animationPane = animationPane;

        // 1. Eine transparente Ebene (Overlay) erstellen.
        // Die StackPane (animationPane) zieht dieses Pane automatisch auf volle Größe.
        this.overlayPane = new Pane();
        this.overlayPane.setPickOnBounds(false); // Klicks gehen durch leere Bereiche durch
        this.overlayPane.setMouseTransparent(true); // Tooltip blockiert keine Maus-Events

        // Das Overlay zur StackPane hinzufügen (liegt nun über dem Canvas)
        animationPane.getChildren().add(overlayPane);

        // 2. Die Tooltip-Box erstellen
        this.tooltipBox = new VBox(5);
        this.tooltipBox.setPadding(new Insets(10));
        this.tooltipBox.setStyle("-fx-background-color: rgba(30, 30, 35, 0.95); -fx-border-color: #D4AF37; -fx-border-width: 1; -fx-background-radius: 5; -fx-border-radius: 5;");
        this.tooltipBox.setEffect(new DropShadow(10, Color.BLACK));
        this.tooltipBox.setVisible(false);

        // Labels initialisieren
        name = new Label(); name.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold; -fx-font-size: 14px;");
        abbr = new Label(); voters = new Label(); pos = new Label(); scandals = new Label();
        String s = "-fx-text-fill: #e0e0e0; -fx-font-size: 12px;";
        abbr.setStyle(s); voters.setStyle(s); pos.setStyle(s); scandals.setStyle(s);

        tooltipBox.getChildren().addAll(name, abbr, new Separator(), voters, pos, scandals);

        // 3. Tooltip zum Overlay hinzufügen (nicht direkt zur StackPane!)
        // In einem normalen Pane funktioniert LayoutX/Y und Autosize perfekt.
        overlayPane.getChildren().add(tooltipBox);
    }

    public void handleMouseMove(double mx, double my, List<Party> parties, Map<String, CanvasRenderer.Point> positions, int total) {
        if (parties == null) return;

        boolean found = false;
        for (Party p : parties) {
            CanvasRenderer.Point pt = positions.get(p.getName());
            if (pt != null) {
                // Radius-Logik analog zum CanvasRenderer (30 Basis + Anteil)
                double share = (double) p.getCurrentSupporterCount() / Math.max(1, total);
                double r = 30.0 + (share * 60.0);

                // Prüfen ob Maus im Kreis ist
                if (Math.sqrt(Math.pow(mx - pt.x, 2) + Math.pow(my - pt.y, 2)) <= r) {
                    showTooltip(p, mx, my);
                    found = true;
                    break;
                }
            }
        }
        if (!found) hideTooltip();
    }

    private void showTooltip(Party p, double x, double y) {
        name.setText(p.getName());
        abbr.setText("Kürzel: " + p.getAbbreviation());
        voters.setText(String.format("Wähler: %,d", p.getCurrentSupporterCount()));
        pos.setText("Ausrichtung: " + p.getPoliticalOrientationName());
        scandals.setText("Skandale: " + p.getScandalCount());

        double lx = x + 15;
        double ly = y + 15;

        // Position prüfen (damit er nicht aus dem Bild rutscht)
        // Wir nutzen overlayPane für die Abmessungen, da es die gleiche Größe wie animationPane hat
        if (lx + 200 > overlayPane.getWidth()) lx = x - 210;
        if (ly + 150 > overlayPane.getHeight()) ly = y - 160;

        tooltipBox.setLayoutX(lx);
        tooltipBox.setLayoutY(ly);
        tooltipBox.setVisible(true);
    }

    public void hideTooltip() {
        tooltipBox.setVisible(false);
    }
}