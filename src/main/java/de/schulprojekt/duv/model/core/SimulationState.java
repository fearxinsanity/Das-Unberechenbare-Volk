package de.schulprojekt.duv.model.core;

import de.schulprojekt.duv.model.scandal.ScandalEvent;
import java.util.ArrayList;
import java.util.List;

public class SimulationState {
    private int currentStep = 0;
    private final List<ScandalEvent> activeScandals = new ArrayList<>();
    private ScandalEvent lastScandal = null;

    public void incrementStep() { currentStep++; }
    public int getCurrentStep() { return currentStep; }

    public void addScandal(ScandalEvent event) {
        activeScandals.add(event);
        lastScandal = event;
    }

    public List<ScandalEvent> getActiveScandals() { return activeScandals; }

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