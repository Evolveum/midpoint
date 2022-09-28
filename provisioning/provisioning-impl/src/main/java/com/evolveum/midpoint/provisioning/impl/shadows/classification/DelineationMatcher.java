/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.classification;

import java.util.Collection;
import java.util.List;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseContextClassificationUseType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import static com.evolveum.midpoint.schema.util.ShadowUtil.getObjectClassRequired;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.BaseContextClassificationUseType.IF_APPLICABLE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.BaseContextClassificationUseType.IGNORED;

/**
 * Answers the question if a shadow matches given object type delineation. It does so by evaluating the following:
 *
 * 1. base context (including scope),
 * 2. filter(s),
 * 3. classification condition.
 *
 * Currently used strictly for classification. Later it may be generalized.
 *
 * Note: Support for base context matching is very limited, with these assumptions:
 *
 * 1. Only LDAP distinguished names are supported.
 * 2. Base context must be specified by "equals" filter with a single value.
 * 3. The attribute specifying base context top must have DN matching rule (that is used as a flag that it's a DN).
 * 4. The DN in shadow is used by finding an identifier with DN matching rule.
 *
 * TODO The serious solution would be either to add a scope checking method to ConnId. (It would be evaluated locally
 *  by connector, requiring a roundtrip in the case of remote connectors.) But maybe even better solution would be to
 *  extend a {@link MatchingRule} contract with a new predicate like `under` that would compare structured data like
 *  LDAP distinguished names, Internet domain names, IP addresses, and so on.
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
        if (!objectClassMatches()) {
            LOGGER.trace("Object class does not match");
            return false;
        }
        if (!isBaseContextIgnored() && !baseContextMatches()) {
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

    private boolean objectClassMatches() throws SchemaException {
        return QNameUtil.match(
                getObjectClassRequired(context.getShadowedResourceObject()),
                delineation.getObjectClassName());
    }

    private boolean isBaseContextIgnored() {
        return delineation.getBaseContextClassificationUse() == IGNORED;
    }

    /**
     * This is tricky, as the base context is currently interpreted in connectors.
     *
     * So only LDAP distinguished names are supported for now.
     *
     * In other cases, we return
     *
     *  - `true`, relying on other means of classification (filters, conditions) - if the use of base context is optional
     *  ({@link BaseContextClassificationUseType#IF_APPLICABLE}
     *  - `false`, considering the object as non-matching - if {@link BaseContextClassificationUseType#REQUIRED} is set
     *
     * See limitations described in the class javadocs.
     */
    private boolean baseContextMatches() throws SchemaException, ConfigurationException {
        ResourceObjectReferenceType baseContext = delineation.getBaseContext();
        if (baseContext == null) {
            return true;
        }
        LdapName scopeRootDn = getRootDistinguishedName(baseContext);
        if (scopeRootDn == null) {
            LOGGER.debug("-> no root DN, base context cannot be used for classification");
            return isBaseContextOptional();
        }
        LdapName shadowDn = getShadowDistinguishedName();
        if (shadowDn == null) {
            LOGGER.debug("-> no DN in shadow, base context cannot be used for classification");
            return isBaseContextOptional();
        }

        SearchHierarchyScope scope = delineation.getSearchHierarchyScope();
        boolean rv = isUnder(shadowDn, scopeRootDn, scope);
        LOGGER.trace("{} is under {} (scope {}): {}", shadowDn, scopeRootDn, scope, rv);
        return rv;
    }

    private boolean isBaseContextOptional() {
        return delineation.getBaseContextClassificationUse() == IF_APPLICABLE;
    }

    private boolean isUnder(LdapName child, LdapName parent, SearchHierarchyScope scope) {
        if (child.startsWith(parent)) {
            if (scope == SearchHierarchyScope.ONE) {
                return child.size() == parent.size() + 1;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Tries to get the DN of the object that represents the root of the base context.
     * Assumes the "equal" filter with definition using DN matching rule.
     */
    private @Nullable LdapName getRootDistinguishedName(ResourceObjectReferenceType baseContext)
            throws SchemaException, ConfigurationException {
        SearchFilterType filterBean = baseContext.getFilter();
        if (filterBean == null) {
            LOGGER.debug("Base context without filter: not using for classification");
            return null;
        }
        QName rootObjectClassName = baseContext.getObjectClass();
        if (rootObjectClassName == null) {
            LOGGER.debug("No object class in base context: not using for classification");
            return null;
        }
        ResourceObjectDefinition scopeObjectClassDefinition = Resource.of(context.getResource())
                .getRawSchemaRequired()
                .findObjectClassDefinitionRequired(rootObjectClassName);
        ObjectFilter filter = QueryConversionUtil.parseFilter(filterBean, scopeObjectClassDefinition);
        if (!(filter instanceof EqualFilter)) {
            LOGGER.debug("Base context filter not supported for classification: {}", filter);
            return null;
        }
        EqualFilter<?> equalFilter = (EqualFilter<?>) filter;
        PrismPropertyDefinition<?> definition =
                MiscUtil.requireNonNull(
                        equalFilter.getDefinition(),
                        () -> new IllegalStateException("No definition in " + filter));
        if (!isDistinguishedNameType(definition)) {
            LOGGER.debug("Base context filter is not DN-based: {}", definition);
            return null;
        }
        PrismPropertyValue<?> rootValue = equalFilter.getSingleValue();
        if (rootValue == null) {
            LOGGER.debug("No base context root value in {}", equalFilter);
            return null;
        }
        Object rootRealValue = rootValue.getRealValue();
        if (!(rootRealValue instanceof String)) {
            LOGGER.debug("Root value of base context is not a String, not using for classification: {}", rootRealValue);
            return null;
        }
        try {
            return new LdapName((String) rootRealValue);
        } catch (InvalidNameException e) {
            LOGGER.warn("Root value of base context is not a legal LDAP name, not using for classification: {}",
                    rootRealValue, e);
            return null;
        }
    }

    /**
     * Tries to get (guess) the DN of the shadow being matched.
     */
    private LdapName getShadowDistinguishedName() {
        ShadowType shadow = context.getShadowedResourceObject();
        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getAllIdentifiers(shadow);
        ResourceAttribute<?> dnIdentifier = selectDistinguishedNameIdentifier(identifiers);
        if (dnIdentifier == null) {
            LOGGER.debug("No DN-identifier in {}", shadow);
            return null;
        }
        Object dnRealValue = dnIdentifier.getRealValue();
        if (!(dnRealValue instanceof String)) {
            LOGGER.debug("Value of DN-identifier is not a String, not using for classification: {}", dnRealValue);
            return null;
        }
        try {
            return new LdapName((String) dnRealValue);
        } catch (InvalidNameException e) {
            LOGGER.warn("DN-identifier is not a legal LDAP name, not using for classification: '{}'; in {}",
                    dnRealValue, shadow, e);
            return null;
        }
    }

    private ResourceAttribute<?> selectDistinguishedNameIdentifier(Collection<ResourceAttribute<?>> identifiers) {
        for (ResourceAttribute<?> identifier : identifiers) {
            ResourceAttributeDefinition<?> definition = identifier.getDefinition();
            if (isDistinguishedNameType(definition)) {
                return identifier;
            }
        }
        return null;
    }

    private boolean isDistinguishedNameType(PrismPropertyDefinition<?> definition) {
        return definition != null
                && QNameUtil.match(PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME, definition.getMatchingRuleQName());
    }

    private boolean filterMatches() throws SchemaException {
        List<SearchFilterType> clauses = delineation.getFilterClauses();
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
