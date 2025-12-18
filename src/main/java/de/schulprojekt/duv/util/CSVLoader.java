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

public class CSVLoader {

    private static final String PARTY_FILE = "de/schulprojekt/duv/data/party_names.csv";
    private static final String SCANDAL_FILE = "de/schulprojekt/duv/data/scandals.csv";

    private final Random random = new Random();

    public List<PartyTemplate> getRandomPartyTemplates(int count) {
        List<PartyTemplate> allTemplates = loadParties();
        if (allTemplates.isEmpty()) {
            System.err.println("WARNUNG: Keine Parteien in CSV gefunden!");
            return new ArrayList<>();
        }
        if (allTemplates.size() <= count) return allTemplates;
        Collections.shuffle(allTemplates);
        return allTemplates.subList(0, count);
    }

    public Scandal getRandomScandal() {
        List<Scandal> scandals = loadScandals();
        if (scandals.isEmpty()) {
            return new Scandal(0, "SCANDAL", "Unbekannt", "Keine Daten geladen.", 0.5);
        }
        return scandals.get(random.nextInt(scandals.size()));
    }

    private List<PartyTemplate> loadParties() {
        List<PartyTemplate> list = new ArrayList<>();
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(PARTY_FILE)) {
            if (is == null) {
                System.err.println("FEHLER: Datei nicht gefunden: " + PARTY_FILE);
                return list;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean firstLine = true;
                while ((line = br.readLine()) != null) {
                    if (firstLine) { firstLine = false; continue; }
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split(line.contains(";") ? ";" : ",");
                    if (parts.length >= 3) {
                        list.add(new PartyTemplate(parts[0].trim(), parts[1].trim(), parts[2].trim()));
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return list;
    }

    private List<Scandal> loadScandals() {
        List<Scandal> list = new ArrayList<>();
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(SCANDAL_FILE)) {
            if (is == null) return list;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty() || line.toLowerCase().startsWith("id")) continue;
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 5) {
                        try {
                            int id = Integer.parseInt(parts[0].trim());
                            String type = parts[1].trim().replace("\"", "");
                            String name = parts[2].trim().replace("\"", "");
                            String desc = parts[3].trim().replace("\"", "");
                            double strength = Double.parseDouble(parts[4].trim().replace(",", ".").replace("\"", ""));
                            list.add(new Scandal(id, type, name, desc, strength));
                        } catch (Exception e) { /* Skip malformed lines */ }
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return list;
    }
}