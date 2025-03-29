package com.evolveum.midpoint.model.api.visualizer;

import java.io.Serializable;

import com.evolveum.midpoint.schema.constants.ObjectTypes;

public record LocalizationCustomizationContext(ObjectTypes objectType, ActionType actionType) implements Serializable {
    private static final LocalizationCustomizationContext EMPTY = new LocalizationCustomizationContext(null, null);

    public static Builder builder() {
        return new Builder();
    }

    public static LocalizationCustomizationContext empty() {
        return EMPTY;
    }

    public static class Builder {
        private ObjectTypes objectType;
        private ActionType actionType;

        public Builder objectType(ObjectTypes objectType) {
            this.objectType = objectType;
            return this;
        }

        public Builder actionType(ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public LocalizationCustomizationContext build() {
            return new LocalizationCustomizationContext(this.objectType, this.actionType);
        }
    }
}
