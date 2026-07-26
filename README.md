# MAA Invest — Android App (Kotlin)

A native Android app for **MAA Invest**, built with **Kotlin + Jetpack
Compose** — no Expo, no React Native, no cross-platform framework. It
matches the look of the provided mockup (deep-green branding, wallet,
savings goals, chamas/groups, investments) and talks directly to your
existing PHP REST API at `invest.maawebhost.co.ke/api`.

This exists as an alternative to the Expo/React Native version of the
app: that version needs Expo's cloud build service (EAS) to reliably
handle Android's native-module build-tool matrix. This version is a
plain Android project, so it builds with a simple `gradle assembleDebug`
— the same kind of GitHub Actions workflow that works for any
straightforward native Android app, no extra accounts needed.

## What's included

- Auth: Welcome/splash, Login, Register, local 4-digit PIN lock
- Home dashboard: portfolio value, wallet balance, active goals
- Wallet: balance, Add Money (M-Pesa/Bank/Card), Withdraw, transaction history
- Save: savings goals list + create goal
- Groups: browse groups, join by invite code, group detail with contribute
- Invest: browse investment products, buy flow (with premium-plan gating)
- Profile & More: account info, KYC status, logout

All screens call the real endpoints documented in your backend's
`api/API_REFERENCE.md` — nothing here is mocked data.

## 1. Point the app at your backend

Edit `app/src/main/java/co/ke/maawebhost/invest/data/api/ApiClient.kt`:

```kotlin
const val API_BASE_URL = "https://invest.maawebhost.co.ke/api/"
```

Make sure your backend's `database/migration_api_tokens.sql` has been run
and the `api/` folder is deployed, per your existing README.

## 2. Push this to GitHub — GitHub builds the installable app for you

This repo includes `.github/workflows/build-android.yml`, which builds a
real Android `.apk` automatically on every push to `main`, entirely on
GitHub's own servers. **No Expo account, no token, nothing to sign up
for.**

```bash
git init
git add .
git commit -m "Initial commit: MAA Invest Android app"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

No local git/shell? Use GitHub's web UI instead: create the repo, then
**Add file → Upload files**, drag in everything from this folder
(including the `.github` folder — if your file picker hides dot-folders,
create `.github/workflows/build-android.yml` separately via **Add file →
Create new file** and paste in its contents, GitHub will make the
folders for you from the slashes in the filename).

**That's it — push, then:**

1. Go to the **Actions** tab on your repo → you'll see the build running
   (a few minutes)
2. When it's green, go to the **Releases** section (right sidebar of the
   repo homepage, or `github.com/<you>/<repo>/releases`)
3. Download `maa-invest-app.apk` from the latest release
4. Drop it into your website's `downloads/maa-invest-app.apk` — the
   QR-code install flow from your site picks it up automatically

Every future push to `main` produces a new release the same way.

> This build is a debug-signed APK — fine for direct-download/QR
> install, but not accepted by the Play Store (which needs a
> release-signed build with your own signing key). That's a later step,
> once you're ready to publish there — ask if you want help setting up
> release signing and a Play Store-ready workflow.

## 3. Working on it locally (optional)

If you or someone else ever gets access to a machine with Android
Studio: open this folder as a project, let Gradle sync (Android Studio
generates the wrapper automatically), and run on an emulator or device
like any other Android project. No special setup beyond normal Android
development.

## Project structure

```
app/src/main/java/co/ke/maawebhost/invest/
  MainActivity.kt              — entry point
  ui/theme/                    — Compose colors & typography (sampled from mockup)
  data/
    api/                       — Retrofit service, client, DTOs matching your backend
    Session.kt                 — current-user / auth state
    TokenStore.kt               — token + PIN storage
  components/                  — reusable buttons, cards, inputs
  nav/NavGraph.kt               — auth stack, bottom tabs, screen routing
  screens/
    auth/                      — Welcome, Login, Register, PIN
    home/                      — Dashboard
    wallet/                    — Wallet, Add Money, Withdraw
    save/                      — Goals list, Create goal
    groups/                    — Groups list, Group detail, Join by code
    invest/                    — Investment products
    history/                   — Transaction history
    profile/                   — Profile, More menu
```

## Notes / things to wire up next

- **Group activity feed**: the backend doesn't yet expose a per-group
  transaction/activity endpoint, so the group detail screen's "Recent
  Activity" is a placeholder.
- **Group withdraw / loans / members**: show a "coming soon" toast since
  those aren't in the current REST API yet.
- **PIN lock** is local-only (device-side SharedPreferences), not tied to
  the backend — it's a re-entry gate, not a second auth factor recognized
  by the server. Consider `EncryptedSharedPreferences` (androidx.security)
  instead of plain SharedPreferences before shipping to real users.
- Wallet deposits/withdrawals go through the real M-Pesa/Pesapal/KCB flow
  your backend already has wired up (STK push etc.), so they return
  `pending` status until the webhook confirms — the app surfaces that as
  a toast rather than an instant balance update.
- This is Android-only. An iPhone version would need a separate Swift
  codebase — there's no shared-codebase option once you're off a
  cross-platform framework like Expo/React Native or Flutter.
