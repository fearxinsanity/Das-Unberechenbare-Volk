package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.util.SimulationConfig;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.*;

public class CanvasRenderer {
    private final Canvas canvas;
    private final GraphicsContext gc;

    private final Map<String, Point> partyPositions = new HashMap<>();
    private final Stack<MovingVoter> particlePool = new Stack<>();
    private final List<MovingVoter> activeParticles = new ArrayList<>();

    // Zum Erkennen von Größenänderungen
    private double lastWidth = 0;
    private double lastHeight = 0;

    public CanvasRenderer(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
    }

    public void recalculatePositions(List<Party> parties) {
        partyPositions.clear();
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        if (width <= 0 || height <= 0) return;

        double cx = width / 2;
        double cy = height / 2;
        double radius = Math.min(cx, cy) * 0.75;

        int count = parties.size();
        for (int i = 0; i < count; i++) {
            Party p = parties.get(i);
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) {
                partyPositions.put(p.getName(), new Point(cx, cy));
                continue;
            }
            double angle = 2 * Math.PI * i / count - Math.PI / 2;
            double x = cx + radius * Math.cos(angle);
            double y = cy + radius * Math.sin(angle);
            partyPositions.put(p.getName(), new Point(x, y));
        }
    }

    public void render(List<Party> parties, int totalVoters) {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        gc.clearRect(0, 0, width, height);

        // FIX: Automatisch neu berechnen, wenn Größe sich ändert
        if (width != lastWidth || height != lastHeight || (partyPositions.isEmpty() && !parties.isEmpty())) {
            recalculatePositions(parties);
            lastWidth = width;
            lastHeight = height;
        }

        // 1. Verbindungslinien
        gc.setStroke(Color.web("#D4AF37", 0.15));
        gc.setLineWidth(0.8);
        for (int i = 0; i < parties.size(); i++) {
            Point p1 = partyPositions.get(parties.get(i).getName());
            if (p1 == null) continue;
            for (int j = i + 1; j < parties.size(); j++) {
                Point p2 = partyPositions.get(parties.get(j).getName());
                if (p2 == null) continue;
                gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        // 2. Parteien
        for (Party p : parties) {
            drawParty(p, totalVoters);
        }

        // 3. Partikel
        renderParticles();
    }

    private void drawParty(Party p, int totalVoters) {
        Point pt = partyPositions.get(p.getName());
        if (pt == null) return;

        String hex = p.getColorCode();
        if (hex != null && !hex.startsWith("#")) hex = "#" + hex;
        Color pColor = (hex != null) ? Color.web(hex) : Color.GRAY;

        Color mysteryColor = pColor.deriveColor(0, 0.8, 0.9, 1.0);
        double share = (double) p.getCurrentSupporterCount() / totalVoters;
        double dynamicRadius = 30.0 + (share * 60.0);

        RadialGradient glow = new RadialGradient(
                0, 0, pt.x, pt.y, dynamicRadius, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, mysteryColor.deriveColor(0, 1.0, 1.0, 0.7)),
                new Stop(0.6, mysteryColor.deriveColor(0, 1.0, 0.6, 0.2)),
                new Stop(1.0, Color.TRANSPARENT)
        );

        gc.setFill(glow);
        gc.fillOval(pt.x - dynamicRadius, pt.y - dynamicRadius, dynamicRadius * 2, dynamicRadius * 2);

        gc.setGlobalAlpha(1.0);
        gc.setFill(mysteryColor.brighter());
        gc.fillOval(pt.x - 10, pt.y - 10, 20, 20);
        gc.setStroke(Color.web("#D4AF37"));
        gc.setLineWidth(1.5);
        gc.strokeOval(pt.x - 10, pt.y - 10, 20, 20);

        gc.setFill(Color.web("#e0e0e0"));
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(p.getAbbreviation(), pt.x, pt.y + 35);

        gc.setFill(Color.web("#D4AF37"));
        gc.fillText(String.format("%.1f%%", share * 100), pt.x, pt.y + 48);
    }

    public void spawnParticles(List<VoterTransition> transitions) {
        int limit = 0;
        for (VoterTransition t : transitions) {
            if (limit++ > 50) break;

            Point start = partyPositions.get(t.getOldParty().getName());
            Point end = partyPositions.get(t.getNewParty().getName());

            if (start != null && end != null) {
                String hex = t.getNewParty().getColorCode();
                if (hex != null && !hex.startsWith("#")) hex = "#" + hex;

                MovingVoter p = particlePool.isEmpty() ? new MovingVoter() : particlePool.pop();
                p.reset(start.x, start.y, end.x, end.y, Color.web(hex));
                activeParticles.add(p);
            }
        }
    }

    private void renderParticles() {
        Iterator<MovingVoter> it = activeParticles.iterator();
        gc.setLineWidth(2.5);

        while (it.hasNext()) {
            MovingVoter p = it.next();
            p.move();

            double baseAlpha = p.getOpacity();
            double trailLength = 5;
            for (int i = 0; i < trailLength; i++) {
                double segmentAlpha = baseAlpha * (1.0 - ((double)i / trailLength));
                if (segmentAlpha < 0.05) continue;
                gc.setGlobalAlpha(segmentAlpha);
                gc.setStroke(p.color.deriveColor(0, 1.0, 1.0, 1.0));

                double backX = p.x - (p.dx * i * 1.5);
                double backY = p.y - (p.dy * i * 1.5);
                double prevX = p.x - (p.dx * (i * 1.5 + 1));
                double prevY = p.y - (p.dy * (i * 1.5 + 1));
                gc.strokeLine(backX, backY, prevX, prevY);
            }

            if (p.hasArrived()) {
                it.remove();
                particlePool.push(p);
            }
        }
        gc.setGlobalAlpha(1.0);
    }

    public void clearParticles() { activeParticles.clear(); }
    public Point getPartyPosition(String name) { return partyPositions.get(name); }

    public static class Point {
        public final double x, y;
        public Point(double x, double y) { this.x = x; this.y = y; }
    }

    private static class MovingVoter {
        double startX, startY, targetX, targetY, x, y, dx, dy, progress, speedStep;
        Color color;
        boolean arrived;

        void reset(double sx, double sy, double tx, double ty, Color c) {
            double spread = 15.0;
            this.startX = sx + (Math.random() - 0.5) * spread;
            this.startY = sy + (Math.random() - 0.5) * spread;
            this.targetX = tx + (Math.random() - 0.5) * spread;
            this.targetY = ty + (Math.random() - 0.5) * spread;
            this.x = startX; this.y = startY; this.color = c;
            this.progress = 0.0; this.arrived = false;
            this.speedStep = 0.010 + (Math.random() * 0.015);
            this.dx = 0; this.dy = 0;
        }

        void move() {
            if (arrived) return;
            progress += speedStep;
            if (progress >= 1.0) { progress = 1.0; arrived = true; }
            double t = progress * progress * (3 - 2 * progress);
            double newX = startX + (targetX - startX) * t;
            double newY = startY + (targetY - startY) * t;
            this.dx = newX - x; this.dy = newY - y;
            this.x = newX; this.y = newY;
        }

        double getOpacity() {
            if (progress < 0.15) return progress / 0.15;
            else if (progress > 0.85) return (1.0 - progress) / 0.15;
            return 1.0;
        }
        boolean hasArrived() { return arrived; }
    }
}