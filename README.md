# MacClient

> **Aesthetic macOS-style utility client for Minecraft 1.20.1 (Fabric)**

MacClient --- клиентский Minecraft-мод, объединяющий HUD, визуальные
инструменты, PvP-функции и настройки рендера в едином **macOS /
Glassmorphism**-стиле.

## ✦ Основные направления

-   🪟 **Glassmorphism UI** --- стеклянные панели, HUD-виджеты и
    macOS-стиль.
-   🎨 **Custom HUD** --- Watermark, Armor HUD, Target HUD, Keystrokes,
    Combo Counter и другие компоненты.
-   ⚔️ **PvP Visuals** --- hit/crit effects, kill effects, hit
    indicator, кастомный crosshair и view model.
-   🧩 **Utility** --- Zoom, Fullbright, Auto Sprint, Anti-AFK,
    Waypoints и Free Look.
-   🔊 **Custom Sounds** --- отдельные звуки ударов и убийств с
    пресетами.
-   🖥️ **Custom Rendering** --- собственные рендер-утилиты, mixins и
    пост-обработка.
-   ⚙️ **Configurable** --- настройки сохраняются в JSON и доступны
    через собственное меню.
-   🍎 **macOS-inspired design** --- единый визуальный язык клиента.

## 🚀 Что уже есть

### HUD

-   Watermark --- ник, FPS, ping и время.
-   Armor HUD --- кастомный индикатор брони.
-   Target HUD --- отображение цели и HP.
-   Combo Counter.
-   Keystrokes --- W/A/S/D, LMB/RMB и CPS.
-   Potion HUD.
-   Custom Crosshair.
-   Custom Chat Widget.
-   Custom Scoreboard.
-   HUD Editor с drag & drop и snap-to-grid.
-   macOS Toast Notifications.
-   Directional Hit Indicator.
-   Attack Cooldown Indicator.

### Gameplay / Utility

-   Zoom.
-   Fullbright.
-   Auto Sprint.
-   Anti-AFK.
-   Waypoints.
-   Free Look.
-   View Model для основной и второй руки.

### Visual / PvP

-   Custom Hit Effects & Crit Effects.
-   Enhanced 3D Kill Effects (Nova core, orbital rings, lightning branches).
-   Aesthetic 3D HitBubble (chromatic shockwave, radiant core, starburst sparks).
-   Jump Circles & Slash Trails.
-   Low Fire overlay toggle.
-   Custom Block Overlay (Smooth, Aurora, Rainbow shaders).
-   Кастомные звуки удара и убийства.
-   Настраиваемый swing mode (с опцией синхронизации с cooldown оружия).
-   Система Glass/Ghost Chams и Glow (8 процедурных шейдеров).

## 🛠️ Технологический стек

  Компонент       Версия
  --------------- -----------------------
  Minecraft       `1.20.1`
  Java            `17+`
  Fabric Loader   `0.19.5`
  Fabric API      `0.92.2+1.20.1`
  Fabric Loom     `1.17-SNAPSHOT`
  Yarn Mappings   `1.20.1+build.10`
  Mod Menu        `7.2.2`
  YACL            `3.6.2+1.20.1-fabric`
  Sodium          `0.5.8` *(optional)*

## 📁 Структура проекта

Основной клиент:

``` text
src/main/java/net/macos/client/
├── chat/          # кастомный чат
├── config/        # конфигурация
├── gui/           # GUI и HUD editor
├── hud/           # HUD-виджеты
├── mixin/         # Minecraft mixins
├── module/        # клиентские модули
├── particle/      # hit / crit / kill effects
├── render/        # рендер и shader-related код
├── utils/         # общие утилиты
└── waypoint/      # waypoint system
```

Ресурсы:

``` text
src/main/resources/assets/macclient/
├── font/
├── fonts/
├── shaders/
├── textures/
├── sounds/
└── lang/
```

## ⚙️ Установка для разработки

### Требования

-   JDK 17
-   IntelliJ IDEA или другая IDE с Gradle
-   Git
-   Minecraft 1.20.1 development environment

### Сборка

``` bash
./gradlew build
```

### Запуск клиента

``` bash
./gradlew runClient
```

Для полной пересборки:

``` bash
./gradlew clean build runClient
```

## 🎛️ Конфигурация

Центральная система настроек:

``` text
net.macos.client.config.ConfigManager
```

Она управляет HUD, GUI, blur, crosshair, view model, sounds,
hit/crit/kill effects, waypoints, zoom, fullbright, movement utilities,
swing и визуальными функциями.

## ⌨️ Управление

  Функция             Клавиша
  ------------------- --------------------
  HUD Editor          `Right Shift`
  Zoom                `C`
  Free Look           `Alt`
  Остальные функции   через конфигурацию

## 🎨 Rendering

Основные элементы rendering stack:

-   `RenderUtils`
-   `GlassWidget`
-   `HudWidget`
-   `BlurRenderer`
-   custom shader resources
-   mixins для Minecraft rendering pipeline

Цель --- единый rendering pipeline для Glassmorphism, blur, bloom и
других post-processing эффектов.

## 🔮 Roadmap

### Rendering

-   [ ] Переработать Blur / Glassmorphism.
-   [ ] Довести Gaussian Blur для GUI.
-   [ ] Bloom.
-   [ ] Motion Blur.
-   [ ] Glass/Ghost Chams без нежелательного просвечивания.
-   [ ] Custom Weather / Rain.
-   [ ] Screen Drops.
-   [ ] Оптимизация FBO и post-processing pipeline.

### GUI / HUD

-   [ ] Полный UI/UX refactor.
-   [ ] Единая система анимаций.
-   [ ] Единый icon font.
-   [ ] Более цельный macOS/Ventura visual language.
-   [ ] Улучшение HUD Editor.
-   [ ] Дальнейшая переработка стеклянных панелей.

### Utility

-   [ ] Jade / Waila-style overlay.
-   [ ] Shulker / Container Preview.

### Polish

-   [ ] Унификация звуков.
-   [ ] Улучшение визуальных эффектов.
-   [ ] Оптимизация.
-   [ ] Устранение legacy-кода.

## 🧪 Текущие технические задачи

### Blur / Glass

Существующая система использует framebuffer ping-pong и собственные
shader passes. Главная задача --- получить нормальное размытие именно
под стеклянными HUD-компонентами, а не просто размывать весь экран.

### Icon Font

Клиент переводится на единый icon font вместо отдельных texture
placeholders.

Планируемая схема:

``` text
assets/macclient/font/mac_icons.json
assets/macclient/font/mac_icons.ttf
```

Font ID:

``` text
macclient:mac_icons
```

Для codepoint выше `U+FFFF` используется:

``` java
new String(Character.toChars(codePoint))
```

## 📌 Статус

Подробное состояние каждой подсистемы находится в
[`PROGRESS.md`](PROGRESS.md).

Не всё, что присутствует в конфигурации или кодовой базе, следует
считать полностью отполированным: часть функций требует дальнейшего
рефакторинга, визуальной доработки или оптимизации.

## 📄 License

Проект распространяется под **MIT License** согласно `fabric.mod.json`.
