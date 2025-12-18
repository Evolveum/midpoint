/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.simulation.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.SplitButtonWithDropdownMenu;
import com.evolveum.midpoint.web.component.menu.cog.*;

import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
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
public abstract class SimulationActionTaskButton<T> extends BasePanel<ResourceObjectTypeDefinitionType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = SimulationActionTaskButton.class.getName() + ".";
    private static final String OP_COUNT_TASKS = DOT_CLASS + "countTasks";

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

    /**
     * Redirects to the simulation tasks wizard (to be implemented by subclasses).
     */
    public abstract void redirectToSimulationTasksWizard(AjaxRequestTarget target);

    protected abstract @NotNull ResourceTaskFlavor<T> getTaskFlavor();

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
                SimulationParams<T> params = new SimulationParams<>(
                        getPageBase(),
                        getResourceObject(),
                        getResourceObjectDefinition(),
                        getTaskFlavor(),
                        getWorkDefinitionConfiguration(),
                        getExecutionMode()
                );

                SimulationActionFlow<T> flow = new SimulationActionFlow<>(params);
                flow.start(target);
            }

            @Override
            protected boolean showIcon() {
                return true;
            }

            @Override
            protected String getAdditionalComponentCssClass() {
                return SimulationActionTaskButton.this.getAdditionalSplitComponentCssClass();
            }
        };
        simulationButton.setOutputMarkupId(true);
        return simulationButton;
    }
    protected ExecutionModeType getExecutionMode() {
        return ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW;
    }

    protected T getWorkDefinitionConfiguration() {
        return null;
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

    private @NotNull ResourceObjectTypeDefinitionType getResourceObjectDefinition() {
        return getModelObject();
    }

    protected ResourceType getResourceObject() {
        return resourceOidModel.getObject();
    }

    protected String getResourceOid() {
        return resourceOidModel.getObject().getOid();
    }

    protected String getAdditionalSplitComponentCssClass() {
        return null;
    }
}
