/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task;

import java.util.Collection;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.OperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.component.TaskOperationalButtonsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.page.admin.server.TaskSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/taskNew")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASK_URL,
                        label = "PageUser.auth.user.label",
                        description = "PageUser.auth.user.description")
        })
public class PageTask extends PageAssignmentHolderDetails<TaskType, AssignmentHolderDetailsModel<TaskType>> {

    public PageTask() {
        super();
    }

    public PageTask(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageTask(PrismObject<TaskType> task) {
        super(task);
    }

    @Override
    protected Class<TaskType> getType() {
        return TaskType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, LoadableModel<TaskType> summaryModel) {
        // TODO create root task model in constructor - however, this method is called from super(), so it is executed
        //  before local constructors have a chance to run!
        //
        // TODO Propagate loaded root also to other parts of task page, namely to subtasks (and their subtasks, etc)
        //  (Unless we want to have really fresh information there.)
        LoadableModel<TaskType> rootTaskModel = RootTaskLoader.createRootTaskModel(this::getModelObjectType, () -> this);
        return new TaskSummaryPanel(id, summaryModel, rootTaskModel, this);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getOperationOptions() {
        return getOperationOptionsBuilder()
                // retrieve
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .item(TaskType.F_NODE_AS_OBSERVED).retrieve()
                .item(TaskType.F_NEXT_RUN_START_TIMESTAMP).retrieve()
                .item(TaskType.F_NEXT_RETRY_TIMESTAMP).retrieve()
                .item(TaskType.F_RESULT).retrieve()         // todo maybe only when it is to be displayed
                .build();
    }

    @Override
    protected TaskOperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<TaskType>> wrapperModel) {
        return new TaskOperationalButtonsPanel(id, wrapperModel) {

            @Override
            protected void afterOperation(AjaxRequestTarget target, OperationResult result) {
                PageTask.this.afterOperation(target, result);
            }

            protected boolean isRefreshEnabled() {
                return PageTask.this.isRefreshEnabled();
            }

            protected void refresh(AjaxRequestTarget target) {
                PageTask.this.refresh(target);
            }

            protected int getRefreshInterval() {
                return PageTask.this.getRefreshInterval();
            }
        };
    }

    private void afterOperation(AjaxRequestTarget target, OperationResult result) {
        showResult(result);
        getModel().reset();
        refresh(target);
    }

}
