package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.List;

public class TooltipManager {
    private final Pane parentPane;
    private final CanvasRenderer renderer;

    // Custom UI Elemente
    private VBox tooltipBox;
    private Label tooltipNameLabel;
    private Label tooltipAbbrLabel;
    private Label tooltipVotersLabel;
    private Label tooltipPositionLabel;
    private Label tooltipScandalsLabel;

    private List<Party> currentParties;

    public TooltipManager(Pane parentPane, CanvasRenderer renderer) {
        this.parentPane = parentPane;
        this.renderer = renderer;
        setupTooltip();
        setupListeners();
    }

    private void setupTooltip() {
        tooltipBox = new VBox(5);
        tooltipBox.setPadding(new Insets(10));
        // ORIGINAL STYLING
        tooltipBox.setStyle("-fx-background-color: rgba(30, 30, 35, 0.95); " +
                "-fx-border-color: #D4AF37; " +
                "-fx-border-width: 1; " +
                "-fx-background-radius: 5; " +
                "-fx-border-radius: 5;");
        tooltipBox.setEffect(new DropShadow(10, Color.BLACK));
        tooltipBox.setVisible(false);
        tooltipBox.setMouseTransparent(true);

        tooltipNameLabel = new Label();
        tooltipNameLabel.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold; -fx-font-size: 14px;");

        tooltipAbbrLabel = new Label();
        tooltipVotersLabel = new Label();
        tooltipPositionLabel = new Label();
        tooltipScandalsLabel = new Label();

        String infoStyle = "-fx-text-fill: #e0e0e0; -fx-font-size: 12px;";
        tooltipAbbrLabel.setStyle(infoStyle);
        tooltipVotersLabel.setStyle(infoStyle);
        tooltipPositionLabel.setStyle(infoStyle);
        tooltipScandalsLabel.setStyle(infoStyle);

        tooltipBox.getChildren().addAll(
                tooltipNameLabel,
                tooltipAbbrLabel,
                new Separator(),
                tooltipVotersLabel,
                tooltipPositionLabel,
                tooltipScandalsLabel
        );

        parentPane.getChildren().add(tooltipBox);
    }

    private void setupListeners() {
        parentPane.setOnMouseMoved(this::handleMouseMove);
        parentPane.setOnMouseExited(e -> hideTooltip());
    }

    public void updateData(List<Party> parties) {
        this.currentParties = parties;
    }

    private void handleMouseMove(MouseEvent event) {
        if (currentParties == null) return;

        boolean found = false;
        double mouseX = event.getX();
        double mouseY = event.getY();

        // Summe aller Wähler berechnen
        int totalVoters = currentParties.stream().mapToInt(Party::getCurrentSupporterCount).sum();
        if (totalVoters <= 0) totalVoters = 1;

        for (Party p : currentParties) {
            CanvasRenderer.Point pos = renderer.getPartyPosition(p.getName());
            if (pos == null) continue;

            // Dynamischer Hit-Radius passend zur gezeichneten Größe
            double share = (double) p.getCurrentSupporterCount() / totalVoters;
            double hitRadius = 30.0 + (share * 60.0);

            double dist = Math.sqrt(Math.pow(mouseX - pos.x, 2) + Math.pow(mouseY - pos.y, 2));

            if (dist <= hitRadius) {
                showTooltip(p, mouseX, mouseY);
                found = true;
                break;
            }
        }

        if (!found) hideTooltip();
    }

    private void showTooltip(Party p, double x, double y) {
        tooltipNameLabel.setText(p.getName());
        tooltipAbbrLabel.setText("Kürzel: " + p.getAbbreviation());
        tooltipVotersLabel.setText(String.format("Wähler: %,d", p.getCurrentSupporterCount()));
        tooltipPositionLabel.setText("Ausrichtung: " + p.getPoliticalOrientationName());
        tooltipScandalsLabel.setText("Skandale: " + p.getScandalCount());

        // Smart Positioning (damit es im Bild bleibt)
        double boxWidth = 180;
        double boxHeight = 120;
        double layoutX = x + 15;
        double layoutY = y + 15;

        if (layoutX + boxWidth > parentPane.getWidth()) layoutX = x - boxWidth - 10;
        if (layoutY + boxHeight > parentPane.getHeight()) layoutY = y - boxHeight - 10;

        tooltipBox.setLayoutX(layoutX);
        tooltipBox.setLayoutY(layoutY);
        tooltipBox.setVisible(true);
        tooltipBox.toFront();
    }

    private void hideTooltip() {
        tooltipBox.setVisible(false);
    }
}