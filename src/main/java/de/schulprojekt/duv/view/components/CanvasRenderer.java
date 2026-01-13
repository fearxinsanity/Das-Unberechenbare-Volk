package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.util.SimulationConfig;
import javafx.animation.AnimationTimer;
import javafx.beans.InvalidationListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.*;

/**
 * Handles the graphical visualization of the simulation.
 * Renders the party network, nodes, and moving voter particles on a JavaFX Canvas.
 */
public class CanvasRenderer {

    // --- Constants ---
    private static final double ROTATION_SPEED = 1.5;
    private static final int PARTICLE_SPAWN_LIMIT_PER_TICK = 50;
    private static final double TARGET_LOCK_SCALE = 1.3;
    private static final int MAX_ACTIVE_PARTICLES = 1200;

    private static final Color GRID_COLOR = Color.web("#D4AF37", 0.1);
    private static final Color TEXT_COLOR = Color.web("#e0e0e0");
    private static final Color PERCENTAGE_COLOR = Color.web("#D4AF37");

    // Fallback color if party color is invalid
    private static final Color ERROR_COLOR = Color.MAGENTA;

    // --- UI Components ---
    private final Canvas canvas;
    private final GraphicsContext gc;
    private final AnimationTimer visualTimer;

    // --- State & Data ---
    private final Map<String, Point> partyPositions = new HashMap<>();
    private List<Party> currentParties = new ArrayList<>();
    private int currentTotalVoters = 1;
    private double currentScaleFactor = 1.0;
    private double targetRotationAngle = 0;

    // --- Particle System (Object Pooling) ---
    private final Stack<MovingVoter> particlePool = new Stack<>();
    private final List<MovingVoter> activeParticles = new ArrayList<>();

    // --- Constructor ---

    public CanvasRenderer(Pane animationPane) {
        this.canvas = new Canvas(0, 0);
        this.canvas.setManaged(true);

        this.canvas.widthProperty().bind(animationPane.widthProperty());
        this.canvas.heightProperty().bind(animationPane.heightProperty());

        // Java 21 syntax
        animationPane.getChildren().addFirst(canvas);
        this.gc = canvas.getGraphicsContext2D();

        InvalidationListener resizeListener = ignored -> {
            if (canvas.getWidth() > 0 && canvas.getHeight() > 0 && !currentParties.isEmpty()) {
                recalculatePartyPositions(currentParties);
                renderCanvas();
            }
        };

        canvas.widthProperty().addListener(resizeListener);
        canvas.heightProperty().addListener(resizeListener);

        this.visualTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                targetRotationAngle += ROTATION_SPEED;
                renderCanvas();
            }
        };
    }

    // --- Public API ---

    public Canvas getCanvas() {
        return canvas;
    }

    public Map<String, Point> getPartyPositions() {
        return partyPositions;
    }

    public void startVisualTimer() {
        visualTimer.start();
    }

    public void stop() {
        visualTimer.stop();
    }

    public void clear(List<Party> parties) {
        activeParticles.forEach(particlePool::push);
        activeParticles.clear();

        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        recalculatePartyPositions(parties);
    }

    public void update(List<Party> parties, List<VoterTransition> transitions, int totalVoters) {
        this.currentParties = parties;
        this.currentTotalVoters = Math.max(1, totalVoters);

        if (partyPositions.size() != parties.size()) {
            recalculatePartyPositions(parties);
        }

        spawnParticles(transitions);
    }

    // --- Rendering Logic ---

    private void renderCanvas() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (currentParties.isEmpty()) return;

        Party leader = currentParties.stream()
                .filter(p -> !p.getName().equals(SimulationConfig.UNDECIDED_NAME))
                .max(Comparator.comparingInt(Party::getCurrentSupporterCount))
                .orElse(null);

        drawNetworkGrid();
        drawPartyNodes(leader);
        drawParticles();
    }

    private void drawNetworkGrid() {
        gc.setLineDashes(4, 6);
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(currentScaleFactor);

        for (int i = 0; i < currentParties.size(); i++) {
            Point p1 = partyPositions.get(currentParties.get(i).getName());
            for (int j = i + 1; j < currentParties.size(); j++) {
                Point p2 = partyPositions.get(currentParties.get(j).getName());
                if (p1 != null && p2 != null) {
                    gc.strokeLine(p1.x(), p1.y(), p2.x(), p2.y());
                }
            }
        }
        gc.setLineDashes((double[]) null);
    }

    private void drawPartyNodes(Party leader) {
        for (Party p : currentParties) {
            Point pt = partyPositions.get(p.getName());
            if (pt == null) continue;

            // FIX: Use safe color parsing
            Color pColor = safeParseColor(p);

            double share = (double) p.getCurrentSupporterCount() / currentTotalVoters;
            double dynamicSize = (30.0 + (Math.pow(share, 0.7) * 120.0)) * currentScaleFactor;
            double half = dynamicSize / 2.0;

            // A. Background
            gc.setFill(pColor.deriveColor(0, 1.0, 1.0, 0.2));
            gc.fillRect(pt.x() - half, pt.y() - half, dynamicSize, dynamicSize);

            // B. Frame
            gc.setStroke(pColor);
            gc.setLineWidth(1.5 * currentScaleFactor);
            gc.strokeRect(pt.x() - half, pt.y() - half, dynamicSize, dynamicSize);

            // C. Target Lock (Leader only)
            if (p == leader) {
                drawTargetLock(pt.x(), pt.y(), dynamicSize * TARGET_LOCK_SCALE);
            }

            // D. Crosshair
            drawCrosshair(pt.x(), pt.y(), dynamicSize * 0.8, pColor);

            // E. Labels
            drawNodeLabels(p, pt, half, share);
        }
    }

    private void drawNodeLabels(Party p, Point pt, double halfSize, double share) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12 * currentScaleFactor));

        gc.setFill(TEXT_COLOR);
        gc.fillText(p.getAbbreviation(), pt.x(), pt.y() + halfSize + 15);

        gc.setFill(PERCENTAGE_COLOR);
        gc.fillText(String.format("%.1f%%", share * 100), pt.x(), pt.y() + halfSize + 28);
    }

    private void drawParticles() {
        Iterator<MovingVoter> it = activeParticles.iterator();
        while (it.hasNext()) {
            MovingVoter p = it.next();
            p.move();

            double trailLen = 15.0 * currentScaleFactor;
            double angle = Math.atan2(p.y - p.startY, p.x - p.startX);

            if (p.progress < 0.1) {
                angle = Math.atan2(p.targetY - p.startY, p.targetX - p.startX);
            }

            double tailX = p.x - Math.cos(angle) * trailLen;
            double tailY = p.y - Math.sin(angle) * trailLen;

            gc.setStroke(p.color);
            gc.setLineWidth(2.0 * currentScaleFactor);
            gc.strokeLine(tailX, tailY, p.x, p.y);

            gc.setFill(Color.WHITE);
            double headSize = 3.0 * currentScaleFactor;
            gc.fillRect(p.x - headSize / 2, p.y - headSize / 2, headSize, headSize);

            gc.setEffect(new Glow(0.8));

            if (p.hasArrived()) {
                it.remove();
                particlePool.push(p);
            }
        }
        gc.setEffect(null);
    }

    // --- Helper Methods ---

    /**
     * Safely parses the party color. Prevents crash on invalid hex codes.
     */
    private Color safeParseColor(Party p) {
        if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) {
            return SimulationConfig.UNDECIDED_COLOR;
        }

        String rawCode = p.getColorCode();
        if (rawCode == null || rawCode.isBlank()) {
            return ERROR_COLOR;
        }

        try {
            // Usually Color.web() handles "#" and without "#", but we ensure robustness
            return Color.web(rawCode);
        } catch (IllegalArgumentException e) {
            // Fallback: Check if hash is missing
            try {
                return Color.web("#" + rawCode);
            } catch (IllegalArgumentException e2) {
                // Completely invalid -> Return Fallback
                System.err.println("Invalid color code for party " + p.getName() + ": " + rawCode);
                return ERROR_COLOR;
            }
        }
    }

    private void drawTargetLock(double x, double y, double size) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(targetRotationAngle);

        gc.setStroke(Color.RED);
        gc.setLineWidth(2.0);
        double s = size / 2.0;
        double len = s * 0.3;

        gc.strokeLine(-s, -s, -s + len, -s); gc.strokeLine(-s, -s, -s, -s + len);
        gc.strokeLine(s, -s, s - len, -s);   gc.strokeLine(s, -s, s, -s + len);
        gc.strokeLine(-s, s, -s + len, s);   gc.strokeLine(-s, s, -s, s - len);
        gc.strokeLine(s, s, s - len, s);     gc.strokeLine(s, s, s, s - len);

        gc.restore();
    }

    private void drawCrosshair(double x, double y, double size, Color color) {
        gc.setStroke(color.deriveColor(0, 1, 1, 0.5));
        gc.setLineWidth(1.0);

        double len = size * 0.4;

        gc.strokeLine(x - len, y, x + len, y);
        gc.strokeLine(x, y - len, x, y + len);
    }

    private void recalculatePartyPositions(List<Party> parties) {
        partyPositions.clear();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double centerX = w / 2;
        double centerY = h / 2;

        double minDim = Math.min(w, h);
        this.currentScaleFactor = Math.max(0.6, minDim / 800.0);
        double radius = minDim * 0.35;

        for (int i = 0; i < parties.size(); i++) {
            Party p = parties.get(i);

            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) {
                partyPositions.put(p.getName(), new Point(centerX, centerY));
                continue;
            }

            double angle = 2 * Math.PI * i / parties.size() - Math.PI / 2;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            partyPositions.put(p.getName(), new Point(x, y));
        }
    }

    private void spawnParticles(List<VoterTransition> transitions) {
        if (activeParticles.size() >= MAX_ACTIVE_PARTICLES) {
            return;
        }
        int spawnedInThisTick = 0;
        for (VoterTransition t : transitions) {
            if (spawnedInThisTick >= PARTICLE_SPAWN_LIMIT_PER_TICK) break;
            if (activeParticles.size() >= MAX_ACTIVE_PARTICLES) break;

            Point start = partyPositions.get(t.from().getName());
            Point end = partyPositions.get(t.to().getName());

            if (start != null && end != null) {
                MovingVoter p = particlePool.isEmpty() ? new MovingVoter() : particlePool.pop();

                Color color = safeParseColor(t.to());
                p.reset(start.x(), start.y(), end.x(), end.y(), color);

                activeParticles.add(p);
                spawnedInThisTick++;
            }
        }
    }

    // --- Inner Classes ---

    public record Point(double x, double y) {}

    private static class MovingVoter {
        double startX, startY, targetX, targetY, x, y, progress, speedStep;
        Color color;
        boolean arrived;

        void reset(double sx, double sy, double tx, double ty, Color c) {
            double noise = 15.0;
            this.startX = sx + (Math.random() - 0.5) * noise;
            this.startY = sy + (Math.random() - 0.5) * noise;
            this.targetX = tx + (Math.random() - 0.5) * noise;
            this.targetY = ty + (Math.random() - 0.5) * noise;

            this.x = startX;
            this.y = startY;
            this.color = c;
            this.progress = 0.0;
            this.arrived = false;

            this.speedStep = 0.010 + (Math.random() * 0.015);
        }

        void move() {
            if (arrived) return;

            progress += speedStep;
            if (progress >= 1.0) {
                progress = 1.0;
                arrived = true;
            }

            double t = progress * progress * (3 - 2 * progress);

            this.x = startX + (targetX - startX) * t;
            this.y = startY + (targetY - startY) * t;
        }

        boolean hasArrived() {
            return arrived;
        }
    }
}