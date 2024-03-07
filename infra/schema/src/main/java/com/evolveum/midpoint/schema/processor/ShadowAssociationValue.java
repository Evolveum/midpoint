/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.io.Serial;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.impl.PrismContainerValueImpl;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContainerImpl;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

/**
 * Represents a specific shadow association value - i.e. something that is put into {@link ShadowAssociation}.
 * For example, a single group membership for a given account: `joe` is a member of `admins`.
 */
@Experimental
public class ShadowAssociationValue extends PrismContainerValueImpl<ShadowAssociationValueType> {

    @Serial private static final long serialVersionUID = 0L;

//    public ShadowAssociationValue(QName name, ShadowAssociationDefinition definition) {
//        super(name, definition, PrismContext.get());
//    }
//
//    /** TODO shouldn't be the definition always required? */
//    @Override
//    public ShadowAssociationDefinition getDefinition() {
//        return (ShadowAssociationDefinition) super.getDefinition();
//    }
//
//    public @NotNull ShadowAssociationDefinition getDefinitionRequired() {
//        return stateNonNull(
//                getDefinition(), "No definition in %s", this);
//    }
//
//    @Override
//    public ShadowAssociationValue cloneComplex(CloneStrategy strategy) {
//        ShadowAssociationValue clone = new ShadowAssociationValue(getElementName(), getDefinition());
//        copyValues(strategy, clone);
//        return clone;
//    }
//
//    /**
//     * This method will clone the item and convert it to a {@link ShadowAssociationValue}.
//     * (A typical use case is that the provided item is not a {@link ShadowAssociationValue}.)
//     *
//     * Currently, this method ignores the identifiers: they are used "as is". No eventual definition application,
//     * and conversion to resource attributes is done.
//     */
//    public static ShadowAssociationValue convertFromPrismItem(
//            @NotNull Item<?, ?> item, @NotNull ShadowAssociationDefinition associationDef) {
//        var association = new ShadowAssociationValue(item.getElementName(), associationDef);
//        for (PrismValue value : item.getValues()) {
//            if (value instanceof PrismContainerValue<?> pcv) {
//                try {
//                    //noinspection unchecked
//                    association.addIgnoringEquivalents((PrismContainerValue<ShadowAssociationValueType>) pcv.clone());
//                } catch (SchemaException e) {
//                    throw new IllegalArgumentException("Couldn't add PCV: " + value, e);
//                }
//            } else {
//                throw new IllegalArgumentException("Not a PCV: " + value);
//            }
//        }
//        return association;
//    }
//
//    public int size() {
//        return values.size();
//    }
//
//    @Override
//    protected String getDebugDumpClassName() {
//        return "SA";
//    }
//
//    @SuppressWarnings("UnusedReturnValue")
//    public @NotNull PrismContainerValue<ShadowAssociationValueType> createNewValueWithIdentifiers(
//            @NotNull ResourceAttributeContainer identifiers) throws SchemaException {
//        var value = createNewValue();
//        value.add(identifiers);
//        return value;
//    }
//
//    public @NotNull PrismContainerValue<ShadowAssociationValueType> createNewValueWithIdentifier(
//            @NotNull ResourceAttribute<?> identifier) throws SchemaException {
//        var identifiersContainer = getDefinitionRequired()
//                .getTargetObjectDefinition()
//                .toResourceAttributeContainerDefinition()
//                .instantiate(ShadowAssociationValueType.F_IDENTIFIERS);
//        identifiersContainer.add(identifier);
//        return createNewValueWithIdentifiers(identifiersContainer);
//    }
//
//    /** Adds both target shadow ref and identifiers. */
//    @SuppressWarnings("UnusedReturnValue")
//    public @NotNull PrismContainerValue<ShadowAssociationValueType> createNewValueForTarget(@NotNull AbstractShadow target)
//            throws SchemaException {
//        var value = createNewValue();
//        value.getValue().setShadowRef(target.getRef());
//        value.add(target.getIdentifiersAsContainer());
//        return value;
//    }
//
//    /** Adds only the target shadow ref. */
//    @SuppressWarnings("UnusedReturnValue")
//    public @NotNull PrismContainerValue<ShadowAssociationValueType> createNewValueForTargetRef(@NotNull ObjectReferenceType ref) {
//        var value = createNewValue();
//        value.getValue().setShadowRef(ref);
//        return value;
//    }
}
