# HypixelTracker

Desktop-App (Java/JavaFX) für Hypixel SkyBlock, basierend auf dem
ursprünglichen AttributeTracker.

## Module

- `Attribute / Shards` – Hunting-Attribute mit Stufe, gesyphonten Shards
  und fehlenden Shards bis zur nächsten Stufe; nicht freigeschaltete Attribute
- `Accessoires` – fehlende Accessoires (inkl. Upgrade-Stufen) mit aktuellem
  Bazaar-/AH-Preis
- `Minions` – aktuelle vs. maximale Stufe je Minion-Typ, coop-weit
- `AH / Bazaar Flipping` – Sofortkäufe deutlich unter dem nächsten Angebot
  (Gewinn nach AH-Gebühren) und Bazaar-Margen nach Steuer mit Mindestvolumen
- `Wiki-Suche` – freie Suche + Basis für kontextuelle Info-Icons

**Accessoires** brauchen die Inventar-API: im Spiel unter
*SkyBlock-Menü → Settings → API Settings* aktivieren.

## Referenzdaten

Shard-Liste, Accessoire-Upgrades und maximale Minion-Stufen liegen unter
`src/main/resources/data/` und stammen aus dem
[NotEnoughUpdates-REPO](https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO)
(MIT-Lizenz, siehe `data/NOTICE.txt`). Nach SkyBlock-Updates aktualisieren:

```
git clone --depth 1 https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO neu
python tools/update_reference_data.py neu
```

## Setup

1. JDK 25 (bereits vorhanden) und Maven installiert.
2. `mvn clean package` im Projektordner ausführen.
3. Beim ersten Start über den Button "Einstellungen" den
   Minecraft-Usernamen eintragen (wird lokal unter
   `~/.hypixeltracker/config.json` gespeichert).

## Hypixel-API-Key & Server

Die App enthält **keinen** API-Key und fragt Nutzer auch nicht nach einem.
Laut [Hypixel-API-Policy](https://developer.hypixel.net/policies/) dürfen
Nutzer ihre Keys nicht in Anwendungen Dritter eintragen.

- Bazaar, Auktionen, Items → brauchen keinen Key, die App fragt direkt bei Hypixel.
- SkyBlock-Profile → brauchen einen Key und laufen über den eigenen Server
  im Ordner [`server/`](server/README.md). Nur der Server kennt den Key.

Die Server-Adresse steht in `AppConfig.DEFAULT_SERVER_URL`
(aktuell `https://lenox-tracker.duckdns.org`) und kann von Nutzern unter
"Einstellungen" überschrieben werden.

## Bekannte offene Punkte

- Rezept-/Materialabgleich für Accessoires und Minion-Upgrades
  (craftbar ja/nein, Materialkosten).
- AH-Flipping vergleicht nur mit dem nächstgünstigeren Angebot; Haustiere
  und Bücher mit mehreren Verzauberungen werden ausgelassen.

## Als .exe bauen

Sobald `mvn clean package` ein lauffähiges JAR erzeugt, das bereits
besprochene `build-exe.ps1`-Skript (jlink + jpackage) darauf anwenden –
Konfiguration dort an `MainClass = com.hypixeltracker.Main` anpassen.
