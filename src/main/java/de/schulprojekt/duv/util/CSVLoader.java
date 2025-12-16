package de.schulprojekt.duv.util;

import de.schulprojekt.duv.model.entities.PartyTemplate;
import de.schulprojekt.duv.model.entities.Scandal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Utility-Klasse zum Laden von Daten aus CSV-Dateien.
 * Lädt Parteinamen und Skandale für die Simulation.
 */
public class CSVLoader {

    private static final String PARTY_NAMES_PATH = "/de.schulprojekt.duv/data/party_names.csv";
    private static final String SCANDALS_PATH = "/de.schulprojekt.duv/data/scandals.csv";

    private final List<PartyTemplate> partyTemplates;
    private final List<Scandal> scandals;
    private final Random random;

    public CSVLoader() {
        this.random = new Random();
        this.partyTemplates = loadPartyTemplates();
        this.scandals = loadScandals();
    }

    /**
     * Lädt die Partei-Templates aus der CSV-Datei.
     */
    private List<PartyTemplate> loadPartyTemplates() {
        List<PartyTemplate> templates = new ArrayList<>();

        // HINWEIS: Es wird ein führender Schrägstrich (/) hinzugefügt, um sicherzustellen,
        // dass die Suche im Wurzelverzeichnis des Classpaths beginnt, was in der
        // Deployment-Umgebung die beste Praxis ist.
        try (InputStream is = getClass().getResourceAsStream(PARTY_NAMES_PATH)) {
            if (is == null) {
                System.err.println("WARNUNG: party_names.csv nicht gefunden unter " + PARTY_NAMES_PATH + ", verwende Fallback");
                return createFallbackPartyTemplates();
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String line;
                boolean firstLine = true;

                while ((line = reader.readLine()) != null) {
                    // Header überspringen
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }

                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        String name = parts[0].trim();
                        String abbreviation = parts[1].trim();
                        String color = parts[2].trim();
                        templates.add(new PartyTemplate(name, abbreviation, color));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Laden der Parteinamen: " + e.getMessage());
            return createFallbackPartyTemplates();
        }

        return templates;
    }

    /**
     * Lädt die Skandale aus der CSV-Datei.
     */
    private List<Scandal> loadScandals() {
        List<Scandal> scandalList = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(SCANDALS_PATH)) {
            if (is == null) {
                System.err.println("WARNUNG: scandals.csv nicht gefunden unter " + SCANDALS_PATH + ", verwende Fallback");
                return createFallbackScandals();
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String line;
                boolean firstLine = true;

                while ((line = reader.readLine()) != null) {
                    // Header überspringen
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }

                    String[] parts = line.split(",");
                    if (parts.length >= 5) {
                        try {
                            int id = Integer.parseInt(parts[0].trim());
                            String type = parts[1].trim();
                            String title = parts[2].trim();
                            String description = parts[3].trim();
                            double strength = Double.parseDouble(parts[4].trim());

                            scandalList.add(new Scandal(id, type, title, description, strength));
                        } catch (NumberFormatException e) {
                            System.err.println("Ungültige Zeile in scandals.csv: " + line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Laden der Skandale: " + e.getMessage());
            return createFallbackScandals();
        }

        return scandalList;
    }

    /**
     * Gibt zufällig ausgewählte, eindeutige Partei-Templates zurück.
     */
    public List<PartyTemplate> getRandomPartyTemplates(int count) {
        if (count > partyTemplates.size()) {
            count = partyTemplates.size();
        }

        List<PartyTemplate> shuffled = new ArrayList<>(partyTemplates);
        Collections.shuffle(shuffled, random);

        return new ArrayList<>(shuffled.subList(0, count));
    }

    /**
     * Gibt einen zufälligen Skandal zurück.
     */
    public Scandal getRandomScandal() {
        if (scandals.isEmpty()) {
            return new Scandal(0, "GENERIC", "Skandal", "Ein Skandal ist aufgetreten", 0.5);
        }
        return scandals.get(random.nextInt(scandals.size()));
    }

    /**
     * Gibt alle geladenen Skandale zurück.
     */
    public List<Scandal> getAllScandals() {
        return Collections.unmodifiableList(scandals);
    }

    /**
     * Gibt alle geladenen Partei-Templates zurück.
     */
    public List<PartyTemplate> getAllPartyTemplates() {
        return Collections.unmodifiableList(partyTemplates);
    }

    // --- Fallback-Methoden falls CSV nicht geladen werden kann ---

    private List<PartyTemplate> createFallbackPartyTemplates() {
        List<PartyTemplate> fallback = new ArrayList<>();
        fallback.add(new PartyTemplate("Partei A", "A", "007bff"));
        fallback.add(new PartyTemplate("Partei B", "B", "dc3545"));
        fallback.add(new PartyTemplate("Partei C", "C", "28a745"));
        fallback.add(new PartyTemplate("Partei D", "D", "ffc107"));
        fallback.add(new PartyTemplate("Partei E", "E", "6f42c1"));
        fallback.add(new PartyTemplate("Partei F", "F", "fd7e14"));
        fallback.add(new PartyTemplate("Partei G", "G", "20c997"));
        fallback.add(new PartyTemplate("Partei H", "H", "6c757d"));
        return fallback;
    }

    private List<Scandal> createFallbackScandals() {
        List<Scandal> fallback = new ArrayList<>();
        fallback.add(new Scandal(1, "CORRUPTION", "Korruptionsvorwürfe", "Bestechungsvorwürfe", 0.7));
        fallback.add(new Scandal(2, "FINANCIAL", "Finanzskandal", "Unregelmäßigkeiten entdeckt", 0.6));
        fallback.add(new Scandal(3, "PERSONAL", "Persönlicher Skandal", "Fehlverhalten aufgedeckt", 0.5));
        return fallback;
    }
}