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
    private Party otherParty;

    @BeforeEach
    void setUp() {
        parties = new ArrayList<>();

        // Party 0: Nichtwähler
        parties.add(new Party(SimulationConfig.UNDECIDED_NAME, "NV", "#808080", 50.0, 0, 0));

        // Party 1: Ziel-Partei
        targetParty = new Party("Target Party", "TP", "#FF0000", 40.0, 1000, 500);
        parties.add(targetParty);

        // Party 2: Unbeteiligte Partei
        otherParty = new Party("Other Party", "OP", "#00FF00", 60.0, 1000, 500);
        parties.add(otherParty);

        // Calculator initialisieren
        calculator = new ScandalImpactCalculator(parties.size() + 5);
    }

    @Test
    @DisplayName("Should calculate acute pressure correctly for active scandals")
    void testCalculateAcutePressure() {
        // Scandal mit ID, Typ, Titel, Beschreibung, Stärke (0.8)
        Scandal scandal = new Scandal(1, "POLITICAL", "Corruption", "Description", 0.8);

        int occurredAt = 100;
        // KORREKTUR: Wir simulieren Tick 150 (50 Ticks nach Start),
        // da bei Tick 100 der TimeFactor noch 0.0 wäre (Fade-In).
        int currentStep = 150;

        ScandalEvent event = new ScandalEvent(scandal, targetParty, occurredAt);

        List<ScandalEvent> activeScandals = new ArrayList<>();
        activeScandals.add(event);

        // Act
        double[] pressures = calculator.calculateAcutePressure(activeScandals, parties, currentStep);

        // Assert
        assertTrue(pressures[1] > 0, "Target party should have acute pressure after 50 ticks");
        assertEquals(0.0, pressures[2], 0.001, "Uninvolved party should have no pressure");
    }

    @Test
    @DisplayName("Should accumulate permanent damage as a side effect")
    void testPermanentDamageAccumulation() {
        Scandal scandal = new Scandal(2, "MINOR", "Minor Scandal", "Desc", 0.5);

        // Auch hier erhöhen wir den Step leicht, um sicherzugehen, dass Schaden berechnet wird,
        // falls die Logik TimeFactor > 0 erfordert (obwohl PermanentDamage oft unabhängig ist, sicher ist sicher).
        ScandalEvent event = new ScandalEvent(scandal, targetParty, 100);
        List<ScandalEvent> activeScandals = List.of(event);

        assertEquals(0.0, calculator.getPermanentDamage(1));

        // Berechnung bei Tick 110 (10 Ticks alt)
        calculator.calculateAcutePressure(activeScandals, parties, 110);

        assertTrue(calculator.getPermanentDamage(1) > 0,
                "Permanent damage should increase after pressure calculation");
    }

    @Test
    @DisplayName("Should reduce permanent damage during recovery process")
    void testProcessRecovery() {
        // Setup: Schaden erzeugen
        Scandal scandal = new Scandal(3, "MAJOR", "Major Scandal", "Desc", 1.0);
        ScandalEvent event = new ScandalEvent(scandal, targetParty, 100);

        // Schaden aufbauen (Tick 150)
        calculator.calculateAcutePressure(List.of(event), parties, 150);

        double damageBefore = calculator.getPermanentDamage(1);
        assertTrue(damageBefore > 0, "Setup failed: No damage to recover from");

        // Act: Recovery (mit 1000 Wählern)
        calculator.processRecovery(parties, 1000);

        // Assert
        double damageAfter = calculator.getPermanentDamage(1);
        assertTrue(damageAfter < damageBefore,
                "Permanent damage should decrease after recovery step");
        assertTrue(damageAfter >= 0, "Damage should not be negative");
    }

    @Test
    @DisplayName("Should reset all damage values")
    void testReset() {
        Scandal scandal = new Scandal(4, "RESET_TEST", "Test", "Desc", 0.5);
        ScandalEvent event = new ScandalEvent(scandal, targetParty, 100);
        calculator.calculateAcutePressure(List.of(event), parties, 120);

        assertTrue(calculator.getPermanentDamage(1) > 0);

        calculator.reset();

        assertEquals(0.0, calculator.getPermanentDamage(1), "Damage should be 0 after reset");
    }

    @Test
    @DisplayName("Should ignore scandals that are too old")
    void testOldScandals() {
        Scandal scandal = new Scandal(5, "OLD", "Old News", "Desc", 1.0);
        // Skandal passierte bei 0, wir sind bei 1000
        ScandalEvent event = new ScandalEvent(scandal, targetParty, 0);

        double[] pressures = calculator.calculateAcutePressure(List.of(event), parties, 1000);

        assertEquals(0.0, pressures[1], 0.001, "Old scandals should generate no pressure");
    }
}