/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.impl.visualizer.VisualizationContext;
import com.evolveum.midpoint.model.impl.visualizer.Visualizer;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Prepares text output for notification purposes.
 *
 * This is only a facade for value and delta formatters.
 * It is probably used in various notification-related expressions so we'll keep it for some time.
 */
@Component
public class TextFormatter {

    private static final Trace LOGGER = TraceManager.getTrace(TextFormatter.class);
    private static final List<ItemPath> SYNCHRONIZATION_PATHS = List.of(
            ShadowType.F_SYNCHRONIZATION_SITUATION,
            ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION,
            ShadowType.F_SYNCHRONIZATION_TIMESTAMP,
            ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP);

    private static final List<ItemPath> AUXILIARY_PATHS = List.of(
            InfraItemName.METADATA,
            ShadowType.F_METADATA,
            ShadowType.F_ACTIVATION.append(ActivationType.F_VALIDITY_STATUS), // works for user activation as well
            ShadowType.F_ACTIVATION.append(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
            ShadowType.F_ACTIVATION.append(ActivationType.F_EFFECTIVE_STATUS),
            ShadowType.F_ACTIVATION.append(ActivationType.F_DISABLE_TIMESTAMP),
            ShadowType.F_ACTIVATION.append(ActivationType.F_ARCHIVE_TIMESTAMP),
            ShadowType.F_ACTIVATION.append(ActivationType.F_ENABLE_TIMESTAMP),
            ShadowType.F_ITERATION,
            ShadowType.F_ITERATION_TOKEN,
            FocusType.F_LINK_REF,
            ShadowType.F_TRIGGER);

    @Autowired
    ValueFormatter valueFormatter;
    @Autowired
    DeltaFormatter deltaFormatter;
    @Autowired
    private Visualizer visualizer;
    @Autowired
    private MidpointFunctions midpointFunctions;

    public String formatShadowAttributes(ShadowType shadowType, boolean showSynchronizationItems, boolean showAuxiliaryItems) {
        final Collection<ItemPath> hiddenAttributes = getHiddenPaths(showSynchronizationItems, showAuxiliaryItems);
        // FIXME change this to delta formatter, however firstly check how is the `formatAccountAttributes` special
        //  that it is separate from `formatObject`
        return valueFormatter.formatAccountAttributes(shadowType, hiddenAttributes, showAuxiliaryItems);
    }

    /**
     * Intended for use by scripts
     */
    public String formatObject(PrismObject<?> object, boolean showSynchronizationAttributes,
            boolean showOperationalAttributes) {
        final Task currentTask = this.midpointFunctions.getCurrentTask();
        return formatObject(object, showSynchronizationAttributes, showOperationalAttributes, currentTask,
                currentTask.getResult());
    }

    public String formatObject(PrismObject<?> object, boolean showSynchronizationAttributes,
            boolean showOperationalAttributes, Task task, OperationResult result) {
        final Collection<ItemPath> hiddenPaths = getHiddenPaths(showSynchronizationAttributes,
                showOperationalAttributes);
        return formatObject(object, hiddenPaths, showOperationalAttributes, task, result);
    }

    /**
     * Intended for use by scripts
     */
    public String formatObject(PrismObject<?> object, Collection<ItemPath> hiddenPaths,
            boolean showOperationalAttributes) {
        final Task task = this.midpointFunctions.getCurrentTask();
        return formatObject(object, hiddenPaths, showOperationalAttributes, task, task.getResult());
    }

    public String formatObject(PrismObject<?> object, Collection<ItemPath> hiddenPaths,
            boolean showOperationalAttributes, Task task, OperationResult result) {
        final Visualization visualization = createVisualization(object, showOperationalAttributes, new ArrayList<>(),
                hiddenPaths, task, result);
        return deltaFormatter.formatVisualization(visualization);
    }

    /**
     * Intended for use by scripts
     */
    @SuppressWarnings("unused")
    public String formatObjectModificationDelta(ObjectDelta<? extends Objectable> objectDelta,
            List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        final Task task = this.midpointFunctions.getCurrentTask();
        return formatObjectModificationDelta(objectDelta, true, showOperationalAttributes, task, task.getResult());
    }

    /**
     * Intended for use by scripts
     */
    public String formatObjectModificationDelta(
            @NotNull ObjectDelta<? extends Objectable> objectDelta, boolean showSynchronizationAttributes,
            boolean showOperationalAttributes, PrismObject<?> objectOld, PrismObject<?> objectNew) {
        final Task task = this.midpointFunctions.getCurrentTask();
        return formatObjectModificationDelta(objectDelta, showSynchronizationAttributes, showOperationalAttributes,
                task, task.getResult());
    }

    /**
     * Intended for use by scripts
     */
    public String formatObjectModificationDelta(
            @NotNull ObjectDelta<? extends Objectable> objectDelta, boolean showSynchronizationAttributes,
            boolean showOperationalAttributes) {
        final Task task = this.midpointFunctions.getCurrentTask();
        return formatObjectModificationDelta(objectDelta, showSynchronizationAttributes, showOperationalAttributes,
                task, task.getResult());
    }

    public String formatObjectModificationDelta(
            @NotNull ObjectDelta<? extends Objectable> objectDelta, Collection<ItemPath> pathsToShow,
            boolean showSynchronizationAttributes, boolean showOperationalAttributes) {
        final Task task = this.midpointFunctions.getCurrentTask();
        return formatObjectModificationDelta(objectDelta, pathsToShow, showSynchronizationAttributes, showOperationalAttributes,
                task, task.getResult());
    }

    public String formatObjectModificationDelta(
            @NotNull ObjectDelta<? extends Objectable> objectDelta, boolean showSynchronizationAttributes,
            boolean showOperationalAttributes, Task task,
            OperationResult result) {
        final Collection<ItemPath> hiddenPaths = getHiddenPaths(showSynchronizationAttributes,
                showOperationalAttributes);
        return formatObjectModificationDelta(objectDelta, new ArrayList<>(), hiddenPaths, showOperationalAttributes, task, result);
    }

    public String formatObjectModificationDelta(
            @NotNull ObjectDelta<? extends Objectable> objectDelta, Collection<ItemPath> pathsToShow,
            boolean showSynchronizationAttributes, boolean showOperationalAttributes, Task task,
            OperationResult result) {
        final Collection<ItemPath> hiddenPaths = getHiddenPaths(showSynchronizationAttributes,
                showOperationalAttributes);
        return formatObjectModificationDelta(objectDelta, pathsToShow, hiddenPaths, showOperationalAttributes, task, result);
    }

    /**
     * Intended for use by scripts
     */
    public String formatObjectModificationDelta(
            @NotNull ObjectDelta<? extends Objectable> objectDelta, Collection<ItemPath> hiddenPaths,
            boolean showOperationalAttributes, PrismObject<?> objectOld, PrismObject<?> objectNew) {
        final Task task = this.midpointFunctions.getCurrentTask();
        return formatObjectModificationDelta(objectDelta, new ArrayList<>(), hiddenPaths, showOperationalAttributes, task,
                task.getResult());
    }

    public String formatObjectModificationDelta(
            @NotNull ObjectDelta<? extends Objectable> objectDelta, Collection<ItemPath> pathsToShow,
            Collection<ItemPath> hiddenPaths, boolean showOperationalAttributes, Task task,
            OperationResult result) {

        final Visualization visualization = createVisualization(objectDelta, showOperationalAttributes, pathsToShow, hiddenPaths,
                task, result);
        return deltaFormatter.formatVisualization(visualization);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean containsVisibleModifiedItems(
            Collection<? extends ItemDelta<?, ?>> modifications,
            boolean showSynchronizationAttributes, boolean showAuxiliaryAttributes) {
        Collection<ItemPath> hiddenPaths = getHiddenPaths(showSynchronizationAttributes, showAuxiliaryAttributes);
        return containsVisibleModifiedItems(modifications, hiddenPaths, showAuxiliaryAttributes);
    }

    static boolean isAmongHiddenPaths(ItemPath path, Collection<ItemPath> hiddenPaths) {
        return hiddenPaths != null
                && ItemPathCollectionsUtil.containsSubpathOrEquivalent(hiddenPaths, path);
    }

    private Visualization createVisualization(PrismObject<?> object, boolean showOperationalAttributes,
            Collection<ItemPath> pathsToShow, Collection<ItemPath> pathsToHide, Task task, OperationResult parentResult) {
        final VisualizationContext context = createVisualizationContext(showOperationalAttributes, pathsToShow, pathsToHide);
        final PrismObject<? extends ObjectType> objectToVisualize = (PrismObject<? extends ObjectType>) object;
        try {
            return  this.visualizer.visualize(objectToVisualize, context, task, parentResult);
        } catch (SchemaException | ExpressionEvaluationException e) {
            // TODO log/throw something useful.
            throw new RuntimeException(e);
        }
    }

    private Visualization createVisualization(ObjectDelta<? extends Objectable> delta,
            boolean showOperationalAttributes, Collection<ItemPath> pathsToShow, Collection<ItemPath> pathsToHide, Task task,
            OperationResult parentResult) {
        ObjectDelta<ObjectType> objectDelta = (ObjectDelta<ObjectType>) delta;
        final VisualizationContext context = createVisualizationContext(showOperationalAttributes, pathsToShow, pathsToHide);
        try {
            return  this.visualizer.visualizeDelta(objectDelta, null, context, true, task, parentResult);
        } catch (SchemaException | ExpressionEvaluationException e) {
            // TODO log/throw something useful.
            throw new RuntimeException(e);
        }
    }

    private Collection<ItemPath> getHiddenPaths(boolean showSynchronizationItems, boolean showAuxiliaryAttributes) {
        List<ItemPath> hiddenPaths = new ArrayList<>();
        if (!showSynchronizationItems) {
            hiddenPaths.addAll(SYNCHRONIZATION_PATHS);
        }
        if (!showAuxiliaryAttributes) {
            hiddenPaths.addAll(AUXILIARY_PATHS);
        }
        return hiddenPaths;
    }

    private static @NotNull VisualizationContext createVisualizationContext(boolean showOperationalAttributes,
            Collection<ItemPath> pathsToShow, Collection<ItemPath> pathsToHide) {
        final VisualizationContext context = new VisualizationContext();
        context.setPathsToShow(pathsToShow);
        context.setPathsToHide(pathsToHide);
        context.setIncludeOperationalItems(showOperationalAttributes);
        context.setIncludeMetadata(showOperationalAttributes);
        return context;
    }

    private static @NotNull List<ItemDelta<?, ?>> getVisibleModifications(
            Collection<? extends ItemDelta<?, ?>> modifications,
            Collection<ItemPath> hiddenPaths, boolean showOperational, Object context) {
        List<ItemDelta<?, ?>> toBeDisplayed = new ArrayList<>(modifications.size());
        List<QName> noDefinition = new ArrayList<>();
        for (ItemDelta<?, ?> itemDelta : modifications) {
            if (itemDelta.getDefinition() == null) {
                noDefinition.add(itemDelta.getElementName());
                continue;
            }
            if (!showOperational && itemDelta.getDefinition().isOperational()) {
                continue;
            }
            if (!showOperational && itemDelta.isMetadataRelated()) {
                continue;
            }
            if (isAmongHiddenPaths(itemDelta.getPath(), hiddenPaths)) {
                continue;
            }
            toBeDisplayed.add(itemDelta);
        }
        if (!noDefinition.isEmpty()) {
            LOGGER.error("Item deltas for {} without definition - WILL NOT BE INCLUDED IN NOTIFICATION. Context:\n{}",
                    noDefinition, context);
        }
        return toBeDisplayed;
    }

    private static boolean containsVisibleModifiedItems(
            Collection<? extends ItemDelta<?, ?>> modifications,
            Collection<ItemPath> hiddenPaths,
            boolean showOperational) {
        List<ItemDelta<?, ?>> visibleModifications = getVisibleModifications(
                modifications, hiddenPaths, showOperational, DebugUtil.debugDumpLazily(modifications));
        return !visibleModifications.isEmpty();
    }
}
