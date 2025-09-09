# Projektdokumentation: Das unberechenbare Volk - Ein interaktiver Wahl-Simulator
## 1. Einführung und Projektziele
Dieses Dokument beschreibt die Konzeption und den technischen Entwurf des Projekts „Das unberechenbare Volk“, einem interaktiven Wahl-Simulator, der im Rahmen der Anforderungen des Projektauftrags für das 3. Lehrjahr entwickelt wird. Das Ziel der Anwendung ist es, die komplexen Dynamiken von Wahlen durch eine zeitabhängige Simulation zu visualisieren. Die Anwendung soll als Windows-Anwendung mit einer grafischen Benutzeroberfläche (GUI) umgesetzt werden, die auf Schulrechnern ausführbar ist.

Kernziele sind die Darstellung der Interaktionen zwischen Wählern, Parteien und äußeren Einflüssen sowie die Bereitstellung einer intuitiven Benutzeroberfläche im Stil von Strategiespielen.

## 2. Vorgehensmodell und Zeitplan
Aufgrund des festen Abgabetermins am 06.02.2025 wird ein lineares Wasserfall-Modell für die Projektdurchführung gewählt. Dieses Modell ermöglicht eine strukturierte, phasenbasierte Entwicklung, die eine genaue Dokumentation jeder Phase gewährleistet. Der Zeitplan ist in diskrete Phasen unterteilt, die sich auf die Implementierung, das Testen und die Dokumentation erstrecken.

## 3. Systemarchitektur und Technologie-Stack
Das gesamte System basiert auf einer klaren Model-View-Controller (MVC)-Architektur. Diese Struktur trennt die Anwendungslogik von der Benutzeroberfläche und erhöht so die Wartbarkeit und Skalierbarkeit des Systems.

**Model**: Enthält die gesamte Simulationslogik und Datenmodelle. Geplante Klassen sind ``Voter`` und ``Party``, die als einfache POJOs (Plain Old Java Objects) die Attribute der simulierten Wähler und Parteien speichern. Die zentrale Klasse ``SimulationEngine`` steuert die Hauptschleife und Interaktionen.

`**View**: Repräsentiert das User Interface, das mit JavaFX umgesetzt wird. Die zentrale Klasse ``DashboardUI`` erstellt und verwaltet das Frontend.

**Controller**: Die Klasse ``SimulationController`` dient als Vermittler, verarbeitet Nutzereingaben aus der View und aktualisiert das Model und die View.

Als Technologie-Stack kommt **Java SE** zum Einsatz. Die Abhängigkeiten für JavaFX werden über **Maven** verwaltet. Das Projekt ist für die Ausführung mit **JDK 21** konfiguriert.

## 4. Simulation und Zufallselemente
Die Simulation ist zeitabhängig und läuft in diskreten Zeitschritten ab. Sie muss die Generierung von mindestens drei Zufallswerten mit verschiedenen Verteilungsformen (Normal-, Gleich-, Exponentialverteilung) implementieren.

**Meinungsschwankungen**: Die Meinungsbildung der Wähler wird durch eine Wahrscheinlichkeitsverteilung, wie z.B. die Normalverteilung, simuliert.

**Werbeeffektivität**: Der Erfolg von Wahlwerbung ist ein Zufallsereignis, dessen Wahrscheinlichkeit durch das Kampagnenbudget beeinflusst wird.

**Zufällige Ereignisse**: Unregelmäßige Ereignisse wie Skandale können mit einer geringen Wahrscheinlichkeit pro Zeitschritt auftreten und die Wählerstimmung stark beeinflussen.

## 5. Benutzeroberfläche (GUI) und Interaktion
Das GUI-Konzept orientiert sich an den Prinzipien des **ISO 9241-110 Standards** und ist auf Benutzerfreundlichkeit ausgelegt. Das Frontend wird in drei Hauptbereiche unterteilt:

**Steuerung**: Über Slider und Eingabefelder kann der Benutzer die Simulation mit mindestens sieben Parametern konfigurieren. Dazu gehören die Anzahl der Wähler und Parteien, die Anfangspräferenzen, der Medieneinfluss und die Kampagnenbudgets.

**Visualisierung**: Die Ergebnisse der Simulation werden in Echtzeit über dynamische Diagramme und eine kleine Animation visualisiert.

**Ereignis-Feed**: Ein separater Bereich zeigt zufällige Ereignisse und deren Auswirkungen an.

Die Simulationsgeschwindigkeit ist in drei Stufen wählbar, was die Echtzeit-Visualisierung der Ergebnisse ermöglicht.

## 6. Qualitätssicherung und Testplanung
Die Qualitätssicherung umfasst die Überprüfung der funktionalen und nichtfunktionalen Anforderungen.

**Funktionalität**: Es werden Tests durchgeführt, um die korrekte Verarbeitung der Eingabeparameter und die Genauigkeit der Verteilungsfunktionen zu gewährleisten.

**Performance**: Die Anwendung muss stabil mit bis zu 100.000 Wählern laufen. Performance-Tests stellen sicher, dass die Benutzeroberfläche auch unter Last reaktionsfähig bleibt.

**Dokumentation**: Die Testprotokolle werden als Anhang der Projektdokumentation beigefügt.

## 7. Dokumentation
Die Projektdokumentation wird nach IHK-Standard erstellt und hat einen Mindestumfang von 6 Seiten ohne Anhang. Sie wird alle Phasen des gewählten Vorgehensmodells abbilden und die Auswahl der Technologien und des Designs detailliert begründen. Ein separates, digitales Benutzerhandbuch wird ebenfalls bereitgestellt.
