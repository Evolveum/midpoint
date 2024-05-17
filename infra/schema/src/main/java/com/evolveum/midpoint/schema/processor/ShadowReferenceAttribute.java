/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.impl.PrismContainerValueImpl;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.impl.PrismContainerImpl;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Object representing a specific shadow association (like `ri:group`). Similar to a {@link ShadowSimpleAttribute}.
 * Contained in {@link ShadowAssociationsContainer}.
 */
@Experimental
public class ShadowReferenceAttribute
        extends PrismContainerImpl<ShadowAssociationValueType>
        implements ShadowAttribute<ShadowAssociationValue, ShadowAssociationValueType> {

    @Serial private static final long serialVersionUID = 0L;

    public ShadowReferenceAttribute(QName name, ShadowReferenceAttributeDefinition definition) {
        super(name, definition);
    }

    /** TODO shouldn't be the definition always required? */
    @Override
    public ShadowReferenceAttributeDefinition getDefinition() {
        return (ShadowReferenceAttributeDefinition) super.getDefinition();
    }

    public @NotNull ShadowReferenceAttributeDefinition getDefinitionRequired() {
        return stateNonNull(
                getDefinition(), "No definition in %s", this);
    }

    @Override
    public ShadowReferenceAttribute clone() {
        return (ShadowReferenceAttribute) super.clone();
    }

    @Override
    public ShadowReferenceAttribute cloneComplex(CloneStrategy strategy) {
        ShadowReferenceAttribute clone = new ShadowReferenceAttribute(getElementName(), getDefinition());
        copyValues(strategy, clone);
        return clone;
    }

    /**
     * This method will clone the item and convert it to a {@link ShadowReferenceAttribute}.
     * (A typical use case is that the provided item is not a {@link ShadowReferenceAttribute}.)
     */
    static ShadowReferenceAttribute convertFromPrismItem(
            @NotNull Item<?, ?> item, @NotNull ShadowReferenceAttributeDefinition associationDef) {
        var association = new ShadowReferenceAttribute(item.getElementName(), associationDef);
        for (PrismValue value : item.getValues()) {
            if (value instanceof PrismContainerValue<?> pcv) {
                try {
                    association.addIgnoringEquivalents(
                            ShadowAssociationValue.of(
                                    ((ShadowAssociationValueType) pcv.asContainerable()).clone(),
                                    associationDef));
                } catch (SchemaException e) {
                    throw new IllegalArgumentException("Couldn't add PCV: " + value, e);
                }
            } else {
                throw new IllegalArgumentException("Not a PCV: " + value);
            }
        }
        return association;
    }

    @Override
    protected boolean addInternalExecution(@NotNull PrismContainerValue<ShadowAssociationValueType> newValue) {
//        argCheck(newValue instanceof ShadowAssociationValue,
//                "Trying to add a value which is not a ShadowAssociationValue: %s", newValue);
        if (newValue instanceof ShadowAssociationValue) {
            return super.addInternalExecution(newValue);
        } else {
            // FIXME we should have resolved this (for deltas) in applyDefinition call, but that's not possible now
            return super.addInternalExecution(ShadowAssociationValue.of(
                    newValue.asContainerable(),
                    getDefinitionRequired()));
        }
    }

    @Override
    public ShadowAssociationValue createNewValue() {
        // Casting is safe here, as "createNewValueInternal" provides this type.
        return (ShadowAssociationValue) super.createNewValue();
    }

    @Override
    protected @NotNull PrismContainerValueImpl<ShadowAssociationValueType> createNewValueInternal() {
        return ShadowAssociationValue.empty();
    }

    public int size() {
        return values.size();
    }

    @Override
    protected String getDebugDumpClassName() {
        return "SA";
    }

    @SuppressWarnings("UnusedReturnValue")
    public @NotNull ShadowAssociationValue createNewValueWithIdentifiers(@NotNull AbstractShadow shadow) throws SchemaException {
        var value = ShadowAssociationValue.of(shadow, true);
        add(value);
        return value;
    }

    public @NotNull ShadowAssociationValue createNewValueWithIdentifier(@NotNull ShadowSimpleAttribute<?> identifier) throws SchemaException {
        var blankShadow = getDefinitionRequired()
                .getRepresentativeTargetObjectDefinition()
                .createBlankShadow();
        blankShadow.getAttributesContainer().add(identifier);
        return createNewValueWithIdentifiers(blankShadow);
    }

    /** Creates a value holding the full object. Its definition must correspond to the one of the association. */
    @SuppressWarnings("UnusedReturnValue")
    public @NotNull ShadowAssociationValue createNewValueWithFullObject(@NotNull AbstractShadow target)
            throws SchemaException {
        // TODO check the definition
        var value = ShadowAssociationValue.of(target, false);
        add(value);
        return value;
    }

    /** Adds only the target shadow ref. */
    @SuppressWarnings("UnusedReturnValue")
    public @NotNull PrismContainerValue<ShadowAssociationValueType> createNewValueForTargetRef(@NotNull ObjectReferenceType ref) {
        var value = createNewValue();
        value.getValue().setShadowRef(ref);
        return value;
    }

    public @NotNull List<? extends ShadowAssociationValue> getAssociationValues() {
        // IDE accepts the version without cast to List, but the compiler doesn't.
        //noinspection unchecked,rawtypes,RedundantCast
        return Collections.unmodifiableList(
                (List<? extends ShadowAssociationValue>) (List) getValues());
    }

    @Override
    public void addValueSkipUniquenessCheck(ShadowAssociationValue value) throws SchemaException {
        addIgnoringEquivalents(value);
    }

    public boolean hasNoValues() {
        return getValues().isEmpty();
    }
}
