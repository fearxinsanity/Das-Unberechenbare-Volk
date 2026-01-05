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
import java.util.stream.Collectors;

/**
 * High-Density "War Room" Parliament Renderer.
 * Version: Clean & Fixed (Voller 180-Grad-Bogen, keine extra Linien).
 */
public class ParliamentRenderer {

    private final Canvas canvas;
    private final List<Seat> seats = new ArrayList<>();
    private AnimationTimer animationLoop;

    // --- Konfiguration ---
    // Erhöhte Sitzzahl für eine dichte, geschlossene Optik
    private static final int TOTAL_SEATS = 400;
    private static final int ROWS = 14;

    // Geometrie
    private static final double START_RADIUS = 80.0;
    private static final double ROW_STEP = 18.0;
    // Maximaler Radius für Skalierungsberechnung
    private static final double MAX_RADIUS = START_RADIUS + (ROWS * ROW_STEP);

    // --- Farben ---
    private static final Color COL_GOLD = Color.web("#D4AF37");
    private static final Color COL_GOLD_DIM = Color.web("#D4AF37", 0.3);
    private static final Color COL_TEXT = Color.web("#888888");
    private static final Color COL_BG_LINES = Color.web("#2a2a2e");
    private static final Color COL_EMPTY_SEAT = Color.web("#222222");

    // Animation & State
    private double time = 0.0;

    // Interaktion
    private double lastCx, lastCy, lastScale;
    private Party selectedParty = null;
    private Party hoveredParty = null;

    public ParliamentRenderer(Pane parentPane) {
        this.canvas = new Canvas(0, 0);
        canvas.widthProperty().bind(parentPane.widthProperty());
        canvas.heightProperty().bind(parentPane.heightProperty());

        Bloom bloom = new Bloom();
        bloom.setThreshold(0.6);
        canvas.setEffect(bloom);

        parentPane.getChildren().add(canvas);

        canvas.widthProperty().addListener(obs -> requestDraw());
        canvas.heightProperty().addListener(obs -> requestDraw());
    }

    public void renderDistribution(List<Party> parties) {
        calculateSeatPositions(parties);
        startAnimation();
    }

    public void stop() {
        if (animationLoop != null) animationLoop.stop();
    }

    private void requestDraw() {
        if (!seats.isEmpty()) draw(time > 0 ? time : 10.0);
    }

    // --- Interaktion ---

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
        if (lastScale == 0) return null;
        double hitRadius = 12.0 * lastScale; // Hitbox etwas kleiner für Präzision

        for (Seat seat : seats) {
            if (seat.party == null) continue;
            double seatScreenX = lastCx + (seat.x * lastScale);
            double seatScreenY = lastCy + (seat.y * lastScale);
            double dist = Math.sqrt(Math.pow(screenX - seatScreenX, 2) + Math.pow(screenY - seatScreenY, 2));
            if (dist < hitRadius) {
                return seat.party;
            }
        }
        return null;
    }

    public double[] getPartyCenterCoordinates(Party p) {
        if (p == null) return new double[]{lastCx, lastCy};
        double sumX = 0, sumY = 0;
        int count = 0;
        for (Seat s : seats) {
            if (p.equals(s.party)) {
                sumX += lastCx + (s.x * lastScale);
                sumY += lastCy + (s.y * lastScale);
                count++;
            }
        }
        if (count == 0) return new double[]{lastCx, lastCy};
        return new double[]{sumX / count, sumY / count};
    }

    // --- Berechnung ---

    private void calculateSeatPositions(List<Party> parties) {
        seats.clear();

        // 1. Sortieren nach politischer Position und Filterung
        List<Party> sortedParties = parties.stream()
                .filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME))
                .sorted(Comparator.comparingDouble(Party::getPoliticalPosition))
                .collect(Collectors.toList());

        // 2. Sitze verteilen (Basis ist nur die Summe der angezeigten Parteien!)
        double totalVotes = sortedParties.stream().mapToDouble(Party::getCurrentSupporterCount).sum();

        List<Party> seatPartyMap = new ArrayList<>();
        if (totalVotes > 0) {
            for (Party p : sortedParties) {
                double share = p.getCurrentSupporterCount() / totalVotes;
                int seatsForParty = (int) Math.round(share * TOTAL_SEATS);
                for (int i = 0; i < seatsForParty; i++) seatPartyMap.add(p);
            }
        }

        // Trimmen oder Auffüllen, um exakt TOTAL_SEATS zu erreichen
        while (seatPartyMap.size() > TOTAL_SEATS) {
            seatPartyMap.remove(seatPartyMap.size() - 1);
        }
        while (seatPartyMap.size() < TOTAL_SEATS) {
            seatPartyMap.add(null); // Auffüllen falls Rundungsdifferenzen
        }

        // 3. Geometrie berechnen (Verteilung auf Reihen)
        List<Point> layoutPoints = new ArrayList<>();

        // Gesamtbogenlänge berechnen
        double totalArcLen = 0;
        for (int r = 0; r < ROWS; r++) {
            totalArcLen += Math.PI * (START_RADIUS + r * ROW_STEP);
        }

        int seatsGenerated = 0;

        for (int row = 0; row < ROWS; row++) {
            double currentRadius = START_RADIUS + (row * ROW_STEP);
            double arcLength = Math.PI * currentRadius;

            // Anteilige Sitze für diese Reihe
            int seatsInRow = (int) Math.round(TOTAL_SEATS * (arcLength / totalArcLen));

            // Korrektur für die letzte Reihe, damit Summe stimmt
            if (row == ROWS - 1) {
                seatsInRow = TOTAL_SEATS - seatsGenerated;
            }
            seatsGenerated += seatsInRow;

            // Winkelberechnung: Voller Bogen von PI bis 0
            double angleStep = (seatsInRow > 1) ? Math.PI / (seatsInRow - 1) : 0;

            for (int s = 0; s < seatsInRow; s++) {
                double angle;
                if (seatsInRow > 1) {
                    // Start bei PI (Links), Ende bei 0 (Rechts)
                    angle = Math.PI - (angleStep * s);
                } else {
                    angle = Math.PI / 2; // Mitte
                }

                double x = Math.cos(angle) * currentRadius;
                double y = -Math.sin(angle) * currentRadius;
                layoutPoints.add(new Point(x, y, angle));
            }
        }

        // Sortieren von Links nach Rechts
        layoutPoints.sort((p1, p2) -> Double.compare(p2.angle, p1.angle));

        // 4. Mapping: Geometrie auf Parteien
        int limit = Math.min(layoutPoints.size(), seatPartyMap.size());
        for (int i = 0; i < limit; i++) {
            Point pt = layoutPoints.get(i);
            Party p = seatPartyMap.get(i);
            int row = (int) Math.round((Math.sqrt(pt.x*pt.x + pt.y*pt.y) - START_RADIUS) / ROW_STEP);
            seats.add(new Seat(pt.x, pt.y, pt.angle, row, p));
        }
    }

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

    // --- Drawing ---

    private void draw(double t) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w < 1 || h < 1) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        this.lastCx = w / 2;
        this.lastCy = h * 0.9; // Basis weit unten

        // Dynamische Skalierung inkl. Padding berechnen
        double contentWidth = MAX_RADIUS * 2 + 80; // +Padding
        double contentHeight = MAX_RADIUS + 80;
        double scaleW = w / contentWidth;
        double scaleH = h / contentHeight;
        this.lastScale = Math.min(scaleW, scaleH);

        if (lastScale < 0.1) return;

        drawBackground(gc, lastCx, lastCy, lastScale, t);
        drawNetwork(gc, lastCx, lastCy, lastScale, t);

        // Sitze zeichnen
        for (Seat seat : seats) {
            double animOffset = Math.max(0, 1.0 - (t * 2.0 - (seat.row * 0.1)));
            double currentScale = lastScale * (1.0 + animOffset * 0.5);
            double alpha = Math.min(1.0, t * 2.0 - (seat.row * 0.1));

            if (alpha <= 0) continue;

            // Logik für Hover/Selektion
            boolean isEmpty = (seat.party == null);
            boolean isSelected = (!isEmpty && selectedParty != null && selectedParty.equals(seat.party));
            boolean isHovered = (!isEmpty && hoveredParty != null && hoveredParty.equals(seat.party));
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

            double x = lastCx + (seat.x * currentScale);
            double y = lastCy + (seat.y * currentScale);

            Color c = isEmpty ? COL_EMPTY_SEAT : Color.GRAY;
            if (!isEmpty) {
                try { c = Color.web(seat.party.getColorCode()); } catch(Exception e) {}
            }

            // Sitz-Rechteck
            gc.setFill(c.deriveColor(0, 1, 1, 0.4));
            double size = 8.5 * lastScale;
            gc.fillRect(x - size/2, y - size/2, size, size);

            // Kern
            if (!isEmpty) {
                gc.setFill(c);
                double coreSize = 3.5 * lastScale;
                gc.fillRect(x - coreSize/2, y - coreSize/2, coreSize, coreSize);
            }
        }

        gc.setGlobalAlpha(1.0);
        gc.setEffect(null);

        drawMarkers(gc, lastCx, lastCy, lastScale);
        drawHUD(gc, w, h);
    }

    private void drawBackground(GraphicsContext gc, double cx, double cy, double scale, double t) {
        gc.setLineWidth(1.0);
        double outerR = (MAX_RADIUS + 20) * scale;

        gc.setStroke(Color.web("#222222"));
        gc.setLineDashes(5, 5);
        gc.strokeOval(cx - outerR, cy - outerR, outerR*2, outerR*2);

        gc.setStroke(COL_BG_LINES);
        gc.setLineDashes(null);
        // Strahlen
        for(int ang = 0; ang <= 180; ang += 15) {
            double rad = Math.toRadians(ang);
            double len = (MAX_RADIUS + 40) * scale;
            gc.strokeLine(cx, cy, cx + Math.cos(rad)*len, cy - Math.sin(rad)*len);
        }

        // Scan Effekt
        double scanR = (t * 250) % (MAX_RADIUS * 1.5);
        double rSc = scanR * scale;
        gc.setStroke(new LinearGradient(0,0,1,1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(0.5, Color.web("#D4AF37", 0.15)), new Stop(1, Color.TRANSPARENT)));
        gc.strokeOval(cx - rSc, cy - rSc, rSc*2, rSc*2);
    }

    private void drawNetwork(GraphicsContext gc, double cx, double cy, double scale, double t) {
        // Original implementation: Nur subtile Linien zur Mitte
        gc.setStroke(Color.web("#D4AF37", 0.05));
        gc.setLineWidth(1.0);
        int step = 10;
        for(int i=0; i<seats.size(); i+=step) {
            Seat s = seats.get(i);
            if(s.party != null && t > 0.5) {
                double sx = cx + s.x * scale;
                double sy = cy + s.y * scale;
                gc.strokeLine(sx, sy, cx, cy + 20*scale);
            }
        }
    }

    private void drawMarkers(GraphicsContext gc, double cx, double cy, double scale) {
        double r = (MAX_RADIUS + 15) * scale;
        gc.setFont(Font.font("Consolas", FontWeight.NORMAL, 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setStroke(COL_GOLD_DIM);
        gc.setFill(COL_TEXT);

        int idx = 1;
        for(int ang=0; ang<=180; ang+=30) {
            double rad = Math.toRadians(ang);
            double x1 = cx + Math.cos(rad) * (r - 5);
            double y1 = cy - Math.sin(rad) * (r - 5);
            double x2 = cx + Math.cos(rad) * (r + 10);
            double y2 = cy - Math.sin(rad) * (r + 10);

            gc.strokeLine(x1, y1, x2, y2);

            double tx = cx + Math.cos(rad) * (r + 25);
            double ty = cy - Math.sin(rad) * (r + 25);

            String lbl = (ang==90) ? "CENTER" : String.format("SEC-%02d", idx++);
            gc.fillText(lbl, tx, ty + 4);
        }
    }

    private void drawHUD(GraphicsContext gc, double w, double h) {
        // Header
        gc.setFill(new LinearGradient(0,0,0,1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#000000",0.8)), new Stop(1, Color.TRANSPARENT)));
        gc.fillRect(0,0,w,60);

        // Pult
        double cx = w/2;
        double cy = h * 0.9;
        gc.setStroke(COL_GOLD);
        gc.setLineWidth(1);
        gc.strokeLine(cx-50, cy+20, cx+50, cy+20);
        gc.strokeLine(cx-50, cy+20, cx-60, cy+30);
        gc.strokeLine(cx+50, cy+20, cx+60, cy+30);

        gc.setFill(COL_GOLD);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.fillText("COMMAND_PODIUM", cx, cy+45);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(COL_TEXT);
        gc.fillText("ENCRYPTION: AES-256", 20, h-20);
    }

    // --- Helper ---
    private static class Point { double x, y, angle; Point(double x, double y, double a){this.x=x;this.y=y;this.angle=a;}}
    private static class Seat {
        double x, y, angle; int row; Party party;
        Seat(double x, double y, double a, int r, Party p){this.x=x;this.y=y;this.angle=a;this.row=r;this.party=p;}
    }
}