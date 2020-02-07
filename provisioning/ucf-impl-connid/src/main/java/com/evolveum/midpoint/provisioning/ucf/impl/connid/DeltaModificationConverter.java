/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeDeltaBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
public class DeltaModificationConverter extends AbstractModificationConverter {

    private Set<AttributeDelta> attributesDelta = new HashSet<>();

    public Set<AttributeDelta> getAttributesDelta() {
        return attributesDelta;
    }

    @Override
    protected <T> void collect(String connIdAttrName, PropertyDelta<T> delta, PlusMinusZero isInModifiedAuxilaryClass, CollectorValuesConverter<T> valuesConverter) throws SchemaException {
        AttributeDeltaBuilder deltaBuilder = new AttributeDeltaBuilder();
        deltaBuilder.setName(connIdAttrName);
        if (delta.isAdd()) {
            List<Object> connIdAttributeValues = valuesConverter.covertAttributeValuesToConnId(delta.getValuesToAdd(), delta.getElementName());
            if (delta.getDefinition().isMultiValue()) {
                deltaBuilder.addValueToAdd(connIdAttributeValues);
            } else {
                // Force "update" for single-valued attributes instead of "add". This is saving one
                // read in some cases. It should also make no substantial difference in such case.
                // But it is working around some connector bugs.
                deltaBuilder.addValueToReplace(connIdAttributeValues);
            }
        }
        if (delta.isDelete()) {
            if (delta.getDefinition().isMultiValue() || isInModifiedAuxilaryClass == PlusMinusZero.MINUS) {
                List<Object> connIdAttributeValues = valuesConverter.covertAttributeValuesToConnId(delta.getValuesToDelete(), delta.getElementName());
                deltaBuilder.addValueToRemove(connIdAttributeValues);
            } else {
                // Force "update" for single-valued attributes instead of "add". This is saving one
                // read in some cases.
                // Update attribute to no values. This will efficiently clean up the attribute.
                // It should also make no substantial difference in such case.
                // But it is working around some connector bugs.
                // update with EMPTY value. The connIdAttributeValues is NOT used in this branch
                // Explicitly replace with empty list. Passing null here would mean "no replace in this delta".
                deltaBuilder.addValueToReplace(Collections.EMPTY_LIST);
            }
        }
        if (delta.isReplace()) {
            List<Object> connIdAttributeValues = valuesConverter.covertAttributeValuesToConnId(delta.getValuesToReplace(), delta.getElementName());
            if (isInModifiedAuxilaryClass == PlusMinusZero.PLUS) {
                deltaBuilder.addValueToAdd(connIdAttributeValues);
            } else {
                deltaBuilder.addValueToReplace(connIdAttributeValues);
            }
        }
        attributesDelta.add(deltaBuilder.build());
    }

    @Override
    protected <T> void collectReplace(String connIdAttrName, T connIdAttrValue) throws SchemaException {
        if (connIdAttrValue == null) {
            // Explicitly replace with empty list. Passing null here would mean "no replace in this delta".
            attributesDelta.add(AttributeDeltaBuilder.build(connIdAttrName, Collections.EMPTY_LIST));
        } else {
            attributesDelta.add(AttributeDeltaBuilder.build(connIdAttrName, connIdAttrValue));
        }
    }

    @Override
    protected void collectPassword(PropertyDelta<ProtectedStringType> passwordDelta) throws SchemaException {
        if (isSelfPasswordChange(passwordDelta)) {
            AttributeDeltaBuilder deltaBuilder = new AttributeDeltaBuilder();
            deltaBuilder.setName(OperationalAttributes.PASSWORD_NAME);

            PrismProperty<ProtectedStringType> newPasswordProperty = passwordDelta.getPropertyNewMatchingPath();
            GuardedString newPasswordGs = passwordToGuardedString(newPasswordProperty.getRealValue(), "new password");
            deltaBuilder.addValueToAdd(newPasswordGs);

            ProtectedStringType oldPasswordPs = passwordDelta.getEstimatedOldValues().iterator().next().getRealValue();
            GuardedString oldPasswordGs = passwordToGuardedString(oldPasswordPs, "old password");
            deltaBuilder.addValueToRemove(oldPasswordGs);

            attributesDelta.add(deltaBuilder.build());
        } else {
            super.collectPassword(passwordDelta);
        }
    }

    protected void collectPasswordDelta(String connIdAttrName, PropertyDelta<ProtectedStringType> delta, PlusMinusZero isInModifiedAuxilaryClass, CollectorValuesConverter<ProtectedStringType> valuesConverter) throws SchemaException {
        AttributeDeltaBuilder deltaBuilder = new AttributeDeltaBuilder();
        deltaBuilder.setName(connIdAttrName);
        List<Object> newPasswordConnIdValues = valuesConverter.covertAttributeValuesToConnId(delta.getValuesToReplace(), delta.getElementName());
        if (isSelfPasswordChange(delta)) {
            // Self-service password *change*
            List<Object> oldPasswordConnIdValues = valuesConverter.covertAttributeValuesToConnId(delta.getEstimatedOldValues(), delta.getElementName());
            deltaBuilder.addValueToAdd(newPasswordConnIdValues);
            deltaBuilder.addValueToRemove(oldPasswordConnIdValues);
        } else {
            // Password *reset*
            deltaBuilder.addValueToReplace(newPasswordConnIdValues);
        }
        attributesDelta.add(deltaBuilder.build());
    }

    private boolean isSelfPasswordChange(PropertyDelta<ProtectedStringType> delta) {
        // We need runAs option, otherwise this is no self-service but an administrator setting the password.
        if (getOptions() == null) {
            return false;
        }
        if (getOptions().getRunAsIdentification() == null) {
            return false;
        }

        Collection<PrismPropertyValue<ProtectedStringType>> estimatedOldValues = delta.getEstimatedOldValues();
        if (estimatedOldValues == null || estimatedOldValues.isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    protected void debugDumpOutput(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "attributesDelta", attributesDelta, indent + 1);
    }
}
