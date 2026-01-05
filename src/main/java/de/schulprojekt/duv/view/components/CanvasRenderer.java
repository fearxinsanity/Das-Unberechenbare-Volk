package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.util.SimulationConfig;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.model.party.Party;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.*;

public class CanvasRenderer {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final AnimationTimer visualTimer;
    private final Map<String, Point> partyPositions = new HashMap<>();
    private final Stack<MovingVoter> particlePool = new Stack<>();
    private final List<MovingVoter> activeParticles = new ArrayList<>();
    private List<Party> currentParties = new ArrayList<>();
    private int currentTotalVoters = 1;
    private double currentScaleFactor = 1.0;

    // Rotation für Target-Lock Animation
    private double targetRotationAngle = 0;

    public CanvasRenderer(Pane animationPane) {
        this.canvas = new Canvas(0, 0);
        canvas.setManaged(true);
        canvas.widthProperty().bind(animationPane.widthProperty());
        canvas.heightProperty().bind(animationPane.heightProperty());
        animationPane.getChildren().add(0, canvas);
        this.gc = canvas.getGraphicsContext2D();

        javafx.beans.value.ChangeListener<Number> resizeListener = (obs, oldVal, newVal) -> {
            if (canvas.getWidth() > 0 && canvas.getHeight() > 0 && !currentParties.isEmpty()) {
                recalculatePartyPositions(currentParties);
                renderCanvas();
            }
        };
        canvas.widthProperty().addListener(resizeListener);
        canvas.heightProperty().addListener(resizeListener);

        this.visualTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                targetRotationAngle += 1.5; // Konstante Rotation
                renderCanvas();
            }
        };
    }

    public Canvas getCanvas() { return canvas; }
    public Map<String, Point> getPartyPositions() { return partyPositions; }
    public void startVisualTimer() { visualTimer.start(); }
    public void stop() { visualTimer.stop(); }

    public void clear(List<Party> parties) {
        activeParticles.forEach(particlePool::push);
        activeParticles.clear();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        recalculatePartyPositions(parties);
    }

    public void update(List<Party> parties, List<VoterTransition> transitions, int totalVoters) {
        this.currentParties = parties;
        this.currentTotalVoters = Math.max(1, totalVoters);
        if (partyPositions.size() != parties.size()) recalculatePartyPositions(parties);
        spawnParticles(transitions);
    }

    private void recalculatePartyPositions(List<Party> parties) {
        partyPositions.clear();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cx = w / 2;
        double cy = h / 2;
        double minDim = Math.min(w, h);
        this.currentScaleFactor = Math.max(0.6, minDim / 800.0);
        double r = minDim * 0.35;

        for (int i = 0; i < parties.size(); i++) {
            if (parties.get(i).getName().equals(SimulationConfig.UNDECIDED_NAME)) {
                partyPositions.put(parties.get(i).getName(), new Point(cx, cy));
                continue;
            }
            double angle = 2 * Math.PI * i / parties.size() - Math.PI / 2;
            partyPositions.put(parties.get(i).getName(), new Point(cx + r * Math.cos(angle), cy + r * Math.sin(angle)));
        }
    }

    private void spawnParticles(List<VoterTransition> transitions) {
        int limit = 0;
        for (VoterTransition t : transitions) {
            if (limit++ > 50) break; // Limitieren für Performance
            Point s = partyPositions.get(t.getOldParty().getName());
            Point e = partyPositions.get(t.getNewParty().getName());
            if (s != null && e != null) {
                MovingVoter p = particlePool.isEmpty() ? new MovingVoter() : particlePool.pop();
                p.reset(s.x, s.y, e.x, e.y, Color.web(t.getNewParty().getColorCode()));
                activeParticles.add(p);
            }
        }
    }

    private void renderCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (currentParties.isEmpty()) return;

        // Führende Partei ermitteln für Target-Lock
        Party leader = currentParties.stream()
                .filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME))
                .max(Comparator.comparingInt(Party::getCurrentSupporterCount))
                .orElse(null);

        // 1. Netzwerk-Grid (Gestrichelt)
        gc.setLineDashes(4, 6); // Gestrichelte Linien
        gc.setStroke(Color.web("#D4AF37", 0.1));
        gc.setLineWidth(1.0 * currentScaleFactor);

        for (int i = 0; i < currentParties.size(); i++) {
            Point p1 = partyPositions.get(currentParties.get(i).getName());
            for (int j = i + 1; j < currentParties.size(); j++) {
                Point p2 = partyPositions.get(currentParties.get(j).getName());
                if (p1 != null && p2 != null) gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
        gc.setLineDashes(null); // Reset

        // 2. Parteien zeichnen
        for (Party p : currentParties) {
            Point pt = partyPositions.get(p.getName());
            if (pt != null) {
                Color pColor = p.getName().equals(SimulationConfig.UNDECIDED_NAME)
                        ? Color.web("#666666") : Color.web(p.getColorCode());

                double share = (double) p.getCurrentSupporterCount() / currentTotalVoters;

                // --- ÄNDERUNG: Dynamischere Skalierung ---
                // Math.pow(share, 0.7) sorgt dafür, dass Unterschiede stärker betont werden,
                // aber kleine Parteien nicht komplett verschwinden.
                double dynamicSize = (30.0 + (Math.pow(share, 0.7) * 120.0)) * currentScaleFactor;

                double half = dynamicSize / 2.0;

                // A. Quadratischer Hintergrund (Tech Look)
                gc.setFill(pColor.deriveColor(0, 1.0, 1.0, 0.2));
                gc.fillRect(pt.x - half, pt.y - half, dynamicSize, dynamicSize);

                // B. Eckiger Rahmen
                gc.setStroke(pColor);
                gc.setLineWidth(1.5 * currentScaleFactor);
                gc.strokeRect(pt.x - half, pt.y - half, dynamicSize, dynamicSize);

                // C. Target Lock (Nur für Leader)
                if (p == leader) {
                    drawTargetLock(gc, pt.x, pt.y, dynamicSize * 1.3, pColor);
                }

                // D. Crosshair Marker (für alle)
                drawCrosshair(gc, pt.x, pt.y, dynamicSize * 0.8, pColor);

                // E. Text
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12 * currentScaleFactor));
                gc.setFill(Color.web("#e0e0e0"));
                // Text etwas unterhalb des Quadrats platzieren
                gc.fillText(p.getAbbreviation(), pt.x, pt.y + half + 15);

                // Prozent (Gold)
                gc.setFill(Color.web("#D4AF37"));
                gc.fillText(String.format("%.1f%%", share * 100), pt.x, pt.y + half + 28);
            }
        }

        // 3. Partikel (Data Packets)
        Iterator<MovingVoter> it = activeParticles.iterator();
        while (it.hasNext()) {
            MovingVoter p = it.next();
            p.move();

            // Zeichne Tracer (Schweif)
            double trailLen = 15.0 * currentScaleFactor;
            // Winkel berechnen für Ausrichtung
            double angle = Math.atan2(p.y - p.startY, p.x - p.startX);
            if (p.progress < 0.1) angle = Math.atan2(p.targetY - p.startY, p.targetX - p.startX);

            double tailX = p.x - Math.cos(angle) * trailLen;
            double tailY = p.y - Math.sin(angle) * trailLen;

            gc.setStroke(p.color);
            gc.setLineWidth(2.0 * currentScaleFactor);
            gc.strokeLine(tailX, tailY, p.x, p.y);

            // High-Energy Kern (Weißes Quadrat am Kopf)
            gc.setFill(Color.WHITE);
            double headSize = 3.0 * currentScaleFactor;
            gc.fillRect(p.x - headSize/2, p.y - headSize/2, headSize, headSize);

            // Glow
            gc.setEffect(new javafx.scene.effect.Glow(0.8));

            if (p.hasArrived()) { it.remove(); particlePool.push(p); }
        }
        gc.setEffect(null);
    }

    private void drawTargetLock(GraphicsContext gc, double x, double y, double size, Color color) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(targetRotationAngle); // Rotierender Rahmen

        gc.setStroke(Color.RED); // Aggressives Rot für Lock
        gc.setLineWidth(2.0);
        double s = size / 2.0;
        double len = s * 0.3; // Länge der Ecken

        // Zeichne 4 Ecken (Klammern)
        gc.strokeLine(-s, -s, -s + len, -s); gc.strokeLine(-s, -s, -s, -s + len); // Oben Links
        gc.strokeLine(s, -s, s - len, -s);   gc.strokeLine(s, -s, s, -s + len);   // Oben Rechts
        gc.strokeLine(-s, s, -s + len, s);   gc.strokeLine(-s, s, -s, s - len);   // Unten Links
        gc.strokeLine(s, s, s - len, s);     gc.strokeLine(s, s, s, s - len);     // Unten Rechts

        gc.restore();
    }

    private void drawCrosshair(GraphicsContext gc, double x, double y, double size, Color color) {
        gc.setStroke(color.deriveColor(0, 1, 1, 0.5));
        gc.setLineWidth(1.0);
        double len = 5 * currentScaleFactor;
        // Kleines Kreuz in der Mitte
        gc.strokeLine(x - len, y, x + len, y);
        gc.strokeLine(x, y - len, x, y + len);
    }

    public static class Point { public double x, y; public Point(double x, double y) { this.x = x; this.y = y; } }

    private static class MovingVoter {
        double startX, startY, targetX, targetY, x, y, progress, speedStep; Color color; boolean arrived;
        void reset(double sx, double sy, double tx, double ty, Color c) {
            this.startX = sx + (Math.random() - 0.5) * 15.0; this.startY = sy + (Math.random() - 0.5) * 15.0;
            this.targetX = tx + (Math.random() - 0.5) * 15.0; this.targetY = ty + (Math.random() - 0.5) * 15.0;
            this.x = startX; this.y = startY; this.color = c; this.progress = 0.0; this.arrived = false;
            this.speedStep = 0.010 + (Math.random() * 0.015);
        }
        void move() {
            if (arrived) return; progress += speedStep; if (progress >= 1.0) { progress = 1.0; arrived = true; }
            double t = progress * progress * (3 - 2 * progress);
            this.x = startX + (targetX - startX) * t;
            this.y = startY + (targetY - startY) * t;
        }
        boolean hasArrived() { return arrived; }
    }
}