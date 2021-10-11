/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

/**
 * @author semancik
 */
public final class RefinedObjectClassDefinitionImpl implements RefinedObjectClassDefinition {

    private static final Trace LOGGER = TraceManager.getTrace(RefinedObjectClassDefinition.class);

    @NotNull private final List<RefinedAttributeDefinition<?>> attributeDefinitions = new ArrayList<>();
    @NotNull private final List<RefinedAssociationDefinition> associationDefinitions = new ArrayList<>();

    @NotNull private final ObjectClassComplexTypeDefinition originalObjectClassDefinition;
    @NotNull private final List<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>();

    private final String resourceOid;
    private ResourceObjectTypeDefinitionType schemaHandlingObjectTypeDefinitionType;
    private String intent;
    private ShadowKindType kind;
    private String displayName;
    private String description;
    private boolean isDefault;
    private boolean shared = true;            // experimental - will be probably removed soon (replaced by immutable)
    private boolean immutable;
    @NotNull private final List<RefinedAttributeDefinition<?>> primaryIdentifiers = new ArrayList<>();
    @NotNull private final List<RefinedAttributeDefinition<?>> secondaryIdentifiers = new ArrayList<>();
    @NotNull private final List<ResourceObjectPattern> protectedObjectPatterns = new ArrayList<>();
    private ResourceObjectReferenceType baseContext;
    private SearchHierarchyScope searchHierarchyScope;
    private RefinedAttributeDefinition<?> displayNameAttributeDefinition;
    private RefinedAttributeDefinition<?> namingAttributeDefinition;
    private RefinedAttributeDefinition<?> descriptionAttributeDefinition;

    /**
     * Refined object definition. The "any" parts are replaced with appropriate schema (e.g. resource schema)
     */
    private PrismObjectDefinition<ShadowType> objectDefinition = null;

    private RefinedObjectClassDefinitionImpl(String resourceOid, @NotNull ObjectClassComplexTypeDefinition objectClassDefinition) {
        this.resourceOid = resourceOid;
        this.originalObjectClassDefinition = objectClassDefinition;
    }

    //region General attribute definitions ========================================================
    @NotNull
    @Override
    public Collection<? extends RefinedAttributeDefinition<?>> getAttributeDefinitions() {
        return Collections.unmodifiableList(attributeDefinitions);
    }

    @NotNull
    @Override
    public List<? extends ItemDefinition> getDefinitions() {
        return (List<? extends ItemDefinition>) getAttributeDefinitions();
    }

    @Override
    public Collection<? extends QName> getNamesOfAttributesWithOutboundExpressions() {
        return getAttributeDefinitions().stream()
                .filter(attrDef -> attrDef.getOutboundMappingType() != null)
                .map(attrDef -> attrDef.getItemName())
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public Collection<? extends QName> getNamesOfAttributesWithInboundExpressions() {
        return getAttributeDefinitions().stream()
                .filter(attrDef -> CollectionUtils.isNotEmpty(attrDef.getInboundMappingTypes()))
                .map(attrDef -> attrDef.getItemName())
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public <ID extends ItemDefinition> ID findLocalItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz,
            boolean caseInsensitive) {
        for (ItemDefinition def : getDefinitions()) {
            if (def.isValidFor(name, clazz, caseInsensitive)) {
                return (ID) def;
            }
        }
        return null;
    }

    //endregion

    //region Special attribute definitions ========================================================
    @NotNull
    @Override
    public Collection<RefinedAttributeDefinition<?>> getPrimaryIdentifiers() {
        return primaryIdentifiers;
    }

    @NotNull
    @Override
    public Collection<RefinedAttributeDefinition<?>> getSecondaryIdentifiers() {
        return secondaryIdentifiers;
    }

    @Override
    public <X> RefinedAttributeDefinition<X> getDescriptionAttribute() {
        return substituteRefinedAttributeDefinition(
                () -> (RefinedAttributeDefinition<X>) descriptionAttributeDefinition,
                rad -> descriptionAttributeDefinition = rad,
                originalObjectClassDefinition::getDescriptionAttribute
        );
    }

    @Override
    public <X> RefinedAttributeDefinition<X> getNamingAttribute() {
        return substituteRefinedAttributeDefinition(
                () -> (RefinedAttributeDefinition<X>) namingAttributeDefinition,
                rad -> namingAttributeDefinition = rad,
                originalObjectClassDefinition::getNamingAttribute
        );
    }

    @Override
    public <X> RefinedAttributeDefinition<X> getDisplayNameAttribute() {
        return substituteRefinedAttributeDefinition(
                () -> (RefinedAttributeDefinition<X>) displayNameAttributeDefinition,
                rad -> displayNameAttributeDefinition = rad,
                originalObjectClassDefinition::getDisplayNameAttribute
        );
    }

    private <X> RefinedAttributeDefinition<X> substituteRefinedAttributeDefinition(
            Supplier<RefinedAttributeDefinition<X>> getter, Consumer<RefinedAttributeDefinition<X>> setter,
            Supplier<ResourceAttributeDefinition<X>> getterOfOriginal) {
        RefinedAttributeDefinition<X> value = getter.get();
        if (value == null) {
            ResourceAttributeDefinition original = getterOfOriginal.get();
            if (original == null) {
                return null;
            }
            value = findAttributeDefinition(original.getItemName());
            setter.accept(value);
        }
        return value;
    }
    //endregion

    //region General association definitions ========================================================
    @NotNull
    @Override
    public Collection<RefinedAssociationDefinition> getAssociationDefinitions() {
        return Collections.unmodifiableList(associationDefinitions);
    }

    @Override
    public Collection<RefinedAssociationDefinition> getAssociationDefinitions(ShadowKindType kind) {
        return Collections.unmodifiableList(
                associationDefinitions.stream()
                        .filter(association -> kind == association.getKind())
                        .collect(Collectors.toList()));
    }

    @Override
    public RefinedAssociationDefinition findAssociationDefinition(QName name) {
        return associationDefinitions.stream()
                .filter(a -> QNameUtil.match(a.getName(), name))
                .findFirst().orElse(null);
    }

    @Override
    public Collection<QName> getNamesOfAssociations() {
        return getAssociationDefinitions().stream()
                .map(a -> a.getName())
                .collect(Collectors.toCollection(HashSet::new));
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

    //endregion

    //region General information ========================================================
    @Override
    public String getDisplayName() {
        return displayName;
    }

    private void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
        return originalObjectClassDefinition;
    }

    @Override
    public String getResourceOid() {
        return resourceOid;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    private void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public boolean isDefaultInAKind() {
        return isDefault;
    }

    @Override
    public ResourceObjectReferenceType getBaseContext() {
        return baseContext;
    }

    private void setBaseContext(ResourceObjectReferenceType baseContext) {
        this.baseContext = baseContext;
    }

    @Override
    public SearchHierarchyScope getSearchHierarchyScope() {
        return searchHierarchyScope;
    }

    public void setSearchHierarchyScope(SearchHierarchyScope searchHierarchyScope) {
        this.searchHierarchyScope = searchHierarchyScope;
    }

    @Override
    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    @Override
    public ShadowKindType getKind() {
        if (kind != null) {
            return kind;
        }
        return getObjectClassDefinition().getKind();
    }

    public void setKind(ShadowKindType kind) {
        this.kind = kind;
    }

    @Override
    public ResourceObjectVolatilityType getVolatility() {
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return null;
        }
        return schemaHandlingObjectTypeDefinitionType.getVolatility();
    }

    @Override
    public ResourceObjectMultiplicityType getMultiplicity() {
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return null;
        }
        return schemaHandlingObjectTypeDefinitionType.getMultiplicity();
    }

    @Override
    public ProjectionPolicyType getProjection() {
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return null;
        }
        return schemaHandlingObjectTypeDefinitionType.getProjection();
    }

    @Override
    public boolean canRepresent(QName typeName) {
        return originalObjectClassDefinition.canRepresent(typeName);
    }

    //endregion

    //region Generating and matching artifacts ========================================================

    @Override
    public PrismObjectDefinition<ShadowType> getObjectDefinition() {
        if (objectDefinition == null) {
            objectDefinition = constructObjectDefinition(this);
        }
        return objectDefinition;
    }

    static PrismObjectDefinition<ShadowType> constructObjectDefinition(RefinedObjectClassDefinition refinedObjectClassDefinition) {
        // Almost-shallow clone of object definition and complex type
        PrismObjectDefinition<ShadowType> originalObjectDefinition =
                refinedObjectClassDefinition.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        return originalObjectDefinition.cloneWithReplacedDefinition(ShadowType.F_ATTRIBUTES,
                refinedObjectClassDefinition.toResourceAttributeContainerDefinition());
    }

    @Override
    public PrismObject<ShadowType> createBlankShadow(RefinedObjectClassDefinition definition) {
        PrismObject<ShadowType> accountShadow;
        try {
            accountShadow = getPrismContext().createObject(ShadowType.class);
        } catch (SchemaException e) {
            // This should not happen
            throw new SystemException("Internal error instantiating account shadow: "+e.getMessage(), e);
        }
        ShadowType accountShadowType = accountShadow.asObjectable();

        accountShadowType
            .intent(getIntent())
            .kind(getKind())
            .objectClass(getObjectClassDefinition().getTypeName())
            .resourceRef(getResourceOid(), ResourceType.COMPLEX_TYPE);

        // Setup definition
        PrismObjectDefinition<ShadowType> newDefinition = accountShadow.getDefinition().cloneWithReplacedDefinition(
                ShadowType.F_ATTRIBUTES, definition.toResourceAttributeContainerDefinition());
        accountShadow.setDefinition(newDefinition);

        return accountShadow;
    }

    @Override
    public ResourceShadowDiscriminator getShadowDiscriminator() {
        return new ResourceShadowDiscriminator(getResourceOid(), getKind(), getIntent(), null, false);
    }

    @Override
    public boolean matches(ShadowType shadowType) {
        if (!matchesWithoutIntent(shadowType)) {
            return false;
        }
        if (shadowType.getIntent() != null) {
            //            if (isDefault) {
            //                return true;
            //            } else {
            //                return false;
            //            }
            //        } else {
            return MiscUtil.equals(intent, shadowType.getIntent());
        }
        return true;
    }

    @Override
    public boolean matchesWithoutIntent(ShadowType shadowType) {
        if (shadowType == null) {
            return false;
        }
        if (!QNameUtil.match(getObjectClassDefinition().getTypeName(), shadowType.getObjectClass())) {
            return false;
        }
        if (shadowType.getKind() == null) {
            if (kind != ShadowKindType.ACCOUNT) {
                return false;
            }
        } else {
            if (!MiscUtil.equals(kind, shadowType.getKind())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException {
        if (getKind() == null) {
            return ObjectQueryUtil.createResourceAndObjectClassQuery(resourceOid, getTypeName(), getPrismContext());
        } else {
            return ObjectQueryUtil.createResourceAndKindIntent(resourceOid, getKind(), getIntent(), getPrismContext());
        }
    }
    //endregion

    //region Accessing parts of schema handling ========================================================
    @NotNull
    @Override
    public Collection<RefinedObjectClassDefinition> getAuxiliaryObjectClassDefinitions() {
        return auxiliaryObjectClassDefinitions;
    }

    @Override
    public boolean hasAuxiliaryObjectClass(QName expectedObjectClassName) {
        return auxiliaryObjectClassDefinitions.stream()
                .anyMatch(def -> QNameUtil.match(def.getTypeName(), expectedObjectClassName));
    }

    @Override
    public ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings() {
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return null;
        }
        return schemaHandlingObjectTypeDefinitionType.getAuxiliaryObjectClassMappings();
    }

    @Override
    public Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
        return protectedObjectPatterns;
    }

    @Override
    public ResourcePasswordDefinitionType getPasswordDefinition() {
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return null;
        }
        ResourceCredentialsDefinitionType credentials = schemaHandlingObjectTypeDefinitionType.getCredentials();
        if (credentials == null) {
            return null;
        }
        return credentials.getPassword();
    }

    @Override
    public List<MappingType> getPasswordInbound() {
        ResourcePasswordDefinitionType password = getPasswordDefinition();
        if (password == null || password.getInbound() == null) {
            return null;
        }
        return password.getInbound();
    }

    @Override
    public List<MappingType> getPasswordOutbound() {
        ResourcePasswordDefinitionType password = getPasswordDefinition();
        if (password == null || password.getOutbound() == null) {
            return null;
        }
        return password.getOutbound();
    }

    @Override
    public AttributeFetchStrategyType getPasswordFetchStrategy() {
        ResourcePasswordDefinitionType password = getPasswordDefinition();
        if (password == null) {
            return AttributeFetchStrategyType.IMPLICIT;
        }
        if (password.getFetchStrategy() == null) {
            return AttributeFetchStrategyType.IMPLICIT;
        }
        return password.getFetchStrategy();
    }


    @Override
    public ObjectReferenceType getSecurityPolicyRef() {
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return null;
        }
        return schemaHandlingObjectTypeDefinitionType.getSecurityPolicyRef();
    }

    @Override
    @Deprecated // Remove in 4.4
    public ObjectReferenceType getPasswordPolicy() {
        ResourcePasswordDefinitionType password = getPasswordDefinition();
        if (password == null || password.getPasswordPolicyRef() == null){
            return null;
        }
        return password.getPasswordPolicyRef();
    }


    @Override
    public ResourceActivationDefinitionType getActivationSchemaHandling(){
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return null;
        }
        return schemaHandlingObjectTypeDefinitionType.getActivation();
    }

    @Override
    public ResourceBidirectionalMappingType getActivationBidirectionalMappingType(QName propertyName) {
        ResourceActivationDefinitionType activationSchemaHandling = getActivationSchemaHandling();
        if (activationSchemaHandling == null) {
            return null;
        }
        if (QNameUtil.match(ActivationType.F_ADMINISTRATIVE_STATUS, propertyName)) {
            return activationSchemaHandling.getAdministrativeStatus();
        } else if (QNameUtil.match(ActivationType.F_VALID_FROM, propertyName)) {
            return activationSchemaHandling.getValidFrom();
        } else if (QNameUtil.match(ActivationType.F_VALID_TO, propertyName)) {
            return activationSchemaHandling.getValidTo();
        } else if (QNameUtil.match(ActivationType.F_LOCKOUT_STATUS, propertyName)) {
            return activationSchemaHandling.getLockoutStatus();
        } else if (QNameUtil.match(ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP, propertyName)) {
            return null;            // todo implement this
        } else {
            throw new IllegalArgumentException("Unknown activation property "+propertyName);
        }
    }

    @Override
    public AttributeFetchStrategyType getActivationFetchStrategy(QName propertyName) {
        ResourceBidirectionalMappingType biType = getActivationBidirectionalMappingType(propertyName);
        if (biType == null) {
            return AttributeFetchStrategyType.IMPLICIT;
        }
        if (biType.getFetchStrategy() == null) {
            return AttributeFetchStrategyType.IMPLICIT;
        }
        return biType.getFetchStrategy();
    }
    //endregion

    //region Capabilities ========================================================
    @Override
    public CapabilitiesType getCapabilities() {
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return null;
        }

        CapabilityCollectionType configuredCapabilities = schemaHandlingObjectTypeDefinitionType.getConfiguredCapabilities();
        if (configuredCapabilities == null) {
            return null;
        }
        CapabilitiesType capabilitiesType = new CapabilitiesType(getPrismContext());
        capabilitiesType.setConfigured(configuredCapabilities);
        return  capabilitiesType;
    }

    @Override
    public <T extends CapabilityType> T getEffectiveCapability(Class<T> capabilityClass, ResourceType resourceType) {
        return ResourceTypeUtil.getEffectiveCapability(resourceType, schemaHandlingObjectTypeDefinitionType, capabilityClass);
    }

    @Override
    public PagedSearchCapabilityType getPagedSearches(ResourceType resourceType) {
        return getEffectiveCapability(PagedSearchCapabilityType.class, resourceType);
    }

    @Override
    public boolean isPagedSearchEnabled(ResourceType resourceType) {
        return getPagedSearches(resourceType) != null;          // null means nothing or disabled
    }

    @Override
    public boolean isObjectCountingEnabled(ResourceType resourceType) {
        return getEffectiveCapability(CountObjectsCapabilityType.class, resourceType) != null;
    }
    //endregion

    @Override
    public void accept(Visitor<Definition> visitor) {
        visitor.visit(this);
        originalObjectClassDefinition.accept(visitor);

        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition : auxiliaryObjectClassDefinitions) {
            auxiliaryObjectClassDefinition.accept(visitor);
        }
        for (RefinedAttributeDefinition<?> attributeDefinition : attributeDefinitions) {
            attributeDefinition.accept(visitor);
        }
        for (RefinedAssociationDefinition associationDefinition : associationDefinitions) {
            associationDefinition.accept(visitor);
        }
    }

    //TODO reconsider this
    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        visitor.visit(this);
        originalObjectClassDefinition.accept(visitor, visitation);

        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition : auxiliaryObjectClassDefinitions) {
            auxiliaryObjectClassDefinition.accept(visitor, visitation);
        }
        for (RefinedAttributeDefinition<?> attributeDefinition : attributeDefinitions) {
            attributeDefinition.accept(visitor, visitation);
        }
        for (RefinedAssociationDefinition associationDefinition : associationDefinitions) {
            associationDefinition.accept(visitor);
        }
        return true;
    }

    //region Cloning ========================================================
    @NotNull
    @Override
    public RefinedObjectClassDefinitionImpl clone() {
        RefinedObjectClassDefinitionImpl clone = new RefinedObjectClassDefinitionImpl(resourceOid, originalObjectClassDefinition);
        copyDefinitionData(clone);
        shared = false;
        return clone;
    }

    // assuming we are called on empty object
    private void copyDefinitionData(RefinedObjectClassDefinitionImpl clone) {
        clone.attributeDefinitions.addAll(cloneDefinitions(this.attributeDefinitions));
        clone.associationDefinitions.addAll(cloneAssociations(this.associationDefinitions));
        clone.auxiliaryObjectClassDefinitions.addAll(auxiliaryObjectClassDefinitions);
        clone.schemaHandlingObjectTypeDefinitionType = this.schemaHandlingObjectTypeDefinitionType;
        clone.intent = this.intent;
        clone.kind = this.kind;
        clone.displayName = this.displayName;
        clone.description = this.description;
        clone.isDefault = this.isDefault;
        clone.primaryIdentifiers.addAll(cloneDefinitions(this.primaryIdentifiers));
        clone.secondaryIdentifiers.addAll(cloneDefinitions(this.secondaryIdentifiers));
        clone.protectedObjectPatterns.addAll(this.protectedObjectPatterns);
        clone.baseContext = this.baseContext;
    }

    @NotNull
    @Override
    public RefinedObjectClassDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        // TODO TODO TODO (note that in original implementation this was also missing...)
        RefinedObjectClassDefinitionImpl clone = new RefinedObjectClassDefinitionImpl(resourceOid, originalObjectClassDefinition.deepClone(ctdMap, onThisPath, postCloneAction));
        copyDefinitionData(clone);
        shared = false;
        return clone;
    }

    private Collection<RefinedAssociationDefinition> cloneAssociations(Collection<RefinedAssociationDefinition> origAsoc) {
        return origAsoc.stream()
                .map(RefinedAssociationDefinition::clone)
                .collect(Collectors.toList());
    }

    private List<? extends RefinedAttributeDefinition<?>> cloneDefinitions(Collection<? extends RefinedAttributeDefinition<?>> origDefs) {
        return origDefs.stream()
                .map(RefinedAttributeDefinition::clone)
                .collect(Collectors.toList());
    }

    //endregion

    /**
     * Creates a derived version of this ROCD for a given layer.
     * TODO clone if necessary/if specified (currently there is no cloning)
     *
     * @param layerType
     * @return
     */
    @Override
    public LayerRefinedObjectClassDefinition forLayer(@NotNull LayerType layerType) {
        Validate.notNull(layerType);
        return LayerRefinedObjectClassDefinitionImpl.wrap(this, layerType);
    }


    //region Delegations ========================================================
    @NotNull
    @Override
    public QName getTypeName() {
        return getObjectClassDefinition().getTypeName();
    }

    @Override
    public String getNativeObjectClass() {
        return getObjectClassDefinition().getNativeObjectClass();
    }

    public boolean isAuxiliary() {
        return getObjectClassDefinition().isAuxiliary();
    }

    @Override
    public PrismContext getPrismContext() {
        return originalObjectClassDefinition.getPrismContext();
    }

    @NotNull
    @Override
    public Collection<TypeDefinition> getStaticSubTypes() {
        return emptySet();          // not supported for now (this type itself is not statically defined)
    }

    @Nullable
    @Override
    public Class<?> getCompileTimeClass() {
        return originalObjectClassDefinition.getCompileTimeClass();        // most probably null
    }

    @Nullable
    @Override
    public QName getExtensionForType() {
        return originalObjectClassDefinition.getExtensionForType();        // most probably null
    }

    @Override
    public boolean isReferenceMarker() {
        return originalObjectClassDefinition.isReferenceMarker();            // most probably false
    }

    @Override
    public boolean isContainerMarker() {
        return originalObjectClassDefinition.isContainerMarker();            // most probably false
    }

    @Override
    public boolean isObjectMarker() {
        return originalObjectClassDefinition.isObjectMarker();         // most probably false
    }

    @Override
    public boolean isXsdAnyMarker() {
        return originalObjectClassDefinition.isXsdAnyMarker();
    }

    // TODO
    @Override
    public <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        if (path.size() != 1) {
            return null;
        }
        ItemName first = path.firstToNameOrNull();
        if (first == null) {
            return null;
        }
        return findLocalItemDefinition(first.asSingleName(), clazz, false);
    }

    // TODO
    @Override
    public <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest,
            @NotNull Class<ID> clazz) {
        return findItemDefinition(ItemName.fromQName(firstName));
    }

    @Nullable
    @Override
    public String getDefaultNamespace() {
        return originalObjectClassDefinition.getDefaultNamespace();
    }

    @Override
    public boolean isRuntimeSchema() {
        return originalObjectClassDefinition.isRuntimeSchema();
    }

    @NotNull
    @Override
    public List<String> getIgnoredNamespaces() {
        return originalObjectClassDefinition.getIgnoredNamespaces();
    }

    @Nullable
    @Override
    public QName getSuperType() {
        return originalObjectClassDefinition.getSuperType();
    }

    @Override
    public void merge(ComplexTypeDefinition otherComplexTypeDef) {
        throw new UnsupportedOperationException("TODO implement this");
    }

    @Override
    public void revive(PrismContext prismContext) {
        originalObjectClassDefinition.revive(prismContext);
        // TODO revive attributes
    }

    @Override
    public boolean isIgnored() {
        return originalObjectClassDefinition.isIgnored();
    }

    @Override
    public ItemProcessing getProcessing() {
        return originalObjectClassDefinition.getProcessing();
    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        return originalObjectClassDefinition.getSchemaMigrations();
    }

    @Override
    public boolean isAbstract() {
        return originalObjectClassDefinition.isAbstract();
    }

    @Override
    public boolean isEmpty() {
        return attributeDefinitions.isEmpty() && associationDefinitions.isEmpty();
    }

    @Override
    public boolean isDeprecated() {
        return originalObjectClassDefinition.isDeprecated();
    }

    @Override
    public String getDeprecatedSince() {
        return originalObjectClassDefinition.getDeprecatedSince();
    }

    @Override
    public boolean isExperimental() {
        return originalObjectClassDefinition.isExperimental();
    }

    @Override
    public String getPlannedRemoval() {
        return originalObjectClassDefinition.getPlannedRemoval();
    }

    @Override
    public boolean isElaborate() {
        return originalObjectClassDefinition.isElaborate();
    }

    @Override
    public boolean isEmphasized() {
        return originalObjectClassDefinition.isEmphasized();
    }

    @Override
    public Integer getDisplayOrder() {
        return originalObjectClassDefinition.getDisplayOrder();
    }

    @Override
    public String getHelp() {
        return originalObjectClassDefinition.getHelp();
    }

    @Override
    public String getDocumentation() {
        return originalObjectClassDefinition.getDocumentation();
    }

    @Override
    public String getDocumentationPreview() {
        return originalObjectClassDefinition.getDocumentationPreview();
    }

    @Override
    public Class getTypeClassIfKnown() {
        return originalObjectClassDefinition.getTypeClassIfKnown();
    }

    @Override
    public Class getTypeClass() {
        return originalObjectClassDefinition.getTypeClass();
    }

    @Override
    public ResourceAttributeContainer instantiate(QName elementName) {
        return ObjectClassComplexTypeDefinitionImpl.instantiate(elementName, this);
    }

    @Override
    public boolean isListMarker() {
        return originalObjectClassDefinition.isListMarker();
    }

    //endregion

    //region ==== Parsing =================================================================================

    static RefinedObjectClassDefinition parse(ResourceObjectTypeDefinitionType entTypeDefType,
            ResourceType resourceType, RefinedResourceSchema rSchema, ShadowKindType impliedKind, PrismContext prismContext,
            String contextDescription) throws SchemaException {

        ShadowKindType kind = entTypeDefType.getKind();
        if (kind == null) {
            kind = impliedKind;
        }
        if (kind == null) {
            kind = ShadowKindType.ACCOUNT;
        }
        String intent = entTypeDefType.getIntent();
        if (intent == null) {
            intent = SchemaConstants.INTENT_DEFAULT;
        }
        RefinedObjectClassDefinition rObjectClassDef = parseRefinedObjectClass(entTypeDefType,
                resourceType, rSchema, prismContext, kind, intent, kind.value(), kind.value() + " type definition '"+intent+"' in " + contextDescription);

        return rObjectClassDef;
    }

    private static void parseProtected(RefinedObjectClassDefinition rAccountDef,
            ResourceObjectTypeDefinitionType accountTypeDefType, PrismContext prismContext) throws SchemaException {
        for (ResourceObjectPatternType protectedType: accountTypeDefType.getProtected()) {
            ResourceObjectPattern protectedPattern = convertToPattern(protectedType, rAccountDef, prismContext);
            rAccountDef.getProtectedObjectPatterns().add(protectedPattern);
        }
    }

    private static ResourceObjectPattern convertToPattern(ResourceObjectPatternType patternType,
            RefinedObjectClassDefinition rAccountDef, PrismContext prismContext) throws SchemaException {
        ResourceObjectPattern resourceObjectPattern = new ResourceObjectPattern(rAccountDef);
        SearchFilterType filterType = patternType.getFilter();
        if (filterType == null) {
            throw new SchemaException("No filter in resource object pattern");
        } else {
            ObjectFilter filter = prismContext.getQueryConverter().parseFilter(filterType, rAccountDef.getObjectDefinition());
            resourceObjectPattern.addFilter(filter);
            return resourceObjectPattern;
        }
    }

    public static RefinedObjectClassDefinition parseFromSchema(ObjectClassComplexTypeDefinition objectClassDef, ResourceType resourceType,
                                                        RefinedResourceSchema rSchema,
                                                        PrismContext prismContext, String contextDescription) throws SchemaException {

        RefinedObjectClassDefinitionImpl rOcDef = new RefinedObjectClassDefinitionImpl(resourceType.getOid(), objectClassDef);

        String intent = objectClassDef.getIntent();
        if (intent == null && objectClassDef.isDefaultInAKind()) {
            intent = SchemaConstants.INTENT_DEFAULT;
        }
        rOcDef.setIntent(intent);

        if (objectClassDef.getDisplayName() != null) {
            rOcDef.setDisplayName(objectClassDef.getDisplayName());
        }

        rOcDef.setDefault(objectClassDef.isDefaultInAKind());

        for (ResourceAttributeDefinition attrDef : objectClassDef.getAttributeDefinitions()) {
            String attrContextDescription = intent + ", in " + contextDescription;

            RefinedAttributeDefinition rAttrDef = RefinedAttributeDefinitionImpl.parse(attrDef, null, objectClassDef, prismContext,
                    attrContextDescription);
            rOcDef.processIdentifiers(rAttrDef, objectClassDef);

            if (rOcDef.containsAttributeDefinition(rAttrDef.getItemName())) {
                throw new SchemaException("Duplicate definition of attribute " + rAttrDef.getItemName() + " in " + attrContextDescription);
            }
            rOcDef.add(rAttrDef);

        }

        return rOcDef;

    }

    private static RefinedObjectClassDefinition parseRefinedObjectClass(ResourceObjectTypeDefinitionType schemaHandlingObjDefType,
            ResourceType resourceType, RefinedResourceSchema rSchema, PrismContext prismContext,
            @NotNull ShadowKindType kind, @NotNull String intent, String typeDesc, String contextDescription) throws SchemaException {

        ObjectClassComplexTypeDefinition objectClassDef;
        if (schemaHandlingObjDefType.getObjectClass() != null) {
            QName objectClass = schemaHandlingObjDefType.getObjectClass();
            objectClassDef = rSchema.getOriginalResourceSchema().findObjectClassDefinition(objectClass);
            if (objectClassDef == null) {
                throw new SchemaException("Object class " + objectClass + " was not found in " + contextDescription);
            }
        } else {
            throw new SchemaException("Definition of "+typeDesc+" type " + schemaHandlingObjDefType.getIntent() + " does not have objectclass, in " + contextDescription);
        }

        RefinedObjectClassDefinitionImpl rOcDef = new RefinedObjectClassDefinitionImpl(resourceType.getOid(), objectClassDef);
        rOcDef.setKind(kind);
        rOcDef.setIntent(intent);
        // clone here to disassociate this definition from the resource. So this definition can be serialized without the need to serialize
        // entire resource. If we do not clone then the resource will be present here through parent in the schemaHandlingObjDefType
        rOcDef.schemaHandlingObjectTypeDefinitionType = schemaHandlingObjDefType.clone();

        if (rOcDef.schemaHandlingObjectTypeDefinitionType.getDisplayName() != null) {
            rOcDef.setDisplayName(rOcDef.schemaHandlingObjectTypeDefinitionType.getDisplayName());
        } else {
            if (objectClassDef.getDisplayName() != null) {
                rOcDef.setDisplayName(objectClassDef.getDisplayName());
            }
        }

        if (rOcDef.schemaHandlingObjectTypeDefinitionType.getDescription() != null) {
            rOcDef.setDescription(rOcDef.schemaHandlingObjectTypeDefinitionType.getDescription());
        }

        if (rOcDef.schemaHandlingObjectTypeDefinitionType.isDefault() != null) {
            rOcDef.setDefault(rOcDef.schemaHandlingObjectTypeDefinitionType.isDefault());
        } else {
            rOcDef.setDefault(objectClassDef.isDefaultInAKind());
        }

        if (rOcDef.schemaHandlingObjectTypeDefinitionType.getBaseContext() != null) {
            rOcDef.setBaseContext(rOcDef.schemaHandlingObjectTypeDefinitionType.getBaseContext());
        }

        if (rOcDef.schemaHandlingObjectTypeDefinitionType.getSearchHierarchyScope() != null) {
            switch (rOcDef.schemaHandlingObjectTypeDefinitionType.getSearchHierarchyScope()) {
                case ONE:
                    rOcDef.setSearchHierarchyScope(SearchHierarchyScope.ONE);
                    break;
                case SUB:
                    rOcDef.setSearchHierarchyScope(SearchHierarchyScope.SUB);
                    break;
                default:
                    throw new SchemaException("Unknown search hierarchy scope: "+rOcDef.schemaHandlingObjectTypeDefinitionType.getSearchHierarchyScope());
            }
        }

        return rOcDef;
    }

    void parseAssociations(RefinedResourceSchema rSchema) throws SchemaException {
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return;
        }
        for (ResourceObjectAssociationType resourceObjectAssociationType: schemaHandlingObjectTypeDefinitionType.getAssociation()) {
            RefinedAssociationDefinition rAssocDef = new RefinedAssociationDefinition(resourceObjectAssociationType);
            ShadowKindType assocKind = rAssocDef.getKind();
            RefinedObjectClassDefinition assocTarget = rSchema.getRefinedDefinition(assocKind, rAssocDef.getIntents());
            rAssocDef.setAssociationTarget(assocTarget);
            associationDefinitions.add(rAssocDef);
        }
    }

    void parseAuxiliaryObjectClasses(RefinedResourceSchema rSchema) throws SchemaException {
        if (schemaHandlingObjectTypeDefinitionType == null) {
            return;
        }
        List<QName> auxiliaryObjectClassQNames = schemaHandlingObjectTypeDefinitionType.getAuxiliaryObjectClass();
        for (QName auxiliaryObjectClassQName: auxiliaryObjectClassQNames) {
            RefinedObjectClassDefinition auxiliaryObjectClassDef = rSchema.getRefinedDefinition(auxiliaryObjectClassQName);
            if (auxiliaryObjectClassDef == null) {
                throw new SchemaException("Auxiliary object class "+auxiliaryObjectClassQName+" specified in "+this+" does not exist");
            }
            auxiliaryObjectClassDefinitions.add(auxiliaryObjectClassDef);
        }
    }

    void parseAttributes(RefinedResourceSchema rSchema, String contextDescription) throws SchemaException {
        if (schemaHandlingObjectTypeDefinitionType == null) {
            // this is definition from schema. We already have all we need.
            return;
        }

        parseAttributesFrom(rSchema, getObjectClassDefinition(), false, contextDescription);
        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
            parseAttributesFrom(rSchema, auxiliaryObjectClassDefinition, true, contextDescription);
        }

        // Check for extra attribute definitions in the account type
        for (ResourceAttributeDefinitionType attrDefType : schemaHandlingObjectTypeDefinitionType.getAttribute()) {
            if (!containsAttributeDefinition(attrDefType.getRef()) && !RefinedAttributeDefinitionImpl.isIgnored(attrDefType)) {
                throw new SchemaException("Definition of attribute " + attrDefType.getRef() + " not found in object class " + originalObjectClassDefinition
                        .getTypeName() + " as defined in " + contextDescription);
            }
        }

        parseProtected(this, schemaHandlingObjectTypeDefinitionType, getPrismContext());
    }

    private void parseAttributesFrom(RefinedResourceSchema rSchema, ObjectClassComplexTypeDefinition ocDef, boolean auxiliary,
            String contextDescription) throws SchemaException {
        if (schemaHandlingObjectTypeDefinitionType == null) {
            // this is definition from schema. We already have all we need.
            return;
        }
        for (ResourceAttributeDefinition road : ocDef.getAttributeDefinitions()) {
            String attrContextDescription = road.getItemName() + ", in " + contextDescription;
            ResourceAttributeDefinitionType attrDefType = findAttributeDefinitionType(road.getItemName(), schemaHandlingObjectTypeDefinitionType,
                    attrContextDescription);
            // We MUST NOT skip ignored attribute definitions here. We must include them in the schema as
            // the shadows will still have that attributes and we will need their type definition to work
            // well with them. They may also be mandatory. We cannot pretend that they do not exist.

            // TODO !!!! fix the cast
            RefinedAttributeDefinition<?> rAttrDef = RefinedAttributeDefinitionImpl.parse(road, attrDefType, ocDef,
                    rSchema.getPrismContext(), "in "+kind+" type " + intent + ", in " + contextDescription);
            if (!auxiliary) {
                processIdentifiers(rAttrDef, ocDef);
            }

            if (containsAttributeDefinition(rAttrDef.getItemName())) {
                if (auxiliary) {
                    continue;
                } else {
                    throw new SchemaException("Duplicate definition of attribute " + rAttrDef.getItemName() + " in "+kind+" type " +
                        intent + ", in " + contextDescription);
                }
            }
            add(rAttrDef);

            if (rAttrDef.isDisplayNameAttribute()) {
                displayNameAttributeDefinition = rAttrDef;
            }
        }
    }

    private void processIdentifiers(RefinedAttributeDefinition rAttrDef, ObjectClassComplexTypeDefinition objectClassDef) {
        QName attrName = rAttrDef.getItemName();

        if (objectClassDef.isPrimaryIdentifier(attrName)) {
            ((Collection)getPrimaryIdentifiers()).add(rAttrDef);
        }

        if (rAttrDef.isSecondaryIdentifierOverride() == null) {
            if (objectClassDef.isSecondaryIdentifier(attrName)) {
                ((Collection)getSecondaryIdentifiers()).add(rAttrDef);
            }
        } else if (rAttrDef.isSecondaryIdentifierOverride()) {
            ((Collection)getSecondaryIdentifiers()).add(rAttrDef);
        }
    }

    private ResourceAttributeDefinitionType findAttributeDefinitionType(QName attrName,
            ResourceObjectTypeDefinitionType rOcDefType, String contextDescription) throws SchemaException {
        ResourceAttributeDefinitionType foundAttrDefType = null;
        for (ResourceAttributeDefinitionType attrDefType : rOcDefType.getAttribute()) {
            if (attrDefType.getRef() != null) {
                QName ref = ItemPathTypeUtil.asSingleNameOrFail(attrDefType.getRef());
                if (QNameUtil.match(ref, attrName)) {
                    if (foundAttrDefType == null) {
                        foundAttrDefType = attrDefType;
                    } else {
                        throw new SchemaException("Duplicate definition of attribute " + ref + " in "+kind+" type "
                                + rOcDefType.getIntent() + ", in " + contextDescription);
                    }
                }
            } else {
                throw new SchemaException("Missing reference to the attribute schema definition in definition " + SchemaDebugUtil.prettyPrint(attrDefType) + " during processing of " + contextDescription);
            }
        }
        return foundAttrDefType;
    }

    private void add(RefinedAttributeDefinition<?> refinedAttributeDefinition) {
        attributeDefinitions.add(refinedAttributeDefinition);
    }
    //endregion

    //region Diagnostic output, hashCode/equals =========================================================

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, null, this);
    }

    public static String debugDump(int indent, LayerType layer, RefinedObjectClassDefinition _this) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(_this.getDebugDumpClassName()).append(_this.getMutabilityFlag()).append("(");
        sb.append(SchemaDebugUtil.prettyPrint(_this.getTypeName()));
        if (_this.isDefault()) {
            sb.append(",default");
        }
        if (_this.getKind() != null) {
            sb.append(",kind=").append(_this.getKind().value());
        }
        if (_this.getIntent() != null) {
            sb.append(",intent=").append(_this.getIntent());
        }
        if (layer != null) {
            sb.append(",layer=").append(layer);
        }
        sb.append(")");
        for (RefinedAttributeDefinition rAttrDef: _this.getAttributeDefinitions()) {
            sb.append("\n");
            sb.append(rAttrDef.debugDump(indent + 1, layer));
        }
        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    public String getDebugDumpClassName() {
        return "rOCD";
    }

    @Override
    public String getHumanReadableName() {
        if (getDisplayName() != null) {
            return getDisplayName();
        } else if (getKind() != null) {
            return getKind()+":"+getIntent();
        } else {
            return getTypeName().getLocalPart();
        }
    }

    @Override
    public String toString() {
        if (getKind() == null) {
            return getDebugDumpClassName() + getMutabilityFlag() + "("+PrettyPrinter.prettyPrint(getTypeName())+")";
        } else {
            return getDebugDumpClassName() + getMutabilityFlag() + "("+getKind()+":"+getIntent()+"="+PrettyPrinter.prettyPrint(getTypeName())+")";
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + associationDefinitions.hashCode();
        result = prime * result + attributeDefinitions.hashCode();
        result = prime * result + auxiliaryObjectClassDefinitions.hashCode();
        result = prime * result + ((baseContext == null) ? 0 : baseContext.hashCode());
        result = prime * result + ((searchHierarchyScope == null) ? 0 : searchHierarchyScope.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result
                + ((displayNameAttributeDefinition == null) ? 0 : displayNameAttributeDefinition.hashCode());
        result = prime * result + ((primaryIdentifiers == null) ? 0 : primaryIdentifiers.hashCode());
        result = prime * result + ((intent == null) ? 0 : intent.hashCode());
        result = prime * result + (isDefault ? 1231 : 1237);
        result = prime * result + ((kind == null) ? 0 : kind.hashCode());
        result = prime * result + originalObjectClassDefinition.hashCode();
        result = prime * result + ((objectDefinition == null) ? 0 : objectDefinition.hashCode());
        result = prime * result + ((protectedObjectPatterns == null) ? 0 : protectedObjectPatterns.hashCode());
        result = prime * result + resourceOid.hashCode();
        result = prime * result + ((schemaHandlingObjectTypeDefinitionType == null) ? 0
                : schemaHandlingObjectTypeDefinitionType.hashCode());
        result = prime * result + ((secondaryIdentifiers == null) ? 0 : secondaryIdentifiers.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RefinedObjectClassDefinitionImpl other = (RefinedObjectClassDefinitionImpl) obj;
        if (!associationDefinitions.equals(other.associationDefinitions)) {
            return false;
        }
        if (!attributeDefinitions.equals(other.attributeDefinitions)) {
            return false;
        }
        if (!auxiliaryObjectClassDefinitions.equals(other.auxiliaryObjectClassDefinitions)) {
            return false;
        }
        if (baseContext == null) {
            if (other.baseContext != null) {
                return false;
            }
        } else if (!baseContext.equals(other.baseContext)) {
            return false;
        }
        if (searchHierarchyScope == null) {
            if (other.searchHierarchyScope != null) {
                return false;
            }
        } else if (!searchHierarchyScope.equals(other.searchHierarchyScope)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (displayName == null) {
            if (other.displayName != null) {
                return false;
            }
        } else if (!displayName.equals(other.displayName)) {
            return false;
        }
        if (displayNameAttributeDefinition == null) {
            if (other.displayNameAttributeDefinition != null) {
                return false;
            }
        } else if (!displayNameAttributeDefinition.equals(other.displayNameAttributeDefinition)) {
            return false;
        }
        if (primaryIdentifiers == null) {
            if (other.primaryIdentifiers != null) {
                return false;
            }
        } else if (!primaryIdentifiers.equals(other.primaryIdentifiers)) {
            return false;
        }
        if (intent == null) {
            if (other.intent != null) {
                return false;
            }
        } else if (!intent.equals(other.intent)) {
            return false;
        }
        if (isDefault != other.isDefault) {
            return false;
        }
        if (kind != other.kind) {
            return false;
        }
        if (!originalObjectClassDefinition.equals(other.originalObjectClassDefinition)) {
            return false;
        }
        if (objectDefinition == null) {
            if (other.objectDefinition != null) {
                return false;
            }
        } else if (!objectDefinition.equals(other.objectDefinition)) {
            return false;
        }
        if (protectedObjectPatterns == null) {
            if (other.protectedObjectPatterns != null) {
                return false;
            }
        } else if (!protectedObjectPatterns.equals(other.protectedObjectPatterns)) {
            return false;
        }
        if (!resourceOid.equals(other.resourceOid)) {
            return false;
        }
        if (schemaHandlingObjectTypeDefinitionType == null) {
            if (other.schemaHandlingObjectTypeDefinitionType != null) {
                return false;
            }
        } else if (!schemaHandlingObjectTypeDefinitionType.equals(other.schemaHandlingObjectTypeDefinitionType)) {
            return false;
        }
        if (secondaryIdentifiers == null) {
            if (other.secondaryIdentifiers != null) {
                return false;
            }
        } else if (!secondaryIdentifiers.equals(other.secondaryIdentifiers)) {
            return false;
        }
        return true;
    }
    //endregion

    //region Typing overhead ==============================================================
    /*
     * There is a natural correspondence between "type definition" classes and items in these classes:
     *
     *    ComplexTypeDefinition .............................. ItemDefinition
     *    ObjectClassComplexTypeDefinition ................... ResourceAttributeDefinition
     *    RefinedObjectClassDefinition ....................... RefinedAttributeDefinition
     *    LayerRefinedObjectClassDefinition .................. LayerRefinedAttributeDefinition
     *
     * It would be great if the interface of "type definition" classes, i.e. methods like getDefinitions(),
     * findItemDefinition, findAttributeDefinition, and so on would be parametrized on the type of item definitions
     * from the list above. Unfortunately, this would make clients very unintuitive, using interfaces like
     *
     *               RefinedObjectClassDefinition<RefinedAttributeDefinition<?>>
     *
     * Therefore the decision is to keep clients' lives simple; at the cost of "typing overhead" - providing correct
     * signatures of derived types. In order to keep it manageable we put all such methods in this single section.
     */

    @Override
    public <X> RefinedAttributeDefinition<X> findAttributeDefinition(@NotNull QName name) {
        return findLocalItemDefinition(ItemName.fromQName(name), RefinedAttributeDefinition.class, false);
    }

    //endregion

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        originalObjectClassDefinition.trimTo(paths);
        List<QName> names = paths.stream()
                .filter(p -> p.isSingleName())
                .map(p -> p.asSingleName())
                .collect(Collectors.toList());
        attributeDefinitions.removeIf(itemDefinition -> !QNameUtil.contains(names, itemDefinition.getItemName()));
        associationDefinitions.removeIf(itemDefinition -> !QNameUtil.contains(names, itemDefinition.getName()));
    }

    @Override
    public MutableObjectClassComplexTypeDefinition toMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isShared() {
        return shared;
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return originalObjectClassDefinition.getAnnotation(qname);
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Integer getInstantiationOrder() {
        return null;
    }

    @Override
    public boolean isImmutable() {
        return immutable;
    }

    @Override
    public void freeze() {
        attributeDefinitions.forEach(Freezable::freeze);
        associationDefinitions.forEach(Freezable::freeze);
        originalObjectClassDefinition.freeze();                             // TODO really?
        auxiliaryObjectClassDefinitions.forEach(Freezable::freeze);
        this.immutable = true;
    }
}
