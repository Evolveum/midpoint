/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.validator;

import java.util.*;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.key.NaturalKeyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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

    private Set<ValidationItemType> typesToCheck = new HashSet<>();

    private String warnPlannedRemovalVersion = null;

    private boolean summarizeItemLifecycleState = true;

    public Set<ValidationItemType> getTypesToCheck() {
        return typesToCheck;
    }

    public void setTypesToCheck(@NotNull Set<ValidationItemType> typesToCheck) {
        this.typesToCheck = typesToCheck;
    }

    public String getWarnPlannedRemovalVersion() {
        return warnPlannedRemovalVersion;
    }

    public void setWarnPlannedRemovalVersion(String warnPlannedRemovalVersion) {
        this.warnPlannedRemovalVersion = warnPlannedRemovalVersion;
    }

    public boolean isSummarizeItemLifecycleState() {
        return summarizeItemLifecycleState;
    }

    public void setSummarizeItemLifecycleState(boolean summarizeItemLifecycleState) {
        this.summarizeItemLifecycleState = summarizeItemLifecycleState;
    }

    public void setTypeToCheck(@NotNull ValidationItemType type, boolean set) {
        if (set) {
            typesToCheck.add(type);
        } else {
            typesToCheck.remove(type);
        }
    }

    public void setAllWarnings() {
        typesToCheck.addAll(Arrays.asList(ValidationItemType.values()));
    }

    public <O extends Objectable> ValidationResult validate(PrismObject<O> object) {
        ValidationResult result = new ValidationResult();
        object.accept(visitable -> visit(visitable, result));

        return result;
    }

    public <C extends Containerable> ValidationResult validate(PrismContainerValue<C> object) {
        ValidationResult result = new ValidationResult();
        object.accept(visitable -> visit(visitable, result));

        return result;
    }

    private void visit(Visitable<?> visitable, ValidationResult result) {
        if (visitable instanceof Item<?, ?>) {
            visitItem((Item<?, ?>) visitable, result);
        } else if (visitable instanceof PrismValue) {
            visitValue((PrismValue) visitable, result);
        }
    }

    private void visitValue(PrismValue value, ValidationResult result) {
        if (check(ValidationItemType.INCORRECT_OID_FORMAT)) {
            if (value instanceof PrismObjectValue<?>) {
                checkOid(result, value, ((PrismObjectValue<?>) value).getOid());
            } else if (value instanceof PrismReferenceValue) {
                checkOid(result, value, ((PrismReferenceValue) value).getOid());
            }
        }
    }

    private boolean check(ValidationItemType type) {
        return typesToCheck.contains(type);
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void visitItem(Item<V, D> item, ValidationResult result) {
        if (item.isRaw()) {
            return;
        }

        D definition = item.getDefinition();
        if (definition == null) {
            return;
        }

        List<String> messages = new ArrayList<>();
        if (check(ValidationItemType.DEPRECATED_ITEM) && definition.isDeprecated()) {
            if (!summarizeItemLifecycleState) {
                warn(
                        result, ValidationItemType.DEPRECATED_ITEM, "Deprecated item " + item.getElementName().getLocalPart(),
                        item, item);
            } else {
                messages.add("deprecated");
            }
        }

        if (check(ValidationItemType.PLANNED_REMOVAL_ITEM)) {
            String plannedRemoval = definition.getPlannedRemoval();
            if (plannedRemoval != null) {
                if (warnPlannedRemovalVersion == null || plannedRemoval.equals(warnPlannedRemovalVersion)) {
                    if (!summarizeItemLifecycleState) {
                        warn(
                                result, ValidationItemType.PLANNED_REMOVAL_ITEM,
                                "Item " + item.getElementName().getLocalPart() + " planned for removal in version " + plannedRemoval,
                                item, item);
                    } else {
                        messages.add("planned for removal in version " + plannedRemoval);
                    }
                }
            }
        }

        if (check(ValidationItemType.REMOVED_ITEM) && definition.isRemoved()) {
            if (!summarizeItemLifecycleState) {
                warn(
                        result, ValidationItemType.REMOVED_ITEM, "Removed item " + item.getElementName().getLocalPart(),
                        item, item);
            } else {
                messages.add("removed");
            }
        }

        if (summarizeItemLifecycleState && !messages.isEmpty()) {
            warn(
                    result, ValidationItemType.DEPRECATED_REMOVED_PLANNED_REMOVAL_ITEM,
                    StringUtils.join(messages, ", "), item, item);
        }

        if (item instanceof PrismProperty<?> property) {
            visitProperty(property, result);
        } else if (item instanceof PrismReference) {
            visitReference((PrismReference) item, result);
        } else if (item instanceof PrismContainer<?> container) {
            visitContainer(container, result);
        }
    }

    private void visitContainer(PrismContainer<?> container, ValidationResult result) {
        PrismContainerDefinition<?> def = container.getDefinition();
        if (def == null) {
            return;
        }

        if (!def.isMultiValue()) {
            return;
        }

        // todo enable natural keys check
        List<QName> constituents = def.getNaturalKeyConstituents();
        if (constituents == null || constituents.isEmpty()) {
            return;
        }

        NaturalKeyDefinition naturalKey = def.getNaturalKeyInstance();

        for (PrismContainerValue<?> value : container.getValues()) {
            if (check(ValidationItemType.MISSING_NATURAL_KEY)) {
                for (QName key : constituents) {
                    if (value.findItem(ItemPath.create(key)) == null) {
                        warn(
                                result, ValidationItemType.MISSING_NATURAL_KEY,
                                "Missing natural key constituent: " + key.getLocalPart(), container, value);
                    }
                }
            }

            if (check(ValidationItemType.NATURAL_KEY_NOT_UNIQUE) && naturalKey != null) {
                // this could be quite expensive, however now there's probably no better way to
                // figure out whether there are two values with the same natural key
                for (PrismContainerValue<?> other : container.getValues()) {
                    if (value == other) {
                        continue;
                    }

                    var key = naturalKey.getConstituents(value);
                    if (key == null || key.isEmpty()) {
                        continue;
                    }

                    if (naturalKey.valuesMatch(value, other)) {
                        warn(
                                result, ValidationItemType.NATURAL_KEY_NOT_UNIQUE,
                                "Non-unique natural key in " + container.getPath(), container, value);
                    }
                }
            }
        }
    }

    private void visitProperty(PrismProperty<?> property, ValidationResult result) {
        PrismPropertyDefinition<?> def = property.getDefinition();

        if (check(ValidationItemType.PROTECTED_DATA_NOT_EXTERNAL)
                && ProtectedStringType.COMPLEX_TYPE.equals(property.getDefinition().getTypeName())) {
            Class<?> type = def.getTypeClass();
            if (ProtectedDataType.class.isAssignableFrom(type)) {
                checkProtectedString(property, result);
            }
        }

        if (check(ValidationItemType.MULTIVALUE_BYTE_ARRAY)) {
            if (!def.isMultiValue()) {
                return;
            }

            if (!byte[].class.equals(def.getTypeClass())) {
                return;
            }

            warn(
                    result, ValidationItemType.MULTIVALUE_BYTE_ARRAY,
                    "Multi-value byte array in " + property.getPath(), property, null);
        }
    }

    private void checkProtectedString(PrismProperty<?> property, ValidationResult result) {
        ProtectedStringViolations violations = new ProtectedStringViolations();

        List<String> messages = new ArrayList<>();
        for (PrismPropertyValue<?> value : property.getValues()) {
            ProtectedDataType<?> ps = (ProtectedDataType<?>) value.getValue();
            if (ps == null) {
                continue;
            }

            if (ps.getEncryptedDataType() != null) {
                messages.add("encrypted data in " + property.getPath());
                violations.addEncrypted(property.getPath());
            }

            if (ps.getHashedDataType() != null) {
                messages.add("hashed data in " + property.getPath());
                violations.addHashed(property.getPath());
            }

            if (ps.getClearValue() != null) {
                messages.add("clear value in " + property.getPath());
                violations.addClearValue(property.getPath());
            }
        }

        if (messages.isEmpty()) {
            return;
        }

        warn(
                result, ValidationItemType.PROTECTED_DATA_NOT_EXTERNAL,
                "Protected string: " + StringUtils.join(messages, ", "), property, violations);
    }

    private void visitReference(PrismReference reference, ValidationResult result) {
        if (check(ValidationItemType.MULTIVALUE_REF_WITHOUT_OID)) {
            checkMultiValueReference(reference, result);
        }
    }

    private void checkMultiValueReference(PrismReference reference, ValidationResult result) {
        PrismReferenceDefinition def = reference.getDefinition();
        if (def == null || def.isSingleValue()) {
            return;
        }

        List<PrismReferenceValue> missingOids = reference.getValues().stream()
                .filter(v -> StringUtils.isEmpty(v.getOid()))
                .toList();

        ItemPath path = reference.getPath();

        for (PrismReferenceValue value : missingOids) {
            warn(
                    result, ValidationItemType.MULTIVALUE_REF_WITHOUT_OID,
                    String.format("Multi-value reference without oid on path %s", path), reference, value);
        }
    }

    public void checkOid(ValidationResult result, PrismValue item, String oid) {
        if (oid == null) {
            return;
        }
        try {
            UUID.fromString(oid);
        } catch (IllegalArgumentException e) {
            warn(result, ValidationItemType.INCORRECT_OID_FORMAT, "OID '" + oid + "' is not valid UUID", (Item<?, ?>) item.getParent(), item);
        }
    }

    public void checkOid(ValidationResult result, ItemPath item, String oid) {
        if (oid == null || !typesToCheck.contains(ValidationItemType.INCORRECT_OID_FORMAT)) {
            return;
        }
        try {
            UUID.fromString(oid);
        } catch (IllegalArgumentException e) {
            warn(result, ValidationItemType.INCORRECT_OID_FORMAT, "OID '" + oid + "' is not valid UUID", item, oid);
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void warn(
            ValidationResult result, ValidationItemType type, String message, Item<V, D> item, Object data) {
        msg(result, type, ValidationItemStatus.WARNING, message, item != null ? item.getPath() : null, data);
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void warn(
            ValidationResult result, ValidationItemType type, String message, ItemPath item, Object data) {
        msg(result, type, ValidationItemStatus.WARNING, message, item, data);
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void msg(
            ValidationResult result, ValidationItemType type, ValidationItemStatus status, String message, ItemPath path,
            Object data) {

        LocalizableMessage msg = new SingleLocalizableMessage(null, null, message);

        result.addItem(new ValidationItem<>(type, status, msg, path, data));
    }
}
