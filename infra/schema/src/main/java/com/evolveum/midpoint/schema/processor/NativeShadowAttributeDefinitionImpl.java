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

import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.util.DisplayableValue;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.NativeShadowAttributeDefinition.NativeShadowAttributeDefinitionBuilder;
import com.evolveum.midpoint.util.PrettyPrinter;

import org.jetbrains.annotations.Nullable;

/**
 * Represents native attribute (simple or reference).
 *
 * Why single class?
 *
 * Because they have much in common:
 *
 * . in ConnId, both are represented as ConnId attributes;
 * . also in XSD, both are represented as CTD items.
 *
 * The main reason is that the instantiation in schema XSOM parser would require reading custom (higher-level) annotations
 * before instantiating the target class. So, it is much simpler to keep both in a single class. It's no much harm, as this
 * knowledge is basically hidden from the clients that generally don't see the native form of the schema.
 *
 * TODO should we support processing ("ignored") here?
 */
public class NativeShadowAttributeDefinitionImpl<T>
        extends AbstractFreezable
        implements
        NativeShadowAttributeDefinition, NativeShadowAttributeDefinitionBuilder,
        NativeShadowSimpleAttributeDefinition<T>, NativeShadowSimpleAttributeDefinition.NativeShadowAttributeDefinitionBuilder<T>,
        NativeShadowReferenceAttributeDefinition,
        PrismItemBasicDefinition.Delegable, PrismItemBasicDefinition.Mutable.Delegable,
        PrismItemAccessDefinition.Delegable, PrismItemAccessDefinition.Mutable.Delegable,
        PrismItemMiscDefinition.Delegable, PrismItemMiscDefinition.Mutable.Delegable,
        PrismPresentationDefinition.Delegable, PrismPresentationDefinition.Mutable.Delegable,
        ShadowAttributeUcfDefinition.Delegable, ShadowAttributeUcfDefinition.Mutable.Delegable,
        PrismItemValuesDefinition.Delegable<T>, PrismItemValuesDefinition.Mutator.Delegable<T>,
        PrismItemMatchingDefinition.Delegable<T>, PrismItemMatchingDefinition.Mutator.Delegable,
        SerializableItemDefinition, SerializablePropertyDefinition, SerializableContainerDefinition {

    @NotNull private final PrismItemBasicDefinition.Data prismItemBasicData;
    @NotNull private final PrismItemAccessDefinition.Data prismItemAccessData = new PrismItemAccessDefinition.Data();
    @NotNull private final PrismItemMiscDefinition.Data prismItemMiscData = new PrismItemMiscDefinition.Data();
    @NotNull private final PrismPresentationDefinition.Data prismPresentationData = new PrismPresentationDefinition.Data();
    @NotNull private final ShadowAttributeUcfDefinition.Data ucfData = new ShadowAttributeUcfDefinition.Data();
    @NotNull private final PrismItemValuesDefinition.Data<T> prismItemValues = new PrismItemValuesDefinition.Data<>();
    @NotNull private final PrismItemMatchingDefinition.Data<T> prismItemMatching;

    // for references
    private ShadowReferenceParticipantRole referenceParticipantRole;
    private QName referencedObjectClassName;

    NativeShadowAttributeDefinitionImpl(@NotNull ItemName itemName, @NotNull QName typeName) {
        this.prismItemBasicData = new PrismItemBasicDefinition.Data(itemName, typeName);
        this.prismItemMatching = new PrismItemMatchingDefinition.Data<>(typeName);
    }

    @SuppressWarnings("SameParameterValue") // The participant role is always SUBJECT for simulated associations (as of today)
    static NativeShadowAttributeDefinitionImpl<?> forSimulatedAssociation(
            @NotNull ItemName itemName, @NotNull QName typeName, @NotNull ShadowReferenceParticipantRole participantRole) {
        // Simulated associations cannot be connected to raw definitions (for now), so we can create our own definition
        var simulatedNativeDef = new NativeShadowAttributeDefinitionImpl<>(itemName, typeName);
        simulatedNativeDef.setMinOccurs(0);
        simulatedNativeDef.setMaxOccurs(-1);
        simulatedNativeDef.setReferenceParticipantRole(participantRole);
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
    public ShadowAttributeUcfDefinition.Data ucfData() {
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
    public void setMergerIdentifier(String value) {
        unsupported("mergerIdentifier", value);
    }

    @Override
    public void setNaturalKeyConstituents(List<QName> naturalKeyConstituents) {
        unsupported("naturalKeyConstituents", naturalKeyConstituents);
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        unsupported("annotation", qname);
    }

    @Override
    public void setSchemaContextDefinition(SchemaContextDefinition schemaContextDefinition) {
        unsupported("schemaContextAnnotation", schemaContextDefinition);
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
    public DisplayHint getDisplayHint() {
        return PrismPresentationDefinition.Delegable.super.getDisplayHint();
    }

    @Override
    public String getDisplayName() {
        return PrismPresentationDefinition.Delegable.super.getDisplayName();
    }

    @Override
    public String getDocumentation() {
        return PrismPresentationDefinition.Delegable.super.getDocumentation();
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
    public @NotNull ShadowReferenceParticipantRole getReferenceParticipantRole() {
        return Objects.requireNonNull(referenceParticipantRole, "role");
    }

    @Override
    public @Nullable ShadowReferenceParticipantRole getReferenceParticipantRoleIfPresent() {
        return referenceParticipantRole;
    }

    @Override
    public void setReferenceParticipantRole(ShadowReferenceParticipantRole value) {
        this.referenceParticipantRole = value;
    }

    public QName getReferencedObjectClassName() {
        return referencedObjectClassName;
    }

    public void setReferencedObjectClassName(QName value) {
        this.referencedObjectClassName = value;
    }

    public void setDescription(String description) {
        ucfData.setDescription(description);
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
                ResourceDefinitionFeatures.ForItem.DF_ROLE_IN_REFERENCE,
                ResourceDefinitionFeatures.ForItem.DF_REFERENCED_OBJECT_CLASS_NAME);
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
    public NativeShadowAttributeDefinitionImpl<T> clone() {
        NativeShadowAttributeDefinitionImpl<T> clone = new NativeShadowAttributeDefinitionImpl<>(getItemName(), getTypeName());
        clone.copyFrom(this);
        return clone;
    }

    @Override
    public NativeShadowAttributeDefinition cloneWithNewCardinality(int newMinOccurs, int newMaxOccurs) {
        var clone = clone();
        clone.setMinOccurs(newMinOccurs);
        clone.setMaxOccurs(newMaxOccurs);
        return clone;
    }

    private void copyFrom(NativeShadowAttributeDefinitionImpl<T> source) {
        prismItemBasicData.copyFrom(source.prismItemBasicData);
        prismItemAccessData.copyFrom(source.prismItemAccessData);
        prismItemMiscData.copyFrom(source.prismItemMiscData);
        prismPresentationData.copyFrom(source.prismPresentationData);
        ucfData.copyFrom(source.ucfData);
        prismItemValues.copyFrom(source.prismItemValues);
        prismItemMatching.copyFrom(source.prismItemMatching);
        this.referenceParticipantRole = source.referenceParticipantRole;
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
        if (isReference()) {
            sb.append(", r=").append(referenceParticipantRole);
        }
        return sb.toString();
    }

    private String getDebugDumpClassName() {
        if (isReference()) {
            return "NativeRefAttrDef";
        } else {
            return "NativeSimpleAttrDef";
        }
    }

    @Override
    public boolean isReference() {
        return referenceParticipantRole != null;
    }

    @Override
    public boolean isSimple() {
        return !isReference();
    }

    @Override
    public @NotNull NativeShadowSimpleAttributeDefinition<?> asSimple() {
        if (isSimple()) {
            return this;
        } else {
            throw new IllegalStateException("Not a simple attribute definition: " + this);
        }
    }

    @Override
    public @NotNull NativeShadowReferenceAttributeDefinition asReference() {
        if (isReference()) {
            return this;
        } else {
            throw new IllegalStateException("Not a reference attribute definition: " + this);
        }
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
        NativeShadowAttributeDefinitionImpl<?> that = (NativeShadowAttributeDefinitionImpl<?>) o;
        return Objects.equals(prismItemBasicData, that.prismItemBasicData)
                && Objects.equals(prismItemAccessData, that.prismItemAccessData)
                && Objects.equals(prismItemMiscData, that.prismItemMiscData)
                && Objects.equals(prismPresentationData, that.prismPresentationData)
                && Objects.equals(ucfData, that.ucfData)
                && Objects.equals(prismItemValues, that.prismItemValues)
                && Objects.equals(prismItemMatching, that.prismItemMatching)
                && referenceParticipantRole == that.referenceParticipantRole;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prismItemBasicData, prismItemAccessData, prismItemMiscData, prismPresentationData, ucfData,
                prismItemValues, prismItemMatching, referenceParticipantRole);
    }
}
