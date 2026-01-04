# 🏗️ Architektur-Dokumentation: Das Unberechenbare Volk

Dieses Dokument beschreibt die technische Architektur, die Design-Entscheidungen und die Struktur der Anwendung **Das Unberechenbare Volk**.

---

## 1. Architektur-Muster: MVC (Model-View-Controller)

Die Anwendung folgt strikt dem **Model-View-Controller (MVC)** Muster, um die Logik von der Darstellung und der Benutzerinteraktion zu trennen.

### 🧩 Die drei Schichten

| Schicht | Verantwortung | Hauptkomponenten |
| :--- | :--- | :--- |
| **Model** | Beinhaltet die gesamte Simulationslogik, den Status (State) und die Datenstrukturen. Es weiß nichts von der UI. | `SimulationEngine`, `VoterPopulation`, `Party`, `Scandal`, `SimulationState` |
| **View** | Zeigt die Daten an und leitet Benutzerinteraktionen weiter. Sie besteht aus FXML-Layouts, CSS und Canvas-Renderern. | `StartView.fxml`, `DashboardUI.fxml`, `CanvasRenderer`, `ChartManager` |
| **Controller** | Vermittelt zwischen Model und View. Er verarbeitet Inputs, steuert die Simulation und aktualisiert die View. | `SimulationController`, `DashboardController`, `StartController` |

---

## 2. Detaillierte Komponenten-Beschreibung

### 2.1 Model (Logik & Daten)
Das Herzstück der Anwendung. Hier werden Entscheidungen getroffen und Berechnungen durchgeführt.

* **`SimulationEngine`**: Die Hauptklasse, die den Simulations-Loop steuert. Sie triggert Updates für Wähler, Parteien und Ereignisse.
* **`VoterPopulation`**: Verwaltet die Gesamtheit aller Wähler (Agenten). Berechnet Wählerwanderungen basierend auf Wahrscheinlichkeiten (Matrix).
* **`Party`**: Repräsentiert eine politische Partei mit Attributen wie Budget, Beliebtheit, Programm und Farbe.
* **`ScandalScheduler`**: Ein Zufallsgenerator, der basierend auf Parametern (z.B. Skandal-Chance) Ereignisse auslöst.
* **`SimulationState`**: Ein Singleton oder zentrales Objekt, das den aktuellen Zustand (laufend, pausiert) und globale Parameter hält.

### 2.2 View (Benutzeroberfläche)
Die UI ist modern gestaltet ("Dark/Gold"-Theme) und nutzt JavaFX.

* **FXML-Dateien**:
    * `StartView.fxml`: Der Einstiegspunkt (Landing Page).
    * `DashboardUI.fxml`: Die Hauptansicht. Nutzt eine `StackPane`-Architektur, um Hintergrund-Layer (Gitter) und UI-Layer (HUD, Controls) zu überlagern.
* **`CanvasRenderer` (Performance-Optimierung)**:
    * Statt tausende JavaFX-Nodes für Wähler zu nutzen, zeichnet diese Komponente Wähler als **Partikel** und Verbindungen direkt auf ein `Canvas`.
    * Dies ermöglicht flüssige Animationen auch bei hohen Wählerzahlen (>100.000 simuliert, visuell repräsentiert).
* **CSS-Styling**:
    * `common.css`: Globale Stile (Fonts, Gitter-Hintergründe, HUD-Texte).
    * `dashboard.css` & `start.css`: Spezifische Stile für die jeweiligen Screens.

### 2.3 Controller (Steuerung)
* **`StartController`**: Handhabt die Navigation vom Start-Screen zum Dashboard.
* **`DashboardController`**:
    * Verbindet die UI-Elemente (Slider, Buttons) mit der `SimulationEngine`.
    * Nutzt `AnimationTimer` für den visuellen Update-Loop (60 FPS), getrennt vom logischen Simulations-Tick.

---

## 3. Datenfluss und Simulations-Loop

### Der Simulations-Zyklus (Tick)
Ein "Tick" repräsentiert eine Zeiteinheit (z.B. eine Woche im Wahlkampf).

1.  **Input**: Benutzer ändert Parameter (z.B. Medien-Einfluss) im Dashboard.
2.  **Update**: `SimulationEngine` berechnet neuen Status:
    * Skandale werden gewürfelt.
    * Parteibudgets werden verbraucht.
    * Wähler berechnen ihre Zufriedenheit neu und wechseln ggf. die Partei.
3.  **Notify**: Die Engine informiert den Controller über Änderungen.
4.  **Render**:
    * `ChartManager` aktualisiert den LineChart mit neuen Stimmenzahlen.
    * `CanvasRenderer` animiert die Partikel, die zwischen den Parteien "fliegen".
    * `FeedManager` fügt neue Ereignisse zum Ticker hinzu.

---

## 4. Wichtige Design-Entscheidungen

### A. Canvas vs. Scene Graph für Wähler
**Entscheidung:** Nutzung von `javafx.scene.canvas.Canvas` für die Darstellung der Wählerströme.
**Begründung:** JavaFX Scene Graph (einzelne Nodes pro Wähler) skaliert schlecht bei tausenden von Objekten. Ein Canvas, der Pixel direkt manipuliert ("Blitting" oder Drawing Primitives), ist wesentlich performanter für Partikel-Systeme.

### B. Trennung von Simulation und Visualisierung
**Entscheidung:** Die Simulation läuft in einem eigenen Takt (Ticks), während die Visualisierung (`AnimationTimer`) so oft wie möglich (bis zu 60fps) zeichnet.
**Vorteil:** Auch wenn die Simulation komplex rechnet, bleibt die UI reaktionsfähig. Partikel bewegen sich flüssig zwischen Start- und Endpunkt, unabhängig davon, wie schnell die Simulations-Ticks feuern.

### C. Zentrales Konfigurations-Objekt (`SimulationConfig` / Parameters)
**Entscheidung:** Alle Schwellenwerte (Farben, Startwerte, Limits) sind in Konfigurationsklassen ausgelagert.
**Vorteil:** Einfache Anpassung des Balancings ohne tiefes Eingreifen in den Code.

---

## 5. Verzeichnisstruktur (Auszug)

```text
src/main/java/de/schulprojekt/duv/
├── model/           # Reine Logik (Kein JavaFX Code!)
│   ├── core/        # Engine & State
│   ├── voter/       # Wählerverhalten
│   └── scandal/     # Ereignis-System
├── view/            # UI Code
│   ├── components/  # Spezialisierte Renderer (Canvas, Charts)
│   └── ...
├── controller/      # Verbindungsschicht
└── util/            # Hilfsklassen (CSV Loader, Config)
