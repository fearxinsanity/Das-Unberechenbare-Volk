/**
 * Validation utilities for input and parameter verification.
 *
 * <p>Provides centralized validation logic to ensure data integrity
 * across all application layers. Validates user input, simulation
 * parameters, and configuration values before they are processed
 * by the business logic.
 *
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link de.schulprojekt.duv.util.validation.ParameterValidator} - Validates simulation parameters
 *       against defined constraints (population size, party count, etc.)
 *   <li>Future validators for CSV data, file paths, and configuration
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * SimulationParameters params = new SimulationParameters(...);
 * ParameterValidator.validate(params); // Throws if invalid
 * }</pre>
 *
 * <h2>Design Rationale:</h2>
 * <p>Validation is separated from business logic to:
 * <ul>
 *   <li>Enable reuse across different input sources (UI, API, config files)
 *   <li>Provide consistent error messages
 *   <li>Simplify unit testing of validation rules
 *   <li>Follow Single Responsibility Principle
 * </ul>
 *
 * @author Nico Hoffmann
 * @version 1.0
 * @since 2.0
 */
package de.schulprojekt.duv.util.validation;