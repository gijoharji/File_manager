# File Manager

A production-quality Android file manager app that categorizes local files and provides efficient file management capabilities.

## Features

### File Categorization
- **Images**: JPG, PNG, GIF, BMP, WebP, HEIC, SVG, and more
- **Videos**: MP4, MKV, AVI, MOV, WMV, and more
- **Audio**: MP3, WAV, FLAC, AAC, OGG, and more
- **Documents**: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, TXT, and more
- **APKs**: Android application packages
- **Archives**: ZIP, RAR, 7Z, TAR, GZ, and more

### Category Display
- Shows item count and total size per category
- Clean, modern Material Design 3 UI
- Fast file scanning with efficient caching

### Source Folder Navigation
- Drills into common sources:
  - Downloads
  - WhatsApp Media
  - Screenshots
  - Camera
  - Other app folders
- Expandable folder cards showing file details

### File Operations
- **Select All**: Quick selection of all files in a source
- **Multi-select**: Tap files to select/deselect
- **Delete**: Remove selected files (with confirmation)
- **Move/Copy**: Move or copy files to different storage locations
  - Internal Storage
  - Downloads
  - Documents
  - Pictures
  - Music
  - Movies
- **Share**: Share multiple files at once
- **Open with**: Open files with default or chosen apps

## Technical Details

### Architecture
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **State Management**: StateFlow with Compose State
- **Coroutines**: For async file operations

### Permissions
- Android 13+ (API 33+): READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_AUDIO
- Android 12 and below: READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE

### File Provider
- Uses FileProvider for secure file sharing
- Supports sharing files with other apps

## Building

1. Open the project in Android Studio
2. Sync Gradle files
3. Build and run on a device or emulator

## Requirements

- Android Studio Hedgehog (2023.1.1) or later
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Gradle 8.2+

## Project Structure

```
app/
├── src/
│   └── main/
│       ├── java/com/filemanager/app/
│       │   ├── data/          # Data models and categories
│       │   ├── ui/            # UI components and screens
│       │   ├── utils/         # Utility functions
│       │   ├── viewmodel/     # ViewModels
│       │   └── MainActivity.kt
│       ├── res/               # Resources
│       └── AndroidManifest.xml
└── build.gradle.kts
```

## License

This project is created for demonstration purposes.

