/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processes.common;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType.REJECT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.AUTO_COMPLETION_CONDITION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.NO_ASSIGNEES_FOUND;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
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
                    rv.approverRefs.addAll(evaluationHelper.evaluateRefExpressions(stageDef.getApproverExpression(), variablesMap,
                            "resolving approver expression", opTask, opResult));
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
            Task opTask, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        List<String> outcomes = evaluationHelper.evaluateExpression(stageDef.getAutomaticallyCompleted(), variables,
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
                //throw new IllegalArgumentException("Couldn't create an URI from " + o);
                return o;        // let someone else complain about this
            }
        };
    }

    private Set<ObjectReferenceType> expandGroups(Set<ObjectReferenceType> approverRefs) {
        Set<ObjectReferenceType> rv = new HashSet<>();
        for (ObjectReferenceType approverRef : approverRefs) {
            Class<? extends Containerable> clazz =
                    prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(approverRef.getType());
            if (clazz == null) {
                throw new IllegalStateException("Unknown object type " + approverRef.getType());
            }
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

