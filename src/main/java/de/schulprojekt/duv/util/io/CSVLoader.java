package de.schulprojekt.duv.util.io;

import de.schulprojekt.duv.model.party.PartyTemplate;
import de.schulprojekt.duv.model.scandal.Scandal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for loading game data from CSV files.
 * Supports localization and ensures unique colors.
 * @author Nico Hoffmann
 * @version 1.3
 */
public class CSVLoader {

    // ========================================
    // Static Variables
    // ========================================

    private static final Logger LOGGER = Logger.getLogger(CSVLoader.class.getName());
    private static final String CSV_SEPARATOR_REGEX = "[,;]";

    private static final String PARTY_FILE_BASE = "de/schulprojekt/duv/data/party_names";
    private static final String SCANDAL_FILE_BASE = "de/schulprojekt/duv/data/scandals";

    // ========================================
    // Instance Variables
    // ========================================

    private List<Scandal> cachedScandals = null;
    private final Locale currentLocale;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Creates a loader for a specific locale.
     * @param locale The locale to load data for (e.g., Locale.GERMAN, Locale.ENGLISH)
     */
    public CSVLoader(Locale locale) {
        this.currentLocale = locale;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    /**
     * Retrieves a set of random party templates.
     * Ensures that no two parties share the same color.
     * @param count number of templates to return
     * @return list of party templates
     */
    public List<PartyTemplate> getRandomPartyTemplates(int count) {
        List<PartyTemplate> allTemplates = loadAllParties();

        if (allTemplates.isEmpty()) {
            LOGGER.warning("No party templates found!");
            return new ArrayList<>();
        }

        Collections.shuffle(allTemplates);

        List<PartyTemplate> selection = new ArrayList<>();
        Set<String> usedColors = new HashSet<>();

        for (PartyTemplate template : allTemplates) {
            if (selection.size() >= count) break;

            if (!usedColors.contains(template.colorCode())) {
                selection.add(template);
                usedColors.add(template.colorCode());
            } else {
                LOGGER.fine("Skipping party " + template.name() + " due to duplicate color: " + template.colorCode());
            }
        }

        return selection;
    }

    /**
     * Retrieves a random scandal, utilizing a cache.
     * @return a random scandal object
     */
    public Scandal getRandomScandal() {
        if (cachedScandals == null) {
            cachedScandals = loadAllScandals();
        }

        if (cachedScandals == null || cachedScandals.isEmpty()) {
            LOGGER.warning("No scandals loaded. Creating fallback.");
            return new Scandal(0, "SCANDAL", "Unknown", "No data loaded.", 0.5);
        }

        return cachedScandals.get(ThreadLocalRandom.current().nextInt(cachedScandals.size()));
    }

    // ========================================
    // Utility Methods
    // ========================================

    private String getLocalizedFilePath(String basePath) {
        String lang = currentLocale.getLanguage().toLowerCase();
        // Support 'de' and 'en', default to 'de' if unknown
        if (!lang.equals("de") && !lang.equals("en")) {
            lang = "de";
        }
        return basePath + "_" + lang + ".csv";
    }

    private List<PartyTemplate> loadAllParties() {
        String filePath = getLocalizedFilePath(PARTY_FILE_BASE);
        LOGGER.info("Loading parties from: " + filePath);

        return loadCsvFile(filePath, line -> {
            String[] parts = line.split(CSV_SEPARATOR_REGEX);
            return (parts.length >= 3) ? new PartyTemplate(parts[0].trim(), parts[1].trim(), parts[2].trim()) : null;
        });
    }

    private List<Scandal> loadAllScandals() {
        String filePath = getLocalizedFilePath(SCANDAL_FILE_BASE);
        LOGGER.info("Loading scandals from: " + filePath);

        return loadCsvFile(filePath, line -> {
            if (line.trim().toLowerCase().startsWith("id")) return null;
            String[] parts = line.split(CSV_SEPARATOR_REGEX, -1);

            if (parts.length >= 5) {
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    String type = cleanCsvString(parts[1]);
                    String name = cleanCsvString(parts[2]);
                    String desc = cleanCsvString(parts[3]);
                    double str = Double.parseDouble(cleanCsvString(parts[4]).replace(",", "."));
                    return new Scandal(id, type, name, desc, str);
                } catch (NumberFormatException e) {
                    LOGGER.warning("Failed to parse scandal line: " + line);
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
            LOGGER.log(Level.SEVERE, "CSV file not found in classpath: {0}", filePath);
            return resultList;
        }

        try (InputStream stream = is;
             BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
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
            LOGGER.log(Level.SEVERE, "Error reading CSV: " + filePath, e);
        }
        return resultList;
    }

    private boolean isHeader(String line) {
        return line.startsWith("id") || line.startsWith("kuerzel") || line.startsWith("name") ||
                line.startsWith("partei") || line.contains("color") || line.contains("farbe") || line.contains("Abbreviation");
    }

    private String cleanCsvString(String input) {
        return (input == null) ? "" : input.trim().replace("\"", "");
    }
}