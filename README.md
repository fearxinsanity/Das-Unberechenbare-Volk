# 🗳️ Das Unberechenbare Volk

![Java](https://img.shields.io/badge/Java-25-orange)
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

## 🚀 Installation & Start

### Voraussetzungen
- **Java JDK 25** oder höher
- **Maven** (zum Bauen und Abhängigkeiten laden)

### Projekt klonen und starten

1. **Repository klonen:**
   ```bash
   git clone [https://github.com/fearxinsanity/das-unberechenbare-volk.git](https://github.com/fearxinsanity/das-unberechenbare-volk.git)
   cd das-unberechenbare-volk
