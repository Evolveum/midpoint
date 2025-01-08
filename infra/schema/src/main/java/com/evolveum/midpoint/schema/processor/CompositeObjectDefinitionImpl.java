/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import java.util.*;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.schema.util.AbstractShadow;

import com.evolveum.midpoint.prism.delta.ItemMerger;
import com.evolveum.midpoint.prism.key.NaturalKeyDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

/**
 * Represents ad-hoc combination of definitions of structural and auxiliary object classes.
 *
 * A specialty of this class is the caching of attribute definitions. It can be done but only if the object classes
 * are immutable. (Which is currently the case for all parsed resource schemas, but sometimes it must be relaxed, namely
 * when applying security constraints onto the definitions.)
 *
 * Not caching attribute definitions in the case of mutable definition is quite dangerous, as it can lead to silent
 * loss of performance if the definitions are not frozen appropriately. See MID-9156.
 *
 * @author semancik
 */
public class CompositeObjectDefinitionImpl
        extends AbstractFreezable
        implements CompositeObjectDefinition {

    @Serial private static final long serialVersionUID = 1L;

    private static final LayerType DEFAULT_LAYER = LayerType.MODEL;

    @NotNull private final LayerType currentLayer;
    @NotNull private final ResourceObjectDefinition structuralDefinition;
    @NotNull private final Collection<? extends ResourceObjectDefinition> auxiliaryDefinitions;

    /** Lazily computed, but only when this instance is immutable. */
    private volatile List<? extends ShadowAttributeDefinition<?, ?, ?, ?>> allAttributeDefinitions;

    private PrismObjectDefinition<ShadowType> prismObjectDefinition;

    private CompositeObjectDefinitionImpl(
            @NotNull LayerType currentLayer,
            @NotNull ResourceObjectDefinition structuralDefinition,
            @Nullable Collection<? extends ResourceObjectDefinition> auxiliaryDefinitions,
            boolean allowMutableDefinitions) {
        this.currentLayer = currentLayer;
        this.structuralDefinition = structuralDefinition;
        this.auxiliaryDefinitions =
                Objects.requireNonNullElseGet(auxiliaryDefinitions, ArrayList::new);

        if (!allowMutableDefinitions) {
            this.structuralDefinition.checkImmutable();
            this.auxiliaryDefinitions.forEach(Freezable::checkImmutable);
        }
    }

    static @NotNull CompositeObjectDefinitionImpl immutable(
            @NotNull ResourceObjectDefinition structuralDefinition,
            @Nullable Collection<? extends ResourceObjectDefinition> auxiliaryDefinitions) {
        var definition = new CompositeObjectDefinitionImpl(
                DEFAULT_LAYER, structuralDefinition, auxiliaryDefinitions, false);
        definition.freeze();
        return definition;
    }

    static @NotNull CompositeObjectDefinitionImpl mutable(
            @NotNull ResourceObjectDefinition structuralDefinition,
            @Nullable Collection<? extends ResourceObjectDefinition> auxiliaryDefinitions) {
        return new CompositeObjectDefinitionImpl(
                DEFAULT_LAYER, structuralDefinition, auxiliaryDefinitions, true);
    }

    @Override
    public @NotNull BasicResourceInformation getBasicResourceInformation() {
        return structuralDefinition.getBasicResourceInformation();
    }

    @NotNull
    @Override
    public Collection<? extends ResourceObjectDefinition> getAuxiliaryDefinitions() {
        return auxiliaryDefinitions;
    }

    @Override
    public PrismObjectDefinition<ShadowType> getPrismObjectDefinition() {
        if (prismObjectDefinition == null) {
            prismObjectDefinition =
                    ObjectFactory.constructObjectDefinition(
                            toShadowAttributesContainerDefinition(),
                            toShadowAssociationsContainerDefinition());
        }
        return prismObjectDefinition;
    }

    @Override
    public boolean isPrimaryIdentifier(QName attrName) {
        return structuralDefinition.isPrimaryIdentifier(attrName);
    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        return structuralDefinition.getSchemaMigrations();
    }

    @Override
    public List<ItemDiagramSpecification> getDiagrams() { return structuralDefinition.getDiagrams(); }

    @Override
    public DisplayHint getDisplayHint() {
        return structuralDefinition.getDisplayHint();
    }

    @Override
    public @Nullable String getMergerIdentifier() {
        return structuralDefinition.getMergerIdentifier();
    }

    @Override
    public @Nullable List<QName> getNaturalKeyConstituents() {
        return structuralDefinition.getNaturalKeyConstituents();
    }

    @Override
    public boolean isEmphasized() {
        return structuralDefinition.isEmphasized();
    }

    @Override
    public boolean isAbstract() {
        return structuralDefinition.isAbstract();
    }

    @Override
    public boolean isSecondaryIdentifier(QName attrName) {
        return structuralDefinition.isSecondaryIdentifier(attrName);
    }

    @Override
    public boolean isDeprecated() {
        return structuralDefinition.isDeprecated();
    }

    @Override
    public boolean isRemoved() {
        return structuralDefinition.isRemoved();
    }

    @Override
    public boolean isOptionalCleanup() {
        return structuralDefinition.isOptionalCleanup();
    }

    @Override
    public String getRemovedSince() {
        return structuralDefinition.getRemovedSince();
    }

    @Override
    public String getDeprecatedSince() {
        return structuralDefinition.getDeprecatedSince();
    }

    @Override
    public boolean isExperimental() {
        return structuralDefinition.isExperimental();
    }

    @Override
    public String getPlannedRemoval() {
        return structuralDefinition.getPlannedRemoval();
    }

    @Override
    public boolean isElaborate() {
        return structuralDefinition.isElaborate();
    }

    @Override
    public Integer getDisplayOrder() {
        return structuralDefinition.getDisplayOrder();
    }

    @Override
    public String getHelp() {
        return structuralDefinition.getHelp();
    }

    @NotNull
    @Override
    public QName getTypeName() {
        return structuralDefinition.getTypeName();
    }

    @Override
    public String getDocumentation() {
        return structuralDefinition.getDocumentation();
    }

    @Override
    public String getDocumentationPreview() {
        return structuralDefinition.getDocumentationPreview();
    }

    @Override
    public boolean isRuntimeSchema() {
        return structuralDefinition.isRuntimeSchema();
    }

    @NotNull
    @Override
    public Collection<? extends ShadowSimpleAttributeDefinition<?>> getPrimaryIdentifiers() {
        return structuralDefinition.getPrimaryIdentifiers();
    }

    @Override
    public @NotNull Collection<QName> getPrimaryIdentifiersNames() {
        return structuralDefinition.getPrimaryIdentifiersNames();
    }

    @NotNull
    @Override
    public Collection<? extends ShadowSimpleAttributeDefinition<?>> getSecondaryIdentifiers() {
        return structuralDefinition.getSecondaryIdentifiers();
    }

    @Override
    public @NotNull Collection<QName> getSecondaryIdentifiersNames() {
        return structuralDefinition.getSecondaryIdentifiersNames();
    }

    @Override
    public @NotNull ShadowMarkingRules getShadowMarkingRules() {
        return structuralDefinition.getShadowMarkingRules();
    }

    @Override
    public String getDisplayName() {
        return structuralDefinition.getDisplayName();
    }

    @Override
    public String getDescription() {
        return structuralDefinition.getDescription();
    }

    @Override
    public @NotNull ResourceObjectClassDefinition getObjectClassDefinition() {
        return structuralDefinition.getObjectClassDefinition();
    }

    @Override
    public @NotNull NativeObjectClassDefinition getNativeObjectClassDefinition() {
        return structuralDefinition.getNativeObjectClassDefinition();
    }

    @Override
    public @NotNull ResourceObjectTypeDelineation getDelineation() {
        return structuralDefinition.getDelineation();
    }

    @Override
    public ResourceObjectReferenceType getBaseContext() {
        return structuralDefinition.getBaseContext();
    }

    @Override
    public SearchHierarchyScope getSearchHierarchyScope() {
        return structuralDefinition.getSearchHierarchyScope();
    }

    @Override
    public @NotNull ResourceObjectVolatilityType getVolatility() {
        return structuralDefinition.getVolatility();
    }

    @Override
    public @Nullable DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases() {
        return structuralDefinition.getDefaultInboundMappingEvaluationPhases();
    }

    @Override
    public @NotNull FocusSpecification getFocusSpecification() {
        return structuralDefinition.getFocusSpecification();
    }

    @Override
    public @NotNull Collection<? extends SynchronizationReactionDefinition> getSynchronizationReactions() {
        return structuralDefinition.getSynchronizationReactions();
    }

    @Override
    public CorrelationDefinitionType getCorrelation() {
        return structuralDefinition.getCorrelation();
    }

    @Override
    public @Nullable String getLifecycleState() {
        return structuralDefinition.getLifecycleState();
    }

    @Override
    public ResourceObjectMultiplicityType getObjectMultiplicity() {
        return structuralDefinition.getObjectMultiplicity();
    }

    @Override
    public ProjectionPolicyType getProjectionPolicy() {
        return structuralDefinition.getProjectionPolicy();
    }

    @Override
    public ObjectReferenceType getSecurityPolicyRef() {
        return structuralDefinition.getSecurityPolicyRef();
    }

    @Override
    public ResourceActivationDefinitionType getActivationSchemaHandling() {
        return structuralDefinition.getActivationSchemaHandling();
    }

    @Override
    public <T extends CapabilityType> T getEnabledCapability(@NotNull Class<T> capabilityClass, ResourceType resource) {
        return structuralDefinition.getEnabledCapability(capabilityClass, resource);
    }

    @Override
    public void validate() throws SchemaException {
        structuralDefinition.validate();
        for (ResourceObjectDefinition auxiliaryDefinition : auxiliaryDefinitions) {
            auxiliaryDefinition.validate();
        }
    }

    @Override
    public synchronized @NotNull List<? extends ShadowAttributeDefinition<?, ?, ?, ?>> getAttributeDefinitions() {
        if (auxiliaryDefinitions.isEmpty()) {
            return structuralDefinition.getAttributeDefinitions();
        }

        if (allAttributeDefinitions != null) {
            return allAttributeDefinitions;
        }

        List<? extends ShadowAttributeDefinition<?, ?, ?, ?>> collectedDefinitions = collectAttributeDefinitions();
        if (isImmutable()) {
            allAttributeDefinitions = collectedDefinitions;
        } else {
            // it's not safe to cache the definitions if this instance is mutable
        }

        return collectedDefinitions;
    }

    private @NotNull List<? extends ShadowAttributeDefinition<?, ?, ?, ?>> collectAttributeDefinitions() {
        // Adds all attribute definitions from aux OCs that are not already known.
        ArrayList<ShadowAttributeDefinition<?, ?, ?, ?>> collectedDefinitions =
                new ArrayList<>(structuralDefinition.getAttributeDefinitions());
        for (ResourceObjectDefinition auxiliaryObjectClassDefinition : auxiliaryDefinitions) {
            for (var auxAttrDef : auxiliaryObjectClassDefinition.getAttributeDefinitions()) {
                boolean shouldAdd = true;
                for (var def : collectedDefinitions) {
                    if (def.getItemName().equals(auxAttrDef.getItemName())) { // FIXME what about case in-sensitiveness?
                        shouldAdd = false;
                        break;
                    }
                }
                if (shouldAdd) {
                    collectedDefinitions.add(auxAttrDef);
                }
            }
        }
        return collectedDefinitions;
    }

    @Override
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {
        //noinspection unchecked,rawtypes
        return (List) getAttributeDefinitions();
    }

    @Override
    public void revive(PrismContext prismContext) {
        structuralDefinition.revive(prismContext);
        for (ResourceObjectDefinition auxiliaryObjectClassDefinition : auxiliaryDefinitions) {
            auxiliaryObjectClassDefinition.revive(prismContext);
        }
    }

    @Override
    public @NotNull Collection<CompleteItemInboundDefinition> getItemInboundDefinitions() {
        // Auxiliary object class definitions do not have their own inbound mapping definitions.
        // "Structural" definition here is actually a type definition with attributes from fixed auxiliary object classes.
        return structuralDefinition.getItemInboundDefinitions();
    }

    @Override
    public ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings() {
        return structuralDefinition.getAuxiliaryObjectClassMappings();
    }

    @Override
    public @NotNull List<MappingType> getAuxiliaryObjectClassInboundMappings() {
        return structuralDefinition.getAuxiliaryObjectClassInboundMappings();
    }

    @Override
    public boolean hasAuxiliaryObjectClass(QName expectedObjectClassName) {
        if (structuralDefinition.hasAuxiliaryObjectClass(expectedObjectClassName)) {
            return true;
        }
        return auxiliaryDefinitions.stream().anyMatch(
                def -> QNameUtil.match(expectedObjectClassName, def.getTypeName()));
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        if (path.size() == 1 && path.startsWithName()) {
            return findLocalItemDefinition(ItemPath.toName(path.first()), clazz, false);
        }
        throw new UnsupportedOperationException("TODO implement if needed");
    }

    @Override
    public Class<?> getTypeClass() {
        return ShadowAttributesType.class;
    }

    @Override
    public AbstractShadow createBlankShadowWithTag(String tag) {
        return structuralDefinition.createBlankShadowWithTag(tag);
    }

    @Override
    public @Nullable ResourcePasswordDefinitionType getPasswordDefinition() {
        return findInDefinitions(ResourceObjectDefinition::getPasswordDefinition);
    }

    @Override
    public @Nullable ResourceLastLoginTimestampDefinitionType getLastLoginTimestampDefinition() {
        return findInDefinitions(ResourceObjectDefinition::getLastLoginTimestampDefinition);
    }

    private <T> T findInDefinitions(Function<ResourceObjectDefinition, T> transform) {
        T val = transform.apply(structuralDefinition);
        if (val != null) {
            return val;
        }
        // TODO what if there is a conflict?
        for (ResourceObjectDefinition auxiliaryObjectClassDefinition : auxiliaryDefinitions) {
            T val2 = transform.apply(auxiliaryObjectClassDefinition);
            if (val2 != null) {
                return val2;
            }
        }
        return null;
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        visitor.visit(this);
        structuralDefinition.accept(visitor);
        for (ResourceObjectDefinition auxiliaryObjectClassDefinition : auxiliaryDefinitions) {
            auxiliaryObjectClassDefinition.accept(visitor);
        }
    }

    // TODO reconsider this
    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        visitor.visit(this);
        structuralDefinition.accept(visitor, visitation);
        for (ResourceObjectDefinition auxiliaryObjectClassDefinition : auxiliaryDefinitions) {
            auxiliaryObjectClassDefinition.accept(visitor, visitation);
        }
        return true;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @NotNull
    @Override
    public CompositeObjectDefinitionImpl clone() {
        ResourceObjectDefinition structuralClone = structuralDefinition.clone();
        Collection<ResourceObjectDefinition> auxiliaryObjectClassDefinitionsClone =
                new ArrayList<>(this.auxiliaryDefinitions.size());
        for (ResourceObjectDefinition auxDefinition : this.auxiliaryDefinitions) {
            auxiliaryObjectClassDefinitionsClone.add(auxDefinition.clone());
        }
        return new CompositeObjectDefinitionImpl(
                currentLayer, structuralClone, auxiliaryObjectClassDefinitionsClone, true);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + auxiliaryDefinitions.hashCode();
        result = prime * result
                + structuralDefinition.hashCode();
        return result;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CompositeObjectDefinitionImpl other = (CompositeObjectDefinitionImpl) obj;
        if (!auxiliaryDefinitions.equals(other.auxiliaryDefinitions)) {
            return false;
        }
        if (!structuralDefinition.equals(other.structuralDefinition)) {
            return false;
        }
        return true;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(INDENT_STRING.repeat(Math.max(0, indent)));
        sb.append(getDebugDumpClassName()).append(getMutabilityFlag()).append(": ");
        sb.append(SchemaDebugUtil.prettyPrint(getTypeName()));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "structural", structuralDefinition, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "auxiliary", auxiliaryDefinitions, indent + 1);
        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    public String getDebugDumpClassName() {
        return "COD";
    }

    @Override
    public String getHumanReadableName() {
        return structuralDefinition.getHumanReadableName();
    }

    @Override
    public String toString() {
        if (auxiliaryDefinitions.isEmpty()) {
            return getDebugDumpClassName() + getMutabilityFlag() + " (" + getTypeNameLocal() + ")";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(getDebugDumpClassName()).append(getMutabilityFlag())
                    .append("(").append(getTypeNameLocal());
            for (ResourceObjectDefinition auxiliaryObjectClassDefinition : auxiliaryDefinitions) {
                sb.append(" + ").append(auxiliaryObjectClassDefinition.getTypeName().getLocalPart());
            }
            sb.append(")");
            return sb.toString();
        }
    }

    @Override
    public @NotNull String getShortIdentification() {
        return "%s with %d aux".formatted(
                structuralDefinition.getShortIdentification(), auxiliaryDefinitions.size());
    }

    private String getTypeNameLocal() {
        return getTypeName().getLocalPart();
    }

    @NotNull
    @Override
    public CompositeObjectDefinitionImpl deepClone(@NotNull DeepCloneOperation operation) {
        ResourceObjectDefinition structuralClone =
                structuralDefinition.deepClone(operation);
        List<ResourceObjectDefinition> auxiliaryClones = auxiliaryDefinitions.stream()
                .map(def -> def.deepClone(operation))
                .toList();
        return new CompositeObjectDefinitionImpl(currentLayer, structuralClone, auxiliaryClones, true);
    }

    @Override
    public @NotNull ResourceObjectTypeDefinitionType getDefinitionBean() {
        return structuralDefinition.getDefinitionBean();
    }

    @Override
    public @NotNull CompositeObjectDefinition forLayerMutable(@NotNull LayerType layer) {
        if (currentLayer == layer && isMutable()) {
            return this;
        } else {
            return createNewForLayerMutable(layer);
        }
    }

    @Override
    public @NotNull ResourceObjectDefinition forLayerImmutable(@NotNull LayerType layer) {
        if (currentLayer == layer && !isMutable()) {
            return this;
        } else {
            var clone = createNewForLayerImmutable(layer);
            clone.freeze();
            return clone;
        }
    }

    private @NotNull CompositeObjectDefinitionImpl createNewForLayerMutable(@NotNull LayerType layer) {
        return new CompositeObjectDefinitionImpl(
                layer,
                structuralDefinition.forLayerMutable(layer),
                auxiliaryDefinitions.stream()
                        .map(def -> def.forLayerMutable(layer))
                        .toList(),
                true);
    }

    private @NotNull CompositeObjectDefinitionImpl createNewForLayerImmutable(@NotNull LayerType layer) {
        return new CompositeObjectDefinitionImpl(
                layer,
                structuralDefinition.forLayerImmutable(layer),
                auxiliaryDefinitions.stream()
                        .map(def -> def.forLayerImmutable(layer))
                        .toList(),
                false);
    }

    @Override
    public @NotNull LayerType getCurrentLayer() {
        throw new UnsupportedOperationException(); // todo
    }

    @Override
    public void replaceAttributeDefinition(@NotNull QName itemName, @Nullable ItemDefinition<?> newDefinition) {

        // We replace only the first occurrence. This is consistent with how we look for attributes.
        // Note that this algorithm may break down if we add/delete attribute definitions afterwards.

        if (structuralDefinition.containsAttributeDefinition(itemName)) {
            structuralDefinition.replaceAttributeDefinition(itemName, newDefinition);
            return;
        }

        for (ResourceObjectDefinition auxiliaryDefinition : auxiliaryDefinitions) {
            if (auxiliaryDefinition.containsAttributeDefinition(itemName)) {
                auxiliaryDefinition.replaceAttributeDefinition(itemName, newDefinition);
                return;
            }
        }

        throw new IllegalArgumentException("No definition for " + itemName + " to be replaced in " + this);
    }

    @Override
    public void trimAttributesTo(@NotNull Collection<ItemPath> paths) {
        structuralDefinition.trimAttributesTo(paths);
        auxiliaryDefinitions.forEach(def -> def.trimAttributesTo(paths));
    }

    @Override
    public ResourceObjectClassDefinition.ResourceObjectClassDefinitionMutator mutator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return structuralDefinition.getAnnotation(qname);
    }

    @Override
    public @Nullable Map<QName, Object> getAnnotations() {
        return structuralDefinition.getAnnotations();
    }

    @Override
    public void performFreeze() {
        structuralDefinition.freeze();
        auxiliaryDefinitions.forEach(Freezable::freeze);
    }

    @Override
    public @NotNull QName getObjectClassName() {
        return structuralDefinition.getObjectClassName();
    }

    @Override
    public @Nullable QName getDescriptionAttributeName() {
        return structuralDefinition.getDescriptionAttributeName();
    }

    @Override
    public @Nullable QName getNamingAttributeName() {
        return structuralDefinition.getNamingAttributeName();
    }

    @Override
    public @Nullable QName getDisplayNameAttributeName() {
        return structuralDefinition.getDisplayNameAttributeName();
    }

    @Override
    public @NotNull ObjectQuery createShadowSearchQuery(String resourceOid) {
        return structuralDefinition.createShadowSearchQuery(resourceOid);
    }

    @Override
    public @NotNull ResourceObjectDefinition getStructuralDefinition() {
        return structuralDefinition;
    }

    @Override
    public @Nullable ResourceObjectTypeIdentification getTypeIdentification() {
        return structuralDefinition.getTypeIdentification();
    }

    @Override
    public @Nullable ResourceObjectTypeDefinition getTypeDefinition() {
        return structuralDefinition.getTypeDefinition();
    }

    @Override
    public boolean isDefaultFor(@NotNull ShadowKindType kind) {
        return structuralDefinition.isDefaultFor(kind);
    }

    @Override
    public @NotNull ShadowCachingPolicyType getEffectiveShadowCachingPolicy() {
        return structuralDefinition.getEffectiveShadowCachingPolicy();
    }

    @Override
    public @Nullable ItemName resolveFrameworkName(@NotNull String frameworkName) {
        return FrameworkNameResolver.findInObjectDefinition(this, frameworkName);
    }

    @Override
    public @Nullable ItemMerger getMergerInstance(@NotNull MergeStrategy strategy, @Nullable OriginMarker originMarker) {
        return structuralDefinition.getMergerInstance(strategy, originMarker);
    }

    @Override
    public @Nullable NaturalKeyDefinition getNaturalKeyInstance() {
        return structuralDefinition.getNaturalKeyInstance();
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return structuralDefinition.getSchemaContextDefinition();
    }

    @Override
    public ShadowAssociationDefinition findAssociationDefinition(QName name) {
        return structuralDefinition.findAssociationDefinition(name);
    }

    @Override
    public @NotNull List<? extends ShadowAssociationDefinition> getAssociationDefinitions() {
        return structuralDefinition.getAssociationDefinitions();
    }

    @Override
    public @NotNull Collection<ShadowAttributeDefinition<?, ?, ?, ?>> getAttributesVolatileOnAddOperation() {
        return structuralDefinition.getAttributesVolatileOnAddOperation();
    }

    @Override
    public @NotNull Collection<ShadowAttributeDefinition<?, ?, ?, ?>> getAttributesVolatileOnModifyOperation() {
        return structuralDefinition.getAttributesVolatileOnModifyOperation();
    }
}
