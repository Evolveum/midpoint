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

    public ObjectUpgradeValidator(@NotNull PrismContext prismContext) {
        this.validator = new ObjectValidator(prismContext);
    }

    public void setWarnDeprecated(boolean value) {
        this.validator.setWarnDeprecated(value);
    }

    public void setWarnPlannedRemoval(boolean value) {
        this.validator.setWarnPlannedRemoval(value);
    }

    public void setWarnPlannedRemovalVersion(String value) {
        this.validator.setWarnPlannedRemovalVersion(value);
    }

    public void setWarnRemoved(boolean value) {
        this.validator.setWarnRemoved(value);
    }

    public void setWarnIncorrectOids(boolean value) {
        this.validator.setWarnIncorrectOids(value);
    }

    public void showAllWarnings() {
        this.validator.setAllWarnings();
    }

    public <O extends ObjectType> UpgradeValidationResult validate(PrismObject<O> object) throws Exception {
        ValidationResult result = validator.validate(object);

        UpgradeProcessor processor = new UpgradeProcessor();
        return processor.process(object, result);
    }
}
