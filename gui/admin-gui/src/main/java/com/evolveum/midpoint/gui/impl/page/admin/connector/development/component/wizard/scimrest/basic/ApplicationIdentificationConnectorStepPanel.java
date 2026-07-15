/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevApplicationInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-app-identification")
@PanelInstance(identifier = "cdw-app-identification",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.appIdentification", icon = "fa fa-wrench"),
        containerPath = "empty")
public class ApplicationIdentificationConnectorStepPanel extends AbstractFormWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String PANEL_TYPE = "cdw-app-identification";

    public ApplicationIdentificationConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(), ConnectorDevelopmentType.F_APPLICATION);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        ((VerticalFormPrismContainerPanel)getVerticalForm().getSingleContainerPanel().getContainer().get("1"))
                .getContainer().add(AttributeAppender.remove("class"));
    }

    @Override
    protected void initLayout() {
//        getTopLevelContainer().add(AttributeAppender.replace("class", "d-flex flex-column col-9 mt-2"));
        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(getVisibilityHandler())
                .mandatoryHandler(this::checkMandatory)
                .build();
        VerticalFormPanel panel = new VerticalFormPanel(ID_FORM, getContainerFormModel(), settings, getContainerConfiguration()) {
            @Override
            protected String getIcon() {
                return ApplicationIdentificationConnectorStepPanel.this.getIcon();
            }

            @Override
            protected IModel<?> getTitleModel() {
                return getFormTitle();
            }

            @Override
            protected WrapperContext createWrapperContext() {
                return getDetailsModel().createWrapperContext();
            }

            @Override
            protected boolean isShowEmptyButtonVisible() {
                return false;
            }

            @Override
            protected boolean isHeaderVisible(IModel model) {
                return false;
            }

            @Override
            protected String getCssClassForFormContainerOfValuePanel() {
                return "";
            }
        };
        panel.setOutputMarkupId(true);
        panel.add(AttributeAppender.replace("class", "col-12"));
        add(panel);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.appIdentification");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.appIdentification.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.appIdentification.subText");
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        if (itemWrapper.getItemName().equals(ConnDevApplicationInfoType.F_APPLICATION_NAME)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(ConnDevApplicationInfoType.F_BASE_API_ENDPOINT)){
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public String appendCssToWizard() {
        return "col-12 col-xl-10 col-xxl-8";
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        boolean applicationInfoChanged = isApplicationInfoChanged();
        try {
            PrismPropertyValueWrapper<Object> value = PrismPropertyWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(), ConnectorDevelopmentType.F_NAME)
                    .getObject().getValue();
            if (value.getRealValue() == null
                    || getDetailsModel().getObjectWrapper().getStatus() == ItemStatus.ADDED) {
                value.setRealValue(
                        PrismPropertyWrapperModel.fromContainerWrapper((IModel<PrismContainerWrapper<ConnDevApplicationInfoType>>) getContainerFormModel(), ConnDevApplicationInfoType.F_APPLICATION_NAME)
                                .getObject().getValue().getRealValue()
                );
            }
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();
        if (result != null && !result.isError()) {
            if (applicationInfoChanged) {
                restartDocumentationDiscovery();
            }
            super.onNextPerformed(target);
        } else {
            target.add(getFeedback());
        }
        return false;
    }

    /**
     * Returns true if the user changed the application information used by the documentation
     * discovery (name, version, integration type), so the already discovered documentation
     * may belong to a different application. Changes of other items (e.g. description)
     * don't influence the discovery.
     */
    private boolean isApplicationInfoChanged() {
        if (getDetailsModel().getObjectWrapper().getStatus() == ItemStatus.ADDED) {
            // Initial creation, there is no previous discovery to restart.
            return false;
        }
        List<ItemName> discoveryRelevantItems = List.of(
                ConnDevApplicationInfoType.F_APPLICATION_NAME,
                ConnDevApplicationInfoType.F_VERSION,
                ConnDevApplicationInfoType.F_INTEGRATION_TYPE);
        try {
            PrismContainerWrapper<ConnDevApplicationInfoType> container =
                    ((IModel<PrismContainerWrapper<ConnDevApplicationInfoType>>) getContainerFormModel()).getObject();
            var deltas = container.getDelta();
            return deltas != null && deltas.stream().anyMatch(
                    delta -> discoveryRelevantItems.stream().anyMatch(
                            item -> QNameUtil.match(item, delta.getElementName())));
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Resets the documentation waiting step, so the documentation discovery is executed
     * again instead of reusing the result of the previous discovery task.
     */
    private void restartDocumentationDiscovery() {
        if (getWizard() instanceof WizardModelWithParentSteps parentWizardModel) {
            for (WizardStep step : parentWizardModel.getActiveChildrenSteps()) {
                if (step instanceof WaitingForDocumentationConnectorStepPanel waitingPanel) {
                    waitingPanel.restartTask();
                    return;
                }
            }
        }
    }

    @Override
    public boolean isCompleted() {
        PrismPropertyValueWrapper<Object> value;
        try {
            value = PrismPropertyWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(), ConnectorDevelopmentType.F_NAME)
                    .getObject().getValue();
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        return value != null && value.getRealValue() != null;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}
