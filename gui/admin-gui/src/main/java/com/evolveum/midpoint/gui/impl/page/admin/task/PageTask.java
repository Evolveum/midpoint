/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangeExecutor;

import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangesExecutorImpl;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.TaskOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.component.TaskWizardPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.server.TaskSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/task")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                        label = "PageTask.auth.tasksAll.label",
                        description = "PageTask.auth.tasksAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TASK_URL,
                        label = "PageTask.auth.task.label",
                        description = "PageTask.auth.task.description")
        })
public class PageTask extends PageAssignmentHolderDetails<TaskType, TaskDetailsModel> {

    private boolean runWizard;

    public PageTask() {
        super();
    }

    public PageTask(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageTask(PrismObject<TaskType> task) {
        super(task);
        if (task.asObjectable().getActivity() != null) {
            this.runWizard = true;
        }
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
    protected Panel createSummaryPanel(String id, IModel<TaskType> summaryModel) {
        return new TaskSummaryPanel(id, summaryModel, getObjectDetailsModels().getRootTaskModel(), getSummaryPanelSpecification());
    }

    @Override
    protected boolean canShowWizard() {
        return runWizard;
    }

    @Override
    protected DetailsFragment createWizardFragment() {
        return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageTask.this) {
            @Override
            protected void initFragmentLayout() {
                TaskWizardPanel wizardPanel = new TaskWizardPanel(ID_TEMPLATE, createObjectWizardPanelHelper());
                add(wizardPanel);
            }
        };

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
            protected void submitPerformed(AjaxRequestTarget target) {
                super.submitPerformed(target);
                PageTask.this.savePerformed(target);
            }

            @Override
            protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
                return PageTask.this.hasUnsavedChanges(target);
            }
        };
    }

    @Override
    protected void showResultAfterExecuteChanges(ObjectChangeExecutor changeExecutor, OperationResult result) {
        if (changeExecutor instanceof ObjectChangesExecutorImpl) {
            showResult(result);
        }
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
    protected void postProcessResultForWizard(OperationResult result, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, AjaxRequestTarget target) {
        super.postProcessResultForWizard(result, executedDeltas, target);
        postProcessResult(result, executedDeltas, target);
    }

    @Override
    public void refresh(AjaxRequestTarget target, boolean soft) {
        if (isEditObject()) {
            ((TaskSummaryPanel) getSummaryPanel()).getTaskInfoModel().reset();
        }
        super.refresh(target, soft);
    }

    @Override
    protected Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews() {
        return TaskOperationUtils.getAllApplicableArchetypeForNewTask(this);
    }

    @Override
    protected List<ObjectReferenceType> getArchetypeReferencesList(CompiledObjectCollectionView collectionView) {
        return TaskOperationUtils.getArchetypeReferencesList(collectionView);
    }
}
