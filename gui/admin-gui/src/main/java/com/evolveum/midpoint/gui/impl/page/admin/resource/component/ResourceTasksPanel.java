/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import static com.evolveum.midpoint.web.page.admin.resources.ResourceContentPanel.*;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.result.OpResult;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.ObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "resourceTasks")
@PanelInstance(identifier = "resourceTasks", applicableForType = ResourceType.class, applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "PageResource.tab.content.tasks", icon = GuiStyleConstants.CLASS_OBJECT_TASK_ICON, order = 40))
public class ResourceTasksPanel extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> implements Popupable {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = ResourceTasksPanel.class.getName() + ".";

    private static final String OPERATION_LOAD_TASKS = DOT_CLASS + "loadTasks";
    private static final String OP_CREATE_TASK = DOT_CLASS + "createTask";

    private static final String ID_TASKS_TABLE = "taskTable";

    private static final String ID_RUN_NOW = "runNow";
    private static final String ID_RESUME = "resume";
    private static final String ID_SUSPEND = "suspend";

    // TODO reconcile with SynchronizationTaskFlavor?
    private final String[] resourceTaskArchetypeOids = new String[] {
            SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value(),
            SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value(),
            SystemObjectsType.ARCHETYPE_IMPORT_TASK.value(),
            SystemObjectsType.ARCHETYPE_ASYNC_UPDATE_TASK.value() };

    public ResourceTasksPanel(String id, final ResourceDetailsModel resourceModel, ContainerPanelConfigurationType config) {
        super(id, resourceModel, config);
    }

    protected void initLayout() {
        final MainObjectListPanel<TaskType> tasksPanel =
                new MainObjectListPanel<>(ID_TASKS_TABLE, TaskType.class, null) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return UserProfileStorage.TableId.PAGE_RESOURCE_TASKS_PANEL;
                    }

                    @Override
                    protected ISelectableDataProvider<SelectableBean<TaskType>> createProvider() {
                        return createSelectableBeanObjectDataProvider(() -> createResourceTasksQuery(), null);
                    }

                    @Override
                    protected List<InlineMenuItem> createInlineMenu() {
                        return null;
                    }

                    @Override
                    protected void newObjectPerformed(
                            AjaxRequestTarget target, AssignmentObjectRelation relation,
                            CompiledObjectCollectionView collectionView) {

                        getPageBase().taskAwareExecutor(target, OP_CREATE_TASK)
                                .withOpResultOptions(
                                        OpResult.Options.create()
                                                .withHideSuccess(true)
                                                .withHideInProgress(true))
                                .runVoid((task, result) -> {
                                    @Nullable var finalCollectionView =
                                            collectionView != null ? collectionView : getObjectCollectionView();
                                    PrismObject<ResourceType> resource = ResourceTasksPanel.this.getObjectWrapper().getObject();
                                    var newTask = ResourceTaskCreator.forResource(resource.asObjectable(), getPageBase())
                                            .withArchetype(finalCollectionView != null ? finalCollectionView.getArchetypeOid() : null)
                                            .withCoordinates(
                                                    getKind(), // FIXME not static
                                                    getIntent(), // FIXME not static
                                                    getObjectClass()) // FIXME not static
                                            .create(task, result);

                                    WebComponentUtil.dispatchToNewObject(newTask, getPageBase());
                                });
                    }

                    @Override
                    protected List<IColumn<SelectableBean<TaskType>, String>> createDefaultColumns() {
                        return ColumnUtils.getDefaultTaskColumns();
                    }

                    @Override
                    protected @NotNull List<CompiledObjectCollectionView> getNewObjectInfluencesList() {
                        List<CompiledObjectCollectionView> newObjectInfluencesList = super.getNewObjectInfluencesList();
                        List<CompiledObjectCollectionView> filteredInfluencesList = new ArrayList<>();
                        if (newObjectInfluencesList != null) {
                            newObjectInfluencesList.forEach(influence -> {
                                if (influence.getCollection() != null && influence.getCollection().getCollectionRef() != null &&
                                        ArrayUtils.contains(resourceTaskArchetypeOids, influence.getCollection().getCollectionRef().getOid())) {
                                    filteredInfluencesList.add(influence);
                                }
                            });
                        }
                        return filteredInfluencesList;
                    }
                };
        tasksPanel.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_TASK_BOX_CSS_CLASSES);
        add(tasksPanel);

        AjaxButton runNow = new AjaxButton(ID_RUN_NOW, getPageBase().createStringResource("pageTaskEdit.button.runNow")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<String> oids = createOidList(getTaskListPanel().getSelectedRealObjects());
                if (!oids.isEmpty()) {
                    OperationResult result = TaskOperationUtils.runNowPerformed(oids, getPageBase());
                    getPageBase().showResult(result);
                } else {
                    noTasksSelected();
                }
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        add(runNow);

        AjaxButton resume = new AjaxButton(ID_RESUME, getPageBase().createStringResource("pageTaskEdit.button.resume")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<TaskType> tasks = getTaskListPanel().getSelectedRealObjects();
                if (!tasks.isEmpty()) {
                    OperationResult result = TaskOperationUtils.resumeTasks(tasks, getPageBase());
                    getPageBase().showResult(result);
                } else {
                    noTasksSelected();
                }
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        add(resume);

        AjaxButton suspend = new AjaxButton(ID_SUSPEND, getPageBase().createStringResource("pageTaskEdit.button.suspend")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<TaskType> tasks = getTaskListPanel().getSelectedRealObjects();
                if (!tasks.isEmpty()) {
                    OperationResult result = TaskOperationUtils.suspendTasks(tasks, getPageBase());
                    getPageBase().showResult(result);
                } else {
                    noTasksSelected();
                }
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        add(suspend);
    }

    private ObjectQuery createResourceTasksQuery() {
        return getPageBase().getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(getObjectWrapper().getOid())
                .build();
    }

    private void noTasksSelected() {
        warn(getString("ResourceTasksPanel.noTasksSelected"));
    }

    private ObjectListPanel<TaskType> getTaskListPanel() {
        //noinspection unchecked
        return (ObjectListPanel<TaskType>) get(ID_TASKS_TABLE);
    }

    private List<String> createOidList(List<TaskType> tasks) {
        List<String> oids = new ArrayList<>();
        for (TaskType task : tasks) {
            oids.add(task.getOid());
        }
        return oids;
    }

    @Override
    public int getWidth() {
        return 900;
    }

    @Override
    public int getHeight() {
        return 500;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return getPageBase().createStringResource("ResourceTasksPanel.definedTasks");
    }

}
