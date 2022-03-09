/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.task;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.TaskOperationalButtonsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.page.admin.server.TaskSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Collection;

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
        if (object != null && object.getOid() == null) {
            // we're adding  task, let's see what we can prepare for use beforehand (not very nice in here, but so far there's no other place)
            prepopulateTask(object);
        }
        return new TaskDetailsModel(createPrismObjectModel(object), PageTask.this);
    }

    @Deprecated
    private void prepopulateTask(PrismObject<TaskType> object) {
        TaskType task = object.asObjectable();
        if (task.getOwnerRef() == null) {
            try {
                FocusType focus = getSecurityContextManager().getPrincipal().getFocus();
                task.ownerRef(getSecurityContextManager().getPrincipalOid(), focus.asPrismObject().getDefinition().getTypeName());
            } catch (SecurityViolationException e) {
                // we can ignore it here probably
            }
        }

        if (task.getName() == null) {
            task.setName(createDefaultTaskName(task));
        }
    }

    private PolyStringType createDefaultTaskName(TaskType task) {
        String archetypeOid = null;
        for (AssignmentType a : task.getAssignment()) {
            ObjectReferenceType targetRef = a.getTargetRef();
            if (targetRef == null || !ArchetypeType.COMPLEX_TYPE.equals(targetRef.getType())) {
                continue;
            }

            archetypeOid = targetRef.getOid();
            break;
        }

        if (archetypeOid == null) {
            return new PolyStringType(getString("PageTask.newTaskDefaultName"));
        }

        Task t = createSimpleTask("Load archetype");
        PrismObject<ArchetypeType> archetype = WebModelServiceUtils.loadObject(ArchetypeType.class, archetypeOid, this, t, t.getResult());
        if (archetype != null) {
            return new PolyStringType(archetype.getName().getOrig());
        }

        return new PolyStringType(getString("PageTask.newTaskDefaultName"));
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<TaskType> summaryModel) {
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
        super.refresh(target, soft);
    }

}
