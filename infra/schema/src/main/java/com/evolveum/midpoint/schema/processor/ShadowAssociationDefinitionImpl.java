/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import static com.google.common.collect.ImmutableSetMultimap.flatteningToImmutableSetMultimap;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemMerger;
import com.evolveum.midpoint.prism.key.NaturalKeyDefinition;
import com.evolveum.midpoint.schema.simulation.ExecutionModeProvider;

import com.evolveum.midpoint.util.DebugUtil;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.PrismContainerDefinition.PrismContainerDefinitionMutator;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.impl.delta.ContainerDeltaImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.config.ResourceObjectAssociationConfigItem;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * The (currently) only implementation of {@link ShadowAssociationDefinition}.
 *
 * TODO Effectively immutable? (if constituent definitions are immutable), except for the ability of
 *  changing the {@link #maxOccurs} value. - is this still true?
 */
public class ShadowAssociationDefinitionImpl
        extends AbstractFreezable
        implements ShadowAssociationDefinition,
        PrismContainerDefinitionMutator<ShadowAssociationValueType> {

    @Serial private static final long serialVersionUID = 1L;

    @NotNull private final ItemName itemName;

    /** The definition of the attribute this association is based on. It exists even for legacy simulated associations. */
    @NotNull private final ShadowReferenceAttributeDefinition referenceAttributeDefinition;

    /** This is the relevant part (specific to given subject association) of the "modern" association type definition. */
    @Nullable private final ShadowAssociationDefinitionType modernAssociationDefinitionBean;

    /** The "modern" association type definition. */
    @Nullable private final ShadowAssociationTypeDefinitionType modernAssociationTypeDefinitionBean;

    /** Extracts from the legacy configuration bean. */
    @Nullable private final LegacyAssociationTypeInformation legacyInformation;

    /** TEMPORARY: Mutable because of GUI! */
    private Integer maxOccurs;

    /**
     * Refined definition for {@link ShadowAssociationValueType} values that are stored in the
     * {@link ShadowAssociation} item as {@link ShadowAssociationValue}s.
     */
    @NotNull private final ComplexTypeDefinition complexTypeDefinition;

    private ShadowAssociationDefinitionImpl(
            @NotNull ItemName itemName,
            @NotNull ShadowReferenceAttributeDefinition referenceAttributeDefinition,
            @Nullable ShadowAssociationDefinitionType modernAssociationDefinitionBean,
            @Nullable ShadowAssociationTypeDefinitionType modernAssociationTypeDefinitionBean,
            @Nullable LegacyAssociationTypeInformation legacyInformation,
            @Nullable Integer maxOccurs) {
        this.itemName = itemName;
        this.referenceAttributeDefinition = referenceAttributeDefinition;
        this.modernAssociationDefinitionBean = modernAssociationDefinitionBean;
        this.modernAssociationTypeDefinitionBean = modernAssociationTypeDefinitionBean;
        this.legacyInformation = legacyInformation;
        this.maxOccurs = maxOccurs;
        this.complexTypeDefinition = createComplexTypeDefinition();
    }

    static ShadowAssociationDefinitionImpl parseLegacy(
            @NotNull ResourceObjectAssociationConfigItem.Legacy definitionCI,
            @NotNull ResourceSchemaImpl schemaBeingParsed,
            @NotNull ResourceObjectTypeDefinition subjectDefinition,
            @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions) throws ConfigurationException {

        var legacyInformation = new LegacyAssociationTypeInformation(
                definitionCI.value().getOutbound(),
                List.copyOf(definitionCI.value().getInbound()));

        var simulatedReferenceTypeDefinition =
                SimulatedShadowReferenceTypeDefinition.Legacy.parse(
                        definitionCI, schemaBeingParsed, subjectDefinition, objectTypeDefinitions);

        var updatedAttrDefBean = definitionCI.value().clone();
        updatedAttrDefBean.setOutbound(null);
        updatedAttrDefBean.getInbound().clear();

        var simulatedReferenceAttrDefinition =
                ShadowReferenceAttributeDefinitionImpl.fromSimulated(
                        simulatedReferenceTypeDefinition, updatedAttrDefBean);
        return new ShadowAssociationDefinitionImpl(
                simulatedReferenceAttrDefinition.getItemName(),
                simulatedReferenceAttrDefinition,
                null,
                null,
                legacyInformation,
                null);
    }

    static ShadowAssociationDefinitionImpl modern(
            @NotNull ItemName associationName,
            @NotNull ShadowReferenceAttributeDefinition referenceAttributeDefinition,
            @NotNull ShadowAssociationDefinitionType associationDefinitionBean,
            @NotNull ShadowAssociationTypeDefinitionType associationTypeDefinitionBean) {
        return new ShadowAssociationDefinitionImpl(
                associationName, referenceAttributeDefinition,
                associationDefinitionBean, associationTypeDefinitionBean, null, null);
    }

    private static ResourceItemDefinitionType toExistingImmutable(@Nullable ResourceItemDefinitionType customizationBean) {
        return CloneUtil.toImmutable(Objects.requireNonNullElseGet(customizationBean, ResourceItemDefinitionType::new));
    }

    @Override
    public ItemProcessing getProcessing() {
        return null; // TODO implement if needed
    }

    @Override
    public boolean isOperational() {
        return false;
    }

    @Override
    public boolean isAlwaysUseForEquals() {
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
    public @NotNull ShadowAssociation instantiate() throws SchemaException {
        return ShadowAssociation.empty(this);
    }

    @Override
    public @NotNull PrismContainer<ShadowAssociationValueType> instantiate(QName name) throws SchemaException {
        return ShadowAssociation.empty(name, this);
    }

//    @Override
//    public @NotNull ShadowAssociationDefinitionImpl forLayer(@NotNull LayerType layer) {
//        if (layer == currentLayer) {
//            return this;
//        } else {
//            return new ShadowAssociationDefinitionImpl(
//                    layer,
//                    nativeDefinition,
//                    customizationBean,
//                    limitationsMap,
//                    accessOverride.clone(), // TODO do we want to preserve also the access override?
//                    referenceTypeDefinition,
//                    maxOccurs);
//        }
//    }

    /**
     * We assume that the checks during the definition parsing were good enough to discover any problems
     * related to broken configuration.
     */
    private static SystemException alreadyChecked(ConfigurationException e) {
        return SystemException.unexpected(e, "(object was already checked)");
    }

    @Override
    public @NotNull ItemName getItemName() {
        return itemName;
    }

    @Override
    public @NotNull QName getTypeName() {
        return ShadowAssociationValueType.COMPLEX_TYPE;
    }

    @Override
    public int getMinOccurs() {
        return 0;
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
    public boolean isOptionalCleanup() {
        return false;
    }

    @Override
    public boolean isElaborate() {
        return false;
    }

    private @NotNull ComplexTypeDefinition createComplexTypeDefinition() {
        var genericDefinition = stateNonNull(
                PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(ShadowAssociationValueType.COMPLEX_TYPE),
                "No definition for %s", ShadowAssociationValueType.COMPLEX_TYPE);

        ComplexTypeDefinition def = genericDefinition.clone();

        if (hasAssociationObject()) {
            ResourceObjectDefinition resourceObjectDefinition = getReferenceAttributeDefinition().getTargetObjectClass();
            def.mutator().replaceDefinition(
                    ShadowAssociationValueType.F_ATTRIBUTES,
                    new ShadowAttributesContainerDefinitionImpl(
                            ShadowAssociationValueType.F_ATTRIBUTES,
                            resourceObjectDefinition.getSimpleAttributesComplexTypeDefinition()));
            def.mutator().replaceDefinition(
                    ShadowAssociationValueType.F_OBJECTS,
                    new ShadowAttributesContainerDefinitionImpl(
                            ShadowAssociationValueType.F_OBJECTS,
                            resourceObjectDefinition.getReferenceAttributesComplexTypeDefinition()));
        } else {
            def.mutator().delete(ShadowAssociationValueType.F_ATTRIBUTES); // ...or replace with empty PCD/CTD
            def.mutator().delete(ShadowAssociationValueType.F_ACTIVATION); // ...or leave it as it is
            def.mutator().replaceDefinition(
                    ShadowAssociationValueType.F_OBJECTS,
                    new ShadowAttributesContainerDefinitionImpl(
                            ShadowAssociationValueType.F_OBJECTS,
                            new ShadowSingleReferenceAttributeComplexTypeDefinitionImpl(
                                    getReferenceAttributeDefinition().clone())));
        }

//        // We apply the prism shadow definition for (representative) target object to the shadowRef definition.
//        var attributesDef = Objects.requireNonNull(def.findContainerDefinition(ShadowAssociationValueType.F_ATTRIBUTES)).clone();
//        attributesDef.mutator().setTargetObjectDefinition(
//                getRepresentativeTargetObjectDefinition().getPrismObjectDefinition());
//        def.mutator().replaceDefinition(
//                ShadowAssociationValueType.F_SHADOW_REF,
//                attributesDef);
//        def.mutator().setRuntimeSchema(true);

        // We have to use migrator, because we don't want to create a special implementation of ComplexTypeDefinition
        // interface here. (Just like ShadowReferenceAttributeDefinitionImpl is a special implementation of
        // PrismReferenceDefinition that provides the migration from PrismReferenceValue to ShadowReferenceAttributeValue.)
        def.mutator().setValueMigrator(new ComplexTypeDefinition.ValueMigrator() {
            @Override
            public @NotNull <C extends Containerable> PrismContainerValue<C> migrateIfNeeded(@NotNull PrismContainerValue<C> value) {
                if (value instanceof ShadowAssociationValue) {
                    return value;
                } else {
                    PrismContainerValue<C> converted;
                    try {
                        //noinspection unchecked
                        converted = (PrismContainerValue<C>) ShadowAssociationValue.fromBean(
                                (ShadowAssociationValueType) value.asContainerable(),
                                ShadowAssociationDefinitionImpl.this);
                    } catch (SchemaException e) {
                        throw new RuntimeException(e); // FIXME
                    }
                    converted.setParent(value.getParent());
                    return converted;
                }
            }
        });

        def.freeze();
        return def;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int getMaxOccurs() {
        //noinspection ReplaceNullCheck
        if (maxOccurs != null) {
            return maxOccurs;
        } else {
            return referenceAttributeDefinition.getMaxOccurs();
        }
    }

    @Override
    public void setMinOccurs(int value) {
    }

    public void setMaxOccurs(int value) {
        checkMutable();
        maxOccurs = value;
    }

    public @Nullable ShadowAssociationDefinitionType getModernAssociationDefinitionBean() {
        return modernAssociationDefinitionBean;
    }

    @Override
    public @Nullable ShadowAssociationTypeDefinitionType getModernAssociationTypeDefinitionBean() {
        return modernAssociationTypeDefinitionBean;
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
            if (clazz.isAssignableFrom(ShadowAssociationDefinitionImpl.class)) {
                //noinspection unchecked
                return (ID) this;
            } else {
                return null;
            }
        }
        return complexTypeDefinition.findItemDefinition(path, clazz);
    }

    @Override
    public @Nullable MappingType getExplicitOutboundMappingBean() {
        if (legacyInformation != null) {
            return legacyInformation.outboundMappingBean();
        }
        return null;
    }

    @Override
    public @NotNull Collection<InboundMappingType> getExplicitInboundMappingBean() {
        if (legacyInformation != null) {
            return legacyInformation.inboundMappingBeans();
        }
        if (modernAssociationDefinitionBean != null) {
            return modernAssociationDefinitionBean.getInbound();
        }
        return List.of();
    }

    @Override
    public boolean isVisible(ExecutionModeProvider modeProvider) {
        if (modernAssociationTypeDefinitionBean != null
                && !modeProvider.canSee(modernAssociationTypeDefinitionBean.getLifecycleState())) {
            return false;
        }
        return referenceAttributeDefinition.isVisible(modeProvider);
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
    public @NotNull ShadowAssociationDefinitionImpl clone() {
        return new ShadowAssociationDefinitionImpl(
                itemName, referenceAttributeDefinition, modernAssociationDefinitionBean, modernAssociationTypeDefinitionBean, null, maxOccurs);
    }

    @Override
    public PrismContainerDefinition<ShadowAssociationValueType> cloneWithNewDefinition(QName newItemName, ItemDefinition<?> newDefinition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceDefinition(QName itemName, ItemDefinition<?> newDefinition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ShadowAssociationValue createValue() {
        return ShadowAssociationValue.empty(this);
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
    public @NotNull PrismContainerDefinitionMutator<ShadowAssociationValueType> mutator() {
        checkMutableOnExposing();
        return this;
    }

    @Override
    public ShadowAssociationDefinitionImpl deepClone(@NotNull DeepCloneOperation operation) {
        return this; // TODO ???
    }

    @Override
    public void debugDumpShortToString(StringBuilder sb) {
        sb.append(this); // FIXME
    }

//    @Override
//    public String toString() {
//        return getClass().getSimpleName() + "{" +
//                "item=" + getItemName() +
//                ", type=" + associationClassDefinition +
//                "}";
//    }


    public String getDebugDumpClassName() {
        return "SRefAttrDef";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass() , indent);
        DebugUtil.debugDumpWithLabelLn(sb, "item name", getItemName(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "reference attribute", referenceAttributeDefinition.toString(), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "association type definition bean", modernAssociationTypeDefinitionBean, indent + 1);
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
    public void revive(PrismContext prismContext) {
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        throw new UnsupportedOperationException();
    }

    public String getHumanReadableDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(getItemName());
        var displayName = getDisplayName();
        if (displayName != null) {
            sb.append(": ").append(displayName);
        }
        return sb.toString();
    }

    @Override
    public boolean isEntitlement() {
        throw new UnsupportedOperationException("FIXME implement");
    }

    public void shortDump(StringBuilder sb) {
        sb.append(this); // TODO
    }

    @Override
    public @NotNull Class<ShadowAssociationValueType> getTypeClass() {
        return ShadowAssociationValueType.class;
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return null;
    }

    @Override
    public @Nullable Map<QName, Object> getAnnotations() {
        return Map.of();
    }

    @Override
    public @Nullable String getMergerIdentifier() {
        return "";
    }

    @Override
    public @Nullable ItemMerger getMergerInstance(@NotNull MergeStrategy strategy, @Nullable OriginMarker originMarker) {
        return null;
    }

    @Override
    public @Nullable List<QName> getNaturalKeyConstituents() {
        return List.of(); // FIXME
    }

    @Override
    public @Nullable NaturalKeyDefinition getNaturalKeyInstance() {
        return null; // FIXME
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return null;
    }

    @Override
    public @NotNull ItemDefinition<PrismContainer<ShadowAssociationValueType>> cloneWithNewName(@NotNull ItemName itemName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull PrismContainerDefinition<?> cloneWithNewType(@NotNull QName newTypeName, @NotNull ComplexTypeDefinition newCtd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ShadowAssociationDefinitionImpl that = (ShadowAssociationDefinitionImpl) o;
        return Objects.equals(referenceAttributeDefinition, that.referenceAttributeDefinition)
                && Objects.equals(modernAssociationDefinitionBean, that.modernAssociationDefinitionBean)
                && Objects.equals(modernAssociationTypeDefinitionBean, that.modernAssociationTypeDefinitionBean)
                && Objects.equals(maxOccurs, that.maxOccurs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceAttributeDefinition, modernAssociationDefinitionBean, modernAssociationTypeDefinitionBean, maxOccurs);
    }

    @Override
    public void setCompileTimeClass(Class<ShadowAssociationValueType> compileTimeClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismPropertyDefinition<?> createPropertyDefinition(QName name, QName propType, int minOccurs, int maxOccurs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismPropertyDefinition<?> createPropertyDefinition(QName name, QName propType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismPropertyDefinition<?> createPropertyDefinition(String localName, QName propType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismContainerDefinition<?> createContainerDefinition(QName name, QName typeName, int minOccurs, int maxOccurs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismContainerDefinition<?> createContainerDefinition(@NotNull QName name, @NotNull ComplexTypeDefinition ctd, int minOccurs, int maxOccurs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProcessing(ItemProcessing processing) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setValueEnumerationRef(PrismReferenceValue valueEnumerationRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOperational(boolean operational) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAlwaysUseForEquals(boolean alwaysUseForEquals) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDynamic(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReadOnly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDeprecatedSince(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addSchemaMigration(SchemaMigration value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSchemaMigrations(List<SchemaMigration> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDeprecated(boolean deprecated) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRemoved(boolean removed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRemovedSince(String removedSince) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExperimental(boolean experimental) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPlannedRemoval(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setElaborate(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHeterogeneousListItem(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSubstitutionHead(QName value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIndexed(Boolean indexed) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIndexOnly(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInherited(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSearchable(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOptionalCleanup(boolean optionalCleanup) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRuntimeSchema(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMergerIdentifier(String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setNaturalKeyConstituents(List<QName> naturalKeyConstituents) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSchemaContextDefinition(SchemaContextDefinition schemaContextDefinition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCanRead(boolean val) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCanModify(boolean val) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCanAdd(boolean val) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDisplayHint(DisplayHint displayHint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEmphasized(boolean emphasized) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDisplayName(String displayName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDisplayOrder(Integer displayOrder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHelp(String help) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDocumentation(String documentation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDiagrams(List<ItemDiagramSpecification> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Collection<ResourceObjectInboundDefinition> getRelevantInboundDefinitions() {
        if (modernAssociationDefinitionBean == null) {
            return List.of();
        } else {
            return ResourceObjectInboundDefinition.forAssociation(modernAssociationDefinitionBean);
        }
    }

    @Override
    public @NotNull ShadowReferenceAttributeDefinition getReferenceAttributeDefinition() {
        return referenceAttributeDefinition;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canModify() {
        return true;
    }

    @Override
    public boolean canAdd() {
        return true;
    }

    @Override
    public Boolean isIndexed() {
        return null;
    }

    @Override
    public boolean isIndexOnly() {
        return false;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public String getDeprecatedSince() {
        return null;
    }

    @Override
    public String getPlannedRemoval() {
        return null;
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
    public boolean isExperimental() {
        return false;
    }

    @Override
    public @Nullable List<SchemaMigration> getSchemaMigrations() {
        return List.of();
    }

    @Override
    public DisplayHint getDisplayHint() {
        return null;
    }

    @Override
    public boolean isEmphasized() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return null;
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
    public String getDocumentation() {
        return null;
    }

    @Override
    public List<ItemDiagramSpecification> getDiagrams() {
        return List.of();
    }

    @Override
    public String getDocumentationPreview() {
        return null;
    }

    public @NotNull Multimap<QName, ShadowRelationParticipantType> getObjectParticipants(
            @NotNull CompleteResourceSchema resourceSchema) {
        var fromRefAttrDef = getObjectParticipantsFromRefAttrDef();
        if (modernAssociationTypeDefinitionBean == null || modernAssociationTypeDefinitionBean.getObject().isEmpty()) {
            return fromRefAttrDef;
        }
        var mapCopy = MultimapBuilder.hashKeys().hashSetValues().build(fromRefAttrDef);
        for (ShadowAssociationTypeObjectDefinitionType objectDefBean : modernAssociationTypeDefinitionBean.getObject()) {
            if (!objectDefBean.getObjectType().isEmpty()) {
                mapCopy.replaceValues(
                        Objects.requireNonNull(objectDefBean.getRef()),
                        parseParticipants(objectDefBean.getObjectType(), resourceSchema));
            }
        }
        return mapCopy;
    }

    private Collection<ShadowRelationParticipantType> parseParticipants(
            List<ResourceObjectTypeIdentificationType> objectTypes,
            CompleteResourceSchema resourceSchema) {
        // TODO move to the parser + treat errors as ConfigurationExceptions
        return objectTypes.stream()
                .map(type -> resourceSchema.getObjectTypeDefinitionRequired(ResourceObjectTypeIdentification.of(type)))
                .map(typeDef -> ShadowRelationParticipantType.forObjectType(typeDef))
                .collect(Collectors.toSet());
    }

    private Multimap<QName, ShadowRelationParticipantType> getObjectParticipantsFromRefAttrDef() {
        if (hasAssociationObject()) {
            // Objects are reference attributes from the association objects
            return getAssociationObjectDefinition().getReferenceAttributeDefinitions().stream()
                    .collect(flatteningToImmutableSetMultimap(
                            objectRefDef -> objectRefDef.getItemName(),
                            objectRefDef -> objectRefDef.getTargetParticipantTypes().stream()));
        } else {
            // "empty" association - just target objects, nothing inside, so immediate targets are the objects
            return ImmutableSetMultimap.<QName, ShadowRelationParticipantType>builder()
                    .putAll(
                            referenceAttributeDefinition.getItemName(),
                            referenceAttributeDefinition.getTargetParticipantTypes())
                    .build();
        }
    }

    private record LegacyAssociationTypeInformation(
            @Nullable MappingType outboundMappingBean,
            @NotNull List<InboundMappingType> inboundMappingBeans) implements Serializable {
    }
}
