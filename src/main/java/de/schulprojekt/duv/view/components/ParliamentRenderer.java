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
 * High-Density "War Room" Parliament Renderer.
 * Renders the seating distribution in a semicircle using a Canvas.
 */
public class ParliamentRenderer {

    // --- Configuration Constants ---
    private static final int TOTAL_SEATS = 400;
    private static final int ROWS = 14;
    private static final double START_RADIUS = 80.0;
    private static final double ROW_STEP = 18.0;
    private static final double MAX_RADIUS = START_RADIUS + (ROWS * ROW_STEP);

    // --- Color Constants ---
    private static final Color COL_GOLD = Color.web("#D4AF37");
    private static final Color COL_GOLD_DIM = Color.web("#D4AF37", 0.3);
    private static final Color COL_TEXT = Color.web("#888888");
    private static final Color COL_BG_LINES = Color.web("#2a2a2e");
    private static final Color COL_EMPTY_SEAT = Color.web("#222222");

    // --- Fields ---
    private final Canvas canvas;
    private final List<Seat> seats = new ArrayList<>();
    private AnimationTimer animationLoop;

    // --- State & Interaction ---
    private double time = 0.0;
    private double lastCx, lastCy, lastScale;
    private Party selectedParty = null;
    private Party hoveredParty = null;

    // --- Constructor ---

    public ParliamentRenderer(Pane parentPane) {
        this.canvas = new Canvas(0, 0);

        canvas.widthProperty().bind(parentPane.widthProperty());
        canvas.heightProperty().bind(parentPane.heightProperty());

        Bloom bloom = new Bloom();
        bloom.setThreshold(0.6);
        canvas.setEffect(bloom);

        parentPane.getChildren().add(canvas);

        // FIX: Renamed 'obs' to 'ignored'
        canvas.widthProperty().addListener(ignored -> requestDraw());
        canvas.heightProperty().addListener(ignored -> requestDraw());
    }

    // --- Public API ---

    public void renderDistribution(List<Party> parties) {
        calculateSeatPositions(parties);
        startAnimation();
    }

    public void stop() {
        if (animationLoop != null) {
            animationLoop.stop();
        }
    }

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

    public Party getPartyAt(double screenX, double screenY) {
        if (lastScale <= 0) return null;

        double hitRadius = 12.0 * lastScale;

        for (Seat seat : seats) {
            if (seat.party() == null) continue;

            double seatScreenX = lastCx + (seat.x() * lastScale);
            double seatScreenY = lastCy + (seat.y() * lastScale);

            double dist = Math.sqrt(Math.pow(screenX - seatScreenX, 2) + Math.pow(screenY - seatScreenY, 2));
            if (dist < hitRadius) {
                return seat.party();
            }
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

        if (count == 0) return new double[]{lastCx, lastCy};
        return new double[]{sumX / count, sumY / count};
    }

    // --- Core Logic: Seat Calculation ---

    private void calculateSeatPositions(List<Party> parties) {
        seats.clear();

        // 1. Sort by political position
        // FIX: Replaced .collect(Collectors.toList()) with .toList()
        List<Party> sortedParties = parties.stream()
                .filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME))
                .sorted(Comparator.comparingDouble(Party::getPoliticalPosition))
                .toList();

        // 2. Distribute seats based on vote share
        double totalVotes = sortedParties.stream().mapToDouble(Party::getCurrentSupporterCount).sum();
        List<Party> seatPartyMap = new ArrayList<>();

        if (totalVotes > 0) {
            for (Party p : sortedParties) {
                double share = p.getCurrentSupporterCount() / totalVotes;
                int seatsForParty = (int) Math.round(share * TOTAL_SEATS);
                for (int i = 0; i < seatsForParty; i++) seatPartyMap.add(p);
            }
        }

        // Adjust to exactly TOTAL_SEATS
        while (seatPartyMap.size() > TOTAL_SEATS) {
            // FIX: Replaced remove(size-1) with removeLast()
            seatPartyMap.removeLast();
        }
        while (seatPartyMap.size() < TOTAL_SEATS) {
            seatPartyMap.add(null);
        }

        // 3. Calculate Geometry (Extracted Method)
        List<Point> layoutPoints = generateSeatingGeometry();

        // 4. Map Parties to Seats
        int limit = Math.min(layoutPoints.size(), seatPartyMap.size());
        for (int i = 0; i < limit; i++) {
            Point pt = layoutPoints.get(i);
            Party p = seatPartyMap.get(i);
            int row = (int) Math.round((Math.sqrt(pt.x() * pt.x() + pt.y() * pt.y()) - START_RADIUS) / ROW_STEP);
            seats.add(new Seat(pt.x(), pt.y(), pt.angle(), row, p));
        }
    }

    // FIX: Extracted complex geometry logic to separate method
    private List<Point> generateSeatingGeometry() {
        List<Point> points = new ArrayList<>();
        double totalArcLen = 0;

        for (int r = 0; r < ROWS; r++) {
            totalArcLen += Math.PI * (START_RADIUS + r * ROW_STEP);
        }

        int seatsGenerated = 0;

        for (int row = 0; row < ROWS; row++) {
            double currentRadius = START_RADIUS + (row * ROW_STEP);
            double arcLength = Math.PI * currentRadius;

            int seatsInRow = (int) Math.round(TOTAL_SEATS * (arcLength / totalArcLen));

            if (row == ROWS - 1) {
                seatsInRow = TOTAL_SEATS - seatsGenerated;
            }
            seatsGenerated += seatsInRow;

            double angleStep = (seatsInRow > 1) ? Math.PI / (seatsInRow - 1) : 0;

            for (int s = 0; s < seatsInRow; s++) {
                double angle;
                if (seatsInRow > 1) {
                    angle = Math.PI - (angleStep * s);
                } else {
                    angle = Math.PI / 2;
                }

                double x = Math.cos(angle) * currentRadius;
                double y = -Math.sin(angle) * currentRadius;
                points.add(new Point(x, y, angle));
            }
        }

        points.sort((p1, p2) -> Double.compare(p2.angle(), p1.angle()));
        return points;
    }

    // --- Animation Logic ---

    private void startAnimation() {
        if (animationLoop != null) animationLoop.stop();
        time = 0.0;

        final long startNano = System.nanoTime();
        animationLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = (now - startNano) / 1_000_000_000.0;
                time = t;
                draw(t);
            }
        };
        animationLoop.start();
    }

    private void requestDraw() {
        if (!seats.isEmpty()) {
            draw(time > 0 ? time : 10.0);
        }
    }

    // --- Rendering Layers ---

    private void draw(double t) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w < 1 || h < 1) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        this.lastCx = w / 2;
        this.lastCy = h * 0.9;

        double contentWidth = MAX_RADIUS * 2 + 80;
        double contentHeight = MAX_RADIUS + 80;
        this.lastScale = Math.min(w / contentWidth, h / contentHeight);

        if (lastScale < 0.1) return;

        drawBackground(gc, lastCx, lastCy, lastScale, t);
        drawNetwork(gc, lastCx, lastCy, lastScale, t);
        drawSeats(gc, t);
        drawMarkers(gc, lastCx, lastCy, lastScale);
        drawHUD(gc, w, h);
    }

    private void drawBackground(GraphicsContext gc, double cx, double cy, double scale, double t) {
        gc.setLineWidth(1.0);
        double outerR = (MAX_RADIUS + 20) * scale;

        gc.setStroke(Color.web("#222222"));
        gc.setLineDashes(5, 5);
        gc.strokeOval(cx - outerR, cy - outerR, outerR * 2, outerR * 2);

        gc.setStroke(COL_BG_LINES);
        gc.setLineDashes((double[]) null);
        for (int ang = 0; ang <= 180; ang += 15) {
            double rad = Math.toRadians(ang);
            double len = (MAX_RADIUS + 40) * scale;
            gc.strokeLine(cx, cy, cx + Math.cos(rad) * len, cy - Math.sin(rad) * len);
        }

        double scanR = (t * 250) % (MAX_RADIUS * 1.5);
        double rSc = scanR * scale;
        gc.setStroke(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.5, Color.web("#D4AF37", 0.15)),
                new Stop(1, Color.TRANSPARENT)));
        gc.strokeOval(cx - rSc, cy - rSc, rSc * 2, rSc * 2);
    }

    private void drawNetwork(GraphicsContext gc, double cx, double cy, double scale, double t) {
        gc.setStroke(Color.web("#D4AF37", 0.05));
        gc.setLineWidth(1.0);
        int step = 10;

        for (int i = 0; i < seats.size(); i += step) {
            Seat s = seats.get(i);
            if (s.party() != null && t > 0.5) {
                double sx = cx + s.x() * scale;
                double sy = cy + s.y() * scale;
                gc.strokeLine(sx, sy, cx, cy + 20 * scale);
            }
        }
    }

    private void drawSeats(GraphicsContext gc, double t) {
        for (Seat seat : seats) {
            // FIX: Extracted duplicate math expression to variable
            double waveFactor = t * 2.0 - (seat.row() * 0.1);

            double animOffset = Math.max(0, 1.0 - waveFactor);
            double currentScale = lastScale * (1.0 + animOffset * 0.5);
            double alpha = Math.min(1.0, waveFactor);

            if (alpha <= 0) continue;

            boolean isEmpty = (seat.party() == null);
            boolean isSelected = (!isEmpty && selectedParty != null && selectedParty.equals(seat.party()));
            boolean isHovered = (!isEmpty && hoveredParty != null && hoveredParty.equals(seat.party()));
            boolean anySelection = (selectedParty != null);

            if (isSelected) {
                gc.setGlobalAlpha(1.0);
                gc.setEffect(new Glow(0.8));
            } else if (isHovered && !anySelection) {
                gc.setGlobalAlpha(1.0);
                gc.setEffect(new Glow(0.5));
            } else if (anySelection) {
                gc.setGlobalAlpha(0.2);
                gc.setEffect(null);
            } else {
                gc.setGlobalAlpha(isEmpty ? 0.3 : alpha);
                gc.setEffect(null);
            }

            double x = lastCx + (seat.x() * currentScale);
            double y = lastCy + (seat.y() * currentScale);

            Color c = isEmpty ? COL_EMPTY_SEAT : Color.GRAY;
            if (!isEmpty) {
                try {
                    c = Color.web(seat.party().getColorCode());
                } catch (Exception ignored) { }
            }

            gc.setFill(c.deriveColor(0, 1, 1, 0.4));
            double size = 8.5 * lastScale;
            gc.fillRect(x - size / 2, y - size / 2, size, size);

            if (!isEmpty) {
                gc.setFill(c);
                double coreSize = 3.5 * lastScale;
                gc.fillRect(x - coreSize / 2, y - coreSize / 2, coreSize, coreSize);
            }
        }
        gc.setGlobalAlpha(1.0);
        gc.setEffect(null);
    }

    private void drawMarkers(GraphicsContext gc, double cx, double cy, double scale) {
        double r = (MAX_RADIUS + 15) * scale;
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setStroke(COL_GOLD_DIM);
        gc.setFill(COL_TEXT);

        int idx = 1;
        for (int ang = 0; ang <= 180; ang += 30) {
            double rad = Math.toRadians(ang);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);

            double x1 = cx + cos * (r - 5);
            double y1 = cy - sin * (r - 5);
            double x2 = cx + cos * (r + 10);
            double y2 = cy - sin * (r + 10);

            gc.strokeLine(x1, y1, x2, y2);

            double tx = cx + cos * (r + 25);
            double ty = cy - sin * (r + 25);

            String lbl = (ang == 90) ? "CENTER" : String.format("SEC-%02d", idx++);
            gc.fillText(lbl, tx, ty + 4);
        }
    }

    private void drawHUD(GraphicsContext gc, double w, double h) {
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#000000", 0.8)),
                new Stop(1, Color.TRANSPARENT)));
        gc.fillRect(0, 0, w, 60);

        double cx = w / 2;
        double cy = h * 0.9;

        gc.setStroke(COL_GOLD);
        gc.setLineWidth(1);
        gc.strokeLine(cx - 50, cy + 20, cx + 50, cy + 20);
        gc.strokeLine(cx - 50, cy + 20, cx - 60, cy + 30);
        gc.strokeLine(cx + 50, cy + 20, cx + 60, cy + 30);

        gc.setFill(COL_GOLD);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.fillText("COMMAND_PODIUM", cx, cy + 45);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(COL_TEXT);
        gc.fillText("ENCRYPTION: AES-256", 20, h - 20);
    }

    // --- Inner Records ---

    private record Point(double x, double y, double angle) {}

    private record Seat(double x, double y, double angle, int row, Party party) {}
}