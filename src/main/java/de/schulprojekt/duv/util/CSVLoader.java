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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for loading game data from CSV files.
 * * @author Nico Hoffmann
 * @version 1.1
 */
public class CSVLoader {

    // ========================================
    // Static Variables
    // ========================================

    private static final Logger LOGGER = Logger.getLogger(CSVLoader.class.getName());
    private static final String PARTY_FILE = "de/schulprojekt/duv/data/party_names.csv";
    private static final String SCANDAL_FILE = "de/schulprojekt/duv/data/scandals.csv";
    private static final String CSV_SEPARATOR_REGEX = "[,;]";

    // ========================================
    // Instance Variables
    // ========================================

    private List<Scandal> cachedScandals = null;

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Retrieves a set of random party templates.
     * * @param count number of templates to return
     * @return list of party templates
     */
    public List<PartyTemplate> getRandomPartyTemplates(int count) {
        List<PartyTemplate> allTemplates = loadAllParties();

        if (allTemplates.isEmpty()) {
            LOGGER.warning("No party templates found!");
            return new ArrayList<>();
        }

        if (allTemplates.size() <= count) {
            return allTemplates;
        }

        List<PartyTemplate> selection = new ArrayList<>(allTemplates);
        Collections.shuffle(selection);
        return selection.subList(0, count);
    }

    /**
     * Retrieves a random scandal, utilizing a cache.
     * * @return a random scandal object
     */
    public Scandal getRandomScandal() {
        if (cachedScandals == null) {
            cachedScandals = loadAllScandals();
        }

        if (cachedScandals.isEmpty()) {
            LOGGER.warning("No scandals loaded. Creating fallback.");
            return new Scandal(0, "SCANDAL", "Unknown", "No data loaded.", 0.5);
        }

        return cachedScandals.get(ThreadLocalRandom.current().nextInt(cachedScandals.size()));
    }

    // ========================================
    // Utility Methods
    // ========================================

    private List<PartyTemplate> loadAllParties() {
        return loadCsvFile(PARTY_FILE, line -> {
            String[] parts = line.split(CSV_SEPARATOR_REGEX);
            return (parts.length >= 3) ? new PartyTemplate(parts[0].trim(), parts[1].trim(), parts[2].trim()) : null;
        });
    }

    private List<Scandal> loadAllScandals() {
        return loadCsvFile(SCANDAL_FILE, line -> {
            if (line.trim().toLowerCase().startsWith("id")) return null;
            String[] parts = line.split(",", -1);
            if (parts.length >= 5) {
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String type = cleanCsvString(parts[1]);
                    String name = cleanCsvString(parts[2]);
                    String desc = cleanCsvString(parts[3]);
                    double str = Double.parseDouble(cleanCsvString(parts[4]).replace(",", "."));
                    return new Scandal(id, type, name, desc, str);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });
    }

    private <T> List<T> loadCsvFile(String filePath, Function<String, T> mapper) {
        List<T> resultList = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("/" + filePath);
        if (is == null) is = getClass().getResourceAsStream(filePath);
        if (is == null) is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);

        if (is == null) {
            LOGGER.log(Level.SEVERE, "CSV file missing: {0}", filePath);
            return resultList;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (isFirstLine && isHeader(line.toLowerCase())) {
                    isFirstLine = false;
                    continue;
                }
                isFirstLine = false;
                T item = mapper.apply(line);
                if (item != null) resultList.add(item);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading CSV", e);
        }
        return resultList;
    }

    private boolean isHeader(String line) {
        return line.startsWith("id") || line.startsWith("kuerzel") || line.startsWith("name") ||
                line.startsWith("partei") || line.contains("color") || line.contains("farbe");
    }

    private String cleanCsvString(String input) {
        return (input == null) ? "" : input.trim().replace("\"", "");
    }
}