/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

/**
 * @author semancik
 * @author mederly
 */
public final class LayerRefinedObjectClassDefinitionImpl implements LayerRefinedObjectClassDefinition {
    private static final long serialVersionUID = 1L;

    private boolean immutable;

    private final RefinedObjectClassDefinition refinedObjectClassDefinition;
    private final LayerType layer;
    /**
     * Keeps layer-specific information on resource object attributes.
     * This list is lazily evaluated.
     */
    private List<LayerRefinedAttributeDefinition<?>> layerRefinedAttributeDefinitions;

    private LayerRefinedObjectClassDefinitionImpl(RefinedObjectClassDefinition refinedAccountDefinition, LayerType layer) {
        this.refinedObjectClassDefinition = refinedAccountDefinition;
        this.layer = layer;
    }

    static LayerRefinedObjectClassDefinition wrap(RefinedObjectClassDefinition rOCD, LayerType layer) {
        if (rOCD == null) {
            return null;
        }
        return new LayerRefinedObjectClassDefinitionImpl(rOCD, layer);
    }

    static List<? extends LayerRefinedObjectClassDefinition> wrapCollection(Collection<? extends RefinedObjectClassDefinition> rOCDs, LayerType layer) {
        return rOCDs.stream()
                .map(rAccountDef -> wrap(rAccountDef, layer))
                .collect(Collectors.toCollection(() -> new ArrayList<>(rOCDs.size())));
    }

    @Override
    public LayerType getLayer() {
        return layer;
    }

    @NotNull
    @Override
    public QName getTypeName() {
        return refinedObjectClassDefinition.getTypeName();
    }

    @Override
    public boolean isIgnored() {
        return refinedObjectClassDefinition.isIgnored();
    }

    @Override
    public ItemProcessing getProcessing() {
        return refinedObjectClassDefinition.getProcessing();
    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        return refinedObjectClassDefinition.getSchemaMigrations();
    }

    public boolean isEmphasized() {
        return refinedObjectClassDefinition.isEmphasized();
    }

    @Override
    public <X> LayerRefinedAttributeDefinition<X> getDescriptionAttribute() {
        // TODO optimize
        return substituteLayerRefinedAttributeDefinition(refinedObjectClassDefinition.getDescriptionAttribute());
    }

    @Override
    @NotNull
    public List<String> getIgnoredNamespaces() {
        return refinedObjectClassDefinition.getIgnoredNamespaces();
    }

    private <X> LayerRefinedAttributeDefinition<X> substituteLayerRefinedAttributeDefinition(ResourceAttributeDefinition<?> attributeDef) {
        return findAttributeDefinition(attributeDef.getItemName());
    }

    private Collection<LayerRefinedAttributeDefinition<?>> substituteLayerRefinedAttributeDefinitionCollection(Collection<? extends RefinedAttributeDefinition<?>> attributes) {
        return attributes.stream()
                .map(this::substituteLayerRefinedAttributeDefinition)
                .collect(Collectors.toList());
    }

    @Override
    public <X> LayerRefinedAttributeDefinition<X> getNamingAttribute() {
        return substituteLayerRefinedAttributeDefinition(refinedObjectClassDefinition.getNamingAttribute());
    }

    @Override
    public String getNativeObjectClass() {
        return refinedObjectClassDefinition.getNativeObjectClass();
    }

    @Override
    public boolean isAuxiliary() {
        return refinedObjectClassDefinition.isAuxiliary();
    }

    @Override
    public Integer getDisplayOrder() {
        return refinedObjectClassDefinition.getDisplayOrder();
    }

    // TODO - doesn't return layered definition (should it?)
    @Override
    public <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path,
            @NotNull Class<ID> clazz) {
        return refinedObjectClassDefinition.findItemDefinition(path, clazz);
    }

    // TODO - doesn't return layered definition (should it?)
    @Override
    public <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest,
            @NotNull Class<ID> clazz) {
        return refinedObjectClassDefinition.findNamedItemDefinition(firstName, rest, clazz);
    }

    @Override
    public boolean isDefaultInAKind() {
        return refinedObjectClassDefinition.isDefaultInAKind();
    }

    @Override
    public ShadowKindType getKind() {
        return refinedObjectClassDefinition.getKind();
    }

    @Override
    public AttributeFetchStrategyType getPasswordFetchStrategy() {
        return refinedObjectClassDefinition.getPasswordFetchStrategy();
    }

    @Override
    public String getIntent() {
        return refinedObjectClassDefinition.getIntent();
    }

    @Override
    public LayerRefinedObjectClassDefinition forLayer(@NotNull LayerType layerType) {
        return refinedObjectClassDefinition.forLayer(layerType);
    }

    @Override
    public <X> LayerRefinedAttributeDefinition<X> getDisplayNameAttribute() {
        return substituteLayerRefinedAttributeDefinition(refinedObjectClassDefinition.getDisplayNameAttribute());
    }

    @Override
    public String getHelp() {
        return refinedObjectClassDefinition.getHelp();
    }

    @NotNull
    @Override
    public Collection<? extends LayerRefinedAttributeDefinition<?>> getPrimaryIdentifiers() {
        return substituteLayerRefinedAttributeDefinitionCollection(refinedObjectClassDefinition.getPrimaryIdentifiers());
    }

    @Override
    public Collection<? extends LayerRefinedAttributeDefinition<?>> getAllIdentifiers() {
        return substituteLayerRefinedAttributeDefinitionCollection(refinedObjectClassDefinition.getAllIdentifiers());
    }

    @NotNull
    @Override
    public Collection<? extends LayerRefinedAttributeDefinition<?>> getSecondaryIdentifiers() {
        return LayerRefinedAttributeDefinitionImpl.wrapCollection(refinedObjectClassDefinition.getSecondaryIdentifiers(), layer);
    }

    @Override
    public Class getTypeClass() {
        return refinedObjectClassDefinition.getTypeClass();
    }

    @Override
    public Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
        return refinedObjectClassDefinition.getProtectedObjectPatterns();
    }

    @Override
    public void merge(ComplexTypeDefinition otherComplexTypeDef) {
        refinedObjectClassDefinition.merge(otherComplexTypeDef);
    }

    @Override
    public PrismContext getPrismContext() {
        return refinedObjectClassDefinition.getPrismContext();
    }

    @Override
    public ResourceAttributeContainer instantiate(QName name) {
        return ObjectClassComplexTypeDefinitionImpl.instantiate(name, this);
    }

//    @Override
//    public <T> PrismPropertyDefinition<T> findPropertyDefinition(@NotNull QName name) {
//        LayerRefinedAttributeDefinition<T> def = findAttributeDefinition(name);
//        if (def != null) {
//            return def;
//        } else {
//            return LayerRefinedAttributeDefinitionImpl.wrap((RefinedAttributeDefinition<T>) refinedObjectClassDefinition.findPropertyDefinition(name), layer);
//        }
//    }

    @Override
    public <X> LayerRefinedAttributeDefinition<X> findAttributeDefinition(@NotNull QName elementQName) {
        for (LayerRefinedAttributeDefinition definition : getAttributeDefinitions()) {
            if (QNameUtil.match(definition.getItemName(), elementQName)) {
                //noinspection unchecked
                return definition;
            }
        }
        return null;
    }

    @Override
    public <X> LayerRefinedAttributeDefinition<X> findAttributeDefinition(String elementLocalName) {
        return LayerRefinedAttributeDefinitionImpl.wrap(refinedObjectClassDefinition.findAttributeDefinition(elementLocalName), layer);
    }

    @Override
    public String getDisplayName() {
        return refinedObjectClassDefinition.getDisplayName();
    }

    @NotNull
    @Override
    public List<? extends ItemDefinition> getDefinitions() {
        return getAttributeDefinitions();
    }

    @Override
    public String getDescription() {
        return refinedObjectClassDefinition.getDescription();
    }

    @Override
    public boolean isDefault() {
        return refinedObjectClassDefinition.isDefault();
    }

    @Override
    public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
        return refinedObjectClassDefinition.getObjectClassDefinition();
    }

    @NotNull
    @Override
    public List<? extends LayerRefinedAttributeDefinition<?>> getAttributeDefinitions() {
        if (layerRefinedAttributeDefinitions == null) {
            if (immutable) {
                throw new IllegalStateException("Definition is immutable: " + this);
            }
            layerRefinedAttributeDefinitions = LayerRefinedAttributeDefinitionImpl.wrapCollection(refinedObjectClassDefinition.getAttributeDefinitions(), layer);
        }
        return layerRefinedAttributeDefinitions;
    }

    @Override
    public boolean containsAttributeDefinition(ItemPathType pathType) {
        return refinedObjectClassDefinition.containsAttributeDefinition(pathType);
    }

    @Override
    public String getResourceOid() {
        return refinedObjectClassDefinition.getResourceOid();
    }

    @Override
    public PrismObjectDefinition<ShadowType> getObjectDefinition() {
        return refinedObjectClassDefinition.getObjectDefinition();
    }

    @Override
    public boolean containsAttributeDefinition(@NotNull QName attributeName) {
        return refinedObjectClassDefinition.containsAttributeDefinition(attributeName);
    }

    @Override
    public boolean isEmpty() {
        return refinedObjectClassDefinition.isEmpty();
    }

    @Override
    public PrismObject<ShadowType> createBlankShadow(RefinedObjectClassDefinition definition) {
        return refinedObjectClassDefinition.createBlankShadow(definition);
    }

    @Override
    public ResourceShadowDiscriminator getShadowDiscriminator() {
        return refinedObjectClassDefinition.getShadowDiscriminator();
    }

    @Override
    public Collection<? extends QName> getNamesOfAttributesWithOutboundExpressions() {
        return refinedObjectClassDefinition.getNamesOfAttributesWithOutboundExpressions();
    }

    @Override
    public Collection<? extends QName> getNamesOfAttributesWithInboundExpressions() {
        return refinedObjectClassDefinition.getNamesOfAttributesWithInboundExpressions();
    }

    @Override
    public List<MappingType> getPasswordInbound() {
        return refinedObjectClassDefinition.getPasswordInbound();
    }

    @Override
    public List<MappingType> getPasswordOutbound() {
        return refinedObjectClassDefinition.getPasswordOutbound();
    }

    @Override
    @Deprecated // Remove in 4.4
    public ObjectReferenceType getPasswordPolicy() {
        return refinedObjectClassDefinition.getPasswordPolicy();
    }

    public ObjectReferenceType getSecurityPolicyRef() {
        return refinedObjectClassDefinition.getSecurityPolicyRef();
    }

    @Override
    public ResourcePasswordDefinitionType getPasswordDefinition() {
        return refinedObjectClassDefinition.getPasswordDefinition();
    }

    @Override
    public Class<?> getCompileTimeClass() {
        return refinedObjectClassDefinition.getCompileTimeClass();
    }

    @Override
    public QName getExtensionForType() {
        return refinedObjectClassDefinition.getExtensionForType();
    }

    @Override
    public boolean isContainerMarker() {
        return refinedObjectClassDefinition.isContainerMarker();
    }

    @Override
    public boolean isReferenceMarker() {
        return refinedObjectClassDefinition.isReferenceMarker();
    }

    @Override
    public boolean isPrimaryIdentifier(QName attrName) {
        return refinedObjectClassDefinition.isPrimaryIdentifier(attrName);
    }

    @Override
    public boolean isObjectMarker() {
        return refinedObjectClassDefinition.isObjectMarker();
    }

    @Override
    public boolean isXsdAnyMarker() {
        return refinedObjectClassDefinition.isXsdAnyMarker();
    }

    @Override
    public boolean isListMarker() {
        return refinedObjectClassDefinition.isListMarker();
    }

    @Override
    public QName getSuperType() {
        return refinedObjectClassDefinition.getSuperType();
    }

    @Override
    public boolean isSecondaryIdentifier(QName attrName) {
        return refinedObjectClassDefinition.isSecondaryIdentifier(attrName);
    }

    @Override
    public boolean isRuntimeSchema() {
        return refinedObjectClassDefinition.isRuntimeSchema();
    }

    @NotNull
    @Override
    public Collection<RefinedAssociationDefinition> getAssociationDefinitions() {
        return refinedObjectClassDefinition.getAssociationDefinitions();
    }

    @Override
    public Collection<RefinedAssociationDefinition> getAssociationDefinitions(ShadowKindType kind) {
        return refinedObjectClassDefinition.getAssociationDefinitions(kind);
    }

    @Override
    public Collection<QName> getNamesOfAssociations() {
        return refinedObjectClassDefinition.getNamesOfAssociations();
    }

    @Override
    public ResourceActivationDefinitionType getActivationSchemaHandling() {
        return refinedObjectClassDefinition.getActivationSchemaHandling();
    }

    @Override
    public ResourceBidirectionalMappingType getActivationBidirectionalMappingType(QName propertyName) {
        return refinedObjectClassDefinition.getActivationBidirectionalMappingType(propertyName);
    }

    @Override
    public AttributeFetchStrategyType getActivationFetchStrategy(QName propertyName) {
        return refinedObjectClassDefinition.getActivationFetchStrategy(propertyName);
    }

    @Override
    public CapabilitiesType getCapabilities() {
        return refinedObjectClassDefinition.getCapabilities();
    }

    @Override
    public <T extends CapabilityType> T getEffectiveCapability(Class<T> capabilityClass, ResourceType resourceType) {
        return refinedObjectClassDefinition.getEffectiveCapability(capabilityClass, resourceType);
    }

    @Override
    public PagedSearchCapabilityType getPagedSearches(ResourceType resourceType) {
        return refinedObjectClassDefinition.getPagedSearches(resourceType);
    }

    @Override
    public boolean isPagedSearchEnabled(ResourceType resourceType) {
        return refinedObjectClassDefinition.isPagedSearchEnabled(resourceType);
    }

    @Override
    public boolean isObjectCountingEnabled(ResourceType resourceType) {
        return refinedObjectClassDefinition.isObjectCountingEnabled(resourceType);
    }

    @Override
    public boolean isAbstract() {
        return refinedObjectClassDefinition.isAbstract();
    }

    @Override
    public boolean isDeprecated() {
        return refinedObjectClassDefinition.isDeprecated();
    }

    @Override
    public String getDeprecatedSince() {
        return refinedObjectClassDefinition.getDeprecatedSince();
    }

    @Override
    public boolean isExperimental() {
        return refinedObjectClassDefinition.isExperimental();
    }

    @Override
    public String getPlannedRemoval() {
        return refinedObjectClassDefinition.getPlannedRemoval();
    }

    @Override
    public boolean isElaborate() {
        return refinedObjectClassDefinition.isElaborate();
    }

    @Override
    public String getDocumentation() {
        return refinedObjectClassDefinition.getDocumentation();
    }

    @Override
    public String getDocumentationPreview() {
        return refinedObjectClassDefinition.getDocumentationPreview();
    }

    @Override
    public RefinedAssociationDefinition findAssociationDefinition(QName name) {
        return refinedObjectClassDefinition.findAssociationDefinition(name);
    }

    @Override
    public <ID extends ItemDefinition> ID findLocalItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz,
            boolean caseInsensitive) {
        if (layerRefinedAttributeDefinitions != null) {
            for (LayerRefinedAttributeDefinition attrDef : layerRefinedAttributeDefinitions) {
                if (attrDef.isValidFor(name, clazz, caseInsensitive)) {
                    //noinspection unchecked
                    return (ID) attrDef;
                }
            }
        }

        ID def = refinedObjectClassDefinition.findLocalItemDefinition(name, clazz, caseInsensitive);
        //noinspection unchecked
        return (ID) LayerRefinedAttributeDefinitionImpl.wrap((RefinedAttributeDefinition) def, layer);
    }

    @Override
    public Collection<? extends QName> getNamesOfAssociationsWithOutboundExpressions() {
        return refinedObjectClassDefinition.getNamesOfAssociationsWithOutboundExpressions();
    }

    @Override
    public Collection<? extends QName> getNamesOfAssociationsWithInboundExpressions() {
        return refinedObjectClassDefinition.getNamesOfAssociationsWithOutboundExpressions();
    }

    @Override
    public boolean matches(ShadowType shadowType) {
        return refinedObjectClassDefinition.matches(shadowType);
    }

    @Override
    public boolean matchesWithoutIntent(ShadowType shadowType) {
        return refinedObjectClassDefinition.matchesWithoutIntent(shadowType);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((layer == null) ? 0 : layer.hashCode());
        result = prime * result + ((refinedObjectClassDefinition == null) ? 0 : refinedObjectClassDefinition.hashCode());
        return result;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;
        LayerRefinedObjectClassDefinitionImpl other = (LayerRefinedObjectClassDefinitionImpl) obj;
        if (layer != other.layer) return false;
        if (refinedObjectClassDefinition == null) {
            if (other.refinedObjectClassDefinition != null) return false;
        } else if (!refinedObjectClassDefinition.equals(other.refinedObjectClassDefinition)) {
            return false;
        }
        return true;
    }

    @Override
    public String getDefaultNamespace() {
        return refinedObjectClassDefinition.getDefaultNamespace();
    }

    @Override
    public String debugDump(int indent) {
        return RefinedObjectClassDefinitionImpl.debugDump(indent, layer, this);
    }

    // Do NOT override&delegate debugDump(int indent, LayerType layer) here.
    // We want to use code in the context of this class so things like
    // getDebugDumpClassName() will be correct.

    /**
     * Return a human readable name of this class suitable for logs.
     */
    public String getDebugDumpClassName() {
        return "LRObjectClassDef";
    }

    @Override
    public String getHumanReadableName() {
        return refinedObjectClassDefinition.getHumanReadableName();
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        visitor.visit(this);
        refinedObjectClassDefinition.accept(visitor);
    }

    // TODO reconsider
    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        visitor.visit(this);
        return refinedObjectClassDefinition.accept(visitor, visitation);
    }

    @NotNull
    @Override
    public LayerRefinedObjectClassDefinition clone() {
        return wrap(refinedObjectClassDefinition.clone(), this.layer);
    }

    @NotNull
    @Override
    public RefinedObjectClassDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        return new LayerRefinedObjectClassDefinitionImpl(refinedObjectClassDefinition.deepClone(ctdMap, onThisPath, postCloneAction), layer);
    }

    @Override
    public ResourceObjectReferenceType getBaseContext() {
        return refinedObjectClassDefinition.getBaseContext();
    }

    @Override
    public SearchHierarchyScope getSearchHierarchyScope() {
        return refinedObjectClassDefinition.getSearchHierarchyScope();
    }

    @Override
    public ResourceObjectVolatilityType getVolatility() {
        return refinedObjectClassDefinition.getVolatility();
    }

    @Override
    public ResourceObjectMultiplicityType getMultiplicity() {
        return refinedObjectClassDefinition.getMultiplicity();
    }

    @Override
    public ProjectionPolicyType getProjection() {
        return refinedObjectClassDefinition.getProjection();
    }

    @Override
    public Class getTypeClassIfKnown() {
        return refinedObjectClassDefinition.getTypeClassIfKnown();
    }

    @NotNull
    @Override
    public Collection<RefinedObjectClassDefinition> getAuxiliaryObjectClassDefinitions() {
        return refinedObjectClassDefinition.getAuxiliaryObjectClassDefinitions();
    }

    @Override
    public boolean hasAuxiliaryObjectClass(QName expectedObjectClassName) {
        return refinedObjectClassDefinition.hasAuxiliaryObjectClass(expectedObjectClassName);
    }

    @Override
    public ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings() {
        return refinedObjectClassDefinition.getAuxiliaryObjectClassMappings();
    }

    @Override
    public ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException {
        return refinedObjectClassDefinition.createShadowSearchQuery(resourceOid);
    }

    @Override
    public void revive(PrismContext prismContext) {
        refinedObjectClassDefinition.revive(prismContext);
    }

    @Override
    public String toString() {
        return "LROCDef" + layer + getMutabilityFlag() + ": " + refinedObjectClassDefinition + ")";
    }

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        if (refinedObjectClassDefinition != null) {
            refinedObjectClassDefinition.trimTo(paths);
        }
    }

    @Override
    public MutableObjectClassComplexTypeDefinition toMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShared() {
        if (refinedObjectClassDefinition != null) {
            return refinedObjectClassDefinition.isShared();
        } else {
            return true;        // ok?
        }
    }

    @NotNull
    @Override
    public Collection<TypeDefinition> getStaticSubTypes() {
        return emptySet();          // not supported for now (this type itself is not statically defined)
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return refinedObjectClassDefinition.getAnnotation(qname);
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        refinedObjectClassDefinition.setAnnotation(qname, value);
    }

    @Override
    public Integer getInstantiationOrder() {
        return null;
    }

    @Override
    public boolean canRepresent(QName typeName) {
        return refinedObjectClassDefinition.canRepresent(typeName);
    }

    @Override
    public boolean isImmutable() {
        return immutable;
    }

    @Override
    public void freeze() {
        getAttributeDefinitions();
        this.immutable = true;
    }
}
