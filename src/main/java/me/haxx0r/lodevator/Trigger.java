package me.haxx0r.lodevator;

public enum Trigger {
    MOVE("<key:key.jump>", "<key:key.sneak>"),
    CLICK("<key:key.use>", "<key:key.attack>");

    private final String up;
    private final String down;

    Trigger(String up, String down) {
        this.up = up;
        this.down = down;
    }

    public String up() {
        return up;
    }

    public String down() {
        return down;
    }

    public static Trigger fromString(String name) {
        for (Trigger value : Trigger.values()) {
            if (value.toString().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
