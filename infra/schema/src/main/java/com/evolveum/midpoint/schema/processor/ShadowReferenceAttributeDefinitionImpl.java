/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.types_3.RawType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.impl.delta.ReferenceDeltaImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReferencesCapabilityType;

/**
 * Definition of a shadow association item, e.g., `ri:group`.
 *
 * Note that unlike the attributes, here the {@link ShadowAttributeDefinitionImpl#nativeDefinition} may be generated artificially
 * based on simulated {@link ReferencesCapabilityType} definition.
 *
 * TODO various options for configuring associations
 */
public class ShadowReferenceAttributeDefinitionImpl
        extends ShadowAttributeDefinitionImpl<
        ShadowReferenceAttributeValue,
        ShadowReferenceAttributeDefinition,
        Referencable,
        ShadowReferenceAttribute,
        NativeShadowReferenceAttributeDefinition>
        implements
        ShadowReferenceAttributeDefinition,
        PrismReferenceDefinition.PrismReferenceDefinitionMutator {

    @Serial private static final long serialVersionUID = 1L;

    /** Participant-independent definition of the reference type. */
    @NotNull private final AbstractShadowReferenceTypeDefinition referenceTypeDefinition;

    private ShadowReferenceAttributeDefinitionImpl(
            @NotNull AbstractShadowReferenceTypeDefinition typeDefinition,
            @NotNull NativeShadowReferenceAttributeDefinition nativeDefinition,
            @NotNull ResourceItemDefinitionType configurationBean) throws SchemaException {
        super(nativeDefinition, configurationBean, false);
        this.referenceTypeDefinition = typeDefinition;
    }

    private ShadowReferenceAttributeDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull NativeShadowReferenceAttributeDefinition nativeDefinition,
            @NotNull ResourceItemDefinitionType customizationBean,
            @NotNull Map<LayerType, PropertyLimitations> limitationsMap,
            @NotNull PropertyAccessType accessOverride,
            @NotNull AbstractShadowReferenceTypeDefinition referenceTypeDefinition) {
        super(layer, nativeDefinition, customizationBean, limitationsMap, accessOverride);
        this.referenceTypeDefinition = referenceTypeDefinition;
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

    /** Both modern and legacy */
    static ShadowReferenceAttributeDefinition fromSimulated(
            @NotNull SimulatedShadowReferenceTypeDefinition simulatedReferenceTypeDefinition,
            @Nullable ResourceItemDefinitionType assocDefBean) {
        try {
            return new ShadowReferenceAttributeDefinitionImpl(
                    simulatedReferenceTypeDefinition,
                    NativeShadowAttributeDefinitionImpl.forSimulatedAssociation(
                            simulatedReferenceTypeDefinition.getLocalSubjectItemName(),
                            simulatedReferenceTypeDefinition.getQName(),
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
                    referenceTypeDefinition);
        }
    }

    @Override
    public @NotNull QName getTypeName() {
        return ObjectReferenceType.COMPLEX_TYPE;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void setMinOccurs(int value) {
    }

    @Override
    public void setMaxOccurs(int value) {
    }

    @Override
    public boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition<?>> clazz, boolean caseInsensitive) {
        Preconditions.checkArgument(!caseInsensitive, "Case-insensitive search is not supported");
        return QNameUtil.match(elementQName, getItemName())
                && clazz.isInstance(this);
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        //noinspection unchecked
        return LivePrismItemDefinition.matchesThisDefinition(path, clazz, this) ? (ID) this : null;
    }

    @Override
    ShadowReferenceAttribute instantiateFromQualifiedName(QName name) {
        return new ShadowReferenceAttribute(name, this);
    }

    @Override
    public @NotNull ReferenceDelta createEmptyDelta(ItemPath path) {
        return new ReferenceDeltaImpl(path, this);
    }

    @Override
    public QName getTargetTypeName() {
        return ShadowType.COMPLEX_TYPE;
    }

    @Override
    public boolean isComposite() {
        return false;
    }

    public @NotNull ShadowReferenceAttributeDefinitionImpl clone() {
        return new ShadowReferenceAttributeDefinitionImpl(
                currentLayer,
                nativeDefinition,
                customizationBean,
                limitationsMap,
                accessOverride.clone(), // TODO do we want to preserve also the access override?
                referenceTypeDefinition);
    }

    @Override
    public @NotNull ShadowReferenceAttributeDefinition cloneWithNewCardinality(int newMinOccurs, int newMaxOccurs) {
        var newNativeDef = (NativeShadowReferenceAttributeDefinition)
                nativeDefinition.cloneWithNewCardinality(newMinOccurs, newMaxOccurs);
        newNativeDef.freeze();
        Map<LayerType, PropertyLimitations> newLimitationsMap =
                limitationsMap.entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey(),
                                e -> e.getValue().cloneWithNewCardinality(newMinOccurs, newMaxOccurs)));
        return new ShadowReferenceAttributeDefinitionImpl(
                currentLayer,
                newNativeDef,
                customizationBean,
                newLimitationsMap,
                accessOverride.clone(), // TODO do we want to preserve also the access override?
                referenceTypeDefinition);
    }

    @Override
    public ShadowReferenceAttributeValue instantiateFromIdentifierRealValue(
            @NotNull QName identifierName, @NotNull Object realValue)
            throws SchemaException {
        ResourceObjectDefinition targetObjectDefinition = getRepresentativeTargetObjectDefinition();
        var blankShadow = targetObjectDefinition.createBlankShadow();
        blankShadow.getAttributesContainer().add(
                (ShadowAttribute<?, ?, ?, ?>)
                        targetObjectDefinition.instantiateAttribute(identifierName, realValue));
        blankShadow.setIdentificationOnly();
        return ShadowReferenceAttributeValue.fromShadow(blankShadow);
    }

    @Override
    public PrismReferenceDefinitionMutator mutator() {
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

    public ReferenceDelta createEmptyDelta() {
        return PrismContext.get().deltaFactory().reference().create(
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
    public @NotNull Class<Referencable> getTypeClass() {
        return Referencable.class;
    }

    @Override
    public ShadowReferenceAttributeValue createPrismValueFromRealValue(@NotNull Object realValue) throws SchemaException {
        if (realValue instanceof RawType raw) {
            return ShadowReferenceAttributeValue.fromReferencable(
                    raw.getParsedRealValue(ObjectReferenceType.class));
        } else if (realValue instanceof Referencable referencable) {
            return ShadowReferenceAttributeValue.fromReferencable(referencable);
        } else {
            throw new SchemaException("Couldn't instantiate reference attribute from " + realValue.getClass());
        }
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return null;
    }

    @Override
    public @NotNull ItemDefinition<PrismReference> cloneWithNewName(@NotNull ItemName itemName) {
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
        return Objects.equals(referenceTypeDefinition, that.referenceTypeDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), referenceTypeDefinition);
    }

    @Override
    public ItemCorrelatorDefinitionType getCorrelatorDefinition() {
        return null; // Association cannot be used as a correlator - for now
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
    public void setSchemaContextDefinition(SchemaContextDefinition schemaContextDefinition) {
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
    public @NotNull Collection<ShadowRelationParticipantType> getTargetParticipantTypes() {
        return referenceTypeDefinition.getObjectTypes();
    }

    @Override
    public void setTargetTypeName(QName typeName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTargetObjectDefinition(PrismObjectDefinition<?> definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setComposite(boolean value) {
        throw new UnsupportedOperationException();
    }

    /**
     * See {@link ShadowAssociationDefinitionImpl#createComplexTypeDefinition()} for analogous functionality
     * (via value migrator).
     */
    @Override
    public @NotNull PrismReferenceValue migrateIfNeeded(@NotNull PrismReferenceValue value) throws SchemaException {
        return ShadowReferenceAttributeValue.fromRefValue(value);
    }

    @Override
    public @NotNull ShadowReferenceParticipantRole getParticipantRole() {
        return nativeDefinition.getReferenceParticipantRole();
    }
}
