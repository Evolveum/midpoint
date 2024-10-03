/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

/**
 * Default implementation of {@link ResourceObjectTypeDefinition}.
 *
 * Definition of a type of resource objects, as defined in the `schemaHandling` section.
 * (The concept of object type is not present in the "raw" view, presented by a connector.
 * The connector sees only object classes.)
 *
 * There is almost nothing specific here (compared to {@link AbstractResourceObjectDefinitionImpl}), because starting with 4.6,
 * object class definitions can be refined as well. However, kind and intent are (still) specific to type definitions.
 *
 * @author semancik
 */
public final class ResourceObjectTypeDefinitionImpl
        extends AbstractResourceObjectDefinitionImpl
        implements ResourceObjectTypeDefinition {

    /**
     * Kind + intent.
     */
    @NotNull private final ResourceObjectTypeIdentification identification;

    /**
     * Kind of objects covered by this type. Just copied from {@link #identification}.
     */
    @NotNull private final ShadowKindType kind;

    /**
     * Intent of objects covered by this type. Just copied from {@link #identification}.
     */
    @NotNull private final String intent;

    @NotNull private final ResourceObjectClassDefinition refinedObjectClassDefinition;

    /**
     * OID of the resource. Usually not null.
     *
     * TODO keep this? If so, shouldn't we have resource OID also in {@link ResourceObjectClassDefinitionImpl} ?
     */
    private final String resourceOid;

    ResourceObjectTypeDefinitionImpl(
            @NotNull ResourceObjectTypeIdentification identification,
            @NotNull ResourceObjectClassDefinition refinedObjectClassDefinition,
            @NotNull ResourceObjectTypeDefinitionType definitionBean,
            String resourceOid) {
        this(DEFAULT_LAYER, identification, refinedObjectClassDefinition, definitionBean, resourceOid);
    }

    private ResourceObjectTypeDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull ResourceObjectTypeIdentification identification,
            @NotNull ResourceObjectClassDefinition refinedObjectClassDefinition,
            @NotNull ResourceObjectTypeDefinitionType definitionBean,
            String resourceOid) {
        super(layer, definitionBean);
        this.identification = identification;
        this.kind = identification.getKind();
        this.intent = identification.getIntent();
        this.refinedObjectClassDefinition = refinedObjectClassDefinition;
        this.resourceOid = resourceOid;
    }

    @Override
    public @NotNull ResourceObjectTypeIdentification getTypeIdentification() {
        return identification;
    }

    @Override
    public @NotNull ResourceObjectTypeDefinition getTypeDefinition() {
        return this;
    }

    @Override
    public boolean isDefaultFor(@NotNull ShadowKindType kind) {
        return getKind() == kind
                && isDefaultForKind();
    }

    @Override
    public @NotNull ResourceObjectClassDefinition getObjectClassDefinition() {
        return refinedObjectClassDefinition;
    }

    @Override
    public @NotNull ResourceObjectClassDefinition getRawObjectClassDefinition() {
        return refinedObjectClassDefinition.getRawObjectClassDefinition();
    }

    @Override
    public @NotNull QName getObjectClassName() {
        return getObjectClassDefinition().getObjectClassName();
    }

    @Override
    public boolean isDefaultForObjectClass() {
        // Note that this value cannot be defined on a parent.
        return ResourceObjectTypeDefinitionTypeUtil.isDefaultForObjectClass(definitionBean);
    }

    @Override
    public boolean isDefaultForKind() {
        // Note that this value cannot be defined on a parent.
        if (definitionBean.isDefaultForKind() != null) {
            return definitionBean.isDefaultForKind();
        } else if (definitionBean.isDefault() != null) {
            return definitionBean.isDefault();
        } else {
            return false;
        }
    }

    @Override
    public @NotNull String getIntent() {
        return intent;
    }

    @Override
    public @NotNull ShadowKindType getKind() {
        return kind;
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        super.accept(visitor);
        refinedObjectClassDefinition.accept(visitor);
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        if (!super.accept(visitor, visitation)) {
            return false;
        } else {
            refinedObjectClassDefinition.accept(visitor, visitation);
            return true;
        }
    }

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        if (isImmutable()) {
            return; // This would fail anyway
        }
        super.trimTo(paths);
        refinedObjectClassDefinition.trimTo(paths);
    }

    //region Cloning ========================================================
    @NotNull
    @Override
    public ResourceObjectTypeDefinitionImpl clone() {
        return cloneInLayer(currentLayer);
    }

    @Override
    public @NotNull ResourceObjectTypeDefinition forLayerMutable(@NotNull LayerType layer) {
        return (ResourceObjectTypeDefinition) super.forLayerMutable(layer);
    }

    @Override
    protected @NotNull ResourceObjectTypeDefinitionImpl cloneInLayer(@NotNull LayerType layer) {
        ResourceObjectTypeDefinitionImpl clone = new ResourceObjectTypeDefinitionImpl(
                layer, identification, refinedObjectClassDefinition, definitionBean, resourceOid);
        clone.copyDefinitionDataFrom(layer, this);
        return clone;
    }

    /**
     * TODO should we really clone the definitions?
     */
    @NotNull
    @Override
    public ResourceObjectTypeDefinition deepClone(@NotNull DeepCloneOperation operation) {
        return clone(); // TODO ok?
    }
    //endregion

    //region Diagnostic output, hashCode/equals =========================================================

    @Override
    public String getDebugDumpClassName() {
        return "ROTD";
    }

    @Override
    public String getHumanReadableName() {
        if (getDisplayName() != null) {
            return getDisplayName();
        } else {
            return getKind() + ":" + getIntent();
        }
    }

    @Override
    public String toString() {
        return getDebugDumpClassName() + getMutabilityFlag() + "(" + getKind() + ":" + getIntent()
                + "=" + PrettyPrinter.prettyPrint(getTypeName()) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ResourceObjectTypeDefinitionImpl that = (ResourceObjectTypeDefinitionImpl) o;
        return attributeDefinitions.equals(that.attributeDefinitions)
                && associationDefinitions.equals(that.associationDefinitions)
                && primaryIdentifiersNames.equals(that.primaryIdentifiersNames)
                && secondaryIdentifiersNames.equals(that.secondaryIdentifiersNames)
                && refinedObjectClassDefinition.equals(that.refinedObjectClassDefinition)
                && auxiliaryObjectClassDefinitions.equals(that.auxiliaryObjectClassDefinitions)
                && Objects.equals(resourceOid, that.resourceOid)
                && definitionBean.equals(that.definitionBean)
                && kind == that.kind
                && intent.equals(that.intent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resourceOid, definitionBean, kind, intent);
    }

    //endregion

    @Override
    public MutableResourceObjectClassDefinition toMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void performFreeze() {
        super.performFreeze();
        refinedObjectClassDefinition.freeze(); // TODO really?
    }

    @Override
    public boolean hasSubstitutions() {
        return false;
    }

    @Override
    public Optional<ItemDefinition<?>> substitution(QName name) {
        return Optional.empty();
    }

    @Override
    public @Nullable CorrelationDefinitionType getCorrelationDefinitionBean() {
        return definitionBean.getCorrelation();
    }

    @Override
    public Boolean isSynchronizationEnabled() {
        return definitionBean.getSynchronization() != null ? true : null; // TODO FIXME
    }

    @Override
    public Boolean isSynchronizationOpportunistic() {
        SynchronizationReactionsType synchronization = definitionBean.getSynchronization();
        return synchronization != null ? synchronization.isOpportunistic() : null;
    }

    @Override
    public QName getFocusTypeName() {
        ResourceObjectFocusSpecificationType focusSpec = definitionBean.getFocus();
        return focusSpec != null ? focusSpec.getType() : null;
    }

    @Override
    public @Nullable ObjectReferenceType getArchetypeRef() {
        ResourceObjectFocusSpecificationType focusSpec = definitionBean.getFocus();
        return focusSpec != null ? focusSpec.getArchetypeRef() : null;
    }

    @Override
    public ExpressionType getClassificationCondition() {
        ResourceObjectTypeDelineationType delineation = definitionBean.getDelineation();
        return delineation != null ? delineation.getClassificationCondition() : null;
    }

    @Override
    public boolean hasSynchronizationReactionsDefinition() {
        return definitionBean.getSynchronization() != null;
    }

    @Override
    public @NotNull Collection<SynchronizationReactionDefinition> getSynchronizationReactions() {
        SynchronizationReactionsType reactions = definitionBean.getSynchronization();
        if (reactions == null) {
            return List.of();
        } else {
            SynchronizationReactionsDefaultSettingsType defaultSettings = reactions.getDefaultSettings();
            ClockworkSettings reactionLevelSettings = ClockworkSettings.of(defaultSettings);
            return reactions.getReaction().stream()
                    .map(bean -> SynchronizationReactionDefinition.of(bean, reactionLevelSettings))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public @Nullable QName getDescriptionAttributeName() {
        return refinedObjectClassDefinition.getDescriptionAttributeName();
    }

    @Override
    public @Nullable QName getNamingAttributeName() {
        return refinedObjectClassDefinition.getNamingAttributeName();
    }

    @Override
    public @Nullable QName getDisplayNameAttributeName() {
        if (displayNameAttributeName != null) {
            return displayNameAttributeName;
        } else {
            return refinedObjectClassDefinition.getDisplayNameAttributeName();
        }
    }

    @Override
    public PrismObject<ShadowType> createBlankShadow(String resourceOid, String tag) {
        return super.createBlankShadow(resourceOid, tag)
                .asObjectable()
                .kind(getKind())
                .intent(getIntent())
                .asPrismObject();
    }

    @Override
    public String getResourceOid() {
        return resourceOid;
    }

    @Override
    public <T extends CapabilityType> @Nullable T getConfiguredCapability(Class<T> capabilityClass) {
        return CapabilityUtil.getCapability(getSpecificCapabilities(), capabilityClass);
    }

    @Override
    public @Nullable CapabilityCollectionType getSpecificCapabilities() {
        return definitionBean.getConfiguredCapabilities();
    }

    @Override
    protected void addDebugDumpHeaderExtension(StringBuilder sb) {
        if (isDefaultForKind()) {
            sb.append(",default-for-kind");
        }
        if (isDefaultForObjectClass()) {
            sb.append(",default-for-class");
        }
        sb.append(",kind=").append(getKind().value());
        sb.append(",intent=").append(getIntent());
    }
}
