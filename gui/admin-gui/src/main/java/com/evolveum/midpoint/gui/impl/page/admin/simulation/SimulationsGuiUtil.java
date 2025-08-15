/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.SimulationMetricValuesTypeUtil;
import com.evolveum.midpoint.schema.util.SimulationResultTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.gui.impl.component.data.column.icon.RoundedIconColumn;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.WrapperVisualization;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SimulationsGuiUtil {

    private static final Trace LOGGER = TraceManager.getTrace(SimulationsGuiUtil.class);

    private static final String DOT_CLASS = SimulationsGuiUtil.class.getName() + ".";
    private static final String OPERATION_PARSE_PROCESSED_OBJECT = DOT_CLASS + "parserProcessedObject";

    public static Label createProcessedObjectStateLabel(String id, IModel<SimulationResultProcessedObjectType> model) {
        Label label = new Label(id, () -> {
            ObjectProcessingStateType state = model.getObject().getState();
            if (state == null) {
                return null;
            }

            return LocalizationUtil.translateEnum(state);
        });

        label.add(AttributeModifier.append("class", () -> {
            ObjectProcessingStateType state = model.getObject().getState();
            return getObjectProcessingStateBadgeCss(state);
        }));

        return label;
    }

    public static String getObjectProcessingStateBadgeCss(ObjectProcessingStateType state) {
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
    }

    public static String getProcessedObjectType(@NotNull IModel<SimulationResultProcessedObjectType> model) {
        SimulationResultProcessedObjectType object = model.getObject();
        if (object == null || object.getType() == null) {
            return null;
        }

        QName type = object.getType();
        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);
        String key = LocalizationUtil.createKeyForEnum(ot);

        return LocalizationUtil.translate(key);
    }

    public static IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createProcessedObjectIconColumn(
            PageAdminLTE parentPage) {
        return new RoundedIconColumn<>(null) {

            @Override
            protected DisplayType createDisplayType(IModel<SelectableBean<SimulationResultProcessedObjectType>> model) {
                SimulationResultProcessedObjectType object = model.getObject().getValue();
                ObjectType obj = object.getAfter() != null ? object.getAfter() : object.getBefore();
                if (obj == null) {
                    return new DisplayType()
                            .icon(new IconType().cssClass(IconAndStylesUtil.createDefaultColoredIcon(object.getType())));
                }
                if (obj.asPrismObject() == null) {
                    obj = ObjectTypeUtil.fix(obj);
                }
                if (obj.asPrismObject() == null) {
                    return new DisplayType()
                            .icon(new IconType().cssClass(IconAndStylesUtil.createDefaultColoredIcon(object.getType())));
                }
                DisplayType archetypeDisplayType = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(obj, parentPage);
                String iconCss = GuiDisplayTypeUtil.getIconCssClass(archetypeDisplayType);
                if (StringUtils.isNotEmpty(iconCss)) {
                    return archetypeDisplayType;
                }
                return new DisplayType()
                        .icon(new IconType().cssClass(IconAndStylesUtil.createDefaultIcon(obj.asPrismObject())));
            }
        };
    }

    public static Visualization createVisualization(ObjectDelta<? extends ObjectType> delta, PageBase page) {
        if (delta == null) {
            return null;
        }

        try {
            Task task = page.getPageTask();
            OperationResult result = task.getResult();

            return page.getModelInteractionService().visualizeDelta(delta, true, false, task, result);
        } catch (SchemaException | ExpressionEvaluationException e) {
            LOGGER.debug("Couldn't convert and visualize delta", e);

            throw new SystemException(e);
        }
    }

    public static VisualizationDto createVisualizationDto(Visualization visualization) {
        if (visualization == null) {
            return null;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Creating dto for deltas:\n{}", DebugUtil.debugDump(visualization));
        }

        final WrapperVisualization wrapper =
                new WrapperVisualization(new SingleLocalizableMessage("PageSimulationResultObject.changes"), List.of(visualization));

        return new VisualizationDto(wrapper);
    }

    public static String getProcessedObjectName(ProcessedObject<?> object, PageBase page) {
        if (object == null) {
            return page.getString("ProcessedObjectsPanel.unnamed");
        }

        ObjectType obj = ObjectProcessingStateType.DELETED.equals(object.getState()) ? object.getBefore() : object.getAfter();
        if (obj == null) {
            return page.getString("ProcessedObjectsPanel.unnamed");
        }

        String name = obj.getName() != null ? obj.getName().getOrig() : null;

        String displayName = null;
        if (obj instanceof ShadowType) {
            try {
                displayName = getProcessedShadowName((ShadowType) obj, page);
            } catch (SystemException ex) {
                LOGGER.debug("Couldn't create processed shadow name", ex);
            }
        } else {
            displayName = WebComponentUtil.getDisplayName(obj.asPrismObject());
        }

        if (name == null && displayName == null) {
            return page.getString("ProcessedObjectsPanel.unnamed");
        }

        if (Objects.equals(name, displayName)) {
            return name;
        }

        if (name == null) {
            return displayName;
        }

        if (displayName == null) {
            return name;
        }

        return name + " (" + displayName + ")";
    }

    @Nullable
    public static String getShadowNameFromAttribute(ProcessedObject<?> object) {

        if (object == null) {
            return null;
        }

        ObjectType obj = ObjectProcessingStateType.DELETED.equals(object.getState()) ? object.getBefore() : object.getAfter();
        if (obj == null) {
            return null;
        }

        if (!(obj instanceof ShadowType)) {
            return null;
        }

        String name = null;
        try {
            ShadowSimpleAttribute<?> namingAttribute = ShadowUtil.getNamingAttribute((ShadowType) obj);
            Object realName = namingAttribute != null ? namingAttribute.getRealValue() : null;
            name = realName != null ? realName.toString() : null;
        } catch (SystemException e) {
            LOGGER.debug("Couldn't create processed shadow name", e);
        }
        return name;
    }

    private static String getProcessedShadowName(ShadowType shadow, PageBase page) {
        ShadowKindType kind = shadow.getKind() != null ? shadow.getKind() : ShadowKindType.UNKNOWN;

        ShadowSimpleAttribute<?> namingAttribute = ShadowUtil.getNamingAttribute(shadow);
        Object realName = namingAttribute != null ? namingAttribute.getRealValue() : null;
        String name = realName != null ? realName.toString() : "";

        String intent = shadow.getIntent() != null ? shadow.getIntent() : "";

        ObjectReferenceType resourceRef = shadow.getResourceRef();

        Object resourceName = WebModelServiceUtils.resolveReferenceName(resourceRef, page, false);
        if (resourceName == null) {
            resourceName = new SingleLocalizableMessage("ProcessedObjectsPanel.unknownResource",
                    new Object[] { resourceRef != null ? resourceRef.getOid() : null });
        }

        LocalizableMessage msg = new SingleLocalizableMessage("ProcessedObjectsPanel.shadow", new Object[] {
                new SingleLocalizableMessage("ShadowKindType." + kind.name()),
                name,
                intent,
                resourceName
        });

        return LocalizationUtil.translateMessage(msg);
    }

    public static ProcessedObject<?> parseProcessedObject(@NotNull SimulationResultProcessedObjectType obj, @NotNull PageBase page) {
        Task task = page.createSimpleTask(OPERATION_PARSE_PROCESSED_OBJECT);
        OperationResult result = task.getResult();

        try {
            return page.getModelService().parseProcessedObject(obj, task, result);
        } catch (Exception ex) {
            result.computeStatusIfUnknown();
            result.recordFatalError("Couldn't parse processed object", ex);

            page.showResult(result);

            return null;
        }
    }

    public static Map<BuiltInSimulationMetricType, Integer> getBuiltInMetrics(SimulationResultType result) {
        List<SimulationMetricValuesType> metrics = result.getMetric();
        List<SimulationMetricValuesType> builtIn = metrics.stream().filter(m -> m.getRef() != null && m.getRef().getBuiltIn() != null)
                .collect(Collectors.toList());

        Map<BuiltInSimulationMetricType, Integer> map = new HashMap<>();

        for (SimulationMetricValuesType metric : builtIn) {
            BuiltInSimulationMetricType identifier = metric.getRef().getBuiltIn();

            BigDecimal value = SimulationMetricValuesTypeUtil.getValue(metric);
            map.put(identifier, value.intValue());
        }

        return map;
    }

    public static int getUnmodifiedProcessedObjectCount(SimulationResultType result, Map<BuiltInSimulationMetricType, Integer> builtInMetrics) {
        int unmodifiedCount = SimulationResultTypeUtil.getObjectsProcessed(result);

        for (Map.Entry<BuiltInSimulationMetricType, Integer> entry : builtInMetrics.entrySet()) {
            BuiltInSimulationMetricType identifier = entry.getKey();
            if (identifier != BuiltInSimulationMetricType.ADDED
                    && identifier != BuiltInSimulationMetricType.MODIFIED
                    && identifier != BuiltInSimulationMetricType.DELETED) {
                continue;
            }

            int value = entry.getValue();

            unmodifiedCount = unmodifiedCount - value;
        }

        return unmodifiedCount;
    }

    public static ObjectProcessingStateType builtInMetricToProcessingState(BuiltInSimulationMetricType identifier) {
        if (identifier == null) {
            return null;
        }

        switch (identifier) {
            case ADDED:
                return ObjectProcessingStateType.ADDED;
            case MODIFIED:
                return ObjectProcessingStateType.MODIFIED;
            case DELETED:
                return ObjectProcessingStateType.DELETED;
            default:
                return null;
        }
    }
}
