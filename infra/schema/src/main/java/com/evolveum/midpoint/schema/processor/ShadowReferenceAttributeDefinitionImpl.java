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
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainerDefinition.PrismContainerDefinitionMutator;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReferencesCapabilityType;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.impl.delta.ContainerDeltaImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.config.ResourceObjectAssociationConfigItem;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Definition of a shadow association item, e.g., `ri:group`.
 *
 * Note that unlike the attributes, here the {@link ShadowAttributeDefinitionImpl#nativeDefinition} may be generated artificially
 * based on simulated {@link ReferencesCapabilityType} definition.
 *
 * TODO various options for configuring associations
 *
 * TODO Effectively immutable? (if constituent definitions are immutable), except for the ability of
 *  changing the {@link #maxOccurs} value. - is this still true?
 */
public class ShadowReferenceAttributeDefinitionImpl
        extends ShadowAttributeDefinitionImpl<ShadowReferenceAttribute, ShadowAssociationValueType, NativeShadowReferenceAttributeDefinition>
        implements ShadowReferenceAttributeDefinition,
        PrismContainerDefinitionMutator<ShadowAssociationValueType> {

    @Serial private static final long serialVersionUID = 1L;

    /** Participant-independent definition of the reference type. */
    @NotNull private final AbstractShadowReferenceTypeDefinition referenceTypeDefinition;

    /**
     * Refined definition for {@link ShadowAssociationValueType} values that are stored in the
     * {@link ShadowReferenceAttribute} item.
     *
     * TODO remove after the content is migrated to a pure reference.
     */
    @NotNull private final ComplexTypeDefinition complexTypeDefinition;

    /** TEMPORARY: Mutable because of GUI! */
    private Integer maxOccurs;

    /** TODO */
    private ShadowAssociationDefinitionType associationDefinitionBean;

    private ShadowReferenceAttributeDefinitionImpl(
            @NotNull AbstractShadowReferenceTypeDefinition typeDefinition,
            @NotNull NativeShadowReferenceAttributeDefinition nativeDefinition,
            @NotNull ResourceItemDefinitionType configurationBean) throws SchemaException {
        super(nativeDefinition, configurationBean, false);
        this.referenceTypeDefinition = typeDefinition;
        this.complexTypeDefinition = createComplexTypeDefinition();
    }

    private ShadowReferenceAttributeDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull NativeShadowReferenceAttributeDefinition nativeDefinition,
            @NotNull ResourceItemDefinitionType customizationBean,
            @NotNull Map<LayerType, PropertyLimitations> limitationsMap,
            @NotNull PropertyAccessType accessOverride,
            @NotNull AbstractShadowReferenceTypeDefinition referenceTypeDefinition,
            Integer maxOccurs) {
        super(layer, nativeDefinition, customizationBean, limitationsMap, accessOverride);
        this.referenceTypeDefinition = referenceTypeDefinition;
        this.complexTypeDefinition = createComplexTypeDefinition();
        this.maxOccurs = maxOccurs;
    }

    static ShadowReferenceAttributeDefinitionImpl parseLegacy(
            @NotNull ResourceObjectAssociationConfigItem.Legacy definitionCI,
            @NotNull ResourceSchemaImpl schemaBeingParsed,
            @NotNull ResourceObjectTypeDefinition referentialSubjectDefinition,
            @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions) throws ConfigurationException {
        try {
            return new ShadowReferenceAttributeDefinitionImpl(
                    SimulatedShadowReferenceTypeDefinition.Legacy.parse(
                            definitionCI,
                            schemaBeingParsed,
                            referentialSubjectDefinition,
                            objectTypeDefinitions),
                    NativeShadowAttributeDefinitionImpl.forSimulatedAssociation(
                            definitionCI.getItemName(),
                            definitionCI.getItemName(),
                            ShadowReferenceParticipantRole.SUBJECT),
                    definitionCI.value());
        } catch (SchemaException e) {
            throw new ConfigurationException(e);
        }
    }

    static ShadowReferenceAttributeDefinitionImpl fromNative(
            @NotNull NativeShadowReferenceAttributeDefinition rawDefinition,
            @NotNull AbstractShadowReferenceTypeDefinition associationClassDefinition,
            @Nullable ResourceItemDefinitionType customizationBean) {
        try {
            return new ShadowReferenceAttributeDefinitionImpl(
                    associationClassDefinition,
                    rawDefinition,
                    toExistingImmutable(customizationBean));
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "TEMPORARY");
        }
    }

    static ShadowReferenceAttributeDefinition fromSimulated(
            @NotNull SimulatedShadowReferenceTypeDefinition simulationDefinition,
            @Nullable ResourceItemDefinitionType assocDefBean) {
        try {
            return new ShadowReferenceAttributeDefinitionImpl(
                    simulationDefinition,
                    NativeShadowAttributeDefinitionImpl.forSimulatedAssociation(
                            simulationDefinition.getLocalSubjectItemName(),
                            simulationDefinition.getQName(),
                            ShadowReferenceParticipantRole.SUBJECT),
                    toExistingImmutable(assocDefBean));
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "TEMPORARY");
        }
    }

    private static ResourceItemDefinitionType toExistingImmutable(@Nullable ResourceItemDefinitionType customizationBean) {
        return CloneUtil.toImmutable(Objects.requireNonNullElseGet(customizationBean, ResourceItemDefinitionType::new));
    }

    /** TODO inspect calls to this method; take specific embedded shadow into account (if possible)! */
    public @NotNull ResourceObjectDefinition getRepresentativeTargetObjectDefinition() {
        return referenceTypeDefinition.getRepresentativeObjectDefinition();
    }

    public boolean isEntitlement() {
        return referenceTypeDefinition.isEntitlement();
    }

    @Override
    public @NotNull ShadowReferenceAttributeDefinitionImpl forLayer(@NotNull LayerType layer) {
        if (layer == currentLayer) {
            return this;
        } else {
            return new ShadowReferenceAttributeDefinitionImpl(
                    layer,
                    nativeDefinition,
                    customizationBean,
                    limitationsMap,
                    accessOverride.clone(), // TODO do we want to preserve also the access override?
                    referenceTypeDefinition,
                    maxOccurs);
        }
    }

    /**
     * We assume that the checks during the definition parsing were good enough to discover any problems
     * related to broken configuration.
     */
    private static SystemException alreadyChecked(ConfigurationException e) {
        return SystemException.unexpected(e, "(object was already checked)");
    }

    @Override
    public @NotNull QName getTypeName() {
        return ShadowAssociationValueType.COMPLEX_TYPE;
    }

    private @NotNull ComplexTypeDefinition createComplexTypeDefinition() {
        var genericDefinition = stateNonNull(
                PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(ShadowAssociationValueType.COMPLEX_TYPE),
                "No definition for %s", ShadowAssociationValueType.COMPLEX_TYPE);

        ComplexTypeDefinition def = genericDefinition.clone();

        // We apply the prism shadow definition for (representative) target object to the shadowRef definition.
        var shadowRefDef = Objects.requireNonNull(def.findReferenceDefinition(ShadowAssociationValueType.F_SHADOW_REF)).clone();
        shadowRefDef.mutator().setTargetObjectDefinition(
                getRepresentativeTargetObjectDefinition().getPrismObjectDefinition());
        def.mutator().replaceDefinition(
                ShadowAssociationValueType.F_SHADOW_REF,
                shadowRefDef);
        def.mutator().setRuntimeSchema(true);

        def.mutator().setValueMigrator(new ComplexTypeDefinition.ValueMigrator() {
            @Override
            public @NotNull <C extends Containerable> PrismContainerValue<C> migrateIfNeeded(@NotNull PrismContainerValue<C> value) {
                if (value instanceof ShadowAssociationValue) {
                    return value;
                } else {
                    //noinspection unchecked
                    return (PrismContainerValue<C>) ShadowAssociationValue.of(
                            (ShadowAssociationValueType) value.asContainerable(),
                            ShadowReferenceAttributeDefinitionImpl.this);
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
            return nativeDefinition.getMaxOccurs();
        }
    }

    @Override
    public void setMinOccurs(int value) {
    }

    public void setMaxOccurs(int value) {
        checkMutable();
        maxOccurs = value;
    }

    public ShadowAssociationDefinitionType getAssociationDefinitionBean() {
        return associationDefinitionBean;
    }

    public void setAssociationDefinitionBean(ShadowAssociationDefinitionType associationDefinitionBean) {
        checkMutable();
        this.associationDefinitionBean = associationDefinitionBean;
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
            if (clazz.isAssignableFrom(ShadowReferenceAttributeDefinitionImpl.class)) {
                //noinspection unchecked
                return (ID) this;
            } else {
                return null;
            }
        }
        return complexTypeDefinition.findItemDefinition(path, clazz);
    }

    @Override
    ShadowReferenceAttribute instantiateFromQualifiedName(QName name) {
        return new ShadowReferenceAttribute(name, this);
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

    public @NotNull ShadowReferenceAttributeDefinitionImpl clone() {
        return new ShadowReferenceAttributeDefinitionImpl(
                currentLayer,
                nativeDefinition,
                customizationBean,
                limitationsMap,
                accessOverride.clone(), // TODO do we want to preserve also the access override?
                referenceTypeDefinition,
                maxOccurs);
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
        return ShadowAssociationValue.empty();
    }

    @Override
    public ShadowAssociationValue instantiateFromIdentifierRealValue(@NotNull QName identifierName, @NotNull Object realValue)
            throws SchemaException {
        ResourceObjectDefinition targetObjectDefinition = getRepresentativeTargetObjectDefinition();
        var blankShadow = targetObjectDefinition.createBlankShadow();
        blankShadow.getAttributesContainer().add(
                targetObjectDefinition.instantiateAttribute(identifierName, realValue));
        return ShadowAssociationValue.of(blankShadow, true);
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
    public ShadowReferenceAttributeDefinitionImpl deepClone(@NotNull DeepCloneOperation operation) {
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

    @Override
    protected void extendToString(StringBuilder sb) {
    }

    @Override
    public String getDebugDumpClassName() {
        return "SRefAttrDef";
    }

    @Override
    public String debugDump(int indent) {
        return super.debugDump(indent, (LayerType) null);
//        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass() , indent);
//        DebugUtil.debugDumpWithLabelLn(sb, "item name", getItemName(), indent + 1);
//        DebugUtil.debugDumpWithLabelLn(sb, "type definition", associationClassDefinition, indent + 1);
//        // TODO
//        //DebugUtil.debugDumpWithLabel(sb, "config item", configItem, indent + 1);
//        return sb.toString();
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

    // FIXME fix this method
    public @NotNull ObjectFilter createTargetObjectsFilter() {
        var resourceOid = stateNonNull(getRepresentativeTargetObjectDefinition().getResourceOid(), "No resource OID in %s", this);
        var targetParticipantTypes = getTargetParticipantTypes();
        assertCheck(!targetParticipantTypes.isEmpty(), "No object type definitions (already checked)");
        var firstObjectType = targetParticipantTypes.iterator().next().getTypeIdentification();
        if (targetParticipantTypes.size() > 1 || firstObjectType == null) {
            var objectClassNames = targetParticipantTypes.stream()
                    .map(def -> def.getObjectDefinition().getObjectClassName())
                    .collect(Collectors.toSet());
            var objectClassName = MiscUtil.extractSingletonRequired(
                    objectClassNames,
                    () -> new UnsupportedOperationException("Multiple object class names in " + this),
                    () -> new IllegalStateException("No object class names in " + this));
            return PrismContext.get().queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref(resourceOid, ResourceType.COMPLEX_TYPE)
                    .and().item(ShadowType.F_OBJECT_CLASS).eq(objectClassName)
                    .buildFilter();
        } else {
            return PrismContext.get().queryFor(ShadowType.class)
                    .item(ShadowType.F_RESOURCE_REF).ref(resourceOid, ResourceType.COMPLEX_TYPE)
                    .and().item(ShadowType.F_KIND).eq(firstObjectType.getKind())
                    .and().item(ShadowType.F_INTENT).eq(firstObjectType.getIntent())
                    .buildFilter();
        }
    }

    public @Nullable SimulatedShadowReferenceTypeDefinition getSimulationDefinition() {
        return referenceTypeDefinition.getSimulationDefinition();
    }

    public boolean isSimulated() {
        return getSimulationDefinition() != null;
    }

    public SimulatedShadowReferenceTypeDefinition getSimulationDefinitionRequired() {
        assert isSimulated();
        return Objects.requireNonNull(getSimulationDefinition());
    }

    public boolean isRaw() {
        return customizationBean.asPrismContainerValue().hasNoItems();
//        return configItem instanceof ShadowAssociationTypeDefinitionConfigItem ci
//                && ci.value().asPrismContainerValue().isEmpty();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(this); // TODO
    }

    @Override
    public @NotNull Class<ShadowAssociationValueType> getTypeClass() {
        return ShadowAssociationValueType.class;
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
        if (!(o instanceof ShadowReferenceAttributeDefinitionImpl that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(referenceTypeDefinition, that.referenceTypeDefinition)
                && Objects.equals(complexTypeDefinition, that.complexTypeDefinition)
                && Objects.equals(maxOccurs, that.maxOccurs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), referenceTypeDefinition, complexTypeDefinition, maxOccurs);
    }

    @Override
    public ItemCorrelatorDefinitionType getCorrelatorDefinition() {
        return null; // Association cannot be used as a correlator - for now
    }

    // FIXME remove this eventually (after GUI stops setting maxOccurs on this definition)

    @Override
    public void setCompileTimeClass(Class<ShadowAssociationValueType> compileTimeClass) {
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
    }

    @Override
    public void setProcessing(ItemProcessing processing) {
    }

    @Override
    public void setValueEnumerationRef(PrismReferenceValue valueEnumerationRef) {
    }

    @Override
    public void setOperational(boolean operational) {
    }

    @Override
    public void setAlwaysUseForEquals(boolean alwaysUseForEquals) {
    }

    @Override
    public void setDynamic(boolean value) {
    }

    @Override
    public void setReadOnly() {
    }

    @Override
    public void setDeprecatedSince(String value) {
    }

    @Override
    public void addSchemaMigration(SchemaMigration value) {
    }

    @Override
    public void setSchemaMigrations(List<SchemaMigration> value) {
    }

    @Override
    public void setDeprecated(boolean deprecated) {
    }

    @Override
    public void setRemoved(boolean removed) {
    }

    @Override
    public void setRemovedSince(String removedSince) {
    }

    @Override
    public void setExperimental(boolean experimental) {
    }

    @Override
    public void setPlannedRemoval(String value) {
    }

    @Override
    public void setElaborate(boolean value) {
    }

    @Override
    public void setHeterogeneousListItem(boolean value) {
    }

    @Override
    public void setSubstitutionHead(QName value) {
    }

    @Override
    public void setIndexed(Boolean indexed) {
    }

    @Override
    public void setIndexOnly(boolean value) {
    }

    @Override
    public void setInherited(boolean value) {
    }

    @Override
    public void setSearchable(boolean value) {
    }

    @Override
    public void setOptionalCleanup(boolean optionalCleanup) {
    }

    @Override
    public void setRuntimeSchema(boolean value) {
    }

    @Override
    public void setMergerIdentifier(String value) {
    }

    @Override
    public void setNaturalKeyConstituents(List<QName> naturalKeyConstituents) {
    }

    @Override
    public void setCanRead(boolean val) {
    }

    @Override
    public void setCanModify(boolean val) {
    }

    @Override
    public void setCanAdd(boolean val) {
    }

    @Override
    public void setDisplayHint(DisplayHint displayHint) {
    }

    @Override
    public void setEmphasized(boolean emphasized) {
    }

    @Override
    public void setDisplayName(String displayName) {
    }

    @Override
    public void setDisplayOrder(Integer displayOrder) {
    }

    @Override
    public void setHelp(String help) {
    }

    @Override
    public void setDocumentation(String documentation) {
    }

    @Override
    public void setDiagrams(List<ItemDiagramSpecification> value) {
    }

    @Override
    public @NotNull Collection<AssociationParticipantType> getTargetParticipantTypes() {
        // TODO use additional information from the association type definition, if there's any
        return referenceTypeDefinition.getObjectTypes();
    }

    @Override
    public @NotNull Collection<ResourceObjectInboundDefinition> getRelevantInboundDefinitions() {
        var inboundsFromRegularDefinition = super.getRelevantInboundDefinitions();
        if (associationDefinitionBean == null) {
            return inboundsFromRegularDefinition;
        } else {
            var rv = new ArrayList<>(inboundsFromRegularDefinition);
            rv.add(ResourceObjectInboundDefinition.forAssociation(associationDefinitionBean));
            return rv;
        }
    }
}
