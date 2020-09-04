/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import static java.util.Collections.singletonList;

import static com.evolveum.midpoint.schema.traces.TraceUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Experimental
public enum OpType {

    CLOCKWORK_RUN(OperationKindType.CLOCKWORK_EXECUTION, "Clockwork run", "com.evolveum.midpoint.model.impl.lens.Clockwork.run",
            "Clockwork run - ${t:focusName}"),

    CLOCKWORK_CLICK(OperationKindType.CLOCKWORK_CLICK, "Clockwork click", "com.evolveum.midpoint.model.impl.lens.Clockwork.click",
            "Clockwork click (#${m:getClickNumber})"),

    PROJECTOR_PROJECT(OperationKindType.PROJECTOR_EXECUTION, "Projector project",
            "com.evolveum.midpoint.model.impl.lens.projector.Projector.project",
            "Projector"),

    MODEL_AUDIT(OperationKindType.MODEL_AUDIT, "Audit", "com.evolveum.midpoint.model.impl.util.AuditHelper.audit",
            "Audit ${p:stage:L} - ${p:eventType} - ${p:targetName}"),

    PROJECTOR_INBOUND(OperationKindType.OTHER, "Inbounds", "com.evolveum.midpoint.model.impl.lens.projector.Projector.inbound",
            "Inbounds (${m:getMappingsCount})"),

    PROJECTOR_TEMPLATE_BEFORE_ASSIGNMENTS(OperationKindType.OTHER, "Template before assignments",
            "com.evolveum.midpoint.model.impl.lens.projector.Projector.objectTemplateBeforeAssignments",
            "Template before assignments (${m:getMappingsCount})"),

    PROJECTOR_TEMPLATE_AFTER_ASSIGNMENTS(OperationKindType.OTHER, "Template after assignments",
            "com.evolveum.midpoint.model.impl.lens.projector.Projector.objectTemplateAfterAssignments",
            "Template after assignments (${m:getMappingsCount})"),

    PROJECTOR_ASSIGNMENTS(OperationKindType.OTHER, "Assignments",
            "com.evolveum.midpoint.model.impl.lens.projector.Projector.assignments",
            "Assignments (${m:getAssignmentEvaluationsCount})"),

    ASSIGNMENT_EVALUATION_OUTER(OperationKindType.OTHER, "Assignment evaluation (outer)",
            "com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentTripleEvaluator.evaluateAssignment",
            "Assignment evaluation: → ${c:assignmentTargetName}"),

    ASSIGNMENT_EVALUATION(OperationKindType.ASSIGNMENT_EVALUATION, "Assignment evaluation", null,
            Arrays.asList("com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator.evaluate",
                    "com.evolveum.midpoint.model.impl.lens.assignments.AssignmentEvaluator.evaluate"),
            "Assignment evaluation${m:getModeInfo}: ${m:getAssignmentInfo}"),

    ASSIGNMENT_SEGMENT_EVALUATION(OperationKindType.OTHER, "Assignment segment evaluation", null,
            Arrays.asList("com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator.evaluateFromSegment",
                "com.evolveum.midpoint.model.impl.lens.assignments.PathSegmentEvaluation.evaluate"),
            "${m:getSegmentLabel}"),

    PROJECTOR_FOCUS_POLICY_RULES(OperationKindType.OTHER, "Focus policy rules",
            "com.evolveum.midpoint.model.impl.lens.projector.Projector.focusPolicyRules",
            "Focus policy rules ${m:getFocusPolicyRulesInfo}"),

    POLICY_RULE_EVALUATION(OperationKindType.OTHER, "Policy rule evaluation",
            "com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleProcessor.evaluateRule",
            "Rule evaluation: ${m:getRuleInfo}"),

    POLICY_CONSTRAINT_EVALUATION(OperationKindType.OTHER, "Policy constraint evaluation",
            "com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators.*ConstraintEvaluator.evaluate",
            "Constraint evaluation: ${m:getConstraintInfo}"),

    PROJECTION_ACTIVATION(OperationKindType.OTHER,"Projection activation",
            "com.evolveum.midpoint.model.impl.lens.projector.ActivationProcessor.projectionActivation",
            "Activation: ${p:resourceName} ${m:getDecisionInfo}"),

    PROJECTOR_PROJECTION(OperationKindType.OTHER,"Projector projection",
            "com.evolveum.midpoint.model.impl.lens.projector.Projector.projection",
            "Projector projection: ${p:resourceName}"),

    PROJECTOR_COMPONENT_OTHER(OperationKindType.OTHER,"Projector component",
            "com.evolveum.midpoint.model.impl.lens.projector.Projector.*"),

    CLOCKWORK_METHOD(OperationKindType.OTHER,"Clockwork method",
            "com.evolveum.midpoint.model.impl.lens.Clockwork.*"),

    MAPPING_EVALUATION(OperationKindType.MAPPING_EVALUATION,"Mapping evaluation",
            "com.evolveum.midpoint.model.common.mapping.MappingImpl.evaluate",
            "Mapping: ${m:getMappingInfo}"),

    MAPPING_TIME_VALIDITY_EVALUATION(OperationKindType.MAPPING_EVALUATION,"Mapping time validity evaluation",
            "com.evolveum.midpoint.model.common.mapping.MappingImpl.evaluateTimeValidity",
            "Mapping time validity: ${m:getMappingInfo}"),

    MAPPING_PREPARATION(OperationKindType.OTHER,"Mapping preparation",
            "com.evolveum.midpoint.model.common.mapping.MappingImpl.prepare",
            "Mapping preparation"),

    MAPPING_EVALUATION_PREPARED(OperationKindType.OTHER,"Prepared mapping evaluation",
            "com.evolveum.midpoint.model.common.mapping.MappingImpl.evaluatePrepared",
            "Prepared mapping evaluation"),

    TRANSFORMATION_EXPRESSION_EVALUATION(OperationKindType.OTHER, "Transformation expression evaluation",
            "com.evolveum.midpoint.model.common.expression.evaluator.transformation.AbstractValueTransformationExpressionEvaluator.evaluate",
            "Transformation expression evaluation (${m:getContextDescription})"),

    VALUE_TUPLE_TRANSFORMATION(OperationKindType.OTHER, "Value tuple transformation",
            "com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTupleTransformation.evaluate",
            "Value tuple transformation ${m:getValueTupleTransformationDescription}"),

    VALUE_METADATA_COMPUTATION(OperationKindType.OTHER, "Value metadata computation",
            "com.evolveum.midpoint.model.common.mapping.metadata.ValueMetadataComputation.execute",
            "Value metadata computation ⇒ ${r:summary}"),

    ITEM_CONSOLIDATION(OperationKindType.OTHER, "Consolidation (item)",
            "com.evolveum.midpoint.model.impl.lens.IvwoConsolidator.consolidateToDelta",
            "Consolidation: ${m:getItemConsolidationInfo}"),

    SCRIPT_EXECUTION(OperationKindType.SCRIPT_EVALUATION, "Script evaluation",
            "com.evolveum.midpoint.model.common.expression.script.ScriptExpression.evaluate"),

    CHANGE_EXECUTION(OperationKindType.OTHER,"Change execution",
            "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.execute",
            "Change execution"),

    FOCUS_CHANGE_EXECUTION(OperationKindType.FOCUS_CHANGE_EXECUTION,"Focus change execution",
            "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.execute.focus.*",
            "Focus change execution: ${m:getInfo}"),

    PROJECTION_CHANGE_EXECUTION(OperationKindType.PROJECTION_CHANGE_EXECUTION,"Projection change execution",
            "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.execute.projection.*",
            "Projection change execution on ${m:getResourceName}: ${m:getInfo}"),

    UPDATE_SHADOW_SITUATION(OperationKindType.OTHER, "Projection change: update shadow situation",
            "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.updateSituationInShadow",
            "Update shadow situation: ${m:getInfo}"),

    LINK_UNLINK_SHADOW(OperationKindType.OTHER, "Projection change: link/unlink shadow", null,
            Arrays.asList("com.evolveum.midpoint.model.impl.lens.ChangeExecutor.linkShadow",
                    "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.unlinkShadow"),
            "${m:getLabel}"),

    CHANGE_EXECUTION_DELTA(OperationKindType.OTHER,"Delta execution",
            "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.executeDelta",
            "Delta execution"),

    @Deprecated
    CHANGE_EXECUTION_OTHER(OperationKindType.OTHER,"Change execution - other",
            "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.*"),

    FOCUS_REPOSITORY_LOAD(OperationKindType.FOCUS_LOAD, "Focus load",
            OpType::isLoadedFromRepository,
            singletonList("com.evolveum.midpoint.model.impl.lens.projector.ContextLoader.determineFocusContext"),
            "Focus load from repository"),

    FOCUS_LOAD_CHECK (OperationKindType.FOCUS_LOAD_CHECK,"Focus load check",
            "com.evolveum.midpoint.model.impl.lens.projector.ContextLoader.determineFocusContext"),

    SHADOW_LOAD (OperationKindType.OTHER,"Shadow load",
            "com.evolveum.midpoint.model.impl.lens.projector.ContextLoader.loadProjection",
            "Shadow load"),

    FULL_PROJECTION_LOAD (OperationKindType.FULL_PROJECTION_LOAD, "Full projection load",
            "com.evolveum.midpoint.model.impl.lens.projector.ContextLoader.loadFullShadow",
            "Projection load: ${m:getInfo}"),

    MODEL_OTHER (OperationKindType.OTHER,"Model - other",
            "com.evolveum.midpoint.model.*"),

    PROVISIONING_API (OperationKindType.PROVISIONING,"Provisioning (API)", "com.evolveum.midpoint.provisioning.api.*"),

    PROVISIONING_INTERNAL (OperationKindType.OTHER,"Provisioning (internal)", "com.evolveum.midpoint.provisioning.impl.*"),

    REPOSITORY (OperationKindType.REPOSITORY, "Repository", "com.evolveum.midpoint.repo.api.RepositoryService.*"),

    REPOSITORY_CACHE (OperationKindType.REPOSITORY_CACHE, "Repository cache", "com.evolveum.midpoint.repo.cache.RepositoryCache.*"),

    CONNECTOR (OperationKindType.CONNECTOR, "Connector", "com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance.*"),

    OTHER (OperationKindType.OTHER, "Other",
            "*");

    private static final String LOADED_FROM_REPOSITORY = "Loaded from repository"; // TODO

    private final OperationKindType kind;
    private final String label;
    private final Function<OperationResultType, Boolean> predicate;
    private final List<Pattern> compiledPatterns;
    private final String nameTemplate;

    OpType(OperationKindType kind, String label, Function<OperationResultType, Boolean> predicate, List<String> patterns, String nameTemplate) {
        this.kind = kind;
        this.label = label;
        this.predicate = predicate;
        this.compiledPatterns = new ArrayList<>();
        for (String aPattern : patterns) {
            String regex = toRegex(aPattern);
            compiledPatterns.add(Pattern.compile(regex));
        }
        this.nameTemplate = nameTemplate;
    }

    private static boolean isLoadedFromRepository(OperationResultType result) {
        return LOADED_FROM_REPOSITORY.equals(OpNode.getResultComment(result));
    }

    OpType(OperationKindType kind, String label, String pattern) {
        this(kind, label, null, singletonList(pattern), null);
    }

    OpType(OperationKindType kind, String label, String pattern, String nameTemplate) {
        this(kind, label, null, singletonList(pattern), nameTemplate);
    }

    public String getFormattedName(OpNode node) {

        if (nameTemplate != null) {
            return new TemplateExpander()
                    .expandTemplate(node, nameTemplate);
        }

        OperationResultType opResult = node.getResult();
        String operation = opResult.getOperation();
        String last = getLast(operation);
        String qualifiers = String.join("; ", opResult.getQualifier());
        String commaQualifiers = qualifiers.isEmpty() ? "" : " - " + qualifiers;

        if ("com.evolveum.midpoint.model.impl.lens.projector.ConsolidationProcessor.consolidateItem".equals(operation)) {
            return "Consolidating " + getParameter(opResult, "itemPath");
        } else if ("com.evolveum.midpoint.model.common.expression.evaluator.AbstractValueTransformationExpressionEvaluator.processValuesCombination".equals(operation)) {
            return "Processing value combination";
        } else if ("com.evolveum.midpoint.model.common.expression.evaluator.AbstractValueTransformationExpressionEvaluator.evaluateScriptExpression".equals(operation) ||
                "com.evolveum.midpoint.model.common.expression.evaluator.AbstractValueTransformationExpressionEvaluator.evaluateExpression".equals(operation)) {
            return "Evaluate: " + getContext(opResult, "context");
        }

        switch (this) {
            case PROJECTOR_COMPONENT_OTHER:
                return "Projector " + last;
            case CLOCKWORK_METHOD: return "Clockwork " + last;
            case CHANGE_EXECUTION_OTHER: return "Change execution - " + last;
            case REPOSITORY: return "Repository " + last + commaQualifiers;
            case REPOSITORY_CACHE:
                return getRepoCacheOpDescription(node, opResult, last, commaQualifiers);
            case PROVISIONING_API: return "Provisioning " + last + commaQualifiers;
            case PROVISIONING_INTERNAL: return getLastTwo(operation) + commaQualifiers;
            case FOCUS_LOAD_CHECK: return "Focus load check (" + node.getResultComment() + ")";
            case MODEL_OTHER:
            case OTHER:
                return getLastTwo(operation) + commaQualifiers;
        }
        return opResult.getOperation() + (qualifiers.isEmpty() ? "" : " (" + qualifiers + ")");
    }

    private String getRepoCacheOpDescription(OpNode node, OperationResultType opResult, String last, String commaQualifiers) {
        String postfix = "";
        if ("getObject".equals(last)) {
            postfix = " - " + getContext(opResult, "objectName");
        } else if ("searchObjects".equals(last) || "searchObjectsIterative".equals(last)) {
            String objectsFound = getReturn(opResult, "objectsFound");
            postfix = " - " + objectsFound + " obj(s)";
            RepositorySearchObjectsTraceType trace = getTrace(opResult, RepositorySearchObjectsTraceType.class);
            if (trace != null && trace.getObjectRef().size() == 1) {
                String name = getName(trace.getObjectRef().get(0));
                if (name != null) {
                    postfix += " - " + name;
                }
            }
        } else if ("addObject".equals(last)) {
            RepositoryAddTraceType add = getTrace(opResult, RepositoryAddTraceType.class);
            String name = add != null ? getName(add.getObjectRef()) : null;
            if (name != null) {
                postfix = " - " + name;
            }
        } else if ("modifyObject".equals(last)) {
            RepositoryModifyTraceType trace = getTrace(opResult, RepositoryModifyTraceType.class);
            if (trace != null) {
                postfix += " - " + trace.getModification().size() + " mod(s)";
                PrismObject<?> object = node.getTraceInfo().findObject(trace.getOid());
                if (object != null) {
                    postfix += " - " + PolyString.getOrig(object.getName());
                }
            }
        }
        return "Cache " + last + commaQualifiers + postfix;
    }

    private String getName(ObjectReferenceType ref) {
        if (ref != null && ref.getObject() != null) {
            return PolyString.getOrig(ref.getObject().getName());
        } else {
            return null;
        }
    }


    private String getLastTwo(String operation) {
        int i = StringUtils.lastOrdinalIndexOf(operation, ".", 2);
        if (i < 0) {
            return operation;
        } else {
            return operation.substring(i+1);
        }
    }

    // todo move somewhere
    public static String getLast(String operation) {
        return StringUtils.substringAfterLast(operation, ".");
    }

    private String toRegex(String pattern) {
        return pattern.replace(".", "\\.").replace("*", ".*");
    }

    public static OpType determine(OperationResultType operation) {
        for (OpType type : OpType.values()) {
            if (type.matches(operation)) {
                return type;
            }
        }
        return null;
    }

    private boolean matches(OperationResultType operation) {
        if (operation.getOperationKind() != null && operation.getOperationKind() != kind) {
            return false;
        }

        // not all kinds are unique, so we have to check also the patterns - FIXME

        for (Pattern pattern : compiledPatterns) {
            if (pattern.matcher(operation.getOperation()).matches()) {
                if (predicate == null || predicate.apply(operation)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getLabel() {
        return label;
    }

    public OperationKindType getKind() {
        return kind;
    }
}
