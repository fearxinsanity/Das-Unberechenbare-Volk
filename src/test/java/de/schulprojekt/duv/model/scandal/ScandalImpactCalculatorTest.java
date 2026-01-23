package de.schulprojekt.duv.model.scandal;

import de.schulprojekt.duv.model.party.Party;
import de.schulprojekt.duv.util.config.SimulationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScandalImpactCalculatorTest {

    private ScandalImpactCalculator calculator;
    private List<Party> parties;
    private Party targetParty;

    @BeforeEach
    void setUp() {
        parties = new ArrayList<>();
        parties.add(new Party(SimulationConfig.UNDECIDED_NAME, "NV", "#808080", 50.0, 0, 0));

        targetParty = new Party("Target Party", "TP", "#FF0000", 40.0, 1000, 500);
        parties.add(targetParty);
        parties.add(new Party("Other Party", "OP", "#00FF00", 60.0, 1000, 500));

        calculator = new ScandalImpactCalculator(parties.size() + 5);
    }

    @Test
    @DisplayName("Sollte akuten Druck bei aktiven Skandalen berechnen")
    void testCalculateAcutePressure() {
        Scandal scandal = new Scandal(1, "POLITICAL", "Corruption", "Desc", 0.8);
        ScandalEvent event = new ScandalEvent(scandal, targetParty, 100);

        // Tick 150 (50 Ticks nach Auftreten)
        double[] pressures = calculator.calculateAcutePressure(List.of(event), parties, 150);

        assertTrue(pressures[1] > 0, "Zielpartei sollte Druck verspüren");
        assertEquals(0.0, pressures[0], 0.001, "Nichtwähler sollten keinen Druck haben");
    }

    @Test
    @DisplayName("Sollte den permanenten Schaden über die Zeit reduzieren (Recovery)")
    void testProcessRecovery() {
        Scandal scandal = new Scandal(3, "MAJOR", "Major Scandal", "Desc", 1.0);
        ScandalEvent event = new ScandalEvent(scandal, targetParty, 100);

        calculator.calculateAcutePressure(List.of(event), parties, 150);
        double damageBefore = calculator.getPermanentDamage(1);

        calculator.processRecovery(parties, 1000);

        assertTrue(calculator.getPermanentDamage(1) < damageBefore, "Schaden sollte sinken");
    }
}