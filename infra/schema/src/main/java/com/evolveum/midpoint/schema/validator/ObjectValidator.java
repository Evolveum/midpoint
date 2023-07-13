/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.validator;

import java.util.UUID;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Validator that can process objects, validate them, check for errors and warning
 * and possibly even transform object during upgrades.
 *
 * The purpose of this object is NOT to apply and validate static schema.
 * Prism will already do that. The purpose is to validate midPoint-specific things.
 *
 * But in fact, it does also work on some generic things, such as deprecated
 * and plannedRemoval markers. Maybe some kind of generic Prism validator
 * can be distilled from this one. But for now let's experiment with this approach.
 *
 * This is NOT a Spring bean by purpose. We want to setup the validator to do
 * various functions depending on how it is used. It may be used from GUI,
 * from task, invoked from ninja and so on.
 *
 * @author Radovan Semancik
 */
public class ObjectValidator {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectValidator.class);

    private final PrismContext prismContext;
    private boolean warnDeprecated = false;
    private boolean warnPlannedRemoval = false;
    private String warnPlannedRemovalVersion = null;
    private boolean warnIncorrectOid = false;

    public ObjectValidator(PrismContext prismContext) {
        super();
        this.prismContext = prismContext;
    }

    public boolean isWarnDeprecated() {
        return warnDeprecated;
    }

    public void setWarnDeprecated(boolean warnDeprecated) {
        this.warnDeprecated = warnDeprecated;
    }

    public boolean isWarnPlannedRemoval() {
        return warnPlannedRemoval;
    }

    public void setWarnPlannedRemoval(boolean warnPlannedRemoval) {
        this.warnPlannedRemoval = warnPlannedRemoval;
    }

    public String getWarnPlannedRemovalVersion() {
        return warnPlannedRemovalVersion;
    }

    public void setWarnPlannedRemovalVersion(String warnPlannedRemovalVersion) {
        this.warnPlannedRemovalVersion = warnPlannedRemovalVersion;
    }

    public void setWarnIncorrectOids(boolean value) {
        this.warnIncorrectOid = value;
    }

    public void setAllWarnings() {
        this.warnDeprecated = true;
        this.warnPlannedRemoval = true;
        this.warnIncorrectOid = true;
    }

    public <O extends Objectable> ValidationResult validate(PrismObject<O> object) {
        ValidationResult result = new ValidationResult();
        object.accept(visitable -> visit(visitable, result));
        return result;
    }

    private void visit(Visitable visitable, ValidationResult result) {
        if (visitable instanceof Item<?, ?>) {
            visitItem((Item<?, ?>) visitable, result);
        } else if (visitable instanceof PrismValue) {
            visitValue((PrismValue) visitable, result);
        }
    }

    private void visitValue(PrismValue value, ValidationResult result) {
        if (warnIncorrectOid) {
            if (value instanceof PrismObjectValue<?>) {
                checkOid(result, value, ((PrismObjectValue<?>) value).getOid());
            } else if (value instanceof PrismReferenceValue) {
                checkOid(result, value, ((PrismReferenceValue) value).getOid());
            }
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void visitItem(Item<V, D> item, ValidationResult result) {
        if (item.isRaw()) {
            return;
        }

        D definition = item.getDefinition();
        if (definition == null) {
            return;
        }

        if (warnDeprecated && definition.isDeprecated()) {
            warn(result, item, "deprecated");
        }

        if (warnPlannedRemoval) {
            String plannedRemoval = definition.getPlannedRemoval();
            if (plannedRemoval != null) {
                if (warnPlannedRemovalVersion == null || plannedRemoval.equals(warnPlannedRemovalVersion)) {
                    warn(result, item, "planned for removal in version " + plannedRemoval);
                }
            }
        }
    }

    private void checkOid(ValidationResult result, PrismValue item, String oid) {
        if (oid == null) {
            return;
        }
        try {
            UUID.fromString(oid);
        } catch (IllegalArgumentException e) {
            warn(result, (Item<?, ?>) item.getParent(), "OID '" + oid + "' is not valid UUID");
        }

    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void warn(ValidationResult result, Item<V, D> item, String message) {
        msg(result, OperationResultStatus.WARNING, item, message);
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void msg(ValidationResult result, OperationResultStatus status, Item<V, D> item, String message) {
        ValidationItem resultItem = new ValidationItem();
        resultItem.setStatus(status);
        if (item != null) {
            resultItem.setItemPath(item.getPath());
        }
        LocalizableMessage lMessage = new SingleLocalizableMessage(null, null, message);
        resultItem.setMessage(lMessage);
        result.addItem(resultItem);
    }
}
