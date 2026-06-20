// ═══════════════════════════════════════════════════════════════════════════
// STEP 1 — Add imports to your Activity
// ═══════════════════════════════════════════════════════════════════════════

import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.URL


// ═══════════════════════════════════════════════════════════════════════════
// STEP 2 — Add these fields to your Activity class
// ═══════════════════════════════════════════════════════════════════════════

// Version constant — must match the GitHub release tag (without 'v')
// BUMP THIS BEFORE EVERY BUILD THAT GOES INTO A RELEASE
companion object {
    const val VERSION_NAME = "1.0"
}

// View references
private lateinit var updateBanner: LinearLayout
private lateinit var updateText: TextView
private lateinit var updateButton: TextView
private lateinit var updateBannerLauncher: LinearLayout
private lateinit var updateTextLauncher: TextView
private lateinit var updateButtonLauncher: TextView


// ═══════════════════════════════════════════════════════════════════════════
// STEP 3 — Wire views in onCreate(), after setContentView()
// ═══════════════════════════════════════════════════════════════════════════

updateBanner         = findViewById(R.id.updateBanner)
updateText           = findViewById(R.id.updateText)
updateButton         = findViewById(R.id.updateButton)
updateBannerLauncher = findViewById(R.id.updateBannerLauncher)
updateTextLauncher   = findViewById(R.id.updateTextLauncher)
updateButtonLauncher = findViewById(R.id.updateButtonLauncher)

// Call from onCreate():
//   - if no share intent (launcher mode):  checkForUpdates(launcher = true)
//   - if share intent present:             checkForUpdates(launcher = false)


// ═══════════════════════════════════════════════════════════════════════════
// STEP 4 — Paste these methods into your Activity class
// ═══════════════════════════════════════════════════════════════════════════

private fun checkForUpdates(launcher: Boolean) {
    lifecycleScope.launch {
        val info = UpdateChecker.check(VERSION_NAME) ?: return@launch
        withContext(Dispatchers.Main) {
            val label = getString(R.string.update_available, info.version)
            if (launcher) {
                updateTextLauncher.text = label
                bindUpdateButton(updateButtonLauncher, info)
                updateBannerLauncher.visibility = View.VISIBLE
            } else {
                updateText.text = label
                bindUpdateButton(updateButton, info)
                updateBanner.visibility = View.VISIBLE
            }
        }
    }
}

private fun bindUpdateButton(button: TextView, info: UpdateInfo) {
    if (info.apkUrl != null) {
        button.setOnClickListener {
            button.text = getString(R.string.update_downloading)
            button.isClickable = false
            lifecycleScope.launch { downloadAndInstall(button, info) }
        }
    } else {
        // No APK asset on the release — fall back to browser
        button.setOnClickListener { openUrl(info.releaseUrl) }
    }
}

private suspend fun downloadAndInstall(button: TextView, info: UpdateInfo) {
    val apkUrl = info.apkUrl ?: return
    try {
        val updateDir = File(cacheDir, "updates").also { it.mkdirs() }
        val apkFile   = File(updateDir, "update.apk")

        withContext(Dispatchers.IO) {
            URL(apkUrl).openStream().use { input ->
                FileOutputStream(apkFile).use { output -> input.copyTo(output) }
            }
        }

        // Replace YOUR_FILE_PROVIDER_AUTHORITY with your actual authority string
        val apkUri = FileProvider.getUriForFile(this, "YOUR_FILE_PROVIDER_AUTHORITY", apkFile)
        val install = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data  = apkUri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, false)
        }
        startActivity(install)

    } catch (_: Exception) {
        withContext(Dispatchers.Main) {
            button.text = getString(R.string.update_download)
            button.isClickable = true
            button.setOnClickListener { openUrl(info.releaseUrl) }
            Toast.makeText(this@YourActivity, getString(R.string.update_download_error), Toast.LENGTH_LONG).show()
        }
    }
}

private fun openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
