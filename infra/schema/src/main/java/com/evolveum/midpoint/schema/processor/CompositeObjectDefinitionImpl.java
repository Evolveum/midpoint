/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.util.*;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;

import com.evolveum.midpoint.prism.query.ObjectQuery;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import org.jetbrains.annotations.Nullable;

/**
 * Represents ad-hoc combination of definitions of structural and auxiliary object classes.
 *
 * @author semancik
 */
public class CompositeObjectDefinitionImpl
        extends AbstractFreezable
        implements CompositeObjectDefinition {
    private static final long serialVersionUID = 1L;

    private static final LayerType DEFAULT_LAYER = LayerType.MODEL;

    @NotNull private final LayerType currentLayer;
    @NotNull private final ResourceObjectDefinition structuralDefinition;
    @NotNull private final Collection<ResourceObjectDefinition> auxiliaryDefinitions;

    private PrismObjectDefinition<ShadowType> prismObjectDefinition;

    public CompositeObjectDefinitionImpl(
            @NotNull ResourceObjectDefinition structuralDefinition,
            @Nullable Collection<ResourceObjectDefinition> auxiliaryDefinitions) {
        this.currentLayer = DEFAULT_LAYER;
        this.structuralDefinition = structuralDefinition;
        this.auxiliaryDefinitions =
                Objects.requireNonNullElseGet(auxiliaryDefinitions, ArrayList::new);
    }

    private CompositeObjectDefinitionImpl(
            @NotNull LayerType currentLayer,
            @NotNull ResourceObjectDefinition structuralDefinition,
            @NotNull Collection<ResourceObjectDefinition> auxiliaryDefinitions) {
        this.currentLayer = currentLayer;
        this.structuralDefinition = structuralDefinition;
        this.auxiliaryDefinitions = auxiliaryDefinitions;
    }

    @NotNull
    @Override
    public Collection<ResourceObjectDefinition> getAuxiliaryDefinitions() {
        return auxiliaryDefinitions;
    }

    @Override
    public PrismObjectDefinition<ShadowType> getPrismObjectDefinition() {
        if (prismObjectDefinition == null) {
            prismObjectDefinition =
                    ObjectFactory.constructObjectDefinition(
                            toResourceAttributeContainerDefinition());
        }
        return prismObjectDefinition;
    }

    @Override
    public Class<?> getCompileTimeClass() {
        return structuralDefinition.getCompileTimeClass();
    }

    @Override
    public boolean isContainerMarker() {
        return structuralDefinition.isContainerMarker();
    }

    @Override
    public boolean isPrimaryIdentifier(QName attrName) {
        return structuralDefinition.isPrimaryIdentifier(attrName);
    }

    @Override
    public boolean isObjectMarker() {
        return structuralDefinition.isObjectMarker();
    }

    @Override
    public ItemProcessing getProcessing() {
        return structuralDefinition.getProcessing();
    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        return structuralDefinition.getSchemaMigrations();
    }

    @Override
    public List<ItemDiagramSpecification> getDiagrams() { return structuralDefinition.getDiagrams(); }

    @Override
    public boolean isEmphasized() {
        return structuralDefinition.isEmphasized();
    }

    @Override
    public boolean isAbstract() {
        return structuralDefinition.isAbstract();
    }

    @Override
    public QName getSuperType() {
        return structuralDefinition.getSuperType();
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
    public Collection<? extends ResourceAttributeDefinition<?>> getPrimaryIdentifiers() {
        return structuralDefinition.getPrimaryIdentifiers();
    }

    @Override
    public @NotNull Collection<QName> getPrimaryIdentifiersNames() {
        return structuralDefinition.getPrimaryIdentifiersNames();
    }

    @NotNull
    @Override
    public Collection<? extends ResourceAttributeDefinition<?>> getSecondaryIdentifiers() {
        return structuralDefinition.getSecondaryIdentifiers();
    }

    @Override
    public @NotNull Collection<QName> getSecondaryIdentifiersNames() {
        return structuralDefinition.getSecondaryIdentifiersNames();
    }

    // TODO - ok???
    @NotNull
    @Override
    public Collection<ResourceAssociationDefinition> getAssociationDefinitions() {
        return structuralDefinition.getAssociationDefinitions();
    }

    @Override
    public boolean isEmpty() {
        return structuralDefinition.isEmpty()
                && auxiliaryDefinitions.stream().noneMatch(ComplexTypeDefinition::isEmpty);
    }

    @Override
    public @NotNull Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
        return structuralDefinition.getProtectedObjectPatterns();
    }

    @Override
    public String getDisplayName() {
        return structuralDefinition.getDisplayName();
    }

    @Override
    public String getDescription() {
        return structuralDefinition.getDescription();
    }

    @Override // todo delete
    public String getResourceOid() {
        return structuralDefinition.getResourceOid();
    }

    @Override
    public @NotNull ResourceObjectClassDefinition getObjectClassDefinition() {
        return structuralDefinition.getObjectClassDefinition();
    }

    @Override
    public @NotNull ResourceObjectClassDefinition getRawObjectClassDefinition() {
        return structuralDefinition.getRawObjectClassDefinition();
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

    @NotNull
    @Override
    public List<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions() {
        // TODO cache the result
        if (auxiliaryDefinitions.isEmpty()) {
            return structuralDefinition.getAttributeDefinitions();
        }

        // Adds all attribute definitions from aux OCs that are not already known.
        List<ResourceAttributeDefinition<?>> allDefinitions =
                new ArrayList<>(structuralDefinition.getAttributeDefinitions());
        for (ResourceObjectDefinition auxiliaryObjectClassDefinition : auxiliaryDefinitions) {
            for (ResourceAttributeDefinition<?> auxRAttrDef : auxiliaryObjectClassDefinition.getAttributeDefinitions()) {
                boolean shouldAdd = true;
                for (ResourceAttributeDefinition<?> def : allDefinitions) {
                    if (def.getItemName().equals(auxRAttrDef.getItemName())) {
                        shouldAdd = false;
                        break;
                    }
                }
                if (shouldAdd) {
                    allDefinitions.add(auxRAttrDef);
                }
            }
        }
        return allDefinitions;
    }

    @Override
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {
        return getAttributeDefinitions();
    }

    @Override
    public PrismContext getPrismContext() {
        return structuralDefinition.getPrismContext();
    }

    @Override
    public void revive(PrismContext prismContext) {
        structuralDefinition.revive(prismContext);
        for (ResourceObjectDefinition auxiliaryObjectClassDefinition : auxiliaryDefinitions) {
            auxiliaryObjectClassDefinition.revive(prismContext);
        }
    }

    @Override
    public QName getExtensionForType() {
        return structuralDefinition.getExtensionForType();
    }

    @Override
    public boolean isXsdAnyMarker() {
        return structuralDefinition.isXsdAnyMarker();
    }

    @Override
    public String getDefaultNamespace() {
        return structuralDefinition.getDefaultNamespace();
    }

    @NotNull
    @Override
    public List<String> getIgnoredNamespaces() {
        return structuralDefinition.getIgnoredNamespaces();
    }

    @Override
    public ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings() {
        return structuralDefinition.getAuxiliaryObjectClassMappings();
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
    public void merge(ComplexTypeDefinition otherComplexTypeDef) {
        throw new UnsupportedOperationException("TODO implement if needed");
    }

    @Override
    public Class<?> getTypeClass() {
        return ShadowAttributesType.class;
    }

    @Override
    public PrismObject<ShadowType> createBlankShadow(String resourceOid, String tag) {
        return structuralDefinition.createBlankShadow(resourceOid, tag);
    }

    @Override
    public ResourcePasswordDefinitionType getPasswordDefinition() {
        return findInDefinitions(ResourceObjectDefinition::getPasswordDefinition);
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
        ResourceObjectDefinition structuralObjectClassDefinitionClone = structuralDefinition.clone();
        Collection<ResourceObjectDefinition> auxiliaryObjectClassDefinitionsClone =
                new ArrayList<>(this.auxiliaryDefinitions.size());
        for (ResourceObjectDefinition auxDefinition : this.auxiliaryDefinitions) {
            auxiliaryObjectClassDefinitionsClone.add(auxDefinition.clone());
        }
        return new CompositeObjectDefinitionImpl(structuralObjectClassDefinitionClone, auxiliaryObjectClassDefinitionsClone);
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
                .collect(Collectors.toCollection(ArrayList::new));
        return new CompositeObjectDefinitionImpl(structuralClone, auxiliaryClones);
    }

    @Override
    public @NotNull ResourceObjectTypeDefinitionType getDefinitionBean() {
        return structuralDefinition.getDefinitionBean();
    }

    @Override
    public CompositeObjectDefinition forLayer(@NotNull LayerType layer) {
        if (currentLayer == layer) {
            return this;
        } else {
            return new CompositeObjectDefinitionImpl(
                    layer,
                    structuralDefinition.forLayer(layer),
                    auxiliaryDefinitions.stream()
                            .map(def -> def.forLayer(layer))
                            .collect(Collectors.toList()));
        }
    }

    @Override
    public @NotNull LayerType getCurrentLayer() {
        throw new UnsupportedOperationException(); // todo
    }

    @Override
    public void replaceDefinition(@NotNull QName itemName, @Nullable ItemDefinition<?> newDefinition) {

        // We replace only the first occurrence. This is consistent with how we look for attributes.
        // Note that this algorithm may break down if we add/delete attribute definitions afterwards.

        if (structuralDefinition.containsAttributeDefinition(itemName)) {
            structuralDefinition.replaceDefinition(itemName, newDefinition);
            return;
        }

        for (ResourceObjectDefinition auxiliaryDefinition : auxiliaryDefinitions) {
            if (auxiliaryDefinition.containsAttributeDefinition(itemName)) {
                auxiliaryDefinition.replaceDefinition(itemName, newDefinition);
                return;
            }
        }

        throw new IllegalArgumentException("No definition for " + itemName + " to be replaced in " + this);
    }

    @Override
    public boolean isListMarker() {
        return structuralDefinition.isListMarker();
    }

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        structuralDefinition.trimTo(paths);
        auxiliaryDefinitions.forEach(def -> def.trimTo(paths));
    }

    @Override
    public MutableResourceObjectClassDefinition toMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isReferenceMarker() {
        return structuralDefinition.isReferenceMarker();
    }

    @NotNull
    @Override
    public Collection<TypeDefinition> getStaticSubTypes() {
        return Collections.emptySet();
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return structuralDefinition.getAnnotation(qname);
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        structuralDefinition.setAnnotation(qname, value);
    }

    @Override
    public @Nullable Map<QName, Object> getAnnotations() {
        return structuralDefinition.getAnnotations();
    }

    @Override
    public Integer getInstantiationOrder() {
        return structuralDefinition.getInstantiationOrder();
    }

    @Override
    public boolean canRepresent(QName typeName) {
        if (structuralDefinition.canRepresent(typeName)) {
            return true;
        }
        for (ResourceObjectDefinition auxiliaryObjectClassDefinition : auxiliaryDefinitions) {
            if (auxiliaryObjectClassDefinition.canRepresent(typeName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void performFreeze() {
        structuralDefinition.freeze();
        auxiliaryDefinitions.forEach(Freezable::freeze);
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
    public @NotNull ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException {
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
}
