package de.schulprojekt.duv.model.core;

import de.schulprojekt.duv.model.scandal.ScandalEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the mutable runtime state of the simulation.
 * * @author Nico Hoffmann
 * @version 1.0
 */
public class SimulationState {

    // ========================================
    // Instance Variables
    // ========================================

    private int currentStep = 0;
    private final List<ScandalEvent> activeScandals = new ArrayList<>();
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

    public void addScandal(ScandalEvent event) {
        activeScandals.add(event);
        lastScandal = event;
    }

    /**
     * Retrieves the most recent scandal and clears the reference.
     * * @return the last occurred scandal event
     */
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