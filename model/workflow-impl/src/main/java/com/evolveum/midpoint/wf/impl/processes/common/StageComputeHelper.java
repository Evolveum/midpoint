/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.wf.impl.processes.common;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType.REJECT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.AUTO_COMPLETION_CONDITION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.NO_ASSIGNEES_FOUND;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.temporary.ComputationMode;
import com.evolveum.midpoint.cases.api.temporary.VariablesProvider;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.primary.policy.PolicyRuleBasedAspect;
import com.evolveum.midpoint.wf.impl.util.ComputationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Helps with computing things needed for stage approval (e.g. approvers, auto-approval result, ...)
 */
@Component
public class StageComputeHelper {

    private static final Trace LOGGER = TraceManager.getTrace(StageComputeHelper.class);

    @Autowired private ExpressionEvaluationHelper evaluationHelper;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private SystemObjectCache systemObjectCache;

    // TODO method name
    public ComputationResult computeStageApprovers(ApprovalStageDefinitionType stageDef, CaseType theCase,
            VariablesProvider variablesProvider, @NotNull ComputationMode computationMode,
            Task opTask, OperationResult opResult) throws SchemaException {
        ComputationResult rv = new ComputationResult();
        VariablesMap variablesMap = null;
        VariablesProvider enhancedVariablesProvider = () -> {
            VariablesMap variables = variablesProvider.get();
            variables.put(ExpressionConstants.VAR_STAGE_DEFINITION, stageDef, ApprovalStageDefinitionType.class);
            variables.put(ExpressionConstants.VAR_POLICY_RULES,
                    ApprovalContextUtil.getRulesForStage(theCase.getApprovalContext(), stageDef.getNumber()), List.class);
            return variables;
        };

        if (stageDef.getAutomaticallyCompleted() != null) {
            try {
                variablesMap = enhancedVariablesProvider.get();
                String outcome = evaluateAutoCompleteExpression(stageDef, variablesMap, opTask, opResult);
                if (outcome != null) {
                    rv.predeterminedOutcome = ApprovalUtils.approvalLevelOutcomeFromUri(outcome);
                    rv.automatedCompletionReason = AUTO_COMPLETION_CONDITION;
                }
            } catch (Exception e) {     // todo
                throw new SystemException("Couldn't evaluate auto-approval expression", e);
            }
        }

        rv.approverRefs = new HashSet<>();

        if (rv.predeterminedOutcome == null) {
            rv.approverRefs.addAll(CloneUtil.cloneCollectionMembers(stageDef.getApproverRef()));

            if (!stageDef.getApproverExpression().isEmpty()) {
                try {
                    if (variablesMap == null) {
                        variablesMap = enhancedVariablesProvider.get();
                    }
                    rv.approverRefs.addAll(evaluateApproverExpressions(stageDef.getApproverExpression(), theCase, variablesMap, opTask, opResult));
                } catch (CommonException | RuntimeException e) {
                    throw new SystemException("Couldn't evaluate approvers expressions", e);
                }
            }

            LOGGER.trace("Approvers at the stage {} (before potential group expansion) are: {}", stageDef, rv.approverRefs);
            if (stageDef.getGroupExpansion() == GroupExpansionType.ON_WORK_ITEM_CREATION) {
                if (shouldExpandGroup(computationMode, opResult)) {
                    rv.approverRefs = expandGroups(rv.approverRefs); // see MID-4105
                    LOGGER.trace("Approvers at the stage {} (after group expansion) are: {}", stageDef, rv.approverRefs);
                } else {
                    LOGGER.trace("Groups will not be expanded; computation mode = {}", computationMode);
                }
            }

            if (rv.approverRefs.isEmpty()) {
                rv.noApproversFound = true;
                rv.predeterminedOutcome = defaultIfNull(stageDef.getOutcomeIfNoApprovers(), REJECT);
                rv.automatedCompletionReason = NO_ASSIGNEES_FOUND;
            }
        }
        return rv;
    }

    /**
     * Evaluates approver expressions, providing a separate assignment copy when available.
     *
     * Assignment resolution is attempted at most once. Expressions explicitly declaring
     * the assignment variable retain their original variable resolution behavior.
     */
    private List<ObjectReferenceType> evaluateApproverExpressions(List<ExpressionType> expressions, CaseType theCase,
            VariablesMap variables, Task task, OperationResult result) throws CommonException {
        List<ObjectReferenceType> approvers = new ArrayList<>();
        AssignmentType assignment = null;
        boolean assignmentResolutionAttempted = false;

        for (ExpressionType expression : expressions) {
            // Preserve explicitly declared assignment variables.
            boolean declaresAssignment = declaresAssignmentVariable(expression);

            if (!declaresAssignment && !assignmentResolutionAttempted) {
                assignment = resolveAssignmentIfApplicable(theCase, variables);
                assignmentResolutionAttempted = true;
            }

            VariablesMap expressionVariables = variables;
            if (assignment != null && !declaresAssignment) {
                expressionVariables = addAssignmentVariableIfPossible(theCase, variables, assignment);
            }

            approvers.addAll(evaluationHelper.evaluateRefExpression(expression, expressionVariables, "resolving approver expression", task, result));
        }

        return approvers;
    }

    private VariablesMap addAssignmentVariableIfPossible(CaseType aCase, VariablesMap variables, AssignmentType assignment) {
        try {
            VariablesMap expressionVariables = variables.shallowClone();
            expressionVariables.put(ExpressionConstants.VAR_ASSIGNMENT, assignment.clone(), AssignmentType.class);
            return expressionVariables;
        } catch (RuntimeException e) {
            LOGGER.debug("Assignment variable is unavailable for case {} because preparing its expression value failed ({})",
                    aCase.getOid(), e.getClass().getSimpleName());
            return variables;
        }
    }

    /**
     * Resolves the assignment from the existing approval data without persisting additional information.
     *
     * Returns null if the case is not an assignment approval or the assignment cannot be
     * determined unambiguously. Resolution failures must not affect existing approval behavior.
     */
    private @Nullable AssignmentType resolveAssignmentIfApplicable(CaseType aCase, VariablesMap variables) {
        if (variables.containsKey(ExpressionConstants.VAR_ASSIGNMENT)
                || aCase.getTargetRef() == null
                || aCase.getApprovalContext() == null
                || !PolicyRuleBasedAspect.class.getName().equals(aCase.getApprovalContext().getChangeAspect())) {
            return null;
        }

        Object deltaValue = variables.getValue(ExpressionConstants.VAR_OBJECT_DELTA);
        if (!(deltaValue instanceof ObjectDelta<?> objectDelta) || !objectDelta.isModify()) {
            LOGGER.debug("Assignment variable is unavailable for case {} because it has no factored focus modification delta", aCase.getOid());
            return null;
        }

        try {
            ItemDelta<?, ?> exactAssignmentDelta = null;
            int exactAssignmentDeltaCount = 0;
            for (ItemDelta<?, ?> modification : objectDelta.getModifications()) {
                if (modification.getPath().equivalent(AssignmentHolderType.F_ASSIGNMENT)) {
                    exactAssignmentDelta = modification;
                    exactAssignmentDeltaCount++;
                }
            }
            // Assignment additions and deletions are factored into separate deltas, one per approval case.
            if (exactAssignmentDelta != null) {
                if (exactAssignmentDeltaCount != 1 || objectDelta.getModifications().size() != 1
                        || !(exactAssignmentDelta instanceof ContainerDelta<?> containerDelta)) {
                    LOGGER.debug("Assignment variable is unavailable for case {} because its complete assignment delta is ambiguous", aCase.getOid());
                    return null;
                }
                return extractAddedOrDeletedAssignment(aCase, containerDelta);
            }
            return reconstructModifiedAssignment(aCase, objectDelta, variables);
        } catch (SchemaException | RuntimeException e) {
            LOGGER.debug("Assignment variable is unavailable for case {} because transient reconstruction failed ({})",
                    aCase.getOid(), e.getClass().getSimpleName());
            return null;
        }
    }

    private @Nullable AssignmentType extractAddedOrDeletedAssignment(CaseType aCase, ContainerDelta<?> assignmentDelta) {
        Collection<? extends PrismContainerValue<?>> values = null;
        if (!assignmentDelta.isReplace() && assignmentDelta.isAdd() != assignmentDelta.isDelete()) {
            values = assignmentDelta.isAdd() ? assignmentDelta.getValuesToAdd() : assignmentDelta.getValuesToDelete();
        }
        if (values == null || values.size() != 1) {
            LOGGER.debug("Assignment variable is unavailable for case {} because its assignment value delta is ambiguous", aCase.getOid());
            return null;
        }
        return (AssignmentType) values.iterator().next().asContainerable();
    }

    /**
     * Reconstructs the modified assignment by applying the factored approval delta
     * to the current focus object without modifying the original.
     *
     * Returns null if the delta does not identify a single assignment or the
     * assignment cannot be reconstructed.
     */
    private @Nullable AssignmentType reconstructModifiedAssignment(CaseType aCase, ObjectDelta<?> objectDelta, VariablesMap variables) throws SchemaException {
        Long assignmentId = null;
        for (ItemDelta<?, ?> modification : objectDelta.getModifications()) {
            ItemPath path = modification.getPath();
            Long modificationAssignmentId = path.startsWithName(AssignmentHolderType.F_ASSIGNMENT)
                    ? path.rest().firstToIdOrNull() : null;
            if (modificationAssignmentId == null || assignmentId != null && !assignmentId.equals(modificationAssignmentId)) {
                LOGGER.debug("Assignment variable is unavailable for case {} because its modification delta is ambiguous", aCase.getOid());
                return null;
            }
            assignmentId = modificationAssignmentId;
        }

        Object objectValue = variables.getValue(ExpressionConstants.VAR_OBJECT);
        if (assignmentId == null || !(objectValue instanceof PrismObject<?> object)) {
            LOGGER.debug("Assignment variable is unavailable for case {} because the current object or assignment ID is missing", aCase.getOid());
            return null;
        }
        if (objectDelta.getOid() != null && !objectDelta.getOid().equals(object.getOid())) {
            LOGGER.debug("Assignment variable is unavailable for case {} because the delta and current object do not match", aCase.getOid());
            return null;
        }

        // Apply the approved changes to the current object to obtain the complete assignment.
        PrismObject<?> changedObject = computeChangedObject(objectDelta, object);
        if (changedObject == null || !(changedObject.asObjectable() instanceof ObjectType changedObjectBean)) {
            LOGGER.debug("Assignment variable is unavailable for case {} because the changed focus object is missing or unsupported", aCase.getOid());
            return null;
        }
        AssignmentType assignment = ObjectTypeUtil.getAssignment(changedObjectBean, assignmentId);
        if (assignment == null) {
            LOGGER.debug("Assignment variable is unavailable for case {} because assignment ID {} is absent from the current object", aCase.getOid(), assignmentId);
            return null;
        }
        return assignment;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private PrismObject<?> computeChangedObject(ObjectDelta<?> objectDelta, PrismObject<?> object) throws SchemaException {
        return ((ObjectDelta) objectDelta).computeChangedObject((PrismObject) object);
    }

    private boolean declaresAssignmentVariable(ExpressionType expression) {
        return expression.getVariable().stream()
                .map(ExpressionVariableDefinitionType::getName)
                .filter(Objects::nonNull)
                .anyMatch(name -> ExpressionConstants.VAR_ASSIGNMENT.equals(name.getLocalPart()));
    }

    private boolean shouldExpandGroup(ComputationMode computationMode, OperationResult result) throws SchemaException {
        if (computationMode != ComputationMode.PREVIEW) {
            return true;
        }
        Boolean valueFromGuiConfig = getExpandRolesFromGuiConfig();
        if (valueFromGuiConfig != null) {
            return valueFromGuiConfig;
        } else {
            return getExpandRolesDefaultValue(result);
        }
    }

    @Nullable
    private Boolean getExpandRolesFromGuiConfig() {
        MidPointPrincipal principal;
        try {
            principal = SecurityUtil.getPrincipal();
        } catch (SecurityViolationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get midPoint principal", e);
            return null;
        }
        if (principal instanceof GuiProfiledPrincipal) {
            return ((GuiProfiledPrincipal) principal).getCompiledGuiProfile().isExpandRolesOnApprovalPreview();
        } else {
            return null;
        }
    }

    private boolean getExpandRolesDefaultValue(OperationResult result) throws SchemaException {
        PrismObject<SystemConfigurationType> config = systemObjectCache.getSystemConfiguration(result);
        return config != null && config.asObjectable().getWorkflowConfiguration() != null &&
                Boolean.TRUE.equals(config.asObjectable().getWorkflowConfiguration().isDefaultExpandRolesOnPreview());
    }

    public String evaluateAutoCompleteExpression(ApprovalStageDefinitionType stageDef, VariablesMap variables,
            Task opTask, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException, SubscriptionComplianceException {
        List<String> outcomes = evaluationHelper.evaluateExpression(
                stageDef.getAutomaticallyCompleted(), variables,
                "automatic completion expression", String.class,
                DOMUtil.XSD_STRING, false, createOutcomeConvertor(), opTask, result);
        LOGGER.trace("Pre-completed = {} for stage {}", outcomes, stageDef);
        Set<String> distinctOutcomes = new HashSet<>(outcomes);
        if (distinctOutcomes.isEmpty()) {
            return null;
        } else if (distinctOutcomes.size() == 1) {
            return distinctOutcomes.iterator().next();
        } else {
            throw new IllegalStateException("Ambiguous result from 'automatically completed' expression: " + distinctOutcomes);
        }
    }

    private Function<Object, Object> createOutcomeConvertor() {
        return (o) -> {
            if (o == null || o instanceof String) {
                return o;
            } else if (o instanceof ApprovalLevelOutcomeType) {
                return ApprovalUtils.toUri((ApprovalLevelOutcomeType) o);
            } else if (o instanceof QName) {
                return QNameUtil.qNameToUri((QName) o);
            } else {
                return o; // let someone else complain about this
            }
        };
    }

    private Set<ObjectReferenceType> expandGroups(Set<ObjectReferenceType> approverRefs) {
        Set<ObjectReferenceType> rv = new HashSet<>();
        for (ObjectReferenceType approverRef : approverRefs) {
            Class<? extends Containerable> clazz =
                    prismContext.getSchemaRegistry().getCompileTimeClassForObjectTypeRequired(approverRef.getType());
            if (UserType.class.isAssignableFrom(clazz)) {
                rv.add(approverRef.clone());
            } else if (AbstractRoleType.class.isAssignableFrom(clazz)) {
                rv.addAll(expandAbstractRole(approverRef));
            } else {
                LOGGER.warn("Unexpected type {} for approver: {}", clazz, approverRef);
                rv.add(approverRef.clone());
            }
        }
        return rv;
    }

    private Collection<ObjectReferenceType> expandAbstractRole(ObjectReferenceType approverRef) {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(approverRef.asReferenceValue())
                .build();
        try {
            return repositoryService
                    .searchObjects(UserType.class, query, null, new OperationResult("dummy")) // FIXME op. result!!
                    .stream()
                    .map(o -> ObjectTypeUtil.createObjectRef(o))
                    .collect(Collectors.toList());
        } catch (SchemaException e) {
            throw new SystemException("Couldn't resolve " + approverRef + ": " + e.getMessage(), e);
        }
    }

}

