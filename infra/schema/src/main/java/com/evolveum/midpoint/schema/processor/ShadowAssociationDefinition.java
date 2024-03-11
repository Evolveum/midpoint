/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.configItem;
import static com.evolveum.midpoint.util.MiscUtil.assertCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.impl.PrismContainerValueImpl;
import com.evolveum.midpoint.prism.impl.delta.ContainerDeltaImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.config.*;
import com.evolveum.midpoint.schema.simulation.ExecutionModeProvider;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Definition of a usually multi-valued association item, e.g., `ri:group`.
 *
 * Effectively immutable (if constituent definitions are immutable), except for the ability of changing the {@link #maxOccurs}
 * value.
 *
 * It has two parts, each of which can be empty:
 *
 * - "raw part"; obtained from the connector, without being connected to the schemaHandling information,
 * - TODO TODO TODO
 */
public class ShadowAssociationDefinition extends AbstractFreezable
        implements Serializable, Visitable<Definition>, Freezable, DebugDumpable,
        PrismContainerDefinition<ShadowAssociationValueType>,
        ShadowItemDefinition<ShadowAssociation>,
        MutablePrismContainerDefinition.Unsupported<ShadowAssociationValueType> {

    @Serial private static final long serialVersionUID = 1L;

    /** Name of the association item (in the subject). */
    @NotNull private final ItemName associationItemName;

    /** Participant-independent definition of the association. Not present for pure raw definitions. */
    @Nullable private final ShadowAssociationTypeDefinition associationTypeDefinition;

    /** Refined definition for {@link ShadowAssociationValueType} values that are stored in the {@link ShadowAssociation} item. */
    @NotNull private final ComplexTypeDefinition complexTypeDefinition;

    /** Information from the connector. Not present for simulated associations. */
    @Nullable private final RawShadowAssociationDefinition rawDefinition;

    /** The configuration item (wrapping the definition bean). Either legacy or "new". May be empty. */
    @NotNull private final AssociationConfigItem configItem;

    private Integer maxOccurs;

    private ShadowAssociationDefinition(
            @NotNull QName associationItemName,
            @Nullable ShadowAssociationTypeDefinition associationTypeDefinition,
            @Nullable RawShadowAssociationDefinition rawDefinition,
            @NotNull AssociationConfigItem configItem) {
        this.associationTypeDefinition = associationTypeDefinition;
        this.rawDefinition = rawDefinition;
        this.configItem = configItem;
        this.associationItemName = ItemName.fromQName(associationItemName);
        this.complexTypeDefinition = createComplexTypeDefinition();
    }

    static ShadowAssociationDefinition parseAssociationType(
            @NotNull ItemName associationItemName,
            @NotNull ShadowAssociationTypeDefinition associationTypeDefinition,
            @Nullable RawShadowAssociationDefinition rawDefinition,
            @NotNull ShadowAssociationTypeDefinitionConfigItem definitionCI) {
        return new ShadowAssociationDefinition(associationItemName, associationTypeDefinition, rawDefinition, definitionCI);
    }

    static ShadowAssociationDefinition parseLegacy(
            @NotNull ShadowAssociationTypeDefinition associationTypeDefinition,
            @NotNull ResourceObjectAssociationConfigItem definitionCI) throws ConfigurationException {
        return new ShadowAssociationDefinition( // Legacy associations cannot be connected to raw definitions
                definitionCI.getItemName(), associationTypeDefinition, null, definitionCI);
    }

    public static ShadowAssociationDefinition fromRaw(
            @NotNull RawShadowAssociationDefinition rawDefinition,
            @Nullable ShadowAssociationTypeDefinition associationTypeDefinition) {
        var emptyCI = configItem(
                new ShadowAssociationTypeDefinitionType(),
                ConfigurationItemOrigin.generated(),
                ShadowAssociationTypeDefinitionConfigItem.class);
        return new ShadowAssociationDefinition(
                rawDefinition.getItemName(), associationTypeDefinition, rawDefinition, emptyCI);
    }

    /** Throws an exception if the definition is raw. */
    public @NotNull ShadowAssociationTypeDefinition getAssociationTypeDefinition() {
        return stateNonNull(associationTypeDefinition, "The association definition is raw: %s", this);
    }

    public @NotNull ResourceObjectTypeDefinition getTargetObjectDefinition() {
        return getAssociationTypeDefinition().getTargetObjectDefinition();
    }

    public boolean isEntitlement() {
        return getAssociationTypeDefinition().isEntitlement();
    }

    public @Nullable MappingConfigItem getOutboundMapping() throws ConfigurationException {
        return configItem.getOutboundMapping();
    }

    public @NotNull List<InboundMappingConfigItem> getInboundMappings() throws ConfigurationException {
        return configItem.getInboundMappings();
    }

    public boolean isExclusiveStrong() {
        return configItem.isExclusiveStrong();
    }

    @Override
    public @NotNull QName getTypeName() {
        return ShadowAssociationValueType.COMPLEX_TYPE;
    }

    @Override
    public boolean isRuntimeSchema() {
        return true;
    }

    @Override
    public ItemProcessing getProcessing() {
        return null; // should be implemented some day
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isDeprecated() {
        try {
            return configItem.isDeprecated();
        } catch (ConfigurationException e) {
            throw alreadyChecked(e);
        }
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    @Override
    public String getRemovedSince() {
        return null;
    }

    @Override
    public boolean isOptionalCleanup() {
        return true;
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
        return getDisplayHint() == DisplayHint.EMPHASIZED;
    }

    @Override
    public DisplayHint getDisplayHint() {
        return MiscSchemaUtil.toDisplayHint(configItem.getDisplayHint());
    }

    /**
     * We assume that the checks during the definition parsing were good enough to discover any problems
     * related to broken configuration.
     */
    private static SystemException alreadyChecked(ConfigurationException e) {
        return SystemException.unexpected(e, "(object was already checked)");
    }

    public boolean isIgnored(LayerType layer) throws SchemaException {
        return false; // FIXME implement
    }

    public PropertyLimitations getLimitations(LayerType layer) throws SchemaException {
        return null; // FIXME implement
    }

    public boolean isTolerant() {
        return configItem.isTolerant();
    }

    @NotNull
    public List<String> getTolerantValuePattern() {
        return configItem.getTolerantValuePatterns();
    }

    @NotNull
    public List<String> getIntolerantValuePattern() {
        return configItem.getIntolerantValuePatterns();
    }

    public String getDisplayName() {
        return configItem.getDisplayName();
    }

    @Override
    public Integer getDisplayOrder() {
        return configItem.getDisplayOrder();
    }

    @Override
    public String getHelp() {
        return configItem.getHelp();
    }

    @Override
    public String getDocumentation() {
        return configItem.getDocumentation();
    }

    @Override
    public String getDocumentationPreview() {
        return null;
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return null;
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
    }

    @Override
    public @Nullable Map<QName, Object> getAnnotations() {
        return null;
    }

    @Override
    public @Nullable List<SchemaMigration> getSchemaMigrations() {
        return null;
    }

    @Override
    public List<ItemDiagramSpecification> getDiagrams() {
        return null;
    }

    public String getLifecycleState() {
        return configItem.getLifecycleState();
    }

    private @NotNull ComplexTypeDefinition createComplexTypeDefinition() {
        var rawDef = MiscUtil.stateNonNull(
                PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(ShadowAssociationValueType.COMPLEX_TYPE),
                "No definition for %s", ShadowAssociationValueType.COMPLEX_TYPE);

        if (associationTypeDefinition != null) {
            MutableComplexTypeDefinition def = rawDef.clone().toMutable();
            // TODO optimize this by keeping only "important" definitions (e.g. the ones that are actually used by the association)
            def.replaceDefinition(
                    ShadowAssociationValueType.F_IDENTIFIERS,
                    getTargetObjectDefinition()
                            .toResourceAttributeContainerDefinition(ShadowAssociationValueType.F_IDENTIFIERS));
            def.freeze();
            return def;
        } else {
            return rawDef; // FIXME
        }
    }

    public boolean isVisible(@NotNull ExecutionModeProvider executionModeProvider) {
        return SimulationUtil.isVisible(getLifecycleState(), executionModeProvider);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public @NotNull ItemName getItemName() {
        return associationItemName;
    }

    @Override
    public int getMinOccurs() {
        return rawDefinition != null ? rawDefinition.getMinOccurs() : 0;
    }

    @Override
    public int getMaxOccurs() {
        if (maxOccurs != null) {
            return maxOccurs;
        } else if (rawDefinition != null) {
            return rawDefinition.getMaxOccurs();
        } else {
            return -1;
        }
    }

    @Override
    public void setMaxOccurs(int value) {
        checkMutable();
        maxOccurs = value;
    }

    @Override
    public boolean isOperational() {
        return false;
    }

    @Override
    public boolean isAlwaysUseForEquals() {
        return true;
    }

    @Override
    public boolean isIndexOnly() {
        return false;
    }

    @Override
    public boolean isInherited() {
        return false;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public QName getSubstitutionHead() {
        return null;
    }

    @Override
    public boolean isHeterogeneousListItem() {
        return false;
    }

    @Override
    public PrismReferenceValue getValueEnumerationRef() {
        return null;
    }

    @Override
    public boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition<?>> clazz, boolean caseInsensitive) {
        Preconditions.checkArgument(!caseInsensitive, "Case-insensitive search is not supported");
        return QNameUtil.match(elementQName, getItemName())
                && clazz.isInstance(this);
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        if (path.isEmpty()) {
            if (clazz.isAssignableFrom(ShadowAssociationDefinition.class)) {
                //noinspection unchecked
                return (ID) this;
            } else {
                return null;
            }
        }
        return complexTypeDefinition.findItemDefinition(path, clazz);
    }

    @Override
    public void adoptElementDefinitionFrom(ItemDefinition<?> otherDef) {
        // TODO
    }

    @Override
    public @NotNull ShadowAssociation instantiate() throws SchemaException {
        return instantiate(getItemName());
    }

    @Override
    public @NotNull ShadowAssociation instantiate(QName name) throws SchemaException {
        return new ShadowAssociation(name, this);
    }

    @Override
    public Class<ShadowAssociationValueType> getCompileTimeClass() {
        return ShadowAssociationValueType.class;
    }

    @Override
    public @NotNull ComplexTypeDefinition getComplexTypeDefinition() {
        return complexTypeDefinition;
    }

    @Override
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {
        return complexTypeDefinition.getDefinitions();
    }

    @Override
    public List<PrismPropertyDefinition<?>> getPropertyDefinitions() {
        return complexTypeDefinition.getPropertyDefinitions();
    }

    @Override
    public @NotNull ContainerDelta<ShadowAssociationValueType> createEmptyDelta(ItemPath path) {
        return new ContainerDeltaImpl<>(path, this);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public @NotNull ShadowAssociationDefinition clone() {
        ShadowAssociationDefinition clone =
                new ShadowAssociationDefinition(associationItemName, associationTypeDefinition, rawDefinition, configItem);
        copyDefinitionDataFrom(clone);
        return clone;
    }

    private void copyDefinitionDataFrom(ShadowAssociationDefinition source) {
        maxOccurs = source.maxOccurs;
    }

    @Override
    public PrismContainerDefinition<ShadowAssociationValueType> cloneWithReplacedDefinition(QName itemName, ItemDefinition<?> newDefinition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceDefinition(QName itemName, ItemDefinition<?> newDefinition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismContainerValue<ShadowAssociationValueType> createValue() {
        return new PrismContainerValueImpl<>();
    }

    @Override
    public boolean isEmpty() {
        return complexTypeDefinition.isEmpty();
    }

    @Override
    public boolean canRepresent(@NotNull QName type) {
        return QNameUtil.match(type, getTypeName());
    }

    @Override
    public @NotNull ShadowAssociationDefinition toMutable() {
        checkMutableOnExposing();
        return this;
    }

    @Override
    public Class<ShadowAssociationValueType> getTypeClass() {
        return ShadowAssociationValueType.class;
    }

    @Override
    public ShadowAssociationDefinition deepClone(@NotNull DeepCloneOperation operation) {
        return this; // TODO ???
    }

    @Override
    public void debugDumpShortToString(StringBuilder sb) {
        sb.append(this); // FIXME
    }

    @Override
    public boolean canBeDefinitionOf(PrismContainer<ShadowAssociationValueType> item) {
        return true;
    }

    @Override
    public boolean canBeDefinitionOf(@NotNull PrismValue pvalue) {
        return pvalue instanceof PrismContainerValue<?> pcv
                && ShadowAssociationValueType.class.equals(pcv.getCompileTimeClass());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "item=" + associationItemName +
                ", type=" + associationTypeDefinition +
                "}";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "item name", associationItemName, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "type definition", associationTypeDefinition, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "config item", configItem, indent + 1);
        return sb.toString();
    }

    public @NotNull ItemPath getStandardPath() {
        return ItemPath.create(ShadowType.F_ASSOCIATIONS, getItemName());
    }

    public ContainerDelta<ShadowAssociationValueType> createEmptyDelta() {
        return PrismContext.get().deltaFactory().container().create(
                getStandardPath(), this);
    }

    @Override
    public boolean canRead() {
        return rawDefinition == null || rawDefinition.canRead();
    }

    @Override
    public boolean canModify() {
        return rawDefinition == null || rawDefinition.canModify();
    }

    @Override
    public boolean canAdd() {
        return rawDefinition == null || rawDefinition.canAdd();
    }

    @Override
    public void revive(PrismContext prismContext) {
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        throw new UnsupportedOperationException();
    }

    public String getHumanReadableDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(associationItemName);
        var displayName = getDisplayName();
        if (displayName != null) {
            sb.append(": ").append(displayName);
        }
        return sb.toString();
    }

    /** Creates a filter that provides all shadows eligible as the target value for this association. */
    public @NotNull ObjectFilter createTargetObjectsFilter() {
        Collection<ResourceObjectTypeDefinition> objectTypeDefinitions = getAssociationTypeDefinition().getObjectTypeDefinitions();
        assertCheck(!objectTypeDefinitions.isEmpty(), "No object type definitions (already checked)");
        S_FilterEntryOrEmpty atomicFilter = PrismContext.get().queryFor(ShadowType.class);
        List<ObjectFilter> orFilterClauses = new ArrayList<>();
        objectTypeDefinitions.stream()
                .map(def -> def.getTypeIdentification())
                .forEach(typeId -> orFilterClauses.add(
                        atomicFilter
                                .item(ShadowType.F_KIND).eq(typeId.getKind())
                                .and().item(ShadowType.F_INTENT).eq(typeId.getIntent())
                                .buildFilter()));
        OrFilter intentFilter = PrismContext.get().queryFactory().createOr(orFilterClauses);

        var resourceOid = stateNonNull(getTargetObjectDefinition().getResourceOid(), "No resource OID in %s", this);
        return atomicFilter.item(ShadowType.F_RESOURCE_REF).ref(resourceOid, ResourceType.COMPLEX_TYPE)
                .and().filter(intentFilter)
                .buildFilter();
    }

    public @Nullable ShadowAssociationClassSimulationDefinition getSimulationDefinition() {
        return getAssociationTypeDefinition().getSimulationDefinition();
    }

    public boolean isSimulated() {
        return getSimulationDefinition() != null;
    }

    public ShadowAssociationClassSimulationDefinition getSimulationDefinitionRequired() {
        assert isSimulated();
        return Objects.requireNonNull(getSimulationDefinition());
    }

    public boolean isRaw() {
        return configItem instanceof ShadowAssociationTypeDefinitionConfigItem ci
                && ci.value().asPrismContainerValue().isEmpty()
                && rawDefinition != null;
    }

    public @Nullable RawShadowAssociationDefinition getRawDefinition() {
        return rawDefinition;
    }

    public @NotNull RawShadowAssociationDefinition getRawDefinitionRequired() {
        return Objects.requireNonNull(rawDefinition);
    }
}
