/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import static com.evolveum.midpoint.schema.traces.TraceUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathParserTemp;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

@Experimental
public enum OpType {

    CLOCKWORK_RUN(OperationKindType.CLOCKWORK_EXECUTION, "Clockwork run", "com.evolveum.midpoint.model.impl.lens.Clockwork.run",
            "Clockwork run - ${t:focusName}"),

    CLOCKWORK_CLICK(OperationKindType.CLOCKWORK_CLICK, "Clockwork click", "com.evolveum.midpoint.model.impl.lens.Clockwork.click",
            "Clockwork click (#${m:getClickNumber})"),

    PROJECTOR_PROJECT(OperationKindType.PROJECTOR_EXECUTION, "Projector project", "com.evolveum.midpoint.model.impl.lens.projector.Projector.project"),

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

    ASSIGNMENT_EVALUATION(OperationKindType.ASSIGNMENT_EVALUATION, "Assignment evaluation",
            "com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentTripleEvaluator.evaluateAssignment",
            "Assignment evaluation (→ ${c:assignmentTargetName})"),

    PROJECTOR_COMPONENT_OTHER(OperationKindType.OTHER,"Projector component",
            "com.evolveum.midpoint.model.impl.lens.projector.Projector.*"),

    CLOCKWORK_METHOD(OperationKindType.OTHER,"Clockwork method",
            "com.evolveum.midpoint.model.impl.lens.Clockwork.*"),

    MAPPING_EVALUATION(OperationKindType.MAPPING_EVALUATION,"Mapping evaluation",
            "com.evolveum.midpoint.model.common.mapping.MappingImpl.evaluate",
            "Mapping: ${m:getMappingInfo}"),

    MAPPING_PREPARATION(OperationKindType.OTHER,"Mapping preparation",
            "com.evolveum.midpoint.model.common.mapping.MappingImpl.prepare"),

    MAPPING_EVALUATION_PREPARED(OperationKindType.OTHER,"Prepared mapping evaluation",
            "com.evolveum.midpoint.model.common.mapping.MappingImpl.evaluatePrepared"),

    TRANSFORMATION_EXPRESSION_EVALUATION(OperationKindType.OTHER, "Transformation expression evaluation",
            "com.evolveum.midpoint.model.common.expression.evaluator.transformation.AbstractValueTransformationExpressionEvaluator.evaluate",
            "Transformation expression evaluation (${t:localContextDescription})"),

    VALUE_TUPLE_TRANSFORMATION(OperationKindType.OTHER, "Value tuple transformation",
            "com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTupleTransformation.evaluate",
            "Value tuple transformation ${m:getValueTupleTransformationDescription}"),

    VALUE_METADATA_COMPUTATION(OperationKindType.OTHER, "Value metadata computation",
            "com.evolveum.midpoint.model.common.mapping.metadata.ValueMetadataComputation.execute",
            "Value metadata computation → ${r:summary}"),

    ITEM_CONSOLIDATION(OperationKindType.OTHER, "Consolidation (item)",
            "com.evolveum.midpoint.model.impl.lens.IvwoConsolidator.consolidateToDelta",
            "Consolidation: ${m:getItemConsolidationInfo}"),

    SCRIPT_EXECUTION (OperationKindType.SCRIPT_EVALUATION, "Script evaluation",
            "com.evolveum.midpoint.model.common.expression.script.ScriptExpression.evaluate"),

    CHANGE_EXECUTION (OperationKindType.OTHER,"Change execution",
            "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.execute"),

    FOCUS_CHANGE_EXECUTION (OperationKindType.FOCUS_CHANGE_EXECUTION,"Focus change execution",
            "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.execute.focus.*"),

    PROJECTION_CHANGE_EXECUTION (OperationKindType.PROJECTION_CHANGE_EXECUTION,"Projection change execution",
            "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.execute.projection.*"),

    CHANGE_EXECUTION_DELTA (OperationKindType.OTHER,"Change execution - delta",
            "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.executeDelta"),

    CHANGE_EXECUTION_OTHER (OperationKindType.OTHER,"Change execution - other",
            "com.evolveum.midpoint.model.impl.lens.ChangeExecutor.*"),

    FOCUS_LOAD (OperationKindType.FOCUS_LOAD, "Focus load",
            result -> isLoadedFromRepository(result),
            "com.evolveum.midpoint.model.impl.lens.projector.ContextLoader.determineFocusContext",
            null),

    FOCUS_LOAD_CHECK (OperationKindType.FOCUS_LOAD_CHECK,"Focus load check",
            "com.evolveum.midpoint.model.impl.lens.projector.ContextLoader.determineFocusContext"),

    SHADOW_LOAD (OperationKindType.OTHER,"Shadow load",
            "com.evolveum.midpoint.model.impl.lens.projector.ContextLoader.loadProjection"),

    FULL_PROJECTION_LOAD (OperationKindType.FULL_PROJECTION_LOAD, "Full projection load",
            "com.evolveum.midpoint.model.impl.lens.projector.ContextLoader.loadFullShadow"),

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
    @SuppressWarnings("unused")
    private final List<String> patterns;
    private final List<Pattern> compiledPatterns;
    private final String nameTemplate;

    OpType(OperationKindType kind, String label, Function<OperationResultType, Boolean> predicate, String pattern, String nameTemplate) {
        this.kind = kind;
        this.label = label;
        this.predicate = predicate;
        this.patterns = Collections.singletonList(pattern);
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
        this(kind, label, null, pattern, null);
    }

    OpType(OperationKindType kind, String label, String pattern, String nameTemplate) {
        this(kind, label, null, pattern, nameTemplate);
    }

    public String getFormattedName(OpNode node) {

        if (nameTemplate != null) {
            return expandTemplate(node);
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
        } else if ("com.evolveum.midpoint.model.common.expression.script.ScriptExpression.evaluate".equals(operation)) {
            return "Script: " + getContext(opResult, "context");
        } else if ("com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator.evaluateFromSegment".equals(operation)) {
            String srcName = getContext(opResult, "segmentSourceName");
            String tgtName = getContext(opResult, "segmentTargetName");
            return "Segment: " + (srcName != null ? srcName + " " : "") + " → " + (tgtName != null ? " " + tgtName : "");
        } else if ("com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator.evaluate".equals(operation)) {
            String tgtName = getContext(opResult, "assignmentTargetName");
            return "AssignmentEvaluator.evaluate" + (tgtName != null ? " (→ " + tgtName + ")" : "");
        } else if ("com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentTripleEvaluator.evaluateAssignment".equals(operation)) {
            String tgtName = getContext(opResult, "assignmentTargetName");
            return "AssignmentTripleEvaluator.evaluateAssignment" + (tgtName != null ? " (→ " + tgtName + ")" : "");
        } else if ("com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleProcessor.evaluateRule".equals(operation)) {
            String triggeredString = getReturn(opResult, "triggered");
            int enabledActions = TraceUtil.getReturnsAsStringList(opResult, "enabledActions").size();
            String triggeredSuffix = "true".equals(triggeredString) ? " # (" + enabledActions + ")" : "";
            return "Evaluate rule: " + getParameter(opResult, "policyRule") + triggeredSuffix;
        }

        switch (this) {
        case CLOCKWORK_RUN: return "Clockwork run";
        case CLOCKWORK_CLICK: return "Clockwork click";
        case PROJECTOR_PROJECT: return "Projector";
        case PROJECTOR_COMPONENT_OTHER:
            if ("projection".equals(last)) {
                return "Projector projection: " + getParameter(opResult, "resourceName");
            } else {
                return "Projector " + last;
            }
        case CLOCKWORK_METHOD: return "Clockwork " + last;

        case MAPPING_PREPARATION: return "Mapping preparation";
        case MAPPING_EVALUATION_PREPARED: return "Prepared mapping evaluation";

        // TODO script
        case CHANGE_EXECUTION: return "Change execution";
        case FOCUS_CHANGE_EXECUTION: return "Change execution for focus (" + last + ")";
        case PROJECTION_CHANGE_EXECUTION: return "Change execution for focus (" + last + ")";
        case CHANGE_EXECUTION_DELTA: return "Delta execution";
        case CHANGE_EXECUTION_OTHER: return "Change execution - " + last;
        case REPOSITORY: return "Repository " + last + commaQualifiers;
        case REPOSITORY_CACHE:
            return getRepoCacheOpDescription(node, opResult, last, commaQualifiers);
        case PROVISIONING_API: return "Provisioning " + last + commaQualifiers;
        case PROVISIONING_INTERNAL: return getLastTwo(operation) + commaQualifiers;
        case FOCUS_LOAD: return "Focus load";
        case FOCUS_LOAD_CHECK: return "Focus load check (" + node.getResultComment() + ")";
        case SHADOW_LOAD: return "Shadow load";
        case MODEL_OTHER:
        case OTHER:
            return getLastTwo(operation) + commaQualifiers;
        }
        return opResult.getOperation() + (qualifiers.isEmpty() ? "" : " (" + qualifiers + ")");
    }

    private String expandTemplate(OpNode node) {
        return new StringSubstitutor(createResolver(node), "${", "}", '\\')
                .replace(nameTemplate);
    }

    private StringLookup createResolver(OpNode node) {
        return spec -> {
            String prefix;
            String suffix;
            String key;

            String[] parts = spec.split(":");
            if (parts.length == 1) {
                prefix = "";
                key = spec;
                suffix = "";
            } else if (parts.length == 2) {
                prefix = parts[0];
                key = parts[1];
                suffix = "";
            } else if (parts.length == 3) {
                prefix = parts[0];
                key = parts[1];
                suffix = parts[2];
            } else {
                return "???";
            }

            List<String> values = new ArrayList<>();
            if (prefix.isEmpty() || prefix.equals("p")) {
                collectMatchingParams(values, key, node.getResult().getParams());
            }
            if (prefix.isEmpty() || prefix.equals("c")) {
                collectMatchingParams(values, key, node.getResult().getContext());
            }
            if (prefix.isEmpty() || prefix.equals("r")) {
                collectMatchingParams(values, key, node.getResult().getReturns());
            }
            if (prefix.isEmpty() || prefix.equals("t")) {
                collectMatchingValues(values, key, node.getResult().getTrace());
            }
            if (prefix.equals("m")) {
                collectFromMethod(values, key, node);
            }
            return String.join(", ", postprocess(values, suffix));
        };
    }

    private void collectFromMethod(List<String> values, String key, OpNode node) {
        try {
            Object rv = MethodUtils.invokeExactMethod(node, key);
            if (rv instanceof Collection) {
                for (Object o : (Collection) rv) {
                    values.add(String.valueOf(o));
                }
            } else if (rv != null) {
                values.add(String.valueOf(rv));
            }
        } catch (Throwable t) {
            values.add("??? " + t.getMessage());
        }
    }

    private void collectMatchingValues(List<String> values, String path, List<TraceType> traces) {
        UniformItemPath itemPath = ItemPathParserTemp.parseFromString(path); // FIXME (hack)
        for (TraceType trace : traces) {
            PrismProperty property = trace.asPrismContainerValue().findProperty(itemPath);
            if (property != null) {
                for (Object realValue : property.getRealValues()) {
                    values.add(String.valueOf(realValue));
                }
            }
        }
    }

    private void collectMatchingParams(List<String> values, String key, ParamsType params) {
        int colon = key.indexOf(':');
        String processing;
        String realKey;
        if (colon >= 0) {
            realKey = key.substring(0, colon);
            processing = key.substring(colon + 1);
        } else {
            realKey = key;
            processing = "";
        }
        List<String> rawStringValues = asStringList(selectByKey(params, realKey));
        values.addAll(postprocess(rawStringValues, processing));
    }

    private List<String> postprocess(List<String> raw, String processing) {
        return raw.stream()
                .map(s -> postprocess(s, processing))
                .collect(Collectors.toList());
    }

    private String postprocess(String string, String processing) {
        if (processing.contains("L")) {
            string = string.toLowerCase();
        }
        return string;
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

    private String getLast(String operation) {
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
