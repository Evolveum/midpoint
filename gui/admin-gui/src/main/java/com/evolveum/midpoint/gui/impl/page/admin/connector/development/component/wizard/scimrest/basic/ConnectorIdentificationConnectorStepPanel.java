/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevApplicationInfoType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-connector-identification")
@PanelInstance(identifier = "cdw-connector-identification",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.connectorIdentification", icon = "fa fa-wrench"),
        containerPath = "empty")
public class ConnectorIdentificationConnectorStepPanel extends AbstractFormWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String PANEL_TYPE = "cdw-connector-identification";

    public ConnectorIdentificationConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getDetailsModel().getObjectWrapperModel(), ConnectorDevelopmentType.F_CONNECTOR);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initPredefinedAttributes();
    }

    private void initPredefinedAttributes() {
        try {
            PrismContainerWrapper<ConnDevApplicationInfoType> container = getDetailsModel().getObjectWrapper().findContainer(ConnectorDevelopmentType.F_APPLICATION);
            ConnDevApplicationInfoType containerBean = container.getValue().getRealValue();

            PrismPropertyWrapper<String> groupIdProperty =
                    getContainerFormModel().getObject().findProperty(ConnDevConnectorType.F_GROUP_ID);
            if (StringUtils.isEmpty(groupIdProperty.getValue().getRealValue())) {
                groupIdProperty.getValue().setRealValue(StringUtils.normalizeSpace(containerBean.getApplicationName().getNorm()));
            }

            PrismPropertyWrapper<String> artifactIdProperty =
                    getContainerFormModel().getObject().findProperty(ConnDevConnectorType.F_ARTIFACT_ID);
            if (StringUtils.isEmpty(artifactIdProperty.getValue().getRealValue())) {
                artifactIdProperty.getValue().setRealValue("com.evolveum." +
                        StringUtils.normalizeSpace(containerBean.getApplicationName().getNorm()));
            }

            PrismPropertyWrapper<String> versionProperty =
                    getContainerFormModel().getObject().findProperty(ConnDevConnectorType.F_VERSION);
            if (StringUtils.isEmpty(versionProperty.getValue().getRealValue())) {
                versionProperty.getValue().setRealValue("1.0");
            }
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        ((VerticalFormPrismContainerPanel) getVerticalForm().getSingleContainerPanel().getContainer().get("1"))
                .getContainer().add(AttributeAppender.remove("class"));
    }

    @Override
    protected void initLayout() {
//        getTopLevelContainer().add(AttributeAppender.replace("class", "d-flex flex-column col-9 mt-2"));
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(getVisibilityHandler())
                .mandatoryHandler(this::checkMandatory)
                .build();
        VerticalFormPanel panel = new VerticalFormPanel(ID_FORM, getContainerFormModel(), settings, getContainerConfiguration()) {
            @Override
            protected String getIcon() {
                return ConnectorIdentificationConnectorStepPanel.this.getIcon();
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
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorIdentification");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorIdentification.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.connectorIdentification.subText");
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        if (itemWrapper.getItemName().equals(ConnDevConnectorType.F_ARTIFACT_ID)
                || itemWrapper.getItemName().equals(ConnDevConnectorType.F_GROUP_ID)
                || itemWrapper.getItemName().equals(ConnDevConnectorType.F_VERSION)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(ConnDevConnectorType.F_CONNECTOR_REF)
                    || wrapper.getItemName().equals(ConnDevConnectorType.F_DIRECTORY)
                    || wrapper.getItemName().equals(ConnDevConnectorType.F_INTEGRATION_TYPE)){
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
        return "col-10";
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
        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();
        if (result != null && !result.isError()) {
            super.onNextPerformed(target);
        } else {
            target.add(getFeedback());
        }
        return false;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }
}
