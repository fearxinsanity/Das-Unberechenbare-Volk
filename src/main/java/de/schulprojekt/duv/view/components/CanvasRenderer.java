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
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cx = w / 2;
        double cy = h / 2;

        double minDim = Math.min(w, h);
        double scaleRef = minDim / 800.0;
        this.currentScaleFactor = Math.max(0.6, scaleRef);

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
            if (limit++ > 50) break;
            Point s = partyPositions.get(t.getOldParty().getName());
            Point e = partyPositions.get(t.getNewParty().getName());
            if (s != null && e != null) {
                MovingVoter p = particlePool.isEmpty() ? new MovingVoter() : particlePool.pop();
                // Verwende die Farbe der Zielpartei für das Partikel
                p.reset(s.x, s.y, e.x, e.y, Color.web(t.getNewParty().getColorCode()));
                activeParticles.add(p);
            }
        }
    }

    private void renderCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (currentParties.isEmpty()) return;

        // 1. Verbindungslinien (DESIGN ÄNDERUNG: Gold, wie im Start-Screen)
        gc.setStroke(Color.web("#D4AF37", 0.15)); // Gold, niedrige Opazität
        gc.setLineWidth(0.8 * currentScaleFactor);

        // Zeichne Linien zu allen anderen (Netzwerk-Stil)
        for (int i = 0; i < currentParties.size(); i++) {
            Point p1 = partyPositions.get(currentParties.get(i).getName());
            for (int j = i + 1; j < currentParties.size(); j++) {
                Point p2 = partyPositions.get(currentParties.get(j).getName());
                if (p1 != null && p2 != null) gc.strokeLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        // 2. Parteien zeichnen
        for (Party p : currentParties) {
            Point pt = partyPositions.get(p.getName());
            if (pt != null) {
                // Farbe: Entweder Partei-Farbe oder Grau für Unsicher
                Color pColor = p.getName().equals(SimulationConfig.UNDECIDED_NAME)
                        ? Color.web("#666666")
                        : Color.web(p.getColorCode());

                double share = (double) p.getCurrentSupporterCount() / currentTotalVoters;
                double r = (30.0 + (share * 60.0)) * currentScaleFactor;
                double d = r * 2;

                // A. GLOW (Radial Gradient) - Angepasst an Gold-Look
                RadialGradient gradient = new RadialGradient(
                        0, 0, pt.x, pt.y, r, false, CycleMethod.NO_CYCLE,
                        new Stop(0.0, pColor.deriveColor(0, 1.0, 1.0, 0.4)),
                        new Stop(1.0, Color.TRANSPARENT)
                );

                gc.setFill(gradient);
                gc.fillOval(pt.x - r, pt.y - r, d, d);

                // B. Kern (Tech-Look: Solide mit Gold-Rand)
                double coreSize = 16.0 * currentScaleFactor;
                double coreOffset = coreSize / 2.0;

                gc.setFill(Color.web("#1a1a1d")); // Dunkler Kern
                gc.fillOval(pt.x - coreOffset, pt.y - coreOffset, coreSize, coreSize);

                gc.setStroke(pColor); // Farbiger Ring
                gc.setLineWidth(2.0 * currentScaleFactor);
                gc.strokeOval(pt.x - coreOffset, pt.y - coreOffset, coreSize, coreSize);

                // C. Text Labels (Consolas, Tech-Style)
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12 * currentScaleFactor));

                // Kürzel
                gc.setFill(Color.web("#e0e0e0"));
                gc.fillText(p.getAbbreviation(), pt.x, pt.y + (35 * currentScaleFactor));

                // Prozent
                gc.setFill(Color.web("#D4AF37")); // Goldene Zahlen
                gc.fillText(String.format("%.1f%%", share * 100), pt.x, pt.y + (48 * currentScaleFactor));
            }
        }

        // 3. Partikel (DESIGN ÄNDERUNG: Punkte statt Striche)
        Iterator<MovingVoter> it = activeParticles.iterator();
        while (it.hasNext()) {
            MovingVoter p = it.next();
            p.move();

            // Partikelgröße und Farbe
            double size = 3.0 * currentScaleFactor;

            // Leuchteffekt für Partikel
            gc.setGlobalAlpha(p.getOpacity());
            gc.setFill(p.color.brighter());
            gc.fillOval(p.x - size/2, p.y - size/2, size, size);

            // Glow um Partikel (optional für Performance, hier einfach größerer kreis mit weniger opacity)
            gc.setGlobalAlpha(p.getOpacity() * 0.3);
            gc.fillOval(p.x - size, p.y - size, size * 2, size * 2);

            gc.setGlobalAlpha(1.0);

            if (p.hasArrived()) { it.remove(); particlePool.push(p); }
        }
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
            // Ease-In-Out Bewegung
            double t = progress * progress * (3 - 2 * progress);
            this.x = startX + (targetX - startX) * t;
            this.y = startY + (targetY - startY) * t;
        }
        double getOpacity() { return progress < 0.1 ? progress / 0.1 : (progress > 0.9 ? (1.0 - progress) / 0.1 : 1.0); }
        boolean hasArrived() { return arrived; }
    }
}