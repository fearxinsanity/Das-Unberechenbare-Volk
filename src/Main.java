import model.SimulationEngine;
import model.Party;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        int numberOfParties = 3;
        int numberOfVoters = 10000;
        int numberOfSteps = 50;

        SimulationEngine engine = new SimulationEngine(numberOfParties, numberOfVoters);

        System.out.println("Starting the election simulation...");
        engine.runSimulation(numberOfSteps);
        System.out.println("Simulation finished.");

        // Print the final results
        System.out.println("\n--- Final Results ---");
        List<Party> finalParties = engine.getParties();
        for (Party party : finalParties) {
            System.out.println(party);
        }
    }
}