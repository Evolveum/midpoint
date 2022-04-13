/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import static java.util.Collections.emptyList;

import static com.evolveum.midpoint.prism.PrismPropertyValue.getRealValue;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeSynchronizationPolicy;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Deprecated
@Component
public class SynchronizationExpressionsEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationExpressionsEvaluator.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;

    @NotNull
    public <F extends FocusType> List<PrismObject<F>> findFocusesByCorrelationRule(Class<F> focusType, ShadowType currentShadow,
            List<ConditionalSearchFilterType> conditionalFilters, ResourceType resourceType, SystemConfigurationType configurationType, Task task, OperationResult result)
                    throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        if (conditionalFilters == null || conditionalFilters.isEmpty()) {
            LOGGER.warn("Correlation rule for resource '{}' doesn't contain query, "
                    + "returning empty list of users.", resourceType);
            return emptyList();
        }

        // TODO: determine from the resource
        ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();

        List<PrismObject<F>> allUsers = new ArrayList<>();
        for (ConditionalSearchFilterType conditionalFilter : conditionalFilters) {
            // TODO: better description
            if (satisfyCondition(currentShadow, conditionalFilter, expressionProfile, resourceType, configurationType,
                    "Condition expression", task, result)) {
                LOGGER.trace("Condition {} in correlation expression evaluated to true", conditionalFilter.getCondition());
                List<PrismObject<F>> usersFound = findFocusesByCorrelationRule(focusType, currentShadow, conditionalFilter,
                        expressionProfile, resourceType, configurationType, task, result);
                for (PrismObject<F> userFound : usersFound) {
                    if (!contains(allUsers, userFound)) {
                        allUsers.add(userFound);
                    }
                }
            }
        }

        LOGGER.debug(
                "SYNCHRONIZATION: CORRELATION: expression for {} returned {} users: {}", currentShadow, allUsers.size(),
                PrettyPrinter.prettyPrint(allUsers, 3));
        return allUsers;
    }

    private boolean satisfyCondition(ShadowType currentShadow, ConditionalSearchFilterType conditionalFilter,
            ExpressionProfile expressionProfile, ResourceType resource, SystemConfigurationType configuration,
            String shortDesc, Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (conditionalFilter.getCondition() == null) {
            return true;
        } else {
            VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(null, currentShadow, resource, configuration);
            return ExpressionUtil.evaluateConditionDefaultFalse(variables,
                    conditionalFilter.getCondition(), expressionProfile, expressionFactory, shortDesc, task, result);
        }
    }

    private <F extends FocusType> boolean contains(List<PrismObject<F>> users, PrismObject<F> foundUser){
        for (PrismObject<F> user : users){
            if (user.getOid().equals(foundUser.getOid())) {
                return true;
            }
        }
        return false;
    }

    @NotNull private <F extends FocusType> List<PrismObject<F>> findFocusesByCorrelationRule(Class<F> focusType,
            ShadowType currentShadow, ConditionalSearchFilterType conditionalFilter, ExpressionProfile expressionProfile, ResourceType resourceType, SystemConfigurationType configurationType,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException{
        if (!conditionalFilter.containsFilterClause()) {
            LOGGER.warn("Correlation rule for resource '{}' doesn't contain filter clause, "
                    + "returning empty list of users.", resourceType);
            return emptyList();
        }

        ObjectQuery q;
        try {
            q = prismContext.getQueryConverter().createObjectQuery(focusType, conditionalFilter);
            q = updateFilterWithAccountValues(currentShadow, resourceType, configurationType, q, expressionProfile, "Correlation expression", task, result);
        } catch (SchemaException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
                    SchemaDebugUtil.prettyPrint(conditionalFilter));
            throw new SchemaException("Couldn't convert query.", ex);
        } catch (ObjectNotFoundException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
                    SchemaDebugUtil.prettyPrint(conditionalFilter));
            throw new ObjectNotFoundException("Couldn't convert query.", ex);
        } catch (ExpressionEvaluationException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
                    SchemaDebugUtil.prettyPrint(conditionalFilter));
            throw new ExpressionEvaluationException("Couldn't convert query.", ex);
        } catch (CommunicationException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
                    SchemaDebugUtil.prettyPrint(conditionalFilter));
            throw new CommunicationException("Couldn't convert query.", ex);
        } catch (ConfigurationException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
                    SchemaDebugUtil.prettyPrint(conditionalFilter));
            throw new ConfigurationException("Couldn't convert query.", ex);
        } catch (SecurityViolationException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
                    SchemaDebugUtil.prettyPrint(conditionalFilter));
            throw new SecurityViolationException("Couldn't convert query.", ex);
        }

        try {
            LOGGER.trace("SYNCHRONIZATION: CORRELATION: expression for results in filter\n{}", q.debugDumpLazily());
            // TODO read-only later
            return repositoryService.searchObjects(focusType, q, null, result);
        } catch (RuntimeException ex) {
            LoggingUtils.logException(LOGGER,
                    "Couldn't search users in repository, based on filter (simplified)\n{}.", ex, q.debugDump());
            throw new SystemException(
                    "Couldn't search users in repository, based on filter (See logs).", ex);
        }
    }

    private <F extends FocusType> boolean matchUserCorrelationRule(Class<F> focusType, ShadowType currentShadow,
            ExpressionProfile expressionProfile, PrismObject<F> userType, ResourceType resourceType, SystemConfigurationType configurationType,
            ConditionalSearchFilterType conditionalFilter, Task task, OperationResult result) throws SchemaException {
        if (conditionalFilter == null) {
            LOGGER.warn("Correlation rule for resource '{}' doesn't contain query, "
                    + "returning empty list of users.", resourceType);
            return false;
        }

        if (!conditionalFilter.containsFilterClause()) {
            LOGGER.warn("Correlation rule for resource '{}' doesn't contain a filter, "
                    + "returning empty list of users.", resourceType);
            return false;
        }

        ObjectQuery q;
        try {
            if (!satisfyCondition(currentShadow, conditionalFilter, expressionProfile,
                    resourceType, configurationType, "", task, result)) {
                LOGGER.trace("Skipping evaluating correlation rule. Condition in {} not satisfied.", conditionalFilter);
                return false;
            }
            q = prismContext.getQueryConverter().createObjectQuery(focusType, conditionalFilter);
            q = updateFilterWithAccountValues(currentShadow, resourceType, configurationType, q, expressionProfile, "Correlation expression", task, result);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Start matching user {} with correlation expression {}", userType, q != null ? q.debugDump() : "(null)");
            }
            if (q == null) {
                // Null is OK here, it means that the value in the filter evaluated
                // to null and the processing should be skipped
                return false;
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't convert query (simplified)\n{}.", ex,
                    SchemaDebugUtil.prettyPrint(conditionalFilter));
            throw new SystemException("Couldn't convert query.", ex);
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("SYNCHRONIZATION: CORRELATION: expression for {} results in filter\n{}", currentShadow, q);
        }

        // we assume userType is already normalized w.r.t. relations
        ObjectTypeUtil.normalizeFilter(q.getFilter(), relationRegistry);
        return ObjectQuery.match(userType, q.getFilter(), matchingRuleRegistry);
    }

    /**
     * TODO describe!
     * TODO adapt to new correlators!
     */
    <F extends FocusType> boolean matchFocusByCorrelationRule(SynchronizationContext<F> syncCtx, PrismObject<F> focus,
            OperationResult result) {

        ResourceObjectTypeSynchronizationPolicy synchronizationPolicy = syncCtx.getSynchronizationPolicy();
        if (synchronizationPolicy == null) {
            LOGGER.warn(
                    "Resource does not support synchronization. Skipping evaluation correlation/confirmation for {} and {}",
                    focus, syncCtx.getShadowedResourceObject());
            return false;
        }

        List<ConditionalSearchFilterType> conditionalFilters = synchronizationPolicy.getSynchronizationBean().getCorrelation();

        try {
            for (ConditionalSearchFilterType conditionalFilter : conditionalFilters) {
                //TODO: can we expect that systemConfig and resource are always present?
                if (matchUserCorrelationRule(syncCtx.getFocusClass(), syncCtx.getShadowedResourceObject(), syncCtx.getExpressionProfile(), focus, syncCtx.getResource(),
                        syncCtx.getSystemConfiguration(), conditionalFilter, syncCtx.getTask(), result)) {
                    LOGGER.debug("SYNCHRONIZATION: CORRELATION: expression for {} match user: {}", syncCtx.getShadowedResourceObject(), focus);
                    return true;
                }
            }
        } catch (SchemaException ex) {
            throw new SystemException("Failed to match user using correlation rule. " + ex.getMessage(), ex);
        }

        LOGGER.debug("SYNCHRONIZATION: CORRELATION: expression for {} does not match user: {}", syncCtx.getShadowedResourceObject(), focus);
        return false;
    }

    private ObjectQuery updateFilterWithAccountValues(ShadowType currentShadow, ResourceType resource, SystemConfigurationType configuration,
            ObjectQuery origQuery, ExpressionProfile expressionProfile, String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        if (origQuery.getFilter() == null) {
            LOGGER.trace("No filter provided, skipping updating filter");
            return origQuery;
        }

        return evaluateQueryExpressions(origQuery, expressionProfile, currentShadow, resource, configuration, shortDesc, task, result);
    }

    private ObjectQuery evaluateQueryExpressions(ObjectQuery query, ExpressionProfile expressionProfile, ShadowType currentShadow, ResourceType resource, SystemConfigurationType configuration,
            String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(null, currentShadow, resource, configuration);
        return ExpressionUtil.evaluateQueryExpressions(query, variables, expressionProfile, expressionFactory, prismContext, shortDesc, task, result);
    }

    // For now only used in sync service. but later can be used in outbound/assignments
    @SuppressWarnings("WeakerAccess")
    public String generateTag(
            ResourceObjectMultiplicityType multiplicityDefinition,
            ShadowType shadow,
            ResourceType resource,
            SystemConfigurationType configuration,
            String shortDesc,
            Task task,
            OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (multiplicityDefinition == null) {
            return null;
        }
        ShadowTagSpecificationType tagSpec = multiplicityDefinition.getTag();
        if (tagSpec == null) {
            return shadow.getOid();
        }
        ExpressionType expressionBean = tagSpec.getExpression();
        if (expressionBean == null) {
            return shadow.getOid();
        }

        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(null, shadow, resource, configuration);
        ItemDefinition<?> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(
                ExpressionConstants.OUTPUT_ELEMENT_NAME, PrimitiveType.STRING.getQname());
        try {
            ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
            PrismPropertyValue<String> tagProp = ExpressionUtil.evaluateExpression(
                    variables,
                    outputDefinition,
                    expressionBean,
                    MiscSchemaUtil.getExpressionProfile(),
                    expressionFactory,
                    shortDesc,
                    task,
                    result);
            return getRealValue(tagProp);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }
}
