/**
 * Statistical distribution providers for stochastic simulation elements.
 *
 * <p>Provides probability distribution implementations used to generate
 * realistic voter characteristics and behavior patterns. Ensures statistical
 * consistency across simulation runs while maintaining randomness.
 *
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link de.schulprojekt.duv.model.random.DistributionProvider} - Samples from normal distribution
 *       for voter loyalty values (mean=50, configurable std deviation)
 * </ul>
 *
 * <h2>Statistical Foundation:</h2>
 * <p>Uses truncated normal distribution for voter loyalty:
 * <pre>
 * μ (mean)  = 50.0
 * σ (std)   = 15.0 (default)
 * Range     = [0, 100]
 * </pre>
 *
 * <p>This produces a realistic bell curve where most voters have moderate
 * loyalty (~40-60), with fewer extremely loyal or disloyal voters.
 *
 * <h2>Design Rationale:</h2>
 * <p>Separated from voter package to:
 * <ul>
 *   <li>Enable reuse for other stochastic elements (scandal probability, etc.)
 *   <li>Facilitate testing with deterministic seeds
 *   <li>Centralize statistical algorithm changes
 *   <li>Support future distribution types (uniform, exponential, etc.)
 * </ul>
 *
 * <h2>Thread Safety:</h2>
 * <p>Uses {@link java.util.concurrent.ThreadLocalRandom} for thread-safe
 * random number generation during parallel voter initialization.
 *
 * @author Nico Hoffmann
 * @version 1.0
 * @see java.util.Random
 * @see java.util.concurrent.ThreadLocalRandom
 */
package de.schulprojekt.duv.model.random;
