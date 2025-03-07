/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Either a resource object, or a repository shadow (after being adopted by provisioning, i.e. with the definitions applied).
 *
 * Conditions:
 *
 * . the definition is known ({@link #getObjectDefinition()})
 * . the bean has the definitions applied
 * . the bean has attributes container (which can be empty)
 *
 * See {@link #checkConsistence()}.
 */
@SuppressWarnings("unused") // until the API stabilizes
public interface AbstractShadow extends ShadowLikeValue, ShortDumpable, DebugDumpable, Cloneable {

    static AbstractShadow of(@NotNull ShadowType bean) {
        return new Impl(bean);
    }

    static AbstractShadow of(@NotNull PrismObject<ShadowType> prismObject) {
        return new Impl(prismObject.asObjectable());
    }

    /**
     * Returns the {@link ShadowType} bean backing this object.
     *
     * It should meet the criteria for individual subtypes.
     */
    @NotNull ShadowType getBean();

    default @Nullable String getOid() {
        return getBean().getOid();
    }

    default @NotNull String getOidRequired() {
        return stateNonNull(getBean().getOid(), "No OID in %s", this);
    }

    /** Currently, returns "plain" reference (only type + OID). This may change in the future. Returns null if there's no OID. */
    default @Nullable ObjectReferenceType getRef() {
        var oid = getOid();
        return oid != null ? ObjectTypeUtil.createObjectRef(oid, ObjectTypes.SHADOW) : null;
    }

    default @NotNull ObjectReferenceType getRefWithEmbeddedObject() {
        return ObjectTypeUtil.createObjectRefWithFullObject(getBean());
    }

    /** Returns the definition corresponding to this shadow. */
    default @NotNull ResourceObjectDefinition getObjectDefinition() {
        return ShadowUtil.getResourceObjectDefinition(getBean());
    }

    default @NotNull PrismObject<ShadowType> getPrismObject() {
        return getBean().asPrismObject();
    }

    default boolean isDead() {
        return ShadowUtil.isDead(getBean());
    }

    default boolean doesExist() {
        return ShadowUtil.isExists(getBean());
    }

    default boolean isImmutable() {
        return getBean().isImmutable();
    }

    default void shortDump(StringBuilder sb) {
        sb.append(ShadowUtil.shortDumpShadow(getBean()));
    }

    default @Nullable ResourceObjectIdentifiers getIdentifiers() throws SchemaException {
        return ResourceObjectIdentifiers
                .optionalOf(getObjectDefinition(), getBean())
                .orElse(null);
    }

    default @NotNull Collection<ShadowSimpleAttribute<?>> getAllIdentifiers() {
        return ShadowUtil.getAllIdentifiers(getBean());
    }

    default boolean hasPrimaryIdentifier() throws SchemaException {
        return getIdentifiers() instanceof ResourceObjectIdentifiers.WithPrimary;
    }

    default @NotNull ResourceObjectIdentifiers getIdentifiersRequired() throws SchemaException {
        return ResourceObjectIdentifiers.of(getObjectDefinition(), getBean());
    }

    default @NotNull ResourceObjectIdentification<?> getIdentificationRequired() throws SchemaException {
        return ResourceObjectIdentification.of(
                getObjectDefinition(),
                getIdentifiersRequired());
    }

    default <T> @Nullable ShadowSimpleAttribute<T> getPrimaryIdentifierAttribute() {
        //noinspection unchecked
        return (ShadowSimpleAttribute<T>) getAttributesContainer().getPrimaryIdentifier();
    }

    default <T> @NotNull ShadowSimpleAttribute<T> getPrimaryIdentifierAttributeRequired() {
        return stateNonNull(getPrimaryIdentifierAttribute(), "No primary identifier in %s", this);
    }

    default @Nullable ResourceObjectIdentification.WithPrimary getPrimaryIdentification() throws SchemaException {
        var identification = getIdentification();
        return identification instanceof ResourceObjectIdentification.WithPrimary withPrimary ? withPrimary : null;
    }

    default @Nullable Object getPrimaryIdentifierValueFromAttributes() throws SchemaException {
        ResourceObjectIdentifiers identifiers = getIdentifiers();
        if (identifiers == null) {
            return null;
        }
        var primaryIdentifier = identifiers.getPrimaryIdentifier();
        if (primaryIdentifier == null) {
            return null;
        }
        return primaryIdentifier.getOrigValue();
    }

    default @Nullable ResourceObjectIdentification<?> getIdentification() throws SchemaException {
        var identifiers = getIdentifiers();
        if (identifiers != null) {
            return ResourceObjectIdentification.of(getObjectDefinition(), identifiers);
        } else {
            return null;
        }
    }

    /** Updates the in-memory representation. */
    default void updateWith(@NotNull Collection<? extends ItemDelta<?, ?>> modifications) throws SchemaException {
        ObjectDeltaUtil.applyTo(getPrismObject(), modifications);
    }

    /** Replaces the in-memory representation with the new content but the same definition. Returns a new instance. */
    @NotNull AbstractShadow withNewContent(@NotNull ShadowType newBean);

    default @NotNull String getResourceOidRequired() {
        return stateNonNull(
                getResourceOid(),
                "No resource OID in %s", this);
    }

    default @Nullable String getResourceOid() {
        return Referencable.getOid(getBean().getResourceRef());
    }

    default @NotNull QName getObjectClassName() throws SchemaException {
        return stateNonNull(
                getBean().getObjectClass(),
                "No object class name in %s", this);
    }

    default PolyString determineShadowName() throws SchemaException {
        return ShadowUtil.determineShadowName(getBean());
    }

    default @NotNull ShadowAttributesContainer getAttributesContainer() {
        return stateNonNull(
                ShadowUtil.getAttributesContainer(getBean()),
                "No attributes container in %s", this);
    }

    default @NotNull Collection<ShadowAttribute<?, ?, ?, ?>> getAttributes() {
        return ShadowUtil.getAttributes(getBean());
    }

    default @NotNull Collection<ShadowSimpleAttribute<?>> getSimpleAttributes() {
        return ShadowUtil.getSimpleAttributes(getBean());
    }

    default @NotNull Collection<ShadowAssociation> getAssociations() {
        return ShadowUtil.getAssociations(getBean());
    }

    /** Should correspond to {@link #getObjectDefinition()}. */
    default @NotNull ShadowAttributesContainerDefinition getAttributesContainerDefinition() {
        return Objects.requireNonNull(
                getAttributesContainer().getDefinition(),
                () -> "No attributes container definition in " + this);
    }

    default @Nullable <X> ShadowSimpleAttribute<X> findAttribute(@NotNull QName name) {
        return getAttributesContainer().findSimpleAttribute(name);
    }

    default <X> @NotNull Collection<PrismPropertyValue<X>> getAttributeValues(@NotNull QName name) {
        ShadowSimpleAttribute<X> attribute = findAttribute(name);
        return attribute != null ? attribute.getValues() : List.of();
    }

    default boolean hasAuxiliaryObjectClass(@NotNull QName name) {
        return ShadowUtil.hasAuxiliaryObjectClass(getBean(), name);
    }

    /** These checks are to be executed even in production (at least when creating the object). */
    default void checkConsistence() {
        stateCheck(getObjectDefinition() != null, "No object definition in %s", this);
        if (InternalsConfig.consistencyChecks) {
            getBean().asPrismObject().checkConsistence();
            getAttributesContainer().checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);
            checkAttributeDefinitions();
            var associationsContainer = getAssociationsContainer();
            if (associationsContainer != null) {
                associationsContainer.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);
            }
        }
    }

    /** TODO merge with {@link #checkConsistence()} */
    default void checkConsistenceComplex(String desc) {
        checkConsistence();
        ShadowUtil.checkConsistence(getPrismObject(), desc);
    }

    default void checkAttributeDefinitions() {
        ResourceObjectDefinition objectDefinition = getObjectDefinition();
        for (var attribute : getAttributes()) {
            var attrDef = stateNonNull(
                    attribute.getDefinition(),
                    "Attribute %s with no definition in %s", attribute, this);
            var attrDefFromObjectDef = objectDefinition.findAttributeDefinitionStrictlyRequired(attribute.getElementName());
            if (!attrDef.equals(attrDefFromObjectDef)) {
                // FIXME This is too harsh. See e.g. TestModelServiceContract#test350, where we provide our own account delta
                //  that gets processed as part of inbound processing. The attribute definition was provided by the caller
                //  (and is derived from resource object class), whereas the "official" definition is derived from the
                //  object type.
                //  OR ... should we overwrite all client-supplied definitions with our own? Probably not a bad idea.
                throw new IllegalStateException(
                        "Attribute %s has a definition (%s) that does not match the one from object definition (%s from %s) in %s"
                                .formatted(attribute, attrDef, attrDefFromObjectDef, objectDefinition, this));
            }
        }
    }

    AbstractShadow clone();

    default void applyDefinition(@NotNull ResourceObjectDefinition newDefinition) throws SchemaException {
        // This causes problems with embedded associations
        //getPrismObject().applyDefinition(newDefinition.getPrismObjectDefinition(), false);
        getAttributesContainer().applyDefinition(
                newDefinition.toShadowAttributesContainerDefinition());
        checkConsistence();
    }

    default PolyStringType getName() {
        return getBean().getName();
    }

    default <T> @NotNull List<T> getAttributeRealValues(QName attrName) {
        return ShadowUtil.getAttributeValues(getPrismObject(), attrName);
    }

    default <T> @Nullable T getAttributeRealValue(QName attrName) {
        return MiscUtil.extractSingleton(getAttributeRealValues(attrName));
    }

    default @Nullable <T> ShadowSimpleAttribute<T> getSimpleAttribute(QName attrName) {
        return ShadowUtil.getSimpleAttribute(getPrismObject(), attrName);
    }

    default @Nullable ShadowReferenceAttribute getReferenceAttribute(QName attrName) {
        return ShadowUtil.getReferenceAttribute(getPrismObject(), attrName);
    }

    default @Nullable ShadowReferenceAttributeValue getReferenceAttributeSingleValue(QName attrName) {
        var attr = getReferenceAttribute(attrName);
        return attr != null ? attr.getSingleValueRequired() : null;
    }

    default @NotNull Collection<ShadowReferenceAttributeValue> getReferenceAttributeValues(QName attrName) {
        var attr = getReferenceAttribute(attrName);
        return attr != null ? attr.getAttributeValues() : List.of();
    }

    default @NotNull <T> ShadowSimpleAttribute<T> getSimpleAttributeRequired(QName attrName) {
        return stateNonNull(
                getSimpleAttribute(attrName),
                "No '%s' in %s", attrName, this);
    }

    /**
     * @see ShadowUtil#getAssociationsContainer(ShadowType)
     */
    default @Nullable ShadowAssociationsContainer getAssociationsContainer() {
        return ShadowUtil.getAssociationsContainer(getBean());
    }

    default @NotNull ShadowAssociationsContainer getOrCreateAssociationsContainer() {
        return ShadowUtil.getOrCreateAssociationsContainer(getBean());
    }

    /** Returns a detached, immutable list. */
    default @NotNull Collection<ShadowReferenceAttribute> getReferenceAttributes() {
        return ShadowUtil.getReferenceAttributes(getBean());
    }

    default @NotNull ShadowReferenceAttributesCollection getReferenceAttributesCollection() {
        return ShadowReferenceAttributesCollection.ofShadow(getBean());
    }

    default @Nullable ShadowKindType getKind() {
        return getBean().getKind();
    }

    default @NotNull QName getObjectClass() {
        return stateNonNull(
                getBean().getObjectClass(),
                "No object class in %s", this);
    }

    default boolean isClassified() {
        return ShadowUtil.isClassified(getBean());
    }

    default boolean isProtectedObject() {
        return BooleanUtils.isTrue(getBean().isProtectedObject());
    }

    default void applyDelta(@NotNull ItemDelta<?, ?> itemDelta) throws SchemaException {
        itemDelta.applyTo(getPrismObject());
    }

    default Object getHumanReadableNameLazily() {
        return ShadowUtil.getHumanReadableNameLazily(getPrismObject());
    }

    default @NotNull Collection<? extends ShadowAssociationValue> getAssociationValues(QName assocName) {
        var associationsContainer = getAssociationsContainer();
        return associationsContainer != null ? associationsContainer.getAssociationValues(assocName) : List.of();
    }

    default @NotNull ShadowAssociationsCollection getAssociationsCollection() {
        return ShadowAssociationsCollection.ofShadow(getBean());
    }

    default ResourceObjectTypeIdentification getTypeIdentification() {
        return ShadowUtil.getTypeIdentification(getBean());
    }

    default void setObjectType(@Nullable ResourceObjectTypeIdentification typeIdentification) {
        if (typeIdentification != null) {
            getBean()
                    .kind(typeIdentification.getKind())
                    .intent(typeIdentification.getIntent());
        } else {
            getBean()
                    .kind(null)
                    .intent(null);
        }
    }

    default @NotNull ShadowContentDescriptionType getContentDescriptionRequired() {
        return stateNonNull(
                getContentDescription(),
                "No content description in %s", this);
    }

    default @Nullable ShadowContentDescriptionType getContentDescription() {
        return getBean().getContentDescription();
    }

    default @NotNull ObjectOperationPolicyType getEffectiveOperationPolicyRequired() {
        return stateNonNull(
                getBean().getEffectiveOperationPolicy(),
                "No effective provisioning policy in %s", this);
    }

    default AbstractShadow addSimpleAttribute(QName attributeName, Object realValue) throws SchemaException {
        getAttributesContainer().addSimpleAttribute(attributeName, realValue);
        return this;
    }

    default boolean isIdentificationOnly() {
        return getBean().getContentDescription() == ShadowContentDescriptionType.IDENTIFICATION_ONLY;
    }

    default void setIdentificationOnly() {
        getBean().setContentDescription(ShadowContentDescriptionType.IDENTIFICATION_ONLY);
    }

    /**
     * True if the shadow could not be correctly fetched from the resource.
     *
     * Beware, maintenance mode signals {@link OperationResultStatusType#PARTIAL_ERROR} here.
     *
     * TODO is it good that this method is overridden in the subclass?
     */
    default boolean isError() {
        return ShadowUtil.hasFetchError(getBean());
    }

    /** Temporary implementation. */
    default void freeze() {
        getPrismObject().freeze();
    }

    /**
     * The default implementation. Other specific implementations reside in particular modules like `provisioning-impl`.
     * (At least for now, until they are stabilized and proved to be generally useful.
     */
    class Impl implements AbstractShadow {

        @NotNull private final ShadowType bean;

        private Impl(@NotNull ShadowType bean) {
            this.bean = bean;
            ShadowUtil.getOrCreateAttributesContainer(bean);
            checkConsistence();
        }

        @Override
        public @NotNull ShadowType getBean() {
            return bean;
        }

        @Override
        public @NotNull AbstractShadow withNewContent(@NotNull ShadowType newBean) {
            return new Impl(newBean);
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public AbstractShadow clone() {
            return new Impl(bean.clone());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Impl impl = (Impl) o;
            return Objects.equals(bean, impl.bean);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bean);
        }

        @Override
        public String toString() {
            return shortDump();
        }

        @Override
        public String debugDump(int indent) {
            return bean.debugDump(indent); // at least for now
        }
    }
}
