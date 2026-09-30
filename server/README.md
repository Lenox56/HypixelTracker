# HypixelTracker-Server

Kleiner Server, der den Hypixel-API-Key hält. Die Desktop-App fragt
SkyBlock-Profile hier ab statt direkt bei Hypixel. So kann die App
weitergegeben werden, **ohne dass jemand einen Key eintragen muss**
(das verbietet die [Hypixel-API-Policy](https://developer.hypixel.net/policies/)).

```
App (Nutzer A, B, C …) ──> dieser Server (hält den Key, Cache) ──> api.hypixel.net
```

Was der Server macht:

- `GET /v1/skyblock/profiles?uuid=<uuid>` → Hypixel-Antwort unverändert durchgereicht
- `GET /health` → Statuscheck
- Speichert Profile zwischen (Standard: 5 Minuten), damit nicht jeder Klick eine Hypixel-Anfrage kostet
- Wertet die `RateLimit-*`-Header von Hypixel aus und pausiert, bevor das Limit des Keys überschritten wird
- Begrenzt Anfragen pro Nutzer-IP (Standard: 30/Minute), damit niemand das gemeinsame Limit aufbraucht
- Gibt den Key nie an Clients zurück, auch nicht in Fehlermeldungen

Bazaar, Auktionen und Items brauchen keinen Key. Die fragt die App weiterhin
direkt bei Hypixel ab.

## Vorher: Key bei Hypixel beantragen

1. Auf <https://developer.hypixel.net> mit dem Minecraft-Account anmelden.
2. Eine **neue Application** für dieses Projekt anlegen (ein Projekt = eine Application).
3. Zum Testen reicht der Development-Key (läuft nach 3 Tagen ab, nicht zum Weitergeben).
4. Damit andere die App nutzen dürfen: **Production-Key** beantragen und die App
   beschreiben (Desktop-Tracker, Key nur auf dem Server, Caching, Rate-Limit pro Nutzer).

## Einrichtung auf Hetzner

### 1. Server anlegen

In der [Hetzner Cloud Console](https://console.hetzner.cloud):

- **Image:** Ubuntu 24.04
- **Typ:** der kleinste und günstigste Typ reicht völlig
- **SSH-Key** hinterlegen (empfohlen statt Passwort)
- **Firewall:** eingehend nur TCP 22, 80 und 443 erlauben

### 2. Domain auf den Server zeigen lassen

Beim Domain-Anbieter einen **A-Record** anlegen, z. B. `tracker.deinedomain.de`,
der auf die IPv4-Adresse des Servers zeigt. Das braucht Caddy für das
kostenlose HTTPS-Zertifikat.

### 3. Docker installieren

```bash
ssh root@<server-ip>
curl -fsSL https://get.docker.com | sh
```

### 4. Projekt holen und konfigurieren

```bash
git clone https://github.com/Lenox56/HypixelTracker.git
cd HypixelTracker/server
cp .env.example .env
nano .env        # HYPIXEL_API_KEY und DOMAIN eintragen
chmod 600 .env   # nur root darf den Key lesen
```

### 5. Starten

```bash
docker compose up -d --build
docker compose logs -f api     # sollte "HypixelTracker-Server laeuft ..." zeigen
curl https://tracker.deinedomain.de/health
```

### 6. App auf den Server zeigen lassen

In `src/main/java/com/hypixeltracker/config/AppConfig.java` die Konstante
`DEFAULT_SERVER_URL` auf `https://tracker.deinedomain.de` setzen und die
.exe neu bauen. Einzelne Nutzer können die Adresse auch unter
„Einstellungen“ ändern.

## Wartung

| Aufgabe | Befehl (im Ordner `server/`) |
|---|---|
| Update nach `git pull` | `docker compose up -d --build` |
| Logs ansehen | `docker compose logs -f api` |
| Neuen Key eintragen | `.env` bearbeiten, dann `docker compose up -d` |
| Stoppen | `docker compose down` |

Die Container starten nach einem Neustart des Servers automatisch wieder
(`restart: unless-stopped`).

## Lokal testen (ohne Hetzner)

```bash
cd server
mvn package
HYPIXEL_API_KEY=dein-dev-key java -jar target/hypixeltracker-server.jar
# in der App unter Einstellungen: http://localhost:8080
```

## Einstellungen (`.env`)

| Variable | Standard | Bedeutung |
|---|---|---|
| `HYPIXEL_API_KEY` | – (Pflicht) | Key deiner Hypixel-Application |
| `DOMAIN` | – | Domain für HTTPS über Caddy |
| `CACHE_TTL_SECONDS` | `300` | Wie lange Profile zwischengespeichert werden |
| `CLIENT_REQUESTS_PER_MINUTE` | `30` | Max. Anfragen pro Minute und Nutzer-IP |
| `PORT` | `8080` | Interner Port (normalerweise nicht ändern) |
