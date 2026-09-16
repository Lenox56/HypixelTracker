# HypixelTracker – Projekt-Grundgerüst

Basiert auf den bisherigen Absprachen und dem bestehenden AttributeTracker.
Dies ist ein **kompilierbares Grundgerüst**, kein fertiges Endprodukt –
an den mit `TODO` markierten Stellen fehlt noch echte Logik (v.a. die
konkrete NBT-Feldauswertung, das Zusammenführen der Excel-Referenzliste
und die vollständige Minion-Referenztabelle).

## Module

- `Attribute / Shards` – Ist/Soll-Abgleich der Attribute-Shards
- `Accessoires` – Ist/Soll-Abgleich inkl. Craftbar-Status
- `Minions` – aktuelle vs. maximale Tier-Stufe, coop-weit
- `AH / Bazaar Flipping` – Unterpreis-Auktionen + Bazaar-Margen
- `Wiki-Suche` – freie Suche + Basis für kontextuelle Info-Icons

## Setup

1. JDK 25 (bereits vorhanden) und Maven installiert.
2. `mvn clean package` im Projektordner ausführen.
3. Beim ersten Start über den Button "Einstellungen" den eigenen
   Hypixel-API-Key und Minecraft-Usernamen eintragen (wird lokal unter
   `~/.hypixeltracker/config.json` gespeichert, **nicht** im Code).

## Bekannte offene Punkte (TODO im Code)

- Echte NBT-Feldnamen für `inv_contents`, `ender_chest_contents`,
  `talisman_bag` etc. gegen die aktuelle API-Antwort verifizieren
  (Struktur kann sich mit SkyBlock-Updates leicht ändern).
- Excel-Referenzliste aus dem ursprünglichen AttributeTracker einbinden.
- Vollständige Minion-Referenztabelle (`MinionReferenceData`) pflegen.
- Rezept-/Materialabgleich für Accessoires und Minion-Upgrades ergänzen.
- API-Calls in `javafx.concurrent.Task` auslagern, damit die UI beim
  Synchronisieren nicht blockiert.
- Normalpreis-Ermittlung für AH-Flipping verbessern (aktuell nur
  Platzhalter über Bazaar-Preise).

## Als .exe bauen

Sobald `mvn clean package` ein lauffähiges JAR erzeugt, das bereits
besprochene `build-exe.ps1`-Skript (jlink + jpackage) darauf anwenden –
Konfiguration dort an `MainClass = com.hypixeltracker.Main` anpassen.
