# 🐨 Koala: The Ultimate PDF Ecosystem

![Koala Logo](app/src/main/res/drawable/logo.png)

> **"Simplicity is the ultimate sophistication."**  
> Koala isn't just a PDF reader; it's the beginning of a world-class PDF ecosystem. Engineered for speed, designed for elegance, and built for the future.

---

## 🌟 Vision
Koala aims to redefine the document experience on mobile. While other readers are bloated with legacy code and intrusive ads, Koala provides a **pure, edge-to-edge, high-performance canvas** for your documents. Our mission is to build the world's most intuitive and powerful PDF ecosystem, starting with a reader that feels like second nature.

## 🚀 Key Features (The "Best in Class" Foundation)

### 🏔️ True Edge-to-Edge Experience
Koala utilizes the latest Android `enableEdgeToEdge()` APIs to reclaim every pixel of your screen. Content flows behind status and navigation bars, providing an immersive reading experience that makes the hardware disappear.

### ⚡ Lightning-Fast Native Rendering
Powered by a highly optimized native **PDFium engine**, Koala renders even the most complex, multi-gigabyte architectural blueprints or academic papers with ease.

### 🧠 Intelligent Memory Management
- **LruCache Integration:** We don't just render; we remember. Koala intelligently caches rendered pages in memory, ensuring that flipping back to a previous page is instantaneous.
- **Bitmap Pooling & Recycling:** To prevent the "stutter" caused by Android's Garbage Collector (GC), Koala implements a custom Bitmap Pool. We recycle memory buffers instead of constantly allocating new ones, resulting in buttery-smooth 60FPS scrolling.
- **Quantized Rendering:** Our engine "quantizes" target resolutions, maximizing cache hits while maintaining razor-sharp visual fidelity.

### 🔍 Adaptive High-Resolution Zoom
Most readers blur when you zoom. Koala **re-renders**. As you pinch to zoom into a document, our engine dynamically generates a high-resolution tile for your specific zoom level, ensuring text remains crisp at 500% magnification.

### 🌗 Dynamic Theming
Full support for **Material You (Material 3)**. Koala automatically adapts its color palette to your system wallpaper and respects your Dark Mode preferences, making it easy on the eyes during late-night study sessions.

---

## 🏗️ Technical Excellence

### Architecture
Koala is built on a rock-solid **MVVM (Model-View-ViewModel)** architecture, ensuring a clean separation of concerns and a codebase that is easy to extend.

- **Frontend:** 100% Jetpack Compose. No XML layouts, no legacy `View` system.
- **Backend:** Kotlin Coroutines and StateFlow manage the data layer, ensuring the UI remains responsive even during heavy I/O operations.
- **Native Layer:** C++ PDFium bindings via `pdfium-android` for industry-standard reliability.

### Performance Profile
- **Cold Start:** < 500ms
- **Memory Footprint:** Dynamic (scales based on device RAM)
- **Scrolling:** Sustained 60FPS on mid-range devices

---

## 🗺️ The Roadmap: From Reader to Ecosystem
Koala is just getting started. Our vision for the "World's Best PDF Ecosystem" includes:

- [ ] **Advanced Annotations:** Highlighting, Apple-pencil style sketching, and sticky notes.
- [ ] **Cloud Sync:** Seamless continuity between your mobile device and desktop.
- [ ] **OCR Integration:** Turn scanned images into searchable, selectable text.
- [ ] **AI-Powered Insights:** Summarize long documents and "ask" your PDF questions.
- [ ] **Form Filling & Signatures:** Professional-grade tools for business workflows.

---

## 🛠️ Building the Future

### Prerequisites
- Android Studio Ladybug (or newer)
- JDK 17
- Android SDK 36 (Vanilla Ice Cream)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/bimbok/koala.git

# Build the debug APK
./gradlew assembleDebug

# Run the test suite
./gradlew test connectedAndroidTest
```

## 🤝 Contributing
We believe the best ecosystem is built by the community. If you share our vision for a better document future, we welcome your PRs.

1. Fork the repo.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📜 License
Koala is released under the [MIT License](LICENSE).

---

*Made with ❤️ by Bimbok for readers everywhere.*
