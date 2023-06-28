package com.evolveum.midpoint.schema.validator;

import com.evolveum.midpoint.prism.delta.ObjectDelta;

public class UpgradeValidationItem {

    private final ValidationItem item;

    private boolean changed;

    private String identifier;

    private UpgradePhase phase;

    private UpgradePriority priority;

    private UpgradeType type;

    private ObjectDelta<?> delta;

    public UpgradeValidationItem(ValidationItem item) {
        this.item = item;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public UpgradePhase getPhase() {
        return phase;
    }

    public void setPhase(UpgradePhase phase) {
        this.phase = phase;
    }

    public UpgradePriority getPriority() {
        return priority;
    }

    public void setPriority(UpgradePriority priority) {
        this.priority = priority;
    }

    public UpgradeType getType() {
        return type;
    }

    public void setType(UpgradeType type) {
        this.type = type;
    }

    public ObjectDelta<?> getDelta() {
        return delta;
    }

    public void setDelta(ObjectDelta<?> delta) {
        this.delta = delta;
    }

    public ValidationItem getItem() {
        return item;
    }
}
