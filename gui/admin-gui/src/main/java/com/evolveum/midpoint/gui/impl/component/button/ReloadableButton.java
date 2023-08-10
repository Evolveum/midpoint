/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.button;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceObjectsPanel;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public abstract class ReloadableButton extends AjaxIconButton {

    private static final String DOT_CLASS = ResourceObjectsPanel.class.getName() + ".";
    protected static final String OPERATION_RELOAD = DOT_CLASS + "reload";

    private AjaxSelfUpdatingTimerBehavior reloadedBehaviour;
    private String taskOidForReloaded;

    private final PageBase pageBase;

    public ReloadableButton(String id, PageBase pageBase) {
        super(id, Model.of(""), pageBase.createStringResource("ReloadableButton.reload"));
        this.pageBase = pageBase;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setModel(createIconModel());

        add(AttributeAppender.append("class", "btn btn-primary btn-sm mr-2"));
        setOutputMarkupId(true);
        showTitleAsLabel(true);

        add(AttributeAppender.append("class", (IModel<String>) () -> taskOidForReloaded != null ? "disabled" : ""));

        if (taskOidForReloaded != null) {
            add(reloadedBehaviour);
        }
    }

    private IModel<String> createIconModel() {
        return () -> {
            if (taskOidForReloaded == null) {
                return "fa fa-rotate-right";
            }
            return "fa fa-spinner fa-spin-pulse";
        };
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        taskOidForReloaded = pageBase.taskAwareExecutor(target, OPERATION_RELOAD)
                .withOpResultOptions(
                        OpResult.Options.create()
                                .withHideSuccess(true)
                                .withHideInProgress(true))
                .run((task, result) -> pageBase.getModelInteractionService().submit(
                        createActivityDefinition(),
                        ActivitySubmissionOptions.create()
                                .withTaskTemplate(new TaskType()
                                        .name(getTaskName())
                                        .cleanupAfterCompletion(XmlTypeConverter.createDuration("PT0S"))),
                        task, result)
                );
        reloadedBehaviour = new AjaxSelfUpdatingTimerBehavior(Duration.ofSeconds(5)) {

            @Override
            protected void onPostProcessTarget(AjaxRequestTarget target) {
                super.onPostProcessTarget(target);
                if (taskOidForReloaded == null) {
                    stop(target);
                    refresh(target);
                    return;
                }

                Task task = pageBase.createSimpleTask("Load task");
                @Nullable PrismObject<TaskType> taskBean = WebModelServiceUtils.loadObject(
                        TaskType.class, taskOidForReloaded, null, true, pageBase, task, task.getResult());
                if (taskBean == null || WebComponentUtil.isClosedTask(taskBean.asObjectable())) {
                    stop(target);
                    taskOidForReloaded = null;
                }
                if (WebComponentUtil.isSuspendedTask(taskBean.asObjectable())) {
                    OperationResult taskResult = OperationResult.createOperationResult(taskBean.asObjectable().getResult());
                    if (taskResult != null && (taskResult.isFatalError() || taskResult.isPartialError())) {
                        stop(target);
                        pageBase.showResult(taskResult);
                        target.add(pageBase.getFeedbackPanel());
                        taskOidForReloaded = null;
                    }
                }
                refresh(target);
            }
        };
        add(reloadedBehaviour);
        refresh(target);
    }

    protected abstract void refresh(AjaxRequestTarget target);

    protected abstract ActivityDefinitionType createActivityDefinition() throws SchemaException;

    protected abstract String getTaskName();

    @Override
    protected void onComponentTag(ComponentTag tag) {
        tag.setName("button");
        super.onComponentTag(tag);
    }
}
