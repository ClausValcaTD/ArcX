# ArcX

ArcX is a modern, fast, open-source archive manager and file browser for Android. Built with Jetpack Compose and Material Design 3, ArcX integrates libarchive via Android NDK for high-performance compression and extraction across multiple archive formats.

---

## Features

- **Broad Format Support**:
  - **Extract**: ZIP, 7Z, RAR, TAR, TAR.GZ, TAR.XZ, TAR.BZ2
  - **Create**: ZIP (ZipCrypto, AES-256), 7Z, TAR
- **Password Protection & Security**: Support for password-protected archives with AES-256 and ZipCrypto encryption methods.
- **Background Operations**: Large compression and extraction tasks run reliably in the background using Android WorkManager with persistent foreground progress notifications.
- **Modern Material You UI**: Full Jetpack Compose interface supporting dynamic colors (Android 12+), dark and light themes, and smooth animations.
- **Full File Manager**: File navigation, multi-select batch operations, details viewer, file renaming, deletion, and quick folder navigation.
- **Customizable Preferences**: Configure default extraction locations, compression defaults, overwrite rules, dynamic theme behavior, and notification alerts.

---

## Screenshots

| File Browser | Extraction Progress | Archive Creation | Settings |
|:---:|:---:|:---:|:---:|
| _![File Browser Screen](docs/screenshots/file_browser.png)_ | _![Extraction Progress](docs/screenshots/extraction.png)_ | _![Archive Creation](docs/screenshots/compression.png)_ | _![Settings Screen](docs/screenshots/settings.png)_ |

---

## Download

Get the latest APK from the [GitHub Releases](https://github.com/m5dev/arcx/releases) page.

---

## Build Instructions

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17
- Android SDK (API 35, Min SDK 24)
- Android NDK (26.1.10909125)
- CMake 3.22.1

### Steps
1. **Clone the repository**:
   ```bash
   git clone https://github.com/m5dev/arcx.git
   cd arcx
   ```
2. **Build debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```
3. **Run unit tests**:
   ```bash
   ./gradlew test
   ```
4. **Build release APK**:
   ```bash
   ./gradlew assembleRelease
   ```

---

## Contributing

Contributions are welcome!
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes following standard commit message conventions.
4. Push to your branch and submit a Pull Request.

Please check the issue templates under `.github/ISSUE_TEMPLATE` when reporting bugs or requesting new features.

---

## License

ArcX is open-source software licensed under the [GNU General Public License v3.0 (GPL v3)](LICENSE).
