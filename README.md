# Marco Update — Android

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?logo=open-source-initiative&logoColor=white)](LICENSE)

Reusable in-app update system for Android apps distributed via GitHub Releases.  
Drop in 5 minutes. Zero third-party dependencies. Free forever.

## How it works

On every launch the app silently calls the GitHub Releases API. If a newer version is found:
- A banner appears with an **Install** button
- Tapping Install downloads the APK directly from the release asset
- Android's native install dialog appears ("Vuoi installare questo aggiornamento?")
- After install the app restarts on the new version — banner gone

Falls back to opening the browser if no APK asset is attached to the release.

---

## Integration checklist (follow in this exact order)

### 1. Copy files into the project

| Source | Destination |
|--------|-------------|
| `files/UpdateChecker.kt` | `app/src/main/java/YOUR_PACKAGE/UpdateChecker.kt` |
| `files/update_btn_bg.xml` | `app/src/main/res/drawable/update_btn_bg.xml` |

Open `UpdateChecker.kt` and set:
```kotlin
private const val GITHUB_USER = "your-github-username"
private const val GITHUB_REPO = "your-repo-name"
```

### 2. Permissions — `AndroidManifest.xml`

Add before `<application>`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

### 3. FileProvider paths — `res/xml/file_provider_paths.xml`

Add inside `<paths>`:
```xml
<cache-path name="updates" path="updates/" />
```

### 4. Layout — add the two banners

Copy the XML from `snippets/layout_banner.xml` into your layout file:
- `updateBanner` → inside your confirmation/result view (hidden until update found during active session)
- `updateBannerLauncher` → on your main/instructions screen (shown when app opened from launcher)

### 5. Strings — `res/values/strings.xml`

Add from `snippets/strings_additions.xml`:
```xml
<string name="update_available">⬆ Update v%s available</string>
<string name="update_download">Install</string>
<string name="update_downloading">Downloading…</string>
<string name="update_download_error">Download failed. Tap to open in browser.</string>
```

### 6. Activity — wire up the logic

See `snippets/activity_additions.kt` for the complete code. Summary:

a. Add the `VERSION_NAME` companion constant  
b. Declare the 6 view fields  
c. Wire them in `onCreate()` after `setContentView()`  
d. Call `checkForUpdates(launcher = true/false)` at the right point in `onCreate()`  
e. Paste the 4 methods: `checkForUpdates`, `bindUpdateButton`, `downloadAndInstall`, `openUrl`  
f. Replace `YOUR_FILE_PROVIDER_AUTHORITY` with your actual authority (e.g. `com.yourapp.fileprovider`)  
g. Replace `YourActivity` with your actual Activity class name  

---

## Release workflow — ALWAYS follow this order

> **Critical:** the binary attached to a release MUST be built AFTER the version bump.  
> If you upload an old binary, users who install it will keep seeing the update banner forever.

```
1. Bump VERSION_NAME in the Activity companion object
   e.g.  const val VERSION_NAME = "1.2"

2. Build the APK
   ./gradlew assembleDebug   (or assembleRelease for production)

3. Create a GitHub Release with tag v1.2
   - Via web UI: repo → Releases → Draft a new release → tag: v1.2
   - Via API (see below)

4. Attach the freshly-built APK to the release as an asset named metadata-randomizer.apk
   (or any .apk name — UpdateChecker finds the first .apk asset automatically)

5. Push the version bump commit to main
```

### Create release via API (Claude can do this automatically)

```bash
TOKEN="your-github-pat"   # stored in macOS keychain for travelermarco

# 1. Create release
RELEASE_ID=$(curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  https://api.github.com/repos/OWNER/REPO/releases \
  -d '{"tag_name":"v1.2","name":"v1.2 – description","draft":false,"prerelease":false}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")

# 2. Upload APK
curl -s -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/vnd.android.package-archive" \
  "https://uploads.github.com/repos/OWNER/REPO/releases/${RELEASE_ID}/assets?name=app.apk" \
  --data-binary @path/to/app-debug.apk
```

---

## Version comparison logic

The checker strips the leading `v` from the tag (so `v1.2` → `1.2`) and compares semver segments as integers. `1.10` > `1.9`. Works with any `MAJOR.MINOR.PATCH` format.

No update banner is shown when:
- The API returns 404 (no releases published yet) — ✅ safe during development
- The remote version equals the local version — ✅ correct after successful update
- The device is offline or the request times out (8s) — ✅ silent fail

---

## Real-world example

This system is live in [Metadata Randomizer for Android](https://github.com/travelermarco/metadata-randomizer).  
See `ShareActivity.kt` and `UpdateChecker.kt` in that repo for the complete working integration.
