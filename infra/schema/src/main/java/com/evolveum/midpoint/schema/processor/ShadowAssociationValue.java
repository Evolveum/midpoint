/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.impl.PrismContainerValueImpl;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ObjectOperationPolicyTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Represents a specific shadow association value - i.e. something that is put into {@link ShadowReferenceAttribute}.
 * For example, a single group membership for a given account: `joe` is a member of `admins`.
 *
 * NOTE: As an experiment, we try to keep instances as consistent as possible. E.g., we require correct `shadowRef` etc.
 * Any places where this is checked, will throw {@link IllegalStateException} instead of {@link SchemaException}.
 * We will simply not allow creating a non-compliant association object. At least we'll try to do this.
 * The exception are situations where the object exists between instantiation and providing the data.
 *
 * *Instantiation*
 *
 * In particular, we must provide reasonable CTD when instantiating this object.
 * Otherwise, {@link PrismContainerValue#asContainerable()} will fail.
 *
 * TODO check if it's possible to implement this approach regarding createNewValue in ShadowAssociation
 */
@Experimental
public class ShadowAssociationValue extends PrismContainerValueImpl<ShadowAssociationValueType>
        implements ShadowLikeValue {

    @Serial private static final long serialVersionUID = 0L;

    private static final EqualsChecker<ShadowAssociationValue> SEMANTIC_EQUALS_CHECKER =
            (o1, o2) -> {
                if (o1 == null || o2 == null) {
                    return o1 == null && o2 == null;
                }

                if (!ShadowUtil.simpleAttributesEqualRelaxed(o1.getAttributes(), o2.getAttributes())) {
                    return false;
                }

                if (!Objects.equals(o1.getActivation(), o2.getActivation())) { // TODO less strict comparison)
                    return false;
                }

                return MiscUtil.unorderedCollectionEquals(
                        o1.getObjectReferences(),
                        o2.getObjectReferences(),
                        ShadowReferenceAttribute.semanticEqualsChecker());
            };

    @NotNull private ShadowAssociationDefinition definition;

    // FIXME decide on this
    private final boolean hasAssociationObject;

    /**
     * For complex associations, this is the association object itself: except attributes and activation.
     *
     * Ignored by equals/hashCode for now.
     *
     * Experimental: to be decided if this is the right approach.
     */
    private ShadowType associationObjectExtraItems;

    private ShadowAssociationValue(
            OriginType type, Objectable source,
            PrismContainerable<?> container, Long id,
            @NotNull ShadowAssociationDefinition definition) {
        super(type, source, container, id, definition.getComplexTypeDefinition());
        this.definition = definition;
        this.hasAssociationObject = definition.isComplex();
    }

    /**
     * Converts association value bean to wrapped {@link ShadowAssociationValue} basically by cloning its content
     * and selected properties (e.g., parent and ID).
     *
     * We should not use the original value any more, e.g. because of the copied "parent" value.
     */
    public static @NotNull ShadowAssociationValue fromBean(
            @NotNull ShadowAssociationValueType bean,
            @NotNull ShadowAssociationDefinition definition) throws SchemaException {
        PrismContainerValue<?> pcv = bean.asPrismContainerValue();
        if (pcv instanceof ShadowAssociationValue shadowAssociationValue) {
            return shadowAssociationValue;
        }

        var newValue = empty(definition);
        if (definition.isComplex()) {
            var applicator = ShadowDefinitionApplicator.strict(definition.getAssociationDataObjectDefinition());

            var rawAttributes = bean.getAttributes();
            if (rawAttributes != null) {
                var newAttributesContainer = newValue.getOrCreateAttributesContainer();
                for (var rawItem : rawAttributes.asPrismContainerValue().getItems()) {
                    if (rawItem instanceof ShadowSimpleAttribute<?> attribute) {
                        newAttributesContainer.addAttribute(attribute.clone());
                    } else {
                        newAttributesContainer.addAttribute(
                                applicator.applyToItem((Item<?, ?>) rawItem));
                    }
                }
            }

            var rawObjects = bean.getObjects();
            if (rawObjects != null && !rawObjects.asPrismContainerValue().hasNoItems()) {
                var newObjectsContainer = newValue.getOrCreateObjectsContainer();
                for (var rawItem : rawObjects.asPrismContainerValue().getItems()) {
                    if (rawItem instanceof ShadowReferenceAttribute attribute) {
                        newObjectsContainer.addAttribute(attribute.clone());
                    } else {
                        newObjectsContainer.addAttribute(
                                applicator.applyToItem((Item<?, ?>) rawItem));
                    }
                }
            }

            newValue.asContainerable().setActivation(
                    CloneUtil.cloneCloneable(
                            bean.getActivation()));

        } else {

            //noinspection unchecked
            var objectRefItem = (Item<?, ?>)
                    MiscUtil.extractSingletonRequired(
                            bean.getObjects().asPrismContainerValue().getItems(),
                            () -> new SchemaException("Multiple object references in an association value: " + bean),
                            () -> new SchemaException("No object reference in an association value: " + bean));
            var objectRef = MiscUtil.castSafely(objectRefItem, PrismReference.class);
            var objectRefValue =
                    MiscUtil.extractSingletonRequired(
                            objectRef.getValues(),
                            () -> new SchemaException("Multiple value in an association object reference: " + bean),
                            () -> new SchemaException("No value in an association object reference: " + bean));

            newValue.getOrCreateObjectsContainer()
                    .findOrCreateReferenceAttribute(definition.getReferenceAttributeDefinition().getItemName())
                    .add(ShadowReferenceAttributeValue.fromRefValue(objectRefValue.clone()));

        }
        newValue.setId(pcv.getId());
        return newValue;
    }

    /** Creates a new value from the association object. */
    public static @NotNull ShadowAssociationValue fromAssociationDataObject(
            @NotNull AbstractShadow associationObject,
            @NotNull ShadowAssociationDefinition associationDefinition) throws SchemaException {
        var newValue = empty(associationDefinition);
        newValue.fillFromAssociationObject(associationObject);
        return newValue;
    }

    /** We need the definition to provide correct CTD. */
    public static ShadowAssociationValue empty(@NotNull ShadowAssociationDefinition definition) {
        return new ShadowAssociationValue(
                null, null, null, null, definition);
    }

    @Override
    public ShadowAssociationValue clone() {
        return (ShadowAssociationValue) super.clone();
    }

    @Override
    public ShadowAssociationValue cloneComplex(CloneStrategy strategy) {
        ShadowAssociationValue clone = new ShadowAssociationValue(
                getOriginType(), getOriginObject(), getParent(), null, definition);
        copyValues(strategy, clone);
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, ShadowAssociationValue clone) {
        super.copyValues(strategy, clone);
        clone.definition = this.definition;
        clone.associationObjectExtraItems = CloneUtil.cloneCloneable(this.associationObjectExtraItems);
    }

    @Override
    public @NotNull PrismContainerDefinition<ShadowAssociationValueType> getDefinition() {
        return Objects.requireNonNull(definition);
    }

    public @NotNull ShadowAssociationDefinition getDefinitionRequired() {
        return stateNonNull(definition, "No definition in %s", this);
    }

    @Nullable
    private ResourceObjectDefinition getAssociatedObjectDefinitionIfPresent() {
        return null; // TODO implement
    }

    @Override
    protected boolean appendExtraHeaderDump(StringBuilder sb, int indent, boolean wasIndent) {
        wasIndent = super.appendExtraHeaderDump(sb, indent, wasIndent);
        if (!wasIndent) {
            DebugUtil.indentDebugDump(sb, indent);
        } else {
            sb.append("; ");
        }
        // TODO this should be a part of ShadowType dumping; but that code is automatically generated for now
        sb.append(getAssociatedObjectDefinitionIfPresent());
        return true;
    }

    public @Nullable ShadowAttributesContainer getAttributesContainer() {
        return ShadowUtil.castShadowContainer(
                this, ShadowAssociationValueType.F_ATTRIBUTES, ShadowAttributesContainer.class);
    }

    public @NotNull ShadowAttributesContainer getAttributesContainerRequired() {
        return stateNonNull(getAttributesContainer(), "No attributes container in %s", this);
    }

    /** Note that these attributes are always simple. */
    public @NotNull Collection<ShadowSimpleAttribute<?>> getAttributes() {
        var container = getAttributesContainer();
        return container != null ? container.getSimpleAttributes() : List.of();
    }

    public @NotNull ShadowAttributesContainer getOrCreateAttributesContainer() {
        try {
            return MiscUtil.castSafely(
                    this.<ShadowReferenceAttributesType>findOrCreateContainer(ShadowAssociationValueType.F_ATTRIBUTES),
                    ShadowAttributesContainer.class);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e);
        }
    }

    public @NotNull ShadowAttributesContainer getObjectsContainerRequired() {
        return stateNonNull(getObjectsContainer(), "No objects container in %s", this);
    }

    public @Nullable ShadowAttributesContainer getObjectsContainer() {
        return ShadowUtil.castShadowContainer(
                this, ShadowAssociationValueType.F_OBJECTS, ShadowAttributesContainer.class);
    }

    public @NotNull Collection<ShadowReferenceAttribute> getObjectReferences() {
        var container = getObjectsContainer();
        return container != null ? container.getReferenceAttributes() : List.of();
    }

    public @NotNull ShadowAttributesContainer getOrCreateObjectsContainer() {
        try {
            return MiscUtil.castSafely(
                    this.<ShadowReferenceAttributesType>findOrCreateContainer(ShadowAssociationValueType.F_OBJECTS),
                    ShadowAttributesContainer.class);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e);
        }
    }

    public @NotNull ObjectReferenceType getSingleObjectRefRequired() {
        return getSingleObjectRefValueRequired().asObjectReferenceType();
    }

    public @Nullable ObjectReferenceType getSingleObjectRefRelaxed() {
        var objectsContainer = getObjectsContainer();
        if (objectsContainer == null) {
            return null;
        }
        var objectRefs = objectsContainer.getReferenceAttributes();
        if (objectRefs.size() != 1) {
            return null;
        }
        var objectRefValues = objectRefs.iterator().next().getReferenceValues();
        if (objectRefValues.size() != 1) {
            return null;
        }
        return objectRefValues.iterator().next().asObjectReferenceType();
    }

    public @NotNull ShadowReferenceAttributeValue getSingleObjectRefValueRequired() {
        var refAttr = MiscUtil.extractSingletonRequired(
                getObjectsContainerRequired().getReferenceAttributes(),
                () -> new IllegalStateException("Multiple object reference attributes in " + this),
                () -> new IllegalStateException("No object reference attributes in " + this));
        return (ShadowReferenceAttributeValue) refAttr.getValue();
    }

    public @NotNull AbstractShadow getSingleObjectShadowRequired() {
        return getSingleObjectRefValueRequired().getShadowRequired();
    }

    public @NotNull ShadowType getSingleObjectShadowBeanRequired() {
        return getSingleObjectShadowRequired().getBean();
    }

    /**
     * Converts this value into the low-level representation by a reference attribute.
     * Returns a free (parent-less) object.
     */
    public @NotNull ShadowReferenceAttributeValue toReferenceAttributeValue() throws SchemaException {
        var def = getDefinitionRequired();
        if (def.isComplex()) {
            var shadow = def.getAssociationDataObjectDefinition().createBlankShadow();
            // We do not preserve the OID, kind/intent and similar things.
            copy(shadow.getAttributesContainer(), getAttributesContainer());
            copy(shadow.getAttributesContainer(), getObjectsContainer());
            shadow.getBean().setActivation(
                    CloneUtil.cloneCloneable(
                            asContainerable().getActivation()));
            // TODO review this
            if (associationObjectExtraItems != null) {
                shadow.getBean().setEffectiveOperationPolicy(
                        CloneUtil.cloneCloneable(associationObjectExtraItems.getEffectiveOperationPolicy()));
            }
            return ShadowReferenceAttributeValue.fromShadow(shadow, true);
        } else {
            return getSingleObjectRefValueRequired().clone();
        }
    }

    @TestOnly
    public @NotNull ShadowReferenceAttributeValue toReferenceAttributeValueUnchecked() {
        try {
            return toReferenceAttributeValue();
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
    }

    private void copy(@NotNull ShadowAttributesContainer target, @Nullable ShadowAttributesContainer source)
            throws SchemaException {
        if (source != null) {
            copy(target, source.getAttributes());
        }
    }

    private static void copy(
            @NotNull ShadowAttributesContainer target, @NotNull Collection<? extends ShadowAttribute<?, ?, ?, ?>> attributes)
            throws SchemaException {
        for (var attribute : attributes) {
            target.addAttribute(attribute.clone());
        }
    }

    /**
     * Fills-in this value from a (fully resolved) {@link ShadowReferenceAttributeValue}.
     * This is an inversion of {@link #toReferenceAttributeValue()}.
     */
    @SuppressWarnings("UnusedReturnValue")
    public ShadowAssociationValue fillFromReferenceAttributeValue(@NotNull ShadowReferenceAttributeValue refAttrValue)
            throws SchemaException {
        var def = getDefinitionRequired();
        if (def.isComplex()) {
            return fillFromAssociationObject(refAttrValue.getShadow());
        } else {
            getOrCreateObjectsContainer()
                    .addReferenceAttribute(
                            def.getReferenceAttributeDefinition().getItemName(),
                            refAttrValue.getShadow(),
                            refAttrValue.isFullObject());
            return this;
        }
    }

    ShadowAssociationValue fillFromAssociationObject(AbstractShadow associationObject) throws SchemaException {
        var simpleAttributes = associationObject.getSimpleAttributes();
        if (!simpleAttributes.isEmpty()) {
            copy(getOrCreateAttributesContainer(), simpleAttributes);
        }
        var referenceAttributes = associationObject.getReferenceAttributes();
        if (!referenceAttributes.isEmpty()) {
            // assuming all are object refs
            copy(getOrCreateObjectsContainer(), referenceAttributes);
        }
        asContainerable().setActivation(
                CloneUtil.cloneCloneable(
                        associationObject.getBean().getActivation()));
        // TODO implement more nicely
        associationObjectExtraItems = associationObject.getBean().clone();
        associationObjectExtraItems.setAttributes(null);
        associationObjectExtraItems.setActivation(null);
        return this;
    }

    @Override
    protected boolean equalsItems(
            PrismContainerValue<ShadowAssociationValueType> other, ParameterizedEquivalenceStrategy strategy) {
        // BRUTAL HACK; the idea is that we want to compare the values semantically e.g. when doing IDI->triple conversion
        if (!(other instanceof ShadowAssociationValue otherSav)
                || strategy.isLiteralDomComparison()
                || strategy.isConsideringOperationalData()) {
            return super.equalsItems(other, strategy);
        }
        return semanticEqualsChecker().test(this, otherSav);
    }

    /** TODO better name */
    public static @NotNull EqualsChecker<ShadowAssociationValue> semanticEqualsChecker() {
        return SEMANTIC_EQUALS_CHECKER;
    }

    public boolean matches(ShadowAssociationValue other) {
        return semanticEqualsChecker().test(this, other);
    }

    public ActivationType getActivation() {
        return asContainerable().getActivation();
    }

    /**
     * Returns the associated object as an {@link AbstractShadow}.
     * Fails if there's none.
     */
    public @NotNull AbstractShadow getAssociationDataObject() {
        try {
            if (hasAssociationObject) {
                return toReferenceAttributeValue().getShadowRequired();
            } else {
                throw new IllegalStateException("No association object in " + this);
            }
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
    }

    public boolean hasAssociationObject() {
        return hasAssociationObject;
    }

    public boolean isInboundMembershipSynchronizationDisabled() {
        if (hasAssociationObject) {
            return false; // Not supported for complex associations
        } else {
            return ObjectOperationPolicyTypeUtil.isMembershipSyncInboundDisabled(
                    getSingleObjectShadowRequired().getEffectiveOperationPolicyRequired());
        }
    }

    @Override
    public String debugDump(int indent) {
        var sb = new StringBuilder(super.debugDump(indent));
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "Has association object", hasAssociationObject, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Association object extra items", associationObjectExtraItems, indent + 1);
        return sb.toString();
    }
}
