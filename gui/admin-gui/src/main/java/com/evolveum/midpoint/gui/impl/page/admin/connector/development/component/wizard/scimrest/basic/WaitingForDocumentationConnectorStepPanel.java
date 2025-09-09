/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartGeneratingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.dto.SmartGeneratingDto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDiscoverDocumentationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-connector-waiting-documentation")
@PanelInstance(identifier = "cdw-connector-identification",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorIdentification", icon = "fa fa-wrench"),
        containerPath = "empty")
public class WaitingForDocumentationConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String CLASS_DOT = WaitingForDocumentationConnectorStepPanel.class.getName() + ".";
    private static final String OP_DEFINE_TYPES = CLASS_DOT + "defineTypes";
    private static final String OP_DETERMINE_STATUS = CLASS_DOT + "determineStatus";

    private static final String ID_PANEL = "panel";

    private LoadableModel<SmartGeneratingDto> statusModel;

    public WaitingForDocumentationConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        createValuesModel();
        initLayout();
    }

    private void createValuesModel() {
        statusModel = new LoadableModel<>() {
            @Override
            protected SmartGeneratingDto load() {
                Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUS);
                OperationResult result = task.getResult();

                String token = getDetailsModel().getConnectorDevelopmentOperation().submitDiscoverDocumentation(task, result);

                LoadableModel<StatusInfo<?>> statusInfoModel = new LoadableModel<>() {
                    @Override
                    protected StatusInfo<?> load() {
                        try {
                            return getDetailsModel().getServiceLocator().getConnectorService().getDiscoverDocumentationStatus(token, task, result);
                        } catch (SchemaException|ObjectNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };

                StatusInfo<?> latest = statusInfoModel.getObject();
                if (latest == null) {
                    return new SmartGeneratingDto();
                }

                PrismObject<TaskType> taskTypePrismObject = WebModelServiceUtils.loadObject(TaskType.class, token, getPageBase(), task, result);
                return new SmartGeneratingDto(statusInfoModel, () -> taskTypePrismObject);
            }
        };
    }

    private void initLayout() {
        getTextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        getSubtextLabel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        SmartGeneratingPanel waitingPanel = new SmartGeneratingPanel(ID_PANEL, statusModel, true) {
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
        };
        waitingPanel.setOutputMarkupId(true);
        add(waitingPanel);
    }

    @Override
    public String appendCssToWizard() {
        return "col-10";
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
                return false;
            }
            return !statusModel.getObject().isFinished();
        };
    }
}
