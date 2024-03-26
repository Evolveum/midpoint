/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.prism.DefinitionFragmentBuilder.fixed;
import static com.evolveum.midpoint.prism.DefinitionFragmentBuilder.unsupported;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.schema.*;

import com.evolveum.midpoint.util.DisplayableValue;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.NativeShadowItemDefinition.NativeShadowItemDefinitionBuilder;
import com.evolveum.midpoint.util.PrettyPrinter;

import org.jetbrains.annotations.Nullable;

/**
 * Represents native attribute or association. They have much in common:
 *
 * . in ConnId, both are represented as ConnId attributes;
 * . also in XSD, both are represented as CTD items.
 *
 * So it is quite practical for them to be represented by a single class.
 *
 * The main reason is that the instantiation in schema XSOM parser would require reading custom (higher-level) annotations
 * before instantiating the target class. So, it is much simpler to keep both in a single class. It's no much harm, as this
 * knowledge is basically hidden from the clients that generally don't see the native form of the schema.
 *
 * TODO should we support processing ("ignored") here?
 */
public class NativeShadowItemDefinitionImpl<T>
        extends AbstractFreezable
        implements
        NativeShadowItemDefinition, NativeShadowItemDefinitionBuilder,
        NativeShadowAttributeDefinition<T>, NativeShadowAttributeDefinition.NativeShadowAttributeDefinitionBuilder<T>,
        NativeShadowAssociationDefinition,
        PrismItemBasicDefinition.Delegable, PrismItemBasicDefinition.Mutable.Delegable,
        PrismItemAccessDefinition.Delegable, PrismItemAccessDefinition.Mutable.Delegable,
        PrismItemMiscDefinition.Delegable, PrismItemMiscDefinition.Mutable.Delegable,
        PrismPresentationDefinition.Delegable, PrismPresentationDefinition.Mutable.Delegable,
        ShadowItemUcfDefinition.Delegable, ShadowItemUcfDefinition.Mutable.Delegable,
        PrismItemValuesDefinition.Delegable<T>, PrismItemValuesDefinition.Mutator.Delegable<T>,
        PrismItemMatchingDefinition.Delegable<T>, PrismItemMatchingDefinition.Mutator.Delegable,
        SerializableItemDefinition, SerializablePropertyDefinition, SerializableContainerDefinition {

    @NotNull private final PrismItemBasicDefinition.Data prismItemBasicData;
    @NotNull private final PrismItemAccessDefinition.Data prismItemAccessData = new PrismItemAccessDefinition.Data();
    @NotNull private final PrismItemMiscDefinition.Data prismItemMiscData = new PrismItemMiscDefinition.Data();
    @NotNull private final PrismPresentationDefinition.Data prismPresentationData = new PrismPresentationDefinition.Data();
    @NotNull private final ShadowItemUcfDefinition.Data ucfData = new ShadowItemUcfDefinition.Data();

    // for attributes
    @NotNull private final PrismItemValuesDefinition.Data<T> prismItemValues = new PrismItemValuesDefinition.Data<>();
    @NotNull private final PrismItemMatchingDefinition.Data<T> prismItemMatching;

    // for associations
    private ShadowAssociationParticipantRole associationParticipantRole;

    NativeShadowItemDefinitionImpl(@NotNull ItemName itemName, @NotNull QName typeName) {
        this.prismItemBasicData = new PrismItemBasicDefinition.Data(itemName, typeName);
        this.prismItemMatching = new PrismItemMatchingDefinition.Data<>(typeName);
    }

    @SuppressWarnings("SameParameterValue") // The participant role is always SUBJECT for simulated associations (as of today)
    static NativeShadowItemDefinitionImpl<?> simulatedAssociation(
            @NotNull ItemName itemName, @NotNull QName typeName, @NotNull ShadowAssociationParticipantRole participantRole) {
        // Simulated associations cannot be connected to raw definitions (for now), so we can create our own definition
        var simulatedNativeDef = new NativeShadowItemDefinitionImpl<>(itemName, typeName);
        simulatedNativeDef.setMinOccurs(0);
        simulatedNativeDef.setMaxOccurs(-1);
        simulatedNativeDef.setAssociationParticipantRole(participantRole);
        simulatedNativeDef.freeze();
        return simulatedNativeDef;
    }

    @Override
    public @NotNull PrismItemBasicDefinition.Data itemBasicDefinition() {
        return prismItemBasicData;
    }

    @Override
    public @NotNull PrismItemAccessDefinition.Data itemAccessDefinition() {
        return prismItemAccessData;
    }

    @Override
    public @NotNull PrismItemMiscDefinition.Data itemMiscDefinition() {
        return prismItemMiscData;
    }

    @Override
    public @NotNull PrismPresentationDefinition.Data prismPresentationDefinition() {
        return prismPresentationData;
    }

    @Override
    public ShadowItemUcfDefinition.Data ucfData() {
        return ucfData;
    }

    @Override
    public PrismItemMatchingDefinition.Data<T> prismItemMatchingDefinition() {
        return prismItemMatching;
    }

    @Override
    public PrismItemValuesDefinition.Data<T> prismItemValuesDefinition() {
        return prismItemValues;
    }

    //region Supported setters

    @Override
    public void setNativeAttributeName(String value) {
        ucfData.setNativeAttributeName(value);
    }

    @Override
    public void setFrameworkAttributeName(String value) {
        ucfData.setFrameworkAttributeName(value);
    }

    @Override
    public void setReturnedByDefault(Boolean value) {
        ucfData.setReturnedByDefault(value);
    }

    @Override
    public void setProcessing(ItemProcessing processing) {
        prismItemMiscData.setProcessing(processing);
    }

    @Override
    public void setReadOnly() {
        setCanRead(true);
        setCanAdd(false);
        setCanModify(false);
    }

    //endregion

    //region Serialization
    @Override
    public Boolean isIndexed() {
        return null;
    }

    @Override
    public QName getMatchingRuleQName() {
        return prismItemMatching.getMatchingRuleQName();
    }

    @Override
    public PrismReferenceValue getValueEnumerationRef() {
        return prismItemValues.getValueEnumerationRef();
    }
    //endregion

    //region Unsupported setters
    @Override
    public void setDiagrams(List<ItemDiagramSpecification> value) {
        unsupported("diagrams", value);
    }

    @Override
    public void setOptionalCleanup(boolean optionalCleanup) {
        unsupported("optionalCleanup", optionalCleanup);
    }

    @Override
    public void setRuntimeSchema(boolean value) {
        fixed("runtimeSchema", value, true);
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        unsupported("annotation", qname);
    }

    @Override
    public void setValueEnumerationRef(PrismReferenceValue valueEnumerationRef) {
        unsupported("valueEnumerationRef", valueEnumerationRef);
    }

    @Override
    public void setOperational(boolean operational) {
        unsupported("operational", operational);
    }

    @Override
    public void setAlwaysUseForEquals(boolean alwaysUseForEquals) {
        unsupported("alwaysUseForEquals", alwaysUseForEquals);
    }

    @Override
    public void setDynamic(boolean value) {
        unsupported("dynamic", value);
    }

    @Override
    public void setDeprecatedSince(String value) {
        unsupported("deprecatedSince", value);
    }

    @Override
    public void setPlannedRemoval(String value) {
        unsupported("plannedRemoval", value);
    }

    @Override
    public void setElaborate(boolean value) {
        unsupported("elaborate", value);
    }

    @Override
    public void setHeterogeneousListItem(boolean value) {
        unsupported("heterogeneousListItem", value);
    }

    @Override
    public void setSubstitutionHead(QName value) {
        unsupported("substitutionHead", value);
    }

    @Override
    public void setIndexOnly(boolean value) {
        unsupported("indexOnly", value);
    }

    @Override
    public void setInherited(boolean value) {
        unsupported("inherited", value);
    }

    @Override
    public void setSearchable(boolean value) {
        unsupported("searchable", value);
    }

    @Override
    public void setIndexed(Boolean indexed) {
        unsupported("indexed", indexed);
    }

    @Override
    public void setDeprecated(boolean deprecated) {
        unsupported("deprecated", deprecated);
    }

    @Override
    public void setRemoved(boolean removed) {
        unsupported("removed", removed);
    }

    @Override
    public void setRemovedSince(String removedSince) {
        unsupported("removedSince", removedSince);
    }

    @Override
    public void setExperimental(boolean experimental) {
        unsupported("experimental", experimental);
    }

    @Override
    public void addSchemaMigration(SchemaMigration value) {
        unsupported("schemaMigration", value);
    }

    @Override
    public void setSchemaMigrations(List<SchemaMigration> value) {
        unsupported("schemaMigrations", value);
    }

    @Override
    public void setDisplayHint(DisplayHint displayHint) {
        unsupported("displayHint", displayHint);
    }

    @Override
    public void setEmphasized(boolean emphasized) {
        unsupported("emphasized", emphasized);
    }

    @Override
    public void setHelp(String help) {
        unsupported("help", help);
    }

    @Override
    public void setDocumentation(String documentation) {
        unsupported("documentation", documentation);
    }
    //endregion

    //region Getters with both abstract and default declarations
    @Override
    public @NotNull ItemName getItemName() {
        return PrismItemBasicDefinition.Delegable.super.getItemName();
    }

    @Override
    public @NotNull QName getTypeName() {
        return PrismItemBasicDefinition.Delegable.super.getTypeName();
    }

    @Override
    public int getMinOccurs() {
        return PrismItemBasicDefinition.Delegable.super.getMinOccurs();
    }

    @Override
    public int getMaxOccurs() {
        return PrismItemBasicDefinition.Delegable.super.getMaxOccurs();
    }

    @Override
    public Integer getDisplayOrder() {
        return PrismPresentationDefinition.Delegable.super.getDisplayOrder();
    }

    @Override
    public boolean isEmphasized() {
        return PrismPresentationDefinition.Delegable.super.isEmphasized();
    }

    @Override
    public String getDisplayName() {
        return PrismPresentationDefinition.Delegable.super.getDisplayName();
    }

    @Override
    public ItemProcessing getProcessing() {
        return PrismItemMiscDefinition.Delegable.super.getProcessing();
    }

    @Override
    public String getHelp() {
        return PrismPresentationDefinition.Delegable.super.getHelp();
    }
    //endregion

    @Override
    public SerializableComplexTypeDefinition getComplexTypeDefinitionToSerialize() {
        return null; // we don't need to serialize CTD of this
    }

    @Override
    public boolean isOperational() {
        return false;
    }

    @Override
    public @NotNull ShadowAssociationParticipantRole getAssociationParticipantRole() {
        return Objects.requireNonNull(associationParticipantRole, "role");
    }

    @Override
    public @Nullable ShadowAssociationParticipantRole getAssociationParticipantRoleIfPresent() {
        return associationParticipantRole;
    }

    @Override
    public void setAssociationParticipantRole(ShadowAssociationParticipantRole associationParticipantRole) {
        this.associationParticipantRole = associationParticipantRole;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(this);
    }

    @Override
    public Collection<DefinitionFeature<?, ?, Object, ?>> getExtraFeaturesToParse() {
        return List.of(
                ResourceDefinitionFeatures.ForItem.DF_NATIVE_ATTRIBUTE_NAME,
                ResourceDefinitionFeatures.ForItem.DF_FRAMEWORK_ATTRIBUTE_NAME,
                ResourceDefinitionFeatures.ForItem.DF_RETURNED_BY_DEFAULT,
                ResourceDefinitionFeatures.ForItem.DF_ASSOCIATION_PARTICIPANT_ROLE);
    }

    @Override
    public Collection<? extends DefinitionFeature<?, ?, ?, ?>> getExtraFeaturesToSerialize() {
        return getExtraFeaturesToParse();
    }

    @Override
    protected void performFreeze() {
        super.performFreeze();
        prismItemBasicData.freeze();
        prismItemAccessData.freeze();
        prismItemMiscData.freeze();
        prismPresentationData.freeze();
        ucfData.freeze();
        prismItemValues.freeze();
        prismItemMatching.freeze();
    }

    @Override
    public Object getObjectBuilt() {
        return this;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public NativeShadowItemDefinitionImpl<T> clone() {
        NativeShadowItemDefinitionImpl<T> clone = new NativeShadowItemDefinitionImpl<>(getItemName(), getTypeName());
        clone.copyFrom(this);
        return clone;
    }

    protected void copyFrom(NativeShadowItemDefinitionImpl<T> source) {
        prismItemBasicData.copyFrom(source.prismItemBasicData);
        prismItemAccessData.copyFrom(source.prismItemAccessData);
        prismItemMiscData.copyFrom(source.prismItemMiscData);
        prismPresentationData.copyFrom(source.prismPresentationData);
        ucfData.copyFrom(source.ucfData);
        prismItemValues.copyFrom(source.prismItemValues);
        prismItemMatching.copyFrom(source.prismItemMatching);
        this.associationParticipantRole = source.associationParticipantRole;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getDebugDumpClassName());
        sb.append(isMutable() ? "+" : ""); // see Definition#getMutabilityFlag
        sb.append(": ").append(getItemName());
        sb.append(" ").append(PrettyPrinter.prettyPrint(getTypeName()));
        sb.append(" [").append(getMinOccurs()).append(",").append(getMaxOccurs()).append("]");
        sb.append(" ");
        sb.append(canRead() ? "R" : "-");
        sb.append(canAdd() ? "A" : "-");
        sb.append(canModify() ? "M" : "-");
        sb.append(" ");
        sb.append("N=").append(getNativeAttributeName());
        sb.append(", F=").append(getFrameworkAttributeName());
        sb.append(", returned-by-default: ").append(getReturnedByDefault());
        var matchingRuleQName = getMatchingRuleQName();
        if (matchingRuleQName != null) {
            sb.append(", MR=").append(PrettyPrinter.prettyPrint(matchingRuleQName));
        }
        if (isAssociation()) {
            sb.append(", r=").append(associationParticipantRole);
        }
        return sb.toString();
    }

    String getDebugDumpClassName() {
        if (isAssociation()) {
            return "NativeAssocDef";
        } else {
            return "NativeAttrDef";
        }
    }

    public boolean isAssociation() {
        return associationParticipantRole != null;
    }

    /**
     * Not sure why the default method is not inherited here -
     * from {@link PrismItemValuesDefinition.Mutator.Delegable#setAllowedValues(Collection)}.
     */
    @Override
    public void setAllowedValues(Collection<? extends DisplayableValue<T>> displayableValues) {
        prismItemValues.setAllowedValues(displayableValues);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NativeShadowItemDefinitionImpl<?> that = (NativeShadowItemDefinitionImpl<?>) o;
        return Objects.equals(prismItemBasicData, that.prismItemBasicData)
                && Objects.equals(prismItemAccessData, that.prismItemAccessData)
                && Objects.equals(prismItemMiscData, that.prismItemMiscData)
                && Objects.equals(prismPresentationData, that.prismPresentationData)
                && Objects.equals(ucfData, that.ucfData)
                && Objects.equals(prismItemValues, that.prismItemValues)
                && Objects.equals(prismItemMatching, that.prismItemMatching)
                && associationParticipantRole == that.associationParticipantRole;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prismItemBasicData, prismItemAccessData, prismItemMiscData, prismPresentationData, ucfData, prismItemValues, prismItemMatching, associationParticipantRole);
    }
}
