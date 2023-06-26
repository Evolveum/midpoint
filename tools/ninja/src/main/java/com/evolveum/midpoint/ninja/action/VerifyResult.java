package com.evolveum.midpoint.ninja.action;

// todo use this
public class VerifyResult {

    private boolean hasCriticalItems;

    private boolean hasNecessaryItems;

    private boolean hasOptionalItems;

    public boolean isHasCriticalItems() {
        return hasCriticalItems;
    }

    public void setHasCriticalItems(boolean hasCriticalItems) {
        this.hasCriticalItems = hasCriticalItems;
    }

    public boolean isHasNecessaryItems() {
        return hasNecessaryItems;
    }

    public void setHasNecessaryItems(boolean hasNecessaryItems) {
        this.hasNecessaryItems = hasNecessaryItems;
    }

    public boolean isHasOptionalItems() {
        return hasOptionalItems;
    }

    public void setHasOptionalItems(boolean hasOptionalItems) {
        this.hasOptionalItems = hasOptionalItems;
    }
}
