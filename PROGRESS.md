# ArcX Development Progress

## Project Info
- **Package:** com.m5dev.arcx
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35
- **Architecture:** MVVM + Clean Architecture

## Completed Steps
- [x] Project structure setup
- [x] Gradle configuration
- [x] Dependency injection setup (Hilt)
- [x] File browser UI
- [x] Storage permissions handling
- [x] Real file system access and browser
- [x] NDK integration (libarchive)
- [x] Archive extraction (ZIP, 7Z, RAR, TAR)
- [x] Background extraction with notifications
- [x] Archive creation (ZIP, 7Z, TAR)
- [x] Password-protected archive creation (ZipCrypto, AES-256)
- [x] Settings screen
- [x] GPL v3 LICENSE added
- [x] README.md with full documentation
- [x] GitHub issue templates added
- [x] Adaptive and legacy app icons added
- [x] ProGuard rules and release minification configured
- [x] All unit tests passing

## Release Status
- **Ready for v0.1.0-beta release**

## Notes
- Phase 1 complete — ready for beta release
- Using NotificationCompat for Android 7 compatibility
- Permission strategy: `MANAGE_EXTERNAL_STORAGE` for Android 11+ (API 30+), `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE` for Android 10 and below (API <= 29)
- Graceful permission request and explanation dialog handling with automatic lifecycle refresh on resume
- Real file system access implemented with `java.io.File` API across `:domain` and `:data` modules
- File type icon detection with custom badged extension icon for archives
- Long-press menu supporting Extract, Compress, Delete, Rename, and Details dialogs
- NDK integrated with libarchive 3.7.7 via CMake 3.22 for API 24+ across arm64-v8a, armeabi-v7a, and x86_64
- JNI bridge (`ArchiveNative`) supporting listing archive contents, standard extraction, password-protected extraction, and JNI progress callbacks (`extractArchiveWithProgress`)
- Background extraction managed by WorkManager (`ExtractionWorker`) as a Foreground Service with persistent NotificationCompat notifications
- Notification system includes ongoing extraction progress bar (current/total files, percentage), Cancel button action, completion notification with "Open folder" action, and failure error alerts
- UI includes Active Jobs sheet accessible from TopAppBar displaying real-time job progress, cancellation controls, and navigation to extracted folders
- Currently supported formats for extraction: ZIP, 7Z, RAR, TAR
- Currently supported formats for creation: ZIP (ZipCrypto, AES-256), 7Z, TAR
- Settings Screen supported using DataStore preferences:
  - General: Default extract location (folder picker), Default compression format (ZIP/7Z/TAR), Default compression level (Store/Fast/Normal/Maximum), Ask before overwrite toggle
  - Appearance: Theme (System/Light/Dark), Dynamic colors (Material You, hidden if Android < 12), Show hidden files toggle
  - Notifications: Show extraction notifications toggle, Show completion sound toggle, Vibrate on completion toggle
  - About: App version, Open source licenses dialog, Privacy policy dialog, GitHub repo link, Rate app action
