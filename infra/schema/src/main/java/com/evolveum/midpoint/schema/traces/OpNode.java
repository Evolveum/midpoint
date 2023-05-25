/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.traces.operations.MappingEvaluationOpNode;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

@Experimental
public class OpNode {

    private final PrismContext prismContext;
    protected final OperationResultType result;
    protected final List<OpNode> children = new ArrayList<>();
    private final OpNode parent;
    protected final OpResultInfo info;
    private final TraceInfo traceInfo;
    private OpNodePresentation presentation;

    private TraceVisualizationInstructionsType visualizationInstructions;
    private TraceVisualizationInstructionType visualizationInstruction;
    private boolean stop = false;
    private boolean visible = true;
    private boolean disabled = false;

    public OpNode(PrismContext prismContext, OperationResultType result, OpResultInfo info, OpNode parent, TraceInfo traceInfo) {
        this.prismContext = prismContext;
        assert result != null;
        this.result = result;
        this.info = info;
        this.parent = parent;
        this.traceInfo = traceInfo;
    }

    public OperationResultType getResult() {
        return result;
    }

    public OperationsPerformanceInformationType getPerformance() {
        return info.getPerformance();
    }

    public Map<PerformanceCategory, PerformanceCategoryInfo> getPerformanceByCategory() {
        return info.getPerformanceByCategory();
    }

    public OpType getType() {
        return info.getType();
    }

    public OperationKindType getKind() {
        return info.getKind();
    }

    public List<OpNode> getChildren() {
        return children;
    }

    public <T extends OpNode> List<T> getChildren(Class<T> aClass) {
        //noinspection unchecked
        return children.stream()
                .filter(child -> aClass.isAssignableFrom(child.getClass()))
                .map(child -> (T) child)
                .collect(Collectors.toList());
    }

    public OpNode getParent() {
        return parent;
    }

    public long getStart(long base) {
        return XmlTypeConverter.toMillis(result.getStart()) - base;
    }

    public String dump() {
        try {
            return OperationResult.createOperationResult(result).debugDump();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public TraceType getFirstTrace() {
        return result.getTrace().isEmpty() ? null : result.getTrace().get(0);
    }

    public String getOperationQualified() {
        StringBuilder sb = new StringBuilder();
        sb.append(result.getOperation());
        if (!result.getQualifier().isEmpty()) {
            sb.append(" (");
            sb.append(String.join("; ", result.getQualifier()));
            sb.append(")");
        }
        return sb.toString();
    }

    public String getLabel() {
        if (presentation != null) {
            String label = presentation.getLabel();
            if (label != null) {
                return label;
            }
        }
        return getOperationNameFormatted();
    }

    public String getToolTip() {
        if (presentation != null) {
            return presentation.getToolTip();
        } else {
            return null;
        }
    }

    public String getOperationNameFormatted() {
        return getType().getFormattedName(this) + (visible ? "" : "!");
    }

    public String getClockworkState() {
        ClockworkTraceType click = getTrace(ClockworkTraceType.class);
        if (click instanceof ClockworkClickTraceType && ((ClockworkClickTraceType) click).getState() != null) {
            return String.valueOf(((ClockworkClickTraceType) click).getState());
        } else if (click != null && click.getInputLensContext() != null && click.getInputLensContext().getState() != null) {
            return String.valueOf(click.getInputLensContext().getState());
        } else if (parent != null) {
            return parent.getClockworkState();
        } else {
            return "";
        }
    }

    public String getExecutionWave() {
        ClockworkTraceType click = getTrace(ClockworkTraceType.class);
        if (click instanceof ClockworkClickTraceType && ((ClockworkClickTraceType) click).getExecutionWave() != null) {
            return String.valueOf(((ClockworkClickTraceType) click).getExecutionWave());
        } else if (click != null && click.getInputLensContext() != null && click.getInputLensContext().getExecutionWave() != null) {
            return String.valueOf(click.getInputLensContext().getExecutionWave());
        } else if (parent != null) {
            return parent.getExecutionWave();
        } else {
            return "";
        }
    }

    public String getProjectionWave() {
        ClockworkTraceType click = getTrace(ClockworkTraceType.class);
        if (click instanceof ClockworkClickTraceType && ((ClockworkClickTraceType) click).getProjectionWave() != null) {
            return String.valueOf(((ClockworkClickTraceType) click).getProjectionWave());
        } else if (click != null && click.getInputLensContext() != null && click.getInputLensContext().getProjectionWave() != null) {
            return String.valueOf(click.getInputLensContext().getProjectionWave());
        } else if (parent != null) {
            return parent.getProjectionWave();
        } else {
            return "";
        }
    }

    public <T> T getTrace(Class<T> aClass) {
        return TraceUtil.getTrace(result, aClass);
    }

    public <T> T getTraceUpwards(Class<T> aClass) {
        T trace = getTrace(aClass);
        if (trace != null || parent == null) {
            return trace;
        } else {
            return parent.getTraceUpwards(aClass);
        }
    }

    public <T> T getTraceDownwards(Class<T> aClass) {
        return getTraceDownwards(aClass, Integer.MAX_VALUE);
    }

    public <T> T getTraceDownwards(Class<T> aClass, int maxLevel) {
        T trace = getTrace(aClass);
        if (trace != null || maxLevel == 0) {
            return trace;
        } else {
            for (OpNode child : children) {
                T inChild = child.getTraceDownwards(aClass, maxLevel-1);
                if (inChild != null) {
                    return inChild;
                }
            }
            return null;
        }
    }

    public <T extends OpNode> List<T> getNodesDownwards(Class<T> aClass, int maxLevel) {
        List<T> nodes = new ArrayList<>();
        if (aClass.isAssignableFrom(this.getClass())) {
            //noinspection unchecked
            nodes.add((T) this);
        }

        if (maxLevel > 0) {
            for (OpNode child : children) {
                nodes.addAll(child.getNodesDownwards(aClass, maxLevel-1));
            }
        }
        return nodes;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void applyVisualizationInstructions(@NotNull TraceVisualizationInstructionsType instructions) {
        applyLocalVisualizationInstruction(instructions);
        children.forEach(child -> child.applyVisualizationInstructions(instructions));
    }

    private void applyLocalVisualizationInstruction(@NotNull TraceVisualizationInstructionsType instructions) {
        visualizationInstructions = instructions;
        if (parent != null && parent.stop) {
            visualizationInstruction = null;
            visible = false;
            stop = true;
        } else {
            visualizationInstruction = findApplicableInstruction(instructions);
            visible = visualizationInstruction != null && visualizationInstruction.getVisualization() != null
                    && isVisible(visualizationInstruction.getVisualization());
        }
    }

    private static boolean isVisible(@NotNull TraceVisualizationType visualization) {
        return visualization.getGeneric() != null && visualization.getGeneric() != GenericTraceVisualizationType.HIDE
                && visualization.getGeneric() != GenericTraceVisualizationType.STOP;
    }

    private TraceVisualizationInstructionType findApplicableInstruction(@NotNull TraceVisualizationInstructionsType instructions) {
        for (TraceVisualizationInstructionType instruction : instructions.getInstruction()) {
            if (matches(instruction)) {
                return instruction;
            }
        }
        return null;
    }

    private boolean matches(TraceVisualizationInstructionType instruction) {
        if (instruction.getSelector().isEmpty()) {
            return true;
        } else {
            for (TraceSelectorType selector : instruction.getSelector()) {
                if (matches(selector)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean matches(TraceSelectorType selector) {
        return matchesTraceType(selector.getTraceType()) && matchesOperationKind(selector.getOperationKind());
    }

    private boolean matchesOperationKind(OperationKindType operationKind) {
        return getResult().getOperationKind() == operationKind;
    }

    private boolean matchesTraceType(List<QName> typeList) {
        if (typeList.isEmpty()) {
            return true;
        } else {
            for (QName traceType : typeList) {
                if (matchesTraceType(traceType)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean matchesTraceType(QName traceType) {
        ComplexTypeDefinition ctd = prismContext.getSchemaRegistry().findComplexTypeDefinitionByType(traceType);
        if (ctd == null) {
            throw new IllegalStateException("No complex type definition for '" + traceType + "'");
        }
        Class<?> traceClass = ctd.getCompileTimeClass();
        if (traceClass == null) {
            throw new IllegalStateException("Trace type '" + traceType + "' has no compile time class: " + ctd);
        }
        for (TraceType trace : result.getTrace()) {
            if (traceClass.isAssignableFrom(trace.getClass())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesOperationKind(Collection<OperationKindType> kindList) {
        if (kindList.isEmpty()) {
            return true;
        } else {
            for (OperationKindType kind : kindList) {
                if (matchesOperationKind(kind)) {
                    return true;
                }
            }
            return false;
        }
    }


    public void applyOptions(Options options) {
        setVisible(isVisible(options));
        for (OpNode child : children) {
            child.applyOptions(options);
        }
    }

    private boolean isVisible(Options options) {
        if (options.getTypesToShow().contains(getType())) {
            return true;
        }
        if (options.getKindsToShow().contains(getKind())) {
            return true;
        }
        for (PerformanceCategory cat : options.getCategoriesToShow()) {
            PerformanceCategoryInfo perfInfo = getPerformanceByCategory().get(cat);
            if (options.isShowAlsoParents()) {
                if (perfInfo.getTotalCount() > 0) {
                    return true;
                }
            } else {
                if (perfInfo.getOwnCount() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getImportanceSymbol() {
        if (result.getImportance() != null) {
            switch (result.getImportance()) {
            case MAJOR: return "O";
            case NORMAL: return "o";
            case MINOR: return ".";
            default: return "?";
            }
        } else return "o";
    }

    public LensContextType getContextToView() {
        List<TraceType> traces = result.getTrace();
        for (TraceType trace : traces) {
            if (trace instanceof ClockworkTraceType) {
                return ((ClockworkTraceType) trace).getOutputLensContext();
            }
        }
        return null;
    }

    public List<ViewedObject> getObjectsToView() {
        List<TraceType> traces = result.getTrace();
        for (TraceType trace : traces) {
            if (trace instanceof ClockworkTraceType) {
                return processContext(((ClockworkTraceType) trace).getOutputLensContext());
            }
        }
        return null;
    }
    public List<ViewedObject> processContext(LensContextType ctx) {
        List<ViewedObject> rv = new ArrayList<>();
        if (ctx != null && ctx.getFocusContext() != null) {
            LensFocusContextType fctx = ctx.getFocusContext();
            ObjectType objectOld = fctx.getObjectOld();
            ObjectType objectCurrent = fctx.getObjectCurrent();
            ObjectType objectNew = fctx.getObjectNew();
            if (objectOld != null) {
                rv.add(new ViewedObject("old", objectOld.asPrismObject()));
            }
            if (objectCurrent != null) {
                rv.add(new ViewedObject("current", objectCurrent.asPrismObject()));
            }
            if (objectNew != null) {
                rv.add(new ViewedObject("new", objectNew.asPrismObject()));
            }
        }
        return rv.isEmpty() ? null : rv;
    }

    public List<String> getTraceNames() {
        return result.getTrace().stream().map(trace -> trace.getClass().getSimpleName()).collect(Collectors.toList());
    }

    public String getResultComment() {
        return getResultComment(result);
    }

    public static String getResultComment(OperationResultType result) {
        ParamsType returns = result.getReturns();
        if (returns == null) {
            return null;
        }
        for (EntryType entry : returns.getEntry()) {
            if (OperationResult.RETURN_COMMENT.equals(entry.getKey())) {
                JAXBElement<?> value = entry.getEntryValue();
                if (value == null) {
                    return null;
                } else if (value.getValue() instanceof RawType) {
                    return ((RawType) value.getValue()).extractString();
                } else {
                    return String.valueOf(value.getValue());
                }
            }
        }
        return null;
    }

    public int getLogEntriesCount() {
        int rv = 0;
        for (LogSegmentType segment : result.getLog()) {
            rv += segment.getEntry().size();
        }
        return rv;
    }

    public TraceInfo getTraceInfo() {
        return traceInfo;
    }

    public Double getOverhead() {
        long repository = getPerformanceByCategory().get(PerformanceCategory.REPOSITORY).getTotalTime();
        long icf = getPerformanceByCategory().get(PerformanceCategory.ICF).getTotalTime();
        Long total = getResult().getMicroseconds();
        if (total != null && total.doubleValue() != 0.0) {
            return (total.doubleValue() - repository - icf) / total.doubleValue();
        } else {
            return null;
        }
    }

    public Double getOverhead2() {
        long repository = getPerformanceByCategory().get(PerformanceCategory.REPOSITORY_CACHE).getTotalTime();
        long icf = getPerformanceByCategory().get(PerformanceCategory.ICF).getTotalTime();
        Long total = getResult().getMicroseconds();
        if (total != null && total.doubleValue() != 0.0) {
            return (total.doubleValue() - repository - icf) / total.doubleValue();
        } else {
            return null;
        }
    }

    public TraceVisualizationInstructionType getVisualizationInstruction() {
        return visualizationInstruction;
    }

    public GenericTraceVisualizationType getGenericVisualization() {
        if (visualizationInstruction != null && visualizationInstruction.getVisualization() != null) {
            return visualizationInstruction.getVisualization().getGeneric();
        } else {
            return null;
        }
    }

    public TraceDataSelectionType getDataSelection() {
        if (visualizationInstruction != null && visualizationInstruction.getVisualization() != null) {
            return visualizationInstruction.getVisualization().getData();
        } else {
            return null;
        }
    }

    public String getFocusName() {
        ClockworkRunTraceType trace = getTraceUpwards(ClockworkRunTraceType.class);
        return trace != null ? trace.getFocusName() : null;
    }

    public boolean isShowInvocationId() {
        return visualizationInstructions.getColumns() != null &&
                Boolean.TRUE.equals(visualizationInstructions.getColumns().isInvocationId());
    }

    public boolean isShowDurationBefore() {
        return visualizationInstructions.getColumns() != null &&
                Boolean.TRUE.equals(visualizationInstructions.getColumns().isDurationBefore());
    }

    public boolean isShowDurationAfter() {
        return visualizationInstructions.getColumns() != null &&
                Boolean.TRUE.equals(visualizationInstructions.getColumns().isDuration());
    }

    public Long getInvocationId() {
        return result.getInvocationId();
    }

    public String getMillisecondsFormatted() {
        if (result.getMicroseconds() != null) {
            return String.format(Locale.US, "%.1f", result.getMicroseconds() / 1000.0);
        } else {
            return "";
        }
    }

    public Double getMilliseconds() {
        return result.getMicroseconds() != null ? result.getMicroseconds() / 1000.0 : null;
    }

    public List<PerformanceCategory> getCountColumns() {
        if (visualizationInstructions.getColumns() != null) {
            return parse(visualizationInstructions.getColumns().getCountFor());
        } else {
            return Collections.emptyList();
        }
    }

    public List<PerformanceCategory> getTimeColumns() {
        if (visualizationInstructions.getColumns() != null) {
            return parse(visualizationInstructions.getColumns().getTimeFor());
        } else {
            return Collections.emptyList();
        }
    }

    private List<PerformanceCategory> parse(List<String> names) {
        return names.stream()
                .map(PerformanceCategory::valueOf)
                .collect(Collectors.toList());
    }

    public List<Pair<PerformanceCategory, PerformanceCategoryInfo>> getCounts() {
        return getSelectedInformation(getCountColumns());
    }

    public List<Pair<PerformanceCategory, PerformanceCategoryInfo>> getTimes() {
        return getSelectedInformation(getTimeColumns());
    }

    @NotNull
    private List<Pair<PerformanceCategory, PerformanceCategoryInfo>> getSelectedInformation(List<PerformanceCategory> categories) {
        Map<PerformanceCategory, PerformanceCategoryInfo> all = getPerformanceByCategory();
        List<Pair<PerformanceCategory, PerformanceCategoryInfo>> selected = new ArrayList<>();
        for (PerformanceCategory category : categories) {
            PerformanceCategoryInfo info = all.get(category);
            if (info != null) {
                PerformanceCategoryInfo adapted = adaptPerformanceInfo(category, info);
                selected.add(new ImmutablePair<>(category, adapted));
            }
        }
        return selected;
    }

    /**
     * Computes "adapted own" information: subtracts all visible "own" data from children from the totals, leading to
     * apparent own information.
     */
    @NotNull
    private PerformanceCategoryInfo adaptPerformanceInfo(PerformanceCategory category, PerformanceCategoryInfo info) {
        PerformanceCategoryInfo adapted = new PerformanceCategoryInfo();
        adapted.setTotalCount(info.getTotalCount());
        adapted.setOwnCount(info.getTotalCount());
        adapted.setTotalTime(info.getTotalTime());
        adapted.setOwnTime(info.getTotalTime());
        children.forEach(child -> child.subtractOwnVisiblePerformanceInfo(category, adapted));
        return adapted;
    }

    private void subtractOwnVisiblePerformanceInfo(PerformanceCategory category, PerformanceCategoryInfo adapted) {
        if (visible) {
            PerformanceCategoryInfo info = getPerformanceByCategory().get(category);
            if (info != null) {
                adapted.setOwnCount(adapted.getOwnCount() - info.getOwnCount());
                adapted.setOwnTime(adapted.getOwnTime() - info.getOwnTime());
            }
        }
        children.forEach(child -> child.subtractOwnVisiblePerformanceInfo(category, adapted));
    }

    public boolean showTotals() {
        // temporary implementation
        OperationKindType kind = getKind();
        return kind == OperationKindType.CLOCKWORK_EXECUTION || kind == OperationKindType.CLOCKWORK_CLICK;
    }

    public List<OpNode> getVisibleChildren() {
        List<OpNode> visibleChildren = new ArrayList<>();
        for (OpNode child : children) {
            if (child.isVisible()) {
                visibleChildren.add(child);
            } else {
                visibleChildren.addAll(child.getVisibleChildren());
            }
        }
        return visibleChildren;
    }

    // used from templates
    public Integer getClickNumber() {
        if (parent == null) {
            return null;
        }
        int count = 0;
        for (OpNode child : parent.getChildren()) {
            if (child.getKind() == OperationKindType.CLOCKWORK_CLICK) {
                count++;
            }
            if (child == this) {
                return count;
            }
        }
        return null;
    }

    public int getMappingsCount() {
        return (int) children.stream()
                .filter(child -> child instanceof MappingEvaluationOpNode)
                .count();
    }

    public Integer getAssignmentEvaluationsCount() {
        return (int) getChildrenStream(3)
                .filter(child -> child.getKind() == OperationKindType.ASSIGNMENT_EVALUATION)
                .count();
    }

    public Stream<OpNode> getChildrenStream(int levels) {
        Stream.Builder<OpNode> streamBuilder = Stream.builder();
        addChildrenToStreamBuilder(streamBuilder, levels);
        return streamBuilder.build();
    }

    private void addChildrenToStreamBuilder(Stream.Builder<OpNode> streamBuilder, int levels) {
        if (levels > 0) {
            for (OpNode child : children) {
                streamBuilder.add(child);
                child.addChildrenToStreamBuilder(streamBuilder, levels-1);
            }
        }
    }

    @Override
    public String toString() {
        return "OpNode{" + getOperationNameFormatted() + '}';
    }

    public OpNodePresentation getPresentation() {
        return presentation;
    }

    public void setPresentation(OpNodePresentation presentation) {
        this.presentation = presentation;
    }

    public void resolveReferenceTargetNames(OpNodeTreeBuilder.NameResolver nameResolver) {
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public final void postProcessRecursive() {
        postProcess();
        children.forEach(OpNode::postProcessRecursive);
    }

    protected void postProcess() {
        // nothing to do at general level
    }

    @NotNull
    public String getContext(String name) {
        return TraceUtil.getContext(result, name);
    }

    @NotNull
    public String getReturn(String name) {
        return TraceUtil.getReturn(result, name);
    }

    @NotNull
    public String getParameter(String name) {
        return TraceUtil.getParameter(result, name);
    }

}
