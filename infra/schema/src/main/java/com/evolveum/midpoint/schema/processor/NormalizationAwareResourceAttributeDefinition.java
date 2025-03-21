/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemMerger;
import com.evolveum.midpoint.prism.key.NaturalKeyDefinition;

import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.impl.PrismPropertyImpl;
import com.evolveum.midpoint.prism.impl.delta.PropertyDeltaImpl;
import com.evolveum.midpoint.prism.impl.match.MatchingRuleRegistryImpl;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.normalization.Normalizer;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * An alternative representation of a {@link ShadowSimpleAttributeDefinition} that describes a normalization-aware resource attribute:
 * one that has both original and normalized values. Such attributes are to be stored in the repository, to facilitate
 * searching by normalized values.
 *
 * The current implementation is such that {@link String} properties with a non-trivial normalizer are represented
 * as {@link PolyString} instances.
 *
 * [NOTE]
 * ====
 * This class intentionally does not implement {@link ShadowSimpleAttributeDefinition} interface. It should not be used
 * in place of attribute definition.
 * ====
 */
public class NormalizationAwareResourceAttributeDefinition<T>
        implements PrismPropertyDefinition<T> {

    @NotNull private final ShadowSimpleAttributeDefinition<?> originalDefinition;

    /**
     * Parts of the definition related to normalized {@link String} values held in {@link PolyString} instances
     * (changing the raw String-typed definition to refined PolyStringType-typed ones).
     */
    @NotNull private final DefinitionOverrides<T> definitionOverrides;

    NormalizationAwareResourceAttributeDefinition(@NotNull ShadowSimpleAttributeDefinition<?> originalDefinition) {
        this.originalDefinition = originalDefinition;
        this.definitionOverrides = determineDefinitionOverrides();
    }

    /** Assumes that {@link #originalDefinition} is set up. */
    private DefinitionOverrides<T> determineDefinitionOverrides() {

        // This is the native type of the attribute. It corresponds to the matching rule (like `distinguishedName`).
        QName nativeTypeName = originalDefinition.getTypeName();

        // The matching rule (like `distinguishedName`) can be defined in the raw definition (from resource) or in schemaHandling.
        QName nativeMatchingRuleName = originalDefinition.getMatchingRuleQName();
        MatchingRule<?> nativeMatchingRule = MatchingRuleRegistryImpl.instance()
                .getMatchingRuleSafe(nativeMatchingRuleName, nativeTypeName);

        @NotNull QName overriddenTypeName;
        @Nullable QName overriddenMatchingRuleName;
        boolean switchedToPolyString;
        if (nativeMatchingRule.getNormalizer().isIdentity()) {
            // No need to change the attribute type, as there is no normalization.
            overriddenTypeName = nativeTypeName;
            overriddenMatchingRuleName = nativeMatchingRuleName;
            switchedToPolyString = false;
        } else if (!QNameUtil.match(nativeTypeName, DOMUtil.XSD_STRING)) {
            // Actually, this should not occur (unless maybe for native PolyString attributes).
            // Even if we could normalize non-string values (although in 4.9, there are only string-based normalizers),
            // we have no way how to store orig+norm pair now.
            overriddenTypeName = nativeTypeName;
            overriddenMatchingRuleName = nativeMatchingRuleName;
            switchedToPolyString = false;
        } else {
            // Pair of orig+norm strings is stored in a PolyString.
            overriddenTypeName = PolyStringType.COMPLEX_TYPE;
            overriddenMatchingRuleName = PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME; // TODO ... or orig? -> reconsider
            switchedToPolyString = true;
        }

        Class<T> refinedTypeClass = MiscUtil.requireNonNull(
                PrismContext.get().getSchemaRegistry().determineClassForType(overriddenTypeName),
                () -> new IllegalStateException("No Java type for " + overriddenTypeName + " in " + this));

        return new DefinitionOverrides<>(
                switchedToPolyString,
                overriddenTypeName,
                refinedTypeClass,
                nativeMatchingRuleName,
                overriddenMatchingRuleName);
    }

    @Override
    public boolean isDynamic() {
        return true; // We consider this definition as always dynamic.
    }

//    /**
//     * Returns the normalizer to be used when creating normalized values (stored in {@link PolyString#norm})
//     * for this attribute.
//     *
//     * Currently, this is derived from the declared name of the matching rule. Later, we may create a separate property for this,
//     * most probably replacing the matching rule name.
//     */
//    @NotNull Normalizer<?> getOriginalNormalizer() {
//        return originalDefinition.getNormalizer();
//    }

    @Override
    public @NotNull QName getTypeName() {
        return definitionOverrides.typeName;
    }

    @Override
    public Class<T> getTypeClass() {
        return definitionOverrides.typeClass;
    }

    @Override
    public boolean isRuntimeSchema() {
        return originalDefinition.isRuntimeSchema();
    }

    @Override
    public ItemProcessing getProcessing() {
        return originalDefinition.getProcessing();
    }

    @Override
    public boolean isAbstract() {
        return originalDefinition.isAbstract();
    }

    @Override
    public boolean isDeprecated() {
        return originalDefinition.isDeprecated();
    }

    @Override
    public boolean isRemoved() {
        return originalDefinition.isRemoved();
    }

    @Override
    public String getRemovedSince() {
        return originalDefinition.getRemovedSince();
    }

    @Override
    public boolean isExperimental() {
        return originalDefinition.isExperimental();
    }

    @Override
    public String getPlannedRemoval() {
        return originalDefinition.getPlannedRemoval();
    }

    @Override
    public boolean isElaborate() {
        return originalDefinition.isElaborate();
    }

    @Override
    public String getDeprecatedSince() {
        return originalDefinition.getDeprecatedSince();
    }

    @Override
    public boolean isEmphasized() {
        return originalDefinition.isEmphasized();
    }

    @Override
    public String getDisplayName() {
        return originalDefinition.getDisplayName();
    }

    @Override
    public Integer getDisplayOrder() {
        return originalDefinition.getDisplayOrder();
    }

    @Override
    public String getHelp() {
        return originalDefinition.getHelp();
    }

    @Override
    public String getDocumentation() {
        return originalDefinition.getDocumentation();
    }

    @Override
    public String getDocumentationPreview() {
        return originalDefinition.getDocumentationPreview();
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return originalDefinition.getAnnotation(qname);
    }

    @Override
    public @Nullable Map<QName, Object> getAnnotations() {
        return originalDefinition.getAnnotations();
    }

    @Override
    public @Nullable List<SchemaMigration> getSchemaMigrations() {
        return originalDefinition.getSchemaMigrations();
    }

    @Override
    public List<ItemDiagramSpecification> getDiagrams() {
        return originalDefinition.getDiagrams();
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Override
    public void freeze() {
        // no-op
    }

    @Override
    public @NotNull ItemName getItemName() {
        return originalDefinition.getItemName();
    }

    @Override
    public int getMinOccurs() {
        return originalDefinition.getMinOccurs();
    }

    @Override
    public int getMaxOccurs() {
        return originalDefinition.getMaxOccurs();
    }

    @Override
    public boolean isOperational() {
        return originalDefinition.isOperational();
    }

    @Override
    public boolean isAlwaysUseForEquals() {
        return originalDefinition.isAlwaysUseForEquals();
    }

    @Override
    public boolean isOptionalCleanup() {
        return originalDefinition.isOptionalCleanup();
    }

    @Override
    public DisplayHint getDisplayHint() {
        return originalDefinition.getDisplayHint();
    }

    @Override
    public @Nullable String getMergerIdentifier() {
        return originalDefinition.getMergerIdentifier();
    }

    @Override
    public @Nullable List<QName> getNaturalKeyConstituents() {
        return originalDefinition.getNaturalKeyConstituents();
    }

    @Override
    public @Nullable ItemMerger getMergerInstance(@NotNull MergeStrategy strategy, @Nullable OriginMarker originMarker) {
        return originalDefinition.getMergerInstance(strategy, originMarker);
    }

    @Override
    public @Nullable NaturalKeyDefinition getNaturalKeyInstance() {
        return originalDefinition.getNaturalKeyInstance();
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return originalDefinition.getSchemaContextDefinition();
    }

    @Override
    public boolean isIndexOnly() {
        return originalDefinition.isIndexOnly();
    }

    @Override
    public boolean isInherited() {
        return originalDefinition.isInherited();
    }

    @Override
    public QName getSubstitutionHead() {
        return originalDefinition.getSubstitutionHead();
    }

    @Override
    public boolean isHeterogeneousListItem() {
        return originalDefinition.isHeterogeneousListItem();
    }

    @Override
    public PrismReferenceValue getValueEnumerationRef() {
        return originalDefinition.getValueEnumerationRef();
    }

    @Override
    public boolean isValidFor(
            @NotNull QName elementQName, @NotNull Class<? extends ItemDefinition<?>> clazz, boolean caseInsensitive) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <T1 extends ItemDefinition<?>> T1 findItemDefinition(@NotNull ItemPath path, @NotNull Class<T1> clazz) {
        //noinspection unchecked
        return LivePrismItemDefinition.matchesThisDefinition(path, clazz, this) ? (T1) this : null;
    }

    @Override
    public ItemDefinition<PrismProperty<T>> deepClone(@NotNull DeepCloneOperation operation) {
        return this;
    }

    /** Return a human readable name of this class suitable for logs. */
    public String getDebugDumpClassName() {
        return "NormAwareRAD";
    }

    @Override
    public void debugDumpShortToString(StringBuilder sb) {
        sb.append(getDebugDumpClassName()).append("(");
        originalDefinition.debugDumpShortToString(sb);
        sb.append(")");
    }

    @Override
    public Optional<ComplexTypeDefinition> structuredType() {
        return Optional.empty();
    }

    @Override
    public boolean canRead() {
        return originalDefinition.canRead();
    }

    @Override
    public boolean canModify() {
        return originalDefinition.canModify();
    }

    @Override
    public boolean canAdd() {
        return originalDefinition.canAdd();
    }

    @Override
    public @Nullable Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return List.of(); // TODO
    }

    @Override
    public @Nullable Collection<? extends DisplayableValue<T>> getSuggestedValues() {
        return List.of(); // TODO
    }

    @Override
    public @Nullable T defaultValue() {
        return null; // TODO
    }

    @Override
    public Boolean isIndexed() {
        return originalDefinition.isIndexed();
    }

    @Override
    public QName getMatchingRuleQName() {
        return definitionOverrides.overriddenMatchingRuleName;
    }

    @Override
    public @NotNull MatchingRule<T> getMatchingRule() {
        return MatchingRuleRegistryImpl.instance()
                .getMatchingRuleSafe(getMatchingRuleQName(), getTypeName());
    }

    @Override
    public @NotNull PropertyDelta<T> createEmptyDelta(ItemPath path) {
        return new PropertyDeltaImpl<>(path, this);
    }

    public @NotNull PropertyDelta<T> createEmptyDelta() {
        return createEmptyDelta(getStandardPath());
    }

    @SuppressWarnings("WeakerAccess") // do if needed
    public @NotNull ItemPath getStandardPath() {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getItemName());
    }

    /** TODO name */
    public @NotNull PrismProperty<T> adoptRealValuesAndInstantiate(@NotNull Collection<?> realValues) throws SchemaException {
        return instantiateFromUniqueRealValues(
                adoptRealValues(realValues));
    }

    public @NotNull PrismProperty<T> instantiateFromUniqueRealValues(@NotNull Collection<T> realValues) {
        PrismProperty<T> attribute = instantiate();
        for (T realValue : realValues) {
            attribute.addRealValueSkipUniquenessCheck(realValue);
        }
        return attribute;
    }

    @Override
    public @NotNull PrismProperty<T> instantiate() {
        return new PrismPropertyImpl<>(getItemName(), this);
    }

    @Override
    public @NotNull PrismProperty<T> instantiate(QName name) {
        return new PrismPropertyImpl<>(name, this);
    }

    @Override
    public PrismPropertyDefinitionMutator<T> mutator() {
        throw new UnsupportedOperationException("Implement if needed");
    }

    @Override
    public void revive(PrismContext prismContext) {
        originalDefinition.revive(prismContext);
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        return originalDefinition.accept(visitor, visitation);
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        originalDefinition.accept(visitor);
    }

    // TODO better name
    public boolean isSwitchedFromStringToPolyString() {
        return definitionOverrides.switchedToPolyString;
    }

    @Override
    public @NotNull Normalizer<String> getStringNormalizerForPolyStringProperty() {
        if (isSwitchedFromStringToPolyString()) {
            //noinspection unchecked
            return (Normalizer<String>) originalDefinition.getNormalizer();
        } else {
            return PrismPropertyDefinition.super.getStringNormalizerForPolyStringProperty();
        }
    }

    @Override
    public boolean isCustomPolyString() {
        return true;
    }

    @Override
    public @NotNull T convertStringValueToPolyString(@NotNull String stringValue) throws SchemaException {
        return adoptRealValues(List.of(stringValue))
                .get(0);
    }

    public @NotNull List<T> adoptRealValues(@NotNull Collection<?> realValues) throws SchemaException {
        List<T> rv = new ArrayList<>(realValues.size());
        if (isSwitchedFromStringToPolyString()) {
            Normalizer<String> polyNormalizer = getStringNormalizerForPolyStringProperty();
            for (Object realValue : realValues) {
                rv.add(toNormalizationAwarePolyStringValue(realValue, polyNormalizer));
            }
        } else {
            // No conversion needed. TODO what about non-identity normalizers?
            //noinspection unchecked
            rv.addAll((Collection<? extends T>) realValues);
        }
        return rv;
    }

    /** Returns detached value. */
    private T toNormalizationAwarePolyStringValue(
            @NotNull Object plainRealValue, @NotNull Normalizer<String> polyNormalizer) throws SchemaException {

        Preconditions.checkNotNull(plainRealValue,
                "null value cannot be adopted by %s", this);
        Preconditions.checkArgument(!(plainRealValue instanceof PrismValue),
                "real value is required here: %s", plainRealValue);

        String oldStringValue;
        if (plainRealValue instanceof RawType raw) {
            oldStringValue = raw.getParsedRealValue(String.class);
        } else if (plainRealValue instanceof String string) {
            oldStringValue = string;
        } else if (plainRealValue instanceof PolyString polyString) {
            oldStringValue = polyString.getOrig();
        } else if (plainRealValue instanceof PolyStringType polyString) {
            oldStringValue = polyString.getOrig(); // just in case
        } else {
            throw new UnsupportedOperationException(
                    "Cannot convert from %s to %s".formatted(plainRealValue.getClass(), getTypeClass()));
        }
        //noinspection unchecked
        return (T) wrap(oldStringValue, polyNormalizer.normalizeString(oldStringValue));
    }

    @VisibleForTesting
    public static PolyString wrap(String orig, String norm) {
        return new PolyString(orig, norm);
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "original definition", originalDefinition, indent + 1);
        return sb.toString();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public @NotNull NormalizationAwareResourceAttributeDefinition<T> clone() {
        return new NormalizationAwareResourceAttributeDefinition<>(
                originalDefinition.clone());
    }

    @Override
    public @NotNull ItemDefinition<PrismProperty<T>> cloneWithNewName(@NotNull ItemName itemName) {
        throw new UnsupportedOperationException("Implement if needed");
    }

    @Override
    public String toString() {
        return getDebugDumpClassName() + "(orig: " + originalDefinition + ")";
    }

    /**
     * @param typeClass Java class representing `refinedTypeName`
     */
    private record DefinitionOverrides<T> (
            boolean switchedToPolyString,
            @NotNull QName typeName,
            @NotNull Class<T> typeClass,
            @Nullable QName nativeMatchingRuleName,
            @Nullable QName overriddenMatchingRuleName) implements Serializable {
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NormalizationAwareResourceAttributeDefinition<?> that)) {
            return false;
        }
        return Objects.equals(originalDefinition, that.originalDefinition)
                && Objects.equals(definitionOverrides, that.definitionOverrides);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalDefinition, definitionOverrides);
    }
}
