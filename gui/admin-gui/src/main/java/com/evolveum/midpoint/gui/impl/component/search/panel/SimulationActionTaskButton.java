/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangeExecutor;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangesExecutorImpl;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceTaskCreator;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TileChoicePopup;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.*;

import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.cases.api.util.QueryUtils.createQueryForObjectTypeSimulationTasks;

/**
 * Button panel for managing simulation tasks on a resource object type.
 * <p>
 * Provides a split-button to:
 * <ul>
 *   <li>Start a new simulation with predefined configuration</li>
 *   <li>View existing simulation tasks with a badge counter</li>
 * </ul>
 *
 * Subclasses must implement {@link #redirectToSimulationTasksWizard(AjaxRequestTarget)}.
 */
public abstract class SimulationActionTaskButton extends BasePanel<ResourceObjectTypeDefinitionType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = SimulationActionTaskButton.class.getName() + ".";
    private static final String OP_COUNT_TASKS = DOT_CLASS + "countTasks";
    private static final String OP_CREATE_TASK = DOT_CLASS + "createTask";

    private static final Trace LOGGER = TraceManager.getTrace(SimulationActionTaskButton.class);

    protected static final String ID_COMPONENT = "component";

    IModel<ResourceType> resourceOidModel;

    public SimulationActionTaskButton(
            @NotNull String id,
            @NotNull IModel<ResourceObjectTypeDefinitionType> model,
            @NotNull IModel<ResourceType> resourceOid) {
        super(id, model);
        this.resourceOidModel = resourceOid;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        SplitButtonWithDropdownMenu simulationMenuButton = createSimulationMenuButton();
        simulationMenuButton.setRenderBodyOnly(true);
        add(simulationMenuButton);
    }

    /**
     * Creates the split-button with simulation actions.
     */
    protected final @NotNull SplitButtonWithDropdownMenu createSimulationMenuButton() {
        List<InlineMenuItem> items = List.of(createSimulationTaskViewMenuItem());
        DropdownButtonDto model = new DropdownButtonDto(
                null, "fa fa-flask", getString("CorrelationItemsTableWizardPanel.simulate.title"), items);
        SplitButtonWithDropdownMenu simulationButton = new SplitButtonWithDropdownMenu(SimulationActionTaskButton.ID_COMPONENT, () -> model) {
            @Override
            protected void performPrimaryButtonAction(AjaxRequestTarget target) {
                showPredefinedConfigPopup(target);
            }

            @Override
            protected boolean showIcon() {
                return true;
            }
        };
        simulationButton.setOutputMarkupId(true);
        return simulationButton;
    }

    /**
     * Shows a popup for selecting a predefined simulation configuration.
     */
    protected void showPredefinedConfigPopup(AjaxRequestTarget target) {
        PredefinedConfigurationType defaultSimulationPredefinedConf = PredefinedConfigurationType.DEVELOPMENT;

        ResourceObjectTypeDefinitionType modelObject = getModelObject();
        String lifecycleState = modelObject.getLifecycleState();
        boolean isConfigurationEnabled = lifecycleState == null
                || !lifecycleState.equals(ShadowLifecycleStateType.PROPOSED.value());

        if (!isConfigurationEnabled) {
            createNewTaskPerformed(target, defaultSimulationPredefinedConf);
            return;
        }

        List<Tile<PredefinedConfigurationType>> tiles = Arrays.stream(PredefinedConfigurationType.values())
                .map(cfg -> {
                    String icon = cfg == PredefinedConfigurationType.PRODUCTION
                            ? "fa-solid fa-industry"
                            : "fa-solid fa-flask";

                    Tile<PredefinedConfigurationType> tile = new Tile<>(icon,
                            getString("PredefinedConfigurationType." + cfg.name()));
                    tile.setValue(cfg);
                    tile.setDescription(getString(
                            "PredefinedConfigurationType." + cfg.name() + ".description"));
                    return tile;
                })
                .toList();

        TileChoicePopup<PredefinedConfigurationType> popup = new TileChoicePopup<>(
                getPageBase().getMainPopupBodyId(),
                () -> tiles,
                defaultSimulationPredefinedConf) {

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
                createNewTaskPerformed(target, value);
            }
        };

        getPageBase().showMainPopup(popup, target);
    }

    /**
     * Creates and saves a new simulation task for the selected configuration.
     */
    protected void createNewTaskPerformed(AjaxRequestTarget target, PredefinedConfigurationType value) {
        TaskType newTask = getPageBase().taskAwareExecutor(target, OP_CREATE_TASK)
                .hideSuccessfulStatus()
                .run((task, result) -> {

                    @Nullable ResourceObjectTypeDefinitionType resourceObjectTypeDef = getResourceObjectDefinition();
                    if (resourceObjectTypeDef == null) {
                        result.recordWarning(createStringResource("SimulationActionButton.noResourceObjectDefinition")
                                .getString());
                        return null;
                    }
                    ResourceType resource = getResourceObject();
                    ResourceTaskCreator creator =
                            ResourceTaskCreator.forResource(resource, getPageBase())
                                    .ofFlavor(getSynchronizationTaskFlavor())
                                    .ownedByCurrentUser()
                                    .withCoordinates(
                                            resourceObjectTypeDef.getKind(),
                                            resourceObjectTypeDef.getIntent(),
                                            resourceObjectTypeDef.getObjectClass());

                    creator = creator
                            .withExecutionMode(getExecutionMode())
                            .withPredefinedConfiguration(value)
                            .withSimulationResultDefinition(
                                    new SimulationDefinitionType().useOwnPartitionForProcessedObjects(false));

                    return creator.create(task, result);
                });

        saveAndPerformSimulation(target, newTask);
    }

    protected @NotNull SynchronizationTaskFlavor getSynchronizationTaskFlavor() {
        return SynchronizationTaskFlavor.IMPORT;
    }

    protected ExecutionModeType getExecutionMode() {
        return ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW;
    }

    /**
     * Persists the newly created task immediately:
     * - Wraps it into a PrismObjectWrapper
     * - Prepares delta and executes it via ObjectChangesExecutor
     * - Ensures task is saved to repository before continuing
     */
    private void saveAndPerformSimulation(AjaxRequestTarget target, TaskType newTask) {
        if (newTask != null) {
            Task task = getPageBase().createSimpleTask(OP_CREATE_TASK);
            OperationResult result = task.getResult();

            PrismObject<TaskType> object = newTask.asPrismObject();
            PrismObjectWrapperFactory<TaskType> factory = getPageBase().findObjectWrapperFactory(object.getDefinition());
            WrapperContext ctx = new WrapperContext(task, result);
            ctx.setCreateIfEmpty(true);

            try {
                PrismObjectWrapper<TaskType> wrapper = factory.createObjectWrapper(object, ItemStatus.ADDED, ctx);
                WebComponentUtil.setTaskStateBeforeSave(wrapper, true, getPageBase(), target);
                ObjectDelta<TaskType> objectDelta = wrapper.getObjectDelta();
                ObjectChangeExecutor changeExecutor = new ObjectChangesExecutorImpl();
                changeExecutor.executeChanges(Collections.singleton(objectDelta), false, task, result, target);
            } catch (CommonException e) {
                LOGGER.error("Couldn't create task wrapper", e);
                getPageBase().error("Couldn't create task wrapper: " + e.getMessage());
                target.add(getPageBase().getFeedbackPanel().getParent());
            } finally {
                result.computeStatusIfUnknown();
                getPageBase().showResult(result);
                target.add(getPageBase().getFeedbackPanel().getParent());
            }
        }
    }

    /**
     * Creates a menu item for viewing existing simulation tasks with a count badge.
     */
    protected final @NotNull InlineMenuItem createSimulationTaskViewMenuItem() {
        return new ButtonInlineMenuItemWithCount(createStringResource("ResourceObjectsPanel.button.viewSimulatedTasks")) {
            @Override
            protected boolean isBadgeVisible() {
                if (!getPageBase().isNativeRepo()) {
                    return false;
                }

                return super.isBadgeVisible();
            }

            @Override
            public int getCount() {
                String resourceOid = getResourceOid();
                ObjectQuery query = createQueryForObjectTypeSimulationTasks(getResourceObjectDefinition(), resourceOid);
                Task task = getPageBase().createSimpleTask(OP_COUNT_TASKS);
                Integer count = null;
                try {
                    count = getPageBase().getModelService().countObjects(
                            TaskType.class, query, null, task, task.getResult());
                } catch (CommonException e) {
                    LOGGER.error("Couldn't count tasks");
                    getPageBase().showResult(task.getResult());
                }

                return Objects.requireNonNullElse(count, 0);
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-eye");
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        redirectToSimulationTasksWizard(target);
                    }

                };
            }

        };
    }

    /**
     * Redirects to the simulation tasks wizard (to be implemented by subclasses).
     */
    public abstract void redirectToSimulationTasksWizard(AjaxRequestTarget target);

    private @Nullable ResourceObjectTypeDefinitionType getResourceObjectDefinition() {
        return getModelObject();
    }

    protected ResourceType getResourceObject() {
        return resourceOidModel.getObject();
    }

    protected String getResourceOid() {
        return resourceOidModel.getObject().getOid();
    }

}
