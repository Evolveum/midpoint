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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AssociationsCapabilityType;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.impl.delta.ContainerDeltaImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
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
 * Note that unlike the attributes, here the {@link ShadowItemDefinitionImpl#nativeDefinition} may be generated artificially
 * based on simulated {@link AssociationsCapabilityType} definition.
 *
 * TODO various options for configuring associations
 *
 * TODO Effectively immutable? (if constituent definitions are immutable), except for the ability of
 *  changing the {@link #maxOccurs} value. - is this still true?
 */
public class ShadowAssociationDefinitionImpl
        extends ShadowItemDefinitionImpl<ShadowAssociation, ShadowAssociationValueType, NativeShadowAssociationDefinition, ResourceObjectAssociationType>
        implements ShadowAssociationDefinition {

    @Serial private static final long serialVersionUID = 1L;

    /** Participant-independent definition of the association. */
    @NotNull private final ShadowAssociationClassDefinition associationClassDefinition;

    /** This is the definition of a "logical association" that is used in the schema. */
    @NotNull private final ShadowAssociationTypeDefinitionNew associationTypeDefinition;

    /** Refined definition for {@link ShadowAssociationValueType} values that are stored in the {@link ShadowAssociation} item. */
    @NotNull private final ComplexTypeDefinition complexTypeDefinition;

    /** TEMPORARY: Mutable because of GUI! */
    private Integer maxOccurs;

    private ShadowAssociationDefinitionImpl(
            @NotNull ShadowAssociationClassDefinition associationClassDefinition,
            @NotNull ShadowAssociationTypeDefinitionNew associationTypeDefinition,
            @NotNull NativeShadowAssociationDefinition nativeDefinition,
            @NotNull ResourceObjectAssociationType configurationBean) throws SchemaException {
        super(nativeDefinition, configurationBean, false);
        this.associationClassDefinition = associationClassDefinition;
        this.associationTypeDefinition = associationTypeDefinition;
        this.complexTypeDefinition = createComplexTypeDefinition();
    }

    private ShadowAssociationDefinitionImpl(
            @NotNull LayerType layer,
            @NotNull NativeShadowAssociationDefinition nativeDefinition,
            @NotNull ResourceObjectAssociationType customizationBean,
            @NotNull Map<LayerType, PropertyLimitations> limitationsMap,
            @NotNull PropertyAccessType accessOverride,
            @NotNull ShadowAssociationClassDefinition associationClassDefinition,
            @NotNull ShadowAssociationTypeDefinitionNew associationTypeDefinition,
            Integer maxOccurs) {
        super(layer, nativeDefinition, customizationBean, limitationsMap, accessOverride);
        this.associationClassDefinition = associationClassDefinition;
        this.associationTypeDefinition = associationTypeDefinition;
        this.complexTypeDefinition = getComplexTypeDefinition();
        this.maxOccurs = maxOccurs;
    }

    static ShadowAssociationDefinitionImpl parseLegacy(
            @NotNull ShadowAssociationClassDefinition associationClassDefinition,
            @NotNull ResourceObjectAssociationConfigItem definitionCI) throws ConfigurationException {
        try {
            return new ShadowAssociationDefinitionImpl(
                    associationClassDefinition,
                    ShadowAssociationTypeDefinitionNew.empty(),
                    NativeShadowItemDefinitionImpl.forSimulatedAssociation(
                            definitionCI.getAssociationName(),
                            associationClassDefinition.getClassName(),
                            ShadowAssociationParticipantRole.SUBJECT),
                    definitionCI.value());
        } catch (SchemaException e) {
            throw new ConfigurationException(e);
        }
    }

    static ShadowAssociationDefinitionImpl fromNative(
            @NotNull NativeShadowAssociationDefinition rawDefinition,
            @NotNull ShadowAssociationClassDefinition associationClassDefinition,
            @NotNull ShadowAssociationTypeDefinitionNew associationTypeDefinitionNew,
            @Nullable ResourceObjectAssociationType customizationBean) {
        try {
            return new ShadowAssociationDefinitionImpl(
                    associationClassDefinition,
                    associationTypeDefinitionNew,
                    rawDefinition,
                    toExistingImmutable(customizationBean));
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "TEMPORARY");
        }
    }

    static ItemDefinition<?> fromSimulated(
            @NotNull ShadowAssociationClassSimulationDefinition simulationDefinition,
            @NotNull ShadowAssociationClassDefinition associationTypeDefinition,
            @NotNull ShadowAssociationTypeDefinitionNew associationTypeDefinitionNew,
            @Nullable ResourceObjectAssociationType assocDefBean) {
        try {
            return new ShadowAssociationDefinitionImpl(
                    associationTypeDefinition,
                    associationTypeDefinitionNew,
                    NativeShadowItemDefinitionImpl.forSimulatedAssociation(
                            simulationDefinition.getLocalSubjectItemName(), simulationDefinition.getQName(), ShadowAssociationParticipantRole.SUBJECT),
                    toExistingImmutable(assocDefBean));
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "TEMPORARY");
        }
    }

    private static ResourceObjectAssociationType toExistingImmutable(@Nullable ResourceObjectAssociationType customizationBean) {
        return CloneUtil.toImmutable(Objects.requireNonNullElseGet(customizationBean, ResourceObjectAssociationType::new));
    }

    public @NotNull ShadowAssociationClassDefinition getAssociationClassDefinition() {
        return associationClassDefinition;
    }

    public @NotNull ShadowAssociationTypeDefinitionNew getAssociationTypeDefinition() {
        return associationTypeDefinition;
    }

    public @NotNull ResourceObjectDefinition getTargetObjectDefinition() {
        return getAssociationClassDefinition().getRepresentativeObjectDefinition();
    }

    public boolean isEntitlement() {
        return getAssociationClassDefinition().isEntitlement();
    }

    @Override
    public @NotNull ShadowAssociationDefinitionImpl forLayer(@NotNull LayerType layer) {
        if (layer == currentLayer) {
            return this;
        } else {
            return new ShadowAssociationDefinitionImpl(
                    layer,
                    nativeDefinition,
                    customizationBean,
                    limitationsMap,
                    accessOverride.clone(), // TODO do we want to preserve also the access override?
                    associationClassDefinition,
                    associationTypeDefinition,
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

    private @NotNull ComplexTypeDefinition createComplexTypeDefinition() {
        var genericDefinition = stateNonNull(
                PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(ShadowAssociationValueType.COMPLEX_TYPE),
                "No definition for %s", ShadowAssociationValueType.COMPLEX_TYPE);

        ComplexTypeDefinition def = genericDefinition.clone();

        // We apply the prism shadow definition for (representative) target object to the shadowRef definition.
        var shadowRefDef = Objects.requireNonNull(def.findReferenceDefinition(ShadowAssociationValueType.F_SHADOW_REF)).clone();
        shadowRefDef.mutator().setTargetObjectDefinition(
                getTargetObjectDefinition().getPrismObjectDefinition());
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
                    ShadowAssociationValueType bean = (ShadowAssociationValueType) value.asContainerable();
                    var shadowRef = bean.getShadowRef();
                    if (shadowRef != null) {
                        var shadow = (ShadowType) shadowRef.getObjectable();
                        if (shadow != null) {
                            try {
                                new ShadowDefinitionApplicator(getTargetObjectDefinition())
                                        .applyTo(shadow);
                            } catch (SchemaException e) {
                                throw SystemException.unexpected(e); // FIXME
                            }
                        }
                    }
                    //noinspection unchecked
                    return (PrismContainerValue<C>) ShadowAssociationValue.of(bean);
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

    public void setMaxOccurs(int value) {
        checkMutable();
        maxOccurs = value;
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
    ShadowAssociation instantiateFromQualifiedName(QName name) {
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
    public @NotNull ShadowAssociationDefinitionImpl clone() {
        return new ShadowAssociationDefinitionImpl(
                currentLayer,
                nativeDefinition,
                customizationBean,
                limitationsMap,
                accessOverride.clone(), // TODO do we want to preserve also the access override?
                associationClassDefinition,
                associationTypeDefinition,
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
        ResourceObjectDefinition targetObjectDefinition = getTargetObjectDefinition();
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
    public @NotNull PrismContainerDefinition.PrismContainerDefinitionMutator<ShadowAssociationValueType> mutator() {
        throw new UnsupportedOperationException(); // FIXME ... what about GUI?
//        checkMutableOnExposing();
//        return this;
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

    @Override
    protected void extendToString(StringBuilder sb) {
    }

    @Override
    public String getDebugDumpClassName() {
        return "SAssocD";
    }

    @Override
    public String debugDump(int indent) {
        return super.debugDump(indent, null);
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

    public @NotNull ObjectFilter createTargetObjectsFilter() {
        var objectDefinitions = getAssociationClassDefinition().getObjectObjectDefinitions();
        assertCheck(!objectDefinitions.isEmpty(), "No object type definitions (already checked)");
        S_FilterEntryOrEmpty atomicFilter = PrismContext.get().queryFor(ShadowType.class);
        List<ObjectFilter> orFilterClauses = new ArrayList<>();
        objectDefinitions.stream()
                .map(def -> def.getTypeIdentification())
                .forEach(typeId -> orFilterClauses.add(
                        atomicFilter
                                .item(ShadowType.F_KIND).eq(typeId.getKind()) // FIXME treat also class definitions
                                .and().item(ShadowType.F_INTENT).eq(typeId.getIntent())
                                .buildFilter()));
        OrFilter intentFilter = PrismContext.get().queryFactory().createOr(orFilterClauses);

        var resourceOid = stateNonNull(getTargetObjectDefinition().getResourceOid(), "No resource OID in %s", this);
        return atomicFilter.item(ShadowType.F_RESOURCE_REF).ref(resourceOid, ResourceType.COMPLEX_TYPE)
                .and().filter(intentFilter)
                .buildFilter();
    }

    public @Nullable ShadowAssociationClassSimulationDefinition getSimulationDefinition() {
        return getAssociationClassDefinition().getSimulationDefinition();
    }

    public boolean isSimulated() {
        return getSimulationDefinition() != null;
    }

    public ShadowAssociationClassSimulationDefinition getSimulationDefinitionRequired() {
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
        if (!(o instanceof ShadowAssociationDefinitionImpl that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(associationClassDefinition, that.associationClassDefinition)
                && Objects.equals(complexTypeDefinition, that.complexTypeDefinition)
                && Objects.equals(maxOccurs, that.maxOccurs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), associationClassDefinition, complexTypeDefinition, maxOccurs);
    }

    @Override
    public ItemCorrelatorDefinitionType getCorrelatorDefinition() {
        return null; // Association cannot be used as a correlator - for now
    }
}
