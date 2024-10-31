/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemMerger;

import com.evolveum.midpoint.prism.key.NaturalKeyDefinition;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.internals.InternalsConfig;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

/**
 * Common implementation for both {@link ResourceObjectClassDefinition} and {@link ResourceObjectTypeDefinition}.
 */
public abstract class AbstractResourceObjectDefinitionImpl
        extends AbstractFreezable
        implements ResourceObjectDefinition {

    /**
     * Default value for {@link #currentLayer}.
     */
    static final LayerType DEFAULT_LAYER = LayerType.MODEL;

    /**
     * Default settings obtained from the system configuration.
     * Guarded by the getter and setter.
     *
     * Temporary solution, to be reviewed (MID-10126).
     */
    private static ShadowCachingPolicyType systemDefaultPolicy;

    /**
     * At what layer do we have the attribute definitions.
     */
    @NotNull final LayerType currentLayer;

    /** See {@link ResourceObjectDefinition#getBasicResourceInformation()}. */
    @NotNull final BasicResourceInformation basicResourceInformation;

    /**
     * Effective shadow caching policy determined from resource and object type/class level.
     * If present, all defaults are resolved.
     *
     * Nullable only for unattached raw object class definitions.
     */
    @Nullable private final ShadowCachingPolicyType effectiveShadowCachingPolicy;

    /**
     * Definition of attributes.
     *
     * It seems that elements are strictly of {@link ShadowAttributeDefinitionImpl} class and its subclasses,
     * but this is currently not enforceable in the compile time -
     * see e.g. {@link #copyDefinitionDataFrom(LayerType, ResourceObjectDefinition)}
     * or {@link #addInternal(ItemDefinition)}. TODO reconsider if it's ok this way.
     *
     * Frozen after creation.
     */
    @NotNull final DeeplyFreezableList<ShadowAttributeDefinition<?, ?, ?, ?>> attributeDefinitions =
            new DeeplyFreezableList<>();

    /**
     * Indexed attribute definitions for faster access.
     * Created (as immutable map) on freezing.
     *
     * Temporary/experimental.
     */
    private Map<QName, ShadowAttributeDefinition<?, ?, ?, ?>> attributeDefinitionMap;

    /**
     * Definition of associations.
     *
     * It seems that elements are strictly of {@link ShadowAssociationDefinitionImpl} class and its subclasses,
     * but this is currently not enforceable in the compile time -
     * see e.g. {@link #copyDefinitionDataFrom(LayerType, ResourceObjectDefinition)}
     * or {@link #addInternal(ItemDefinition)}. TODO reconsider if it's ok this way.
     *
     * Frozen after creation.
     */
    @NotNull final DeeplyFreezableList<ShadowAssociationDefinition> associationDefinitions =
            new DeeplyFreezableList<>();

    /**
     * Indexed association definitions for faster access.
     * Created (as immutable map) on freezing.
     *
     * Temporary/experimental.
     */
    private Map<QName, ShadowAssociationDefinition> associationDefinitionMap;

    /**
     * Names of primary identifiers. They are the same for both raw and refined definitions.
     * (Currently we do not support tweaking of this information.)
     *
     * Frozen after creation.
     */
    @NotNull final FreezableList<QName> primaryIdentifiersNames = new FreezableList<>();

    /**
     * Names of secondary identifiers. The refinement using `schemaHandling` may add or remove some identifiers
     * from the raw list.
     *
     * Frozen after creation.
     *
     * @see ResourceAttributeDefinitionType#isSecondaryIdentifier()
     */
    @NotNull final FreezableList<QName> secondaryIdentifiersNames = new FreezableList<>();

    /**
     * Object definition for compliant prism objects.
     * The "any" parts are replaced with appropriate schema (e.g. resource schema).
     *
     * Immutable.
     */
    private PrismObjectDefinition<ShadowType> prismObjectDefinition;

    /**
     * Definition of auxiliary object classes. They originate from
     * {@link ResourceObjectTypeDefinitionType#getAuxiliaryObjectClass()} and are resolved during parsing.
     *
     * However, they are _not_ used by default for attribute resolution!
     * A {@link CompositeObjectDefinition} must be created in order to "activate" them.
     */
    @NotNull final DeeplyFreezableList<ResourceObjectDefinition> auxiliaryObjectClassDefinitions =
            new DeeplyFreezableList<>();

    /**
     * The "source" bean for this definition.
     *
     * It contains all refinements relevant for this object definition:
     *
     * - For raw object class definitions, it is always empty. (Still, it's non-null to avoid writing special null-checks
     * in getter methods like {@link #getDisplayName()}.)
     * - For refined object class definitions it may be empty (if there are no refinements), or not empty (if there are
     * refinements).
     * - For object type definitions, it is relevant `objectType` value in `schemaHandling`, expanded by resolving
     * object type inheritance. Note that object type definition that refers to refined object class definition is also
     * a case of inheritance, so such type definition bean contains all data from the refined object class definition bean.
     *
     * Immutable.
     */
    @NotNull final ResourceObjectTypeDefinitionType definitionBean;

    /**
     * Compiled instructions for marking shadows (e.g., as protected objects).
     *
     * Frozen after parsing.
     *
     * @see ResourceObjectTypeDefinitionType#getProtected()
     * @see ResourceObjectTypeDefinitionType#getMarking()
     * @see ResourceObjectPatternType
     */
    @NotNull private final FreezableReference<ShadowMarkingRules> shadowMarkingRules = new FreezableReference<>();

    /**
     * "Compiled" object set delineation.
     */
    @NotNull protected final DeeplyFreezableReference<ResourceObjectTypeDelineation> delineation = new DeeplyFreezableReference<>();

    /**
     * Name of "display name" attribute. May override the value obtained from the resource.
     */
    QName displayNameAttributeName;

    AbstractResourceObjectDefinitionImpl(
            @NotNull LayerType currentLayer,
            @NotNull BasicResourceInformation basicResourceInformation,
            @NotNull ResourceObjectTypeDefinitionType definitionBean)
            throws SchemaException, ConfigurationException {
        this.currentLayer = currentLayer;
        this.basicResourceInformation = basicResourceInformation;
        definitionBean.freeze();
        this.definitionBean = definitionBean;
        this.effectiveShadowCachingPolicy = computeEffectiveShadowCachingPolicy();
    }

    @Override
    public @NotNull BasicResourceInformation getBasicResourceInformation() {
        return basicResourceInformation;
    }

    @Override
    public @NotNull List<? extends ShadowAttributeDefinition<?, ?, ?, ?>> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    @Override
    public @NotNull List<ShadowAssociationDefinition> getAssociationDefinitions() {
        return associationDefinitions;
    }

    @Override
    public <T> @Nullable ShadowSimpleAttributeDefinition<T> findSimpleAttributeDefinition(QName name, boolean caseInsensitive) {
        if (caseInsensitive || isMutable() || QNameUtil.isUnqualified(name)) {
            return ResourceObjectDefinition.super.findSimpleAttributeDefinition(name, caseInsensitive);
        }
        var def = attributeDefinitionMap.get(name);
        //noinspection unchecked
        return def instanceof ShadowSimpleAttributeDefinition<?> ? (ShadowSimpleAttributeDefinition<T>) def : null;
    }

    @Override
    public ShadowAssociationDefinition findAssociationDefinition(QName name) {
        if (isMutable() || QNameUtil.isUnqualified(name)) {
            for (var associationDefinition : associationDefinitions) {
                if (QNameUtil.match(associationDefinition.getItemName(), name)) {
                    return associationDefinition;
                }
            }
            return null;
        } else {
            return associationDefinitionMap.get(name);
        }
    }

    @NotNull
    @Override
    public Collection<ShadowSimpleAttributeDefinition<?>> getPrimaryIdentifiers() {
        return primaryIdentifiersNames.stream()
                .map(this::findSimpleAttributeDefinitionStrictlyRequired)
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Override
    public Collection<ShadowSimpleAttributeDefinition<?>> getSecondaryIdentifiers() {
        return secondaryIdentifiersNames.stream()
                .map(this::findSimpleAttributeDefinitionStrictlyRequired)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Names of primary identifiers. The list is modifiable until the whole definition is frozen.
     */
    public @NotNull List<QName> getPrimaryIdentifiersNames() {
        return primaryIdentifiersNames;
    }

    /**
     * Names of secondary identifiers. The list is modifiable until the whole definition is frozen.
     */
    public @NotNull List<QName> getSecondaryIdentifiersNames() {
        return secondaryIdentifiersNames;
    }

    @Override
    public @Nullable String getDisplayName() {
        return definitionBean.getDisplayName();
    }

    @Override
    public String getDescription() {
        return definitionBean.getDescription();
    }

    @Override
    public String getDocumentation() {
        return definitionBean.getDocumentation();
    }

    /** Sets the delineation and freezes the holder. */
    void setDelineation(@NotNull ResourceObjectTypeDelineation value) {
        delineation.setAndFreeze(value);
    }

    @Override
    public @NotNull ResourceObjectTypeDelineation getDelineation() {
        return Objects.requireNonNull(
                delineation.getValue(),
                () -> "no delineation in " + this);
    }

    @Override
    public ResourceObjectReferenceType getBaseContext() {
        return getDelineation()
                .getBaseContext();
    }

    @Override
    public SearchHierarchyScope getSearchHierarchyScope() {
        return getDelineation()
                .getSearchHierarchyScope();
    }

    @Override
    public @NotNull ResourceObjectVolatilityType getVolatility() {
        return Objects.requireNonNullElse(
                definitionBean.getVolatility(),
                ResourceObjectVolatilityType.NONE);
    }

    @Override
    public @Nullable DefaultInboundMappingEvaluationPhasesType getDefaultInboundMappingEvaluationPhases() {
        // In the future we may define the value also on resource or even global system level
        ResourceMappingsEvaluationConfigurationType definition = definitionBean.getMappingsEvaluation();
        if (definition == null) {
            return null;
        }
        if (definition.getInbound() == null) {
            return null;
        }
        return definition.getInbound().getDefaultEvaluationPhases();
    }

    @Override
    public @Nullable String getLifecycleState() {
        return definitionBean.getLifecycleState();
    }

    @Override
    public ResourceObjectMultiplicityType getObjectMultiplicity() {
        return definitionBean.getMultiplicity();
    }

    @Override
    public ProjectionPolicyType getProjectionPolicy() {
        return definitionBean.getProjection();
    }

    @Override
    public PrismObjectDefinition<ShadowType> getPrismObjectDefinition() {
        if (prismObjectDefinition == null) {
            PrismObjectDefinition<ShadowType> definition = toPrismObjectDefinition();
            definition.freeze();
            // We could also consider not caching the definition if this object is still mutable. That would be perhaps safer.
            // Currently, it is solved by invalidation this definition when attributes/associations are added. See also MID-9535.
            this.prismObjectDefinition = definition;
        }
        return prismObjectDefinition;
    }

    private void invalidatePrismObjectDefinition() {
        prismObjectDefinition = null;
    }

    //region Accessing parts of schema handling ========================================================
    @NotNull
    @Override
    public Collection<ResourceObjectDefinition> getAuxiliaryDefinitions() {
        return auxiliaryObjectClassDefinitions;
    }

    @Override
    public boolean hasAuxiliaryObjectClass(QName expectedObjectClassName) {
        return auxiliaryObjectClassDefinitions.stream()
                .anyMatch(def -> QNameUtil.match(def.getTypeName(), expectedObjectClassName));
    }

    @Override
    public ResourceBidirectionalMappingAndDefinitionType getAuxiliaryObjectClassMappings() {
        return definitionBean.getAuxiliaryObjectClassMappings();
    }

    @Override
    public @NotNull ShadowMarkingRules getShadowMarkingRules() {
        return MiscUtil.stateNonNull(
                shadowMarkingRules.getValue(),
                "Unparsed definition? %s", this);
    }

    void setShadowMarkingRules(ShadowMarkingRules rules) {
        shadowMarkingRules.setValue(rules);
    }

    @Override
    public ResourcePasswordDefinitionType getPasswordDefinition() {
        ResourceCredentialsDefinitionType credentials = definitionBean.getCredentials();
        if (credentials == null) {
            return null;
        }
        return credentials.getPassword();
    }

    @Override
    public ObjectReferenceType getSecurityPolicyRef() {
        return definitionBean.getSecurityPolicyRef();
    }

    @Override
    public ResourceActivationDefinitionType getActivationSchemaHandling() {
        return definitionBean.getActivation();
    }

    //endregion

    //region Capabilities ========================================================
    @Override
    public <T extends CapabilityType> T getEnabledCapability(@NotNull Class<T> capabilityClass, ResourceType resource) {
        return ResourceTypeUtil.getEnabledCapability(resource, definitionBean, capabilityClass);
    }
    //endregion

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractResourceObjectDefinitionImpl that = (AbstractResourceObjectDefinitionImpl) o;
        return attributeDefinitions.equals(that.attributeDefinitions)
                && associationDefinitions.equals(that.associationDefinitions)
                && primaryIdentifiersNames.equals(that.primaryIdentifiersNames)
                && secondaryIdentifiersNames.equals(that.secondaryIdentifiersNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeDefinitions, associationDefinitions, primaryIdentifiersNames, secondaryIdentifiersNames);
    }

    public abstract @NotNull AbstractResourceObjectDefinitionImpl clone();

    @SuppressWarnings("WeakerAccess") // open for subclassing
    protected void copyDefinitionDataFrom(
            @NotNull LayerType layer,
            @NotNull ResourceObjectDefinition source) {
        source.getAttributeDefinitions().forEach(
                def -> attributeDefinitions.add(def.forLayer(layer)));
        associationDefinitions.addAll(source.getAssociationDefinitions());
        primaryIdentifiersNames.addAll(source.getPrimaryIdentifiersNames());
        secondaryIdentifiersNames.addAll(source.getSecondaryIdentifiersNames());
        auxiliaryObjectClassDefinitions.addAll(source.getAuxiliaryDefinitions());
        shadowMarkingRules.setValue(source.getShadowMarkingRules());
        displayNameAttributeName = source.getDisplayNameAttributeName();
        // prism object definition need not be copied
    }

    @Override
    public void revive(PrismContext prismContext) {
    }

    @Override
    protected void performFreeze() {
        attributeDefinitions.freeze();
        createAttributeDefinitionMap();
        associationDefinitions.freeze();
        createAssociationDefinitionMap();

//        stateCheck(itemDefinitions.isEmpty(), "Any item definitions in %s", this);
//        //noinspection unchecked,rawtypes
//        itemDefinitions.addAll((Collection) getMergedDefinitions());
//        itemDefinitions.freeze();

        primaryIdentifiersNames.freeze();
        secondaryIdentifiersNames.freeze();
        auxiliaryObjectClassDefinitions.freeze();
        shadowMarkingRules.freeze();
    }

    private void createAttributeDefinitionMap() {
        var map = new HashMap<QName, ShadowAttributeDefinition<?, ?, ?, ?>>();
        attributeDefinitions.forEach(def -> {
            var previous = map.put(def.getItemName(), def);
            stateCheck(previous == null, "Multiple definitions for attribute %s in %s", def, this);
        });
        attributeDefinitionMap = Collections.unmodifiableMap(map);
    }

    private void createAssociationDefinitionMap() {
        var map = new HashMap<QName, ShadowAssociationDefinition>();
        associationDefinitions.forEach(def -> {
            var previous = map.put(def.getItemName(), def);
            stateCheck(previous == null, "Multiple definitions for association %s in %s", def, this);
        });
        associationDefinitionMap = Collections.unmodifiableMap(map);
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        visitor.visit(this);
        attributeDefinitions.forEach(def -> def.accept(visitor));
        auxiliaryObjectClassDefinitions.forEach(def -> def.accept(visitor));
        associationDefinitions.forEach(def -> def.accept(visitor));
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        if (visitation.alreadyVisited(this)) {
            return false;
        } else {
            visitor.visit(this);
            attributeDefinitions.forEach(def -> def.accept(visitor, visitation));
            auxiliaryObjectClassDefinitions.forEach(def -> def.accept(visitor, visitation));
            associationDefinitions.forEach(def -> def.accept(visitor));
            return true;
        }
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, null, this);
    }

    public static String debugDump(int indent, LayerType layer, AbstractResourceObjectDefinitionImpl _this) {
        StringBuilder sb = new StringBuilder();
        sb.append(INDENT_STRING.repeat(Math.max(0, indent)));
        sb.append(_this.getDebugDumpClassName()).append(_this.getMutabilityFlag()).append("(");
        sb.append(SchemaDebugUtil.prettyPrint(_this.getTypeName()));
        _this.addDebugDumpHeaderExtension(sb);
        if (layer != null) {
            sb.append(", layer=").append(layer);
        }
        List<? extends ShadowAttributeDefinition<?, ?, ?, ?>> attributeDefinitions = _this.getAttributeDefinitions();
        sb.append(") with ").append(attributeDefinitions.size()).append(" attribute definitions");
        for (var attrDef : attributeDefinitions) {
            sb.append("\n");
            sb.append(attrDef.debugDump(indent + 1, layer));
        }
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Expanded definition bean", _this.getDefinitionBean(), indent + 1);
        _this.addDebugDumpTrailer(sb, indent);
        return sb.toString();
    }

    protected void addDebugDumpHeaderExtension(StringBuilder sb) {
    }

    protected void addDebugDumpTrailer(StringBuilder sb, int indent) {
    }

    @Override
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {
        //noinspection unchecked
        return (List<? extends ItemDefinition<?>>) getAttributeDefinitions();
    }

//    @Override
//    public @NotNull Collection<? extends ShadowAttributeDefinition<?, ?>> getShadowItemDefinitions() {
//        //noinspection unchecked
//        return (Collection<? extends ShadowAttributeDefinition<?, ?>>) getDefinitions();
//    }

    @Override
    public @NotNull QName getTypeName() {
        return getObjectClassName();
    }

    @Override
    public boolean isRuntimeSchema() {
        return true;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    @Override
    public boolean isOptionalCleanup() {
        return false;
    }

    @Override
    public String getRemovedSince() {
        return null;
    }

    @Override
    public boolean isExperimental() {
        return false;
    }

    @Override
    public String getPlannedRemoval() {
        return null;
    }

    @Override
    public boolean isElaborate() {
        return false;
    }

    @Override
    public String getDeprecatedSince() {
        return null;
    }

    @Override
    public DisplayHint getDisplayHint() {
        return null;
    }

    @Override
    public @Nullable String getMergerIdentifier() {
        return null;
    }

    @Override
    public @Nullable List<QName> getNaturalKeyConstituents() {
        return null;
    }

    @Override
    public @Nullable ItemMerger getMergerInstance(@NotNull MergeStrategy strategy, @Nullable OriginMarker originMarker) {
        return null;
    }

    @Override
    public @Nullable NaturalKeyDefinition getNaturalKeyInstance() {
        return null;
    }

    @Override
    public boolean isEmphasized() {
        return false;
    }

    @Override
    public Integer getDisplayOrder() {
        return null;
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public String getDocumentationPreview() {
        return null;
    }

    @Override
    public Class<?> getTypeClass() {
        return null; // No static definition here
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return null;
    }

    @Override
    public @Nullable Map<QName, Object> getAnnotations() {
        return null;
    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        return null;
    }

    @Override
    public List<ItemDiagramSpecification> getDiagrams() {
        return null;
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        if (path.size() != 1) {
            return null;
        }
        ItemName first = path.firstToNameOrNull();
        if (first == null) {
            return null;
        }
        return findLocalItemDefinition(first.asSingleName(), clazz, false);
    }

    public @NotNull ResourceObjectDefinition forLayerMutable(@NotNull LayerType layer) {
        if (currentLayer == layer && isMutable()) {
            return this;
        } else {
            return cloneInLayer(layer);
        }
    }

    public @NotNull ResourceObjectDefinition forLayerImmutable(@NotNull LayerType layer) {
        if (currentLayer == layer && !isMutable()) {
            return this;
        } else {
            var clone = cloneInLayer(layer);
            clone.freeze();
            return clone;
        }
    }

    /** Returns mutable copy. */
    @NotNull protected abstract AbstractResourceObjectDefinitionImpl cloneInLayer(@NotNull LayerType layer);

    @Override
    public @NotNull LayerType getCurrentLayer() {
        return currentLayer;
    }

    @Override
    public void replaceAttributeDefinition(@NotNull QName itemName, @Nullable ItemDefinition<?> newDefinition) {
        checkMutable();
        invalidatePrismObjectDefinition();
        attributeDefinitions.removeIf(def -> def.getItemName().equals(itemName));
        if (newDefinition != null) {
            add(newDefinition);
        }
    }

    public void add(ItemDefinition<?> definition) {
        addInternal(definition);
    }

    private void addInternal(@NotNull ItemDefinition<?> definition) {
        if (definition instanceof ShadowAttributeDefinition<?, ?, ?, ?> attributeDefinition) {
            // Can occur during definition replacement.
            attributeDefinitions.add(attributeDefinition);
        } else if (definition instanceof ShadowAssociationDefinition associationDefinition) {
            associationDefinitions.add(associationDefinition);
        } else {
            throw new IllegalArgumentException(
                    ("Only attribute or association definitions should be put into a resource object definition. "
                            + "Item definition = %s (%s), object definition = %s").formatted(
                                    definition, definition.getClass(), this));
        }
        invalidatePrismObjectDefinition();
    }

    @Override
    public @NotNull Collection<QName> getConfiguredAuxiliaryObjectClassNames() {
        // TODO keep the names, not the resolved definitions
        return getAuxiliaryDefinitions().stream()
                .map(Definition::getTypeName)
                .collect(Collectors.toSet());
    }

    @Override
    public void trimAttributesTo(@NotNull Collection<ItemPath> paths) {
        if (isImmutable()) {
            return; // This would fail anyway
        }
        List<QName> names = paths.stream()
                .filter(ItemPath::isSingleName)
                .map(ItemPath::asSingleName)
                .collect(Collectors.toList());
        attributeDefinitions.removeIf(itemDefinition -> !QNameUtil.contains(names, itemDefinition.getItemName()));

        // TODO what about QName references like primary or secondary identifier names,
        //  or name, display name, or description attribute names?
    }

    @Override
    public void validate() throws SchemaException {
        Set<QName> attributeNames = new HashSet<>();
        for (ShadowSimpleAttributeDefinition<?> attributeDefinition : getSimpleAttributeDefinitions()) {
            QName attrName = attributeDefinition.getItemName();
            if (!attributeNames.add(attrName)) {
                throw new SchemaException("Duplicate definition of attribute " + attrName + " in " + this);
            }
        }

        Collection<? extends ShadowSimpleAttributeDefinition<?>> primaryIdentifiers = getPrimaryIdentifiers();
        Collection<? extends ShadowSimpleAttributeDefinition<?>> secondaryIdentifiers = getSecondaryIdentifiers();

        if (primaryIdentifiers.isEmpty() && secondaryIdentifiers.isEmpty()) {
            throw new SchemaException("No identifiers in definition of object class " + getTypeName() + " in " + this);
        }
    }

    public @NotNull ResourceObjectTypeDefinitionType getDefinitionBean() {
        return definitionBean;
    }

//    void addReferenceAttributeDefinition(@NotNull ShadowReferenceAttributeDefinition definition) {
//        checkMutable();
//        associationDefinitions.add(definition);
//        invalidatePrismObjectDefinition();
//    }

    void addAuxiliaryObjectClassDefinition(@NotNull ResourceObjectDefinition definition) {
        checkMutable();
        auxiliaryObjectClassDefinitions.add(definition);
    }

    void setDisplayNameAttributeName(QName name) {
        checkMutable();
        this.displayNameAttributeName = name;
    }

    @Override
    public @NotNull ShadowCachingPolicyType getEffectiveShadowCachingPolicy() {
        return MiscUtil.argNonNull(
                effectiveShadowCachingPolicy,
                "Effective shadow caching policy is not available for unattached definitions: %s", this);
    }

    private @NotNull ShadowCachingPolicyType computeEffectiveShadowCachingPolicy()
            throws SchemaException, ConfigurationException {

        var cachingDefault = InternalsConfig.getShadowCachingDefault();

        ShadowCachingPolicyType parentPolicy; // everything above the object class/type level
        if (cachingDefault == InternalsConfig.ShadowCachingDefault.FROM_SYSTEM_CONFIGURATION) {
            parentPolicy = BaseMergeOperation.merge(basicResourceInformation.cachingPolicy(), getSystemDefaultPolicy());
        } else {
            parentPolicy = basicResourceInformation.cachingPolicy();
        }

        var merged = BaseMergeOperation.merge(definitionBean.getCaching(), parentPolicy);
        var workingCopy = merged != null ? merged.clone() : new ShadowCachingPolicyType();

        boolean readCachedCapabilityPresent = isReadCachedCapabilityPresent();

        var defaultForSimpleAttributesScope = ShadowSimpleAttributesCachingScopeType.DEFINED;
        var defaultForCredentialsScope = ShadowItemsCachingScopeType.ALL;
        var defaultForCacheUse = CachedShadowsUseType.USE_FRESH;
        var defaultForTtl = "P1D";
        if (workingCopy.getCachingStrategy() == null) {
            if (readCachedCapabilityPresent) {
                workingCopy.setCachingStrategy(CachingStrategyType.PASSIVE);
                defaultForSimpleAttributesScope = ShadowSimpleAttributesCachingScopeType.ALL;
                defaultForCredentialsScope = ShadowItemsCachingScopeType.NONE;
                defaultForTtl = "P1000Y";
            } else if (cachingDefault == InternalsConfig.ShadowCachingDefault.FULL) {
                // Currently used for testing
                workingCopy.setCachingStrategy(CachingStrategyType.PASSIVE);
                defaultForSimpleAttributesScope = ShadowSimpleAttributesCachingScopeType.ALL;
                defaultForCacheUse = CachedShadowsUseType.USE_CACHED_OR_FRESH;
                defaultForTtl = "P1000Y";
            } else if (cachingDefault == InternalsConfig.ShadowCachingDefault.FULL_BUT_USING_FRESH) {
                // Currently used for testing
                workingCopy.setCachingStrategy(CachingStrategyType.PASSIVE);
                defaultForSimpleAttributesScope = ShadowSimpleAttributesCachingScopeType.ALL;
                defaultForTtl = "P1000Y";
            } else {
                workingCopy.setCachingStrategy(CachingStrategyType.NONE);
            }
        }

        if (workingCopy.getScope() == null) {
            workingCopy.setScope(new ShadowCachingScopeType());
        }
        var scope = workingCopy.getScope();
        if (scope.getAttributes() == null) {
            scope.setAttributes(defaultForSimpleAttributesScope);
        }
        if (scope.getAssociations() == null) {
            scope.setAssociations(ShadowItemsCachingScopeType.ALL);
        }
        if (scope.getActivation() == null) {
            scope.setActivation(ShadowItemsCachingScopeType.ALL);
        }
        if (scope.getCredentials() == null) {
            scope.setCredentials(defaultForCredentialsScope);
        }
        if (scope.getAuxiliaryObjectClasses() == null) {
            scope.setAuxiliaryObjectClasses(ShadowItemsCachingScopeType.ALL);
        }
        if (workingCopy.getDefaultCacheUse() == null) {
            workingCopy.setDefaultCacheUse(defaultForCacheUse);
        }
        if (workingCopy.getTimeToLive() == null) {
            workingCopy.setTimeToLive(XmlTypeConverter.createDuration(defaultForTtl));
        }
        return workingCopy;
    }

    private boolean isReadCachedCapabilityPresent() {
        var typeDef = getTypeDefinition();
        if (typeDef != null) {
            var readCapabilityOfObjectType = typeDef.getConfiguredCapability(ReadCapabilityType.class);
            if (readCapabilityOfObjectType != null) {
                return CapabilityUtil.isCapabilityEnabled(readCapabilityOfObjectType)
                        && Boolean.TRUE.equals(readCapabilityOfObjectType.isCachingOnly());
            }
        }
        return basicResourceInformation.readCachedCapabilityPresent();
    }

    @Override
    public @Nullable ItemName resolveFrameworkName(@NotNull String frameworkName) {
        return FrameworkNameResolver.findInObjectDefinition(this, frameworkName);
    }

    @Override
    public ItemInboundDefinition getSimpleAttributeInboundDefinition(ItemName itemName) {
        return findSimpleAttributeDefinition(itemName);
    }

    @Override
    public ItemInboundDefinition getReferenceAttributeInboundDefinition(ItemName itemName) {
        return findReferenceAttributeDefinition(itemName);
    }

    @Override
    public CorrelationDefinitionType getCorrelation() {
        return getDefinitionBean().getCorrelation();
    }

    @Override
    public DefinitionMutator mutator() {
        throw new UnsupportedOperationException();
    }

    public static synchronized ShadowCachingPolicyType getSystemDefaultPolicy() {
        return systemDefaultPolicy;
    }

    public static synchronized void setSystemDefaultPolicy(ShadowCachingPolicyType value) {
        systemDefaultPolicy = value != null ? value.clone() : null;
    }
}
