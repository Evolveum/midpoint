/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task;

import static java.util.Collections.singletonList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.component.OperationalButtonsPanel;

import com.evolveum.midpoint.gui.impl.page.admin.component.TaskOperationalButtonsPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ActivityStatisticsUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports;
import com.evolveum.midpoint.web.page.admin.server.LivesyncTokenEditorPanel;
import com.evolveum.midpoint.web.page.admin.server.TaskSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
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
    
    public PageTask(PageParameters pageParameters) {
        super(pageParameters);
    }

    @Override
    protected Class<TaskType> getType() {
        return TaskType.class;
    }

    @Override
    protected Panel getSummaryPanel(String id, LoadableModel<TaskType> summaryModel) {
        return new TaskSummaryPanel(id, summaryModel, this);
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
    protected OperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<TaskType>> wrapperModel) {
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
