/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.currentState.ActionsExecutedInformationPanel;
import com.evolveum.midpoint.web.page.admin.server.currentState.IterativeInformationPanel;
import com.evolveum.midpoint.web.page.admin.server.currentState.SynchronizationInformationPanel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskCurrentStateDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskCurrentStateDtoModel;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActionsExecutedInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author semancik
 */
public class TaskProgressTabPanel extends AbstractObjectTabPanel<TaskType> implements TaskTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_ITERATIVE_INFORMATION_PANEL = "iterativeInformationPanel";
    private static final String ID_SYNCHRONIZATION_INFORMATION_PANEL_BEFORE = "synchronizationInformationPanelBefore";
    private static final String ID_SYNCHRONIZATION_INFORMATION_PANEL_AFTER = "synchronizationInformationPanelAfter";
    private static final String ID_ACTIONS_EXECUTED_INFORMATION_PANEL = "actionsExecutedInformationPanel";

    private static final Trace LOGGER = TraceManager.getTrace(TaskProgressTabPanel.class);

    private IterativeInformationPanel iterativeInformationPanel;
    private SynchronizationInformationPanel synchronizationInformationPanelBefore;
    private SynchronizationInformationPanel synchronizationInformationPanelAfter;
    private ActionsExecutedInformationPanel actionsExecutedInformationPanel;

    private IModel<TaskDto> taskDtoModel = null;

    public TaskProgressTabPanel(String id, Form mainForm,
            LoadableModel<PrismObjectWrapper<TaskType>> taskWrapperModel,
            IModel<TaskDto> taskDtoModel) {
        super(id, mainForm, taskWrapperModel);
        this.taskDtoModel = taskDtoModel;
        setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        final TaskCurrentStateDtoModel model = new TaskCurrentStateDtoModel(taskDtoModel);
        iterativeInformationPanel = new IterativeInformationPanel(ID_ITERATIVE_INFORMATION_PANEL, model, getPageBase());
        iterativeInformationPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return model.getObject().getIterativeTaskInformationType() != null;
            }
        });
        iterativeInformationPanel.setOutputMarkupId(true);
        add(iterativeInformationPanel);

        synchronizationInformationPanelBefore = new SynchronizationInformationPanel(ID_SYNCHRONIZATION_INFORMATION_PANEL_BEFORE,
            new PropertyModel<>(model, TaskCurrentStateDto.F_SYNCHRONIZATION_INFORMATION_DTO), false);
        synchronizationInformationPanelBefore.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !allCountsIsZero(model.getObject().getSynchronizationInformationType());
            }
        });
        synchronizationInformationPanelBefore.setOutputMarkupId(true);
        add(synchronizationInformationPanelBefore);

        synchronizationInformationPanelAfter = new SynchronizationInformationPanel(ID_SYNCHRONIZATION_INFORMATION_PANEL_AFTER,
            new PropertyModel<>(model, TaskCurrentStateDto.F_SYNCHRONIZATION_INFORMATION_AFTER_DTO), true);
        synchronizationInformationPanelAfter.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !allCountsIsZero(model.getObject().getSynchronizationInformationType())
                        && !taskDtoModel.getObject().isDryRun();
            }
        });
        synchronizationInformationPanelAfter.setOutputMarkupId(true);
        add(synchronizationInformationPanelAfter);

        actionsExecutedInformationPanel = new ActionsExecutedInformationPanel(ID_ACTIONS_EXECUTED_INFORMATION_PANEL,
            new PropertyModel<>(model, TaskCurrentStateDto.F_ACTIONS_EXECUTED_INFORMATION_DTO));
        actionsExecutedInformationPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !isEmpty(model.getObject().getActionsExecutedInformationType());
            }
        });
        actionsExecutedInformationPanel.setOutputMarkupId(true);
        add(actionsExecutedInformationPanel);

    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        List<Component> rv = new ArrayList<>();
        rv.add(iterativeInformationPanel);
        rv.add(synchronizationInformationPanelBefore);
        rv.add(synchronizationInformationPanelAfter);
        rv.addAll(actionsExecutedInformationPanel.getComponentsToUpdate());
        return rv;
    }

    private boolean isEmpty(ActionsExecutedInformationType actionsExecutedInformation) {
        return actionsExecutedInformation == null || (
                actionsExecutedInformation.getObjectActionsEntry().isEmpty()
                && actionsExecutedInformation.getResultingObjectActionsEntry().isEmpty());
    }

    private boolean allCountsIsZero(SynchronizationInformationType synchronizationInformation) {
        return synchronizationInformation == null || (
                synchronizationInformation.getCountDeleted() == 0
                && synchronizationInformation.getCountDeletedAfter() == 0
                && synchronizationInformation.getCountDisputed() == 0
                && synchronizationInformation.getCountDisputedAfter() == 0
                && synchronizationInformation.getCountLinked() == 0
                && synchronizationInformation.getCountLinkedAfter() == 0
                && synchronizationInformation.getCountNoSynchronizationPolicy() == 0
                && synchronizationInformation.getCountNoSynchronizationPolicyAfter() == 0
                && synchronizationInformation.getCountNotApplicableForTask() == 0
                && synchronizationInformation.getCountNotApplicableForTaskAfter() == 0
                && synchronizationInformation.getCountProtected() == 0
                && synchronizationInformation.getCountProtectedAfter() == 0
                && synchronizationInformation.getCountSynchronizationDisabled() == 0
                && synchronizationInformation.getCountSynchronizationDisabledAfter() == 0
                && synchronizationInformation.getCountUnlinked() == 0
                && synchronizationInformation.getCountUnlinkedAfter() == 0
                && synchronizationInformation.getCountUnmatched() == 0
                && synchronizationInformation.getCountUnmatchedAfter() == 0);
    }

}
