# tv-shortcuts

Remap the dedicated app buttons on a Sony Bravia (Google TV) remote — Disney+,
Crunchyroll, etc. — to open any app you like, **without** an accessibility
service (so volume keys etc. are left alone).

Developed and tested on a Bravia A95L (Android 14 / Google TV). Other Sony
Google TV models probably behave the same, but that's unverified.

## How it works

Those remote keys aren't handled by the apps themselves. On the A95L:

1. The key arrives as `KEYCODE_VIDEO_APP_8` with a per-button **scancode**
   (490 Prime Video, 491 Disney+, 507 Crunchyroll, …).
2. Sony's `com.sony.dtv.sonysystemservice` looks the scancode up in its bundled
   `partner_keys.xml` to get a hard-coded package name, and checks that a
   package with that name is **installed and enabled**. If not, you get a
   "not installed" toast.
3. Otherwise it hands the launch to Google's launcher, which opens that
   package's leanback launcher activity.

Nothing in that path checks who signed the package. So we install a tiny
**stand-in** APK that claims the expected package name and, when launched,
forwards to whatever app you chose.

| Component | Package | Role |
|---|---|---|
| `hub` | `dev.jonathanclark.tvshortcuts.hub` | Leanback app where you pick an app per button; exposes the choice via a ContentProvider |
| `standin` (flavor `disney`) | `com.disney.disneyplus` | Stand-in for scancode 491 |
| `standin` (flavor `crunchyroll`) | `com.crunchyroll.crunchyroid` | Stand-in for scancode 507 |
| `standin` (flavor `prime`) | `com.amazon.amazonvideo.livingroom` | Stand-in for scancode 490 — **doesn't work, see below** |

Changing a button later is just a matter of opening the hub and picking a
different app; no rebuild or reinstall.

## Limitations

- **Only works if the real app isn't installed.** Android refuses to install a
  package whose name matches an existing one signed with a different key. Fine
  for Disney+/Crunchyroll if you never installed them; if you did, uninstall
  them first.
- **Prime Video doesn't work.** It ships as a signed system app
  (`/product/app/AmazonVideo`). Even after `pm uninstall --user 0`, Android
  keeps the package record and rejects our APK with
  `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. The flavor is left in the tree in case
  that changes (e.g. on a rooted/modified TV).
- Relies on Sony's firmware behaviour as observed; a TV update could change it.
- Debug-signed APKs only so far.

## Install

You need adb access to the TV (Developer options → Wireless debugging; note
that on this TV it **won't stay enabled on Ethernet**, use Wi-Fi). Pair with
`adb pair`, then `adb connect`.

```sh
./gradlew assembleDebug
adb install -r hub/build/outputs/apk/debug/hub-debug.apk
adb install -r standin/build/outputs/apk/disney/debug/standin-disney-debug.apk
adb install -r standin/build/outputs/apk/crunchyroll/debug/standin-crunchyroll-debug.apk
```

Then open **TV Shortcuts** from the home row (or press a mapped button, which
opens it while nothing is set) and choose an app for each button.

To undo: `adb uninstall com.disney.disneyplus` (and the others).

## Building

Needs JDK 17+ (21 recommended) and the Android SDK (platform 34, build-tools
34.0.0) with `sdk.dir` in `local.properties`. Then `./gradlew assembleDebug`;
the stand-in flavors are `disney`, `crunchyroll`, `prime`.

**aarch64 Linux note:** Google only ships `aapt2` for x86-64, so the Android
Gradle Plugin can't run natively there. This project was built on an x86-64
machine. On Asahi, `aapt2` itself does run under FEX inside a `muvm` microVM
(plain FEX fails on 16K pages), but I did not get a full Gradle build working
that way.

## Layout

```
hub/       settings UI + TargetProvider (content://<hub>.targets/<buttonId>)
standin/   invisible trampoline activity, built once per button via flavors
```
