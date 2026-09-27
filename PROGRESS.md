# MacClient --- Progress

> Текущее состояние разработки клиента и ближайший roadmap.

**Minecraft:** 1.20.1\
**Java:** 17+\
**Fabric Loader:** 0.19.5\
**Fabric API:** 0.92.2+1.20.1\
**Fabric Loom:** 1.17-SNAPSHOT\
**Yarn:** 1.20.1+build.10\
**Mod Menu:** 7.2.2\
**YACL:** 3.6.2+1.20.1-fabric\
**Style:** macOS / Glassmorphism

## Legend

-   `[x]` --- реализовано и присутствует в проекте.
-   `[~]` --- частично реализовано / требует доработки.
-   `[ ]` --- запланировано.

# 1. HUD

-   [x] Watermark --- ник, FPS, ping, время.
-   [x] Armor HUD --- дуговые progress bars.
-   \[\~\] Target HUD --- имя цели и HP; требуется визуальная доработка.
-   [x] Combo Counter.
-   [x] Keystrokes --- W/A/S/D, LMB/RMB, CPS.
-   [x] Potion HUD.
-   [x] Custom Crosshair.
-   \[\~\] Custom Chat Widget.
-   [x] Custom Scoreboard.
-   [x] Directional Hit Indicator.
-   [x] Attack Cooldown Indicator.
-   [x] macOS Toast Notifications.

# 2. HUD Editor / GUI

-   [x] HUD Editor.
-   [x] Drag & Drop.
-   [x] Snap-to-grid.
-   [x] Настраиваемая сетка.
-   \[\~\] Единая Glassmorphism visual system.
-   [ ] Полный UI/UX refactor.
-   [ ] macOS/Ventura visual language.
-   [ ] Улучшенная GUI animation system.

# 3. Glass / Rendering

## Уже есть

-   [x] `RenderUtils`.
-   [x] `GlassWidget`.
-   [x] HUD rendering infrastructure.
-   [x] framebuffer-based blur infrastructure.
-   [x] custom shader resources.
-   [x] ping-pong framebuffer passes.
-   [x] `BlurRenderer`.

## В работе

-   \[\~\] Blur / Glassmorphism.
-   \[\~\] Gaussian blur для GUI.
-   [ ] Bloom.
-   [ ] Motion Blur.
-   [ ] Glass/Ghost Chams.
-   [ ] Оптимизация framebuffer pipeline.
-   [ ] Cleanup / refactor rendering code.

Целевая схема:

``` text
Scene
  ↓
Capture
  ↓
Post Processing
  ├── Blur
  ├── Bloom
  └── Motion Blur
  ↓
Glass Widgets / HUD
```

# 4. Icon Font

-   [x] Custom font provider infrastructure.
-   [x] `mac_icons.json`.
-   \[\~\] Интеграция `mac_icons.ttf`.
-   [ ] Проверить совместимость итогового TTF с Minecraft 1.20.1.
-   [ ] Перевести основные HUD icons на font glyphs.
-   [ ] Перевести GUI icons на общий icon font.
-   [ ] Убрать временные texture placeholders.

Планируемые ресурсы:

``` text
assets/macclient/font/mac_icons.json
assets/macclient/font/mac_icons.ttf
```

Font ID:

``` text
macclient:mac_icons
```

# 5. Utility

-   [x] Zoom.
-   [x] Fullbright.
-   [x] Auto Sprint.
-   [x] Anti-AFK.
-   [x] Waypoints.
-   [x] Free Look.
-   \[\~\] View Model.
-   \[\~\] Off Hand View Model.
-   [ ] Jade / Waila-style overlay.
-   [ ] Shulker / Container Preview.

### Controls

-   `C` --- Zoom.
-   `Alt` --- Free Look.
-   `Right Shift` --- HUD Editor.

# 6. View Model / Combat Visuals

-   [x] Main-hand View Model infrastructure.
-   [x] X/Y/Z offsets.
-   [x] Scale.
-   [x] Rotation X/Y/Z.
-   [x] Off-hand View Model infrastructure.
-   [x] Free Look.
-   \[\~\] Smooth Swing.
-   \[\~\] Sword Block.
-   \[\~\] Swing Mode system.
-   [ ] Дальнейшая переработка attack animations.
-   [ ] Дополнительные swing presets.

# 7. PvP Effects

-   [x] Custom Hit Effects.
-   [x] Custom Crit Effects.
-   [x] Custom Kill Effects.
-   [x] Kill tracking.
-   [x] Hit Indicator.
-   [x] Custom hit sound.
-   [x] Custom kill sound.
-   [x] Sound preset system.

# 8. Mixins

Используются client-side mixins для изменения стандартного поведения
Minecraft.

Основные области:

-   GameRenderer
-   BackgroundRenderer
-   InGameHud
-   PlayerListHud
-   ChatHud
-   Scoreboard
-   Crosshair
-   HeldItemRenderer
-   LivingEntity
-   Mouse
-   Camera
-   ClientPlayerInteractionManager
-   ClientPlayNetworkHandler
-   PlayerEntity

Задача:

-   [ ] Уменьшить связанность mixins.
-   [ ] Вынести rendering logic в отдельные системы.
-   [ ] Оставить mixins максимально тонкими.

# 9. Configuration

`ConfigManager` --- центральное хранилище настроек клиента.

Поддерживаются настройки HUD, GUI, blur, crosshair, potion HUD, armor
HUD, view model, free look, sounds, hit/crit/kill effects, waypoints,
zoom, fullbright, auto sprint, anti-AFK, visual effects и swing modes.

-   [x] JSON configuration.
-   [x] Auto-save.
-   [x] Mod Menu integration.
-   [x] YACL settings screen.
-   [ ] Разделить configuration на более чистые logical categories.
-   [ ] Упростить legacy options.

# 10. Waypoints

-   [x] Waypoint model.
-   [x] Waypoint manager.
-   [x] Save/load.
-   [x] Add/remove by name.
-   [x] Coordinates.
-   [x] Dimension.
-   [x] Color.
-   [x] Renderer.

Файл:

``` text
config/macclient_waypoints.json
```

# 11. Rendering Roadmap

## Phase 1 --- Foundation

-   \[\~\] Stabilize current blur.
-   [ ] Clean up FBO lifecycle.
-   [ ] Normalize shader resource layout.
-   [ ] Remove duplicate / legacy blur paths.

## Phase 2 --- Glass

-   [ ] Region-based background capture.
-   [ ] Stable Gaussian blur.
-   [ ] Glass tint.
-   [ ] Rounded clipping.
-   [ ] Optional border/highlight.
-   [ ] Better performance on large HUD layouts.

## Phase 3 --- Effects

-   [ ] Bloom.
-   [ ] Motion Blur.
-   [ ] Screen Drops.
-   [ ] Custom Rain.
-   [ ] Additional post-processing effects.

## Phase 4 --- Optimization

-   [ ] Reduce unnecessary framebuffer copies.
-   [ ] Avoid repeated resize/reallocation.
-   [ ] Downsample where appropriate.
-   [ ] Reuse shader resources.
-   [ ] Profile render passes.

# 12. Known Technical Issues

## Blur

Blur infrastructure exists, но финальный visual result требует
доработки.

Проблемная цепочка:

``` text
capture → blur → region rendering → glass widget
```

Нужно проверить:

-   FBO capture;
-   framebuffer dimensions vs GUI scale;
-   UV conversion;
-   shader uniforms;
-   texture lifetime;
-   blend state;
-   порядок HUD rendering.

## Icon Font

Custom icon font требует проверки итогового TTF/provider loading
pipeline.

Ожидаемый layout:

``` text
assets/macclient/font/mac_icons.json
assets/macclient/font/mac_icons.ttf
```

Если Minecraft выдаёт:

``` text
Failed to load builder (macclient:mac_icons ...)
Invalid ttf
```

нужно проверять сам TTF и совместимость его структуры с Minecraft/Java
font loader.

# 13. Code Quality Roadmap

-   [ ] Убрать legacy render paths.
-   [ ] Унифицировать naming.
-   [ ] Разделить HUD logic и rendering logic.
-   [ ] Уменьшить размер крупных классов.
-   [ ] Вынести повторяющийся OpenGL state management.
-   [ ] Централизовать shader/FBO utilities.
-   [ ] Добавить больше защит от invalid GL state.
-   [ ] Добавить debug logging для сложных rendering systems.
-   [ ] Проверять build после каждого крупного render refactor.

# 14. Release Checklist

-   [ ] `./gradlew clean build` проходит без ошибок.
-   [ ] Клиент запускается через `runClient`.
-   [ ] Config корректно создаётся и загружается.
-   [ ] HUD Editor работает.
-   [ ] Основные HUD widgets не конфликтуют.
-   [ ] Blur не ломает GUI.
-   [ ] Shader resources загружаются без ошибок.
-   [ ] Icon font загружается без `Invalid ttf`.
-   [ ] Waypoints сохраняются и загружаются.
-   [ ] Sound resources не дают missing-resource errors.
-   [ ] Mixins не дают startup warnings/errors.
-   [ ] Проверен запуск с Sodium, если он используется.
-   [ ] Debug / legacy code убран или отключён.

# 15. Current Focus

## 🔥 Priority 1 --- Rendering

Главная задача --- привести rendering side к единой архитектуре:

``` text
Blur
Glass
FBO
Shaders
HUD rendering
Bloom
```

## 🔧 Priority 2 --- GUI

После стабилизации rendering pipeline:

``` text
Glass widgets
↓
Animations
↓
Icon font
↓
Ventura/macOS visual polish
```

## 🧹 Priority 3 --- Cleanup

После этого:

``` text
Legacy code
↓
Duplicated rendering
↓
Mixins
↓
Performance
```

# 16. Definition of Done

Функция считается полностью готовой, когда она:

-   стабильно работает;
-   не ломает остальные HUD components;
-   корректно сохраняет настройки;
-   не оставляет GL state в неправильном состоянии;
-   нормально работает после resize;
-   не спамит лог ошибками;
-   соответствует общему macOS/Glassmorphism дизайну.

------------------------------------------------------------------------

## Current Status

**MacClient 1.0.0 --- active development**

Фундамент клиента уже существует. Следующий крупный этап --- не просто
добавление новых функций, а **доведение rendering/UI системы до
цельного, стабильного и производительного состояния**.
