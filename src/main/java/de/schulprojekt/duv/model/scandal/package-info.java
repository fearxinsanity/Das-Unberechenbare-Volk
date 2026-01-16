/**
 * Scandal system modeling crisis events affecting political parties.
 *
 * <p>Implements a comprehensive scandal mechanic where parties can be hit
 * by crisis events that damage their reputation and cause voter flight.
 * Scandals have both acute (temporary) and permanent effects on party
 * attractiveness.
 *
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link de.schulprojekt.duv.model.scandal.Scandal} - Scandal definition with type, name, description,
 *       and strength (loaded from CSV)
 *   <li>{@link de.schulprojekt.duv.model.scandal.ScandalEvent} - Active scandal instance tracking which party
 *       is affected and when it occurred
 *   <li>{@link de.schulprojekt.duv.model.scandal.ScandalImpactCalculator} - Calculates time-dependent impact
 *       on voter behavior (acute pressure + permanent damage)
 * </ul>
 *
 * <h2>Scandal Mechanics:</h2>
 * <p><b>Acute Pressure</b> (Temporary):
 * <ul>
 *   <li>Peaks at 90 ticks after occurrence (fade-in period)
 *   <li>Gradually decays over next 210 ticks
 *   <li>Total duration: 300 ticks before removal
 *   <li>Formula: {@code strength * 4.0 * timeFactor}
 * </ul>
 *
 * <p><b>Permanent Damage</b> (Long-term):
 * <ul>
 *   <li>Accumulates during scandal duration
 *   <li>Persists after scandal ends
 *   <li>Slowly recovers based on vote share
 *   <li>Recovery rate: {@code 0.005 + (voteShare * 0.04)} per tick
 * </ul>
 *
 * <h2>Example Scenario:</h2>
 * <pre>
 * Tick 0:   Scandal hits Party A (strength=0.8)
 * Tick 90:  Acute pressure peaks at 3.2, voters flee
 * Tick 300: Scandal removed, but permanent damage remains
 * Tick 500: Party slowly recovers as it rebuilds support
 * </pre>
 *
 * <h2>Design Pattern:</h2>
 * <p>Uses Calculator pattern to separate:
 * <ul>
 *   <li><b>Data</b>: {@link de.schulprojekt.duv.model.scandal.Scandal}, {@link de.schulprojekt.duv.model.scandal.ScandalEvent} (immutable records)
 *   <li><b>Logic</b>: {@link de.schulprojekt.duv.model.scandal.ScandalImpactCalculator} (stateful calculator)
 * </ul>
 *
 * <h2>Thread Safety:</h2>
 * <p>Calculator maintains mutable state (permanent damage array) and
 * should only be accessed from the simulation thread.
 *
 * @author Nico Hoffmann
 * @version 1.1
 * @see de.schulprojekt.duv.util.config.SimulationConfig#SCANDAL_MAX_AGE_TICKS
 */
package de.schulprojekt.duv.model.scandal;
