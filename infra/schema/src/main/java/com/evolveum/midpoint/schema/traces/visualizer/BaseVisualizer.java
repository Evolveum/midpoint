/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.visualizer;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.traces.OpNode;

import com.evolveum.midpoint.schema.traces.PerformanceCategory;
import com.evolveum.midpoint.schema.traces.PerformanceCategoryInfo;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericTraceVisualizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.xml.bind.JAXBElement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.QNameUtil.getLocalPart;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

@Experimental
abstract class BaseVisualizer implements Visualizer {

    private static final int MAX_CODE_CHARS = 120;

    private static final String SEPARATOR_FIRST = " | ";
    private static final String SEPARATOR_CONTINUATION = " | ";

    @Autowired TraceVisualizerRegistry registry;

    PrismContext prismContext;

    BaseVisualizer(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    void defaultOneLineVisualization(StringBuilder sb, OpNode node, int indent) {
        indent(sb, node, indent);
        sb.append(node.getOperationNameFormatted());
        appendInvocationIdAndDuration(sb, node);
        sb.append("\n");
    }

    void appendInvocationIdAndDuration(StringBuilder sb, OpNode node) {
        if (node.isShowInvocationId()) {
            sb.append(" #").append(node.getInvocationId());
        }
        if (node.isShowDurationAfter()) {
            sb.append(" - ").append(node.getMillisecondsFormatted()).append(" ms");
        }
    }

    String indent(StringBuilder sb, OpNode opNode, int indent) {
        String nullPrefix = printPerfData(sb, opNode);
        String indentText = StringUtils.repeat("  ", indent);
        sb.append(indentText);
        return nullPrefix + indentText;
    }

    private String printPerfData(StringBuilder sb, OpNode opNode) {
        StringBuilder nullPrefix = new StringBuilder();
        boolean showTotals = opNode.showTotals();
        char showTotalsFlag = showTotals ? '*' : ' ';
        if (opNode.isShowDurationBefore()) {
            Double milliseconds = opNode.getMilliseconds();
            String text;
            if (milliseconds != null) {
                text = String.format("%8.1f ms", defaultIfNull(milliseconds, 0.0));
            } else {
                text = StringUtils.repeat(" ", 11);
            }
            appendTextOrSpaces(sb, text, true);
            sb.append(SEPARATOR_FIRST);
            appendTextOrSpaces(nullPrefix, text, false);
            nullPrefix.append(SEPARATOR_CONTINUATION);
        }
        for (Pair<PerformanceCategory, PerformanceCategoryInfo> pair : opNode.getCounts()) {
            PerformanceCategory category = pair.getLeft();
            PerformanceCategoryInfo info = pair.getRight();
            int count = showTotals ? info.getTotalCount() : info.getOwnCount();
            String text = String.format("%s %5d%c", category.getShortLabel(), count, showTotalsFlag);
            appendTextOrSpaces(sb, text, count > 0);
            sb.append(SEPARATOR_FIRST);
            appendTextOrSpaces(nullPrefix, text, false);
            nullPrefix.append(SEPARATOR_CONTINUATION);
        }
        for (Pair<PerformanceCategory, PerformanceCategoryInfo> pair : opNode.getTimes()) {
            PerformanceCategory category = pair.getLeft();
            PerformanceCategoryInfo info = pair.getRight();
            long time = showTotals ? info.getTotalTime() : info.getOwnTime();
            String text = String.format("%s %8.1f ms%c", category.getShortLabel(), time / 1000.0, showTotalsFlag);
            appendTextOrSpaces(sb, text, time > 0);
            sb.append(SEPARATOR_FIRST);
            appendTextOrSpaces(nullPrefix, text, false);
            nullPrefix.append(SEPARATOR_CONTINUATION);
        }
        return nullPrefix.toString();
    }

    private void appendTextOrSpaces(StringBuilder sb, String text, boolean condition) {
        if (condition) {
            sb.append(text);
        } else {
            sb.append(spaces(text));
        }
    }

    private String spaces(String text) {
        return StringUtils.repeat(' ', text.length());
    }

    void appendWithPrefix(StringBuilder sb, String prefix, String text) {
        for (String line : text.split("\\n")) {
            sb.append(prefix).append(line).append("\n");
        }
    }

    String formatMultiLine(ItemDeltaItemType idi, int indent) {
        return "NYI";
    }

    String formatSingleLine(ItemDeltaItemType idi, GenericTraceVisualizationType generic) {
        if (idi.getDelta().isEmpty() && java.util.Objects.equals(idi.getOldItem(), idi.getNewItem())) {
            return formatSingleLine(idi.getOldItem(), false, generic);
        } else {
            if (generic == GenericTraceVisualizationType.BRIEF) {
                return formatSingleLine(idi.getOldItem(), false, generic) + " -> " + formatSingleLine(idi.getNewItem(), false, generic);
            } else {
                return formatSingleLine(idi.getOldItem(), false, generic) + " + " + formatSingleLine(idi.getDelta()) + " = " + formatSingleLine(idi.getNewItem(), false, generic);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private String formatSingleLine(ItemType item, boolean showName, GenericTraceVisualizationType generic) {
        if (item != null) {
            StringBuilder sb = new StringBuilder();
            if (showName) {
                sb.append(getLocalPart(item.getName())).append(": ");
            }
            formatSingleLine(sb, item.getValue());
            return sb.toString();
        } else {
            return "(no values)";
        }
    }

    private void formatSingleLine(StringBuilder sb, List<Object> values) {
        if (values.isEmpty()) {
            sb.append("(no values)");
        } else if (values.size() == 1) {
            sb.append(formatSingleLine(values.get(0)));
        } else {
            sb.append(values.stream()
                    .map(this::formatSingleLine)
                    .collect(Collectors.joining(", ", "(", ")")));
        }
    }

    private String formatSingleLine(Object value) {
        if (value instanceof RawType) {
            try {
                return ((RawType) value).guessFormattedValue();
            } catch (SchemaException e) {
                return String.valueOf(value);
            }
        } else {
            return String.valueOf(value);
        }
    }

    boolean isSingleLine(ItemDeltaItemType idi, GenericTraceVisualizationType generic) {
        return true;// || formatSingleLine(idi).length() < 80;
    }

    String formatSingleLine(DeltaSetTripleType deltaSetTriple, GenericTraceVisualizationType generic) {
        if (deltaSetTriple != null) {
            StringBuilder sb = new StringBuilder();
            if (!deltaSetTriple.getZero().isEmpty()) {
                formatSingleLine(sb, deltaSetTriple.getZero());
            }
            if (!deltaSetTriple.getMinus().isEmpty() || !deltaSetTriple.getPlus().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                if (deltaSetTriple.getMinus().isEmpty()) {
                    sb.append("Add: ");
                    formatSingleLine(sb, deltaSetTriple.getPlus());
                } else if (deltaSetTriple.getPlus().isEmpty()) {
                    sb.append("Remove: ");
                    formatSingleLine(sb, deltaSetTriple.getMinus());
                } else {
                    formatSingleLine(sb, deltaSetTriple.getMinus());
                    sb.append(" -> ");
                    formatSingleLine(sb, deltaSetTriple.getPlus());
                }
            }
            if (sb.length() != 0) {
                return sb.toString();
            } else {
                return "(none)";
            }
        } else {
            return "(none)";
        }
    }

    String shortEvaluatorDebugDump(JAXBElement<?> evaluator, GenericTraceVisualizationType level) {
        Object value = evaluator.getValue();
        if (value instanceof ScriptExpressionEvaluatorType) {
            String code = ((ScriptExpressionEvaluatorType) value).getCode();
            if (code != null) {
                if (level == GenericTraceVisualizationType.BRIEF || level == GenericTraceVisualizationType.DETAILED) {
                    return DebugUtil.excerpt(code.replaceAll("[\\s\\r\\n]+", " ").trim(), MAX_CODE_CHARS);
                } else {
                    if (code.trim().contains("\n")) {
                        return code.trim();
                    } else {
                        return code;
                    }
                }
            } else {
                return "(script with no code)";
            }
        } else {
            return evaluator.getName().getLocalPart();
        }
    }

    PrismObject<?> removeOperationalItemsIfNeeded(PrismObject<?> focus, GenericTraceVisualizationType generic) {
        PrismObject<?> objectToDisplay;
        if (generic != GenericTraceVisualizationType.BRIEF) {
            objectToDisplay = focus;
        } else {
            objectToDisplay = focus.clone();
            objectToDisplay.getValue().removeOperationalItems();
        }
        return objectToDisplay;
    }

    PrismObject<?> removeShadowAuxiliaryItemsIfNeeded(PrismObject<?> shadow, GenericTraceVisualizationType generic) {
        PrismObject<?> objectToDisplay;
        if (generic != GenericTraceVisualizationType.BRIEF) {
            objectToDisplay = shadow;
        } else {
            objectToDisplay = shadow.clone();
            objectToDisplay.getValue().removeOperationalItems();
            try {
                objectToDisplay.getValue().keepPaths(
                        Arrays.asList(
                                ShadowType.F_NAME,
                                ShadowType.F_RESOURCE_REF,
                                ShadowType.F_SYNCHRONIZATION_SITUATION,
                                ShadowType.F_OBJECT_CLASS,
                                ShadowType.F_KIND,
                                ShadowType.F_INTENT,
                                ShadowType.F_ATTRIBUTES,
                                ShadowType.F_ASSOCIATION,
                                ShadowType.F_ACTIVATION,
                                ShadowType.F_CREDENTIALS
                        ));
            } catch (SchemaException e) {
                e.printStackTrace(); // todo logger
            }
        }
        return objectToDisplay;
    }

    // todo
    String dumpDelta(ObjectDeltaType objectDelta, GenericTraceVisualizationType generic) {
        try {
            ObjectDelta<?> delta = DeltaConvertor.createObjectDelta(objectDelta, prismContext);
            ObjectDelta<?> deltaToDisplay;
            if (generic != GenericTraceVisualizationType.BRIEF) {
                deltaToDisplay = delta;
            } else {
                deltaToDisplay = delta.clone();
                deltaToDisplay.removeOperationalItems();
                deltaToDisplay.removeEstimatedOldValues();
            }
            return deltaToDisplay.debugDump();
        } catch (SchemaException e) {
            return e.getMessage() + "\n" + ExceptionUtil.printStackTrace(e);
        }
    }
}
