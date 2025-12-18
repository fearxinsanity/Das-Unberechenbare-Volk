package de.schulprojekt.duv.util;

import de.schulprojekt.duv.model.party.PartyTemplate;
import de.schulprojekt.duv.model.scandal.Scandal;

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
 * Utility-Klasse zum Laden von Daten aus CSV-Dateien (Resources).
 * Liest Parteinamen und Skandale ein.
 */
public class CSVLoader {

    private static final String PARTY_FILE = "/de/schulprojekt/duv/data/party_names.csv";
    private static final String SCANDAL_FILE = "/de/schulprojekt/duv/data/scandals.csv";

    private final Random random = new Random();

    /**
     * Lädt eine zufällige Auswahl an Parteivorlagen aus der CSV-Datei.
     *
     * @param count Die gewünschte Anzahl an Parteien.
     * @return Eine Liste von PartyTemplates.
     */
    public List<PartyTemplate> getRandomPartyTemplates(int count) {
        List<PartyTemplate> allTemplates = loadParties();

        // Falls wir weniger Vorlagen haben als gewünscht, nehmen wir alle
        if (allTemplates.size() <= count) {
            return allTemplates;
        }

        // Zufällig mischen und die ersten 'count' zurückgeben
        Collections.shuffle(allTemplates);
        return allTemplates.subList(0, count);
    }

    /**
     * Liefert einen zufälligen Skandal aus der Datenbank.
     *
     * @return Ein Scandal-Objekt.
     */
    public Scandal getRandomScandal() {
        List<Scandal> scandals = loadScandals();
        if (scandals.isEmpty()) {
            // Fallback, falls Datei leer oder nicht gefunden
            return new Scandal("Unbekannter Fehler", 0.1, "Ein unerwartetes Ereignis ist eingetreten.");
        }
        return scandals.get(random.nextInt(scandals.size()));
    }

    private List<PartyTemplate> loadParties() {
        List<PartyTemplate> list = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(PARTY_FILE)) {
            if (is == null) {
                System.err.println("FEHLER: Konnte Partei-Datei nicht finden: " + PARTY_FILE);
                return list;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;

                while ((line = br.readLine()) != null) {
                    // Header überspringen
                    if (firstLine) { firstLine = false; continue; }
                    if (line.trim().isEmpty()) continue;

                    // Trennzeichen erkennen (Semikolon oder Komma)
                    String[] parts = line.contains(";") ? line.split(";") : line.split(",");

                    if (parts.length >= 3) {
                        String name = parts[0].trim();
                        String abbr = parts[1].trim();
                        String color = parts[2].trim();

                        list.add(new PartyTemplate(name, abbr, color));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<Scandal> loadScandals() {
        List<Scandal> list = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(SCANDAL_FILE)) {
            if (is == null) {
                System.err.println("FEHLER: Konnte Skandal-Datei nicht finden: " + SCANDAL_FILE);
                return list;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;

                while ((line = br.readLine()) != null) {
                    if (firstLine) { firstLine = false; continue; }
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.contains(";") ? line.split(";") : line.split(",");

                    if (parts.length >= 3) {
                        try {
                            String name = parts[0].trim();
                            // Stärke parsen (Punkt oder Komma als Dezimaltrenner erlauben)
                            String strengthStr = parts[1].trim().replace(",", ".");
                            double strength = Double.parseDouble(strengthStr);
                            String desc = parts[2].trim();

                            list.add(new Scandal(name, strength, desc));
                        } catch (NumberFormatException e) {
                            System.err.println("Warnung: Ungültige Zahl in Skandal-CSV: " + line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }
}