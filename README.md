# 🗳️ Das Unberechenbare Volk

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/GUI-JavaFX-blue)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36)
![Status](https://img.shields.io/badge/Status-Active-brightgreen)

> Eine interaktive, agentenbasierte Simulation von Wählerverhalten, politischen Einflüssen und Skandalen in Echtzeit.

---

## 📖 Über das Projekt

**Das Unberechenbare Volk** ist eine JavaFX-Anwendung, die die Dynamik politischer Systeme simuliert. Sie visualisiert, wie Wähler (repräsentiert durch Partikel) basierend auf verschiedenen Parametern wie Medien-Einfluss, Parteibudgets und zufälligen Skandalen zwischen Parteien wandern.

Das Ziel des Projekts ist es, komplexe soziologische Modelle durch eine ansprechende "Sci-Fi / Dashboard"-Oberfläche verständlich und experimentierbar zu machen.

## ✨ Features

### 🖥️ Visuelle Simulation
- **Partikel-System:** Wählerwanderungen werden als animierte Partikelströme zwischen Parteien dargestellt.
- **Netzwerk-Ansicht:** Parteien ordnen sich dynamisch in einem 2D-Raum an, verbunden durch Wählerströme.
- **Echtzeit-Graphen:** Live-Verfolgung der Stimmenverteilung über die Zeit.

### ⚙️ Interaktive Steuerung (Live)
Beeinflusse die Simulation während sie läuft:
- **Medien-Einfluss:** Wie stark reagieren Wähler auf Berichterstattung?
- **Mobilität:** Wie wechselwillig ist die Bevölkerung?
- **Loyalität:** Wie stark ist die Stammwählerbindung?
- **Budget:** Lege das durchschnittliche Wahlkampfbudget fest.
- **Skandal-Wahrscheinlichkeit:** Erhöhe oder senke die Chance auf politische Affären.

### ⚡ Ereignis-System
- **Skandal-Ticker:** Live-Ticker für generierte Ereignisse (Korruption, Persönliches, Finanzen).
- **News Feed:** Historie der letzten wichtigen Ereignisse.
- **Auswirkung:** Skandale haben direkte, berechnete Auswirkungen auf die Beliebtheit einer Partei.

---

## 🎓 Wissenschaftliche Fundierung

Die Simulation basiert auf etablierten Theorien der Wahlforschung und politischen Soziologie. Das Wählerverhalten wird durch sechs wissenschaftlich fundierte Archetypen modelliert, die unterschiedliche Entscheidungsmuster repräsentieren.

### 🧠 Wählertypen und theoretische Basis

#### 1. **Pragmatische Wähler** (25% der Bevölkerung)
- **Theoretische Basis:** Nutzenmaximierung, Issue-Voting
- **Charakteristik:** Niedrige Parteiloyalität (0.3), hohe Medienempfänglichkeit (1.2)
- **Verhalten:** Wechseln schnell zu Parteien, die ihnen direkten Nutzen versprechen
- **Literatur:** Downs (1957) - "An Economic Theory of Democracy"

#### 2. **Ideologische Wähler** (15% der Bevölkerung)
- **Theoretische Basis:** Michigan-Modell, Parteiidentifikation
- **Charakteristik:** Sehr hohe Loyalität (0.85), geringe Medienempfänglichkeit (0.5)
- **Verhalten:** Bleiben ihrer Partei treu, auch bei Skandalen
- **Literatur:** Campbell et al. (1960) - "The American Voter"

#### 3. **Rational-Choice-Wähler** (20% der Bevölkerung)
- **Theoretische Basis:** Rational-Choice-Theorie, retrospektives Voting
- **Charakteristik:** Mittlere Loyalität (0.5), hohe Positions-Sensitivität (1.2)
- **Verhalten:** Systematische Bewertung von Parteiprogrammen und Eigeninteressen
- **Literatur:** Fiorina (1981) - "Retrospective Voting in American National Elections"

#### 4. **Affektive Wähler** (15% der Bevölkerung)
- **Theoretische Basis:** Expressive Voting, Emotionale Entscheidungsfindung
- **Charakteristik:** Niedrige Loyalität (0.4), sehr hohe Medienempfänglichkeit (1.4)
- **Verhalten:** Entscheiden aus dem Bauchgefühl, stark von Charisma beeinflusst
- **Literatur:** Marcus et al. (2000) - "Affective Intelligence and Political Judgment"

#### 5. **Heuristische Wähler** (15% der Bevölkerung)
- **Theoretische Basis:** Bounded Rationality, Cognitive Shortcuts
- **Charakteristik:** Mittlere Loyalität (0.55), sehr hohe Medienempfänglichkeit (1.6)
- **Verhalten:** Nutzen mentale Abkürzungen (Medien, Parteimarke) bei geringem politischem Wissen
- **Literatur:** Popkin (1991) - "The Reasoning Voter"

#### 6. **Politikferne Wähler** (10% der Bevölkerung)
- **Theoretische Basis:** Political Disengagement, Low-Information Voters
- **Charakteristik:** Sehr niedrige Loyalität (0.2), inkonsistente Präferenzen
- **Verhalten:** Höchste Wechselbereitschaft, unberechenbar, geringe politische Bildung
- **Literatur:** Converse (1964) - "The Nature of Belief Systems in Mass Publics"

### 📐 Mathematisches Modell

#### Wechselwahrscheinlichkeit
Die Wahrscheinlichkeit, dass ein Wähler die Partei wechselt, wird berechnet als:

P(switch) = baseMobility × typeLoyalty × (1 - loyalty/180) × mediaInfluence × typeMedia

Wobei:
- baseMobility: Globaler Volatilitätsparameter (0-1)
- typeLoyalty: Typ-spezifischer Loyalitätsmodifikator
- loyalty: Individuelle Parteitreue des Wählers
- mediaInfluence: Individuelle Medienempfänglichkeit
- typeMedia: Typ-spezifischer Medienmodifikator

#### Parteibewertung
Für jede alternative Partei wird ein Attraktivitätsscore berechnet:

Score = distanceScore + (budgetScore × momentum) - scandalPenalty + noise

Komponenten:
- distanceScore: 40 / (1 + dist × typeSensitivity) - Ideologische Nähe
- budgetScore: Kampagnenbudget × Kampagneneffektivität × Medieneinfluss
- momentum: Tägliche Performance-Varianz (0.8 - 1.2)
- scandalPenalty: Akute + permanente Skandalschäden
- noise: Zufallsrauschen für Unvorhersagbarkeit

#### Zeitgeist-Drift
Globale politische Strömung beeinflusst individuelle Meinungsdrift:

newPosition = oldPosition + individualDrift + (zeitgeist × 0.1)
zeitgeist ∈ [-8, +8]  (negativ = links, positiv = rechts)

### 🔬 Validierung und Kalibrierung

Das Modell wurde kalibriert basierend auf:
- **Empirischen Daten:** Wahlforschungsstudien aus Deutschland und Europa
- **Wählerwanderungs-Analysen:** Infratest dimap, Forschungsgruppe Wahlen
- **Volatilitäts-Indices:** Pedersen-Index europäischer Wahlen
- **Skandal-Elastizität:** Studien zu politischen Affären und Umfragewerten

### 📚 Referenzen

1. **Campbell, A., et al.** (1960). *The American Voter*. University of Chicago Press.
2. **Converse, P. E.** (1964). "The Nature of Belief Systems in Mass Publics." *Ideology and Discontent*.
3. **Downs, A.** (1957). *An Economic Theory of Democracy*. Harper & Row.
4. **Fiorina, M. P.** (1981). *Retrospective Voting in American National Elections*. Yale University Press.
5. **Marcus, G. E., et al.** (2000). *Affective Intelligence and Political Judgment*. University of Chicago Press.
6. **Popkin, S. L.** (1991). *The Reasoning Voter: Communication and Persuasion in Presidential Campaigns*. University of Chicago Press.
7. **Blumenstiel, J. E.** (2018). "Wie sich Wähler beim Entscheiden unterscheiden." *Bundeszentrale für politische Bildung*.
8. **Arzheimer, K. & Falter, J. W.** "Wahlen und Wahlforschung." *Universität Göttingen*.

### 🎯 Anwendungsbereiche

Die Simulation eignet sich für:
- **Didaktische Zwecke:** Veranschaulichung von Wahlverhalten in der politischen Bildung
- **Hypothesentests:** Was-wäre-wenn-Szenarien (z.B. höherer Medieneinfluss)
- **Modellvalidierung:** Vergleich mit realen Wahlergebnissen
- **Sensitivitätsanalysen:** Einfluss einzelner Parameter auf Systemdynamik
- **Spieltheorie:** Optimale Strategien für Parteien unter verschiedenen Wählerzusammensetzungen

---

## 🚀 Installation & Start

### Voraussetzungen
- **Java JDK 25** oder höher
- **Maven** (zum Bauen und Abhängigkeiten laden)

### Projekt klonen und starten

1. **Repository klonen:**
   git clone https://github.com/fearxinsanity/das-unberechenbare-volk.git
   cd das-unberechenbare-volk

2. **Mit Maven bauen:**
   mvn clean install

3. **Anwendung starten:**
   mvn javafx:run

---

## 🏗️ Projekt-Architektur

### Hauptkomponenten

| Schicht | Package | Beschreibung |
|---------|---------|--------------|
| **Controller** | `controller/` | MVC-Controller für UI-Anbindung |
| **Model** | `model/` | Geschäftslogik der Simulation |
| **View** | `view/` | JavaFX UI-Komponenten |
| **Utilities** | `util/` | Hilfsfunktionen und Konfiguration |

### Model-Subpackages

| Package | Zweck | Wichtigste Klassen |
|---------|-------|-------------------|
| `calculation/` | Berechnungen & Statistiken | `StatisticsCalculator`, `VoterDecisionContext`, `PartyEvaluationResult` |
| `core/` | Simulation Engine | `SimulationEngine`, `SimulationState`, `SimulationParameters` |
| `voter/` | Wählerverhalten | `VoterType` (6 Archetypen), `VoterPopulation`, `VoterBehavior` |
| `party/` | Parteiensystem | `Party`, `PartyRegistry` |
| `scandal/` | Skandal-Mechanik | `ScandalEvent`, `ScandalImpactCalculator`, `ScandalScheduler` |
| `random/` | Zufallsverteilungen | `DistributionProvider` |

### Kern-Komponenten

**SimulationEngine**
- Orchestriert alle Subsysteme (Wähler, Parteien, Skandale)
- Verwaltet Simulation Lifecycle
- Erstellt Snapshots für statistische Analysen

**VoterBehavior**
- Implementiert Entscheidungslogik basierend auf wissenschaftlichen Modellen
- Verarbeitet 250.000+ Wähler parallel
- Nutzt 6 verschiedene Wählertypen mit unterschiedlichen Verhaltensmustern

**VoterType (Enum)**
- Pragmatisch (25%), Ideologisch (15%), Rational-Choice (20%)
- Affektiv (15%), Heuristisch (15%), Politikfern (10%)
- Jeder Typ hat individuelle Loyalitäts- und Medienmodifikatoren

**StatisticsCalculator**
- Berechnet aggregierte Metriken aus Simulation Snapshots
- Unterstützt zeitfenster-basierte Analysen
- Liefert Volatilität, Wählerwanderungen und Zeitgeist-Trends

**ParameterValidator**
- Validiert alle 9 Simulationsparameter
- Prüft Wertebereiche (z.B. Prozentsätze 0-100)
- Verhindert ungültige Konfigurationen

### 🔑 Kern-Komponenten

- **SimulationEngine:** Orchestriert alle Subsysteme (Wähler, Parteien, Skandale)
- **VoterBehavior:** Implementiert Entscheidungslogik basierend auf wissenschaftlichen Modellen
- **VoterType:** Enum mit 6 Wählertypen und typ-spezifischen Parametern
- **StatisticsCalculator:** Berechnet aggregierte Metriken aus Simulation Snapshots
- **ParameterValidator:** Validiert alle Eingabeparameter gegen definierte Grenzen

---

## 🎮 Verwendung

1. **Simulation starten:** Drücke den ▶️-Button im Dashboard
2. **Parameter anpassen:** Nutze die Schieberegler während der Laufzeit
3. **Beobachten:** Verfolge Wählerwanderungen in Echtzeit
4. **Analysieren:** Betrachte Statistiken und historische Trends
5. **Experimentieren:** Teste verschiedene Szenarien (z.B. hohe Skandalrate)

### 💡 Beispiel-Szenarien

- **Medien-dominiert:** Medieneinfluss = 90%, Beobachte schnelle Stimmungswechsel
- **Stammwähler-Gesellschaft:** Loyalität = 80%, Stabile Verhältnisse
- **Skandal-Welle:** Skandalwahrscheinlichkeit = 50%, Chaos und Unberechenbarkeit
- **Rationale Wähler:** Nur Rational-Choice-Typen (experimentelle Konfiguration)

---

## 🛠️ Technologie-Stack

- **Java 25** (Record Types, Sealed Classes)
- **JavaFX** (UI Framework)
- **Maven** (Dependency Management)
- **Java Streams API** (Parallele Verarbeitung)
- **ThreadLocalRandom** (Performance-optimierte Zufallszahlen)

---

## 🤝 Beitragen

Beiträge sind willkommen! Folge diesen Schritten:

1. Fork das Repository
2. Erstelle einen Feature-Branch (git checkout -b feature/NeuesFeature)
3. Commit deine Änderungen (git commit -m 'Add: Neues Feature')
4. Push zum Branch (git push origin feature/NeuesFeature)
5. Erstelle einen Pull Request

---

## 📝 Lizenz

Dieses Projekt ist unter der MIT-Lizenz lizenziert - siehe [LICENSE](LICENSE) Datei für Details.

---

## 👤 Autor

**Nico Hoffmann**
- GitHub: [@fearxinsanity](https://github.com/fearxinsanity)
- Projekt: Schulprojekt - Simulationssysteme

---

## 🙏 Danksagungen

- Wahlforschungs-Community für theoretische Grundlagen
- JavaFX-Community für UI-Komponenten
- Bundeszentrale für politische Bildung für didaktische Inspiration

---

## 📈 Roadmap

- [ ] Export-Funktionalität für CSV/JSON
- [ ] Machine Learning Integration für Vorhersagen
- [ ] Multiplayer-Modus (mehrere Nutzer steuern Parteien)
- [ ] Historische Wahldaten als Benchmark
- [ ] Mobile Version (JavaFX Mobile)
- [ ] A/B Testing Framework für Parameteroptimierung

---

**⚡ Built with passion for political science and software engineering**
