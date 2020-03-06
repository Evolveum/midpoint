/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.management.relation.RelationType;
import javax.xml.namespace.QName;

/**
 * @author mederly
 */
@Component
public class AssignExecutor extends AssignmentOperationsExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(AssignExecutor.class);

    @Autowired
    protected RelationRegistry relationRegistry;

//    private static final String NAME = "assign";
//    private static final String PARAM_RESOURCE = "resource";
//    private static final String PARAM_ROLE = "role";
//    private static final String PARAM_RELATION = "relation";

//    @PostConstruct
//    public void init() {
//        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
//    }

//    @Override
//    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {
//
//        ModelExecuteOptions executionOptions = getOptions(expression, input, context, globalResult);
//        boolean dryRun = getParamDryRun(expression, input, context, globalResult);
//
//        ActionParameterValueType resourceParameterValue = expressionHelper.getArgument(expression.getParameter(), PARAM_RESOURCE, false, false, NAME);
//        ActionParameterValueType roleParameterValue = expressionHelper.getArgument(expression.getParameter(), PARAM_ROLE, false, false, NAME);
//        Collection<String> relations = expressionHelper.getArgumentValues(expression.getParameter(), PARAM_RELATION, false, false, NAME, input, context, String.class, globalResult);
//
//        Collection<ObjectReferenceType> resources;
//        try {
//            if (resourceParameterValue != null) {
//                PipelineData data = expressionHelper
//                        .evaluateParameter(resourceParameterValue, null, input, context, globalResult);
//                resources = data.getDataAsReferences(ResourceType.COMPLEX_TYPE, ResourceType.class, context, globalResult);
//            } else {
//                resources = null;
//            }
//        } catch (CommonException e) {
//            throw new ScriptExecutionException("Couldn't evaluate '" + PARAM_RESOURCE + "' parameter of a scripting expression: " + e.getMessage(), e);
//        }
//
//        Collection<ObjectReferenceType> roles;
//        try {
//            if (roleParameterValue != null) {
//                PipelineData data = expressionHelper.evaluateParameter(roleParameterValue, null, input, context, globalResult);
//                roles = data.getDataAsReferences(RoleType.COMPLEX_TYPE, AbstractRoleType.class, context, globalResult);        // if somebody wants to assign Org, he has to use full reference value (including object type)
//            } else {
//                roles = null;
//            }
//        } catch (CommonException e) {
//            throw new ScriptExecutionException("Couldn't evaluate '" + PARAM_ROLE + "' parameter of a scripting expression: " + e.getMessage(), e);
//        }
//
//        if (resources == null && roles == null) {
//            throw new ScriptExecutionException("Nothing to assign: neither resource nor role specified");
//        }
//
//        if (CollectionUtils.isEmpty(resources) && CollectionUtils.isEmpty(roles)) {
//            LOGGER.warn("No resources and no roles to assign in a scripting expression");
//            context.println("Warning: no resources and no roles to assign");        // TODO some better handling?
//            return input;
//        }
//
//        for (PipelineItem item : input.getData()) {
//            PrismValue value = item.getValue();
//            OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
//            context.checkTaskStop();
//            if (value instanceof PrismObjectValue && ((PrismObjectValue) value).asObjectable() instanceof FocusType) {
//                @SuppressWarnings({"unchecked", "raw"})
//                PrismObject<? extends ObjectType> prismObject = ((PrismObjectValue) value).asPrismObject();
//                ObjectType objectType = prismObject.asObjectable();
//                long started = operationsHelper.recordStart(context, objectType);
//                Throwable exception = null;
//                try {
//                    operationsHelper.applyDelta(createDelta(objectType, resources, roles, relations), executionOptions, dryRun, context, result);
//                    operationsHelper.recordEnd(context, objectType, started, null);
//                } catch (Throwable ex) {
//                    operationsHelper.recordEnd(context, objectType, started, ex);
//                    exception = processActionException(ex, NAME, value, context);
//                }
//                context.println((exception != null ? "Attempted to modify " : "Modified ") + prismObject.toString() + optionsSuffix(executionOptions, dryRun) + exceptionSuffix(exception));
//            } else {
//                //noinspection ThrowableNotThrown
//                processActionException(new ScriptExecutionException("Item is not a PrismObject of FocusType"), NAME, value, context);
//            }
//            operationsHelper.trimAndCloneResult(result, globalResult, context);
//        }
//        return input;           // TODO updated objects?
//    }

    @Override
    protected String getName() {
        return AssignmentOperationsExecutor.ASSIGN_NAME;
    }

    @Override
    protected ObjectDelta<? extends ObjectType> createDelta(AssignmentHolderType objectType, Collection<ObjectReferenceType> resources, Collection<ObjectReferenceType> roles, Collection<String> relations) throws ScriptExecutionException {

        String relation;

        if (relations == null || relations.isEmpty()) {
            QName defaultRelation = prismContext.getDefaultRelation() != null ?
                    prismContext.getDefaultRelation() : RelationTypes.MEMBER.getRelation();
                    relation = QNameUtil.qNameToUri(defaultRelation);
        } else if (relations.size() > 1) {
            throw new IllegalArgumentException("Couldn't use relation as multivalue parameter");
        } else {
            relation = relations.iterator().next();
        }

        List<AssignmentType> assignments = new ArrayList<>();

        if (roles != null) {
            List<RelationDefinitionType> relationDefinitions = relationRegistry.getRelationDefinitions();
            for (ObjectReferenceType roleRef : roles) {
                AssignmentType assignmentType = new AssignmentType();
                RelationDefinitionType foundRelation = null;
                for (RelationDefinitionType relationDefinitionType : relationDefinitions) {
                    if (prismContext.relationMatches(QNameUtil.uriToQName(relation, true),
                            relationDefinitionType.getRef())) {
                        foundRelation = relationDefinitionType;
                        break;
                    }
                }
                if(foundRelation == null) {
                    throw new IllegalArgumentException("Relation " + relation + " not found");
                }
                roleRef.setRelation(foundRelation.getRef());
                assignmentType.setTargetRef(roleRef);
                assignments.add(assignmentType);
            }
        }

        if (resources != null) {
            for (ObjectReferenceType resourceRef : resources) {
                AssignmentType assignmentType = new AssignmentType();
                ConstructionType constructionType = new ConstructionType();
                constructionType.setResourceRef(resourceRef);
                assignmentType.setConstruction(constructionType);
                assignments.add(assignmentType);
            }
        }

        ObjectDelta<? extends ObjectType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(objectType.getClass(), objectType.getOid());
        try {
            delta.addModificationAddContainer(FocusType.F_ASSIGNMENT, assignments.toArray(new AssignmentType[0]));
        } catch (SchemaException e) {
            throw new ScriptExecutionException("Couldn't prepare modification to add resource/role assignments", e);
        }
        return delta;
    }
}
