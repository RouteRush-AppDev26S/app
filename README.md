# Route Rush

**Route Rush** ist eine Karten- und Tracking-App, die Bewegung spielerisch macht: echte Standortdaten treffen auf Gamification, Social Features und smarte Datenauswertung — damit Laufen, Gehen und Erkunden nicht nur Fortschritt bringt, sondern auch Spaß macht.

---

## Tech Stack

### Backend

| Bereich               | Technologie             |
| --------------------- | ----------------------- |
| Backend               | Spring Boot             |
| Persistenz            | Spring Data JPA         |
| Datenbank-Migrationen | Flyway                  |
| Containerisierung     | Docker / Docker Compose |
| Datenbank             | PostgreSQL              |

### Frontend

| Bereich                | Technologie                   |
| ---------------------- | ----------------------------- |
| UI                     | Jetpack Compose / Material 3  |
| Lokale Persistenz      | Room, DataStore               |
| Netzwerk               | Retrofit, OkHttp              |
| Echtzeit-Kommunikation | STOMP over WebSocket          |
| Karten                 | MapLibre                      |
| Standort               | Google Play Services Location |

---

## Team

| Name                  | Bereich         |
| --------------------- | --------------- |
| Hans Wornik           | Basic Features  |
| Moritz Gutschi        | Gamification    |
| Klemens Wibmer        | Datenauswertung |
| Thomas Jacques Currie | Social Features |

---

## Features

### Basic Features — _Hans Wornik_

- Standort-Tracking in Echtzeit
- Suche nach Orten und Adressen
- Eigene Marker/Pins mit Notizen setzen
- Routenberechnung von A nach B
- Rundkurs-Routen
- Routen-Generierung anhand einer Ziel-Schrittanzahl
- Offline-Kartenunterstützung

### Gamification — _Moritz Gutschi_

- Weekly Challenges (z. B. 50.000 Schritte/Woche)
- Achievements
- XP- und Level-System
- Leaderboard
- Persönliche Bestleistungen

### Datenauswertung — _Klemens Wibmer_

- Heatmap der eigenen Bewegungen
- Statistik-Dashboard
- Wochen-/Monatsvergleiche
- Persönliche Trends & Ziel-Fortschritt
- Wetter-Overlay für die aktuelle Position

### Social Features — _Thomas Jacques Currie_

- Registrierung & Login
- Profilverwaltung
- Standort mit Freunden teilen
- Gruppenchats
- Routen teilen
- Pins mit Notiz teilen
