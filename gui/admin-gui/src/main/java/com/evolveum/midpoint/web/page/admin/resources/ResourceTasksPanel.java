/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.ObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.page.admin.server.PageTask;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class ResourceTasksPanel extends Panel implements Popupable{
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = ResourceTasksPanel.class.getName() + ".";

    private static final String OPERATION_LOAD_TASKS = DOT_CLASS + "loadTasks";

    private static final String ID_TASKS_TABLE = "taskTable";

    private static final String ID_RUN_NOW = "runNow";
    private static final String ID_RESUME = "resume";
    private static final String ID_SUSPEND = "suspend";

    private PageBase pageBase;

    public ResourceTasksPanel(String id, ListModel<TaskType> tasks, PageBase pageBase) {
        super(id);
        this.pageBase = pageBase;

        initLayout(tasks);
    }

    public ResourceTasksPanel(String id, final IModel<PrismObject<ResourceType>> resourceModel, PageBase pageBase) {
        super(id);
        this.pageBase = pageBase;

        ListModel<TaskType> model = createTaskModel(resourceModel.getObject());
        initLayout(model);
    }

    private ListModel<TaskType> createTaskModel(PrismObject<ResourceType> object) {
        OperationResult result = new OperationResult(OPERATION_LOAD_TASKS);
        List<PrismObject<TaskType>> tasks = WebModelServiceUtils
                .searchObjects(TaskType.class,
                        pageBase.getPrismContext().queryFor(TaskType.class)
                                .item(TaskType.F_OBJECT_REF).ref(object.getOid())
                                .build(),
                        result, pageBase);
        List<TaskType> tasksType = new ArrayList<>();
        for (PrismObject<TaskType> task : tasks) {
            tasksType.add(task.asObjectable());
        }
        return new ListModel<>(tasksType);

    }

    private void initLayout(final ListModel<TaskType> tasks){
        final MainObjectListPanel<TaskType> tasksPanel =
                new MainObjectListPanel<TaskType>(ID_TASKS_TABLE, TaskType.class, TableId.PAGE_RESOURCE_TASKS_PANEL, null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected BaseSortableDataProvider<SelectableBean<TaskType>> initProvider() {
                return new SelectableListDataProvider<>(pageBase, tasks);
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return null;
            }

            @Override
            public void objectDetailsPerformed(AjaxRequestTarget target, TaskType task) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, task.getOid());
                getPageBase().navigateToNext(PageTask.class, parameters);
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
                getPageBase().navigateToNext(PageTask.class);

            }

            @Override
            protected List<IColumn<SelectableBean<TaskType>, String>> createColumns() {
                return ColumnUtils.getDefaultTaskColumns();
            }
        };
        tasksPanel.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_TASK_BOX_CSS_CLASSES);
        add(tasksPanel);

        AjaxButton runNow = new AjaxButton(ID_RUN_NOW, pageBase.createStringResource("pageTaskEdit.button.runNow")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<String> oids = createOidList(getTaskListPanel().getSelectedObjects());
                if (!oids.isEmpty()) {
                    OperationResult result = TaskOperationUtils.runNowPerformed(pageBase.getTaskService(), oids, pageBase);
                    pageBase.showResult(result);
                } else {
                    noTasksSelected();
                }
                target.add(pageBase.getFeedbackPanel());
            }
        };
        add(runNow);

        AjaxButton resume = new AjaxButton(ID_RESUME, pageBase.createStringResource("pageTaskEdit.button.resume")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<String> oids = createOidList(getTaskListPanel().getSelectedObjects());
                if (!oids.isEmpty()) {
                    OperationResult result = TaskOperationUtils.resumePerformed(pageBase.getTaskService(), oids, pageBase);
                    pageBase.showResult(result);
                } else {
                    noTasksSelected();
                }
                target.add(pageBase.getFeedbackPanel());
            }
        };
        add(resume);

        AjaxButton suspend = new AjaxButton(ID_SUSPEND, pageBase.createStringResource("pageTaskEdit.button.suspend")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<String> oids = createOidList(getTaskListPanel().getSelectedObjects());
                if (!oids.isEmpty()) {
                    OperationResult result = TaskOperationUtils.suspendPerformed(pageBase.getTaskService(), oids, pageBase);
                    pageBase.showResult(result);
                } else {
                    noTasksSelected();
                }
                target.add(pageBase.getFeedbackPanel());
            }
        };
        add(suspend);
    }

    private void noTasksSelected() {
        warn(getString("ResourceTasksPanel.noTasksSelected"));
    }

    private ObjectListPanel<TaskType> getTaskListPanel(){
        //noinspection unchecked
        return (ObjectListPanel<TaskType>) get(ID_TASKS_TABLE);
    }

    private List<String> createOidList(List<TaskType> tasks){
        List<String> oids = new ArrayList<>();
        for (TaskType task : tasks){
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
    public String getWidthUnit(){
        return "px";
    }

    @Override
    public String getHeightUnit(){
        return "px";
    }

    @Override
    public StringResourceModel getTitle() {
        return pageBase.createStringResource("ResourceTasksPanel.definedTasks");
    }

    @Override
    public Component getComponent() {
        return this;
    }

}
