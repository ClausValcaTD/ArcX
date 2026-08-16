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
- [x] Archive extraction (ZIP)
- [x] Background extraction with notifications
- [x] Archive creation
- [x] Password-protected archive creation

## Next Steps
- [ ] Settings screen

## Notes
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
