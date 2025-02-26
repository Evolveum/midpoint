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
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.BulkAction;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.AssignActionExpressionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.model.impl.scripting.actions.AssignExecutor.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Component
public class AssignExecutor extends AssignmentOperationsExecutor<AssignParameters> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignExecutor.class);

    /**
     * These are "purified" parameters: targets and constructions to assign.
     * They are created by merging dynamically and statically defined parameters, resolving
     * filters in references, and so on.
     */
    protected static class AssignParameters extends AssignmentOperationsExecutor.Parameters {
        private final List<ObjectReferenceType> targetRefs = new ArrayList<>();
        private final List<ConstructionType> constructions = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(this);
    }

    @Override
    public @NotNull BulkAction getActionType() {
        return BulkAction.ASSIGN;
    }

    @Override
    AssignParameters parseParameters(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            PolicyViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        AssignParameters parameters = new AssignParameters();

        // Dynamic parameters

        Collection<ObjectReferenceType> dynamicRoleRefs = getRolesParameter(action, input, context, result);
        Collection<ObjectReferenceType> dynamicResourceRefs = getResourcesParameter(action, input, context, result);
        Collection<QName> relationSpecifications = getRelationsParameter(action, input, context, result);

        QName relationSpecification = MiscUtil.extractSingleton(relationSpecifications,
                () -> new IllegalArgumentException("Using 'relation' as a multivalued parameter is not allowed"));

        if (PrismConstants.Q_ANY.matches(relationSpecification)) {
            throw new IllegalArgumentException("Using 'q:any' as relation specification is not allowed");
        }

        QName relationOverride;
        if (relationSpecification != null) {
            List<RelationDefinitionType> relationDefinitions = relationRegistry.getRelationDefinitions();
            relationOverride = relationDefinitions.stream()
                    .filter(definition -> prismContext.relationMatches(relationSpecification, definition.getRef()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Relation matching '" + relationSpecification + "' not found"))
                    .getRef();
        } else {
            relationOverride = null;
        }

        // Static parameters

        Collection<ObjectReferenceType> staticTargetRefs;
        Collection<ObjectReferenceType> staticResourceRefs;
        Collection<ConstructionType> staticConstructions;
        if (action instanceof AssignActionExpressionType) {
            staticTargetRefs = ((AssignActionExpressionType) action).getTargetRef();
            staticResourceRefs = ((AssignActionExpressionType) action).getResourceRef();
            staticConstructions = ((AssignActionExpressionType) action).getConstruction();
        } else {
            staticTargetRefs = emptyList();
            staticResourceRefs = emptyList();
            staticConstructions = emptyList();
        }

        // Consolidation

        Task task = context.getTask();

        parameters.targetRefs.addAll(resolve(staticTargetRefs, relationOverride, task, result));
        parameters.targetRefs.addAll(resolve(dynamicRoleRefs, relationOverride, task, result));

        QName defaultRelation = relationRegistry.getDefaultRelation();
        parameters.constructions.addAll(staticConstructions);
        parameters.constructions.addAll(resourceRefsToConstructions(resolve(staticResourceRefs, defaultRelation, task, result)));
        parameters.constructions.addAll(resourceRefsToConstructions(resolve(dynamicResourceRefs, defaultRelation, task, result)));

        return parameters;
    }

    @Override
    boolean checkParameters(AssignParameters parameters, ExecutionContext context) {
        if (parameters.targetRefs.isEmpty() && parameters.constructions.isEmpty()) {
            LOGGER.warn("There are no targets nor constructions to assign");
            context.println("Warning: There are no targets nor constructions to assign");
            return false;
        } else {
            return true;
        }
    }

    private Collection<ObjectReferenceType> resolve(Collection<ObjectReferenceType> targetRefs, QName relationOverride, Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Collection<ObjectReferenceType> rv = new ArrayList<>();
        for (ObjectReferenceType ref : targetRefs) {
            rv.addAll(resolve(ref, relationOverride, task, result));
        }
        return rv;
    }

    private Collection<ObjectReferenceType> resolve(ObjectReferenceType ref, QName relationOverride, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (ref.getFilter() != null) {
            Class<? extends ObjectType> clazz = prismContext.getSchemaRegistry().determineCompileTimeClass(ref.getType());
            if (clazz == null) {
                throw new SchemaException("No compile time class for " + ref.getType() + " in " + ref);
            }
            ObjectQuery query = prismContext.getQueryConverter().createObjectQuery(clazz, ref.getFilter());
            QName effectiveRelation = getEffectiveRelation(ref, relationOverride);
            return modelService.searchObjects(clazz, query, readOnly(), task, result).stream()
                    .map(object -> ObjectTypeUtil.createObjectRef(object, effectiveRelation))
                    .collect(Collectors.toList());
        } else if (relationOverride != null) {
            return singletonList(ref.clone().relation(relationOverride));
        } else {
            return singletonList(ref);
        }
    }

    private QName getEffectiveRelation(ObjectReferenceType reference, QName relationOverride) {
        QName effectiveRelation;
        if (relationOverride != null) {
            effectiveRelation = relationOverride;
        } else if (reference.getRelation() != null) {
            effectiveRelation = reference.getRelation();
        } else {
            effectiveRelation = relationRegistry.getDefaultRelation();
        }
        return effectiveRelation;
    }

    private Collection<ConstructionType> resourceRefsToConstructions(Collection<ObjectReferenceType> resourceRefs) {
        return resourceRefs.stream()
                .map(ref -> new ConstructionType().resourceRef(ref.clone()))
                .collect(Collectors.toList());
    }

    private Collection<AssignmentType> targetsToAssignments(Collection<ObjectReferenceType> targetRefs) {
        return targetRefs.stream()
                .map(ref -> new AssignmentType().targetRef(ref.clone()))
                .collect(Collectors.toList());
    }

    private Collection<AssignmentType> constructionsToAssignments(Collection<ConstructionType> constructions) {
        return constructions.stream()
                .map(c -> new AssignmentType().construction(c.clone()))
                .collect(Collectors.toList());
    }

    @Override
    protected ObjectDelta<? extends ObjectType> createDelta(AssignmentHolderType object, PipelineItem item,
            AssignParameters parameters, ExecutionContext context, OperationResult result) throws SchemaException {

        List<AssignmentType> assignmentsToAdd = new ArrayList<>();
        assignmentsToAdd.addAll(targetsToAssignments(parameters.targetRefs));
        assignmentsToAdd.addAll(constructionsToAssignments(parameters.constructions));

        return prismContext.deltaFor(object.getClass())
                .item(AssignmentHolderType.F_ASSIGNMENT)
                    .addRealValues(assignmentsToAdd)
                .asObjectDelta(object.getOid());
    }
}
