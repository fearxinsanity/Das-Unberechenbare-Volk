package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.SimulationConfig;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Renders the parliament seating distribution in a semicircle.
 * * @author Nico Hoffmann
 * @version 1.1
 */
public class ParliamentRenderer {

    // ========================================
    // Static Variables
    // ========================================

    private static final int TOTAL_SEATS = 400;
    private static final int ROWS = 14;
    private static final double START_RADIUS = 80.0;
    private static final double ROW_STEP = 18.0;
    private static final double MAX_RADIUS = START_RADIUS + (ROWS * ROW_STEP);

    private static final Color COL_GOLD = Color.web("#D4AF37");
    private static final Color COL_GOLD_DIM = Color.web("#D4AF37", 0.3);
    private static final Color COL_TEXT = Color.web("#888888");
    private static final Color COL_BG_LINES = Color.web("#2a2a2e");
    private static final Color COL_EMPTY_SEAT = Color.web("#222222");

    // ========================================
    // Instance Variables
    // ========================================

    private final Canvas canvas;
    private final List<Seat> seats = new ArrayList<>();
    private AnimationTimer animationLoop;

    private double time = 0.0;
    private double lastCx, lastCy, lastScale;
    private Party selectedParty = null;
    private Party hoveredParty = null;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Initializes the renderer and binds it to the parent pane.
     * * @param parentPane the pane for the canvas
     */
    public ParliamentRenderer(Pane parentPane) {
        this.canvas = new Canvas(0, 0);
        canvas.widthProperty().bind(parentPane.widthProperty());
        canvas.heightProperty().bind(parentPane.heightProperty());

        Bloom bloom = new Bloom();
        bloom.setThreshold(0.6);
        canvas.setEffect(bloom);

        parentPane.getChildren().add(canvas);
        canvas.widthProperty().addListener(ignored -> requestDraw());
        canvas.heightProperty().addListener(ignored -> requestDraw());
    }

    // ========================================
    // Getter Methods
    // ========================================

    public Party getPartyAt(double screenX, double screenY) {
        if (lastScale <= 0) return null;
        double hitRadius = 12.0 * lastScale;
        for (Seat seat : seats) {
            if (seat.party() == null) continue;
            double dist = Math.sqrt(Math.pow(screenX - (lastCx + seat.x() * lastScale), 2) + Math.pow(screenY - (lastCy + seat.y() * lastScale), 2));
            if (dist < hitRadius) return seat.party();
        }
        return null;
    }

    public double[] getPartyCenterCoordinates(Party p) {
        if (p == null) return new double[]{lastCx, lastCy};
        double sumX = 0, sumY = 0;
        int count = 0;
        for (Seat s : seats) {
            if (p.equals(s.party())) {
                sumX += lastCx + (s.x() * lastScale);
                sumY += lastCy + (s.y() * lastScale);
                count++;
            }
        }
        return count == 0 ? new double[]{lastCx, lastCy} : new double[]{sumX / count, sumY / count};
    }

    // ========================================
    // Setter Methods
    // ========================================

    public void setSelectedParty(Party p) {
        this.selectedParty = p;
        requestDraw();
    }

    public void setHoveredParty(Party p) {
        if (this.hoveredParty != p) {
            this.hoveredParty = p;
            requestDraw();
        }
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    public void renderDistribution(List<Party> parties) {
        calculateSeatPositions(parties);
        startAnimation();
    }

    public void stop() {
        if (animationLoop != null) animationLoop.stop();
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void calculateSeatPositions(List<Party> parties) {
        seats.clear();
        List<Party> sorted = parties.stream().filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME)).sorted(Comparator.comparingDouble(Party::getPoliticalPosition)).toList();
        double total = sorted.stream().mapToDouble(Party::getCurrentSupporterCount).sum();
        List<Party> map = new ArrayList<>();
        if (total > 0) {
            for (Party p : sorted) {
                int count = (int) Math.round((p.getCurrentSupporterCount() / total) * TOTAL_SEATS);
                for (int i = 0; i < count; i++) map.add(p);
            }
        }
        while (map.size() > TOTAL_SEATS) map.removeLast();
        while (map.size() < TOTAL_SEATS) map.add(null);

        List<Point> layout = generateSeatingGeometry();
        for (int i = 0; i < Math.min(layout.size(), map.size()); i++) {
            Point pt = layout.get(i);
            seats.add(new Seat(pt.x(), pt.y(), pt.angle(), (int) Math.round((Math.sqrt(pt.x() * pt.x() + pt.y() * pt.y()) - START_RADIUS) / ROW_STEP), map.get(i)));
        }
    }

    private List<Point> generateSeatingGeometry() {
        List<Point> points = new ArrayList<>();
        double totalArc = 0;
        for (int r = 0; r < ROWS; r++) totalArc += Math.PI * (START_RADIUS + r * ROW_STEP);

        int generated = 0;
        for (int row = 0; row < ROWS; row++) {
            double radius = START_RADIUS + (row * ROW_STEP);
            int count = (row == ROWS - 1) ? TOTAL_SEATS - generated : (int) Math.round(TOTAL_SEATS * (Math.PI * radius / totalArc));
            generated += count;
            double step = (count > 1) ? Math.PI / (count - 1) : 0;
            for (int s = 0; s < count; s++) {
                double ang = (count > 1) ? Math.PI - (step * s) : Math.PI / 2;
                points.add(new Point(Math.cos(ang) * radius, -Math.sin(ang) * radius, ang));
            }
        }
        points.sort((p1, p2) -> Double.compare(p2.angle(), p1.angle()));
        return points;
    }

    private void startAnimation() {
        if (animationLoop != null) animationLoop.stop();
        final long start = System.nanoTime();
        animationLoop = new AnimationTimer() {
            @Override public void handle(long now) { time = (now - start) / 1_000_000_000.0; draw(time); }
        };
        animationLoop.start();
    }

    private void requestDraw() {
        if (!seats.isEmpty()) draw(time > 0 ? time : 10.0);
    }

    private void draw(double t) {
        if (canvas.getWidth() < 1 || canvas.getHeight() < 1) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        lastCx = canvas.getWidth() / 2; lastCy = canvas.getHeight() * 0.9;
        lastScale = Math.min(canvas.getWidth() / (MAX_RADIUS * 2 + 80), canvas.getHeight() / (MAX_RADIUS + 80));
        if (lastScale < 0.1) return;

        drawBackground(gc, lastCx, lastCy, lastScale, t);
        drawNetwork(gc, lastCx, lastCy, lastScale, t);
        drawSeats(gc, t);
        drawMarkers(gc, lastCx, lastCy, lastScale);
        drawHUD(gc, canvas.getWidth(), canvas.getHeight());
    }

    private void drawBackground(GraphicsContext gc, double cx, double cy, double scale, double t) {
        double outerR = (MAX_RADIUS + 20) * scale;
        gc.setStroke(Color.web("#222222")); gc.setLineDashes(5, 5);
        gc.strokeOval(cx - outerR, cy - outerR, outerR * 2, outerR * 2);
        gc.setStroke(COL_BG_LINES); gc.setLineDashes((double[]) null);
        for (int ang = 0; ang <= 180; ang += 15) {
            double r = Math.toRadians(ang);
            gc.strokeLine(cx, cy, cx + Math.cos(r) * (MAX_RADIUS + 40) * scale, cy - Math.sin(r) * (MAX_RADIUS + 40) * scale);
        }
        double rSc = ((t * 250) % (MAX_RADIUS * 1.5)) * scale;
        gc.setStroke(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.TRANSPARENT), new Stop(0.5, Color.web("#D4AF37", 0.15)), new Stop(1, Color.TRANSPARENT)));
        gc.strokeOval(cx - rSc, cy - rSc, rSc * 2, rSc * 2);
    }

    private void drawNetwork(GraphicsContext gc, double cx, double cy, double scale, double t) {
        gc.setStroke(Color.web("#D4AF37", 0.05));
        for (int i = 0; i < seats.size(); i += 10) {
            Seat s = seats.get(i);
            if (s.party() != null && t > 0.5) gc.strokeLine(cx + s.x() * scale, cy + s.y() * scale, cx, cy + 20 * scale);
        }
    }

    private void drawSeats(GraphicsContext gc, double t) {
        for (Seat seat : seats) {
            double wave = t * 2.0 - (seat.row() * 0.1);
            if (wave <= 0) continue;
            boolean empty = seat.party() == null;
            boolean selected = !empty && selectedParty != null && selectedParty.equals(seat.party());
            boolean hovered = !empty && hoveredParty != null && hoveredParty.equals(seat.party());
            boolean any = selectedParty != null;

            if (selected) { gc.setGlobalAlpha(1.0); gc.setEffect(new Glow(0.8)); }
            else if (hovered && !any) { gc.setGlobalAlpha(1.0); gc.setEffect(new Glow(0.5)); }
            else if (any) { gc.setGlobalAlpha(0.2); gc.setEffect(null); }
            else { gc.setGlobalAlpha(empty ? 0.3 : Math.min(1.0, wave)); gc.setEffect(null); }

            double sc = lastScale * (1.0 + Math.max(0, 1.0 - wave) * 0.5);
            Color c = empty ? COL_EMPTY_SEAT : Color.GRAY;
            if (!empty) { try { c = Color.web(seat.party().getColorCode()); } catch (Exception ignored) {} }
            gc.setFill(c.deriveColor(0, 1, 1, 0.4));
            gc.fillRect(lastCx + seat.x() * sc - 4.25 * lastScale, lastCy + seat.y() * sc - 4.25 * lastScale, 8.5 * lastScale, 8.5 * lastScale);
            if (!empty) { gc.setFill(c); gc.fillRect(lastCx + seat.x() * sc - 1.75 * lastScale, lastCy + seat.y() * sc - 1.75 * lastScale, 3.5 * lastScale, 3.5 * lastScale); }
        }
        gc.setGlobalAlpha(1.0); gc.setEffect(null);
    }

    private void drawMarkers(GraphicsContext gc, double cx, double cy, double scale) {
        double r = (MAX_RADIUS + 15) * scale;
        gc.setFont(Font.font("Consolas", 9)); gc.setTextAlign(TextAlignment.CENTER); gc.setStroke(COL_GOLD_DIM); gc.setFill(COL_TEXT);
        int idx = 1;
        for (int ang = 0; ang <= 180; ang += 30) {
            double rad = Math.toRadians(ang);
            gc.strokeLine(cx + Math.cos(rad) * (r - 5), cy - Math.sin(rad) * (r - 5), cx + Math.cos(rad) * (r + 10), cy - Math.sin(rad) * (r + 10));
            gc.fillText((ang == 90) ? "CENTER" : String.format("SEC-%02d", idx++), cx + Math.cos(rad) * (r + 25), cy - Math.sin(rad) * (r + 25) + 4);
        }
    }

    private void drawHUD(GraphicsContext gc, double w, double h) {
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#000000", 0.8)), new Stop(1, Color.TRANSPARENT)));
        gc.fillRect(0, 0, w, 60);
        gc.setStroke(COL_GOLD); gc.setLineWidth(1);
        gc.strokeLine(w / 2 - 50, h * 0.9 + 20, w / 2 + 50, h * 0.9 + 20);
        gc.setFill(COL_GOLD); gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.fillText("COMMAND_PODIUM", w / 2, h * 0.9 + 45);
        gc.setTextAlign(TextAlignment.LEFT); gc.setFill(COL_TEXT); gc.fillText("ENCRYPTION: AES-256", 20, h - 20);
    }

    // ========================================
    // Inner Classes
    // ========================================

    private record Point(double x, double y, double angle) {}
    private record Seat(double x, double y, double angle, int row, Party party) {}
}