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
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangeExecutor;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangesExecutorImpl;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceTaskCreator;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TileChoicePopup;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SimulationActionFlow<T> implements Serializable {

    private static final String DOT_CLASS = SimulationActionFlow.class.getName() + ".";
    private static final String OP_CREATE_TASK = DOT_CLASS + "createTask";

    private static final Trace LOGGER = TraceManager.getTrace(SimulationActionFlow.class);

    private final SimulationParams<T> context;

    public SimulationActionFlow(@NotNull SimulationParams<T> context) {
        this.context = context;
    }

    /**
     * Entry point: show popup (or run directly if config disabled).
     */
    public void start(AjaxRequestTarget target) {
        ResourceObjectTypeDefinitionType def = context.definition();
        PredefinedConfigurationType defaultCfg = PredefinedConfigurationType.DEVELOPMENT;

        String lifecycleState = def.getLifecycleState();
        boolean isConfigurationEnabled = lifecycleState == null
                || !lifecycleState.equals(ShadowLifecycleStateType.PROPOSED.value());

        if (!isConfigurationEnabled) {
            runSimulation(target, defaultCfg);
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
                return createStringResource("SimulationActionButton.simulate.save.and.execute");
            }

            @Override
            protected void performAction(AjaxRequestTarget target, PredefinedConfigurationType value) {
                runSimulation(target, value);
            }
        };

        pageBase.showMainPopup(popup, target);
    }

    protected void runSimulation(AjaxRequestTarget target, PredefinedConfigurationType cfg) {
        PageBase pageBase = context.pageBase();
        ResourceType resource = context.resource();
        ResourceObjectTypeDefinitionType def = context.definition();
        ResourceTaskFlavor<T> flavor = context.flavor();

        TaskType newTask = pageBase.taskAwareExecutor(target, OP_CREATE_TASK)
                .hideSuccessfulStatus()
                .run((task, result) -> createTask(task, result, pageBase, resource, def, flavor, cfg));

        saveAndPerformSimulation(target, pageBase, newTask);
    }

    private @Nullable TaskType createTask(
            Task task,
            OperationResult result,
            PageBase pageBase,
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
     * Persists the newly created task immediately.
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

        PrismObject<TaskType> object = newTask.asPrismObject();
        PrismObjectWrapperFactory<TaskType> factory =
                pageBase.findObjectWrapperFactory(object.getDefinition());
        WrapperContext ctx = new WrapperContext(task, result);
        ctx.setCreateIfEmpty(true);

        try {
            PrismObjectWrapper<TaskType> wrapper =
                    factory.createObjectWrapper(object, ItemStatus.ADDED, ctx);
            WebComponentUtil.setTaskStateBeforeSave(wrapper, true, pageBase, target);

            ObjectDelta<TaskType> objectDelta = wrapper.getObjectDelta();
            ObjectChangeExecutor changeExecutor = new ObjectChangesExecutorImpl();
            changeExecutor.executeChanges(Collections.singleton(objectDelta), false, task, result, target);
        } catch (CommonException e) {
            LOGGER.error("Couldn't create task wrapper", e);
            pageBase.error("Couldn't create task wrapper: " + e.getMessage());
            target.add(pageBase.getFeedbackPanel().getParent());
        } finally {
            result.computeStatusIfUnknown();
            pageBase.showResult(result);
            target.add(pageBase.getFeedbackPanel().getParent());
        }
    }
}
