/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.component;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangeExecutor;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangesExecutorImpl;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceTaskCreator;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TileChoicePopup;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SimulationActionFlow<T> implements Serializable {

    private static final String DOT_CLASS = SimulationActionFlow.class.getName() + ".";
    private static final String OP_CREATE_TASK = DOT_CLASS + "createTask";
    private static final String OP_LOAD_TASK = DOT_CLASS + "loadTask";

    private static final Trace LOGGER = TraceManager.getTrace(SimulationActionFlow.class);

    private final SimulationParams<T> context;
    boolean isSamplingEnabled = false;
    boolean showProgressPopup = true;
    boolean isCorrelationFastSimulation = false;

    public SimulationActionFlow(@NotNull SimulationParams<T> context) {
        this.context = context;

        T workDefinitionConfig = context.workDefinitionConfiguration();
        if (workDefinitionConfig != null && workDefinitionConfig.getClass().equals(SimulatedCorrelatorsType.class)) {
            this.isCorrelationFastSimulation = true;
        }
    }

    /**
     * Entry point: show popup (or run directly if config disabled).
     */
    public void start(AjaxRequestTarget target) {
        ResourceObjectTypeDefinitionType def = context.definition();
        PredefinedConfigurationType defaultCfg = PredefinedConfigurationType.DEVELOPMENT;

        String lifecycleState = def.getLifecycleState();
        boolean isProposed = ShadowLifecycleStateType.PROPOSED.value().equals(lifecycleState);

        if (isProposed) {
            if (isSamplingEnabled) {
                runSamplingSimulation(context.pageBase(), target, defaultCfg);
            } else {
                runSimulation(target, defaultCfg, null);
            }
            return;
        }

        showPredefinedConfigPopup(target, defaultCfg);
    }

    private void showPredefinedConfigPopup(
            AjaxRequestTarget target,
            PredefinedConfigurationType defaultCfg) {

        PageBase pageBase = context.pageBase();

        List<Tile<PredefinedConfigurationType>> tiles = Arrays.stream(PredefinedConfigurationType.values())
                .map(cfg -> {
                    String icon = cfg == PredefinedConfigurationType.PRODUCTION
                            ? "fa-solid fa-industry"
                            : "fa-solid fa-flask";

                    Tile<PredefinedConfigurationType> tile = new Tile<>(
                            icon,
                            pageBase.getString("PredefinedConfigurationType." + cfg.name()));
                    tile.setValue(cfg);
                    tile.setDescription(pageBase.getString(
                            "PredefinedConfigurationType." + cfg.name() + ".description"));
                    return tile;
                })
                .toList();

        TileChoicePopup<PredefinedConfigurationType> popup = new TileChoicePopup<>(
                pageBase.getMainPopupBodyId(),
                () -> tiles,
                defaultCfg) {

            @Override
            protected IModel<String> getText() {
                return createStringResource("SimulationActionButton.simulate.text");
            }

            @Override
            protected IModel<String> getSubText() {
                return createStringResource("SimulationActionButton.simulate.subText");
            }

            @Override
            protected IModel<String> getAcceptButtonLabel() {

                return isSamplingEnabled ? createStringResource("SimulationActionButton.simulate.save.execute.and.sample")
                        : createStringResource("SimulationActionButton.simulate.save.and.execute");
            }

            @Override
            protected void performAction(AjaxRequestTarget target, PredefinedConfigurationType value) {
                pageBase.hideMainPopup(target);

                if (!isSamplingEnabled) {
                    runSimulation(target, value, null);
                    return;
                }

                runSamplingSimulation(pageBase, target, value);
            }
        };
        pageBase.showMainPopup(popup, target);
    }

    private void runSamplingSimulation(PageBase pageBase, AjaxRequestTarget target, PredefinedConfigurationType value) {
        ResourceDetailsModel resourceModel = getResourceDetailsModel(pageBase);
        ResourceObjectTypeDefinitionType def = context.definition();
        SimulationDataSamplingPanel panel = new SimulationDataSamplingPanel(
                pageBase.getMainPopupBodyId(),
                Model.of(resourceModel)) {

            @Override
            protected ResourceObjectTypeDefinition getObjectTypeDefinition() {
                return resourceModel.getObjectTypeDefinition(def.getKind(), def.getIntent());
            }

            @Override
            protected List<IModel<String>> getInfoMessagesModels() {
                if (!isCorrelationFastSimulation) {
                    return List.of(
                            createStringResource("SimulationDataSamplingPanel.simulation.mode.mapping.info"),
                            createStringResource("SimulationDataSamplingPanel.approximateCount.mapping.info")
                    );
                }
                return super.getInfoMessagesModels();
            }

            public void yesPerformed(AjaxRequestTarget target,
                    @Nullable Integer sampleSize,
                    @Nullable ObjectQuery objectQuery) {
                Integer effectiveSampleSize = sanitizeSampleSize(sampleSize);

                if (objectQuery == null && effectiveSampleSize == null) {
                    pageBase.hideMainPopup(target);
                    runSimulation(target, value, null);
                    return;
                }

                ObjectQuery localQuery = objectQuery != null
                        ? objectQuery.clone()
                        : PrismContext.get().queryFactory().createQuery();

                if (effectiveSampleSize != null) {
                    ObjectPaging paging = PrismContext.get()
                            .queryFactory()
                            .createPaging(0, effectiveSampleSize);
                    localQuery.setPaging(paging);
                }

                QueryType query;
                try {
                    query = PrismContext.get().getQueryConverter().createQueryType(localQuery);
                } catch (SchemaException e) {
                    var result = new OperationResult("createQueryType");
                    result.recordFatalError("Couldn't prepare simulation query: " + e.getMessage(), e);
                    getPageBase().showResult(result);
                    target.add(getPageBase().getFeedbackPanel());
                    return;
                }

                pageBase.hideMainPopup(target);
                runSimulation(target, value, query);
            }
        };
        pageBase.showMainPopup(panel, target);
    }

    /**
     * Returns null if sample size is missing/invalid => "run full".
     * Otherwise, returns a clamped positive sample size.
     */
    private @Nullable Integer sanitizeSampleSize(@Nullable Integer sampleSize) {
        if (sampleSize == null) {
            return null;
        }
        if (sampleSize <= 0) {
            return null;
        }

        return sampleSize;
    }

    private @NotNull ResourceDetailsModel getResourceDetailsModel(PageBase pageBase) {
        ResourceType resource = context.resource();
        PrismObject<ResourceType> prismObject = resource.asPrismObject();
        LoadableDetachableModel<PrismObject<ResourceType>> resourceModelModel =
                new LoadableDetachableModel<>() {
                    @Override
                    protected PrismObject<ResourceType> load() {
                        return prismObject;
                    }
                };

        return new ResourceDetailsModel(resourceModelModel,
                pageBase);
    }

    protected void runSimulation(AjaxRequestTarget target, PredefinedConfigurationType cfg, QueryType query) {
        PageBase pageBase = context.pageBase();
        ResourceType resource = context.resource();
        ResourceObjectTypeDefinitionType def = context.definition();
        ResourceTaskFlavor<T> flavor = context.flavor();
        TaskType newTask = pageBase.taskAwareExecutor(target, OP_CREATE_TASK)
                .hideSuccessfulStatus()
                .run((task, result) -> createTask(task, result, pageBase, query, resource, def, flavor, cfg));

        saveAndPerformSimulation(target, pageBase, newTask);
    }

    private @Nullable TaskType createTask(
            Task task,
            OperationResult result,
            PageBase pageBase,
            QueryType query,
            ResourceType resource,
            @NotNull ResourceObjectTypeDefinitionType def,
            ResourceTaskFlavor<T> flavor,
            PredefinedConfigurationType cfg) {

        try {
            return ResourceTaskCreator.of(flavor, pageBase)
                    .forResource(resource)
                    .withConfiguration(context.workDefinitionConfiguration())
                    .ownedByCurrentUser()
                    .withCoordinates(
                            def.getKind(),
                            def.getIntent(),
                            def.getObjectClass())
                    .withQuery(query)
                    .withExecutionMode(context.executionMode())
                    .withPredefinedConfiguration(cfg)
                    .withSubmissionOptions(ActivitySubmissionOptions.create()
                            .withTaskTemplate(new TaskType()
                                    .name("Preview of " + flavor.flavorName()
                                            + " on " + resource.getName() + " resource")))
                    .withSimulationResultDefinition(
                            new SimulationDefinitionType().useOwnPartitionForProcessedObjects(false))
                    .create(task, result);
        } catch (CommonException e) {
            LOGGER.error("Couldn't create simulation task", e);
            result.recordFatalError(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Persists the newly created task immediately and optionally
     * shows the simulation progress popup.
     */
    private void saveAndPerformSimulation(
            AjaxRequestTarget target,
            PageBase pageBase,
            TaskType newTask) {

        if (newTask == null) {
            return;
        }

        Task task = pageBase.createSimpleTask(OP_CREATE_TASK);
        OperationResult result = task.getResult();

        PrismObject<TaskType> prismTask = newTask.asPrismObject();
        PrismObjectWrapperFactory<TaskType> wrapperFactory = pageBase.findObjectWrapperFactory(prismTask.getDefinition());

        WrapperContext context = new WrapperContext(task, result);
        context.setCreateIfEmpty(true);

        String taskOid = null;

        try {
            PrismObjectWrapper<TaskType> wrapper = wrapperFactory.createObjectWrapper(prismTask, ItemStatus.ADDED, context);

            WebComponentUtil.setTaskStateBeforeSave(wrapper, true, pageBase, target);

            ObjectDelta<TaskType> delta = wrapper.getObjectDelta();
            ObjectChangeExecutor executor = new ObjectChangesExecutorImpl();

            Collection<ObjectDeltaOperation<? extends ObjectType>> operations = executor.executeChanges(
                    Collections.singleton(delta),
                    false,
                    task,
                    result,
                    target);

            taskOid = ObjectDeltaOperation.findAddDeltaOidRequired(operations, TaskType.class);

        } catch (CommonException e) {
            LOGGER.error("Couldn't create task wrapper", e);
            pageBase.error("Couldn't create task wrapper: " + e.getMessage());
            target.add(pageBase.getFeedbackPanel().getParent());

        } finally {
            result.computeStatusIfUnknown();

            if (!result.isError() && showProgressPopup) {
                showSimulationProgressPopup(pageBase, target, taskOid);
            } else {
                pageBase.showResult(result);
                target.add(pageBase.getFeedbackPanel().getParent());
            }
        }
    }

    private void showSimulationProgressPopup(
            @NotNull PageBase pageBase,
            AjaxRequestTarget target,
            String taskOid) {

        IModel<String> titleModel = isCorrelationFastSimulation
                ? pageBase.createStringResource(
                "SimulationProgressPanel.correlation.simulation.title")
                : pageBase.createStringResource(
                "SimulationProgressPanel.mapping.simulation.title");

        IModel<String> subTitleModel = isCorrelationFastSimulation
                ? pageBase.createStringResource(
                "SimulationProgressPanel.correlation.simulation.subTitle")
                : pageBase.createStringResource(
                "SimulationProgressPanel.mapping.simulation.subTitle");

        SimulationProgressPanel panel = new SimulationProgressPanel(pageBase.getMainPopupBodyId(), titleModel, subTitleModel,
                () -> loadTask(pageBase, taskOid));

        pageBase.showMainPopup(panel, target);
    }

    private @Nullable TaskType loadTask(@NotNull PageBase pageBase, String taskOid) {
        Task task = pageBase.createSimpleTask(OP_LOAD_TASK);

        PrismObject<TaskType> taskObject = WebModelServiceUtils.loadObject(
                TaskType.class, taskOid, null, true, pageBase, task, task.getResult());

        return taskObject != null ? taskObject.asObjectable() : null;
    }

    public void enableSampling() {
        this.isSamplingEnabled = true;
    }

    //TODO require BE implementation (simulation task not reporting progress yet)
    public void showProgressPopup() {
        this.showProgressPopup = true;
    }
}
