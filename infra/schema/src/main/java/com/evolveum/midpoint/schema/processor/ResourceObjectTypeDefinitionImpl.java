/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.DeepCloneOperation;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.SmartVisitation;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.processor.SynchronizationReactionDefinition.ObjectSynchronizationReactionDefinition;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
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

    /** All supertypes of this object type. */
    @NotNull private final Set<ResourceObjectTypeIdentification> ancestorsIds;

    @NotNull private final ResourceObjectClassDefinition objectClassDefinition;

    ResourceObjectTypeDefinitionImpl(
            @NotNull BasicResourceInformation basicResourceInformation,
            @NotNull ResourceObjectTypeIdentification identification,
            @NotNull Set<ResourceObjectTypeIdentification> ancestorsIds,
            @NotNull ResourceObjectClassDefinition objectClassDefinition,
            @NotNull ResourceObjectTypeDefinitionType definitionBean)
            throws SchemaException, ConfigurationException {
        this(DEFAULT_LAYER, basicResourceInformation, identification, ancestorsIds, objectClassDefinition, definitionBean);
    }

    private ResourceObjectTypeDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull BasicResourceInformation basicResourceInformation,
            @NotNull ResourceObjectTypeIdentification identification,
            @NotNull Set<ResourceObjectTypeIdentification> ancestorsIds,
            @NotNull ResourceObjectClassDefinition objectClassDefinition,
            @NotNull ResourceObjectTypeDefinitionType definitionBean)
            throws SchemaException, ConfigurationException {
        super(layer, basicResourceInformation, definitionBean);
        this.identification = identification;
        this.ancestorsIds = ancestorsIds;
        this.kind = identification.getKind();
        this.intent = identification.getIntent();
        this.objectClassDefinition = objectClassDefinition;
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
    public @NotNull Set<ResourceObjectTypeIdentification> getAncestorsIds() {
        return ancestorsIds;
    }

    @Override
    public boolean isDefaultFor(@NotNull ShadowKindType kind) {
        return getKind() == kind
                && isDefaultForKind();
    }

    @Override
    public @NotNull ResourceObjectClassDefinition getObjectClassDefinition() {
        return objectClassDefinition;
    }

    @Override
    public @NotNull NativeObjectClassDefinition getNativeObjectClassDefinition() {
        return objectClassDefinition.getNativeObjectClassDefinition();
    }

    @Override
    public @NotNull QName getObjectClassName() {
        return getObjectClassDefinition().getObjectClassName();
    }

    @Override
    public boolean isDefaultForObjectClass() {
        // Note that this value cannot be defined on a parent.
        if (definitionBean.isDefaultForObjectClass() != null) {
            return definitionBean.isDefaultForObjectClass();
        } else if (definitionBean.isDefault() != null) {
            return definitionBean.isDefault();
        } else {
            return false;
        }
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
        objectClassDefinition.accept(visitor);
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        if (!super.accept(visitor, visitation)) {
            return false;
        } else {
            objectClassDefinition.accept(visitor, visitation);
            return true;
        }
    }

    @Override
    public void trimAttributesTo(@NotNull Collection<ItemPath> paths) {
        if (isImmutable()) {
            return; // This would fail anyway
        }
        super.trimAttributesTo(paths);
        objectClassDefinition.trimAttributesTo(paths);
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return objectClassDefinition.getSchemaContextDefinition();
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
        ResourceObjectTypeDefinitionImpl clone;
        try {
            clone = new ResourceObjectTypeDefinitionImpl(
                    layer, getBasicResourceInformation(), identification, ancestorsIds,
                    objectClassDefinition, definitionBean);
        } catch (SchemaException | ConfigurationException e) {
            // The data should be already checked for correctness, so this should not happen.
            throw SystemException.unexpected(e, "when cloning");
        }
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
                && objectClassDefinition.equals(that.objectClassDefinition)
                && auxiliaryObjectClassDefinitions.equals(that.auxiliaryObjectClassDefinitions)
                && Objects.equals(basicResourceInformation, that.basicResourceInformation)
                && definitionBean.equals(that.definitionBean)
                && kind == that.kind
                && intent.equals(that.intent)
                && ancestorsIds.equals(that.ancestorsIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), basicResourceInformation, definitionBean, kind, intent);
    }

    //endregion

    @Override
    public void performFreeze() {
        super.performFreeze();
        objectClassDefinition.freeze(); // TODO really?
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

    // FIXME TEMPORARY, reconsider
    @Override
    public @NotNull FocusSpecification getFocusSpecification() {
        return new FocusSpecification() {

            @Override
            public String getAssignmentSubtype() {
                var focusSpec = getDefinitionBean().getFocus();
                return focusSpec != null ? focusSpec.getAssignmentSubtype() : null;
            }

            @Override
            public String getArchetypeOid() {
                return ResourceObjectTypeDefinitionImpl.this.getArchetypeOid();
            }
        };
    }

    @Override
    public @NotNull Collection<? extends ObjectSynchronizationReactionDefinition> getSynchronizationReactions() {
        return SynchronizationReactionDefinition.modern(
                definitionBean.getSynchronization());
    }

    @Override
    public @Nullable QName getDescriptionAttributeName() {
        return objectClassDefinition.getDescriptionAttributeName();
    }

    @Override
    public @Nullable QName getNamingAttributeName() {
        return objectClassDefinition.getNamingAttributeName();
    }

    @Override
    public @Nullable QName getDisplayNameAttributeName() {
        if (displayNameAttributeName != null) {
            return displayNameAttributeName;
        } else {
            return objectClassDefinition.getDisplayNameAttributeName();
        }
    }

    @Override
    public AbstractShadow createBlankShadowWithTag(String tag) {
        var shadow = super.createBlankShadowWithTag(tag);
        shadow.getBean()
                .kind(getKind())
                .intent(getIntent());
        return shadow;
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
            sb.append(", default-for-kind");
        }
        if (isDefaultForObjectClass()) {
            sb.append(", default-for-class");
        }
        sb.append(", kind=").append(getKind().value());
        sb.append(", intent=").append(getIntent());
    }

    @Override
    protected void addDebugDumpTrailer(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "ancestors", ancestorsIds, indent + 1);
    }

    @Override
    public @NotNull String getShortIdentification() {
        return identification.toString();
    }

}
