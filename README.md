# 👋 TgFlowBot - Android Application

A modern Android application showcasing professional development practices with Gradle automation and modern Android architecture.

---

## 🌟 Project Overview

**TgFlowBot** is an Android project built with:
- **Language**: Java (97.9%), Shell (2.1%)
- **Build System**: Gradle 9.1.0
- **Target**: Android API 24+
- **Status**: Active Development

---

## 🛠️ Tech Stack

| Component | Version |
|-----------|---------|
| Gradle | 9.1.0 |
| Android Gradle Plugin | 8.13.2 |
| AndroidX Core | 1.17.0 |
| AndroidX AppCompat | 1.7.1 |
| Material Design | 1.13.0 |
| ConstraintLayout | 2.2.1 |
| RecyclerView | 1.4.0 |
| Activity | 1.10.1 |
| Fragment | 1.8.6 |
| OkHttp | 4.12.0 |
| Gson | 2.11.0 |

---

## 🚀 Installation & Setup

### Prerequisites
- ✅ Java 11 or higher
- ✅ Android Studio (latest)
- ✅ Git
- ✅ Android SDK 24+

### Step 1: Clone Repository
```bash
git clone https://github.com/padz24/TgFlowBot.git
cd TgFlowBot
```

### Step 2: Open in Android Studio
1. Launch **Android Studio**
2. **File** → **Open**
3. Select the `TgFlowBot` folder
4. Click **Open**

### Step 3: Gradle Sync
- Android Studio will prompt **"Sync Now"** → Click it
- Wait for dependency download (10-15 minutes on first build)

### Step 4: Build Project
```
Build → Make Project
```
or press `Ctrl+F9` (Windows/Linux) / `Cmd+F9` (Mac)

### Step 5: Run App
1. **Tools** → **Device Manager** (Create emulator or connect device)
2. **Run** → **Run 'app'** or press `Shift+F10`
3. Select your target device

---

## 📁 Project Structure

```
TgFlowBot/
├── app/                           # Main app module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/             # Source code
│   │   │   ├── res/              # Resources (layouts, drawables)
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                 # Unit tests
│   │   └── androidTest/          # Instrumented tests
│   ├── build.gradle.kts          # App build config
│   └── proguard-rules.pro        # Code obfuscation
│
├── gradle/                       # Gradle configuration
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Project settings
├── gradle.properties             # Gradle properties
├── gradlew                       # Unix/Linux/Mac wrapper
├── gradlew.bat                   # Windows wrapper
└── .gitignore
```

---

## 📦 Key Dependencies

### AndroidX
- Core, AppCompat, ConstraintLayout, RecyclerView, Activity, Fragment

### UI
- Material Design 3

### Networking & Data
- OkHttp 4.12.0
- Gson 2.11.0

---

## ⚙️ Build Configuration

### Gradle Wrapper
```properties
distributionUrl=https://services.gradle.org/distributions/gradle-9.1.0-bin.zip
```

### Kotlin DSL Scripts
Type-safe build configuration with IDE autocompletion

### JVM Settings
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

---

## 🔧 Troubleshooting

| Problem | Solution |
|---------|----------|
| Gradle sync fails | File → Invalidate Caches → Restart |
| Dependencies not resolving | File → Sync Now + Invalidate Caches |
| Emulator not showing | Tools → Device Manager → Create device |
| Build too slow | Enable parallel build in Gradle settings |

---

## 🎯 Features

✅ Modern Android Architecture  
✅ Gradle 9.1.0 with Kotlin DSL  
✅ Material Design Components  
✅ Network Support (OkHttp)  
✅ JSON Serialization (Gson)  
✅ RecyclerView for Lists  
✅ Backward Compatible (API 24+)  

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature/YourFeature`
3. Commit: `git commit -m 'Add YourFeature'`
4. Push: `git push origin feature/YourFeature`
5. Open Pull Request

---

## 📄 License

This project is open source and available under the MIT License.

---

## 📞 Support

- 🐛 [Open an Issue](https://github.com/padz24/TgFlowBot/issues)
- 💬 [Start a Discussion](https://github.com/padz24/TgFlowBot/discussions)

---

**Built with ❤️ by padz24**