/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.impl.PrismReferenceImpl;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.SchemaException;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Represents a shadow reference attribute (like `ri:group` or `ri:access`). It is a reference to another shadow.
 *
 * It is a regular attribute, just like {@link ShadowSimpleAttribute}.
 *
 * The reference can be _native_ or _simulated_. The former is provided by the connector, while the latter is simulated
 * by the midPoint provisioning module.
 *
 * NOTE: This is a lower-level concept, present in resource-facing shadows and shadows stored in the repository.
 * For model-visible shadows, each such attribute is converted into a {@link ShadowAssociation}.
 * (Whether the value of this attribute will be kept in the shadow, is an open question.)
 *
 * @see ShadowReferenceAttributeDefinition
 */
public class ShadowReferenceAttribute
        extends PrismReferenceImpl
        implements ShadowAttribute<
        ShadowReferenceAttributeValue,
        ShadowReferenceAttributeDefinition,
        Referencable,
        ShadowReferenceAttribute
        > {

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
     * This method will clone the {@link Item} and convert it to a {@link ShadowReferenceAttribute} (if not already one).
     */
    static ShadowReferenceAttribute convertFromPrismItem(
            @NotNull Item<?, ?> item, @NotNull ShadowReferenceAttributeDefinition attrDef) {
//        var attr = new ShadowReferenceAttribute(item.getElementName(), attrDef);
//        for (PrismValue value : item.getValues()) {
//            if (value instanceof PrismReferenceValue refVal) {
//                try {
//                    attr.addIgnoringEquivalents(
//                            ShadowReferenceAttributeValue.of(
//                                    ((ShadowAssociationValueType) refVal.asContainerable()).clone()
//                            ));
//                } catch (SchemaException e) {
//                    throw new IllegalArgumentException("Couldn't add PCV: " + value, e);
//                }
//            } else {
//                throw new IllegalArgumentException("Not a PCV: " + value);
//            }
//        }
//        return attr;
        throw new UnsupportedOperationException("FIXME");
    }

//    @Override
//    protected boolean addInternalExecution(@NotNull PrismContainerValue<ShadowAssociationValueType> newValue) {
////        argCheck(newValue instanceof ShadowAssociationValue,
////                "Trying to add a value which is not a ShadowAssociationValue: %s", newValue);
//        if (newValue instanceof ShadowReferenceAttributeValue) {
//            return super.addInternalExecution(newValue);
//        } else {
//            // FIXME we should have resolved this (for deltas) in applyDefinition call, but that's not possible now
//            return super.addInternalExecution(ShadowReferenceAttributeValue.of(
//                    newValue.asContainerable(),
//                    getDefinitionRequired()));
//        }
//    }

//    @Override
//    public ShadowReferenceAttributeValue createNewValue() {
//        // Casting is safe here, as "createNewValueInternal" provides this type.
//        return (ShadowReferenceAttributeValue) super.createNewValue();
//    }

//    @Override
//    protected @NotNull PrismContainerValueImpl<ShadowAssociationValueType> createNewValueInternal() {
//        return ShadowReferenceAttributeValue.empty();
//    }

    public int size() {
        return values.size();
    }

    @Override
    protected String getDebugDumpClassName() {
        return "SA";
    }

    @SuppressWarnings("UnusedReturnValue")
    public @NotNull ShadowReferenceAttributeValue createNewValueWithIdentifiers(@NotNull AbstractShadow shadow) throws SchemaException {
        var value = ShadowReferenceAttributeValue.fromShadow(shadow);
        add(value);
        return value;
    }

    public @NotNull ShadowReferenceAttributeValue createNewValueWithIdentifier(@NotNull ShadowSimpleAttribute<?> identifier)
            throws SchemaException {
        var blankShadow = getDefinitionRequired()
                .getRepresentativeTargetObjectDefinition()
                .createBlankShadow();
        blankShadow.getAttributesContainer().add((ShadowAttribute<?, ?, ?, ?>) identifier);
        return createNewValueWithIdentifiers(blankShadow);
    }

    /** Creates a value holding the full object. Its definition must correspond to the one of the association. */
    public @NotNull ShadowReferenceAttributeValue createNewValueWithFullObject(@NotNull AbstractShadow target)
            throws SchemaException {
        // TODO check the definition
        var value = ShadowReferenceAttributeValue.fromShadow(target);
        add(value);
        return value;
    }

//    /** Adds only the target shadow ref. */
//    @SuppressWarnings("UnusedReturnValue")
//    public @NotNull ShadowReferenceAttributeValue createNewValueForTargetRef(@NotNull ObjectReferenceType ref) {
//        var value = createNewValue();
//        value.getValue().setShadowRef(ref);
//        return value;
//    }

    public @NotNull List<ShadowReferenceAttributeValue> getReferenceValues() {
        // IDE accepts the version without cast to List, but the compiler doesn't.
        //noinspection unchecked,rawtypes,RedundantCast
        return List.copyOf(
                (List<? extends ShadowReferenceAttributeValue>) (List) getValues());
    }

    @Override
    public void addValueSkipUniquenessCheck(ShadowReferenceAttributeValue value) throws SchemaException {
        addIgnoringEquivalents(value);
    }

    public boolean hasNoValues() {
        return getValues().isEmpty();
    }

    @Override
    public ShadowReferenceAttribute createImmutableClone() {
        return (ShadowReferenceAttribute) super.createImmutableClone();
    }

    public @NotNull ShadowReferenceAttributeValue getSingleValueRequired() {
        return MiscUtil.extractSingletonRequired(
                getReferenceValues(),
                () -> new IllegalStateException("Multiple values where only a single one was expected: " + this),
                () -> new IllegalStateException("Missing attribute value in " + this));
    }
}
