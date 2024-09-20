package com.evolveum.midpoint.schema.validator;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Enhanced validator that also produces more information about how validation item should be updated.
 */
public class ObjectUpgradeValidator {

    private final ObjectValidator validator;

    public ObjectUpgradeValidator() {
        this.validator = new ObjectValidator();
    }

    public void setTypeToCheck(@NotNull ValidationItemType item, boolean check) {
        this.validator.setTypeToCheck(item, check);
    }

    public void setWarnPlannedRemovalVersion(String value) {
        this.validator.setWarnPlannedRemovalVersion(value);
    }

    public void showAllWarnings() {
        this.validator.setAllWarnings();
    }

    public <O extends ObjectType> UpgradeValidationResult validate(PrismObject<O> object) throws Exception {
        ValidationResult result = validator.validate(object);

        ValidationResult filtered = new ValidationResult();

        result.getItems().stream()
                .filter(i -> i.type() == ValidationItemType.DEPRECATED_REMOVED_PLANNED_REMOVAL_ITEM)
                .forEach(filtered::addItem);

        UpgradeProcessor processor = new UpgradeProcessor();
        return processor.process(object, filtered);
    }
}
