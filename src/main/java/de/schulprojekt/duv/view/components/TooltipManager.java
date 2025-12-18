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

    public TooltipManager(Pane animationPane) {
        this.animationPane = animationPane;
        this.tooltipBox = new VBox(5);
        tooltipBox.setPadding(new Insets(10));
        tooltipBox.setStyle("-fx-background-color: rgba(30, 30, 35, 0.95); -fx-border-color: #D4AF37; -fx-border-width: 1; -fx-background-radius: 5; -fx-border-radius: 5;");
        tooltipBox.setEffect(new DropShadow(10, Color.BLACK));
        tooltipBox.setVisible(false); tooltipBox.setMouseTransparent(true);

        name = new Label(); name.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold; -fx-font-size: 14px;");
        abbr = new Label(); voters = new Label(); pos = new Label(); scandals = new Label();
        String s = "-fx-text-fill: #e0e0e0; -fx-font-size: 12px;";
        abbr.setStyle(s); voters.setStyle(s); pos.setStyle(s); scandals.setStyle(s);

        tooltipBox.getChildren().addAll(name, abbr, new Separator(), voters, pos, scandals);
        animationPane.getChildren().add(tooltipBox);
    }

    public void handleMouseMove(double mx, double my, List<Party> parties, Map<String, CanvasRenderer.Point> positions, int total) {
        if (parties == null) return;
        for (Party p : parties) {
            CanvasRenderer.Point pt = positions.get(p.getName());
            if (pt != null) {
                double r = 30.0 + (((double) p.getCurrentSupporterCount() / Math.max(1, total)) * 60.0);
                if (Math.sqrt(Math.pow(mx - pt.x, 2) + Math.pow(my - pt.y, 2)) <= r) {
                    showTooltip(p, mx, my); return;
                }
            }
        }
        hideTooltip();
    }

    private void showTooltip(Party p, double x, double y) {
        name.setText(p.getName()); abbr.setText("Kürzel: " + p.getAbbreviation());
        voters.setText(String.format("Wähler: %,d", p.getCurrentSupporterCount()));
        pos.setText("Ausrichtung: " + p.getPoliticalOrientationName()); scandals.setText("Skandale: " + p.getScandalCount());
        double lx = x + 15, ly = y + 15;
        if (lx + 180 > animationPane.getWidth()) lx = x - 190;
        if (ly + 120 > animationPane.getHeight()) ly = y - 130;
        tooltipBox.setLayoutX(lx); tooltipBox.setLayoutY(ly);
        tooltipBox.setVisible(true); tooltipBox.toFront();
    }

    public void hideTooltip() { tooltipBox.setVisible(false); }
}