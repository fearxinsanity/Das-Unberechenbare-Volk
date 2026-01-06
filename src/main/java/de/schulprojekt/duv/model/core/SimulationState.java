package de.schulprojekt.duv.model.core;

import de.schulprojekt.duv.model.scandal.ScandalEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the mutable runtime state of the simulation.
 * Tracks the current time step (tick) and the list of currently active scandals.
 * This class is purely a data holder for the engine.
 */
public class SimulationState {

    // --- FIELDS ---
    private int currentStep = 0;
    private final List<ScandalEvent> activeScandals = new ArrayList<>();
    private ScandalEvent lastScandal = null;

    // --- CONSTRUCTOR ---
    public SimulationState() {
        // Default constructor
    }

    // --- MAIN LOGIC ---

    public void incrementStep() {
        currentStep++;
    }

    public void addScandal(ScandalEvent event) {
        activeScandals.add(event);
        lastScandal = event;
    }

    /**
     * Retrieves the most recent scandal and clears the reference.
     * Used by the UI/Controller to show "News Flash" notifications only once.
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

    // --- GETTERS ---

    public int getCurrentStep() {
        return currentStep;
    }

    public List<ScandalEvent> getActiveScandals() {
        return activeScandals;
    }
}