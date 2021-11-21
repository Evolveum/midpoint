/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task;

import java.util.Collection;

import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
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
public class PageTask extends PageAssignmentHolderDetails<TaskType, TaskDetailsModel> {

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
    public Class<TaskType> getType() {
        return TaskType.class;
    }

    @Override
    protected TaskDetailsModel createObjectDetailsModels(PrismObject<TaskType> object) {
        return new TaskDetailsModel(createPrismObjectModel(object), PageTask.this);
    }

    @Override
    protected Panel createSummaryPanel(String id, LoadableModel<TaskType> summaryModel) {
        return new TaskSummaryPanel(id, summaryModel, getObjectDetailsModels().getRootTaskModel(), getSummaryPanelSpecification());
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

            protected void refresh(AjaxRequestTarget target) {
                PageTask.this.refresh(target);
            }

            @Override
            protected void savePerformed(AjaxRequestTarget target) {
                super.savePerformed(target);
                PageTask.this.savePerformed(target);
            }

        };
    }

    @Override
    protected void postProcessResult(OperationResult result, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, AjaxRequestTarget target) {
        if (executedDeltas == null) {
            super.postProcessResult(result, executedDeltas, target);
            return;
        }
        String taskOid = ObjectDeltaOperation.findFocusDeltaOidInCollection(executedDeltas);
        if (taskOid != null) {
            result.setBackgroundTaskOid(taskOid);
        }
        super.postProcessResult(result, executedDeltas, target);
    }

    @Override
    public void refresh(AjaxRequestTarget target, boolean soft) {
        if (isEditObject()) {
            ((TaskSummaryPanel) getSummaryPanel()).getTaskInfoModel().reset();
        }
        super.refresh(target, soft) ;
    }

}
