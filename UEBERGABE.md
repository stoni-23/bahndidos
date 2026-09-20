# Übergabe — Bahndidos Eco Jump & Dash Assets v1

**Für:** Thron (Physics/Engine), Kacki (Integration)  
**Von:** Art pipeline (Pillow + Daniel-Refs chroma-key)  
**Datum:** 2026-09-20  

## Prinzip

- **Player / Opa / Oma:** Daniel-Referenzsprites → Green-/Blackscreen entfernt → true RGBA → nearest-neighbor auf Game-Höhe.
- **Parallax BG, Items, Taube, Props:** Procedural 16-bit SNK/Neo-Geo (Pillow), konsistente Palette.
- Player **schaut nach RECHTS** (Side-Scroller). Opa-Walk ebenfalls RIGHT.

## Parallax (`assets/bg/`)

| Datei | Größe | Scroll |
|-------|-------|--------|
| `bg_sky.png` | 512×192 | 0.2× |
| `bg_city_far.png` | 512×192 | 0.5× |
| `bg_park_mid.png` | 640×256 | 1.0× |
| `bg_ground.png` | 640×256 | 1.8× (transparent oberhalb Bodenstreifen) |

Siehe auch `assets/bg/README.md`.

## Player (`assets/player/`)

| Datei | Größe | Notes |
|-------|-------|-------|
| `bahndidos_scooter.png` | ~128h | Zwei Bahndidos auf rotem E-Scooter, Side, **facing RIGHT** |
| `bahndidos_scooter_160.png` | ~160h | Größere Variante |

Jump/Duck/Salto-Frames: noch keine Refs → bewusst weggelassen (keine Stick-Figures).

## Enemies (`assets/enemies/`)

| Datei | Größe | Facing / Pose |
|-------|-------|---------------|
| `opa_walk.png` | ~128h | Side walk + Stock, **RIGHT** (Primary für Gameplay) |
| `opa_idle.png` | ~128h | Frontal idle |
| `opa_threat.png` | ~128h | Frontal Faust drohend |
| `oma.png` | ~128h | Frontal, Handtasche + Brille (stehendes Hindernis) |
| `pigeon_1.png` / `pigeon_2.png` | 40×28 | 2-Frame Flug, facing RIGHT |
| `taube.png` | = pigeon_1 | Alias |

## Items (`assets/items/`)

| Datei | Größe | Wirkung (Konzept) |
|-------|-------|-------------------|
| `powerbank.png` | 48×48 | Energy |
| `bier.png` | 40×40 | Turbo |
| `kaffee.png` | 40×40 | Turbo |
| `wlan.png` | 40×40 | Shield-Hotspot |
| `ticket.png` | 48×48 | Zone-C Ticket |
| `hamster.png` | 40×40 | Freeze-Battery (optional) |
| `prop_bench.png` | 64×32 | Deko |
| `prop_trash.png` | 40×40 | Deko |

## Preview

- `/workspace/bahndidos/preview/proof_assets_v1.png`
- Script: `_scripts/build_bahndidos_assets_v1.py`
- Rebuild: `/workspace/.venv-art/bin/python _scripts/build_bahndidos_assets_v1.py`

## Dateiliste

- `app/src/main/assets/player/bahndidos_scooter.png`
- `app/src/main/assets/player/bahndidos_scooter_160.png`
- `app/src/main/assets/enemies/opa_walk.png`
- `app/src/main/assets/enemies/opa_walk_96.png`
- `app/src/main/assets/enemies/opa_idle.png`
- `app/src/main/assets/enemies/opa_threat.png`
- `app/src/main/assets/enemies/oma.png`
- `app/src/main/assets/bg/bg_sky.png`
- `app/src/main/assets/bg/bg_city_far.png`
- `app/src/main/assets/bg/bg_park_mid.png`
- `app/src/main/assets/bg/bg_ground.png`
- `app/src/main/assets/bg/README.md`
- `app/src/main/assets/items/powerbank.png`
- `app/src/main/assets/items/bier.png`
- `app/src/main/assets/items/kaffee.png`
- `app/src/main/assets/items/wlan.png`
- `app/src/main/assets/items/ticket.png`
- `app/src/main/assets/items/hamster.png`
- `app/src/main/assets/items/prop_bench.png`
- `app/src/main/assets/items/prop_trash.png`
- `app/src/main/assets/enemies/pigeon_1.png`
- `app/src/main/assets/enemies/pigeon_2.png`
- `app/src/main/assets/enemies/taube.png`
- `preview/proof_assets_v1.png`
