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

    public CanvasRenderer(Pane animationPane) {
        // Initialgröße 0, damit Layout nicht blockiert wird
        this.canvas = new Canvas(0, 0);

        // Responsive Logic:
        canvas.setManaged(true);
        canvas.widthProperty().bind(animationPane.widthProperty());
        canvas.heightProperty().bind(animationPane.heightProperty());

        animationPane.getChildren().add(0, canvas);
        this.gc = canvas.getGraphicsContext2D();

        // Listener: Bei Größenänderung sofort neu berechnen
        javafx.beans.value.ChangeListener<Number> resizeListener = (obs, oldVal, newVal) -> {
            if (canvas.getWidth() > 0 && canvas.getHeight() > 0 && !currentParties.isEmpty()) {
                recalculatePartyPositions(currentParties);
                renderCanvas();
            }
        };

        canvas.widthProperty().addListener(resizeListener);
        canvas.heightProperty().addListener(resizeListener);

        this.visualTimer = new AnimationTimer() {
            @Override public void handle(long now) { renderCanvas(); }
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

        double cx = canvas.getWidth() / 2;
        double cy = canvas.getHeight() / 2;
        // Radius für die Anordnung der Parteien (35% der kleineren Seite)
        double r = Math.min(canvas.getWidth(), canvas.getHeight()) * 0.35;

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
            if (limit++ > 50) break;
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

        // 1. Verbindungslinien
        gc.setStroke(Color.web("#D4AF37", 0.15)); gc.setLineWidth(0.8);
        for (int i = 0; i < currentParties.size(); i++) {
            Point p1 = partyPositions.get(currentParties.get(i).getName());
            for (int j = i + 1; j < currentParties.size(); j++) {
                Point p2 = partyPositions.get(currentParties.get(j).getName());
                if (p1 != null && p2 != null) gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        // 2. Parteien zeichnen (RE-DESIGN: Zurück zum Gradienten)
        for (Party p : currentParties) {
            Point pt = partyPositions.get(p.getName());
            if (pt != null) {
                Color pColor = Color.web(p.getColorCode());
                // Farbe etwas "magischer" machen für den Glow
                Color mysteryColor = pColor.deriveColor(0, 0.8, 0.9, 1.0);

                double share = (double) p.getCurrentSupporterCount() / currentTotalVoters;
                // Basis-Größe + Anteil
                double r = 30.0 + (share * 60.0);
                double d = r * 2; // Durchmesser

                // A. GLOW (Radial Gradient)
                // Wir nutzen exakte Koordinaten (pt.x, pt.y) als Zentrum für absolute Symmetrie
                RadialGradient gradient = new RadialGradient(
                        0, 0, pt.x, pt.y, r, false, CycleMethod.NO_CYCLE,
                        new Stop(0.0, mysteryColor.deriveColor(0, 1.0, 1.0, 0.7)),  // Kern: Hell & Deckend
                        new Stop(0.6, mysteryColor.deriveColor(0, 1.0, 0.8, 0.3)),  // Mitte: Leuchtend
                        new Stop(1.0, Color.TRANSPARENT)                            // Rand: Unsichtbar
                );

                gc.setFill(gradient);
                // WICHTIG: width und height sind identisch (d), damit es ein Kreis bleibt!
                gc.fillOval(pt.x - r, pt.y - r, d, d);

                // B. Kern (Fester Punkt in der Mitte)
                gc.setFill(mysteryColor.brighter());
                gc.fillOval(pt.x - 10, pt.y - 10, 20, 20);

                // C. Rand um den Kern (Gold)
                gc.setStroke(Color.web("#D4AF37"));
                gc.setLineWidth(1.5);
                gc.strokeOval(pt.x - 10, pt.y - 10, 20, 20);

                // D. Text Labels
                // Zentriert unter dem Kreis, damit es symmetrisch wirkt
                gc.setTextAlign(TextAlignment.CENTER);

                gc.setFill(Color.web("#e0e0e0"));
                gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                gc.fillText(p.getAbbreviation(), pt.x, pt.y + 35); // Etwas tiefer setzen

                gc.setFill(Color.web("#D4AF37"));
                gc.fillText(String.format("%.1f%%", share * 100), pt.x, pt.y + 48);

                gc.setTextAlign(TextAlignment.LEFT); // Reset
            }
        }

        // 3. Partikel
        Iterator<MovingVoter> it = activeParticles.iterator();
        gc.setLineWidth(2.5);
        while (it.hasNext()) {
            MovingVoter p = it.next(); p.move();
            double alpha = p.getOpacity();
            for (int i = 0; i < 5; i++) {
                double segAlpha = alpha * (1.0 - ((double)i / 5));
                if (segAlpha < 0.05) continue;
                gc.setGlobalAlpha(segAlpha); gc.setStroke(p.color.deriveColor(0, 1.0, 1.0, 1.0));
                double bx = p.x - (p.dx * i * 1.5), by = p.y - (p.dy * i * 1.5);
                gc.strokeLine(bx, by, bx - p.dx, by - p.dy);
            }
            if (p.hasArrived()) { it.remove(); particlePool.push(p); }
        }
        gc.setGlobalAlpha(1.0);
    }

    public static class Point { public double x, y; public Point(double x, double y) { this.x = x; this.y = y; } }

    private static class MovingVoter {
        double startX, startY, targetX, targetY, x, y, dx, dy, progress, speedStep; Color color; boolean arrived;
        void reset(double sx, double sy, double tx, double ty, Color c) {
            this.startX = sx + (Math.random() - 0.5) * 15.0; this.startY = sy + (Math.random() - 0.5) * 15.0;
            this.targetX = tx + (Math.random() - 0.5) * 15.0; this.targetY = ty + (Math.random() - 0.5) * 15.0;
            this.x = startX; this.y = startY; this.color = c; this.progress = 0.0; this.arrived = false;
            this.speedStep = 0.010 + (Math.random() * 0.015); this.dx = 0; this.dy = 0;
        }
        void move() {
            if (arrived) return; progress += speedStep; if (progress >= 1.0) { progress = 1.0; arrived = true; }
            double t = progress * progress * (3 - 2 * progress);
            double nX = startX + (targetX - startX) * t, nY = startY + (targetY - startY) * t;
            this.dx = nX - x; this.dy = nY - y; this.x = nX; this.y = nY;
        }
        double getOpacity() { return progress < 0.15 ? progress / 0.15 : (progress > 0.85 ? (1.0 - progress) / 0.15 : 1.0); }
        boolean hasArrived() { return arrived; }
    }
}