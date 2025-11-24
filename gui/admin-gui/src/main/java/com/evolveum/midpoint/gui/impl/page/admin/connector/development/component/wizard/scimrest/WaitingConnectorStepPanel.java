/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.MidPointApplication;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * @author lskublik
 */
public abstract class WaitingConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(WaitingConnectorStepPanel.class);

    private static final String CLASS_DOT = WaitingConnectorStepPanel.class.getName() + ".";
    private static final String OP_DETERMINE_STATUS = CLASS_DOT + "determineStatus";
    private static final String OP_LOAD_CONNECTOR = CLASS_DOT + "loadConnector";

    private static final String ID_PANEL = "panel";

    private LoadableModel<SmartGeneratingDto> statusModel;
    private LoadableModel<String> tokenModel;

    public WaitingConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    public void init(WizardModel wizard) {
        super.init(wizard);

        tokenModel = new LoadableModel<>() {
            @Override
            protected String load() {
                try {
                    var objectClass = getObjectClassName();
                    if (objectClassRequired() && objectClass == null) {
                        // Sometimes panels are checked earlier than object class exists.
                        return null;
                    }

                    return ConnectorDevelopmentWizardUtil.getTaskToken(
                            getActivityType(),
                            objectClass,
                            getScripType() != null ? getScripType().scriptIntent : null,
                            getDetailsModel().getObjectWrapper().getOid(),
                            getDetailsModel().getPageAssignmentHolder());
                } catch (CommonException e) {
                    LOGGER.error("Couldn't search tasks for " + getActivityType());
                    return null;
                }
            }
        };
    }

    /**
     * Returns if the panel is object class specific.
     *
     * @return true if panel is object class specific, false if it is otherwise
     */
    protected abstract boolean objectClassRequired();

    protected ConnectorDevelopmentArtifacts.KnownArtifactType getScripType() {
        return null;
    }

    protected String getObjectClassName() {
        return null;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        createStatusModel();
        initLayout();
    }

    private void createStatusModel() {
        statusModel = new LoadableModel<>() {
            @Override
            protected SmartGeneratingDto load() {

                Task task = getDetailsModel().getPageAssignmentHolder().createSimpleTask(OP_DETERMINE_STATUS);
                OperationResult result = task.getResult();

                if (StringUtils.isEmpty(tokenModel.getObject())) {
                    tokenModel.setObject(getNewTaskToken(task, result));
                }
                Optional.ofNullable(getKeyForStoringToken()).ifPresent(key -> getHelper().putVariable(key, tokenModel.getObject()));

                LoadableModel<StatusInfo<?>> statusInfoModel = new LoadableModel<>() {
                    @Override
                    protected StatusInfo<?> load() {
                        try {
                            MidPointApplication app = MidPointApplication.get();
                            Task task = app.createSimpleTask(OP_DETERMINE_STATUS);
                            OperationResult result = task.getResult();
                            return obtainResult(tokenModel.getObject(), task, result);
                        } catch (SchemaException|ObjectNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

                StatusInfo<?> latest = statusInfoModel.getObject();
                if (latest == null) {
                    return new SmartGeneratingDto() {
                        @Override
                        protected boolean rejectEmptyProgress() {
                            return false;
                        }
                    };
                }

                PrismObject<TaskType> taskTypePrismObject = WebModelServiceUtils.loadObject(TaskType.class, tokenModel.getObject(), getDetailsModel().getPageAssignmentHolder(), task, result);
                return new SmartGeneratingDto(statusInfoModel, () -> taskTypePrismObject){
                    @Override
                    protected boolean rejectEmptyProgress() {
                        return false;
                    }
                };
            }
        };
    }

    protected abstract ItemName getActivityType();

    protected String getKeyForStoringToken(){
        return null;
    };

    protected abstract StatusInfo<?> obtainResult(String token, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException;

    protected abstract String getNewTaskToken(Task task, OperationResult result);

    private void initLayout() {
        getTextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        getSubtextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        SmartGeneratingPanel waitingPanel = createWaitingPanel();
        waitingPanel.setOutputMarkupId(true);
        add(waitingPanel);
    }

    protected final @NotNull SmartGeneratingPanel createWaitingPanel() {
        return new SmartGeneratingPanel(ID_PANEL, statusModel, true) {
            @Override
            protected void onFinishActionPerform(AjaxRequestTarget target) {
                onNextPerformed(target);
            }

            @Override
            protected void onDiscardPerform(AjaxRequestTarget target) {
                target.add(getFeedback());
            }

            @Override
            protected boolean allowShowInBackground() {
                return false;
            }

            @Override
            protected boolean allowRerun() {
                return true;
            }

            @Override
            protected boolean allowActionButton() {
                return false;
            }

            @Override
            protected IModel<String> getIconCssModel() {
                return getIconModel();
            }

            protected IModel<String> getTitleModel() {
                return WaitingConnectorStepPanel.this.getTextModel();
            }

            protected IModel<String> getSubTitleModel() {
                return WaitingConnectorStepPanel.this.getSubTextModel();
            }

            @Override
            protected void onInitialize() {
                super.onInitialize();
                getSubTextLabelPanel().add(
                        AttributeAppender.replace("class", "text-center text-secondary mb-2 col-6 lh-2"));
            }
        };
    }

    protected @NotNull Model<String> getIconModel() {
        return Model.of("fa fa-search");
    }

    @Override
    public String appendCssToWizard() {
        return "col-12";
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> {
            if (statusModel == null || !statusModel.isLoaded()) {
                return !isCompleted();
            }
            return !statusModel.getObject().isFinished();
        };
    }

    protected final LoadableModel<SmartGeneratingDto> getStatusModel() {
        return statusModel;
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        String oid = getDetailsModel().getObjectWrapper().getOid();
        Task task = getPageBase().createSimpleTask(OP_LOAD_CONNECTOR);

        getDetailsModel().reset();
        getDetailsModel().reloadPrismObjectModel(
                WebModelServiceUtils.loadObject(ConnectorDevelopmentType.class, oid, getPageBase(), task, task.getResult()));
        return super.onNextPerformed(target);
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected boolean isSubmitEnable() {
        return false;
    }

    protected final Object getResult(){
        return statusModel.getObject().getStatusInfo().getObject().getResult();
    }

    public SmartGeneratingPanel getPanel() {
        return (SmartGeneratingPanel) get(ID_PANEL);
    }

    @Override
    public boolean isCompleted() {
        String token = tokenModel.getObject();
        if (StringUtils.isEmpty(token)) {
            return false;
        }

        Task operationTask = getDetailsModel().getPageAssignmentHolder().createSimpleTask("create_task_instance");

        @NotNull Task task;
        try {
            task = getDetailsModel().getPageAssignmentHolder().getTaskManager().getTask(
                    token, null, operationTask.getResult());
        } catch (CommonException e) {
            LOGGER.error("Couldn't create Task instance");
            return false;
        }
        return task.getExecutionState() == TaskExecutionStateType.CLOSED && task.getResultStatus() == OperationResultStatusType.SUCCESS;

    }
}
