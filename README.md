<div align="center">

# Crystal

**A clear, private alarm clock for Android.**

A beautiful glass-themed alarm app with a smooth analog clock face, countdown timer, stopwatch, puzzle dismiss, volume ramp, and Material You theming — all stored locally on your phone with no internet access.

[![License: AGPL-3.0-only](https://img.shields.io/badge/license-AGPL--3.0--only-34A853?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](#install)
[![Internet](https://img.shields.io/badge/internet%20permission-none-2e8b57?style=flat-square)](PRIVACY.md)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](#tech-stack)

[Features](#features) · [How it works](#how-it-works) · [Install](#install) · [Permissions](#permissions) · [Documentation](docs/README.md)

</div>

---

## Features

### ⏰ Alarms

| | |
|---|---|
| **Set and forget** | Create one-shot or repeating alarms (pick any combination of days). |
| **Sound picker** | Choose from system alarm, notification or ringtone sounds. |
| **Volume ramp** | Optional — starts quiet and grows louder over 15 seconds, so you wake gently. |
| **Puzzle dismiss** | Optional — solve a simple math problem before the alarm stops. Keeps you from turning it off half-asleep. |
| **Snooze** | 1, 3, 5, 10, 15 or 30 minute snooze. |
| **Smart snooze** *(planned)* | Flip your phone over or pick it up to auto-snooze. |
| **Fullscreen alert** | The alarm opens a bright, full-screen activity even when the phone is locked. |

### 🕐 Timer

- **Pomodoro**, **Short break** and **Long break** presets, plus a custom timer.
- Circular countdown ring that shrinks as time runs out.
- Pause, resume and add 1 minute on the fly.

### ⏱ Stopwatch

- **Lap timing** — tap Lap to record splits.
- Millisecond precision, running at 20 fps.
- List of all laps, newest first.

### 🧊 Clock

- Hand-drawn analog clock face with a glass/glossy aesthetic.
- Radial gradient background (adapts to dark/light theme).
- Specular highlight sweep animates slowly around the face.
- Smooth second hand with counterweight tail.
- Digital time overlay.

### ⚙️ Settings

- One-tap links to disable battery optimisation, adjust notification settings, and grant exact alarm permission (Android 14+).

### 🎨 Theme

- **Material You** — automatically picks up your wallpaper colours on Android 12+.
- Custom light and dark colour schemes with a crystal accent palette.

## How it works

```
You add an alarm → saved as JSON in SharedPreferences
                → AlarmScheduler sets an exact alarm via AlarmManager
                → at the right time, AlarmReceiver fires
                → AlarmForegroundService starts (plays sound + vibrates)
                → AlarmAlertActivity opens fullscreen

Timer and stopwatch run inside the app — no services needed.
```

### Data storage

Everything is stored in your phone's private storage:
- **Alarms** → JSON array in `SharedPreferences`
- **Timer/stopwatch** → in-memory (not persisted)

Nothing is uploaded anywhere. The app declares no internet permission.

## Permissions

| Permission | Why |
|---|---|
| `SCHEDULE_EXACT_ALARM` | Fire alarms at the exact time you set (Android 12+) |
| `VIBRATE` | Vibrate when the alarm rings |
| `FOREGROUND_SERVICE` | Run the alarm sound in a foreground service |
| `POST_NOTIFICATIONS` | Show the alarm notification (Android 13+) |
| `USE_EXACT_ALARM` | Schedule exact alarms (Android 12+) |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule alarms after a reboot |

## Install

Build the app yourself with Android SDK 34 and JDK 17:

```bash
cd me2008/alarm
./build.sh debug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

See the [building guide](docs/guides/building.md) for details.

## Tech stack

| | |
|---|---|
| Language | **Kotlin** 1.9.24 |
| UI | **Jetpack Compose** with Material 3 |
| Compose BOM | 2024.06.00 |
| Kotlin compiler extension | 1.5.14 |
| Min / target SDK | 26 / 34 |
| Build system | Gradle 8.7 + AGP 8.5.2 |
| Storage | SharedPreferences (JSON) |
| Internet | None |

## Documentation

- [Documentation index](docs/README.md)
- [Architecture overview](docs/architecture/overview.md)
- [Building and installing](docs/guides/building.md)
- [Feature guide](docs/guides/features.md)

## License

AGPL-3.0-only. See [LICENSE](LICENSE).
