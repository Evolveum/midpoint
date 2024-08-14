/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

/** A {@link ShadowType} or {@link ShadowAssociationValue} that provides the source data for inbound mappings. */
public interface InboundSourceData extends DebugDumpable, Serializable {

    static InboundSourceData forShadow(
            @Nullable PrismObject<ShadowType> shadow,
            @Nullable ObjectDelta<ShadowType> aPrioriShadowDelta,
            @NotNull ResourceObjectDefinition resourceObjectDefinition) {
        return new Shadow(
                asObjectable(shadow),
                aPrioriShadowDelta,
                resourceObjectDefinition);
    }

    /**
     * The object definition is where we look for attribute definitions related to the association value.
     * So, for rich association it points to the associated object, whereas for trivial ones it points to the _subject_.
     */
    static InboundSourceData forAssociationValue(
            @NotNull ShadowAssociationValue associationValue,
            @NotNull ResourceObjectDefinition resourceObjectDefinition) {
        return new AssociationValue(associationValue, resourceObjectDefinition);
    }

    static @NotNull InboundSourceData forShadowLikeValue(
            @NotNull ShadowLikeValue shadowLikeValue,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta,
            @NotNull ResourceObjectDefinition objectDefinitionRequired) {
        if (shadowLikeValue instanceof AbstractShadow shadow) {
            return forShadow(shadow.getPrismObject(), resourceObjectDelta, objectDefinitionRequired);
        } else if (shadowLikeValue instanceof ShadowAssociationValue associationValue) {
            return forAssociationValue(associationValue, objectDefinitionRequired);
        } else {
            throw new IllegalStateException("Unsupported shadow-like value: " + shadowLikeValue);
        }
    }

    @Nullable ObjectDelta<ShadowType> getAPrioriDelta();

    /**
     * Returns either the shadow definition or throws an exception.
     */
    default @NotNull ResourceObjectDefinition getShadowObjectDefinition() {
        throw new UnsupportedOperationException("Not supported for " + this);
    }

    Collection<? extends ShadowSimpleAttributeDefinition<?>> getSimpleAttributeDefinitions();

    Collection<? extends ShadowReferenceAttributeDefinition> getReferenceAttributeDefinitions();

    Collection<? extends ShadowAssociationDefinition> getAssociationDefinitions();

    boolean isEmpty();

    <TA> PrismProperty<TA> getSimpleAttribute(ItemName attributeName);

    PrismReference getReferenceAttribute(ItemName refAttrName);

    ShadowAssociation getAssociation(ItemName assocName);

    /** Returns a-priori delta for given item. */
    <V extends PrismValue, D extends ItemDefinition<?>> ItemDelta<V, D> getItemAPrioriDelta(ItemPath path);

    @Nullable PrismProperty<QName> getAuxiliaryObjectClasses();

    default PrismObject<ShadowType> getShadowIfPresent() {
        return null;
    }

    default ShadowAssociationValueType getAssociationValueBeanIfPresent() {
        return null;
    }

    class Shadow implements InboundSourceData {

        @Nullable private final ShadowType shadow;
        @Nullable private final ObjectDelta<ShadowType> aPrioriShadowDelta;
        @NotNull private final ResourceObjectDefinition resourceObjectDefinition;

        public Shadow(
                @Nullable ShadowType shadow,
                @Nullable ObjectDelta<ShadowType> aPrioriShadowDelta,
                @NotNull ResourceObjectDefinition resourceObjectDefinition) {
            this.shadow = shadow;
            this.aPrioriShadowDelta = aPrioriShadowDelta;
            this.resourceObjectDefinition = resourceObjectDefinition;
        }

        public @Nullable ShadowType getShadow() {
            return shadow;
        }

        @Override
        public @Nullable ObjectDelta<ShadowType> getAPrioriDelta() {
            return aPrioriShadowDelta;
        }

        @Override
        public @NotNull ResourceObjectDefinition getShadowObjectDefinition() {
            return resourceObjectDefinition;
        }

        @Override
        public Collection<? extends ShadowSimpleAttributeDefinition<?>> getSimpleAttributeDefinitions() {
            return resourceObjectDefinition.getSimpleAttributeDefinitions();
        }

        @Override
        public Collection<? extends ShadowReferenceAttributeDefinition> getReferenceAttributeDefinitions() {
            return resourceObjectDefinition.getReferenceAttributeDefinitions();
        }

        @Override
        public Collection<? extends ShadowAssociationDefinition> getAssociationDefinitions() {
            return resourceObjectDefinition.getAssociationDefinitions();
        }

        @Override
        public boolean isEmpty() {
            return shadow == null;
        }

        @Override
        public @Nullable <TA> PrismProperty<TA> getSimpleAttribute(ItemName attributeName) {
            if (shadow != null) {
                return shadow.asPrismObject().findProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, attributeName));
            } else {
                return null;
            }
        }

        @Override
        public @Nullable PrismReference getReferenceAttribute(ItemName attributeName) {
            if (shadow != null) {
                return shadow.asPrismObject().findReference(ItemPath.create(ShadowType.F_ATTRIBUTES, attributeName));
            } else {
                return null;
            }
        }

        @Override
        public @Nullable ShadowAssociation getAssociation(ItemName associationName) {
            if (shadow != null) {
                return ShadowUtil.getAssociation(shadow.asPrismObject(), associationName);
            } else {
                return null;
            }
        }

        @Override
        public @Nullable PrismProperty<QName> getAuxiliaryObjectClasses() {
            if (shadow != null) {
                return shadow.asPrismObject().findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
            } else {
                return null;
            }
        }

        @Override
        public <V extends PrismValue, D extends ItemDefinition<?>> ItemDelta<V, D> getItemAPrioriDelta(ItemPath path) {
            if (aPrioriShadowDelta != null) {
                return aPrioriShadowDelta.findItemDelta(path);
            } else {
                return null;
            }
        }

        @Override
        public PrismObject<ShadowType> getShadowIfPresent() {
            return asPrismObject(shadow);
        }

        @Override
        public String debugDump(int indent) {
            return DebugUtil.debugDump(shadow, indent);
        }
    }

    class AssociationValue implements InboundSourceData {

        @NotNull private final ShadowAssociationValue associationValue;
        @NotNull private final ResourceObjectDefinition resourceObjectDefinition;

        AssociationValue(
                @NotNull ShadowAssociationValue associationValue,
                @NotNull ResourceObjectDefinition resourceObjectDefinition) {
            this.associationValue = associationValue;
            this.resourceObjectDefinition = resourceObjectDefinition;
        }

        public @NotNull ShadowAssociationValue getAssociationValue() {
            return associationValue;
        }

        @Override
        public @Nullable ObjectDelta<ShadowType> getAPrioriDelta() {
            return null;
        }

        @Override
        public Collection<? extends ShadowSimpleAttributeDefinition<?>> getSimpleAttributeDefinitions() {
            if (associationValue.hasAssociationObject()) {
                return resourceObjectDefinition.getSimpleAttributeDefinitions();
            } else {
                return List.of();
            }
        }

        @Override
        public Collection<? extends ShadowReferenceAttributeDefinition> getReferenceAttributeDefinitions() {
            if (associationValue.hasAssociationObject()) {
                return resourceObjectDefinition.getReferenceAttributeDefinitions();
            } else {
                return associationValue.getObjectReferences().stream()
                        .map(ref -> ref.getDefinitionRequired())
                        .toList();
            }
        }

        @Override
        public Collection<? extends ShadowAssociationDefinition> getAssociationDefinitions() {
            return List.of();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public <TA> PrismProperty<TA> getSimpleAttribute(ItemName attributeName) {
            var attributes = associationValue.getAttributesContainer();
            return attributes != null ? attributes.findSimpleAttribute(attributeName) : null;
        }

        @Override
        public PrismReference getReferenceAttribute(ItemName refAttrName) {
            var objects = associationValue.getObjectsContainer();
            return objects != null ? objects.findReferenceAttribute(refAttrName) : null;
        }

        @Override
        public PrismObject<ShadowType> getShadowIfPresent() {
            return associationValue.hasAssociationObject() ?
                    associationValue.getAssociationDataObject().getPrismObject() : null;
        }

        @Override
        public ShadowAssociation getAssociation(ItemName assocName) {
            return null;
        }

        @Override
        public <V extends PrismValue, D extends ItemDefinition<?>> ItemDelta<V, D> getItemAPrioriDelta(ItemPath path) {
            return null;
        }

        @Override
        public @Nullable PrismProperty<QName> getAuxiliaryObjectClasses() {
            return null;
        }

        @Override
        public ShadowAssociationValueType getAssociationValueBeanIfPresent() {
            return associationValue.asContainerable();
        }

        @Override
        public String debugDump(int indent) {
            return associationValue.debugDump(indent);
        }
    }
}
