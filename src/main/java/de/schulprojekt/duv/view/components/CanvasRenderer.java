package de.schulprojekt.duv.view.components;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.model.voter.VoterTransition;
import de.schulprojekt.duv.util.SimulationConfig;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.*;

public class CanvasRenderer {
    private final Canvas canvas;
    private final GraphicsContext gc;

    // Positionen der Parteien
    private final Map<String, Point> partyPositions = new HashMap<>();

    // Partikel-System
    private final Stack<MovingVoter> particlePool = new Stack<>();
    private final List<MovingVoter> activeParticles = new ArrayList<>();
    private final Random random = new Random();

    public CanvasRenderer(Canvas canvas) {
        this.canvas = canvas;
        this.gc = canvas.getGraphicsContext2D();
    }

    /**
     * Berechnet die Positionen der Parteien im Kreis neu.
     */
    public void recalculatePositions(List<Party> parties) {
        partyPositions.clear();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(centerX, centerY) * 0.75; // 75% des verfügbaren Platzes

        int partyCount = parties.size();
        for (int i = 0; i < partyCount; i++) {
            Party p = parties.get(i);
            if (p.getName().equals(SimulationConfig.UNDECIDED_NAME)) {
                // Nichtwähler in die Mitte
                partyPositions.put(p.getName(), new Point(centerX, centerY));
            } else {
                // Andere im Kreis anordnen
                double angle = 2 * Math.PI * i / partyCount - Math.PI / 2;
                double x = centerX + radius * Math.cos(angle);
                double y = centerY + radius * Math.sin(angle);
                partyPositions.put(p.getName(), new Point(x, y));
            }
        }
    }

    /**
     * Haupt-Render-Methode.
     */
    public void render(List<Party> parties, int totalVoters) {
        double width = canvas.getWidth();
        double height = canvas.getHeight();

        // 1. Hintergrund löschen
        gc.clearRect(0, 0, width, height);

        // Falls noch keine Positionen berechnet wurden
        if (partyPositions.isEmpty() && !parties.isEmpty()) {
            recalculatePositions(parties);
        }

        // 2. Verbindungslinien zeichnen (rein optisch, z.B. zur Mitte)
        Point center = partyPositions.get(SimulationConfig.UNDECIDED_NAME);
        if (center != null) {
            gc.setStroke(Color.LIGHTGRAY);
            gc.setLineWidth(1.0);
            for (Point p : partyPositions.values()) {
                if (p != center) {
                    gc.strokeLine(center.x, center.y, p.x, p.y);
                }
            }
        }

        // 3. Parteien zeichnen
        for (Party p : parties) {
            drawPartyCircle(p, totalVoters);
        }

        // 4. Partikel animieren und zeichnen
        updateAndDrawParticles();
    }

    private void drawPartyCircle(Party p, int totalVoters) {
        Point pos = partyPositions.get(p.getName());
        if (pos == null) return;

        // Größe basierend auf Stimmenanteil
        double share = (double) p.getCurrentSupporterCount() / totalVoters;
        double radius = 20 + (share * 100); // Mindestgröße 20

        // Farbe
        Color color = p.getName().equals(SimulationConfig.UNDECIDED_NAME) ? Color.GRAY : Color.web(p.getColorCode());

        // Kreis füllen
        gc.setFill(color.deriveColor(0, 1, 1, 0.7));
        gc.fillOval(pos.x - radius, pos.y - radius, radius * 2, radius * 2);

        // Rand
        gc.setStroke(color.darker());
        gc.setLineWidth(2);
        gc.strokeOval(pos.x - radius, pos.y - radius, radius * 2, radius * 2);

        // Text (Name + %)
        gc.setFill(Color.BLACK);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(new Font("System", 12));
        gc.fillText(p.getName(), pos.x, pos.y - radius - 5);
        gc.fillText(String.format("%.1f%%", share * 100), pos.x, pos.y + 5);
    }

    /**
     * Erstellt neue Partikel basierend auf den Wählerwanderungen.
     */
    public void spawnParticles(List<VoterTransition> transitions) {
        // 1. Zählen: Wie viele Wähler wechseln von A nach B?
        // Key: "VonPartei->NachPartei", Value: Anzahl
        Map<String, Integer> transitionCounts = new HashMap<>();

        // Wir brauchen auch die Start/End-Namen, um die Positionen zu finden
        // (Da wir im Key nur Strings speichern, müssen wir die Zuordnung behalten)
        Map<String, String> routeToFromParty = new HashMap<>();
        Map<String, String> routeToToParty = new HashMap<>();

        for (VoterTransition t : transitions) {
            String fromName = t.getOldParty().getName(); // FIX: getOldParty() statt fromParty()
            String toName = t.getNewParty().getName();   // FIX: getNewParty() statt toParty()

            String key = fromName + "->" + toName;

            transitionCounts.put(key, transitionCounts.getOrDefault(key, 0) + 1);

            if (!routeToFromParty.containsKey(key)) {
                routeToFromParty.put(key, fromName);
                routeToToParty.put(key, toName);
            }
        }

        // 2. Partikel für die Gruppen erzeugen
        for (String key : transitionCounts.keySet()) {
            int count = transitionCounts.get(key);
            String fromName = routeToFromParty.get(key);
            String toName = routeToToParty.get(key);

            Point start = partyPositions.get(fromName);
            Point end = partyPositions.get(toName);

            if (start != null && end != null) {
                int particleCount = Math.max(1, count / 5);
                // Performance-Bremse: Nicht mehr als 20 Partikel pro Route gleichzeitig
                particleCount = Math.min(particleCount, 20);

                for (int i = 0; i < particleCount; i++) {
                    MovingVoter p = particlePool.isEmpty() ? new MovingVoter() : particlePool.pop();
                    p.init(start.x, start.y, end.x, end.y);
                    activeParticles.add(p);
                }
            }
        }
    }

    private void updateAndDrawParticles() {
        gc.setFill(Color.DARKGRAY);
        Iterator<MovingVoter> it = activeParticles.iterator();
        while (it.hasNext()) {
            MovingVoter p = it.next();
            if (p.update()) {
                // Partikel ist am Ziel angekommen
                it.remove();
                particlePool.push(p);
            } else {
                // Zeichnen
                gc.fillOval(p.x - 2, p.y - 2, 4, 4);
            }
        }
    }

    public void clearParticles() {
        activeParticles.clear();
    }

    public Point getPartyPosition(String name) {
        return partyPositions.get(name);
    }

    // Hilfsklasse für Koordinaten
    public static class Point {
        public final double x, y;
        public Point(double x, double y) { this.x = x; this.y = y; }
    }

    // Innere Klasse für die Partikel
    private class MovingVoter {
        double x, y, targetX, targetY;
        double speed;

        void init(double startX, double startY, double endX, double endY) {
            this.x = startX;
            this.y = startY;
            this.targetX = endX;
            this.targetY = endY;
            // Zufällige Geschwindigkeit und leichter Streuung
            this.speed = 2.0 + random.nextDouble() * 3.0;
        }

        // Gibt true zurück, wenn das Ziel erreicht ist
        boolean update() {
            double dx = targetX - x;
            double dy = targetY - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < speed) {
                return true;
            }

            x += (dx / dist) * speed;
            y += (dy / dist) * speed;
            return false;
        }
    }
}