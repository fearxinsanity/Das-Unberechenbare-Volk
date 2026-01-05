package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.SimulationConfig;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Zeichnet das Parlament mit politischer Sortierung (Links -> Rechts).
 */
public class ParliamentRenderer {

    private final Canvas canvas;
    private final List<Seat> seats = new ArrayList<>();
    private AnimationTimer introAnimation;
    private static final int TOTAL_SEATS = 300; // Mehr Sitze für dichtere Optik
    private static final int ROWS = 10;         // Mehr Reihen

    public ParliamentRenderer(Pane parentPane) {
        this.canvas = new Canvas(parentPane.getWidth(), parentPane.getHeight());
        canvas.widthProperty().bind(parentPane.widthProperty());
        canvas.heightProperty().bind(parentPane.heightProperty());
        parentPane.getChildren().add(canvas);

        canvas.widthProperty().addListener(obs -> { if(!seats.isEmpty()) draw(10.0); });
        canvas.heightProperty().addListener(obs -> { if(!seats.isEmpty()) draw(10.0); });
    }

    public void renderDistribution(List<Party> parties) {
        calculateSeatPositions(parties);
        startAnimation();
    }

    public void stop() {
        if (introAnimation != null) introAnimation.stop();
    }

    private void calculateSeatPositions(List<Party> parties) {
        seats.clear();
        double totalVotes = parties.stream().mapToDouble(Party::getCurrentSupporterCount).sum();

        // 1. Sortierung nach politischem Spektrum (DE-Standard)
        List<Party> sortedParties = parties.stream()
                .filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME))
                .sorted(new SpectrumComparator())
                .collect(Collectors.toList());

        // 2. Farb-Liste erstellen (Ein Eintrag pro Sitz)
        List<String> seatColorMap = new ArrayList<>();

        for (Party p : sortedParties) {
            double share = (totalVotes > 0) ? p.getCurrentSupporterCount() / totalVotes : 0;
            int seatsForParty = (int) Math.round(share * TOTAL_SEATS);

            for (int i = 0; i < seatsForParty; i++) {
                if (seatColorMap.size() < TOTAL_SEATS) {
                    seatColorMap.add(p.getColorCode());
                }
            }
        }
        // Auffüllen mit Grau, falls Rundungsfehler
        while (seatColorMap.size() < TOTAL_SEATS) {
            seatColorMap.add("444444");
        }

        // 3. Geometrische Berechnung der Sitze (Halbkreis)
        // Wir berechnen ALLE Sitzpositionen im Saal, sortieren sie von Links nach Rechts
        // und weisen dann die Farben zu. Das garantiert perfekte Blöcke.

        List<Point> layoutPoints = new ArrayList<>();
        double startRadius = 100.0;
        double rowStep = 25.0; // Engere Reihen

        for (int row = 0; row < ROWS; row++) {
            double currentRadius = startRadius + (row * rowStep);
            double arcLength = Math.PI * currentRadius;

            // Proportionale Anzahl Sitze pro Reihe
            double totalArcParams = 0;
            for(int r=0; r<ROWS; r++) totalArcParams += Math.PI * (startRadius + (r * rowStep));
            int seatsInRow = (int) (TOTAL_SEATS * (arcLength / totalArcParams));

            double angleStep = Math.PI / (seatsInRow + 1);

            for (int s = 0; s < seatsInRow; s++) {
                // Winkel von PI (Links) bis 0 (Rechts)
                double angle = Math.PI - (angleStep * (s + 1));

                // Koordinaten (Relativ zur Mitte)
                double x = Math.cos(angle) * currentRadius;
                double y = -Math.sin(angle) * currentRadius; // Nach oben

                // Wir speichern den Winkel für die spätere Sortierung
                layoutPoints.add(new Point(x, y, angle));
            }
        }

        // 4. Sortieren der physikalischen Plätze von LINKS (großer Winkel) nach RECHTS (kleiner Winkel)
        // Damit füllen wir die Parteien "streifenweise" auf.
        layoutPoints.sort((p1, p2) -> Double.compare(p2.angle, p1.angle));

        // 5. Farben zuweisen
        int limit = Math.min(layoutPoints.size(), seatColorMap.size());
        for (int i = 0; i < limit; i++) {
            Point pt = layoutPoints.get(i);
            seats.add(new Seat(pt.x, pt.y, seatColorMap.get(i)));
        }
    }

    /**
     * Sortiert Parteien nach deutscher Parlamentslogik (Links -> Rechts).
     */
    private static class SpectrumComparator implements Comparator<Party> {
        @Override
        public int compare(Party p1, Party p2) {
            return Integer.compare(getSpectrumIndex(p1), getSpectrumIndex(p2));
        }

        private int getSpectrumIndex(Party p) {
            String name = p.getName().toLowerCase();
            String abbr = p.getAbbreviation().toLowerCase();

            // Index: 0 (Links) bis 10 (Rechts)
            if (name.contains("linke") || abbr.contains("linke")) return 0;
            if (name.contains("spd") || abbr.contains("spd") || name.contains("sozial")) return 1;
            if (name.contains("grüne") || abbr.contains("grüne") || name.contains("green")) return 2;
            if (name.contains("fdp") || abbr.contains("fdp") || name.contains("libera")) return 3;
            if (name.contains("cdu") || abbr.contains("cdu") || name.contains("csu") || name.contains("christ")) return 4;
            if (name.contains("afd") || abbr.contains("afd") || name.contains("alternative")) return 5;

            return 10; // Sonstige (ganz rechts außen oder separat)
        }
    }

    private void startAnimation() {
        final long startNano = System.nanoTime();
        introAnimation = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = (now - startNano) / 1_000_000_000.0;
                draw(t);
                if (t > 2.0) stop();
            }
        };
        introAnimation.start();
    }

    private void draw(double time) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cx = w / 2;
        double cy = h * 0.9; // Basis weit unten

        gc.clearRect(0, 0, w, h);
        gc.setEffect(new Glow(0.6));

        double scale = Math.min(w, h) / 600.0;

        for (Seat seat : seats) {
            // Sweep von Links nach Rechts
            double normalizedX = (seat.x + 400) / 800.0;
            if (time < normalizedX * 1.5) continue;

            gc.setFill(Color.web("#" + seat.color));
            double drawX = cx + (seat.x * scale);
            double drawY = cy + (seat.y * scale);
            double size = 7.0 * scale;

            gc.fillOval(drawX - size/2, drawY - size/2, size, size);
        }

        // Pult
        gc.setFill(Color.web("#555555"));
        gc.fillRect(cx - 30*scale, cy - 10*scale, 60*scale, 20*scale);

        gc.setEffect(null);
    }

    private static class Point { double x, y, angle; Point(double x, double y, double a) {this.x=x; this.y=y; this.angle=a;} }
    private static class Seat { double x, y; String color; Seat(double x, double y, String c) {this.x=x; this.y=y; this.color=c;} }
}