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
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.MutableObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinitionImpl;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.SearchHierarchyScope;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;

/**
 * Used to represent combined definition of structural and auxiliary object classes.
 *
 * @author semancik
 *
 */
public class CompositeRefinedObjectClassDefinitionImpl implements CompositeRefinedObjectClassDefinition {
    private static final long serialVersionUID = 1L;

    private boolean immutable;

    @NotNull private final RefinedObjectClassDefinition structuralObjectClassDefinition;
    @NotNull private final Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions;

    private PrismObjectDefinition<ShadowType> objectDefinition;

    public CompositeRefinedObjectClassDefinitionImpl(@NotNull RefinedObjectClassDefinition structuralObjectClassDefinition, Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions) {
        this.structuralObjectClassDefinition = structuralObjectClassDefinition;
        if (auxiliaryObjectClassDefinitions != null) {
            this.auxiliaryObjectClassDefinitions = auxiliaryObjectClassDefinitions;
        } else {
            this.auxiliaryObjectClassDefinitions = new ArrayList<>();
        }
    }

    public RefinedObjectClassDefinition getStructuralObjectClassDefinition() {
        return structuralObjectClassDefinition;
    }

    @NotNull
    @Override
    public Collection<RefinedObjectClassDefinition> getAuxiliaryObjectClassDefinitions() {
        return auxiliaryObjectClassDefinitions;
    }

    @Override
    public PrismObjectDefinition<ShadowType> getObjectDefinition() {
        if (objectDefinition == null) {
            objectDefinition = RefinedObjectClassDefinitionImpl.constructObjectDefinition(this);
        }
        return objectDefinition;
    }

    @Override
    public Class<?> getCompileTimeClass() {
        return structuralObjectClassDefinition.getCompileTimeClass();
    }

    @Override
    public boolean isContainerMarker() {
        return structuralObjectClassDefinition.isContainerMarker();
    }

    @Override
    public boolean isPrimaryIdentifier(QName attrName) {
        return structuralObjectClassDefinition.isPrimaryIdentifier(attrName);
    }

    @Override
    public boolean isObjectMarker() {
        return structuralObjectClassDefinition.isObjectMarker();
    }

    @Override
    public boolean isIgnored() {
        return structuralObjectClassDefinition.isIgnored();
    }

    @Override
    public ItemProcessing getProcessing() {
        return structuralObjectClassDefinition.getProcessing();
    }

    public List<SchemaMigration> getSchemaMigrations() {
        return structuralObjectClassDefinition.getSchemaMigrations();
    }

    @Override
    public boolean isEmphasized() {
        return structuralObjectClassDefinition.isEmphasized();
    }

    @Override
    public boolean isAbstract() {
        return structuralObjectClassDefinition.isAbstract();
    }

    @Override
    public QName getSuperType() {
        return structuralObjectClassDefinition.getSuperType();
    }

    @Override
    public boolean isSecondaryIdentifier(QName attrName) {
        return structuralObjectClassDefinition.isSecondaryIdentifier(attrName);
    }

    @Override
    public boolean isDeprecated() {
        return structuralObjectClassDefinition.isDeprecated();
    }

    @Override
    public String getDeprecatedSince() {
        return structuralObjectClassDefinition.getDeprecatedSince();
    }

    @Override
    public boolean isExperimental() {
        return structuralObjectClassDefinition.isExperimental();
    }

    @Override
    public String getPlannedRemoval() {
        return structuralObjectClassDefinition.getPlannedRemoval();
    }

    @Override
    public boolean isElaborate() {
        return structuralObjectClassDefinition.isElaborate();
    }

    @Override
    public Integer getDisplayOrder() {
        return structuralObjectClassDefinition.getDisplayOrder();
    }

    @Override
    public <X> RefinedAttributeDefinition<X> getDescriptionAttribute() {
        return structuralObjectClassDefinition.getDescriptionAttribute();
    }

    @Override
    public <X> RefinedAttributeDefinition<X> getNamingAttribute() {
        return structuralObjectClassDefinition.getNamingAttribute();
    }

    @Override
    public String getHelp() {
        return structuralObjectClassDefinition.getHelp();
    }

    @NotNull
    @Override
    public QName getTypeName() {
        return structuralObjectClassDefinition.getTypeName();
    }

    @Override
    public String getNativeObjectClass() {
        return structuralObjectClassDefinition.getNativeObjectClass();
    }

    @Override
    public String getDocumentation() {
        return structuralObjectClassDefinition.getDocumentation();
    }

    @Override
    public boolean isDefaultInAKind() {
        return structuralObjectClassDefinition.isDefaultInAKind();
    }

    @Override
    public String getDocumentationPreview() {
        return structuralObjectClassDefinition.getDocumentationPreview();
    }

    @Override
    public String getIntent() {
        return structuralObjectClassDefinition.getIntent();
    }

    @Override
    public ShadowKindType getKind() {
        return structuralObjectClassDefinition.getKind();
    }

    @Override
    public boolean isRuntimeSchema() {
        return structuralObjectClassDefinition.isRuntimeSchema();
    }

    @Override
    public <X> RefinedAttributeDefinition<X> getDisplayNameAttribute() {
        return structuralObjectClassDefinition.getDisplayNameAttribute();
    }

    @NotNull
    @Override
    public Collection<? extends RefinedAttributeDefinition<?>> getPrimaryIdentifiers() {
        return structuralObjectClassDefinition.getPrimaryIdentifiers();
    }

    @NotNull
    @Override
    public Collection<? extends RefinedAttributeDefinition<?>> getSecondaryIdentifiers() {
        return structuralObjectClassDefinition.getSecondaryIdentifiers();
    }

    @Override
    public Collection<? extends RefinedAttributeDefinition<?>> getAllIdentifiers() {
        return structuralObjectClassDefinition.getAllIdentifiers();
    }

    @Override
    public boolean isAuxiliary() {
        return structuralObjectClassDefinition.isAuxiliary();
    }

    // TODO - ok???
    @NotNull
    @Override
    public Collection<RefinedAssociationDefinition> getAssociationDefinitions() {
        return structuralObjectClassDefinition.getAssociationDefinitions();
    }

    @Override
    public Collection<RefinedAssociationDefinition> getAssociationDefinitions(ShadowKindType kind) {
        return structuralObjectClassDefinition.getAssociationDefinitions(kind);
    }

    @Override
    public Collection<QName> getNamesOfAssociations() {
        return structuralObjectClassDefinition.getNamesOfAssociations();
    }

    @Override
    public boolean isEmpty() {
        return structuralObjectClassDefinition.isEmpty()
                && !auxiliaryObjectClassDefinitions.stream()
                .filter(def -> def.isEmpty())
                .findAny().isPresent();
    }

    @Override
    public Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
        return structuralObjectClassDefinition.getProtectedObjectPatterns();
    }

    @Override
    public String getDisplayName() {
        return structuralObjectClassDefinition.getDisplayName();
    }

    @Override
    public String getDescription() {
        return structuralObjectClassDefinition.getDescription();
    }

    @Override
    public boolean isDefault() {
        return structuralObjectClassDefinition.isDefault();
    }

    @Override
    public String getResourceOid() {
        return structuralObjectClassDefinition.getResourceOid();
    }

    @Override
    public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
        return structuralObjectClassDefinition.getObjectClassDefinition();
    }

    @Override
    public ResourceObjectReferenceType getBaseContext() {
        return structuralObjectClassDefinition.getBaseContext();
    }

    @Override
    public SearchHierarchyScope getSearchHierarchyScope() {
        return structuralObjectClassDefinition.getSearchHierarchyScope();
    }

    @Override
    public ResourceObjectVolatilityType getVolatility() {
        return structuralObjectClassDefinition.getVolatility();
    }

    @Override
    public ResourceObjectMultiplicityType getMultiplicity() {
        return structuralObjectClassDefinition.getMultiplicity();
    }

    @Override
    public ProjectionPolicyType getProjection() {
        return structuralObjectClassDefinition.getProjection();
    }

    @Override
    public List<MappingType> getPasswordInbound() {
        return structuralObjectClassDefinition.getPasswordInbound();
    }

    @Override
    public List<MappingType> getPasswordOutbound() {
        return structuralObjectClassDefinition.getPasswordOutbound();
    }

    @Override
    public AttributeFetchStrategyType getPasswordFetchStrategy() {
        return structuralObjectClassDefinition.getPasswordFetchStrategy();
    }

    @Override
    @Deprecated // Remove in 4.4
    public ObjectReferenceType getPasswordPolicy() {
        return structuralObjectClassDefinition.getPasswordPolicy();
    }

    public ObjectReferenceType getSecurityPolicyRef() {
        return structuralObjectClassDefinition.getSecurityPolicyRef();
    }

    @Override
    public ResourceActivationDefinitionType getActivationSchemaHandling() {
        return structuralObjectClassDefinition.getActivationSchemaHandling();
    }

    @Override
    public ResourceBidirectionalMappingType getActivationBidirectionalMappingType(QName propertyName) {
        return structuralObjectClassDefinition.getActivationBidirectionalMappingType(propertyName);
    }

    @Override
    public AttributeFetchStrategyType getActivationFetchStrategy(QName propertyName) {
        return structuralObjectClassDefinition.getActivationFetchStrategy(propertyName);
    }

    @Override
    public boolean matches(ShadowType shadowType) {
        return structuralObjectClassDefinition.matches(shadowType);
    }

    @Override
    public boolean matchesWithoutIntent(ShadowType shadowType) {
        return structuralObjectClassDefinition.matchesWithoutIntent(shadowType);
    }

    @Override
    public CapabilitiesType getCapabilities() {
        return structuralObjectClassDefinition.getCapabilities();
    }

    public <T extends CapabilityType> T getEffectiveCapability(Class<T> capabilityClass, ResourceType resourceType) {
        return structuralObjectClassDefinition.getEffectiveCapability(capabilityClass, resourceType);
    }

    @Override
    public PagedSearchCapabilityType getPagedSearches(ResourceType resourceType) {
        return structuralObjectClassDefinition.getPagedSearches(resourceType);
    }

    @Override
    public boolean isPagedSearchEnabled(ResourceType resourceType) {
        return structuralObjectClassDefinition.isPagedSearchEnabled(resourceType);
    }

    @Override
    public boolean isObjectCountingEnabled(ResourceType resourceType) {
        return structuralObjectClassDefinition.isObjectCountingEnabled(resourceType);
    }

    @Override
    public <T extends ItemDefinition> T findLocalItemDefinition(@NotNull QName name, @NotNull Class<T> clazz, boolean caseInsensitive) {
        T itemDef = structuralObjectClassDefinition.findLocalItemDefinition(name, clazz, caseInsensitive);
        if (itemDef == null) {
            for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
                itemDef = auxiliaryObjectClassDefinition.findLocalItemDefinition(name, clazz, caseInsensitive);
                if (itemDef != null) {
                    break;
                }
            }
        }
        return itemDef;
    }

    @Override
    public <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest,
            @NotNull Class<ID> clazz) {
        throw new UnsupportedOperationException();        // implement if needed
    }

    @NotNull
    @Override
    public Collection<? extends RefinedAttributeDefinition<?>> getAttributeDefinitions() {
        if (auxiliaryObjectClassDefinitions.isEmpty()) {
            return structuralObjectClassDefinition.getAttributeDefinitions();
        }
        Collection<? extends RefinedAttributeDefinition<?>> defs = new ArrayList<>();
        defs.addAll((Collection)structuralObjectClassDefinition.getAttributeDefinitions());
        for(RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
            for (RefinedAttributeDefinition<?> auxRAttrDef: auxiliaryObjectClassDefinition.getAttributeDefinitions()) {
                boolean add = true;
                for (RefinedAttributeDefinition def: defs) {
                    if (def.getItemName().equals(auxRAttrDef.getItemName())) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    ((Collection)defs).add(auxRAttrDef);
                }
            }
        }
        return defs;
    }

    @Override
    public PrismContext getPrismContext() {
        return structuralObjectClassDefinition.getPrismContext();
    }

    @Override
    public void revive(PrismContext prismContext) {
        structuralObjectClassDefinition.revive(prismContext);
        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition : auxiliaryObjectClassDefinitions) {
            auxiliaryObjectClassDefinition.revive(prismContext);
        }
    }

    @NotNull
    @Override
    public List<? extends ItemDefinition> getDefinitions() {
        return (List) getAttributeDefinitions();
    }

    @Override
    public QName getExtensionForType() {
        return structuralObjectClassDefinition.getExtensionForType();
    }

    @Override
    public boolean isXsdAnyMarker() {
        return structuralObjectClassDefinition.isXsdAnyMarker();
    }

    @Override
    public String getDefaultNamespace() {
        return structuralObjectClassDefinition.getDefaultNamespace();
    }

    @NotNull
    @Override
    public List<String> getIgnoredNamespaces() {
        return structuralObjectClassDefinition.getIgnoredNamespaces();
    }

    @Override
    public LayerRefinedObjectClassDefinition forLayer(@NotNull LayerType layerType) {
        return LayerRefinedObjectClassDefinitionImpl.wrap(this, layerType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> RefinedAttributeDefinition<X> findAttributeDefinition(QName elementQName, boolean caseInsensitive) {
        return findLocalItemDefinition(ItemName.fromQName(elementQName), RefinedAttributeDefinition.class, caseInsensitive);
    }

    @Override
    public <X> RefinedAttributeDefinition<X> findAttributeDefinition(@NotNull QName name) {
        return findAttributeDefinition(name, false);
    }

    public RefinedAssociationDefinition findAssociationDefinition(QName name) {
        for (RefinedAssociationDefinition assocType: getAssociationDefinitions()) {
            if (QNameUtil.match(assocType.getName(), name)) {
                return assocType;
            }
        }
        return null;
    }

    public Collection<RefinedAssociationDefinition> getEntitlementAssociationDefinitions() {
        return getAssociationDefinitions(ShadowKindType.ENTITLEMENT);
    }

    @Override
    public Collection<? extends QName> getNamesOfAssociationsWithOutboundExpressions() {
        return getAssociationDefinitions().stream()
                .filter(assocDef -> assocDef.getOutboundMappingType() != null)
                .map(a -> a.getName())
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public Collection<? extends QName> getNamesOfAssociationsWithInboundExpressions() {
        return getAssociationDefinitions().stream()
                .filter(assocDef -> CollectionUtils.isNotEmpty(assocDef.getInboundMappingTypes()))
                .map(a -> a.getName())
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings() {
        return structuralObjectClassDefinition.getAuxiliaryObjectClassMappings();
    }

    @Override
    public boolean hasAuxiliaryObjectClass(QName expectedObjectClassName) {
        if (structuralObjectClassDefinition.hasAuxiliaryObjectClass(expectedObjectClassName)) {
            return true;
        }
        return auxiliaryObjectClassDefinitions.stream().anyMatch(
                def -> QNameUtil.match(expectedObjectClassName, def.getTypeName()));
    }

    @Override
    public ResourceAttributeContainer instantiate(QName elementName) {
        return ObjectClassComplexTypeDefinitionImpl.instantiate(elementName, this);
    }

    @Override
    public <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        if (path.size() == 1 && path.startsWithName()) {
            return findLocalItemDefinition(ItemPath.toName(path.first()), clazz, false);
        }
        throw new UnsupportedOperationException("TODO implement if needed");
    }

    @Override
    public void merge(ComplexTypeDefinition otherComplexTypeDef) {
        throw new UnsupportedOperationException("TODO implement if needed");
    }

    // TODO
    @Override
    public Class getTypeClassIfKnown() {
        return null;
    }

    // TODO
    @Override
    public Class getTypeClass() {
        return null;
    }

    @Override
    public boolean containsAttributeDefinition(ItemPathType pathType) {
        return getRefinedObjectClassDefinitionsStream()
                .filter(def -> containsAttributeDefinition(pathType))
                .findAny()
                .isPresent();
    }

    private Stream<RefinedObjectClassDefinition> getRefinedObjectClassDefinitionsStream() {
        return Stream.concat(Stream.of(structuralObjectClassDefinition), auxiliaryObjectClassDefinitions.stream());
    }

    @Override
    public boolean containsAttributeDefinition(QName attributeName) {
        return getRefinedObjectClassDefinitionsStream()
                .filter(def -> containsAttributeDefinition(attributeName))
                .findAny()
                .isPresent();
    }

    @Override
    public ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException {
        return structuralObjectClassDefinition.createShadowSearchQuery(resourceOid);
    }

    @Override
    public PrismObject<ShadowType> createBlankShadow(RefinedObjectClassDefinition definition) {
        return structuralObjectClassDefinition.createBlankShadow(definition);
    }

    @Override
    public ResourceShadowDiscriminator getShadowDiscriminator() {
        return structuralObjectClassDefinition.getShadowDiscriminator();
    }

    @Override
    public Collection<? extends QName> getNamesOfAttributesWithOutboundExpressions() {
        Set<QName> names = new HashSet<>();
        getRefinedObjectClassDefinitionsStream().forEach(
                def -> names.addAll(def.getNamesOfAttributesWithOutboundExpressions())
        );
        return names;
    }

    @Override
    public Collection<? extends QName> getNamesOfAttributesWithInboundExpressions() {
        Set<QName> names = new HashSet<>();
        getRefinedObjectClassDefinitionsStream().forEach(
                def -> names.addAll(def.getNamesOfAttributesWithInboundExpressions())
        );
        return names;
    }

    @Override
    public ResourcePasswordDefinitionType getPasswordDefinition() {
        return findInDefinitions(def -> def.getPasswordDefinition());
    }

    private <T> T findInDefinitions(Function<RefinedObjectClassDefinition,T> transform) {
        if (structuralObjectClassDefinition != null) {
            T val = transform.apply(structuralObjectClassDefinition);
            if (val != null) {
                return val;
            }
        }
        // TODO what if there is a conflict?
        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
            T val = transform.apply(auxiliaryObjectClassDefinition);
            if (val != null) {
                return val;
            }
        }
        return null;
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        visitor.visit(this);
        structuralObjectClassDefinition.accept(visitor);
        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition : auxiliaryObjectClassDefinitions) {
            auxiliaryObjectClassDefinition.accept(visitor);
        }
    }

    // TODO reconsider this
    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        visitor.visit(this);
        structuralObjectClassDefinition.accept(visitor, visitation);
        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition : auxiliaryObjectClassDefinitions) {
            auxiliaryObjectClassDefinition.accept(visitor, visitation);
        }
        return true;
    }

    @NotNull
    @Override
    public CompositeRefinedObjectClassDefinitionImpl clone() {
        RefinedObjectClassDefinition structuralObjectClassDefinitionClone = structuralObjectClassDefinition.clone();
        Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitionsClone = null;
        auxiliaryObjectClassDefinitionsClone = new ArrayList<>(this.auxiliaryObjectClassDefinitions.size());
        for(RefinedObjectClassDefinition auxiliaryObjectClassDefinition: this.auxiliaryObjectClassDefinitions) {
            auxiliaryObjectClassDefinitionsClone.add(auxiliaryObjectClassDefinition.clone());
        }
        return new CompositeRefinedObjectClassDefinitionImpl(structuralObjectClassDefinitionClone, auxiliaryObjectClassDefinitionsClone);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((auxiliaryObjectClassDefinitions == null) ? 0 : auxiliaryObjectClassDefinitions.hashCode());
        result = prime * result
                + ((structuralObjectClassDefinition == null) ? 0 : structuralObjectClassDefinition.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
//        if (!super.equals(obj)) {
//            return false;
//        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CompositeRefinedObjectClassDefinitionImpl other = (CompositeRefinedObjectClassDefinitionImpl) obj;
        if (!auxiliaryObjectClassDefinitions.equals(other.auxiliaryObjectClassDefinitions)) {
            return false;
        }
        if (!structuralObjectClassDefinition.equals(other.structuralObjectClassDefinition)) {
            return false;
        }
        return true;
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, (LayerType) null);
    }

    protected String debugDump(int indent, LayerType layer) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(getDebugDumpClassName()).append(getMutabilityFlag()).append(": ");
        sb.append(SchemaDebugUtil.prettyPrint(getTypeName()));
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "structural", structuralObjectClassDefinition, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "auxiliary", auxiliaryObjectClassDefinitions, indent + 1);
        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    public String getDebugDumpClassName() {
        return "crOCD";
    }

    public String getHumanReadableName() {
        if (getDisplayName() != null) {
            return getDisplayName();
        } else {
            return getKind()+":"+getIntent();
        }
    }

    @Override
    public String toString() {
        if (auxiliaryObjectClassDefinitions.isEmpty()) {
            return getDebugDumpClassName() + getMutabilityFlag() + " ("+getTypeName()+")";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(getDebugDumpClassName()).append(getMutabilityFlag()).append("(").append(getTypeName());
            for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
                sb.append("+").append(auxiliaryObjectClassDefinition.getTypeName());
            }
            sb.append(")");
            return sb.toString();
        }
    }

    @NotNull
    @Override
    public CompositeRefinedObjectClassDefinitionImpl deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        RefinedObjectClassDefinition structuralClone = structuralObjectClassDefinition.deepClone(ctdMap, onThisPath, postCloneAction);
        List<RefinedObjectClassDefinition> auxiliaryClones = auxiliaryObjectClassDefinitions.stream()
                .map(def -> def.deepClone(ctdMap, onThisPath, postCloneAction))
                .collect(Collectors.toCollection(ArrayList::new));
        return new CompositeRefinedObjectClassDefinitionImpl(structuralClone, auxiliaryClones);
    }

    @Override
    public boolean isListMarker() {
        return structuralObjectClassDefinition.isListMarker();
    }

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        structuralObjectClassDefinition.trimTo(paths);
        auxiliaryObjectClassDefinitions.forEach(def -> def.trimTo(paths));
    }

    @Override
    public MutableObjectClassComplexTypeDefinition toMutable() {
        throw new UnsupportedOperationException();
    }

    // TODO
    @Override
    public boolean isShared() {
        return false;
    }

    @Override
    public boolean isReferenceMarker() {
        return structuralObjectClassDefinition.isReferenceMarker();
    }

    @NotNull
    @Override
    public Collection<TypeDefinition> getStaticSubTypes() {
        return emptySet();
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return structuralObjectClassDefinition.getAnnotation(qname);
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        structuralObjectClassDefinition.setAnnotation(qname, value);
    }

    @Override
    public Integer getInstantiationOrder() {
        return null;
    }

    @Override
    public boolean canRepresent(QName typeName) {
        if (structuralObjectClassDefinition.canRepresent(typeName)) {
            return true;
        }
        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition : auxiliaryObjectClassDefinitions) {
            if (auxiliaryObjectClassDefinition.canRepresent(typeName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void freeze() {
        structuralObjectClassDefinition.freeze();
        auxiliaryObjectClassDefinitions.forEach(Freezable::freeze);
        this.immutable = true;
    }

    @Override
    public boolean isImmutable() {
        return immutable;
    }
}
