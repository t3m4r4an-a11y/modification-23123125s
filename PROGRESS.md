# MacClient — Прогресс разработки

**Версия:** 1.0.0
**Minecraft:** 1.20.1
**Fabric Loom:** 1.17.20
**Mappings:** Yarn 1.20.1+build.10
**Лицензия:** MIT
**Стиль:** macOS Glassmorphism

---

## Готово и работает

- [x] **Watermark** — ник, FPS, пинг, время (сверху слева)
- [x] **ArmorHud** — дуговые прогресс-бары брони (фикс краша: normal(0,0,1))
- [x] **TargetHud** — HP цели, имя (заготовка)
- [x] **ComboCounter** — счётчик комбо (фикс +6 → +1)
- [x] **ConfigManager** — JSON-конфиг с автосохранением
- [x] **ModMenuIntegration** — YACL-меню в Mod Menu
- [x] **GlassPanel** — базовый класс для HUD-виджетов
- [x] **RenderUtils** — утилиты (drawRoundedRect, drawArc, hexToColor)
- [x] **Keystrokes** — W/A/S/D + LMB/RMB + CPS
### Лёгкие (чистый рендер)
- [x] Attack Cooldown Indicator
- [x] Zoom (клавиша C)
- [x] Fullbright (гамма → 1.0)
- [x] Auto Sprint
- [x] Anti-AFK
- [x] Waypoints (macOS-стиль)

### Средние (миксины)
- [x] HUD Editor (Right Shift, drag & drop, snap-to-grid)
- [x] Custom Chat Widget(Частично выполнено)
- [x] Custom Scoreboard
- [x] macOS Toast Notifications
- [x] Directional Hit Indicator
- [x] View Model (X/Y/Z, scale, rotation, FOV, action offsets)
- [x] Free Look (Alt)
- [x] Custom Sounds (Pop/CoD/Apex/CS)
- [x] Custom Crit/Kill Particles

## 🚧 В работе

### Сложные (шейдеры, пост-процессинг)
- [ ] Glass/Ghost Chams (без просвечивания сквозь стены)
- [ ] Motion Blur
- [ ] Blur / Glassmorphism (гауссово размытие GUI)
- [ ] Custom Weather / Rain / Screen Drops
- [ ] Рефакторинг GUI(улучшение внешнего вида(UI/UX DESIGN)
### Утилиты
- [ ] Jade / Waila
- [ ] Shulker/Container Preview



> Продолжаем MacClient для MC 1.20.1 Fabric. Стек: Java 17, Loom 1.17, Yarn. Что работает: [см. PROGRESS.md]. Делаем сейчас: [задача].