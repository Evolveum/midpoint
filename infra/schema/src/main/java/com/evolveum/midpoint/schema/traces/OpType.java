/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import static com.evolveum.midpoint.schema.traces.TraceUtil.*;

@Experimental
public enum OpType {

    CLOCKWORK_RUN(OperationKindType.CLOCKWORK_EXECUTION, "Clockwork run", "com.evolveum.midpoint.model.impl.lens.Clockwork.run"),
    CLOCKWORK_CLICK(OperationKindType.CLOCKWORK_CLICK, "Clockwork click", "com.evolveum.midpoint.model.impl.lens.Clockwork.click"),
    PROJECTOR_PROJECT(OperationKindType.PROJECTOR_EXECUTION, "Projector project", "com.evolveum.midpoint.model.impl.lens.projector.Projector.project"),
    PROJECTOR_COMPONENT(OperationKindType.OTHER,"Projector component",
            "com.evolveum.midpoint.model.impl.lens.projector.Projector.*"),
    CLOCKWORK_METHOD(OperationKindType.OTHER,"Clockwork method",
            "com.evolveum.midpoint.model.impl.lens.Clockwork.*"),
    MAPPING_EVALUATION(OperationKindType.MAPPING_EVALUATION,"Mapping evaluation",
            "com.evolveum.midpoint.model.common.mapping.MappingImpl.evaluate"),
    MAPPING_PREPARATION(OperationKindType.OTHER,"Mapping preparation",
            "com.evolveum.midpoint.model.common.mapping.MappingImpl.prepare"),
    MAPPING_EVALUATION_PREPARED (OperationKindType.OTHER,"Prepared mapping evaluation",
            "com.evolveum.midpoint.model.common.mapping.MappingImpl.evaluatePrepared"),
    SCRIPT_EXECUTION (OperationKindType.SCRIPT_EVALUATION, "Script evaluation",
            "xxxx"),
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
    FOCUS_LOAD (OperationKindType.FOCUS_LOAD,"Focus load",
            result -> isLoadedFromRepository(result),
            "com.evolveum.midpoint.model.impl.lens.projector.ContextLoader.determineFocusContext"),
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

    OpType(OperationKindType kind, String label, Function<OperationResultType, Boolean> predicate, String... patterns) {
        this.kind = kind;
        this.label = label;
        this.predicate = predicate;
        this.patterns = Arrays.asList(patterns);
        this.compiledPatterns = new ArrayList<>();
        for (String pattern : patterns) {
            String regex = toRegex(pattern);
            compiledPatterns.add(Pattern.compile(regex));
        }
    }

    private static boolean isLoadedFromRepository(OperationResultType result) {
        return LOADED_FROM_REPOSITORY.equals(OpNode.getResultComment(result));
    }

    OpType(OperationKindType kind, String label, String... patterns) {
        this(kind, label, null, patterns);
    }

    public String getFormattedName(OpNode node) {
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
            return "Segment: " + (srcName != null ? srcName + " " : "") + "->" + (tgtName != null ? " " + tgtName : "");
        } else if ("com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator.evaluate".equals(operation)) {
            String tgtName = getContext(opResult, "assignmentTargetName");
            return "AssignmentEvaluator.evaluate" + (tgtName != null ? " (-> " + tgtName + ")" : "");
        } else if ("com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentTripleEvaluator.evaluateAssignment".equals(operation)) {
            String tgtName = getContext(opResult, "assignmentTargetName");
            return "AssignmentTripleEvaluator.evaluateAssignment" + (tgtName != null ? " (-> " + tgtName + ")" : "");
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
        case PROJECTOR_COMPONENT:
            if ("projection".equals(last)) {
                return "Projector projection: " + getParameter(opResult, "resourceName");
            } else {
                return "Projector " + last;
            }
        case CLOCKWORK_METHOD: return "Clockwork " + last;

        case MAPPING_EVALUATION: return "Mapping: " + getMappingInfo(opResult);
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

    private String getRepoCacheOpDescription(OpNode node, OperationResultType opResult, String last, String commaQualifiers) {
        String postfix = "";
        if ("getObject".equals(last)) {
            postfix = " - " + getContext(opResult, "objectName");
        } else if ("searchObjects".equals(last) || "searchObjectsIterative".equals(last)) {
            String objectsFound = getReturn(opResult, "objectsFound");
            if (objectsFound != null) {
                postfix = " - " + objectsFound + " obj(s)";
            }
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

    private String getMappingInfo(OperationResultType result) {
        MappingEvaluationTraceType trace = getTrace(result, MappingEvaluationTraceType.class);
        String context = getContext(result, "context");
        if (trace != null) {
            AbstractMappingType mapping = trace.getMapping();
            if (mapping != null) {
                if (mapping.getName() != null) {
                    return mapping.getName();
                } else {
                    String sources = mapping.getSource().stream()
                            .map(source -> stringifyPath(source.getPath()))
                            .collect(Collectors.joining(", "));
                    if (mapping.getTarget() != null && mapping.getTarget().getPath() != null) {
                        String sourcesPlusSpace = sources.isEmpty() ? "" : sources + " ";
                        return sourcesPlusSpace + "-> " + stringifyPath(mapping.getTarget().getPath());
                    } else {
                        return context + (sources.isEmpty() ? "" : " <- " + sources);
                    }
                }
            }
        }
        return context;
    }

    private String stringifyPath(ItemPathType pathBean) {
        if (pathBean != null) {
            ItemPath path = pathBean.getItemPath();
            return path.stripVariableSegment().toString();
        } else {
            return "(no path)";
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
