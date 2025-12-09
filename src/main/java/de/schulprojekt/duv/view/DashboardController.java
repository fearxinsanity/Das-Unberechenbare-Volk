package de.schulprojekt.duv.view;

import de.schulprojekt.duv.controller.SimulationController;
import de.schulprojekt.duv.model.entities.Party;
import de.schulprojekt.duv.model.entities.Voter;
import javafx.fxml.FXML;
import java.util.List;

/**
 * FXML Controller for the DashboardUI.
 * This class adheres to the MVC pattern by acting as a "dumb" View.
 * It holds references to all FXML UI components and provides public methods
 * for the SimulationController to update them.
 * It contains NO simulation logic.
 */
public class DashboardController {

    private SimulationController simulationController;

    // TODO: Hier kommen später deine @FXML-Elemente (Diagramme, Slider, etc.) hin, z.B.:
    // @FXML private Label timestepLabel;
    // @FXML private Slider voterCountSlider;


    /**
     * Sets the SimulationController reference. This is called by the Main class
     * immediately after the FXML is loaded to connect View and Controller.
     * @param controller The main application controller.
     */
    public void setSimulationController(SimulationController controller) {
        this.simulationController = controller;
    }

    /**
     * Automatically called by the FXMLLoader after the FXML elements are loaded.
     */
    @FXML
    public void initialize() {
        // Hier werden später die Startwerte der UI-Elemente gesetzt oder Events initialisiert.
    }

    // --- EVENT HANDLER (View -> Controller) ---

    /**
     * Handles the 'Start Simulation' button press by delegating the action to the SimulationController.
     * Must be public for FXML to access it.
     */
    @FXML
    public void handleStartSimulation() {
        if (simulationController != null) {
            simulationController.startSimulation();
            // TODO: UI-Anpassungen hier (z.B. Start-Button deaktivieren, Pause-Button aktivieren)
        }
    }

    /**
     * Handles the 'Pause Simulation' button press.
     * Must be public for FXML to access it.
     */
    @FXML
    public void handlePauseSimulation() {
        if (simulationController != null) {
            simulationController.pauseSimulation();
            // TODO: UI-Anpassungen hier
        }
    }

    /**
     * Handles the 'Reset Simulation' button press.
     * Must be public for FXML to access it.
     */
    @FXML
    public void handleResetSimulation() {
        if (simulationController != null) {
            // TODO: simulationController.resetSimulation() aktivieren, sobald implementiert
        }
    }

    // --- DATA DISPLAY (Controller -> View) ---

    /**
     * Called by the SimulationController every tick to update the entire UI.
     * This method is the final step of the MVC data flow (View-Aktualisierung).
     * @param parties The list of current Party states.
     * @param voters The list of all Voter states (used for animation).
     */
    public void updateDashboard(List<Party> parties, List<Voter> voters) {
        // TODO: Implementiere hier die Logik, um:
        // 1. Die Unterstützerzahlen in Diagrammen zu aktualisieren (parties.getCurrentSupporterCount()).
        // 2. Die Animation basierend auf Voter-Positionen zu aktualisieren.
    }
}