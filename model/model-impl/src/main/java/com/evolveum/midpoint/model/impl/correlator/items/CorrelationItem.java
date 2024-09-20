/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.correlation.CorrelationPropertyDefinition;
import com.evolveum.midpoint.model.api.indexing.IndexingItemConfiguration;
import com.evolveum.midpoint.model.api.indexing.IndexedItemValueNormalizer;
import com.evolveum.midpoint.model.impl.ModelBeans;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.FuzzyStringMatchFilter;
import com.evolveum.midpoint.prism.query.FuzzyStringMatchFilter.FuzzyMatchingMethod;
import com.evolveum.midpoint.prism.query.FuzzyStringMatchFilter.ThresholdMatchingMethod;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.*;
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
 * TODO finish! cleanup!
 */
public class CorrelationItem implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItem.class);

    @NotNull private final String name;

    @NotNull private final ItemPath itemPath;

    /** Null iff {@link #indexingItemConfiguration} is null. */
    @Nullable private final IndexedItemValueNormalizer valueNormalizer;

    // TODO
    @Nullable private final IndexingItemConfiguration indexingItemConfiguration;

    /** Note we ignore "index" from this configuration. It is already processed into {@link #valueNormalizer} field. */
    @NotNull private final ItemSearchDefinitionType searchDefinitionBean;

    /** Matching rule defined for this item in the object template. (Not related to indexing.) */
    @Nullable private final QName defaultMatchingRuleName;

    // TODO
    @NotNull private final List<? extends PrismValue> prismValues;

    private CorrelationItem(
            @NotNull String name,
            @NotNull ItemPath itemPath,
            @Nullable IndexedItemValueNormalizer valueNormalizer,
            @Nullable ItemSearchDefinitionType searchDefinitionBean,
            @Nullable IndexingItemConfiguration indexingItemConfiguration,
            @Nullable QName defaultMatchingRuleName,
            @NotNull List<? extends PrismValue> prismValues) {
        this.name = name;
        this.itemPath = itemPath;
        this.valueNormalizer = valueNormalizer;
        this.searchDefinitionBean = searchDefinitionBean != null ? searchDefinitionBean : new ItemSearchDefinitionType();
        this.indexingItemConfiguration = indexingItemConfiguration;
        this.defaultMatchingRuleName = defaultMatchingRuleName;
        this.prismValues = prismValues;
    }

    public static CorrelationItem create(
            @NotNull CorrelationItemType itemBean,
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull Containerable preFocus)
            throws ConfigurationException {
        @NotNull CorrelationPropertyDefinition propertyDef =
                CorrelationPropertyDefinition.fromConfiguration(
                        itemBean, preFocus.asPrismContainerValue().getComplexTypeDefinition());
        @NotNull ItemPath path = propertyDef.getItemPath();
        @Nullable IndexingItemConfiguration indexingConfig = getIndexingItemConfiguration(itemBean, correlatorContext);
        @Nullable ItemSearchDefinitionType searchDef = getSearch(itemBean, correlatorContext, path);
        @Nullable String explicitIndexName = searchDef != null ? searchDef.getIndex() : null;
        @Nullable QName defaultMatchingRuleName =
                correlatorContext.getTemplateCorrelationConfiguration().getDefaultMatchingRuleName(path);
        return new CorrelationItem(
                propertyDef.getName(),
                path,
                getValueNormalizer(indexingConfig, explicitIndexName, path),
                searchDef,
                indexingConfig,
                defaultMatchingRuleName,
                getPrismValues(preFocus, path));
    }

    private static IndexingItemConfiguration getIndexingItemConfiguration(
            @NotNull CorrelationItemType itemBean, @NotNull CorrelatorContext<?> correlatorContext) {
        ItemPathType itemPathBean = itemBean.getRef();
        if (itemPathBean != null) {
            return correlatorContext
                    .getTemplateCorrelationConfiguration()
                    .getIndexingConfiguration()
                    .getForPath(itemPathBean.getItemPath());
        } else {
            return null;
        }
    }

    private static ItemSearchDefinitionType getSearch(
            @NotNull CorrelationItemType itemBean, @NotNull CorrelatorContext<?> correlatorContext, @NotNull ItemPath path) {
        var local = itemBean.getSearch();
        if (local != null) {
            return local;
        }
        ItemCorrelationDefinitionType inTemplateDef =
                correlatorContext.getTemplateCorrelationConfiguration().getCorrelationDefinitionMap().get(path);
        return inTemplateDef != null ? inTemplateDef.getSearch() : null;
    }

    private static IndexedItemValueNormalizer getValueNormalizer(
            IndexingItemConfiguration indexingConfig, String index, ItemPath path)
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
                    indexingConfig.findNormalizer(index),
                    () -> new ConfigurationException(
                            String.format("Index '%s' was not found in indexing configuration for '%s'", index, path)));
        }
    }

    private static @NotNull List<? extends PrismValue> getPrismValues(
            @NotNull Containerable preFocus, @NotNull ItemPath itemPath) {
        Item<?, ?> item = preFocus.asPrismContainerValue().findItem(itemPath);
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

    public S_FilterExit addClauseToQueryBuilder(
            S_FilterEntry builder, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        SearchSpec searchSpec = createSearchSpec(task, result);
        LOGGER.trace("Will look for {}", searchSpec);

        FuzzyMatchingMethod fuzzyMatchingMethod = getFuzzyMatchingMethod();
        if (fuzzyMatchingMethod != null) {
            return builder
                    .item(searchSpec.itemPath, searchSpec.itemDef)
                    .fuzzyString(convertToString(searchSpec.value), fuzzyMatchingMethod);
        } else if (searchSpec.value instanceof Referencable referencable) {
            return builder
                    .item(searchSpec.itemPath, searchSpec.itemDef)
                    .ref(referencable.asReferenceValue().clone());
        } else {
            return builder
                    .item(searchSpec.itemPath, searchSpec.itemDef)
                    .eq(searchSpec.value)
                    .matching(getMatchingRuleName());
        }
    }

    private SearchSpec createSearchSpec(Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (indexingItemConfiguration != null) {
            assert valueNormalizer != null;
            return new SearchSpec(
                    valueNormalizer.getIndexItemPath(),
                    valueNormalizer.getIndexItemDefinition(),
                    valueNormalizer.normalize(getValueToFind(), task, result));
        } else {
            return new SearchSpec(
                    itemPath,
                    null, // will be found by the query builder
                    getValueToFind());
        }
    }

    private @Nullable FuzzyMatchingMethod getFuzzyMatchingMethod()
            throws ConfigurationException {
        FuzzySearchDefinitionType fuzzyDef = searchDefinitionBean.getFuzzy();
        if (fuzzyDef == null) {
            return null;
        }

        LevenshteinDistanceSearchDefinitionType levenshtein = fuzzyDef.getLevenshtein();
        if (levenshtein != null) {
            return new FuzzyStringMatchFilter.Levenshtein(
                    MiscUtil.configNonNull(
                            levenshtein.getThreshold(),
                            () -> "Please specify Levenshtein edit distance threshold"),
                    !Boolean.FALSE.equals(levenshtein.isInclusive()));
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
        QName explicitRule = searchDefinitionBean.getMatchingRule();
        if (explicitRule != null) {
            return explicitRule;
        }
        if (indexingItemConfiguration != null) {
            return PrismConstants.DEFAULT_MATCHING_RULE_NAME;
        } else {
            return defaultMatchingRuleName;
        }
    }

    /**
     * Can we use this item for correlation?
     *
     * Temporary implementation: We can, if it's non-null. (In future we might configure the behavior in such cases.)
     */
    public boolean isApplicable() throws SchemaException {
        return getRealValue() != null;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull ItemPath getItemPath() {
        return itemPath;
    }

    double computeConfidence(Containerable candidate, Task task, OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        ExpressionType expression = getConfidenceExpression();
        if (expression == null) {
            return 1;
        }
        LOGGER.trace("Computing confidence of {} in relation to {}", candidate, this);
        List<Double> matchMetricValues = computeMatchMetricValues(candidate, task, result);
        List<Double> confidenceValues = convertMetricToConfidence(matchMetricValues, expression, task, result);
        // This is the default aggregator - could be made configurable in the future.
        double resultingConfidence = confidenceValues.stream()
                .max(Comparator.naturalOrder())
                .orElse(1.0);
        LOGGER.trace("Confidence values {} yielding {}", confidenceValues, resultingConfidence);
        return resultingConfidence;
    }

    private ExpressionType getConfidenceExpression() {
        ItemSearchConfidenceDefinitionType confidenceDef = searchDefinitionBean.getConfidence();
        ExpressionType expression = confidenceDef != null ? confidenceDef.getExpression() : null;
        return expression != null ? expression : getDefaultConfidenceExpression();
    }

    /** For fuzzy search. */
    private ExpressionType getDefaultConfidenceExpression() {
        FuzzySearchDefinitionType fuzzyDef = searchDefinitionBean.getFuzzy();
        if (fuzzyDef == null) {
            return null;
        }
        if (fuzzyDef.getLevenshtein() != null) {
            return createConfidenceExpression("1/(input+1)");
        } else if (fuzzyDef.getSimilarity() != null) {
            return createConfidenceExpression("input");
        } else {
            return null; // should not occur anyway
        }
    }

    private ExpressionType createConfidenceExpression(String code) {
        return
                new ExpressionType()
                        .expressionEvaluator(
                                new ObjectFactory().createScript(
                                        new ScriptExpressionEvaluatorType()
                                                .code(code)));
    }

    /** Returns the values of given metric (e.g. Levenshtein distance) for given candidate for this item. No nulls on return. */
    private @NotNull List<Double> computeMatchMetricValues(Containerable candidate, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        SearchSpec searchSpec = createSearchSpec(task, result);
        String sourceValue = convertToString(searchSpec.value);
        Collection<PrismValue> allValues = candidate.asPrismContainerValue().getAllValues(searchSpec.itemPath);
        MatchMetricValueComputer matchMetricValueComputer = getMatchMetricValueComputer();
        List<Double> matchValues = allValues.stream()
                .map(PrismValue::getRealValue)
                .filter(Objects::nonNull)
                .map(CorrelationItem::convertToString)
                .map(targetValue -> matchMetricValueComputer.computeMatchMetricValue(sourceValue, targetValue))
                .collect(Collectors.toList());
        LOGGER.trace("Matching strings: {} leading to values: {} (search spec: {})", allValues, matchValues, searchSpec);
        return matchValues;
    }

    private MatchMetricValueComputer getMatchMetricValueComputer() throws ConfigurationException {
        ThresholdMatchingMethod<?> thresholdMatchingMethod;
        FuzzyMatchingMethod fuzzyMatchingMethod = getFuzzyMatchingMethod();
        if (!(fuzzyMatchingMethod instanceof ThresholdMatchingMethod<?>)) {
            thresholdMatchingMethod = null;
        } else {
            thresholdMatchingMethod = (ThresholdMatchingMethod<?>) fuzzyMatchingMethod;
        }
        return (source, target) -> {
            if (thresholdMatchingMethod == null) {
                return 1;
            } else {
                return thresholdMatchingMethod
                        .computeMatchMetricValue(source, target)
                        .doubleValue();
            }
        };
    }

    /** No nulls in returned list. */
    private @NotNull List<Double> convertMetricToConfidence(
            List<Double> matchMetricValues, ExpressionType expression, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        QName inputTypeName = DOMUtil.XSD_DOUBLE;
        PrismPropertyDefinition<Double> inputPropertyDef =
                PrismContext.get().definitionFactory().newPropertyDefinition(
                        ExpressionConstants.VAR_INPUT_QNAME, inputTypeName);
        inputPropertyDef.mutator().setMaxOccurs(-1);
        PrismPropertyDefinition<Double> outputPropertyDef =
                PrismContext.get().definitionFactory().newPropertyDefinition(
                        ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_DOUBLE);
        outputPropertyDef.mutator().setMaxOccurs(-1);
        PrismProperty<Double> inputProperty = inputPropertyDef.instantiate();
        new HashSet<>(matchMetricValues) // To avoid "Adding value to property input that already exists (overwriting)" warnings
                .forEach(inputProperty::addRealValue);
        Source<PrismPropertyValue<Double>, PrismPropertyDefinition<Double>> inputSource =
                new Source<>(
                        inputProperty, null, inputProperty, inputProperty.getElementName(), inputPropertyDef);
        Collection<PrismPropertyValue<Double>> confidenceValues = ExpressionUtil.evaluateExpressionNative(
                List.of(inputSource),
                new VariablesMap(),
                outputPropertyDef,
                expression,
                MiscSchemaUtil.getExpressionProfile(),
                ModelBeans.get().expressionFactory,
                "confidence expression for " + this,
                task,
                result);
        return confidenceValues.stream()
                .filter(Objects::nonNull) // maybe not necessary
                .map(pv -> pv.getRealValue())
                .filter(Objects::nonNull) // maybe not necessary
                .collect(Collectors.toList());
    }

    private static String convertToString(@NotNull Object o) {
        if (o instanceof String) {
            return (String) o;
        } else if (o instanceof PolyString) {
            return ((PolyString) o).getOrig();
        } else if (o instanceof PolyStringType) {
            return ((PolyStringType) o).getOrig();
        } else {
            throw new UnsupportedOperationException(
                    "Couldn't use fuzzy search to look for non-string value of " + MiscUtil.getValueWithClass(o));
        }
    }

    @Override
    public String toString() {
        return "CorrelationItem{" +
                "name=" + name +
                ", itemPath=" + itemPath +
                ", valueNormalizer=" + valueNormalizer +
                ", indexing=" + indexingItemConfiguration +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "name", name, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "itemPath", String.valueOf(itemPath), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "valueNormalizer", String.valueOf(valueNormalizer), indent + 1);
        DebugUtil.debugDumpWithLabelLn(
                sb, "indexingItemConfiguration", String.valueOf(indexingItemConfiguration), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "values", prismValues, indent + 1);
        return sb.toString();
    }

    /** What we are looking for, when correlating according to this item? */
    private record SearchSpec(
            @NotNull ItemPath itemPath,
            @Nullable ItemDefinition<?> itemDef,
            @NotNull Object value) {

        @Override
            public String toString() {
                return "path='" + itemPath + "'" +
                        ", def='" + itemDef + "'" +
                        ", value='" + value + "'";
            }
        }

    private interface MatchMetricValueComputer {
        double computeMatchMetricValue(String source, String target);
    }
}
