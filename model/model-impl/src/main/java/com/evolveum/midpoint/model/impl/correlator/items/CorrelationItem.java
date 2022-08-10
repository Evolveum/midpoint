/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.indexing.IndexingItemConfiguration;
import com.evolveum.midpoint.model.api.indexing.Normalization;
import com.evolveum.midpoint.model.impl.lens.identities.IndexingManager;

import com.evolveum.midpoint.prism.query.FuzzyStringMatchFilter;
import com.evolveum.midpoint.prism.query.FuzzyStringMatchFilter.FuzzyMatchingMethod;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.CorrelationProperty;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Instance of a correlation item
 *
 * TODO finish!
 *
 * TODO what's the relation to {@link CorrelationProperty}?
 */
public class CorrelationItem implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItem.class);

    @NotNull private final String name;

    @NotNull private final ItemPath itemPath;

    /** Null iff {@link #indexingItemConfiguration} is null. */
    @Nullable private final Normalization normalization;

    // TODO
    @Nullable private final IndexingItemConfiguration indexingItemConfiguration;

    /** Note we ignore "index" from this configuration. It is already processed into {@link #normalization} field. */
    @NotNull private final ItemSearchDefinitionType searchDefinitionBean;

    // TODO
    @NotNull private final List<? extends PrismValue> prismValues;

    private CorrelationItem(
            @NotNull String name,
            @NotNull ItemPath itemPath,
            @Nullable Normalization normalization,
            @Nullable ItemSearchDefinitionType searchDefinitionBean,
            @Nullable IndexingItemConfiguration indexingItemConfiguration,
            @NotNull List<? extends PrismValue> prismValues) {
        this.name = name;
        this.itemPath = itemPath;
        this.normalization = normalization;
        this.searchDefinitionBean = searchDefinitionBean != null ? searchDefinitionBean : new ItemSearchDefinitionType();
        this.indexingItemConfiguration = indexingItemConfiguration;
        this.prismValues = prismValues;
    }

    public static CorrelationItem create(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull ObjectType preFocus)
            throws ConfigurationException {
        ItemPath path = getPath(itemBean);
        IndexingItemConfiguration indexingConfig = getIndexingItemConfiguration(itemBean, correlatorContext);
        String explicitIndexName = getExplicitIndexName(itemBean);
        return new CorrelationItem(
                getName(itemBean),
                path,
                getNormalization(indexingConfig, explicitIndexName, path),
                itemBean.getSearch(),
                indexingConfig,
                getPrismValues(preFocus, path));
    }

    private static String getExplicitIndexName(ItemCorrelationType itemBean) {
        ItemSearchDefinitionType searchSpec = itemBean.getSearch();
        return searchSpec != null ? searchSpec.getIndex() : null;
    }

    private static Normalization getNormalization(IndexingItemConfiguration indexingConfig, String index, ItemPath path)
            throws ConfigurationException {
        if (indexingConfig == null) {
            if (index != null) {
                throw new ConfigurationException(
                        String.format("Index '%s' cannot be used, because no indexing configuration is available for '%s'",
                                index, path));
            }
            return null;
        } else {
            return MiscUtil.requireNonNull(
                    indexingConfig.findNormalization(index),
                    () -> new ConfigurationException(
                            String.format("Index '%s' was not found in indexing configuration for '%s'", index, path)));
        }
    }

    private static IndexingItemConfiguration getIndexingItemConfiguration(
            @NotNull ItemCorrelationType itemBean, @NotNull CorrelatorContext<?> correlatorContext) {
        ItemPathType itemPathBean = itemBean.getPath();
        if (itemPathBean != null) {
            return correlatorContext.getIndexingConfiguration().getForPath(itemPathBean.getItemPath());
        } else {
            return null;
        }
    }

    // Temporary code
    private static @NotNull String getName(ItemCorrelationType itemBean) {
        String explicitName = itemBean.getName();
        if (explicitName != null) {
            return explicitName;
        }
        ItemPathType pathBean = itemBean.getPath();
        if (pathBean != null) {
            ItemName lastName = pathBean.getItemPath().lastName();
            if (lastName != null) {
                return lastName.getLocalPart();
            }
        }
        throw new IllegalStateException(
                "Couldn't determine name for correlation item: no name nor path in " + itemBean);
    }

    // Temporary code
    private static @NotNull ItemPath getPath(@NotNull ItemCorrelationType itemBean) throws ConfigurationException {
        ItemPathType specifiedPath = itemBean.getPath();
        if (specifiedPath != null) {
            return specifiedPath.getItemPath();
        } else {
            throw new ConfigurationException("No path for " + itemBean);
        }
    }

    private static @NotNull List<? extends PrismValue> getPrismValues(@NotNull ObjectType preFocus, @NotNull ItemPath itemPath) {
        Item<?, ?> item = preFocus.asPrismObject().findItem(itemPath);
        return item != null ? item.getValues() : List.of();
    }

    private @NotNull Object getValueToFind() throws SchemaException {
        return MiscUtil.requireNonNull(
                getRealValue(),
                () -> new UnsupportedOperationException("Correlation on null item values is not yet supported"));
    }

    /**
     * Returns the source value that should be used for the correlation.
     * We assume there is a single one.
     */
    public Object getRealValue() throws SchemaException {
        PrismValue single = getSinglePrismValue();
        return single != null ? single.getRealValue() : null;
    }

    private PrismValue getSinglePrismValue() {
        return MiscUtil.extractSingleton(
                prismValues,
                () -> new UnsupportedOperationException("Multiple values of " + itemPath + " are not supported: " + prismValues));
    }

    /** Shouldn't return `null` values. */
    public @NotNull Collection<?> getRealValues() throws SchemaException {
        return prismValues.stream()
                .map(PrismValue::getRealValue)
                .collect(Collectors.toList());
    }

    public @Nullable PrismProperty<?> getProperty() throws SchemaException {
        PrismValue single = getSinglePrismValue();
        if (single == null) {
            return null;
        }
        Itemable parent = single.getParent();
        if (parent == null) {
            throw new IllegalStateException("Parent-less source value: " + single + " in " + this);
        } else if (parent instanceof PrismProperty) {
            return (PrismProperty<?>) parent;
        } else {
            throw new UnsupportedOperationException("Non-property sources are not supported: " + single + " in " + this);
        }
    }

    public @Nullable ItemDefinition<?> getDefinition() throws SchemaException {
        // Very temporary implementation
        PrismProperty<?> property = getProperty();
        return property != null ? property.getDefinition() : null;
    }

    S_FilterExit addClauseToQueryBuilder(
            S_FilterEntry builder, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        Object valueToFind = getValueToFind();
        if (indexingItemConfiguration != null) {
            assert normalization != null;
            ItemPath normalizedItemPath = normalization.getIndexItemPath();
            String normalizedValue = IndexingManager.normalizeValue(valueToFind, normalization, task, result);
            LOGGER.trace("Will look for normalized value '{}' in '{}' (of '{}')", normalizedValue, normalizedItemPath, itemPath);
            ItemDefinition<?> normalizedItemDefinition = normalization.getIndexItemDefinition();

            FuzzySearchDefinitionType fuzzyDef = searchDefinitionBean.getFuzzy();
            if (fuzzyDef != null) {
                return builder
                        .item(normalizedItemPath, normalizedItemDefinition)
                        .fuzzyString(normalizedValue, getFuzzyMatchingMethod(fuzzyDef));
            } else {
                return builder
                        .item(normalizedItemPath, normalizedItemDefinition)
                        .eq(normalizedValue)
                        .matching(getMatchingRuleName());
            }
        } else {
            LOGGER.trace("Will look for value '{}' of '{}'", valueToFind, itemPath);
            return builder
                    .item(itemPath)
                    .eq(valueToFind)
                    .matching(getMatchingRuleName());
        }
    }

    private FuzzyMatchingMethod getFuzzyMatchingMethod(FuzzySearchDefinitionType fuzzyDef) throws ConfigurationException {
        LevenshteinDistanceSearchDefinitionType levenshtein = fuzzyDef.getLevenshtein();
        if (levenshtein != null) {
            return new FuzzyStringMatchFilter.Levenshtein(
                    MiscUtil.configNonNull(
                            levenshtein.getThreshold(),
                            () -> "Please specify Levenshtein edit distance threshold"),
                    true);
        }
        TrigramSimilaritySearchDefinitionType similarity = fuzzyDef.getSimilarity();
        if (similarity != null) {
            return new FuzzyStringMatchFilter.Similarity(
                    MiscUtil.configNonNull(
                            similarity.getThreshold(),
                            () -> "Please specify trigram similarity threshold"),
                    !Boolean.FALSE.equals(similarity.isInclusive()));
        }
        throw new ConfigurationException("Please specify Levenshtein or trigram similarity fuzzy string matching method");
    }

    private QName getMatchingRuleName() {
        return Objects.requireNonNullElse(
                searchDefinitionBean.getMatchingRule(),
                PrismConstants.DEFAULT_MATCHING_RULE_NAME);
    }

    /**
     * Can we use this item for correlation?
     *
     * Temporary implementation: We can, if it's non-null. (In future we might configure the behavior in such cases.)
     */
    public boolean isApplicable() throws SchemaException {
        return getRealValue() != null;
    }

    /**
     * Returns the source value wrapped in a property.
     * The property will be named after correlation item, not after the source property.
     *
     * It may be empty. But must not be multi-valued.
     *
     * TODO
     */
    public @Nullable PrismProperty<?> getRenamedSourceProperty() throws SchemaException {
        var property = getProperty();
        if (property == null || name.equals(property.getElementName().getLocalPart())) {
            return property;
        }
        PrismProperty<?> clone = property.clone();
        clone.setElementName(new QName(name));
        return clone;
    }

    public @NotNull String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "CorrelationItem{" +
                "name=" + name +
                ", itemPath=" + itemPath +
                ", normalization=" + normalization +
                ", indexing=" + indexingItemConfiguration +
                '}';
    }

    // Temporary
    public @NotNull CorrelationProperty asCorrelationProperty() throws SchemaException {
        return CorrelationProperty.create(
                name,
                itemPath,
                getRealValues(),
                getDefinition());
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "name", name, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "itemPath", String.valueOf(itemPath), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "normalization", String.valueOf(normalization), indent + 1);
        DebugUtil.debugDumpWithLabelLn(
                sb, "indexingItemConfiguration", String.valueOf(indexingItemConfiguration), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "values", prismValues, indent + 1);
        return sb.toString();
    }
}
