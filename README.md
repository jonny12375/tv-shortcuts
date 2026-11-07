# tv-shortcuts

Remap the dedicated app buttons on a Sony Bravia (Google TV) remote, such as
Disney+ and Crunchyroll, to open any app you like. It doesn't use an
accessibility service, so nothing else on the remote is affected.

Tested on a Bravia A95L (Android 14 / Google TV).

> [!IMPORTANT]
> **Some buttons can't be remapped.** Android refuses to install an app whose
> package name belongs to a system app signed with a different key, and that
> includes system apps that have been uninstalled for your user. On the A95L
> this affects **Netflix, YouTube, Prime Video, BRAVIA CORE and Sony Select**
> (plus Google Play, which isn't included here). The hub shows these as
> `[X] system app` so you know up front. Which apps are system apps varies by
> model and region; the hub checks your actual TV.
>
> The same applies to any app you installed yourself: uninstall the real app
> first (the hub can start that for you), then install the stand-in.

## How it works

Those remote keys aren't handled by the apps themselves. Sony's system service
maps each button to a hard-coded package name, and just checks that a package
with that name is installed and enabled, then asks the launcher to open it. It
doesn't check who signed it.

So for each button this project builds a tiny **stand-in** APK that claims the
expected package name. When launched, the stand-in asks the **hub** app which
app you chose for that button, starts it, and finishes. Changing a button later
only means picking a different app in the hub; no rebuild or reinstall.

## Supported buttons

The list lives in [`config/buttons.json`](config/buttons.json) and drives both
the stand-in builds and the hub, so adding a button is one entry.

| Button | Package the TV expects |
|---|---|
| Netflix | `com.netflix.ninja` |
| YouTube / YouTube Music | `com.google.android.youtube.tv` |
| Prime Video | `com.amazon.amazonvideo.livingroom` |
| Disney+ | `com.disney.disneyplus` |
| Disney+ (Middle East & Africa) | `com.disneyplus.mea` |
| Crunchyroll | `com.crunchyroll.crunchyroid` |
| BRAVIA CORE | `com.sonypicturescore` |
| Sony Select | `com.sony.dtv.sonyselect` |
| My BRAVIA | `jp.sony.mybravia` |
| Hulu (Japan) | `jp.happyon.android` |
| ABEMA | `tv.abema` |
| U-NEXT | `jp.unext.tv.player` |
| TSUTAYA TV | `jp.tmediahd.android.tsutaya_tv_atv` |
| myVideo (Taiwan) | `com.taiwanmobile.myVideotv` |
| Disney+ Hotstar | `in.startv.hotstar` |
| SonyLIV | `com.sonyliv` |
| FOD | `air.jp.co.fujitv.fodviewer.tv` |
| VTV Go | `vn.vtv.vtvgotv` |
| FPT Play | `net.fptplay.ottbox` |
| TVer | `jp.co.tver.tvapp` |

You only install the stand-ins for buttons your remote actually has.

## Install

Enable Developer options on the TV and turn on Wireless debugging (use Wi-Fi,
not Ethernet), then pair and connect with `adb pair` / `adb connect`.

```sh
./gradlew assembleDebug

# the hub, then one stand-in per button you want to remap
adb install -r hub/build/outputs/apk/debug/hub-debug.apk
adb install -r standin/build/outputs/apk/disney/debug/standin-disney-debug.apk
adb install -r standin/build/outputs/apk/crunchyroll/debug/standin-crunchyroll-debug.apk
```

Open **TV Shortcuts** from the home row. It sorts every button by who owns the
package name the TV looks for:

- **Shortcuts**: a stand-in is installed. Pick the app it should open, or remove
  the shortcut (which uninstalls the stand-in).
- **Not set up**: no stand-in yet. Install one from here, or, if the real app is
  in the way, start its uninstall.
- **Can't be changed**: system apps that can't be replaced.

The hub carries every stand-in APK, so you can also install them from the hub
instead of `adb`. Android asks you to allow installs from TV Shortcuts the first
time, and Google Play Protect may block each one as coming from an unknown
developer. Choose **More details**, then **Install anyway**, or use `adb`.

To undo a button, remove its shortcut in the hub, or run
`adb uninstall com.disney.disneyplus`.

## Adding an app

This only remaps the remote's dedicated **app buttons**, not any other buttons.
If your remote has an app button that isn't in the table above:

1. **Find the package name the TV launches.** On a Sony TV the list is in the
   system service's `res/xml/partner_keys.xml`: pull
   `/system_ext/priv-app/SonySystemService/SonySystemService.apk` with `adb` and
   decode it (for example with `apktool` or `jadx`). Each `<PartnerKey>` entry
   names the package that button launches. A few buttons, such as Netflix and
   YouTube, are handled separately with a fixed package and, for Netflix, an
   intent action.
2. **Add an entry to [`config/buttons.json`](config/buttons.json):** `id`,
   `label`, a short `mark` for the icon, and `package`. Add `actions` if the
   firmware launches the app with a specific intent action (as it does for
   Netflix), and a large `versionCode` if the package is a system app.
3. Run `python3 tools/generate_icons.py`, then `./gradlew assembleDebug`.

Pull requests with new entries are welcome.

## Compatibility

Tested on a Sony Bravia A95L (Google TV, Android 14).

The approach should work on any TV whose app buttons launch a fixed package name
and only check that the package is installed, which is how the A95L behaves.
Part of that path is Google's own TV launcher code, which other Google TV
devices share. Other manufacturers may do it differently, for example by
verifying the app's signature or launching a specific component, in which case
the stand-ins won't be accepted. The hub works out on the device whether each
package is a stand-in, a regular app or a system app, so it will show you what
applies on your TV.

If you try it on another TV, please open an issue or pull request with what
happened.

## Building

Requires JDK 17+ and the Android SDK (platform 34, build-tools 34.0.0), with
`sdk.dir` set in `local.properties`.

```sh
./gradlew assembleDebug                 # hub + every stand-in
./gradlew :standin:assembleDisneyDebug  # a single stand-in
```

## Layout

```
config/buttons.json   the button list (single source of truth)
hub/                  settings UI + a ContentProvider the stand-ins query
standin/              invisible trampoline activity; one product flavor per button
tools/                generate_icons.py: makes the icons and banners
```

The icons and banners are generated from `config/buttons.json` (each entry has a
short `mark`) and committed, so a normal build doesn't need Python. After adding
or editing a button, rerun `python3 tools/generate_icons.py` (needs Pillow).
