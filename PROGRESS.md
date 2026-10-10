# Aetherion --- Development Progress

> **Aetherion** (formerly MacClient) — High-performance visual client for Minecraft 1.20.1 (Fabric).
> **Aesthetic:** Dark Liquid Glass, Neon Accents, VisionOS Refractions, GPU SDF Superellipses.
> **Philosophy:** Exclusively visual and quality-of-life enhancements. **Zero combat/cheat modules.**

---

## 📋 Environment
- **Minecraft:** 1.20.1
- **Fabric Loader:** 0.19.5
- **Fabric API:** 0.92.2+1.20.1
- **Fabric Loom:** 1.17-SNAPSHOT
- **Yarn Mappings:** 1.20.1+build.10
- **Java:** 17+

---

## 🎨 Design Identity: Aetherion
Aetherion is **not** a 1-to-1 clone of macOS. While it borrows smooth glassmorphism and clean typography, it features an original, dark liquid-glass aesthetic with:
1. **GPU-Accelerated SDF Squircles**: Continuous superellipse curvatures ($n=4$) rendered via subpixel anti-aliased shaders.
2. **VisionOS-Inspired Glassmorphism**: Multi-pass Kawase blur, specular rim highlights, and ambient drop shadows.
3. **Reactive Visuals**: Dynamic crosshairs, smooth attack cooldown pills, floating 3D waypoint badges, subtle sword slash ribbons, hit bubbles, and jumping ripples.

---

## 🚀 Module Status

### 1. Rendering Engine & Glass Infrastructure
- [x] **GPU Squircle Engine (`SquircleRenderer.java`, `squircle.vsh`, `squircle.fsh`)**:
  - Exact Signed Distance Field superellipse formula.
  - Subpixel anti-aliasing via screen-space derivatives (`fwidth`).
  - Integrated into `GlassRenderer` for both solid fills and borders.
- [x] **Dual Framebuffer Blur (`BlurRenderer.java`)**: Multi-pass Kawase / Gaussian downsample & upsample blur.
- [x] **Hand & Item Chams Pipeline (`HandChamsRenderer.java`, `HandGlowRenderer.java`)**:
  - Restored crisp vanilla/resource pack textures (no hardcoded overrides).
  - Clean mask generation (`depthAfter < 0.9999`).
  - 8 procedural shader patterns: Glow, Waves, Plasma, Cyberpunk, Fire, Lightning, Rainbow, Aurora.
  - Added uniform `handChamsShaderIntensity` for live pattern brightness & density adjustment.
  - Soft Kawase bloom halo instead of harsh overblown glBlit.
- [x] **Wetness & Droplets (`WetnessRenderer.java`)**: Glossy rainy reflection shaders.
- [x] **Motion Blur (`MotionBlurRenderer.java`)**: Accumulation-based camera motion blur.

### 2. HUD & Widgets
- [x] **Watermark Widget**: Modern glass pill showing nickname, FPS, ping, and local time.
- [x] **Armor HUD**: Dynamic item durability bars (vertical and horizontal modes).
- [x] **Combo Counter**: Liquid glass counter displaying active hit combos.
- [x] **Keystrokes Widget**: W/A/S/D + LMB/RMB with CPS tracking and key press animations.
- [x] **Potion HUD**: Glass pill list of active potion effects with duration countdowns.
- [x] **Jade HUD & Shulker Preview**: In-world block/entity info tag and hoverable container item inspection.
- [x] **Attack Cooldown Indicator (`AttackCooldown.java`)**:
  - Replaced legacy green U-arc with an ultra-sleek 2px glass pill under the crosshair.
  - Smooth color ramp from warning orange to neon cyan, critical flash, and auto-fade at 100%.
- [x] **Custom Crosshair (`CustomCrosshair.java`)**:
  - Subpixel precision rendering.
  - Styles: Cross, Dot, Circle, Cross+Dot.
  - Dynamic recoil expansion on swing and enemy aim detection (turns red when targeting living entities).
- [x] **Floating 3D Waypoints (`WaypointRenderer.java`)**:
  - Floating Liquid Glass pill with POI symbol (`✦`), name, and distance.
  - 3D downward pointer pin.
  - Off-screen screen-edge clamping with smooth directional navigation arrows.

### 3. Visual & Combat FX
- [x] **Sword Slash Trails (`SlashTrailRenderer.java`)**:
  - Smooth 3D catmull-rom ribbon trailing sword swing tips.
  - Clean 240ms lifetime with additive blend and configurable accent colors.
- [x] **Impact Hit Bubble (`HitBubbleRenderer.java`)**:
  - 3D floating glowing energy orb expanding and dissipating at the strike point.
- [x] **Jump Circle (`JumpCircleRenderer.java`)**:
  - Expanding ground ripple ring with neon falloff on jump and landing.
- [x] **Directional Hit Indicator (`HitIndicator.java`)**:
  - Radial damage arc pointing towards attackers.
- [x] **Hit & Crit Particles (`HitFX.java`)**:
  - 27 custom particle presets with custom hex color overrides.
- [x] **Procedural BlockOverlay (`BlockOverlayRenderer.java`)**:
  - Animated procedural highlight on targeted blocks (Aurora, Cyber, Rainbow, Pulse, Normal).
  - Smooth box position interpolation (`Smooth` mode).
  - Configurable line width, outline color, line opacity, fill opacity, and pattern.
  - Cancels vanilla wireframe outline via `WorldRendererMixin`.
- [x] **Expanded Swing Animations (`Animation.java`)**:
  - 13 distinct styles: `DIAGONAL`, `HORIZONTAL`, `BACKHAND`, `THRUST`, `CHOP`, `JAB`, `SPIN`, `SWIPE`, `SWIPE_BACK`, `SWIPE_DOWN`, `BLOCKHIT_1_7`, `BLOCKHIT_1_8`, `SMOOTH`.
- [x] **Expanded Kill Effects (`KillEffect.java`, `KillEffectType.java`)**:
  - `BEAMS`: Radiant 8-beam rotating neon laser blast with glow halos.
  - `BURST`: Exploding physics particle cloud with gravity drops and color glow.
  - `RING`, `LIGHTNING`, `SPIRAL`, `GHOST`.
- [x] **Complete Rendering Stability Engine (`GLStateGuard.java`, `SquircleRenderer.java`, `GlassBackdrop.java`, `GlassRenderer.java`)**:
  - Solved missing widget backgrounds, missing toggle switches, missing sliders, and missing target HUD bars.
  - Eliminated GL state leaks: `GlassBackdrop.draw()` and `SquircleRenderer` now properly preserve 2D GUI depth testing disabled (`disableDepthTest()`, `depthMask(false)`) and blend enabled (`defaultBlendFunc()`).
  - Fixed double-fill scanline overlap in `GlassRenderer.roundedRect` and `MacClientMenu.drawRoundRect` for clean non-overlapping alpha blending.
  - Restored rich Liquid Glass background opacity in `GlassSurface` (removed artificial 12-27% alpha clamping).
  - Added HUD Editor preview dummies to `TargetHudWidget`, `ArmorHudWidget`, `PotionHudWidget`, and `ComboCounterWidget` so widgets never appear empty or invisible while configuring the HUD.
  - Overhauled `BlockOverlayRenderer.java` with dual-ribbon 3D thick edges and box expansion (`0.0025m`) so wireframe outlines are thick and visible from all 360° camera angles across all 7 styles (Aurora, Cyber, Fire, Plasma, Rainbow, Pulse, Normal).
  - 100% GPU 3D Kill Effects Engine in `WorldRenderEvents.LAST` with zero CPU frame drops.
- [x] **Target HUD Restoration (`TargetHudWidget.java`)**:
  - Replaced washed-out panel styling with deep obsidian liquid glass (`0xD80A0E18`) and specular sheen.
  - Immediate initial target sync so health bars appear instantaneously upon acquiring a mob/player without delay.
  - 8px liquid health bar with high-contrast track background, chip damage trailing animation, and dynamic health gradient.
- [x] **2026 Aetherion Rebranding & Shader GUI (`MenuShaderRenderer.java`, `menu_mesh.vsh`, `menu_mesh.fsh`)**:
  - Real-time animated chromatic fluid aurora mesh background shader rendering smoothly behind menus.
  - Rebranded header to `✦ AETHERION` with `2026` pill badge.
  - Fixed missing square glyph on Editor dock icon (codepoints corrected in `MacIcons.java`).
- [x] **First-Person Model Wetness (`HandGlowRenderer.java`, `hands_block_overlay.fsh`)**:
  - Realistic dripping water droplet physics, specular glints, dripping trails, and glossy surface sheen for first-person hands and weapons.
  - Supported as both procedural Chams Mode 8 and standalone `Model Wetness` toggle.
- [x] **ViewModel Reorganization & Coordinate Minimization (`MacClientMenu.java`)**:
  - Consolidated all hand, swing (13 styles), chams (8 modes), glow, and wetness features into the `ViewModel` tab.
  - Minimized 14 separate tall coordinate sliders into compact, sleek Vector3 pill cards (`[ X ] [ Y ] [ Z ]`) with horizontal drag-to-scrub and mouse-wheel scrolling support.
- [x] **Procedural Sky & Tint Fix (`ClientWorldMixin.java`)**:
  - Fixed `ClientWorld.getSkyColor` crash by matching Yarn's `Vec3d` return type.
  - Custom sky tint presets: Cyberpunk, Cold Ice, Deep Dark, Warm Sunset.
- [x] **Motion Blur Pipeline (`MotionBlurRenderer.java`, `GameRendererMixin.java`)**:
  - Accumulation-based camera motion blur rendered seamlessly before in-game HUD.
  - Isolated with `GLStateGuard` to protect OpenGL context states.
- [x] **Hit Hurt Color Pipeline (`LivingEntityRendererMixin.java`)**:
  - Dynamic `HitColorVertexConsumer` vertex color blender for custom RGB, Rainbow, Accent, Red, Golden.
  - Suppressed vanilla red overlay in `onGetOverlay` so custom tints pop vividly even in dark environments.
- [x] **Diverse Hand Chams Styles (`HandChamsRenderer.java`)**:
  - Added `Wireframe` (hardware `glPolygonMode` line geometry), `Hologram` (sinusoidal pulse), `Rainbow` (chroma wave), `Gold` (metallic sheen), `Flat` (unshaded solid CS2-style), and `Glass`.
- [x] **Enhanced Slash Trails (`SlashTrailRenderer.java`)**:
  - Multi-layer glowing inner blade spine and per-vertex Rainbow chroma wave.
- [x] **Modernized HUD Overhauls**:
  - `CustomScoreboard`: Liquid glass squircle cards, specular rim highlights, drop shadows, and Aetherion typography.
  - `CustomTabList`: Rounded player head avatars, latency signal bars, and player count badge.
  - `PotionHudWidget`: Squircle glass status cards, sprite icons, roman numerals, and smooth linear progress tracks.
  - `ArmorHudWidget`: Individual squircle glass capsules and color-coded durability tracks.
  - `AttackCooldown`: Glowing critical strike pill under crosshair.
  - `CustomCrosshair`: GPU subpixel geometry, anti-aliased dot and ring reticles, tactical chevrons.
  - `HitIndicator`: Rotated floating directional damage chevrons with neon crimson glow.

- [x] **2026 Liquid Glass Chat Overhaul (`ChatRenderer.java`, `ChatHudMixin.java`)**:
  - Full VisionOS glass panel with blur backdrop, GPU squircle cards, and subtle drop shadow.
  - Header bar with chat SF icon, title `✦ CHAT`, active channel capsule `[ ALL ]`, and total message count.
  - Message hover highlight pill and neon accent pip on newest incoming messages.
  - Immediate `ctx.draw()` buffer flushing.
- [x] **Smart Jade HUD (`JadeHudWidget.java`)**:
  - Automatically suppresses itself when Target HUD is active or during combat, preventing dual-overlay clutter.
  - Upgraded with GPU squircle item capsule, mod namespace pill badges, and health bar.
- [x] **Container & Shulker Preview Overhaul (`ShulkerPreviewRenderer.java`)**:
  - Shulker dye color matching: dynamically tints the neon accent rim to match the actual shulker box color.
  - Anti-aliased squircle item slots, item count rendering, and clean z-layering.
- [x] **FreeLook Singularity & Glitch Fix (`FreeLook.java`, `CameraMixin.java`)**:
  - Replaced linear lerp with `MathHelper.lerpAngleDegrees` to eliminate the 180° wrap-around snap to 0° (same direction as player).
  - Smooth camera glide back to player view upon releasing Alt key.
- [x] **Attack Cooldown-Synced Swing Animations (`Animation.java`, `ConfigManager.java`)**:
  - Preserved all classic swing animations untouched (`DIAGONAL`, `HORIZONTAL`, etc.).
  - Added toggle `Sync Swing Cooldown`: times the swing progression with actual weapon recharge (half cooldown strike, half cooldown return).
- [x] **World-Locked Hit Damage Indicator (`HitIndicator.java`, `LivingEntityMixin.java`)**:
  - Stored attacker world angle so the damage arc stays locked onto the enemy in 3D space even as the player rotates their camera.
  - Added hot-white apex pointer and smooth squircle curve.
- [x] **0 FPS Periodic Drops & Performance Fixes (`GLStateGuard.java`, `HandGlowRenderer.java`, `WetnessRenderer.java`, `MacClient.java`)**:
  - **Fixed `GLStateGuard` `GL_INVALID_ENUM` error 1005**: Replaced invalid `GL11.GL_TEXTURE_2D` query with `GL_TEXTURE_BINDING_2D`, preventing millions of OpenGL debug errors and massive 1GB+ disk write stalls.
  - **Decoupled `enableModelWetness` from `HandGlowRenderer`**: Stopped heavy multi-pass Kawase blur and full-screen FBO blits from executing every frame when hand chams was disabled.
  - **Removed duplicate `MotionBlurRenderer.apply()`**: Eliminated duplicate blur execution in `WorldRenderEvents.END` while retaining the primary call before HUD rendering.
  - **Optimized `WetnessRenderer` skylight checks**: Prevented redundant raycasting across distant entities in crowded combat.
- [x] **Universal Font Fallback (`aetherion.json`)**:
  - Added vanilla reference providers (`space`, `default`, `unifont`) to `aetherion.json`, ensuring missing glyphs (Cyrillic, symbols like `∞`, and potion names) seamlessly fall back without stalls.
- [x] **Resource Packs Screen Exit & Reload (`MacClientMenu.java`)**:
  - Fixed "Done" button freeze by comparing pack lists before/after, invoking `mc.reloadResources()` when changed, and cleanly returning to `new MacClientMenu()`.
- [x] **Chat Message Dropping & Readability (`ChatRenderer.java`, `AetherionFont.java`)**:
  - Switched message age loop from `break` to `continue` to preserve all active messages.
  - Added `drawWithShadow` for chat lines to ensure crystal-clear text contrast on dark glass backgrounds.
- [x] **Snappy FreeLook Return (`FreeLook.java`)**:
  - Reduced return damping from 0.70f to 0.28f, enabling snappy camera glide back to center in ~100ms upon releasing Alt.
- [x] **Aesthetic 3D HitBubble Overhaul (`HitBubbleRenderer.java`)**:
  - Upgraded with pure radiant core, chromatic perimeter ring, and 4-point starburst sparks with cubic ease-out pop.
- [x] **Built-in Low Fire Toggle (`InGameOverlayRendererMixin.java`, `ConfigManager.java`, `MacClientMenu.java`)**:
  - Added Low Fire toggle in Visuals tab to comfortably offset first-person fire overlay height for PvP clarity.

---

## 🛠️ Build & Verification
- Compile command: `./gradlew compileJava`
- Build command: `./gradlew build -x test`
- Run client command: `./gradlew runClient`
- Verification: Clean compilation, 100% successful build, zero runtime GL errors.

