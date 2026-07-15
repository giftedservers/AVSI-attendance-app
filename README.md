# Attendance App (Android)

Native Android attendance app for the HRMS platform — geofenced clock
in/out for every organisation hosted on `https://hrm.olkazi.com/api/`. One
app serves every tenant; which organisation someone belongs to is
determined by their login, not by the build.

## Pushing this to GitHub

```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

That's it — no secrets or keys need to be added to the repo itself. See
below for what's deliberately excluded.

## Automatic builds (GitHub Actions)

`.github/workflows/main.yml` builds a debug APK automatically on every push
to `main`/`master`, and can also be triggered manually from the Actions tab
("Run workflow"). No setup needed on your end — the workflow installs its
own JDK, Android SDK, and Gradle each run.

After a build finishes, download the APK from that run's **Artifacts**
section (named `attendance-app-debug-apk`).

## What's intentionally not committed (see `.gitignore`)

- `gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar` — the CI
  workflow generates these fresh via `gradle wrapper` on every run, so
  there's no wrapper JAR binary sitting in the repo.
- `local.properties` — your own machine's Android SDK path; never the same
  between two developers.
- `*.jks` / `*.keystore` — signing keys. If you later add release signing,
  keep the keystore itself out of the repo and pass its contents through a
  GitHub Actions secret instead.
- Standard build output (`build/`, `*.apk`, `*.aab`) and IDE files (`.idea/`,
  `*.iml`).

## Building locally instead

Requires Android Studio (or a local JDK 17 + Android SDK) — Gradle will
generate its own wrapper the first time you open the project or run:

```bash
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/hrms/attendance/
│   ├── MainActivity.kt
│   ├── data/LocationHelper.kt          # geofence / location logic
│   └── network/
│       ├── ApiClient.kt                # Retrofit client setup
│       ├── ApiModels.kt                # request/response DTOs
│       └── AttendanceApi.kt            # API endpoint interface
└── res/                                # icons, colors, strings, themes
```

The API base URL (`https://hrm.olkazi.com/api/`) is the shared backend
every tenant's app instance talks to — it's set in `app/build.gradle.kts`
as a `BuildConfig` field, not hardcoded inline in source, so it's easy to
point at a different environment (e.g. a staging server) if you ever need
to.
