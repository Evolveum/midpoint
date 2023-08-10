/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.schema.util.roles.RoleManagementUtil;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.UnassignActionExpressionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;

import static com.evolveum.midpoint.model.impl.scripting.actions.UnassignExecutor.*;

/**
 * Executor for "unassign" actions.
 */
@Component
public class UnassignExecutor extends AssignmentOperationsExecutor<UnassignParameters> {

    private static final Trace LOGGER = TraceManager.getTrace(UnassignExecutor.class);

    private static final String NAME = "unassign";

    protected static class UnassignParameters extends AssignmentOperationsExecutor.Parameters {
        // These come from dynamic parameters (~ legacy way)
        private final Collection<ObjectReferenceType> dynamicRoleRefs = new ArrayList<>();
        private final Collection<ObjectReferenceType> dynamicResourceRefs = new ArrayList<>();
        private final Collection<QName> dynamicRelations = new ArrayList<>(); // used only with dynamicRoleRefs
        // This one is defined statically (~ modern way)
        private ObjectFilter staticFilter;
    }

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, UnassignActionExpressionType.class, this);
    }

    @Override
    UnassignParameters parseParameters(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult result) throws SchemaException, ScriptExecutionException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        UnassignParameters parameters = new UnassignParameters();
        parameters.dynamicResourceRefs.addAll(getResourcesParameter(action, input, context, result));
        parameters.dynamicRoleRefs.addAll(getRolesParameter(action, input, context, result));
        parameters.dynamicRelations.addAll(getRelationsParameter(action, input, context, result));
        // This was the original behavior: if not specified, we look for org:default references.
        if (parameters.dynamicRelations.isEmpty()) {
            parameters.dynamicRelations.add(relationRegistry.getDefaultRelation());
        }

        if (action instanceof UnassignActionExpressionType) {
            SearchFilterType filterBean = ((UnassignActionExpressionType) action).getFilter();
            if (filterBean != null) {
                parameters.staticFilter = prismContext.getQueryConverter().parseFilter(filterBean, AssignmentType.class);
            }
        }
        return parameters;
    }

    @Override
    boolean checkParameters(UnassignParameters parameters, ExecutionContext context) {
        if (parameters.dynamicRoleRefs.isEmpty() && parameters.dynamicResourceRefs.isEmpty()
                && parameters.staticFilter == null) {
            LOGGER.warn("There are no roles nor resources to unassign and no filter is specified");
            context.println("Warning: There are no roles nor resources to unassign and no filter is specified");
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected ObjectDelta<? extends ObjectType> createDelta(AssignmentHolderType object, PipelineItem item,
            UnassignParameters parameters, ExecutionContext context, OperationResult result) throws SchemaException,
            ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ExpressionEvaluationException {

        ObjectFilter resolvedFilter = resolveFilter(object, item, parameters, context, result);

        List<AssignmentType> assignmentsToDelete = new ArrayList<>();
        for (AssignmentType existingAssignment : object.getAssignment()) {
            if (matches(existingAssignment, parameters, resolvedFilter)) {
                assignmentsToDelete.add(existingAssignment);
            }
        }

        return prismContext.deltaFor(object.getClass())
                .item(ItemPath.create(AssignmentHolderType.F_ASSIGNMENT))
                    .deleteRealValues(CloneUtil.cloneCollectionMembers(assignmentsToDelete))
                .asObjectDelta(object.getOid());
    }

    @Nullable
    private ObjectFilter resolveFilter(AssignmentHolderType object, PipelineItem item, UnassignParameters parameters,
            ExecutionContext context, OperationResult result) throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (parameters.staticFilter != null) {
            return ExpressionUtil.evaluateFilterExpressions(
                    parameters.staticFilter,
                    createVariables(object, item),
                    context.getExpressionProfile(),
                    expressionFactory,
                    "expression evaluation in unassign filter for " + object,
                    context.getTask(),
                    result);
        } else {
            return null;
        }
    }

    private VariablesMap createVariables(AssignmentHolderType input, PipelineItem item) {
        VariablesMap variables = createVariables(item.getVariables());
        variables.put(ExpressionConstants.VAR_INPUT, input, AssignmentHolderType.class);
        return variables;
    }

    /** Roughly related: {@link RoleManagementUtil#getMatchingAssignments(List, Collection)}. */
    @SuppressWarnings("SimplifiableIfStatement")
    private boolean matches(AssignmentType existingAssignment, UnassignParameters parameters, ObjectFilter resolvedFilter)
            throws SchemaException {
        ObjectReferenceType targetRef = existingAssignment.getTargetRef();
        if (targetRef != null
                && matchesOid(targetRef.getOid(), parameters.dynamicRoleRefs)
                && matchesRelation(targetRef.getRelation(), parameters.dynamicRelations)) {
            return true;
        }
        ConstructionType construction = existingAssignment.getConstruction();
        if (construction != null
                && construction.getResourceRef() != null
                && matchesOid(construction.getResourceRef().getOid(), parameters.dynamicResourceRefs)) {
            return true;
        }
        return resolvedFilter != null && resolvedFilter.match(existingAssignment.asPrismContainerValue(), matchingRuleRegistry);
    }

    private boolean matchesOid(String existingOid, Collection<ObjectReferenceType> refsToUnassign) {
        return existingOid != null &&
                refsToUnassign.stream()
                        .anyMatch(ref -> existingOid.equals(ref.getOid()));
    }

    private boolean matchesRelation(QName existingRelation, Collection<QName> relationsToUnassign) {
        return relationsToUnassign.stream()
                .anyMatch(relationToUnassign -> prismContext.relationMatches(relationToUnassign, existingRelation));
    }

    @Override
    protected String getActionName() {
        return NAME;
    }
}
