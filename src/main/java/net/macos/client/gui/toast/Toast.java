package net.macos.client.gui.toast;

public class Toast {
    public static final long ANIM_IN  = 550L;   // было 400
    public static final long HOLD     = 3000L;  // было 2500
    public static final long ANIM_OUT = 500L;   // было 400
    public static final long LIFETIME = ANIM_IN + HOLD + ANIM_OUT;

    public final String title;
    public final String description;
    public final ToastType type;
    public final long startTime;

    public Toast(String title, String description, ToastType type) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.startTime = System.currentTimeMillis();
    }

    public float getProgress() {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed < ANIM_IN) {
            float t = elapsed / (float) ANIM_IN;
            return smoothstep(t);
        } else if (elapsed < ANIM_IN + HOLD) {
            return 1f;
        } else if (elapsed < LIFETIME) {
            float t = (elapsed - ANIM_IN - HOLD) / (float) ANIM_OUT;
            return 1f - smoothstep(t);
        }
        return 0f;
    }

    public boolean isDone() {
        return System.currentTimeMillis() - startTime > LIFETIME;
    }

    public static float smoothstep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}