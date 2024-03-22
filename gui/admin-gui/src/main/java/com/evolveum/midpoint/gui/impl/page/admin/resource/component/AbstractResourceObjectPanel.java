/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItemWithCount;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.common.LocalizationTestUtil.getLocalizationService;

/**
 * Abstract class contains common methods for panels that show uncategorized and categorized resource objects.
 */
public abstract class AbstractResourceObjectPanel extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractResourceObjectPanel.class);

    private static final String DOT_CLASS = AbstractResourceObjectPanel.class.getName() + ".";
    private static final String OP_COUNT_TASKS = DOT_CLASS + "countTasks";
    private static final String OP_CREATE_TASK = DOT_CLASS + "createTask";

    public AbstractResourceObjectPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected final void createTasksButton(String id) {

        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new ButtonInlineMenuItem(createStringResource("ResourceObjectsPanel.button.createTask")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-plus-circle");
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        createTaskPerformed(target);
                    }
                };
            }
        });

        addViewMenuItemsForCreateTaskButton(items);

        DropdownButtonDto model = new DropdownButtonDto(
                null, "fa fa-tasks", getString("ResourceObjectsPanel.button.tasks"), items);
        DropdownButtonPanel createTask = new DropdownButtonPanel(id, model) {
            @Override
            protected String getSpecialButtonClass() {
                return "btn-sm btn-default";
            }

            protected String getSpecialDropdownMenuClass() {
                return "dropdown-menu-left";
            }

            @Override
            protected boolean showIcon() {
                return true;
            }
        };
        createTask.setOutputMarkupId(true);
        createTask.add(new VisibleBehaviour(this::isTaskButtonVisible));
        add(createTask);
    }

    protected boolean isTaskButtonVisible() {
        return true;
    }

    protected abstract void addViewMenuItemsForCreateTaskButton(List<InlineMenuItem> items);

    private void createTaskPerformed(AjaxRequestTarget target) {
        TaskCreationPopup<?> createTaskPopup = createNewTaskPopup();
        getPageBase().showMainPopup(createTaskPopup, target);

    }

    protected abstract TaskCreationPopup<?> createNewTaskPopup();

    protected void createNewTaskPerformed(SynchronizationTaskFlavor flavor, boolean isSimulation, AjaxRequestTarget target) {
        var newTask = getPageBase().taskAwareExecutor(target, OP_CREATE_TASK)
                .hideSuccessfulStatus()
                .run((task, result) -> {

                    ResourceType resource = getObjectDetailsModels().getObjectType();
                    ResourceTaskCreator creator =
                            ResourceTaskCreator.forResource(resource, getPageBase())
                                    .ofFlavor(flavor)
                                    .ownedByCurrentUser()
                                    .withCoordinates(
                                            getKind(), // FIXME not static
                                            getIntent(), // FIXME not static
                                            getObjectClass()); // FIXME not static

                    if (isSimulation) {
                        creator = creator
                                .withExecutionMode(ExecutionModeType.PREVIEW)
                                .withPredefinedConfiguration(PredefinedConfigurationType.DEVELOPMENT)
                                .withSimulationResultDefinition(
                                        new SimulationDefinitionType().useOwnPartitionForProcessedObjects(false));
                    }

                    customizeTaskCreator(creator, isSimulation);

                    return creator.create(task, result);
                });

        if (newTask != null) {
            DetailsPageUtil.dispatchToNewObject(newTask, getPageBase());
        }
    }

    protected void customizeTaskCreator(ResourceTaskCreator creator, boolean isSimulation) {

    }

    protected abstract QName getObjectClass();

    protected String getIntent() {
        return null;
    }

    protected ShadowKindType getKind() {
        return null;
    }

    protected final InlineMenuItem createTaskViewMenuItem(StringResourceModel label, String archetypeOid, boolean isSimulationTasks) {
        return new ButtonInlineMenuItemWithCount(label) {
            @Override
            protected boolean isBadgeVisible() {
                if (!getPageBase().isNativeRepo()) {
                    return false;
                }

                return super.isBadgeVisible();
            }

            @Override
            public int getCount() {
                if (!getPageBase().isNativeRepo()) {
                    return 0;
                }

                ObjectQuery query = createQueryForTasks(isSimulationTasks);
                if (archetypeOid != null) {
                    query.addFilter(PrismContext.get()
                            .queryFor(TaskType.class)
                            .item(TaskType.F_ARCHETYPE_REF)
                            .ref(archetypeOid)
                            .buildFilter());
                }

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

                        if (warnIfNonNative(target)) {
                            return;
                        }

                        redirectToTasksListPage(archetypeOid, isSimulationTasks);
                    }

                };
            }

        };
    }

    protected final S_FilterExit addSimulationRule(S_FilterEntry filter, boolean useNegation, Object value) {
        if (useNegation) {
            filter = filter.not();
        }

        return filter
                .item(ItemPath.create(
                        TaskType.F_AFFECTED_OBJECTS,
                        TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_EXECUTION_MODE))
                .eq(value);
    }

    private void redirectToTasksListPage(@Nullable String archetypeOid, boolean isSimulationTasks) {
        PageParameters pageParameters = new PageParameters();
        if (archetypeOid != null) {
            String taskCollectionViewName =
                    Objects.requireNonNull(getPageBase().getCompiledGuiProfile().findApplicableArchetypeView(archetypeOid)).getViewIdentifier();

            if (StringUtils.isNotEmpty(taskCollectionViewName)) {
                pageParameters.add(PageBase.PARAMETER_OBJECT_COLLECTION_NAME, taskCollectionViewName);
            }
        }

        ObjectQuery query = createQueryForTasks(isSimulationTasks);

        PageTasks pageTasks = new PageTasks(query, pageParameters);
        getPageBase().setResponsePage(pageTasks);
    }

    private boolean warnIfNonNative(AjaxRequestTarget target) {
        if (!getPageBase().isNativeRepo()) {
            String warnMessage = getString("PageAdmin.operation.nonNativeRepositoryWarning");
            String localeWarnMessage = getLocalizationService()
                    .translate(PolyString.fromOrig(warnMessage),
                            WebComponentUtil.getCurrentLocale(), true);
            warn(localeWarnMessage);
            target.add(getPageBase().getFeedbackPanel());
            return true;
        }
        return false;
    }

    protected abstract ObjectQuery createQueryForTasks(boolean isSimulationTasks);
}
