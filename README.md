# tv-shortcuts

Remap the dedicated app buttons on a Sony Bravia (Google TV) remote, such as
Disney+ and Crunchyroll, to open any app you like. It doesn't use an
accessibility service, so nothing else on the remote is affected.

Tested on my Sony Bravia A95L (Google TV, Android 14).

> [!IMPORTANT]
> **Some buttons can't be remapped.** Android refuses to install an app whose
> package name belongs to a system app signed with a different key, and that
> includes system apps that have been uninstalled for your user. On my A95L this
> affects **Netflix, YouTube, Prime Video, Bravia Core and Sony Select** (plus
> Google Play, which isn't included here). Which apps are system apps will
> differ between models and regions. The hub checks your actual TV and lists
> these buttons under **Can't be changed**.
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
only means picking a different app in the hub; no rebuild or reinstall. If you
press a button that has no app chosen yet, the hub opens straight on that
button's app picker.

The stand-ins appear in the Google TV apps list like any other app. As far as I
can tell the launcher has no way to hide an app, so that's likely not possible;
you can only move them (long-press an app, then **Move**). They show up at the
end of the list.

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
| Bravia Core | `com.sonypicturescore` |
| Sony Select | `com.sony.dtv.sonyselect` |
| My Bravia | `jp.sony.mybravia` |
| Hulu (Japan) | `jp.happyon.android` |
| Abema | `tv.abema` |
| U-Next | `jp.unext.tv.player` |
| Tsutaya TV | `jp.tmediahd.android.tsutaya_tv_atv` |
| myVideo (Taiwan) | `com.taiwanmobile.myVideotv` |
| Disney+ Hotstar | `in.startv.hotstar` |
| SonyLIV | `com.sonyliv` |
| FOD | `air.jp.co.fujitv.fodviewer.tv` |
| VTV Go | `vn.vtv.vtvgotv` |
| FPT Play | `net.fptplay.ottbox` |
| TVer | `jp.co.tver.tvapp` |

You only install the stand-ins for buttons your remote actually has.

## Install

Download the signed hub APK from the
[Releases](https://github.com/jonny12375/tv-shortcuts/releases) page (or build it
yourself, see [Building](#building) and [Building a release](#building-a-release))
and get it onto your TV. You can do that either way:

- **With adb.** Turn on Developer options on the TV, then Wireless debugging (on
  some systems it's called network debugging), and use `adb pair`,
  `adb connect` and `adb install -r` on the APK. I had to disconnect Ethernet
  and use Wi-Fi for wireless debugging to work.
- **With a file manager.** Copy the APK to the TV, for example with a file
  manager app, and open it there. Android will ask you to allow installs from
  that app.

Then open **TV Shortcuts** from the apps list. It sorts every button by who
owns the package name the TV looks for:

- **Shortcuts**: a stand-in is installed. Pick the app it should open, or remove
  the shortcut (which uninstalls the stand-in).
- **Not set up**: no stand-in yet. Install one from here, or, if the real app is
  in the way, start its uninstall.
- **Can't be changed**: system apps that can't be replaced.

The hub carries every stand-in APK and installs them for you, so there's nothing
else to build or copy over. Android asks you to allow installs from TV
Shortcuts the first time, and Google Play Protect may block each stand-in as
coming from an unknown developer. Choose **More details**, then **Install
anyway**.

**About** (top right) shows the version, a QR code for this page, and a debug
info screen with the details worth including in a bug report.

To undo a button, remove its shortcut in the hub.

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
   `label`, a one or two character `mark` for the icon, and `package`. Add
   `actions` if the firmware launches the app with a specific intent action (as
   it does for Netflix), and a large `versionCode` if the package is a system
   app.
3. Run `uv run tools/generate_icons.py`, then `./gradlew assembleDebug`.

Pull requests with new entries are welcome.

## Compatibility

Tested on my Sony Bravia A95L (Google TV, Android 14).

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

## Building a release

You only need this if you want to build and sign TV Shortcuts yourself. The
[Releases](https://github.com/jonny12375/tv-shortcuts/releases) page has signed
builds ready to install.

> [!WARNING]
> **Everything has to be signed with the same key.** The hub recognises a stand-in
> by comparing signing certificates, and Android won't update an app that was
> signed with a different key. So don't mix APKs from different builds: a
> stand-in from one build next to the hub from another shows up as a regular app
> instead of a stand-in. If you switch between the official release and your own
> build, uninstall the hub and every stand-in first. Stand-ins installed from
> inside a hub always match that hub, so this only comes up when mixing builds.

Release builds are signed with your own key, and the hub and every stand-in use
the same one.

1. Create a key once, keep it **outside** the repo and back it up. Losing it
   means existing installs can't be updated in place.
   ```sh
   keytool -genkeypair -keystore ~/tv-shortcuts-signing/release.jks \
     -alias tv-shortcuts -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Copy `keystore.properties.example` to `keystore.properties` (gitignored) and
   fill it in.
3. Run `./gradlew assembleRelease`. The hub APK in
   `hub/build/outputs/apk/release/` already contains every stand-in, signed with
   the same key, so it's the only file to distribute.

Without a `keystore.properties`, `assembleRelease` falls back to the debug key
and prints a warning; those builds aren't real releases.

### Versions

The release version is `appVersion` in `gradle.properties`, as semver
(`MAJOR.MINOR.PATCH`). The hub and every stand-in use it as their version name,
and Android's version code is derived from it (`1.2.3` becomes `10203`), so that's
the only place to bump. Tag releases as `v<appVersion>`, for example `v1.0.0`.

### Stand-in versions

Separately from the release version, every stand-in carries a compatibility version (`dev.jonathanclark.tvshortcuts.STANDIN_VERSION`
in its manifest), and the hub knows which version it expects. If a change means
the hub needs newer stand-ins, bump `standinVersion` in `gradle.properties`. The
hub reads each installed stand-in's version, and the debug info screen shows
them.

## Layout

```
config/buttons.json   the button list (single source of truth)
hub/                  settings UI + a ContentProvider the stand-ins query
standin/              invisible trampoline activity; one product flavor per button
tools/                generate_icons.py: makes the icons and banners
```

The icons and banners are generated from `config/buttons.json` (each entry has a
short `mark`) and committed, so a normal build doesn't need Python. After adding
or editing a button, rerun `uv run tools/generate_icons.py`. It uses
[uv](https://docs.astral.sh/uv/), which installs Pillow for you.
