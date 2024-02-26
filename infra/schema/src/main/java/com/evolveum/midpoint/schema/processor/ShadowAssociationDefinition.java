/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.impl.PrismContainerValueImpl;
import com.evolveum.midpoint.prism.impl.delta.ContainerDeltaImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

/** Effectively immutable (if constituent definitions are immutable). */
public class ShadowAssociationDefinition extends AbstractFreezable
        implements Serializable, Visitable<Definition>, Freezable, DebugDumpable,
        PrismContainerDefinition<ShadowAssociationValueType> {

    @Serial private static final long serialVersionUID = 1L;

    @NotNull private final ResourceObjectAssociationType definitionBean;

    @NotNull private final QName associationAttributeName;

    @NotNull private final QName valueAttributeName;

    @NotNull private final List<String> intents;

    @NotNull private final ResourceObjectTypeDefinition associationTarget;

    @NotNull private final ComplexTypeDefinition complexTypeDefinition;

    public ShadowAssociationDefinition(
            @NotNull ResourceObjectAssociationType definitionBean,
            @NotNull ResourceObjectTypeDefinition associationTarget,
            @NotNull Object errorCtx) throws ConfigurationException {
        this.definitionBean = definitionBean;
        this.associationTarget = associationTarget;
        this.complexTypeDefinition = createComplexTypeDefinition();
        this.associationAttributeName = MiscUtil.configNonNull(
                definitionBean.getAssociationAttribute(),
                () -> "No association attribute defined in entitlement association: " + this + " in " + errorCtx);
        this.valueAttributeName = MiscUtil.configNonNull(
                definitionBean.getValueAttribute(),
                () -> "No value attribute defined in entitlement association: " + this + " in " + errorCtx);
        this.intents = List.copyOf(definitionBean.getIntent());
        configCheck(!intents.isEmpty(), "No intent(s) defined for %s in %s", this, errorCtx);
    }

    public @NotNull ResourceObjectAssociationType getDefinitionBean() {
        return definitionBean;
    }

    public @NotNull ResourceObjectTypeDefinition getAssociationTarget() {
        return associationTarget;
    }

    public @NotNull ItemName getName() {
        return ItemPathTypeUtil.asSingleNameOrFail(definitionBean.getRef());
    }

    public @NotNull ShadowKindType getKind() {
        return getKind(definitionBean);
    }

    public static @NotNull ShadowKindType getKind(@NotNull ResourceObjectAssociationType definitionBean) {
        return Objects.requireNonNullElse(
                definitionBean.getKind(), ShadowKindType.ENTITLEMENT);
    }

    public @NotNull ResourceObjectAssociationDirectionType getDirection() {
        return Objects.requireNonNull(
                definitionBean.getDirection(),
                () -> "No association direction provided in association definition: " + this);
    }

    public boolean isObjectToSubject() {
        return getDirection() == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT;
    }

    public boolean isSubjectToObject() {
        return getDirection() == ResourceObjectAssociationDirectionType.SUBJECT_TO_OBJECT;
    }

    /** There is at least one. */
    public @NotNull Collection<String> getIntents() {
        return intents;
    }

    /** We rely on the assumptions about multiple intents described for {@link ResourceObjectAssociationType#getIntent()}. */
    public @NotNull String getAnyIntent() {
        return intents.iterator().next();
    }

    public QName getAuxiliaryObjectClass() {
        return definitionBean.getAuxiliaryObjectClass();
    }

    public MappingType getOutboundMappingType() {
        return definitionBean.getOutbound();
    }

    public List<InboundMappingType> getInboundMappingBeans() {
        return definitionBean.getInbound();
    }

    public boolean isExclusiveStrong() {
        return BooleanUtils.isTrue(definitionBean.isExclusiveStrong());
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
        return false;
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
        return Boolean.TRUE.equals(definitionBean.isEmphasized());
    }

    @Override
    public DisplayHint getDisplayHint() {
        return MiscSchemaUtil.toDisplayHint(definitionBean.getDisplayHint());
    }

    public boolean isIgnored(LayerType layer) throws SchemaException {
        return getTargetAssociationAttributeDefinition().isIgnored(layer);
    }

    public PropertyLimitations getLimitations(LayerType layer) throws SchemaException {
        return getTargetAssociationAttributeDefinition().getLimitations(layer);
    }

    private @NotNull ResourceAttributeDefinition<Object> getTargetAssociationAttributeDefinition() throws SchemaException {
        return associationTarget.findAttributeDefinitionRequired(
                getTargetAssociationAttribute(),
                () -> " as defined for association " + this);
    }

    private QName getTargetAssociationAttribute() {
        if (isObjectToSubject()) {
            return associationAttributeName; // e.g., "ri:member" on a group (for object-to-subject)
        } else {
            return valueAttributeName; // e.g., "icfs:name" on a privilege (for subject-to-object)
        }
    }

    public @NotNull QName getAssociationAttributeName() {
        return associationAttributeName;
    }

    public @NotNull QName getValueAttributeName() {
        return valueAttributeName;
    }

    public @Nullable QName getShortcutAssociationAttributeName() {
        return definitionBean.getShortcutAssociationAttribute();
    }

    public @Nullable QName getShortcutValueAttributeName() {
        return definitionBean.getShortcutValueAttribute();
    }

    public @NotNull QName getShortcutValueAttributeNameRequired() throws ConfigurationException {
        return MiscUtil.configNonNull(
                getShortcutValueAttributeName(),
                "Shortcut value attribute name must be present if there's shortcut association attribute in %s",
                this);
    }

    public boolean isTolerant() {
        return BooleanUtils.isNotFalse(definitionBean.isTolerant());
    }

    @NotNull
    public List<String> getTolerantValuePattern() {
        return definitionBean.getTolerantValuePattern();
    }

    @NotNull
    public List<String> getIntolerantValuePattern() {
        return definitionBean.getIntolerantValuePattern();
    }

    public boolean requiresExplicitReferentialIntegrity() {
        return !BooleanUtils.isFalse(getDefinitionBean().isExplicitReferentialIntegrity()); // because default is TRUE
    }

    public QName getMatchingRule() {
        return getDefinitionBean().getMatchingRule();
    }

    public String getDisplayName() {
        return definitionBean.getDisplayName();
    }

    @Override
    public Integer getDisplayOrder() {
        return definitionBean.getDisplayOrder();
    }

    @Override
    public String getHelp() {
        return definitionBean.getHelp();
    }

    @Override
    public String getDocumentation() {
        return definitionBean.getDocumentation();
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
        return definitionBean.getLifecycleState();
    }

    private @NotNull ComplexTypeDefinition createComplexTypeDefinition() {
        var rawDef = MiscUtil.stateNonNull(
                PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(ShadowAssociationValueType.COMPLEX_TYPE),
                "No definition for %s", ShadowAssociationValueType.COMPLEX_TYPE);

        MutableComplexTypeDefinition def = rawDef.clone().toMutable();
        // TODO optimize this by keeping only "important" definitions (e.g. the ones that are actually used by the association)
        def.replaceDefinition(
                ShadowAssociationValueType.F_IDENTIFIERS,
                associationTarget.toResourceAttributeContainerDefinition(ShadowAssociationValueType.F_IDENTIFIERS));
        def.freeze();
        return def;
    }

    public boolean isVisible(@NotNull TaskExecutionMode taskExecutionMode) {
        return SimulationUtil.isVisible(getLifecycleState(), taskExecutionMode);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public @NotNull ItemName getItemName() {
        return getName();
    }

    @Override
    public int getMinOccurs() {
        return 0;
    }

    @Override
    public int getMaxOccurs() {
        return -1;
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
        return QNameUtil.match(elementQName, getName())
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
        return instantiate(getName());
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
    public ShadowAssociationDefinition clone() {
        return this;
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
    public MutablePrismContainerDefinition<ShadowAssociationValueType> toMutable() {
        throw new UnsupportedOperationException();
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
        return "ResourceAssociationDefinition{" +
                "ref=" + definitionBean.getRef() +
                ", associationTarget=" + associationTarget +
                "}";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "definition", definitionBean, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "associationTarget", String.valueOf(associationTarget), indent + 1);
        return sb.toString();
    }

    public @NotNull ItemPath getStandardPath() {
        return ItemPath.create(ShadowType.F_ASSOCIATIONS, getName());
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
}
