package de.schulprojekt.duv.model.core;

import de.schulprojekt.duv.model.scandal.ScandalEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Verwaltet den Laufzeitzustand der Simulation.
 * Die Klasse agiert als zentrales Gedächtnis für Daten, die während eines
 * Simulationslaufs entstehen und sich in jedem Tick ändern können.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public class SimulationState {

    // ========================================
    // Instance Variables
    // ========================================

    private int currentStep = 0;
    private final List<ScandalEvent> activeScandals = new ArrayList<>();

    /** Referenz auf den aktuellsten Skandal.
     * Ermöglicht UI direkten Trigger für Benachrichtigungen.
     */
    private ScandalEvent lastScandal = null;

    // ========================================
    // Constructors
    // ========================================

    public SimulationState() {
        // Default constructor
    }

    // ========================================
    // Getter Methods
    // ========================================

    public int getCurrentStep() {
        return currentStep;
    }

    public List<ScandalEvent> getActiveScandals() {
        return activeScandals;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    public void incrementStep() {
        currentStep++;
    }


    /**
     * Ruft den zuletzt aufgetretenen Skandal ab und löscht die interne Referenz darauf.
     * Es stellt sicher, dass ein Ereignis nur genau einmal als
     * "neu" verarbeitet wird,
     * obwohl es in der Liste der aktiven Skandale erhalten bleibt.
     */
    public void addScandal(ScandalEvent event) {
        activeScandals.add(event);
        lastScandal = event;
    }

    public ScandalEvent consumeLastScandal() {
        ScandalEvent s = lastScandal;
        lastScandal = null;
        return s;
    }

    public void reset() {
        currentStep = 0;
        activeScandals.clear();
        lastScandal = null;
    }
}
