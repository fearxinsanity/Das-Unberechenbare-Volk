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
 * Loads game data from CSV files located in the classpath.
 * Optimized with caching to minimize I/O operations and includes robust logging.
 */
public class CSVLoader {

    // --- Constants ---
    private static final Logger LOGGER = Logger.getLogger(CSVLoader.class.getName());
    private static final String PARTY_FILE = "de/schulprojekt/duv/data/party_names.csv";
    private static final String SCANDAL_FILE = "de/schulprojekt/duv/data/scandals.csv";
    private static final String CSV_SEPARATOR_REGEX = "[,;]";

    // --- Cache Fields ---
    private List<Scandal> cachedScandals = null;

    // --- Public API ---

    /**
     * Loads party templates, shuffles them, and returns the requested amount.
     *
     * @param count The number of party templates to retrieve.
     * @return A list of random party templates.
     */
    public List<PartyTemplate> getRandomPartyTemplates(int count) {
        List<PartyTemplate> allTemplates = loadAllParties();

        if (allTemplates.isEmpty()) {
            LOGGER.warning("No party templates found! Returning empty list.");
            return new ArrayList<>();
        }

        if (allTemplates.size() <= count) {
            return allTemplates;
        }

        // Create a copy to keep the original list intact (if we decide to cache it later)
        List<PartyTemplate> selection = new ArrayList<>(allTemplates);
        Collections.shuffle(selection);
        return selection.subList(0, count);
    }

    /**
     * Returns a random scandal from the list.
     * Uses caching to ensure the file is only read once.
     *
     * @return A random Scandal object.
     */
    public Scandal getRandomScandal() {
        if (cachedScandals == null) {
            cachedScandals = loadAllScandals();
        }

        if (cachedScandals.isEmpty()) {
            LOGGER.warning("No scandals loaded. Creating fallback scandal.");
            return new Scandal(0, "SCANDAL", "Unknown", "No data loaded (Fallback).", 0.5);
        }

        return cachedScandals.get(ThreadLocalRandom.current().nextInt(cachedScandals.size()));
    }

    // --- Private Loader Implementations ---

    private List<PartyTemplate> loadAllParties() {
        return loadCsvFile(PARTY_FILE, line -> {
            String[] parts = line.split(CSV_SEPARATOR_REGEX);
            if (parts.length >= 3) {
                return new PartyTemplate(parts[0].trim(), parts[1].trim(), parts[2].trim());
            }
            return null;
        });
    }

    private List<Scandal> loadAllScandals() {
        return loadCsvFile(SCANDAL_FILE, line -> {
            // Header check is handled in the generic part, strictly checking content here
            if (line.toLowerCase().startsWith("id")) return null;

            // Split with limit -1 to preserve empty strings
            String[] parts = line.split(",", -1);
            if (parts.length >= 5) {
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String type = cleanCsvString(parts[1]);
                    String name = cleanCsvString(parts[2]);
                    String desc = cleanCsvString(parts[3]);
                    double strength = Double.parseDouble(cleanCsvString(parts[4]).replace(",", "."));

                    return new Scandal(id, type, name, desc, strength);
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.FINE, "Skipping malformed line in scandal CSV: {0}", line);
                    return null;
                }
            }
            return null;
        });
    }

    // --- Generic Core Logic ---

    /**
     * Generic method to parse a CSV file.
     * Handles file I/O and stream management.
     *
     * @param filePath Path to the file in the classpath.
     * @param mapper   Function mapping a CSV line to an object T (or null).
     * @param <T>      The type of object to load.
     * @return A list of loaded objects.
     */
    private <T> List<T> loadCsvFile(String filePath, Function<String, T> mapper) {
        List<T> resultList = new ArrayList<>();

        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath)) {
            if (is == null) {
                LOGGER.log(Level.SEVERE, "CSV file not found: {0}", filePath);
                return resultList;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                boolean isFirstLine = true;

                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    // Skip header
                    if (isFirstLine && (line.toLowerCase().startsWith("id") || line.toLowerCase().startsWith("kuerzel"))) {
                        isFirstLine = false;
                        continue;
                    }
                    isFirstLine = false;

                    T item = mapper.apply(line);
                    if (item != null) {
                        resultList.add(item);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading file: " + filePath, e);
        }
        return resultList;
    }

    private String cleanCsvString(String input) {
        if (input == null) return "";
        return input.trim().replace("\"", "");
    }
}