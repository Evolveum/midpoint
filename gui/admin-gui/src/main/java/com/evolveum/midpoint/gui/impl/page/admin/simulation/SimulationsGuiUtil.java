/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.RoundedIconColumn;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.WrapperVisualization;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Arrays;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SimulationsGuiUtil {

    private static final Trace LOGGER = TraceManager.getTrace(SimulationsGuiUtil.class);

    public static Label createProcessedObjectStateLabel(String id, IModel<SimulationResultProcessedObjectType> model) {
        Label label = new Label(id, () -> {
            ObjectProcessingStateType state = model.getObject().getState();
            if (state == null) {
                return null;
            }

            return LocalizationUtil.translate(WebComponentUtil.createEnumResourceKey(state));
        });

        label.add(AttributeModifier.append("class", () -> {
            ObjectProcessingStateType state = model.getObject().getState();
            if (state == null) {
                return null;
            }

            switch (state) {
                case ADDED:
                    return Badge.State.SUCCESS.getCss();
                case DELETED:
                    return Badge.State.DANGER.getCss();
                case MODIFIED:
                    return Badge.State.INFO.getCss();
                case UNMODIFIED:
                default:
                    return Badge.State.SECONDARY.getCss();
            }
        }));

        return label;
    }

    public static String getProcessedObjectType(@NotNull IModel<SimulationResultProcessedObjectType> model) {
        SimulationResultProcessedObjectType object = model.getObject();
        if (object == null || object.getType() == null) {
            return null;
        }

        QName type = object.getType();
        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);
        String key = WebComponentUtil.createEnumResourceKey(ot);

        return LocalizationUtil.translate(key);
    }

    public static IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createProcessedObjectIconColumn() {
        return new RoundedIconColumn<>(null) {

            @Override
            protected DisplayType createDisplayType(IModel<SelectableBean<SimulationResultProcessedObjectType>> model) {
                SimulationResultProcessedObjectType object = model.getObject().getValue();
                ObjectType obj = object.getBefore() != null ? object.getBefore() : object.getAfter();
                if (obj == null || obj.asPrismObject() == null) {
                    return new DisplayType()
                            .icon(new IconType().cssClass(WebComponentUtil.createDefaultColoredIcon(object.getType())));
                }

                return new DisplayType()
                        .icon(new IconType().cssClass(WebComponentUtil.createDefaultIcon(obj.asPrismObject())));
            }
        };
    }

    public static Visualization createVisualization(ObjectDeltaType objectDelta, PageBase page) {
        if (objectDelta == null) {
            return null;
        }

        try {
            ObjectDelta delta = DeltaConvertor.createObjectDelta(objectDelta);

            Task task = page.getPageTask();
            OperationResult result = task.getResult();

            return page.getModelInteractionService().visualizeDelta(delta, task, result);
        } catch (SchemaException | ExpressionEvaluationException e) {
            LOGGER.debug("Couldn't convert and visualize delta", e);

            throw new SystemException(e);
        }
    }

    public static VisualizationDto createVisualizationDto(ObjectDeltaType objectDelta, PageBase page) {
        if (objectDelta == null) {
            return null;
        }

        Visualization visualization = createVisualization(objectDelta, page);
        return createVisualizationDto(visualization);
    }

    public static VisualizationDto createVisualizationDto(Visualization visualization) {
        if (visualization == null) {
            return null;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Creating dto for deltas:\n{}", DebugUtil.debugDump(visualization));
        }

        final WrapperVisualization wrapper =
                new WrapperVisualization(new SingleLocalizableMessage("PageSimulationResultObject.changes"), Arrays.asList(visualization));

        return new VisualizationDto(wrapper);
    }
}
