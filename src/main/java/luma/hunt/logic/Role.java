package luma.hunt.logic;

public enum Role {
    NOBODY,
    HUNTER,
    RUNNER;

    public Role next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
