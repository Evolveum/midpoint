/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContainerValueImpl;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Represents a specific shadow association value - i.e. something that is put into {@link ShadowAssociation}.
 * For example, a single group membership for a given account: `joe` is a member of `admins`.
 *
 * NOTE: As an experiment, we try to keep instances as consistent as possible. E.g., we require correct `shadowRef` etc.
 * Any places where this is checked, will throw {@link IllegalStateException} instead of {@link SchemaException}.
 * We will simply not allow creating a non-compliant association object. At least we'll try to do this.
 * The exception are situations where the object exists between instantiation and providing the data.
 *
 * TODO check if it's possible to implement this approach regarding createNewValue in ShadowAssociation
 */
@Experimental
public class ShadowAssociationValue extends PrismContainerValueImpl<ShadowAssociationValueType> {

    @Serial private static final long serialVersionUID = 0L;

    private ShadowAssociationValue() {
        super();
    }

    private ShadowAssociationValue(
            OriginType type, Objectable source,
            PrismContainerable<?> container, Long id,
            ComplexTypeDefinition complexTypeDefinition) {
        super(type, source, container, id, complexTypeDefinition);
    }

    /**
     * Converts association value bean to wrapped {@link ShadowAssociationValue} basically by cloning its content
     * and selected properties (e.g., parent and ID).
     *
     * We should not use the original value any more, e.g. because of the copied "parent" value.
     */
    public static @NotNull ShadowAssociationValue of(@NotNull ShadowAssociationValueType bean) {
        PrismContainerValue<?> pcv = bean.asPrismContainerValue();
        var newValue = new ShadowAssociationValue();
        try {
            newValue.addAll(
                    CloneUtil.cloneCollectionMembers(
                            pcv.getItems()));
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when transferring association value items to a SAV");
        }
        newValue.setParent(pcv.getParent()); // TODO maybe temporary?
        newValue.setId(pcv.getId());
        return newValue;
    }

    /** Creates a new value from the full or ID-only shadow. No cloning here. */
    public static @NotNull ShadowAssociationValue of(@NotNull AbstractShadow shadow) {
        var newValue = empty();
        var shadowRef = PrismContext.get().getSchemaRegistry()
                .findComplexTypeDefinitionByCompileTimeClass(ShadowAssociationValueType.class)
                .findReferenceDefinition(ShadowAssociationValueType.F_SHADOW_REF)
                .instantiate();
        try {
            shadowRef.add(
                    shadow.getRefWithEmbeddedObject().asReferenceValue());
            newValue.add(shadowRef);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when adding shadowRef to a SAV");
        }
        return newValue;
    }

    public static ShadowAssociationValue empty() {
        return new ShadowAssociationValue();
    }

    public static ShadowAssociationValue withReferenceTo(@NotNull String targetOid) {
        return of(
                new ShadowAssociationValueType()
                        .shadowRef(targetOid, ShadowType.COMPLEX_TYPE));
    }

    @Override
    public ShadowAssociationValue cloneComplex(CloneStrategy strategy) {
        ShadowAssociationValue clone = new ShadowAssociationValue(
                getOriginType(), getOriginObject(), getParent(), null, this.complexTypeDefinition);
        copyValues(strategy, clone);
        return clone;
    }

    public @NotNull ResourceAttributeContainer getAttributesContainer() {
        return ShadowUtil.getAttributesContainerRequired(getShadowBean());
    }

    public @NotNull ShadowAssociationsContainer getAssociationsContainer() {
        return ShadowUtil.getAssociationsContainerRequired(getShadowBean());
    }

    /** Target object or its reference. TODO better name. */
    public ShadowType getShadowBean() {
        var shadowRef = MiscUtil.stateNonNull(asContainerable().getShadowRef(), "No shadowRef in %s", this);
        return (ShadowType) MiscUtil.stateNonNull(shadowRef.getObjectable(), "No shadow in %s", this);
    }

    public QName getTargetObjectClassName() {
        return getAttributesContainer().getDefinitionRequired().getTypeName();
    }

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
