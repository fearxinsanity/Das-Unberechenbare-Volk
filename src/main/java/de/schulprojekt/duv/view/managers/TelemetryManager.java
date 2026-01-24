package de.schulprojekt.duv.view.managers;

import de.schulprojekt.duv.view.Main;
import javafx.application.Platform;
import javafx.scene.control.Label;

import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manager für die Systemüberwachung und Hardware-Telemetrie.
 * @author Nico Hoffmann
 * @version 1.0
 */
public class TelemetryManager {

    // ========================================
    // Instance Variables
    // ========================================

    private final Label cpuLabel, gpuLabel, serverLoadLabel, ramLabel, uptimeLabel;
    private ScheduledExecutorService telemetryExecutor;

    // ========================================
    // Constructor
    // ========================================

    public TelemetryManager(Label cpu, Label gpu, Label load, Label ram, Label uptime) {
        this.cpuLabel = cpu;
        this.gpuLabel = gpu;
        this.serverLoadLabel = load;
        this.ramLabel = ram;
        this.uptimeLabel = uptime;
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    public void start() {
        if (telemetryExecutor != null && !telemetryExecutor.isShutdown()) return;

        fetchStaticHardwareInfo();

        telemetryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Telemetry-Monitor");
            t.setDaemon(true);
            return t;
        });

        telemetryExecutor.scheduleAtFixedRate(this::updateSystemTelemetry, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (telemetryExecutor != null) {
            telemetryExecutor.shutdownNow();
            telemetryExecutor = null;
        }
    }

    // ========================================
    // Utility Methods
    // ========================================

    private void fetchStaticHardwareInfo() {
        String cpuName = System.getenv("PROCESSOR_IDENTIFIER");
        if (cpuName == null || cpuName.isBlank()) {
            cpuName = System.getProperty("os.arch") + " PROCESSOR";
        }
        if (cpuName.length() > 28) cpuName = cpuName.substring(0, 28) + "...";
        if (cpuLabel != null) cpuLabel.setText(cpuName);

        String gpuText = "JFX DEFAULT";
        try {
            Object prism = System.getProperty("prism.order");
            if (prism != null) gpuText = "PIPELINE: " + prism.toString().toUpperCase();
        } catch (Exception ignored) {}
        if (gpuLabel != null) gpuLabel.setText(gpuText);
    }

    private void updateSystemTelemetry() {
        String ramText = getRealSystemRam();
        String loadText = getRealCpuLoad();
        String timeText = getUptimeText();

        Platform.runLater(() -> {
            if (ramLabel != null) ramLabel.setText(ramText);
            if (serverLoadLabel != null) serverLoadLabel.setText(loadText);
            if (uptimeLabel != null) uptimeLabel.setText(timeText);
        });
    }

    private String getRealSystemRam() {
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());
        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            long total = sunOsBean.getTotalMemorySize();
            long free = sunOsBean.getFreeMemorySize();
            return String.format(Locale.US, "%.1f / %.1f GB %s", (total - free) / 1073741824.0, total / 1073741824.0, bundle.getString("hw.physical"));
        }
        return "N/A";
    }

    private String getRealCpuLoad() {
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());
        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            double load = sunOsBean.getCpuLoad();
            return (load < 0) ? "CALC..." : String.format(Locale.US, bundle.getString("hw.load"), load * 100, osBean.getAvailableProcessors());
        }
        return "ACTIVE";
    }

    private String getUptimeText() {
        ResourceBundle bundle = ResourceBundle.getBundle("de.schulprojekt.duv.messages", Main.getLocale());
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        return String.format("%s %02d:%02d:%02d", bundle.getString("stats.session"), uptime / 3600, (uptime % 3600) / 60, uptime % 60);
    }
}