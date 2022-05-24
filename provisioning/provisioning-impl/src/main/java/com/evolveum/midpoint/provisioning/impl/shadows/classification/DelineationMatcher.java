/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.classification;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.naming.ldap.LdapName;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.provisioning.api.Resource;
import com.evolveum.midpoint.provisioning.util.QueryConversionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Answers the question if a shadow matches given object type delineation.
 *
 * Currently used strictly for classification. Later it may be generalized.
 *
 * Limitations:
 *
 * - support for base context matching is very limited (to LDAP distinguished names)
 */
class DelineationMatcher {

    private static final Trace LOGGER = TraceManager.getTrace(DelineationMatcher.class);

    @NotNull private final ResourceObjectTypeDelineation delineation;
    @NotNull private final ResourceObjectDefinition resourceObjectDefinition;
    @NotNull private final ClassificationContext context;

    DelineationMatcher(
            @NotNull ResourceObjectTypeDelineation delineation,
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ClassificationContext context) {
        this.delineation = delineation;
        this.resourceObjectDefinition = resourceObjectDefinition;
        this.context = context;
    }

    public boolean matches(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (!baseContextMatches()) {
            LOGGER.trace("Base context does not match");
            return false;
        }
        if (!filterMatches()) {
            LOGGER.trace("Filter does not match");
            return false;
        }
        if (!conditionMatches(result)) {
            LOGGER.trace("Condition does not match");
            return false;
        }
        LOGGER.trace("Delineation matches");
        return true;
    }

    /**
     * This is tricky, as the base context is currently interpreted in connectors.
     *
     * So only LDAP distinguished names are supported for now.
     *
     * In other cases, we return true, relying on other means of classification (filters, conditions).
     *
     * UGLY HACKING ... FIXME?
     */
    private boolean baseContextMatches() throws SchemaException, ConfigurationException {
        ResourceObjectReferenceType baseContext = delineation.getBaseContext();
        if (baseContext == null) {
            return true;
        }
        SearchFilterType filterBean = baseContext.getFilter();
        if (filterBean == null) {
            LOGGER.debug("Base context without filter: not using for classification");
            return true;
        }
        QName scopeObjectClassName = MiscUtil.requireNonNull(
                baseContext.getObjectClass(),
                () -> new ConfigurationException("No object class in base context: " + baseContext));
        ResourceObjectDefinition scopeObjectClassDefinition = Resource.of(context.getResource())
                .getRawSchemaRequired()
                .findDefinitionForObjectClassRequired(scopeObjectClassName);
        ObjectFilter filter = QueryConversionUtil.parseFilter(filterBean, scopeObjectClassDefinition);
        if (!(filter instanceof EqualFilter)) {
            LOGGER.debug("Base context filter not supported for classification: {}", filter);
            return true;
        }
        EqualFilter<?> equalFilter = (EqualFilter<?>) filter;
        PrismPropertyDefinition<?> definition =
                MiscUtil.requireNonNull(
                        equalFilter.getDefinition(),
                        () -> new IllegalStateException("No definition in " + filter));
        QName matchingRuleQName = definition.getMatchingRuleQName();
        if (!PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME.equals(matchingRuleQName)) {
            LOGGER.debug("Base context filter is not DN-based: {}", matchingRuleQName);
            return true;
        }
        Collection<ResourceAttribute<?>> identifiers =
                ShadowUtil.getAllIdentifiers(context.getShadowedResourceObject());
        ResourceAttribute<?> dnIdentifier = selectDistinguishedNameIdentifier(identifiers);
        if (dnIdentifier == null) {
            LOGGER.debug("No DN-identifier in {}", context.getShadowedResourceObject());
            return true;
        }
        PrismPropertyValue<?> shadowValue = dnIdentifier.getValue();
        PrismPropertyValue<?> scopeValue = equalFilter.getSingleValue();
        if (scopeValue == null) {
            LOGGER.debug("No scope value in {}", definition);
            return true;
        }
        SearchHierarchyScope scope = delineation.getSearchHierarchyScope();
        boolean rv = isUnder(shadowValue, scopeValue, scope);
        LOGGER.trace("{} is under {} (scope {}): {}", shadowValue, scopeValue, scope, rv);
        return rv;
    }

    private boolean isUnder(PrismPropertyValue<?> childVal, PrismPropertyValue<?> parentVal, SearchHierarchyScope scope) {
        String child = (String) childVal.getRealValue();
        String parent = (String) parentVal.getRealValue();
        try {
            LdapName childLdapName = new LdapName(Objects.requireNonNull(child));
            LdapName parentLdapName = new LdapName(Objects.requireNonNull(parent));
            if (childLdapName.startsWith(parentLdapName)) {
                if (scope == SearchHierarchyScope.ONE) {
                    return childLdapName.size() == parentLdapName.size() + 1;
                } else {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw SystemException.unexpected(e, "when comparing LDAP names");
        }
    }

    private ResourceAttribute<?> selectDistinguishedNameIdentifier(Collection<ResourceAttribute<?>> identifiers) {
        for (ResourceAttribute<?> identifier : identifiers) {
            ResourceAttributeDefinition<?> definition = identifier.getDefinition();
            if (definition != null
                    && PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME.equals(definition.getMatchingRuleQName())) {
                return identifier;
            }
        }
        return null;
    }

    private boolean filterMatches() throws SchemaException {
        List<SearchFilterType> clauses = delineation.getAllFilterClauses();
        List<ObjectFilter> filterList = QueryConversionUtil.parseFilters(clauses, resourceObjectDefinition);
        PrismContainerValue<?> shadowValue = context.getShadowedResourceObject().asPrismContainerValue();
        for (ObjectFilter objectFilter : filterList) {
            if (!objectFilter.match(shadowValue, context.getBeans().matchingRuleRegistry)) {
                return false;
            }
        }
        return true;
    }

    private boolean conditionMatches(
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        ExpressionType classificationConditionBean = delineation.getClassificationCondition();
        if (classificationConditionBean == null) {
            return true;
        }
        String desc = "condition in object synchronization";
        try {
            Task task = context.getTask();
            ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                    new ExpressionEnvironment(task, result));
            return ExpressionUtil.evaluateConditionDefaultTrue(
                    context.createVariablesMap(),
                    classificationConditionBean,
                    MiscSchemaUtil.getExpressionProfile(),
                    context.getBeans().expressionFactory,
                    desc,
                    task,
                    result);
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }
}
