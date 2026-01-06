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
 * Manages tooltips for BOTH views:
 * 1. Dashboard: Dynamic via Mouse-Hover (handleMouseMove)
 * 2. Parliament: Static via Click (showStaticTooltip)
 */
public class TooltipManager {

    // --- Constants: Styling ---
    private static final String STYLE_HEADER_LABEL = "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-font-family: 'Consolas';";
    private static final String STYLE_SUBHEADER_LABEL = "-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 11px; -fx-font-family: 'Consolas';";
    private static final String STYLE_INFO_LABEL = "-fx-text-fill: #e0e0e0; -fx-font-size: 12px; -fx-font-family: 'Consolas';";
    private static final String STYLE_CONTENT_BOX = "-fx-background-color: rgba(20, 20, 25, 0.95);";
    private static final String STYLE_SCANDAL_LABEL = "-fx-font-size: 12px; -fx-font-family: 'Consolas'; -fx-font-weight: bold;";

    // --- UI Components ---
    private final Pane overlayPane;
    private final VBox tooltipBox;
    private final Label nameLabel, abbrLabel, votersLabel, posLabel, scandalsLabel;
    private final Line connectionLine;
    private final Circle anchorPoint;

    // --- State ---
    private Party currentActiveParty = null;

    // --- Constructor ---

    public TooltipManager(Pane animationPane) {
        // 1. Overlay Layer
        this.overlayPane = new Pane();
        this.overlayPane.setPickOnBounds(false);
        this.overlayPane.setMouseTransparent(true);

        // Bind size to parent
        this.overlayPane.prefWidthProperty().bind(animationPane.widthProperty());
        this.overlayPane.prefHeightProperty().bind(animationPane.heightProperty());
        animationPane.getChildren().add(overlayPane);

        // 2. Connection Elements (Line & Anchor)
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

        // 4. Build Content Structure
        VBox headerBox = new VBox(2);
        headerBox.setPadding(new Insets(8, 10, 8, 10));
        headerBox.setId("headerBox"); // For CSS lookup

        nameLabel = new Label();
        nameLabel.setStyle(STYLE_HEADER_LABEL);
        abbrLabel = new Label();
        abbrLabel.setStyle(STYLE_SUBHEADER_LABEL);
        headerBox.getChildren().addAll(nameLabel, abbrLabel);

        VBox contentBox = new VBox(4);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle(STYLE_CONTENT_BOX);

        votersLabel = new Label();
        votersLabel.setStyle(STYLE_INFO_LABEL);

        posLabel = new Label();
        posLabel.setStyle(STYLE_INFO_LABEL);

        Separator sep = new Separator();
        sep.setOpacity(0.3);

        scandalsLabel = new Label();
        scandalsLabel.setStyle(STYLE_SCANDAL_LABEL);

        contentBox.getChildren().addAll(votersLabel, posLabel, sep, scandalsLabel);
        tooltipBox.getChildren().addAll(headerBox, contentBox);

        // Add everything to overlay
        overlayPane.getChildren().addAll(connectionLine, anchorPoint, tooltipBox);
    }

    // --- Mode 1: Dashboard (Dynamic Hover) ---

    public void handleMouseMove(double mx, double my, List<Party> parties, Map<String, CanvasRenderer.Point> positions, int total) {
        if (parties == null || positions == null) return;

        boolean foundAny = false;

        for (Party p : parties) {
            CanvasRenderer.Point pt = positions.get(p.getName());
            if (pt != null) {
                // Determine Hit-Radius based on party size
                double share = (double) p.getCurrentSupporterCount() / Math.max(1, total);
                double r = 40.0 + (share * 60.0);

                // FIX: Access record fields via method calls pt.x() / pt.y()
                double px = pt.x();
                double py = pt.y();

                // Hit-Test (Euclidean Distance)
                if (Math.sqrt(Math.pow(mx - px, 2) + Math.pow(my - py, 2)) <= r) {
                    foundAny = true;

                    // Calculate target position (offset from mouse)
                    double targetBoxX = mx + 30;
                    double targetBoxY = my - 20;

                    // New party or just movement?
                    if (!tooltipBox.isVisible() || currentActiveParty != p) {
                        currentActiveParty = p;
                        showCallout(p, px, py, targetBoxX, targetBoxY);
                    } else {
                        // Just update position (no animation)
                        updateCalloutPosition(px, py, targetBoxX, targetBoxY);
                    }
                    break;
                }
            }
        }

        if (!foundAny) {
            hideTooltip();
        }
    }

    // --- Mode 2: Parliament (Static Click) ---

    public void showStaticTooltip(Party p, double anchorX, double anchorY) {
        currentActiveParty = p;

        // Simulate "preferred" box position relative to anchor
        double targetBoxX = anchorX + 40;
        double targetBoxY = anchorY - 60;

        showCallout(p, anchorX, anchorY, targetBoxX, targetBoxY);
    }

    // --- Public Control ---

    public void hideTooltip() {
        if (tooltipBox.isVisible()) {
            tooltipBox.setVisible(false);
            connectionLine.setVisible(false);
            anchorPoint.setVisible(false);
            currentActiveParty = null;
        }
    }

    // --- Private Helper: Rendering & Logic ---

    private void showCallout(Party p, double anchorX, double anchorY, double boxX, double boxY) {
        // 1. Dynamic Styling based on Party Color
        Color pColor;
        try {
            pColor = Color.web(p.getColorCode());
        } catch (Exception e) {
            pColor = Color.WHITE;
        }
        String hexColor = toHexString(pColor);

        tooltipBox.lookup("#headerBox").setStyle("-fx-background-color: " + hexColor + ";");
        tooltipBox.setStyle("-fx-border-color: " + hexColor + "; -fx-border-width: 1; -fx-background-radius: 3; -fx-border-radius: 3;");
        connectionLine.setStroke(pColor);
        anchorPoint.setFill(pColor);

        // 2. Set Text Content
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

        // 3. Make Visible & Layout
        tooltipBox.setVisible(true);
        connectionLine.setVisible(true);
        anchorPoint.setVisible(true);
        tooltipBox.applyCss();
        tooltipBox.layout();

        // Initial Position
        updateCalloutPosition(anchorX, anchorY, boxX, boxY);

        // 4. "Tech Unfold" Animation
        ScaleTransition st = new ScaleTransition(Duration.millis(200), tooltipBox);
        st.setFromY(0);
        st.setToY(1);

        FadeTransition ft = new FadeTransition(Duration.millis(200), tooltipBox);
        ft.setFromValue(0);
        ft.setToValue(1);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.play();
    }

    private void updateCalloutPosition(double anchorX, double anchorY, double desiredBoxX, double desiredBoxY) {
        double finalBoxX = desiredBoxX;
        double finalBoxY = desiredBoxY;

        // Boundary Check: Keep inside overlay
        if (finalBoxX + 220 > overlayPane.getWidth()) {
            finalBoxX = anchorX - 230;
        }
        if (finalBoxY + 150 > overlayPane.getHeight()) {
            finalBoxY = anchorY - 160;
        }

        // Apply Box Position
        tooltipBox.setLayoutX(finalBoxX);
        tooltipBox.setLayoutY(finalBoxY);

        // Draw Connection Line
        connectionLine.setStartX(anchorX);
        connectionLine.setStartY(anchorY);

        // Connect line to the nearest edge of the box
        connectionLine.setEndX(finalBoxX > anchorX ? finalBoxX : finalBoxX + tooltipBox.getWidth());
        connectionLine.setEndY(finalBoxY > anchorY ? finalBoxY : finalBoxY + tooltipBox.getHeight());

        // Update Anchor Point
        anchorPoint.setCenterX(anchorX);
        anchorPoint.setCenterY(anchorY);
    }

    private String toHexString(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}