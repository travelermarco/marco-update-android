# Security

This is a reusable snippet, not a runnable app — there's no build/CI here, since it only exists to be copied into a host project. Its `UpdateChecker.kt` snippet has a security-relevant trust model worth stating explicitly for anyone integrating it:

- It calls the GitHub Releases API over HTTPS for a **repo you configure** (`GITHUB_USER`/`GITHUB_REPO`) and downloads the `.apk` asset attached to the latest release.
- It does **not** verify a checksum or signature of the downloaded APK itself beyond the HTTPS connection — but Android's own installer enforces that an update's APK **must be signed with the same certificate** as the currently installed app, or the install is rejected. This means the practical trust boundary is: whoever controls the configured GitHub repo (and your app's signing key) controls what gets installed.
- Treat your GitHub account's security (2FA, etc.) as part of your app's supply chain if you use this.

## Reporting a vulnerability

If you find an issue with this snippet's approach, please open a GitHub issue or contact the maintainer directly rather than disclosing it publicly first.
