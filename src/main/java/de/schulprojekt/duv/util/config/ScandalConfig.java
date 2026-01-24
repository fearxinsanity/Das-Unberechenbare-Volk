package de.schulprojekt.duv.util.config;

/**
 * Konfigurationskonstanten für Skandal-Mechaniken und Auswirkungsberechnungen.
 * Steuert, wie Skandale Parteien über die Zeit beeinflussen und wie deren Erholung verläuft.
 *
 * @author Nico Hoffmann
 * @version 1.0
 */
public final class ScandalConfig {

    /**
     * Gesamtdauer, die ein Skandal aktiv bleibt.
     */
    public static final int SCANDAL_DURATION = 300;

    /**
     * Anzahl der Ticks, bis die Skandalauswirkung ihre maximale Intensität erreicht.
     */
    public static final int FADE_IN_TICKS = 90;

    /**
     * Multiplikator für akuten Skandaldruck auf Wählerentscheidungen.
     */
    public static final double ACUTE_PRESSURE_FACTOR = 6.0;

    /**
     * Multiplikator für die Akkumulation permanenter Reputationsschäden.
     */
    public static final double PERMANENT_DAMAGE_FACTOR = 1.5;

    /**
     * Basisrate, mit der sich permanenter Schaden pro Tick erholt.
     */
    public static final double BASE_RECOVERY_RATE = 0.003;

    /**
     * Zusätzlicher Erholungsratten-Bonus basierend auf dem Wähleranteil.
     */
    public static final double VOTER_SHARE_RECOVERY_FACTOR = 0.04;

    private ScandalConfig() {
        throw new UnsupportedOperationException("Configuration class cannot be instantiated");
    }

    /**
     * Berechnet den zeitbasierten Einflussfaktor für einen Skandal in einem bestimmten Alter.
     * Der Einfluss folgt einer quadratischen Anstiegskurve und danach einem linearen Abfall.
     *
     * @param age das Alter des Skandals in Ticks
     * @return Einflussfaktor
     */
    public static double calculateTimeFactor(int age) {
        if (age < 0) return 0.0;
        if (age >= SCANDAL_DURATION) return 0.0;

        if (age < FADE_IN_TICKS) {
            double progress = (double) age / FADE_IN_TICKS;
            return progress * progress;
        }

        return 1.0 - ((double) (age - FADE_IN_TICKS) / (SCANDAL_DURATION - FADE_IN_TICKS));
    }

    /**
     * Berechnet die Akkumulation des permanenten Schadens pro Tick.
     *
     * @param scandalStrength die Stärke des Skandals
     * @return Schadenszuwachs pro Tick
     */
    public static double calculatePermanentDamagePerTick(double scandalStrength) {
        return (scandalStrength * PERMANENT_DAMAGE_FACTOR) / (double) SCANDAL_DURATION;
    }

    /**
     * Berechnet die Erholungsrate für eine Partei basierend auf ihrem Wähleranteil.
     *
     * @param voterSharePercent der aktuelle Wähleranteil
     * @return Gesamte Erholungsrate pro Tick
     */
    public static double calculateRecoveryRate(double voterSharePercent) {
        return BASE_RECOVERY_RATE + (voterSharePercent * VOTER_SHARE_RECOVERY_FACTOR);
    }
}