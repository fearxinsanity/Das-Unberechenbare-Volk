# Das unberechenbare Volk - Ein interaktiver Wahl-Simulator 🗳️
Dieses Projekt, "Das unberechenbare Volk", ist ein interaktiver Wahl-Simulator, der in Java mit einem JavaFX-Frontend entwickelt wird. Ziel ist es, auf anschauliche Weise zu visualisieren, wie verschiedene Faktoren wie Medieneinfluss, politische Kampagnen und zufällige Ereignisse den Ausgang einer Wahl in einer simulierten Bevölkerung beeinflussen können.

Das Design und die Benutzeroberfläche orientieren sich an beliebten "Democracy"-Strategiespielen, um eine intuitive und visuell ansprechende Erfahrung zu bieten. Die Unvorhersehbarkeit des politischen Prozesses wird durch die Integration von Zufallselementen simuliert, wodurch jede Ausführung ein einzigartiges Ergebnis liefert.

## Projektziele
Verständnis der Dynamiken: Visualisierung der komplexen Interaktionen zwischen Wählern, Parteien und äußeren Einflüssen.

Interaktive Steuerung: Live-Anpassung von Kernparametern wie Medieneinfluss und Kampagnenbudgets, um deren Auswirkungen direkt zu beobachten.

Ansprechende Visualisierung: Ein intuitives und optisch ansprechendes User Interface (UI) im Stil von Strategiespielen, das die Simulationsergebnisse klar darstellt.

Technologische Umsetzung: Anwendung und Vertiefung von Kenntnissen in Java-Programmierung und JavaFX.

## Funktionsumfang
### Kernsimulation
Wähler: Jeder simulierte Wähler hat eine initiale Präferenz, kann seine Meinung ändern und wird von verschiedenen Faktoren beeinflusst. Um die Performance zu gewährleisten, ist die Simulation auf maximal 100.000 Wähler beschränkt.

Parteien/Kandidaten: Jede Partei besitzt ein Kampagnenbudget und eine dynamische Anzahl an Unterstützern.

Zeitschritte: Die Simulation läuft in diskreten Zeitschritten ab, die den Verlauf der Wahlperiode abbilden.

### Eingabeparameter & Steuerung
Zu Beginn und während der Simulation können die folgenden Parameter über Slider im Dashboard angepasst werden:

Anzahl der Wähler und Parteien

Anfängliche Wählerpräferenzen

Faktor für den Medieneinfluss

Kampagnenbudgets der Parteien

### Zufallsparameter und Ereignisse
Um die Realität abzubilden, sind folgende Zufallselemente integriert, deren Effekte mithilfe von Wahrscheinlichkeitsverteilungen modelliert werden:

Meinungsschwankungen der Wähler: Simuliert persönliche Meinungsbildung.

Effektivität von Wahlwerbung: Der Erfolg von Kampagnen ist zufällig, wird aber durch das Budget beeinflusst.

Zufällige Ereignisse: Unregelmäßige Ereignisse wie Skandale oder Debatten können die Wählerstimmung beeinflussen.

## Technischer Entwurf
Das Projekt basiert auf einer Model-View-Controller (MVC)-Architektur.

Model: Enthält die gesamte Simulationslogik und die Datenmodelle (Wähler, Parteien) als einfache Java POJOs.

View (JavaFX): Das grafische Dashboard, das alle Diagramme und Interaktionselemente anzeigt. Es wird mit JavaFX gebaut und als eigenständige Desktop-Anwendung verpackt. Die Oberfläche wird in drei Bereiche aufgeteilt: Steuerung, Visualisierung und ein Ereignis-Feed.

Controller: Verbindet die View und das Model. Er verarbeitet die Benutzereingaben aus dem JavaFX-Frontend und kommuniziert mit der Java-Simulations-Engine, um die Simulationsdaten zu erhalten und das Frontend zu aktualisieren.

## Zeitplan
Der Zeitplan ist ambitioniert, aber realistisch, um die definierten Kernfunktionen umzusetzen.

Monat 1-2: Grundlagen und Kernfunktionalität (JavaFX-Setup, Wähler-/Partei-Klassen, grundlegende Simulations-Engine).

Monat 3-4: Zufälligkeit und Interaktion (Implementierung von Zufallsverteilungen für Meinungen und Ereignisse, Hinzufügen interaktiver Parameter).

Monat 5-6: Visualisierung und Feinschliff (Integration dynamischer Diagramme, Designanpassungen, Tests und Optimierung).

## Erwartete Ergebnisse
Ein voll funktionsfähiger, interaktiver Wahl-Simulator, der die Komplexität politischer Dynamiken in Echtzeit darstellt und dem Benutzer ein tieferes Verständnis für die Vielschichtigkeit von Wahlprozessen vermittelt. Das Projekt wird als abgeschlossen betrachtet, wenn:

Die Simulation stabil mit bis zu 100.000 Wählern und 10 Parteien läuft.

Alle im UI verfügbaren Parameter die Simulation in Echtzeit beeinflussen.

Das Frontend die Ergebnisse der Simulation in lesbaren Diagrammen anzeigt.

Dieser Entwurf dient als Grundlage. Zusätzliche Funktionen, wie z. B. erweiterte Ereignisbäume oder weitere Visualisierungsoptionen, werden in einer Phase 2 in Betracht gezogen.







