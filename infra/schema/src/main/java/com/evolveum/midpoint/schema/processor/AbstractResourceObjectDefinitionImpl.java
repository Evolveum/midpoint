/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.impl.ComplexTypeDefinitionImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.util.QNameUtil;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Common implementation for both {@link ResourceObjectClassDefinition} and {@link ResourceObjectTypeDefinition}.
 *
 * Note about not inheriting from {@link ComplexTypeDefinitionImpl}:
 *
 * As we do not inherit from that class, we have to provide our own implementation of various methods like
 * {@link #getExtensionForType()}, {@link #isContainerMarker()}, and so on. This is basically
 * no problem, as this information is not available from a resource connector, so we are OK with the default values.
 * Should this change, we would need to reconsider this design. The current implementation is more straightforward,
 * less entangled with a hierarchy of ancestor implementations.
 */
public abstract class AbstractResourceObjectDefinitionImpl
        extends AbstractFreezable
        implements ResourceObjectDefinition {

    /**
     * Default value for {@link #currentLayer}.
     */
    static final LayerType DEFAULT_LAYER = LayerType.MODEL;

    /**
     * At what layer do we have the attribute definitions.
     */
    @NotNull final LayerType currentLayer;

    /**
     * Definition of attributes.
     *
     * It seems that elements are strictly of {@link ResourceAttributeDefinitionImpl} class, but this is currently
     * not enforceable in the compile time - see e.g. {@link #copyDefinitionDataFrom(LayerType, ResourceObjectDefinition)}
     * or {@link #addInternal(ItemDefinition)}. TODO reconsider if it's ok this way.
     *
     * Frozen after creation.
     */
    @NotNull final DeeplyFreezableList<ResourceAttributeDefinition<?>> attributeDefinitions =
            new DeeplyFreezableList<>();

    /**
     * Indexed attribute definitions for faster access.
     * Created (as immutable map) on freezing.
     *
     * Temporary/experimental.
     */
    private Map<QName, ResourceAttributeDefinition<?>> attributeDefinitionMap;

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
     * Definition of associations.
     *
     * They are not present in the "raw" object class definition, as they do not exist in this form on the resource.
     *
     * Immutable.
     */
    @NotNull final DeeplyFreezableList<ResourceAssociationDefinition> associationDefinitions =
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
     * a case of inheritance! Such definition bean contains all data from the refining object class definition bean.
     *
     * Immutable.
     */
    @NotNull final ResourceObjectTypeDefinitionType definitionBean;

    /**
     * Compiled patterns denoting protected objects.
     *
     * @see ResourceObjectTypeDefinitionType#getProtected()
     * @see ResourceObjectPatternType
     *
     * Frozen after parsing. (TODO)
     */
    @NotNull private final FreezableList<ResourceObjectPattern> protectedObjectPatterns = new FreezableList<>();

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
            @NotNull ResourceObjectTypeDefinitionType definitionBean) {
        this.currentLayer = currentLayer;
        definitionBean.freeze();
        this.definitionBean = definitionBean;
    }

    @NotNull
    @Override
    public List<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    @Override
    public @NotNull Collection<ResourceAssociationDefinition> getAssociationDefinitions() {
        return associationDefinitions;
    }

    @Override
    public @Nullable ResourceAttributeDefinition<?> findAttributeDefinition(QName name, boolean caseInsensitive) {
        if (caseInsensitive || isMutable()) {
            return ResourceObjectDefinition.super.findAttributeDefinition(name, caseInsensitive);
        } else {
            return attributeDefinitionMap.get(name);
        }
    }

    @NotNull
    @Override
    public Collection<ResourceAttributeDefinition<?>> getPrimaryIdentifiers() {
        return primaryIdentifiersNames.stream()
                .map(this::findAttributeDefinitionStrictlyRequired)
                .collect(Collectors.toUnmodifiableList());
    }

    @NotNull
    @Override
    public Collection<ResourceAttributeDefinition<?>> getSecondaryIdentifiers() {
        return secondaryIdentifiersNames.stream()
                .map(this::findAttributeDefinitionStrictlyRequired)
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
            PrismObjectDefinition<ShadowType> definition = computePrismObjectDefinition();
            definition.freeze();
            this.prismObjectDefinition = definition;
        }
        return prismObjectDefinition;
    }

    private void invalidatePrismObjectDefinition() {
        prismObjectDefinition = null;
    }

    @NotNull PrismObjectDefinition<ShadowType> computePrismObjectDefinition() {
        return ObjectFactory.constructObjectDefinition(
                toResourceAttributeContainerDefinition());
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
    public @NotNull Collection<ResourceObjectPattern> getProtectedObjectPatterns() {
        return protectedObjectPatterns;
    }

    void addProtectedObjectPattern(ResourceObjectPattern pattern) {
        protectedObjectPatterns.add(pattern);
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
    public PrismObject<ShadowType> createBlankShadow(String resourceOid, String tag) {
        ShadowType shadow =
                new ShadowType()
                        .tag(tag)
                        .objectClass(getObjectClassName())
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE);

        PrismObject<ShadowType> shadowPrismObject = shadow.asPrismObject();

        // Setup definition
        shadowPrismObject.setDefinition(
                shadowPrismObject.getDefinition()
                        .cloneWithReplacedDefinition(
                                ShadowType.F_ATTRIBUTES, toResourceAttributeContainerDefinition()));

        return shadowPrismObject;
    }

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
                && primaryIdentifiersNames.equals(that.primaryIdentifiersNames)
                && secondaryIdentifiersNames.equals(that.secondaryIdentifiersNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeDefinitions, primaryIdentifiersNames, secondaryIdentifiersNames);
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
        protectedObjectPatterns.addAll(source.getProtectedObjectPatterns());
        displayNameAttributeName = source.getDisplayNameAttributeName();
        // prism object definition need not be copied
    }

    @Override
    public PrismContext getPrismContext() {
        return PrismContext.get();
    }

    @Override
    public void revive(PrismContext prismContext) {
    }

    @Override
    public @Nullable Class<?> getCompileTimeClass() {
        return null;
    }

    @Override
    public @Nullable QName getSuperType() {
        return null;
    }

    @Override
    public @NotNull Collection<TypeDefinition> getStaticSubTypes() {
        return List.of();
    }

    @Override
    public Integer getInstantiationOrder() {
        return null;
    }

    @Override
    public boolean canRepresent(QName typeName) {
        return QNameUtil.match(typeName, getObjectClassName());
    }

    @Override
    protected void performFreeze() {
        attributeDefinitions.freeze();
        createAttributeDefinitionMap();
        associationDefinitions.freeze();
        primaryIdentifiersNames.freeze();
        secondaryIdentifiersNames.freeze();
        auxiliaryObjectClassDefinitions.freeze();
    }

    private void createAttributeDefinitionMap() {
        var map = new HashMap<QName, ResourceAttributeDefinition<?>>();
        attributeDefinitions.forEach(def -> {
            var previous = map.put(def.getItemName(), def);
            stateCheck(previous == null, "Multiple definitions for attribute %s in %s", def, this);
        });
        attributeDefinitionMap = Collections.unmodifiableMap(map);
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
            sb.append(",layer=").append(layer);
        }
        sb.append(")");
        for (ResourceAttributeDefinition<?> rAttrDef : _this.getAttributeDefinitions()) {
            sb.append("\n");
            sb.append(rAttrDef.debugDump(indent + 1, layer));
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
        return getAttributeDefinitions();
    }

    @Override
    public @Nullable QName getExtensionForType() {
        return null;
    }

    @Override
    public boolean isReferenceMarker() {
        return false;
    }

    @Override
    public boolean isContainerMarker() {
        return true;
    }

    @Override
    public boolean isObjectMarker() {
        return false;
    }

    @Override
    public boolean isXsdAnyMarker() {
        return true;
    }

    @Override
    public boolean isListMarker() {
        return false;
    }

    @Override
    public @Nullable String getDefaultNamespace() {
        return null;
    }

    @Override
    public @NotNull List<String> getIgnoredNamespaces() {
        return List.of();
    }

    @Override
    public void merge(ComplexTypeDefinition otherComplexTypeDef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return attributeDefinitions.isEmpty()
                && associationDefinitions.isEmpty()
                && auxiliaryObjectClassDefinitions.isEmpty();
    }

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
    public <A> void setAnnotation(QName qname, A value) {
        throw new UnsupportedOperationException();
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

    public ResourceObjectDefinition forLayer(@NotNull LayerType layer) {
        if (currentLayer == layer) {
            return this;
        } else {
            return cloneInLayer(layer);
        }
    }

    protected abstract AbstractResourceObjectDefinitionImpl cloneInLayer(@NotNull LayerType layer);

    @Override
    public @NotNull LayerType getCurrentLayer() {
        return currentLayer;
    }

    @Override
    public void replaceDefinition(@NotNull QName itemName, @Nullable ItemDefinition<?> newDefinition) {
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

    @NotNull ResourceAttributeDefinition<?> addInternal(@NotNull ItemDefinition<?> definition) {
        ResourceAttributeDefinition<?> definitionToAdd;
        if (definition instanceof ResourceAttributeDefinition<?>) {
            // Can occur during definition replacement.
            definitionToAdd = (ResourceAttributeDefinition<?>) definition;
        } else if (definition instanceof RawResourceAttributeDefinition<?>) {
            // This is the case during parsing. We get the really raw (and mutable) definition.
            // The following call will convert it into usable form, including freezing.
            definitionToAdd = ResourceAttributeDefinitionImpl.create((RawResourceAttributeDefinition<?>) definition);
        } else {
            throw new IllegalArgumentException(
                    "Only ResourceAttributeDefinitions should be put into a ResourceObjectClassDefinition. "
                            + "Item definition = " + definition + " (" + definition.getClass() + "), "
                            + "ResourceObjectClassDefinition = " + this);
        }

        attributeDefinitions.add(definitionToAdd);
        return definitionToAdd;
    }

    @Override
    public ItemProcessing getProcessing() {
        return null; // No information at the level of object class
    }

    @Override
    public Collection<QName> getConfiguredAuxiliaryObjectClassNames() {
        // TODO keep the names, not the resolved definitions
        return getAuxiliaryDefinitions().stream()
                .map(Definition::getTypeName)
                .collect(Collectors.toSet());
    }

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        if (isImmutable()) {
            return; // This would fail anyway
        }
        List<QName> names = paths.stream()
                .filter(ItemPath::isSingleName)
                .map(ItemPath::asSingleName)
                .collect(Collectors.toList());
        attributeDefinitions.removeIf(itemDefinition -> !QNameUtil.contains(names, itemDefinition.getItemName()));
        associationDefinitions.removeIf(itemDefinition -> !QNameUtil.contains(names, itemDefinition.getName()));

        // TODO what about QName references like primary or secondary identifier names,
        //  or name, display name, or description attribute names?
    }

    @Override
    public void validate() throws SchemaException {
        Set<QName> attributeNames = new HashSet<>();
        for (ResourceAttributeDefinition<?> attributeDefinition : getAttributeDefinitions()) {
            QName attrName = attributeDefinition.getItemName();
            if (!attributeNames.add(attrName)) {
                throw new SchemaException("Duplicate definition of attribute " + attrName + " in " + this);
            }
        }

        Collection<? extends ResourceAttributeDefinition<?>> primaryIdentifiers = getPrimaryIdentifiers();
        Collection<? extends ResourceAttributeDefinition<?>> secondaryIdentifiers = getSecondaryIdentifiers();

        if (primaryIdentifiers.isEmpty() && secondaryIdentifiers.isEmpty()) {
            throw new SchemaException("No identifiers in definition of object class " + getTypeName() + " in " + this);
        }
    }

    public @NotNull ResourceObjectTypeDefinitionType getDefinitionBean() {
        return definitionBean;
    }

    void addAssociationDefinition(@NotNull ResourceAssociationDefinition associationDef) {
        checkMutable();
        associationDefinitions.add(associationDef);
    }

    void addAuxiliaryObjectClassDefinition(@NotNull ResourceObjectDefinition definition) {
        checkMutable();
        auxiliaryObjectClassDefinitions.add(definition);
    }

    public void setDisplayNameAttributeName(QName name) {
        checkMutable();
        this.displayNameAttributeName = name;
    }
}
