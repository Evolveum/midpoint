/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.search;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;

import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.ScriptConfirmationPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceUncategorizedPanel;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-show-search-result")
@PanelInstance(identifier = "cdw-show-search-result",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.showSearchResult", icon = "fa fa-wrench"),
        containerPath = "empty")
public class SearchAllObjectsConnectorStepPanel extends ScriptConfirmationPanel {

    private static final String PANEL_TYPE = "cdw-show-search-result";

    private static final String ID_PANEL = "panel";

    public SearchAllObjectsConnectorStepPanel(WizardPanelHelper<? extends Containerable,
            ConnectorDevelopmentDetailsModel> helper, IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel) {
        super(helper, valueModel);
    }

    @Override
    protected List<ConnectorDevelopmentArtifacts.KnownArtifactType> getScriptClassifications() {
        return List.of(ConnectorDevelopmentArtifacts.KnownArtifactType.SEARCH_ALL_DEFINITION);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        ResourceDetailsModel resourceDetailsModel;

        try {
            PrismReferenceWrapper<Referencable> resource = getDetailsModel().getObjectWrapper().findReference(
                    ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));

            ObjectDetailsModels objectDetailsModel = resource.getValue().getNewObjectModel(
                    getContainerConfiguration(PANEL_TYPE), getPageBase(), new OperationResult("getResourceModel"));
            resourceDetailsModel = (ResourceDetailsModel) objectDetailsModel;

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        ResourceUncategorizedPanel table = new ResourceUncategorizedPanel(
                ID_PANEL, resourceDetailsModel, getContainerConfiguration(PANEL_TYPE)) {
            @Override
            protected VisibleEnableBehaviour getTitleVisibleBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected boolean isShadowDetailsEnabled() {
                return false;
            }

            @Override
            protected QName getDefaultObjectClass() {
                return new QName(getValueModel().getObject().getRealValue().getName());
            }

            @Override
            protected boolean isEnabledInlineMenu() {
                return false;
            }

            @Override
            protected Consumer<Task> createProviderSearchTaskCustomizer() {
                return (Consumer<Task> & Serializable) (task) -> task.setExecutionMode(TaskExecutionMode.SIMULATED_SHADOWS_DEVELOPMENT);
            }

            @Override
            protected boolean isTaskButtonVisible() {
                return false;
            }

            @Override
            protected boolean isObjectClassFieldVisible() {
                return false;
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected void processErrorResult(OperationResult errorResult) {
                if (getWizard() instanceof WizardModelWithParentSteps wizardModel) {
                    wizardModel.addOperationResult(getStepId(), "cdw-search-all-script", errorResult);
                }
            }

            @Override
            public Component getFeedbackPanel() {
                return getFeedback();
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }


    @Override
    public String appendCssToWizard() {
        return "col-12";
    }

    @Override
    protected boolean isSubmitVisible() {
        return true;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSearchResult");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSearchResult.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.showSearchResult.subText");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("SearchObjectsConnectorStepPanel.submit");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        onNextPerformed(target);
    }
}
