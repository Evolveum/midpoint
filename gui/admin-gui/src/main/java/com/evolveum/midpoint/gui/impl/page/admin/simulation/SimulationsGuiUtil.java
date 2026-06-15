/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.web.page.error.PageError404;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.util.MappingUtil;
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
    /**
     * Note: only simulation event marks (eventMarkRef) are handled here.
     * Policy statement marks (e.g. Protected) are stored on the live object via policyStatement/markRef
     * and are not part of the simulation result snapshot.
     */
    public static List<Badge> createEventMarkBadges(
            @NotNull List<ObjectReferenceType> eventMarkRefs, @NotNull PageBase page) {
        return eventMarkRefs.stream()
                .map(ref -> createEventMarkBadge(ref, page))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static @Nullable Badge createEventMarkBadge(
            @NotNull ObjectReferenceType ref, @NotNull PageBase page) {
        String oid = ref.getOid();
        if (oid == null) {
            return null;
        }
        if (oid.equals(SystemObjectsType.MARK_ITEM_VALUE_FAILED.value())) {
            return new Badge(MappingUtil.MappingStatus.FAILED.cssClass(), page.getString(MappingUtil.MappingStatus.FAILED.translationKey()));
        } else if (oid.equals(SystemObjectsType.MARK_ITEM_VALUE_ADDED.value())) {
            return new Badge(MappingUtil.MappingStatus.ADDED.cssClass(), page.getString(MappingUtil.MappingStatus.ADDED.translationKey()));
        } else if (oid.equals(SystemObjectsType.MARK_ITEM_VALUE_REMOVED.value())) {
            return new Badge(MappingUtil.MappingStatus.REMOVED.cssClass(), page.getString(MappingUtil.MappingStatus.REMOVED.translationKey()));
        } else if (oid.equals(SystemObjectsType.MARK_ITEM_VALUE_MODIFIED.value())) {
            return new Badge(MappingUtil.MappingStatus.MODIFIED.cssClass(), page.getString(MappingUtil.MappingStatus.MODIFIED.translationKey()));
        } else if (oid.equals(SystemObjectsType.MARK_ITEM_VALUE_NOT_CHANGED.value())) {
            return new Badge(MappingUtil.MappingStatus.NOT_CHANGED.cssClass(), page.getString(MappingUtil.MappingStatus.NOT_CHANGED.translationKey()));
        } else if (oid.equals(SystemObjectsType.MARK_ITEM_VALUE_CHANGE_NOT_APPLIED.value())) {
            return new Badge(MappingUtil.MappingStatus.CHANGE_NOT_APPLIED.cssClass(), page.getString(MappingUtil.MappingStatus.CHANGE_NOT_APPLIED.translationKey()));
        }
        String name = WebModelServiceUtils.resolveReferenceName(ref, page);
        return new Badge(Badge.State.SECONDARY.getCss(), name);
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

    public static @Nullable String createResultDurationText(@NotNull SimulationResultType result, Component panel) {
        XMLGregorianCalendar start = result.getStartTimestamp();
        if (start == null) {
            return panel.getString("SimulationResultsPanel.notStartedYet");
        }

        XMLGregorianCalendar end = result.getEndTimestamp();
        if (end == null) {
            end = MiscUtil.asXMLGregorianCalendar(new Date());
        }

        long duration = (end != null ? end.toGregorianCalendar().getTimeInMillis() : 0) - start.toGregorianCalendar().getTimeInMillis();
        if (duration < 0) {
            return null;
        }

        return DurationFormatUtils.formatDurationWords(duration, true, true);
    }

    public static @NotNull Component createTaskStateLabel(
            String id, IModel<SimulationResultType> model, IModel<TaskType> taskModel, PageBase page) {
        IModel<TaskExecutionStateType> stateModel = () -> {
            TaskType task;
            if (taskModel != null) {
                task = taskModel.getObject();
            } else {
                SimulationResultType result = model.getObject();
                if (result == null || result.getRootTaskRef() == null) {
                    return null;
                }

                PrismObject<TaskType> obj = WebModelServiceUtils.loadObject(result.getRootTaskRef(), page);
                task = obj != null ? obj.asObjectable() : null;
            }

            return task != null ? task.getExecutionState() : null;
        };

        Label label = new Label(id, () -> {
            if (model.getObject().getEndTimestamp() != null) {
                return page.getString("PageSimulationResult.finished");
            }

            TaskExecutionStateType state = stateModel.getObject();
            if (state == null) {
                return null;
            }

            return page.getString(state);
        });
        label.add(AttributeAppender.replace("class", () -> {
            TaskExecutionStateType state = stateModel.getObject();
            if (state == TaskExecutionStateType.RUNNABLE || state == TaskExecutionStateType.RUNNING) {
                return Badge.State.SUCCESS.getCss();
            }

            return Badge.State.SECONDARY.getCss();
        }));

        return label;
    }

    public static @NotNull SimulationResultType loadSimulationResult(PageBase page, String resultOid) {

        if (!Utils.isPrismObjectOidValid(resultOid)) {
            throw new RestartResponseException(PageError404.class);
        }

        Task task = page.getPageTask();

        PrismObject<SimulationResultType> object = WebModelServiceUtils.loadObject(
                SimulationResultType.class, resultOid, page, task, task.getResult());
        if (object == null) {
            throw new RestartResponseException(PageError404.class);
        }

        return object.asObjectable();
    }

    public static @NotNull LoadableDetachableModel<SimulationResultProcessedObjectType> loadSimulationResultProcessedObjectModel(
            @NotNull PageBase pageBase,
            @NotNull String simulationResultOid,
            @Nullable Long simulationResultProcessedObjectId) {
        return new LoadableDetachableModel<>() {

            @Override
            protected SimulationResultProcessedObjectType load() {
                Task task = pageBase.getPageTask();


                if (simulationResultProcessedObjectId == null) {
                    throw new RestartResponseException(PageError404.class);
                }

                ObjectQuery query = PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                        .ownedBy(SimulationResultType.class, SimulationResultType.F_PROCESSED_OBJECT)
                        .ownerId(simulationResultOid)
                        .and()
                        .id(simulationResultProcessedObjectId)
                        .build();

                List<SimulationResultProcessedObjectType> result = WebModelServiceUtils.searchContainers(SimulationResultProcessedObjectType.class,
                        query, null, task.getResult(), pageBase);

                if (result.isEmpty()) {
                    throw new RestartResponseException(PageError404.class);
                }

                return result.get(0);
            }
        };
    }

    public static @NotNull LoadableDetachableModel<PrismObjectWrapper<? extends ObjectType>> loadWrapper(
            PageBase pageBase,
            SimulationResultProcessedObjectType resultProcessedObjectType) {
        return new LoadableDetachableModel<>() {
            @Override
            protected PrismObjectWrapper<? extends ObjectType> load() {
                if (resultProcessedObjectType == null) {
                    return null;
                }

                Task task = pageBase.createSimpleTask("createWrapper");

                Collection<SelectorOptions<GetOperationOptions>> options = pageBase.getOperationOptionsBuilder()
                        .noFetch()
                        .item(ItemPath.create(ObjectType.F_POLICY_STATEMENT, PolicyStatementType.F_MARK_REF)).resolve()
                        .item(ItemPath.create(ObjectType.F_POLICY_STATEMENT, PolicyStatementType.F_LIFECYCLE_STATE)).resolve()
                        .build();

                try {
                    PrismObject<ObjectType> prismObject = WebModelServiceUtils.loadObject(
                            ObjectTypes.getObjectTypeClass(resultProcessedObjectType.getType()),
                            resultProcessedObjectType.getOid(),
                            options, pageBase, task, task.getResult());

                    if (prismObject == null) {
                        return null;
                    }

                    PrismObjectWrapperFactory<ObjectType> factory = pageBase.findObjectWrapperFactory(
                            prismObject.getDefinition());
                    OperationResult result = task.getResult();
                    WrapperContext ctx = new WrapperContext(task, result);
                    ctx.setCreateIfEmpty(true);

                    return factory.createObjectWrapper(prismObject, ItemStatus.NOT_CHANGED, ctx);
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create object wrapper for " + resultProcessedObjectType, e);
                }
                return null;
            }
        };
    }

    public static void performMarkObjects(List<String> markOids,
            @NotNull List<SimulationResultProcessedObjectType> selected,
            PageBase page,
            Task task,
            OperationResult result) {
        for (var object : selected) {
            if (ObjectProcessingStateType.ADDED.equals(object.getState())) {
                // skip object, since it is added
                continue;
            }

            // We recreate statements (can not reuse them between multiple objects - we can create new or clone
            // but for each delta we need separate statement
            List<PolicyStatementType> statements = new ArrayList<>();
            for (String oid : markOids) {
                statements.add(new PolicyStatementType().markRef(oid, MarkType.COMPLEX_TYPE)
                        .type(PolicyStatementTypeType.APPLY));
            }

            try {
                @SuppressWarnings("unchecked")
                var type = (Class<? extends ObjectType>) page.getPrismContext().getSchemaRegistry()
                        .getCompileTimeClassForObjectType(object.getType());
                var delta = page.getPrismContext().deltaFactory().object()
                        .createModificationAddContainer(type,
                                object.getOid(), ObjectType.F_POLICY_STATEMENT,
                                statements.toArray(new PolicyStatementType[0]));
                page.getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);
            } catch (Exception e) {
                result.recordPartialError(
                        page.createStringResource(
                                        "ProcessedObjectsPanel.message.markObjectError", object)
                                .getString(),
                        e);
                LOGGER.error("Could not mark object {} with marks {}", object, markOids, e);
            }
        }
    }
}
