# Architektur-Dokument: "Das unberechenbare Volk"
## 1. Gesamtsystem-Architektur
Das Projekt folgt einer Model-View-Controller (MVC)-Architektur, um eine klare Trennung der Verantwortlichkeiten zu gewährleisten.

 - **Model**: Enthält die gesamte **Kernlogik der Simulation** und die zugehörigen Daten. Es ist komplett vom UI entkoppelt und kann theoretisch auch von einem anderen Frontend verwendet werden.

- **View**: Repräsentiert das **User Interface (UI)**, das mit **JavaFX** umgesetzt wird. Es ist für die Darstellung der Simulation und die Erfassung der Benutzereingaben verantwortlich. Die View ist passiv und wird durch den Controller aktualisiert.

- **Controller**: Dient als **Vermittler** zwischen Model und View. Er verarbeitet Ereignisse aus der View (z. B. eine Änderung an einem Slider), leitet die Anfragen an das Model weiter, ruft die Simulationsdaten ab und aktualisiert die View.

Diese Struktur stellt sicher, dass Änderungen am UI die Simulationslogik nicht beeinträchtigen und umgekehrt, was die Wartbarkeit und Skalierbarkeit des Systems erhöht.

## 2. Datenfluss
Der Datenfluss im System ist unidirektional und klar definiert:

1. **Benutzereingabe**: Der Benutzer ändert über die UI-Steuerung (z. B. einen Slider im **View**) einen Parameter wie den Medieneinfluss oder das Budget einer Partei.

2. **Eingabeverarbeitung**: Der **Controller** fängt dieses Ereignis ab und ruft eine entsprechende Methode im **Model** auf, um die Simulationsparameter zu aktualisieren.

3. **Simulations-Logik**: Das **Model** verarbeitet die neuen Parameter und führt die Simulation für den nächsten Zeitschritt aus. Hierbei werden die Meinungen der Wähler aktualisiert, neue Ereignisse generiert etc.

4. **Datenabfrage**: Der **Controller** fragt die aktualisierten Daten (z. B. aktuelle Wählerpräferenzen, Unterstützerzahlen der Parteien) aus dem **Model** ab.

5. **View-Aktualisierung**: Der **Controller** übergibt die abgerufenen Daten an die **View**, die sie in Echtzeit aktualisiert. Dies betrifft dynamische Diagramme und den Ereignis-Feed.

## 3. Kern-Komponenten (Geplante Klassen)
Die folgenden Klassen bilden das Rückgrat der Anwendung. Sie sollten im Einklang mit unseren Javadoc-Konventionen dokumentiert werden.

- `Voter`: Einfaches POJO. Speichert Attribute wie die aktuelle politische Präferenz.

- `Party`: Einfaches POJO. Speichert Attribute wie das Kampagnenbudget und die aktuelle Anzahl der Unterstützer.

- `SimulationEngine`: Die zentrale Klasse des Models. Sie enthält die Haupt-Schleife der Simulation, implementiert die Logik für die Zeitschritte und verwaltet die Interaktionen zwischen Wählern und Parteien.

- `SimulationController`: Der **Controller**, der die Kommunikation zwischen der `View` und der `SimulationEngine` steuert. Er verarbeitet die Benutzerinteraktionen und löst die Simulation aus.

- `DashboardUI`: Die zentrale Klasse des **Views**, die das JavaFX-Frontend mit allen Diagrammen und Steuerelementen aufbaut und verwaltet.

4. Modellierung der Zufallselemente
Die Unberechenbarkeit des politischen Prozesses wird durch die Integration von Zufallselementen modelliert.

- **Zufällige Meinungsänderungen**: Um die persönliche Meinungsbildung zu simulieren, wird eine **Wahrscheinlichkeitsverteilung** (z.B. eine Normalverteilung) verwendet. Jeder Wähler hat eine kleine Chance, seine Präferenz zu ändern. Die Wahrscheinlichkeit dafür kann durch den Medieneinfluss-Faktor gewichtet werden.

- **Effektivität der Wahlwerbung**: Der Erfolg von Kampagnen ist zufällig. Die Wirkung eines eingesetzten Budgets sollte nicht deterministisch sein, sondern als ein Wahrscheinlichkeitsereignis mit einer variablen Effektivität modelliert werden.

- **Zufällige Ereignisse**: Skandale oder Debatten können mit einer bestimmten, geringen Wahrscheinlichkeit pro Zeitschritt auftreten. Wenn ein Ereignis eintritt, wird seine Auswirkung (z.B. eine starke Verschiebung der Präferenzen einer bestimmten Wählergruppe) mit einer Gewichtung auf die Simulation angewendet.
