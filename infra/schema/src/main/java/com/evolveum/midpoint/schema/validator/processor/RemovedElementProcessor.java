package com.evolveum.midpoint.schema.validator.processor;

import com.evolveum.midpoint.schema.validator.UpgradePhase;
import com.evolveum.midpoint.schema.validator.UpgradePriority;
import com.evolveum.midpoint.schema.validator.UpgradeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class RemovedElementProcessor<T extends ObjectType> {//implements UpgradeObjectProcessor<T> {

    // todo better name :)))
    enum Whatever {

        ADD_REMOVE_ATTRIBUTE_VALUES("addRemoveAttributeValues");

        public final String identifier;

        public final UpgradePhase phase;

        public final UpgradeType type;

        public final UpgradePriority priority;

        Whatever(String identifier) {
            this(identifier, UpgradePhase.BEFORE, UpgradeType.SEAMLESS, UpgradePriority.NECESSARY);
        }

        Whatever(String identifier, UpgradePhase phase, UpgradeType type, UpgradePriority priority) {
            this.identifier = identifier;
            this.phase = phase;
            this.type = type;
            this.priority = priority;
        }
    }

//    @Override
//    public boolean processObject(PrismObject<T> object, OperationResult result) {
//        return true;
//    }
}
