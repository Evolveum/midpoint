/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.assertCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Definition of a usually multi-valued association item, e.g., `ri:group`.
 *
 * Effectively immutable (if constituent definitions are immutable), except for the ability of changing the {@link #maxOccurs}
 * value.
 */
public class ShadowAssociationDefinition extends AbstractFreezable
        implements Serializable, Visitable<Definition>, Freezable, DebugDumpable,
        PrismContainerDefinition<ShadowAssociationValueType>,
        MutablePrismContainerDefinition.Unsupported<ShadowAssociationValueType> {

    @Serial private static final long serialVersionUID = 1L;

    /** Name of the association item (in the subject). */
    @NotNull private final ItemName associationItemName;

    /** Participant-independent definition of the association. */
    @NotNull private final ShadowAssociationTypeDefinition associationTypeDefinition;

    /** Refined definition for {@link ShadowAssociationValueType} values that are stored in the {@link ShadowAssociation} item. */
    @NotNull private final ComplexTypeDefinition complexTypeDefinition;

    /** The configuration item (wrapping the definition bean). Either legacy or "new". */
    @NotNull private final AssociationConfigItem associationConfigItem;

    private int maxOccurs = -1;

    private ShadowAssociationDefinition(
            @NotNull ItemName associationItemName,
            @NotNull ShadowAssociationTypeDefinition associationTypeDefinition,
            @NotNull AssociationConfigItem associationConfigItem) {
        this.associationTypeDefinition = associationTypeDefinition;
        this.associationConfigItem = associationConfigItem;
        this.associationItemName = associationItemName;
        this.complexTypeDefinition = createComplexTypeDefinition();
    }

    static ShadowAssociationDefinition parseAssociationType(
            @NotNull ShadowAssociationTypeDefinition associationTypeDefinition,
            @NotNull ShadowAssociationClassSimulationDefinition simulationDefinition,
            @NotNull ShadowAssociationTypeDefinitionConfigItem definitionCI) {
        return new ShadowAssociationDefinition(
                simulationDefinition.getLocalSubjectItemName(),
                associationTypeDefinition,
                definitionCI);
    }

    static ShadowAssociationDefinition parseLegacy(
            @NotNull ShadowAssociationTypeDefinition associationTypeDefinition,
            @NotNull ResourceObjectAssociationConfigItem definitionCI) throws ConfigurationException {
        return new ShadowAssociationDefinition(
                definitionCI.getItemName(),
                associationTypeDefinition,
                definitionCI);
    }

    public @NotNull ShadowAssociationTypeDefinition getAssociationTypeDefinition() {
        return associationTypeDefinition;
    }

    public @NotNull ResourceObjectTypeDefinition getTargetObjectDefinition() {
        return associationTypeDefinition.getTargetObjectDefinition();
    }

    public boolean isEntitlement() {
        return associationTypeDefinition.isEntitlement();
    }

    public @Nullable MappingConfigItem getOutboundMapping() throws ConfigurationException {
        return associationConfigItem.getOutboundMapping();
    }

    public @NotNull List<InboundMappingConfigItem> getInboundMappings() throws ConfigurationException {
        return associationConfigItem.getInboundMappings();
    }

    public boolean isExclusiveStrong() {
        return associationConfigItem.isExclusiveStrong();
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
            return associationConfigItem.isDeprecated();
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
        return MiscSchemaUtil.toDisplayHint(associationConfigItem.getDisplayHint());
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
        return associationConfigItem.isTolerant();
    }

    @NotNull
    public List<String> getTolerantValuePattern() {
        return associationConfigItem.getTolerantValuePatterns();
    }

    @NotNull
    public List<String> getIntolerantValuePattern() {
        return associationConfigItem.getIntolerantValuePatterns();
    }

    public String getDisplayName() {
        return associationConfigItem.getDisplayName();
    }

    @Override
    public Integer getDisplayOrder() {
        return associationConfigItem.getDisplayOrder();
    }

    @Override
    public String getHelp() {
        return associationConfigItem.getHelp();
    }

    @Override
    public String getDocumentation() {
        return associationConfigItem.getDocumentation();
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
        return associationConfigItem.getLifecycleState();
    }

    private @NotNull ComplexTypeDefinition createComplexTypeDefinition() {
        var rawDef = MiscUtil.stateNonNull(
                PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(ShadowAssociationValueType.COMPLEX_TYPE),
                "No definition for %s", ShadowAssociationValueType.COMPLEX_TYPE);

        MutableComplexTypeDefinition def = rawDef.clone().toMutable();
        // TODO optimize this by keeping only "important" definitions (e.g. the ones that are actually used by the association)
        def.replaceDefinition(
                ShadowAssociationValueType.F_IDENTIFIERS,
                getTargetObjectDefinition()
                        .toResourceAttributeContainerDefinition(ShadowAssociationValueType.F_IDENTIFIERS));
        def.freeze();
        return def;
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
        return 0;
    }

    @Override
    public int getMaxOccurs() {
        return maxOccurs;
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
        return new ContainerDeltaImpl<>(path, this, PrismContext.get());
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public @NotNull ShadowAssociationDefinition clone() {
        ShadowAssociationDefinition clone =
                new ShadowAssociationDefinition(associationItemName, associationTypeDefinition, associationConfigItem);
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
        return new PrismContainerValueImpl<>(getPrismContext());
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
    public ShadowAssociationDefinition toMutable() {
        checkMutableOnExposing();
        return this;
    }

    @Override
    public Class<ShadowAssociationValueType> getTypeClass() {
        return ShadowAssociationValueType.class;
    }

    @Override
    public ItemDefinition<PrismContainer<ShadowAssociationValueType>> deepClone(@NotNull DeepCloneOperation operation) {
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
        DebugUtil.debugDumpWithLabel(sb, "config item", associationConfigItem, indent + 1);
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
    public PrismContext getPrismContext() {
        return PrismContext.get();
    }

    @Override
    public boolean canRead() {
        return true; // FIXME
    }

    @Override
    public boolean canModify() {
        return true; // FIXME
    }

    @Override
    public boolean canAdd() {
        return true; // FIXME
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
        return associationTypeDefinition.getSimulationDefinition();
    }

    public boolean isSimulated() {
        return getSimulationDefinition() != null;
    }

    public ShadowAssociationClassSimulationDefinition getSimulationDefinitionRequired() {
        assert isSimulated();
        return Objects.requireNonNull(getSimulationDefinition());
    }
}
