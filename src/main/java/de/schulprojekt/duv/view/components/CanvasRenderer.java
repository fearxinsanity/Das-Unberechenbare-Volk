package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.dto.VoterTransition;
import de.schulprojekt.duv.util.config.SimulationConfig;
import de.schulprojekt.duv.view.managers.AdaptiveParticleManager;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verwaltet die grafische Visualisierung der Simulation auf einem Canvas.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class CanvasRenderer {

    // ========================================
    // Statische Variablen
    // ========================================

    private static final double ROTATION_SPEED = 1.5;
    private static final int PARTICLE_SPAWN_LIMIT_PER_TICK = 50;
    private static final double TARGET_LOCK_SCALE = 1.3;
    private static final int INITIAL_POOL_SIZE = 300;

    private static final Color GRID_COLOR = Color.web("#D4AF37", 0.1);
    private static final Color TEXT_COLOR = Color.web("#e0e0e0");
    private static final Color PERCENTAGE_COLOR = Color.web("#D4AF37");
    private static final Color ERROR_COLOR = Color.MAGENTA;

    // ========================================
    // Instanzvariablen
    // ========================================

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final AnimationTimer visualTimer;
    private final AdaptiveParticleManager adaptiveManager;

    private final Map<String, Point> partyPositions = new HashMap<>();
    private volatile List<Party> currentParties = new ArrayList<>();
    private volatile int currentTotalVoters = 1;
    private volatile double currentScaleFactor = 1.0;
    private final AtomicReference<Double> targetRotationAngle = new AtomicReference<>(0.0);

    private final ArrayDeque<MovingVoter> particlePool = new ArrayDeque<>(INITIAL_POOL_SIZE);
    private final List<MovingVoter> activeParticles = new ArrayList<>();

    // ========================================
    // Konstruktoren
    // ========================================

    /**
     * Initialisiert den Renderer und bindet ihn an das bereitgestellte Container-Pane.
     *
     * @param animationPane Das Pane, in welches das Canvas eingefügt wird.
     */
    public CanvasRenderer(Pane animationPane) {
        this.canvas = new Canvas(0, 0);
        this.canvas.setManaged(true);

        this.adaptiveManager = new AdaptiveParticleManager();

        this.canvas.widthProperty().bind(animationPane.widthProperty());
        this.canvas.heightProperty().bind(animationPane.heightProperty());

        animationPane.getChildren().addFirst(canvas);
        this.gc = canvas.getGraphicsContext2D();

        for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
            particlePool.push(new MovingVoter());
        }

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
                targetRotationAngle.updateAndGet(angle -> angle + ROTATION_SPEED);
                renderCanvas();
            }
        };
    }

    // ========================================
    // Getter-Methoden
    // ========================================

    public Canvas getCanvas() {
        return canvas;
    }

    public Map<String, Point> getPartyPositions() {
        return partyPositions;
    }

    // ========================================
    // Business-Logik-Methoden
    // ========================================

    public void startVisualTimer() {
        adaptiveManager.reset();
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

    /**
     * Aktualisiert den internen Zustand des Renderers mit neuen Simulationsdaten und erzeugt Partikel.
     * @param parties Die aktuelle Liste der Parteien.
     * @param transitions Eine Liste der Wählerwanderungen zwischen Parteien.
     * @param totalVoters Die Gesamtanzahl der Wähler in der Simulation.
     */
    public void update(List<Party> parties, List<VoterTransition> transitions, int totalVoters) {
        this.currentParties = parties;
        this.currentTotalVoters = Math.max(1, totalVoters);

        if (partyPositions.size() != parties.size()) {
            recalculatePartyPositions(parties);
        }

        spawnParticles(transitions);
    }

    // ========================================
    // Hilfsmethoden (Utility)
    // ========================================

    /**
     * Hauptmethode für den Zeichenvorgang. Aktualisiert das Framework und zeichnet Gitter, Knoten sowie Partikel.
     */
    private void renderCanvas() {
        adaptiveManager.updateFrame();

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

    /**
     * Zeichnet gestrichelte Linien zwischen allen Parteiknoten, um ein Netzwerk-Gitter darzustellen.
     */
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

    /**
     * Zeichnet die einzelnen Parteiknoten und deren visuelle Effekte.
     * @param leader Die aktuell stärkste Partei.
     */
    private void drawPartyNodes(Party leader) {
        for (Party p : currentParties) {
            Point pt = partyPositions.get(p.getName());
            if (pt == null) continue;

            Color pColor = safeParseColor(p);
            double share = (double) p.getCurrentSupporterCount() / currentTotalVoters;
            double dynamicSize = (30.0 + (Math.pow(share, 0.7) * 120.0)) * currentScaleFactor;
            double half = dynamicSize / 2.0;

            gc.setFill(pColor.deriveColor(0, 1.0, 1.0, 0.2));
            gc.fillRect(pt.x() - half, pt.y() - half, dynamicSize, dynamicSize);

            gc.setStroke(pColor);
            gc.setLineWidth(1.5 * currentScaleFactor);
            gc.strokeRect(pt.x() - half, pt.y() - half, dynamicSize, dynamicSize);

            if (p == leader) {
                drawTargetLock(pt.x(), pt.y(), dynamicSize * TARGET_LOCK_SCALE);
            }

            drawCrosshair(pt.x(), pt.y(), dynamicSize * 0.8, pColor);
            drawNodeLabels(p, pt, half, share);
        }
    }

    /**
     * Zeichnet die Textbeschriftungen für eine Partei.
     * @param p Die betreffende Partei.
     * @param pt Die Position des Knotens.
     * @param halfSize Die halbe Größe des Knotens für den Versatz des Textes.
     * @param share Der aktuelle Wähleranteil.
     */
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

    /**
     * Versucht den Farbcode einer Partei zu interpretieren. Verwendet eine Fehlerfarbe bei Misserfolg.
     * @param p Die zu prüfende Partei.
     * @return Ein gültiges JavaFX Color-Objekt.
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
            return Color.web(rawCode);
        } catch (IllegalArgumentException e) {
            try {
                return Color.web("#" + rawCode);
            } catch (IllegalArgumentException e2) {
                return ERROR_COLOR;
            }
        }
    }

    /**
     * Zeichnet eine rotierende, rote Zielmarkierung an der angegebenen Position.
     * @param x X-Koordinate des Zentrums.
     * @param y Y-Koordinate des Zentrums.
     * @param size Gesamtdurchmesser der Markierung.
     */
    private void drawTargetLock(double x, double y, double size) {
        gc.save();
        gc.translate(x, y);
        double currentAngle = targetRotationAngle.get();
        gc.rotate(currentAngle);

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

    /**
     * Zeichnet ein dezentes Fadenkreuz innerhalb eines Parteiknotens.
     * @param x X-Koordinate des Zentrums.
     * @param y Y-Koordinate des Zentrums.
     * @param size Größe des Fadenkreuzes.
     * @param color Die Farbe der Linien.
     */
    private void drawCrosshair(double x, double y, double size, Color color) {
        gc.setStroke(color.deriveColor(0, 1, 1, 0.5));
        gc.setLineWidth(1.0);
        double len = size * 0.4;
        gc.strokeLine(x - len, y, x + len, y);
        gc.strokeLine(x, y - len, x, y + len);
    }

    /**
     * Ordnet die Parteiknoten kreisförmig auf dem Canvas an. Berücksichtigt dabei die aktuelle Größe.
     * @param parties Die Liste der Parteien, die positioniert werden sollen.
     */
    private void recalculatePartyPositions(List<Party> parties) {
        partyPositions.clear();
        double centerX = canvas.getWidth() / 2;
        double centerY = canvas.getHeight() / 2;
        double minDim = Math.min(canvas.getWidth(), canvas.getHeight());
        this.currentScaleFactor = Math.max(0.6, minDim / 800.0);
        double radius = minDim * 0.35;

        for (int i = 0; i < parties.size(); i++) {
            Party p = parties.get(i);
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) {
                partyPositions.put(p.getName(), new Point(centerX, centerY));
                continue;
            }
            double angle = 2 * Math.PI * i / parties.size() - Math.PI / 2;
            partyPositions.put(p.getName(), new Point(centerX + radius * Math.cos(angle), centerY + radius * Math.sin(angle)));
        }
    }

    /**
     * Erzeugt neue Partikelobjekte basierend auf den übergebenen Wählerwanderungen.
     * @param transitions Die Liste der Wanderungsereignisse.
     */
    private void spawnParticles(List<VoterTransition> transitions) {
        int maxParticles = adaptiveManager.getMaxParticles();

        if (activeParticles.size() >= maxParticles) return;

        int spawned = 0;
        for (VoterTransition t : transitions) {
            if (spawned >= PARTICLE_SPAWN_LIMIT_PER_TICK || activeParticles.size() >= maxParticles) break;

            Point start = partyPositions.get(t.from().getName());
            Point end = partyPositions.get(t.to().getName());

            if (start != null && end != null) {
                MovingVoter p = particlePool.isEmpty() ? new MovingVoter() : particlePool.pop();
                p.reset(start.x(), start.y(), end.x(), end.y(), safeParseColor(t.to()));
                activeParticles.add(p);
                spawned++;
            }
        }
    }

    // ========================================
    // Innere Klassen / Records
    // ========================================

    /**
     * Repräsentiert einen Punkt im zweidimensionalen Raum.
     *
     * @param x Die x-Koordinate.
     * @param y Die y-Koordinate.
     */
    public record Point(double x, double y) {}

    /**
     * Repräsentiert einen sich bewegenden Partikel auf dem Canvas.
     */
    private static class MovingVoter {
        double startX, startY, targetX, targetY, x, y, progress, speedStep;
        Color color;
        boolean arrived;

        /**
         * Setzt den Partikel für einen neuen Bewegungspfad mit zufälligem Versatz (Noise) zurück.
         * @param sx Start-X-Koordinate.
         * @param sy Start-Y-Koordinate.
         * @param tx Ziel-X-Koordinate.
         * @param ty Ziel-Y-Koordinate.
         * @param c Farbe des Partikels.
         */
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

        /**
         * Berechnet die neue Position des Partikels basierend auf dem Fortschritt und einer Easing-Funktion.
         */
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

        /**
         * Gibt an, ob der Partikel seine Zielkoordinaten erreicht hat.
         * @return true, wenn das Ziel erreicht wurde.
         */
        boolean hasArrived() {
            return arrived;
        }
    }
}