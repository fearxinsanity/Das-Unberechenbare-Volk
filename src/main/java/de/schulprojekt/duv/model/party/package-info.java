/**
 * Political party entities and related data structures.
 *
 * <p>Defines the representation of political parties in the simulation,
 * including their attributes, behavior, and initialization from external
 * data sources (CSV templates).
 *
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link de.schulprojekt.duv.model.party.Party} - Main party entity with political position,
 *       campaign budget, supporter count, and scandal tracking
 *   <li>{@link de.schulprojekt.duv.model.party.PartyTemplate} - Immutable template record for loading
 *       party data from CSV files (name, abbreviation, color)
 * </ul>
 *
 * <h2>Domain Model:</h2>
 * <p>Parties are characterized by:
 * <ul>
 *   <li><b>Political Position</b> (0-100): Left-Right ideological spectrum
 *   <li><b>Campaign Budget</b>: Financial resources for voter outreach
 *   <li><b>Supporter Count</b>: Current number of affiliated voters
 *   <li><b>Color Code</b>: Hex color for visualization (e.g., "#FF0000")
 * </ul>
 *
 * <h2>Political Spectrum Mapping:</h2>
 * <pre>
 *   0─────20─────40─────60─────80─────100
 *   Far-Left  Left  Center  Right  Far-Right
 * </pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>Party objects are mutable and should only be accessed from the
 * simulation thread. Use defensive copying when passing to UI layer.
 *
 * @author Nico Hoffmann
 * @version 1.0
 * @see de.schulprojekt.duv.util.io.CSVLoader
 */
package de.schulprojekt.duv.model.party;
