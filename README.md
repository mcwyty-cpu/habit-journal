# Habit Journal (Android)

A 90-day habit journal with daily reminders built in.

- **Journal**: tick each habit ✓ or ✗, write what actually happened, see your score.
- **Progress**: 90-day grid, average score, streak, and completion % for each habit.
- **Reminders**: 17 daily reminders already set (7:00 AM wake-up to 11:00 PM lights out). Change times, edit text, switch any on or off, or add your own.
- **Setup**: edit habits, start date and motto; copy or restore a backup.

Everything is saved on the phone. Reminders keep working after a restart.

## Option A: let GitHub build the app (no software to install)

1. Create a free account at github.com if you don't have one.
2. Click **+** (top right) → **New repository**. Name it `habit-journal`, choose **Public**, and click **Create repository**.
3. On the new repository page, click **uploading an existing file**.
4. Unzip `HabitJournal.zip` on your laptop. Open the `HabitJournal` folder, select **everything inside it** (including the `.github` folder), and drag it all into the upload area. Click **Commit changes**.
   - If you can't see `.github` on Windows: in File Explorer, click View → Show → Hidden items.
5. Click the **Actions** tab. A build named "Build APK" starts on its own and takes about 3–5 minutes. Wait for the green tick.
6. On your phone, open `github.com/<your-username>/habit-journal/releases`, tap **HabitJournal.apk** to download it, then open it to install.

## Option B: build it in Android Studio

1. Install Android Studio from developer.android.com/studio.
2. **File → Open**, choose the `HabitJournal` folder, and wait for it to finish syncing.
3. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. Click **locate** in the pop-up. Copy `app-debug.apk` to your phone and open it.

## Installing on the phone

- Android will warn that the app is from an unknown source. Tap **Settings**, allow installing from Chrome or Files, go back, and tap **Install**.
- Play Protect may say it doesn't recognise the app. Tap **More details → Install anyway**. It's your own app, built from this code.
- Open the app and tap **Allow** when it asks to send notifications.
- Go to the **Reminders** tab. Each row at the top should show a green dot. Tap any button next to a red dot and allow it.
- Tap **Send a test** to check that a notification appears.

### Phones that block reminders (Tecno, Infinix, itel, Xiaomi, Oppo, Samsung)

These phones close apps in the background to save battery. If reminders don't arrive:
- Long-press the app icon → **App info** → **Battery** → choose **Unrestricted** / **No restrictions**.
- Turn on **Auto-start** / **Allow background activity** if your phone has it.
- Lock the app in the recent-apps screen (pull it down or tap the lock icon).

## Moving your entries from the web version

In the web version: Setup → **Copy backup**. Send it to yourself (WhatsApp "message yourself" works). In the app: Setup → paste it into the Backup box → **Restore pasted backup** → **Replace**.
