# UltimateFocus

**An Android app that stops you scrolling — without taking your whole phone away.**

Most blockers are all‑or‑nothing: block Instagram, and you lose your messages too.
UltimateFocus blocks **only the part that steals your time** — Reels, Shorts, Stories,
the For You feed — and leaves the rest of the app working.

---

## 📲 Install the app (2 minutes)

The ready‑to‑install app is in the **`release`** folder:

```
release/UltimateFocus-v1.0.apk
```

**Steps:**

1. Copy `UltimateFocus-v1.0.apk` to your Android phone.
2. Open the file on the phone.
3. Android will say *"you can't install unknown apps from this source"*. Tap **Settings → Allow from this source**, then go back.
4. Tap **Install**.
5. Tap **Open**.

> ✅ Works on **Android 7.0 and newer**, on every phone type.
> One single file — there is no "wrong version for my phone".

---

## 🔑 First time you open it

The app will show one screen asking for **one permission**. This is required.
Without it, the app cannot see which app you opened, so it can never block anything.

**Steps:**

1. Tap **Open Accessibility settings**.
2. Tap **Downloaded apps**.
3. Tap **UltimateFocus**.
4. Turn the switch **on**.
5. Tap **Allow**.
6. Press back twice.

The app opens by itself. **It will never ask you again** as long as the permission stays on.

> The second permission on that screen, *Usage Access*, is **optional**. You can skip it.
> The app measures screen time on its own.

---

## ▶️ Try it in 2 minutes

A quick way to see everything working:

| # | Do this | What you should see |
|---|---------|---------------------|
| 1 | Open **Blocks** → **+ Add New Block Rule** | A list of the apps on your phone |
| 2 | Pick **Block Shorts / Reels** → choose **YouTube** | Tick **Shorts**, then tap **Save Rule** |
| 3 | Open YouTube | Opens normally ✅ |
| 4 | Tap the **Shorts** tab | UltimateFocus stops you with a full‑screen pause 🛑 |
| 5 | Press back | You return to **YouTube's home page** — not into Shorts |
| 6 | Open **Insights** | Your block is counted there |

---

## ✨ What the app can do

### Block what you choose

- **Whole app** — block an app completely.
- **Just one part** — block only Shorts, Reels, Stories, Explore, or the For You feed. The rest of the app still works.
- **Daily time limit** — "30 minutes of Reels a day". Pick any time you like, in hours and minutes.
- **Teach this screen** — if the app does not recognise a screen, you can *show* it. Open the rule, tap **Teach this screen**, then go to that screen in the app. It learns it on your phone in 20 seconds.

### Focus sessions

- Start a timer and focus.
- The timer uses the real clock, so it stays correct even if you close the app or your phone sleeps.
- **Strict Mode** locks your rules while a session runs, so you cannot switch a block off the moment it annoys you.

### The pause screen

When you open something you blocked, you get a full‑screen pause with **your own message and photo**.
Leaving it takes you back to the app's **home page**, not back into the feed.

### Insights

Focus time, screen time, blocks stopped, your streak, and a 7‑day chart.
**Every number is measured on your phone.** Nothing is fake.

---

## 💻 Run it from the source code

**You need:** [Android Studio](https://developer.android.com/studio) (Narwhal or newer) and a phone or emulator running Android 7.0+.

**Steps:**

1. Open Android Studio.
2. Choose **Open** and select this project folder.
3. Wait for Gradle to finish loading (bottom bar shows when it is done).
4. Press the green **▶ Run** button.
5. On the phone, grant the Accessibility permission as shown above.

That is all. No API keys and no extra setup are needed.

### Run the tests

```bash
./gradlew :app:testDebugUnitTest
```

### Build your own release APK

```bash
STORE_PASSWORD=<your password> KEY_PASSWORD=<your password> ./gradlew :app:assembleRelease
```

The finished file appears in `app/build/outputs/apk/release/`.

> ⚠️ Updates must be signed with the **same** keystore (`my-upload-key.jks`).
> If you lose it, phones will refuse to install your update over the old app. Keep a backup.

---

## 🧠 How it works

UltimateFocus uses Android's **Accessibility Service** to see which app is in front.
That is the only way an app can know this.

1. You open an app.
2. The service reads the app's name (for example `com.instagram.android`).
3. The **blocking engine** checks it against your rules — is it on? is it in schedule? is the time limit used up?
4. For part‑of‑app blocks, a **detector** checks whether the Reels or Shorts screen is actually on the display.
5. If it should be blocked, the pause screen appears and the attempt is saved.

**It does not read, store, or send anything on your screen.** It only checks the app against your own rules.

### Project layout

```
app/src/main/java/com/example/
├── data/
│   ├── analytics/     turns your activity into the Insights numbers
│   ├── local/         saves everything on the phone (DataStore)
│   ├── model/         the app's data types
│   └── repository/    one source of truth for all state
├── service/
│   ├── BlockingEngine        decides what gets blocked (no Android code, fully tested)
│   ├── FocusAccessibilityService  watches which app is open
│   ├── ScreenLearner         learns a screen you teach it
│   └── detection/            per‑app detectors (YouTube, Instagram, TikTok…)
├── ui/                Jetpack Compose screens
└── viewmodel/
```

**Built with:** Kotlin · Jetpack Compose · Material 3 · DataStore · Moshi · Accessibility Service API

---

## 🙋 If something does not work

| Problem | Why | Fix |
|---------|-----|-----|
| Nothing gets blocked | The permission is off | Settings → Accessibility → Downloaded apps → UltimateFocus → **on** |
| It stopped working after I reinstalled | Android removes the permission when an app is reinstalled or force‑stopped | Turn it on again |
| Reels/Stories are not blocked | The app maker changed the screen's internal name | Open the rule → **Teach this screen** |
| All the numbers show zero | It is a fresh install | They fill up as you use the app |

---

## 📌 Honest notes

We would rather tell you the limits than pretend they are not there.

- **Part‑of‑app blocking is best effort.** YouTube Shorts is tested and working. Instagram, Snapchat, TikTok, Facebook, X and Reddit use the names those apps use inside themselves, and those names change between updates. When one stops matching, **Teach this screen** fixes it on your phone in 20 seconds.
- **Detection fails safely.** If a screen is not recognised, the app stays usable. It will never block a whole app by mistake.
- **"Recovered time" is an estimate** — 3 minutes for each block stopped. Nobody can measure the scrolling that never happened, so we say clearly that it is an estimate.
- **Fresh installs start at zero.** Where there is not enough history to compare, the app says so instead of showing a made‑up percentage.

---

<div align="center">

**UltimateFocus** — block the feed, keep the app.

</div>
