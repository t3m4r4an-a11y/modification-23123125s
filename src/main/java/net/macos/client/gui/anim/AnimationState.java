package net.macos.client.gui.anim;

public class AnimationState {

    private float current;
    private float target;
    private float speed;

    public AnimationState(float initial, float speed) {
        this.current = initial;
        this.target = initial;
        this.speed = speed;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public void snapTo(float value) {
        this.current = value;
        this.target = value;
    }

    public float tick(float delta) {
        float t = Math.min(1f, delta * speed);
        current += (target - current) * t;
        if (Math.abs(current - target) < 0.001f) current = target;
        return current;
    }

    public float get() {
        return current;
    }

    public float getTarget() {
        return target;
    }

    public boolean isAnimating() {
        return current != target;
    }
}