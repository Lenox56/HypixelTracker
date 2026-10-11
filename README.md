# SkyBlock Progress Tracker

> **Unofficial fan project. Not affiliated with, endorsed by, or associated
> with Hypixel Inc.** "Hypixel" and "SkyBlock" are trademarks of their
> respective owners.

**English summary:** A non-monetized desktop app for a small group of friends
to track their own Hypixel SkyBlock progress (hunting attribute levels, missing
accessories, minion tiers) and view Bazaar prices.

- The API key is never included in the app, and users never enter their own key.
- The only authenticated endpoint used is `/v2/skyblock/profiles`, called by
  our own small backend (`server/`). The backend **does not proxy the API**:
  it evaluates the profile on the server and returns only the derived progress
  data the app displays (attribute shard counts, crafted minion tiers, owned
  accessory IDs) – never the raw API response.
- Responses are cached for 5 minutes per player, the `RateLimit-*` headers are
  respected, and each client is limited to 10 requests per minute.
- Bazaar, item and auction data use the public endpoints without a key.
- No automated data collection, no player tracking, no auction sniping.

---

Desktop-App (Java/JavaFX) für Hypixel SkyBlock, basierend auf dem
ursprünglichen AttributeTracker. **Inoffizielles Fan-Projekt, nicht mit
Hypixel verbunden und nicht von Hypixel unterstützt.**

## Module

- `Attribute / Shards` – Hunting-Attribute mit Stufe, gesyphonten Shards
  und fehlenden Shards bis zur nächsten Stufe; nicht freigeschaltete Attribute
- `Accessoires` – fehlende Accessoires (inkl. Upgrade-Stufen) mit aktuellem
  Bazaar-Preis bzw. günstigstem AH-Sofortkauf
- `Minions` – aktuelle vs. maximale Stufe je Minion-Typ, coop-weit
- `Bazaar Flipping` – Bazaar-Margen nach Steuer mit Mindestvolumen
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
- Fortschrittsdaten (Attribute, Minions, Accessoires) → brauchen einen Key und
  kommen vom eigenen Server im Ordner [`server/`](server/README.md). Nur der
  Server kennt den Key; er wertet die Profile selbst aus und gibt nur die
  fertigen Fortschrittsdaten heraus, nie die rohe API-Antwort.

Die Server-Adresse steht in `AppConfig.DEFAULT_SERVER_URL`
(aktuell `https://lenox-tracker.duckdns.org`) und kann von Nutzern unter
"Einstellungen" überschrieben werden.

## Bekannte offene Punkte

- Rezept-/Materialabgleich für Accessoires und Minion-Upgrades
  (craftbar ja/nein, Materialkosten).
- Eigenes Logo: `src/main/resources/icons/` und `packaging/icon.ico` enthalten
  ein schlichtes Platzhalter-Icon (Lupe mit Diagramm).

## Als .exe bauen

Sobald `mvn clean package` ein lauffähiges JAR erzeugt, das bereits
besprochene `build-exe.ps1`-Skript (jlink + jpackage) darauf anwenden –
Konfiguration dort an `MainClass = com.hypixeltracker.Main` anpassen.
