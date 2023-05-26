/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.visualizer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.QNameUtil.getLocalPart;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.xml.bind.JAXBElement;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.traces.OpNode;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.DeltaSetTripleType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaItemType;

public class MappingEvaluationVisualizer extends BaseVisualizer {

    MappingEvaluationVisualizer(PrismContext prismContext) {
        super(prismContext);
    }

    @Override
    public void visualize(StringBuilder sb, OpNode node, int indent) {
        GenericTraceVisualizationType generic = defaultIfNull(node.getGenericVisualization(), GenericTraceVisualizationType.ONE_LINE);
        TraceDataSelectionType data = defaultIfNull(node.getDataSelection(), TraceDataSelectionType.ALL); // todo other default?

        boolean oneLine = generic == GenericTraceVisualizationType.ONE_LINE;
        MappingEvaluationTraceType trace = node.getTrace(MappingEvaluationTraceType.class);
        MappingKindType kind = trace != null ? trace.getMappingKind() : null;
        AbstractMappingType mapping = trace != null ? trace.getMapping() : null;
        String name = mapping != null ? mapping.getName() : null;

        String nullPrefix = indent(sb, node, indent);
        if (trace != null) {
            List<String> sources = getSources(mapping, trace);
            String target = getTarget(mapping, trace);
            sb.append("Mapping - ").append(kind);
            if (trace.getContainingObjectRef() != null && trace.getContainingObjectRef().getTargetName() != null) {
                sb.append(" (").append(trace.getContainingObjectRef().getTargetName().getOrig()).append(")");
            }
            sb.append(": ").append(emptyIfNull(name));
            if (!sources.isEmpty() || target != null) {
                if (name != null) {
                    sb.append(" (");
                }
                sb.append(String.join(", ", sources));
                if (!sources.isEmpty()) {
                    sb.append(" ");
                }
                sb.append("->");
                if (target != null) {
                    sb.append(" ").append(target);
                }
                if (name != null) {
                    sb.append(")");
                }
            }
            if (oneLine) {
                if (trace.isConditionResultOld() != null || trace.isConditionResultNew() != null) {
                    if (!trace.isConditionResultOld() || !trace.isConditionResultNew()) {
                        sb.append(" [c: ").append(trace.isConditionResultOld()).append("->").append(trace.isConditionResultNew()).append("]");
                    }
                }
                if (Boolean.FALSE.equals(trace.isTimeConstraintValid())) {
                    sb.append(" [time constraint not valid");
                    if (trace.getNextRecomputeTime() != null) {
                        sb.append("; deferred to ").append(trace.getNextRecomputeTime());
                    }
                    sb.append("]");
                }
            } else {
                // will be shown in detailed output
            }
        } else {
            sb.append(node.getOperationNameFormatted()); // todo more context from op.result
        }
        appendInvocationIdAndDuration(sb, node);
        sb.append("\n");

        if (trace != null && generic != GenericTraceVisualizationType.ONE_LINE) {
            String prefix = nullPrefix + " | ";
            MappingStrengthType strength = getStrength(mapping);
            if (generic != GenericTraceVisualizationType.BRIEF || strength != MappingStrengthType.NORMAL) {
                sb.append(prefix).append("Strength: ").append(strength).append("\n");
            }
            if (!isAuthoritative(mapping)) {
                sb.append(prefix).append("Not authoritative\n");
            }
            if (isExclusive(mapping)) {
                sb.append(prefix).append("Exclusive\n");
            }
            for (MappingSourceEvaluationTraceType source : trace.getSource()) {
                appendWithPrefix(sb, prefix, shortSourceDump(source, generic));
            }
            if (trace.isConditionResultOld() != null || trace.isConditionResultNew() != null) {
                if (generic != GenericTraceVisualizationType.BRIEF ||
                        !Boolean.TRUE.equals(trace.isConditionResultOld()) ||
                        !Boolean.TRUE.equals(trace.isConditionResultNew())) {
                    appendWithPrefix(sb, prefix, "Condition: " + trace.isConditionResultOld() + " -> " + trace.isConditionResultNew());
                }
            }
            if (generic != GenericTraceVisualizationType.BRIEF ||
                    !Boolean.TRUE.equals(trace.isTimeConstraintValid()) ||
                    trace.getNextRecomputeTime() != null) {
                appendWithPrefix(sb, prefix, "Time validity: " + trace.isTimeConstraintValid() +
                        (trace.getNextRecomputeTime() != null ? "; next recompute time is " + trace.getNextRecomputeTime() : ""));
            }
            appendWithPrefix(sb, prefix, "Output: " + shortOutputDump(trace.getOutput(), generic));
            appendWithPrefix(sb, prefix, "Expression: " + shortExpressionDebugDump(mapping, generic));

            if (generic == GenericTraceVisualizationType.FULL) {
                appendWithPrefix(sb, prefix, ((MappingEvaluationTraceType) (node.getResult().getTrace().get(0))).getTextTrace());
            }
        }
    }

    private String shortSourceDump(MappingSourceEvaluationTraceType source, GenericTraceVisualizationType generic) {
        StringBuilder sb = new StringBuilder("Input ");
        sb.append(getLocalPart(source.getName())).append(": ");
        ItemDeltaItemType idi = source.getItemDeltaItem();
        if (idi != null) {
            if (isSingleLine(idi, generic)) {
                sb.append(formatSingleLine(idi, generic)).append("\n");
            } else {
                sb.append("\n").append(formatMultiLine(idi, 1));
            }
        } else {
            sb.append("(no value)\n");
        }
        return sb.toString();
    }

    private String shortOutputDump(DeltaSetTripleType output, GenericTraceVisualizationType generic) {
        return formatSingleLine(output, generic);
    }

    private boolean isAuthoritative(AbstractMappingType mapping) {
        if (mapping != null) {
            return defaultIfNull(mapping.isAuthoritative(), true);
        } else {
            return true;
        }
    }

    private boolean isExclusive(AbstractMappingType mapping) {
        if (mapping != null) {
            return defaultIfNull(mapping.isExclusive(), false);
        } else {
            return false;
        }
    }

    private MappingStrengthType getStrength(AbstractMappingType mapping) {
        if (mapping != null) {
            return defaultIfNull(mapping.getStrength(), MappingStrengthType.NORMAL);
        } else {
            return MappingStrengthType.NORMAL;
        }
    }

    private String getTarget(AbstractMappingType mapping, MappingEvaluationTraceType trace) {
        if (mapping != null && mapping.getTarget() != null) {
            return String.valueOf(mapping.getTarget().getPath());
        } else if (trace.getImplicitTargetPath() != null) {
            return trace.getImplicitTargetPath().toString();
        } else {
            return null;
        }
    }

    private List<String> getSources(AbstractMappingType mapping, MappingEvaluationTraceType trace) {
        if (mapping == null || mapping.getSource().isEmpty()) {
            if (trace.getImplicitSourcePath() != null) {
                return singletonList(String.valueOf(trace.getImplicitSourcePath()));
            } else {
                return emptyList();
            }
        } else {
            return mapping.getSource().stream()
                    .map(src -> String.valueOf(src.getPath()))
                    .collect(Collectors.toList());
        }
    }

    private String shortExpressionDebugDump(AbstractMappingType mapping, GenericTraceVisualizationType level) {
        List<JAXBElement<?>> evaluators;
        if (mapping != null && mapping.getExpression() != null) {
            evaluators = mapping.getExpression().getExpressionEvaluator();
        } else {
            evaluators = emptyList();
        }
        if (evaluators.isEmpty()) {
            return "(none)";
        } else if (evaluators.size() == 1) {
            return shortEvaluatorDebugDump(evaluators.get(0), level);
        } else {
            return evaluators.stream()
                    .map(evaluator -> shortEvaluatorDebugDump(evaluator, level))
                    .collect(Collectors.joining(", ", "[", "]"));
        }
    }

}
