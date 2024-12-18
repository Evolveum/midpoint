/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.io.Serial;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.impl.PrismReferenceImpl;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

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
        ShadowReferenceAttribute> {

    @Serial private static final long serialVersionUID = 0L;

    private static final EqualsChecker<ShadowReferenceAttribute> SEMANTIC_EQUALS_CHECKER =
            (o1, o2) -> {
                if (o1 == null || o2 == null) {
                    return o1 == null && o2 == null;
                }
                return MiscUtil.unorderedCollectionEquals(
                        o1.getReferenceValues(),
                        o2.getReferenceValues(),
                        ShadowReferenceAttributeValue.semanticEqualsChecker());
            };

    public ShadowReferenceAttribute(QName name, ShadowReferenceAttributeDefinition definition) {
        super(name, definition);
    }

    public static EqualsChecker<ShadowReferenceAttribute> semanticEqualsChecker() {
        return SEMANTIC_EQUALS_CHECKER;
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

    public int size() {
        return values.size();
    }

    @Override
    protected String getDebugDumpClassName() {
        return "SA";
    }

    /** Creates a value holding the shadow. Its definition must correspond to the one of the association. */
    @SuppressWarnings("UnusedReturnValue")
    public @NotNull ShadowReferenceAttributeValue createNewValueFromShadow(@NotNull AbstractShadow shadow) throws SchemaException {
        // TODO check the definition
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
        blankShadow.setIdentificationOnly();
        return createNewValueFromShadow(blankShadow);
    }

    public @NotNull List<ShadowReferenceAttributeValue> getReferenceValues() {
        // IDE accepts the version without cast to List, but the compiler doesn't.
        //noinspection unchecked,rawtypes,RedundantCast
        return List.copyOf(
                (List<? extends ShadowReferenceAttributeValue>) (List) getValues());
    }

    public @NotNull List<ObjectReferenceType> getReferenceRealValues() {
        return getReferenceValues().stream()
                .map(refVal -> refVal.asObjectReferenceType())
                .toList();
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

    public void applyDefinitionFrom(@NotNull ResourceObjectDefinition objectDefinition)
            throws SchemaException {
        applyDefinition(
                objectDefinition.findReferenceAttributeDefinitionRequired(getElementName()));
    }

    /** Creates a delta that would enforce (via REPLACE operation) the values of this attribute. */
    public @NotNull ReferenceDelta createReplaceDelta() {
        var delta = getDefinitionRequired().createEmptyDelta();
        delta.setValuesToReplace(
                CloneUtil.cloneCollectionMembers(getValues()));
        return delta;
    }
}
