/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlation;

import static com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.Match.*;
import static com.evolveum.midpoint.schema.GetOperationOptions.createRetrieveCollection;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.Match;
import com.evolveum.midpoint.model.api.correlation.CorrelationPropertyDefinition;
import com.evolveum.midpoint.model.api.correlator.Correlator;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription;
import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.CandidateDescription;
import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.CorrelationPropertyValuesDescription;
import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlation.CorrelationService.CorrelationCaseDescriptionOptions;
import com.evolveum.midpoint.model.api.correlation.TemplateCorrelationConfiguration;
import com.evolveum.midpoint.model.api.correlator.CorrelationExplanation;
import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.api.indexing.IndexingItemConfiguration;
import com.evolveum.midpoint.model.api.indexing.ValueNormalizer;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.items.CorrelationItem;
import com.evolveum.midpoint.model.impl.lens.indexing.IndexingManager;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MatchingUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

/**
 * Creates {@link CorrelationCaseDescription} for given correlation "case".
 *
 * TODO better name?
 */
class CorrelationCaseDescriber<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationCaseDescriber.class);

    @NotNull private final CorrelatorContext<?> correlatorContext;

    @NotNull private final CorrelationContext correlationContext;

    /** Not null after the start of {@link #describe(OperationResult)} method. */
    private Correlator correlator;

    @NotNull private final F preFocus;

    @NotNull private final List<ResourceObjectOwnerOptionType> ownerOptionsList;

    private final boolean explain;

    /** The result */
    @NotNull private final CorrelationCaseDescription<F> description;

    /** Relates to pre-focus. We assume that it is applicable also to the candidates. */
    private final @NotNull TemplateCorrelationConfiguration templateCorrelationConfiguration;

    @NotNull private final String contextDesc;

    @NotNull private final Task task;

    @NotNull private final ModelBeans beans;

    CorrelationCaseDescriber(
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull CorrelationContext correlationContext,
            @NotNull List<ResourceObjectOwnerOptionType> ownerOptionsList,
            @Nullable CorrelationCaseDescriptionOptions options,
            @NotNull String contextDesc,
            @NotNull Task task,
            @NotNull ModelBeans beans) {

        //noinspection unchecked
        this.preFocus = (F) correlationContext.getPreFocus();
        this.ownerOptionsList = ownerOptionsList;
        this.description = new CorrelationCaseDescription<>(preFocus);
        this.correlatorContext = correlatorContext;
        this.correlationContext = correlationContext;
        this.explain = CorrelationCaseDescriptionOptions.isExplain(options);
        this.templateCorrelationConfiguration = correlatorContext.getTemplateCorrelationConfiguration();
        this.contextDesc = contextDesc;
        this.task = task;
        this.beans = beans;
    }

    public @NotNull CorrelationCaseDescription<?> describe(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {

        correlator = beans.correlatorFactoryRegistry.instantiateCorrelator(correlatorContext, task, result);

        setupCorrelationProperties(result);
        setupCandidates(result);

        return description;
    }

    private void setupCorrelationProperties(OperationResult result) throws ConfigurationException, SchemaException {
        addCorrelationPropertiesFromCorrelator(result);
        addCorrelationPropertiesFromPreFocus();
        LOGGER.trace("Correlation properties:\n{}", DebugUtil.debugDumpLazily(description.getCorrelationPropertiesDefinitions(), 1));
    }

    private void addCorrelationPropertiesFromCorrelator(OperationResult result) throws ConfigurationException, SchemaException {
        var focusDefinition = correlationContext.getPreFocus().asPrismObject().getDefinition();
        var correlationProperties = correlator.getCorrelationPropertiesDefinitions(focusDefinition, task, result);
        for (CorrelationPropertyDefinition propertyDefinition : correlationProperties) {
            description.addCorrelationPropertyDefinition(propertyDefinition);
        }
    }

    private void addCorrelationPropertiesFromPreFocus() {
        for (PrismProperty<?> preFocusProperty : MatchingUtil.getSingleValuedProperties(preFocus)) {
            ItemPath path = preFocusProperty.getPath().namedSegmentsOnly();
            if (!description.hasCorrelationProperty(path)) {
                description.addCorrelationPropertyDefinition(
                        CorrelationPropertyDefinition.fromData(path, preFocusProperty));
            }
        }
    }

    private void setupCandidates(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        for (ResourceObjectOwnerOptionType ownerOption : ownerOptionsList) {
            ObjectReferenceType candidateOwnerRef = ownerOption.getCandidateOwnerRef();
            if (candidateOwnerRef == null) {
                continue;
            }
            F candidate = retrieveCandidate(candidateOwnerRef, result);
            if (candidate == null) {
                continue;
            }

            double confidence;
            CorrelationExplanation explanation;
            if (explain) {
                explanation = correlator.explain(correlationContext, candidate, result);
                confidence = explanation.getConfidence().getValue();
            } else {
                explanation = null;
                confidence = or0(ownerOption.getConfidence());
            }

            var propertiesValuesMap = createCandidatePropertiesValuesMap(candidate, result);
            description.addCandidate(
                    new CandidateDescription<>(candidate, confidence, propertiesValuesMap, explanation));
        }
    }

    private PathKeyedMap<CorrelationPropertyValuesDescription> createCandidatePropertiesValuesMap(
            F candidate, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        PathKeyedMap<CorrelationPropertyValuesDescription> map = new PathKeyedMap<>();
        for (CorrelationPropertyDefinition correlationPropertyDef : description.getCorrelationPropertiesDefinitions().values()) {
            map.put(
                    correlationPropertyDef.getItemPath(),
                    createCandidatePropertyValuesDescription(candidate, correlationPropertyDef, result));
        }
        return map;
    }

    private CorrelationPropertyValuesDescription createCandidatePropertyValuesDescription(
            F candidate, CorrelationPropertyDefinition correlationPropertyDef, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        PrismContainerValue<?> pcv = candidate.asPrismContainerValue();
        ItemPath itemPath = correlationPropertyDef.getItemPath();
        Set<PrismValue> primaryValues = new HashSet<>(pcv.getAllValues(itemPath));
        Set<PrismValue> allSecondaryValues = new HashSet<>(pcv.getAllValues(correlationPropertyDef.getSecondaryPath()));
        Set<PrismValue> secondaryOnlyValues = Sets.difference(allSecondaryValues, primaryValues);
        Set<PrismValue> allValues = Sets.union(primaryValues, allSecondaryValues);

        Collection<PrismValue> preFocusValues = preFocus.asPrismContainerValue().getAllValues(itemPath);
        IndexingItemConfiguration indexing = templateCorrelationConfiguration.getIndexingConfiguration().getForPath(itemPath);
        QName defaultMatchingRule = templateCorrelationConfiguration.getDefaultMatchingRuleName(itemPath);
        Match match =
                new MatchDetermination(
                        candidate, correlationPropertyDef, preFocusValues, primaryValues, allValues, indexing, defaultMatchingRule)
                        .determine(task, result);
        return new CorrelationPropertyValuesDescription(
                correlationPropertyDef, primaryValues, secondaryOnlyValues, match);
    }

    private @Nullable F retrieveCandidate(ObjectReferenceType candidateOwnerRef, OperationResult result)
            throws SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, ObjectNotFoundException {
        Class<F> aClass = PrismContext.get().getSchemaRegistry().determineClassForTypeRequired(
                MiscUtil.requireNonNull(
                        candidateOwnerRef.getType(),
                        () -> String.format("No type information in candidate reference %s when describing %s",
                                candidateOwnerRef, contextDesc)));
        String oid =
                MiscUtil.requireNonNull(
                        candidateOwnerRef.getOid(),
                        () -> String.format("No OID in candidate reference %s when describing %s",
                                candidateOwnerRef, contextDesc));
        try {
            return beans.modelService
                    .getObject(aClass, oid, createRetrieveCollection(), task, result)
                    .asObjectable();
        } catch (ObjectNotFoundException e) {
            if (oid.equals(e.getOid())) {
                LOGGER.warn("Correlation candidate {} does not exist (any longer?) - ignoring; when describing {}",
                        oid, contextDesc);
                return null;
            } else {
                throw e;
            }
        }
    }

    /** Determines the {@link Match} (e.g. full, partial, none) for given candidate, property, and situation. */
    private class MatchDetermination {

        @NotNull private final F candidate;
        @NotNull private final CorrelationPropertyDefinition correlationPropertyDef;
        @NotNull private final Collection<PrismValue> preFocusValues;
        @NotNull private final Set<PrismValue> primaryValues;
        @NotNull private final Set<PrismValue> allValues;

        /** Indexing configuration (from object template) for given item. */
        @Nullable private final IndexingItemConfiguration indexing;

        /** Default matching rule defined in the object template for given item. */
        @Nullable private final QName defaultMatchingRule;

        private MatchDetermination(
                @NotNull F candidate,
                @NotNull CorrelationPropertyDefinition correlationPropertyDef,
                @NotNull Collection<PrismValue> preFocusValues,
                @NotNull Set<PrismValue> primaryValues,
                @NotNull Set<PrismValue> allValues,
                @Nullable IndexingItemConfiguration indexing,
                @Nullable QName defaultMatchingRule) {
            this.candidate = candidate;
            this.correlationPropertyDef = correlationPropertyDef;
            this.preFocusValues = preFocusValues;
            this.primaryValues = primaryValues;
            this.allValues = allValues;
            this.indexing = indexing;
            this.defaultMatchingRule = defaultMatchingRule;
        }

        Match determine(Task task, OperationResult result)
                throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
                SecurityViolationException, ObjectNotFoundException {
            LOGGER.trace("Determining match for {}, pre-focus: {}, primary: {}, all: {}, indexing: {}",
                    correlationPropertyDef, preFocusValues, primaryValues, allValues, indexing);
            if (preFocusValues.size() != 1) {
                LOGGER.trace("... not applicable because # values in pre-focus is not 1");
                return NOT_APPLICABLE;
            }
            Object preFocusRealValue = preFocusValues.iterator().next().getRealValue();
            if (preFocusRealValue == null) {
                LOGGER.trace("... not applicable because real value in pre-focus is null"); // shouldn't be
                return NOT_APPLICABLE;
            }
            ValueNormalizer defaultValueNormalizer = indexing != null ?
                    indexing.getDefaultNormalizer() : IndexingManager.getNormalizerFor(defaultMatchingRule);
            String preFocusNormalized = defaultValueNormalizer.normalize(preFocusRealValue, task, result);

            for (PrismValue primaryValue : primaryValues) {
                Object primaryRealValue = primaryValue.getRealValue();
                if (primaryRealValue == null) {
                    continue;
                }
                String primaryNormalized = defaultValueNormalizer.normalize(primaryRealValue, task, result);
                if (primaryNormalized.equals(preFocusNormalized)) {
                    LOGGER.trace("Match of primary value '{}' (normalized to '{}' using default normalizer '{}') -> FULL",
                            primaryRealValue, primaryNormalized, defaultValueNormalizer);
                    return FULL;
                } else {
                    LOGGER.trace("No match of primary value '{}' (normalized to '{}' using default normalizer '{}') -> continuing",
                            primaryRealValue, primaryNormalized, defaultValueNormalizer);
                }
            }

            ItemPath itemPath = correlationPropertyDef.getItemPath();
            Set<CorrelationItemType> correlationDefSet =
                    correlatorContext.getConfiguration().getAllConfigurationsDeeply().stream()
                            .map(CorrelatorConfiguration::getConfigurationBean)
                            .filter(bean -> bean instanceof ItemsCorrelatorType)
                            .map(bean -> (ItemsCorrelatorType) bean)
                            .flatMap(bean -> bean.getItem().stream())
                            .filter(item -> item.getRef() != null && item.getRef().getItemPath().equivalent(itemPath))
                            .collect(Collectors.toSet());

            LOGGER.trace("Correlation 'item' definitions:\n{}", DebugUtil.toStringCollectionLazy(correlationDefSet, 1));
            for (CorrelationItemType correlationDef : correlationDefSet) {
                CorrelationItem correlationItem = CorrelationItem.create(correlationDef, correlatorContext, preFocus);
                S_FilterEntry builder = PrismContext.get().queryFor(preFocus.getClass());
                ObjectFilter filter =
                        correlationItem.addClauseToQueryBuilder(builder, task, result)
                                .buildFilter();
                if (filter.match(candidate.asPrismContainerValue(), beans.matchingRuleRegistry)) {
                    LOGGER.trace("Match on item-derived filter: {} -> PARTIAL", filter);
                    return PARTIAL;
                } else {
                    LOGGER.trace("No match on item-derived filter: {} -> continuing", filter);
                }
            }

            Collection<? extends ValueNormalizer> normalizers = indexing != null ?
                    indexing.getNormalizers() : Set.of(IndexingManager.getNormalizerFor(defaultMatchingRule));
            LOGGER.trace("Trying to find a match using applicable normalizers (count: {})", normalizers.size());

            for (PrismValue anyValue : allValues) {
                Object anyRealValue = anyValue.getRealValue();
                if (anyRealValue == null) {
                    continue;
                }
                for (ValueNormalizer normalizer : normalizers) {
                    String anyNormalized = normalizer.normalize(anyRealValue, task, result);
                    if (anyNormalized.equals(preFocusNormalized)) {
                        LOGGER.trace("Match of 'any' value '{}' (normalized to '{}' using a normalizer '{}') -> PARTIAL",
                                anyRealValue, anyNormalized, normalizer);
                        return PARTIAL;
                    } else {
                        LOGGER.trace("No match of 'any' value '{}' (normalized to '{}' using a normalizer '{}') -> continuing",
                                anyRealValue, anyNormalized, normalizer);
                    }
                }
            }
            LOGGER.trace("No value matches -> NONE");
            return NONE;
        }
    }
}
