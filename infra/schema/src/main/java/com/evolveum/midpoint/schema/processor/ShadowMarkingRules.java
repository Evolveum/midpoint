/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.schema.config.ConfigurationItem.DESC;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.MARK_PROTECTED_OID;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.config.AbstractResourceObjectDefinitionConfigItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Rules that drive automatic shadow marking.
 *
 * They are derived from {@link ShadowMarkingConfigurationType}.
 *
 * 1. During parsing, filter beans are converted into filters; but expressions are not evaluated.
 * 2. Only after a provisioning operation starts, expressions (if any) are evaluated.
 */
public class ShadowMarkingRules implements Serializable {

    /** Were expressions in filters (if there are any) already evaluated? */
    private final boolean expressionsEvaluated;

    /** Immutable. */
    @NotNull private final Map<String, MarkingRule> markingRulesMap;

    private ShadowMarkingRules(
            boolean expressionsEvaluated,
            @NotNull Map<String, MarkingRule> markingRulesMap) {
        this.expressionsEvaluated = expressionsEvaluated;
        this.markingRulesMap = Map.copyOf(markingRulesMap);
    }

    public static ShadowMarkingRules parse(
            @NotNull AbstractResourceObjectDefinitionConfigItem<?> definitionCI,
            @NotNull AbstractResourceObjectDefinitionImpl definition) throws ConfigurationException {

        var markingRulesMap = new Parser(definitionCI, definition).parse();
        return new ShadowMarkingRules(false, markingRulesMap);
    }

    public @NotNull Map<String, MarkingRule> getMarkingRulesMap() {
        return markingRulesMap;
    }

    public boolean areExpressionsEvaluated() {
        return expressionsEvaluated;
    }

    /** Returns the same rules but with expressions evaluated. */
    public ShadowMarkingRules evaluateExpressions(@NotNull FilterExpressionEvaluator evaluator, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var transformedRulesMap = new HashMap<String, MarkingRule>();
        for (var sourceRulesEntry : markingRulesMap.entrySet()) {
            transformedRulesMap.put(
                    sourceRulesEntry.getKey(),
                    evaluateExpressionsInRule(sourceRulesEntry.getValue(), evaluator, result));
        }
        return new ShadowMarkingRules(true, transformedRulesMap);
    }

    private MarkingRule evaluateExpressionsInRule(
            MarkingRule sourceRule, FilterExpressionEvaluator evaluator, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var transformedPatterns = new ArrayList<ResourceObjectPattern>();
        for (var sourcePattern : sourceRule.getPatterns()) {
            transformedPatterns.add(
                    new ResourceObjectPattern(
                            sourcePattern.getObjectDefinition(),
                            evaluator.evaluate(sourcePattern.getFilter(), result)));
        }
        return new MarkingRule(sourceRule.getApplicationTime(), transformedPatterns);
    }

    /** Rule for a single shadow mark. */
    public static class MarkingRule {

        /** When is this rule applied? */
        @NotNull private final ShadowMarkApplicationTimeType applicationTime;

        /** Individual patterns comprising this rule. Zero patterns are legal as well. */
        @NotNull private final Collection<ResourceObjectPattern> patterns;

        private MarkingRule(
                @NotNull ShadowMarkApplicationTimeType applicationTime, @NotNull Collection<ResourceObjectPattern> patterns) {
            this.applicationTime = applicationTime;
            this.patterns = List.copyOf(patterns);
        }

        public @NotNull ShadowMarkApplicationTimeType getApplicationTime() {
            return applicationTime;
        }

        public @NotNull Collection<ResourceObjectPattern> getPatterns() {
            return patterns;
        }

        public boolean matches(@NotNull AbstractShadow shadow) throws SchemaException {
            if (patterns.isEmpty()) {
                return true; // no patterns -> each shadow matches
            }
            for (var pattern : patterns) {
                if (pattern.matches(shadow)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** TODO deduplicate with the similar interface in ReferenceResolver in model-api. */
    public interface FilterExpressionEvaluator extends Serializable {
        ObjectFilter evaluate(ObjectFilter rawFilter, OperationResult result) throws SchemaException,
                ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
                ConfigurationException, SecurityViolationException;
    }

    private static class Parser {

        @NotNull private final AbstractResourceObjectDefinitionConfigItem<?> definitionCI;
        @NotNull private final AbstractResourceObjectDefinitionImpl definition;

        @NotNull private final Map<String, ShadowMarkConfigurationType> configBeansMap = new HashMap<>();
        @NotNull private final Map<String, MarkingRule> parsedRulesMap = new HashMap<>();

        Parser(
                @NotNull AbstractResourceObjectDefinitionConfigItem<?> definitionCI,
                @NotNull AbstractResourceObjectDefinitionImpl definition) {
            this.definitionCI = definitionCI;
            this.definition = definition;
        }

        Map<String, MarkingRule> parse() throws ConfigurationException {

            createConfigBeansMap();

            for (var configBeanEntry : configBeansMap.entrySet()) {
                var markOid = configBeanEntry.getKey();
                var ruleBean = configBeanEntry.getValue();
                parsedRulesMap.put(
                        markOid,
                        parseMarkingRule(
                                resolveDefaultApplicationTime(ruleBean.getApplicationTime(), markOid),
                                ruleBean.getPattern()));
            }

            var legacyProtectedPatternBean = definitionCI.value().getProtected();
            if (!legacyProtectedPatternBean.isEmpty()) {
                definitionCI.configCheck(
                        !parsedRulesMap.containsKey(MARK_PROTECTED_OID),
                        "Protected objects cannot be specified in both legacy and modern way in %s", DESC);
                parsedRulesMap.put(
                        MARK_PROTECTED_OID,
                        parseMarkingRule(ShadowMarkApplicationTimeType.ALWAYS, legacyProtectedPatternBean));
            }

            return parsedRulesMap;
        }

        private @NotNull ShadowMarkApplicationTimeType resolveDefaultApplicationTime(
                @Nullable ShadowMarkApplicationTimeType specified, @NotNull String markOid) {
            if (specified != null) {
                return specified;
            } else if (SystemObjectsType.MARK_TOLERATED.value().equals(markOid)) {
                return ShadowMarkApplicationTimeType.CLASSIFICATION;
            } else {
                return ShadowMarkApplicationTimeType.ALWAYS;
            }
        }

        private MarkingRule parseMarkingRule(
                @NotNull ShadowMarkApplicationTimeType applicationTime,
                @NotNull Collection<ResourceObjectPatternType> patternBeans) throws ConfigurationException {
            var prismObjectDef = definition.getPrismObjectDefinition();
            var patterns = new ArrayList<ResourceObjectPattern>();
            for (var patternBean : patternBeans) {
                patterns.add(convertToPattern(patternBean, prismObjectDef));
            }
            return new MarkingRule(applicationTime, patterns);
        }

        private void createConfigBeansMap() throws ConfigurationException {
            var markingDefBean = definitionCI.value().getMarking();
            if (markingDefBean != null) {
                addConfigBean(markingDefBean.getProtected(), SystemObjectsType.MARK_PROTECTED.value());
                addConfigBean(markingDefBean.getIgnored(), SystemObjectsType.MARK_IGNORED.value());
                addConfigBean(markingDefBean.getTolerated(), SystemObjectsType.MARK_TOLERATED.value());
                for (var otherBean : markingDefBean.getOther()) {
                    var markOid = definitionCI.nonNull(getOid(otherBean.getMarkRef()), "marking ref OID");
                    addConfigBean(otherBean, markOid);
                }
            }
        }

        private void addConfigBean(
                @Nullable ShadowMarkConfigurationType markConfigBean,
                @NotNull String markOid) throws ConfigurationException {
            if (markConfigBean != null) {
                definitionCI.configCheck(
                        !configBeansMap.containsKey(markOid),
                        "Marking rule for mark %s is defined multiple times in %s",
                        markOid, DESC);
                configBeansMap.put(markOid, markConfigBean);
            }
        }

        private ResourceObjectPattern convertToPattern(
                ResourceObjectPatternType patternBean, PrismObjectDefinition<ShadowType> prismObjectDef)
                throws ConfigurationException {
            SearchFilterType filterBean =
                    definitionCI.nonNull(patternBean.getFilter(), "filter in resource object pattern");
            try {
                // It is strange, but some weird filters may be parsed as null.
                var filterParsed =
                        definitionCI.nonNull(
                                PrismContext.get().getQueryConverter().parseFilter(filterBean, prismObjectDef),
                                "filter in resource object pattern");
                return new ResourceObjectPattern(definition, filterParsed);
            } catch (SchemaException e) { // TODO add ConfigItem information
                throw new ConfigurationException("Couldn't parse protected object filter: " + e.getMessage(), e);
            }
        }
    }
}
