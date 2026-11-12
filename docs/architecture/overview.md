# Architecture overview

Crystal is a single-module Android app (`:app`, package `me2008.alarm`) written in Kotlin with Jetpack Compose. It has no internet access.

## Packages

```
me2008.alarm/
├── data/          Data models and persistence
│   ├── Alarm.kt           Alarm data class + JSON conversion
│   ├── AlarmRepository.kt SharedPreferences-backed storage
│   ├── TimerSession.kt    Countdown timer state
│   └── StopwatchSession.kt Stopwatch state
├── service/       Background logic
│   ├── AlarmScheduler.kt     AlarmManager wrapper + AlarmReceiver
│   ├── AlarmForegroundService.kt  Foreground service for ringing
│   ├── BootReceiver.kt       Re-schedule after reboot
│   └── TimerManager.kt       In-app countdown timer coroutine manager
├── ui/            Compose screens
│   ├── AlarmApp.kt           Main app composable (navigation shell)
│   ├── GlassClockFace.kt     Canvas-drawn analog clock
│   ├── theme/Theme.kt        Material You + custom colour schemes
│   ├── components/           Reusable composables
│   │   ├── TimePickerDialogNew.kt  Up/down time picker
│   │   └── SoundPicker.kt         Alarm sound selector dialog
│   └── screens/
│       ├── clock/ClockScreen.kt      Wraps GlassClockFace
│       ├── alarm/AlarmListScreen.kt  List of all alarms
│       ├── alarm/AlarmEditScreen.kt  Add / edit alarm form
│       ├── timer/TimerScreen.kt      Countdown with circular ring
│       ├── stopwatch/StopwatchScreen.kt  Stopwatch with laps
│       └── settings/SettingsScreen.kt    Battery, notification, exact alarm links
└── AlarmAlertActivity.kt  Fullscreen activity when an alarm fires
```

## Runtime components

| Component | Type | Role |
|---|---|---|
| `MainActivity` | Activity | Hosts `AlarmApp` composable |
| `AlarmAlertActivity` | Activity | Fullscreen alert shown when an alarm fires (wakes screen, shows snooze/dismiss) |
| `AlarmForegroundService` | Foreground service | Plays alarm sound, vibration, shows persistent notification |
| `AlarmReceiver` | Broadcast receiver | Receives `AlarmManager` intents, starts the foreground service |
| `BootReceiver` | Broadcast receiver | Re-schedules alarms after `ACTION_BOOT_COMPLETED` |

## Data flow for one alarm

1. User creates an alarm → saved via `AlarmRepository` (JSON in SharedPreferences)
2. `AlarmScheduler.schedule()` calls `AlarmManager.setExactAndAllowWhileIdle()`
3. At the trigger time, `AlarmReceiver` receives the intent
4. `AlarmReceiver` calls `AlarmForegroundService.start()` with the alarm ID
5. `AlarmForegroundService` plays sound (with optional volume ramp), vibrates, opens `AlarmAlertActivity`
6. User snoozes or dismisses → service stops, alarm is rescheduled for its next repeat

## Design rules

- **No network.** The manifest declares no internet permission.
- **Local-only storage.** Alarms are in SharedPreferences, not a remote server.
- **Fail open.** If a sound can't be played, the alarm still fires with the default sound.
- **One licence header.** Every `.kt` file carries the AGPL-3.0-only header.
