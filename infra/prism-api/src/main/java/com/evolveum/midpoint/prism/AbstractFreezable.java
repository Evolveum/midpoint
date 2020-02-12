package com.evolveum.midpoint.prism;

public abstract class AbstractFreezable implements Freezable {

    private boolean frozen = false;

    @Override
    public final void freeze() {
        performFreeze();
        this.frozen = true;
    }

    protected void performFreeze() {
        // Intentional NOOP, for overriding
    }

    protected final boolean isMutable() {
        return !this.frozen;
    }

    public final boolean isImmutable() {
        return this.frozen;
    }


}
