/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-base-url")
@PanelInstance(identifier = "cdw-base-url",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.baseUrl", icon = "fa fa-wrench"),
        containerPath = "empty")
public class BaseUrlConnectorStepPanel extends AbstractFormWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String PANEL_TYPE = "cdw-base-url";
    public static final ItemName BASE_ADDRESS_ITEM_NAME = ItemName.from("", "baseAddress");
    public static final ItemName SCIM_BASE_URL_ITEM_NAME = ItemName.from("", "scimBaseUrl");

    private static final ItemName DEVELOPMENT_MODE_ITEM_NAME = ItemName.from("", "developmentMode");
    private static final ItemPath CONNECTOR_CONFIGURATION = ItemPath.create("connectorConfiguration", SchemaConstants.ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME);

    private static final String ID_AI_ALERT = "aiAlert";

    private PrismContainerWrapper<? extends Containerable> containerWrapper;

    public BaseUrlConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        try {
            ObjectDetailsModels<ResourceType> objectDetailsModel =
                    ConnectorDevelopmentWizardUtil.getTestingResourceModel(getDetailsModel(), getPanelType());
            ItemPath path = ItemPath.create("connectorConfiguration", SchemaConstants.ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME);

            try {
                // Enable development mode (slower  requests, more verbose login, native schema as resources)
                objectDetailsModel.getObjectWrapper().findProperty(CONNECTOR_CONFIGURATION.append(DEVELOPMENT_MODE_ITEM_NAME)).getValue().setRealValue(true);

                // Mark resource as down
                PrismPropertyWrapper<Object> stateProperty = objectDetailsModel.getObjectWrapper().findProperty(ItemPath.create(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS));
                stateProperty.getValue().setRealValue(AvailabilityStatusType.DOWN);
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }

            return PrismContainerWrapperModel.fromContainerWrapper(objectDetailsModel.getObjectWrapperModel(), path);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        try {
            String suggestedUrl = (String) getDetailsModel().getObjectWrapper().findProperty(
                    ItemPath.create(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_BASE_API_ENDPOINT))
                    .getValue().getRealValue();
            if (StringUtils.isNotEmpty(suggestedUrl)) {
                ItemName urlField = isScim() ? SCIM_BASE_URL_ITEM_NAME : BASE_ADDRESS_ITEM_NAME;
                PrismPropertyValueWrapper<String> fieldValue = (PrismPropertyValueWrapper<String>) getContainerFormModel().getObject().findProperty(urlField).getValue();
                if (StringUtils.isEmpty(fieldValue.getRealValue())) {
                    fieldValue.setRealValue(suggestedUrl);
                }
            }
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onBeforeRender() {
        PrismContainerWrapper<?> wrapper = getContainerFormModel().getObject();

        if (containerWrapper != null && containerWrapper != wrapper) {
            initLayout();
        }
        containerWrapper = wrapper;

        super.onBeforeRender();
        ((VerticalFormPrismContainerPanel) getVerticalForm().getSingleContainerPanel().getContainer().get("1"))
                .getContainer().add(AttributeAppender.remove("class"));
    }

    @Override
    protected void initLayout() {
//        getTopLevelContainer().add(AttributeAppender.replace("class", "d-flex flex-column col-9 mt-2"));
        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        WebMarkupContainer aiAlert = new WebMarkupContainer(ID_AI_ALERT);
        aiAlert.setOutputMarkupId(true);
        addOrReplace(aiAlert);
        aiAlert.add(new VisibleBehaviour(this::isAiAlertVisible));

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(getVisibilityHandler())
                .mandatoryHandler(this::checkMandatory)
                .build();
        VerticalFormPanel panel = new VerticalFormPanel(ID_FORM, getContainerFormModel(), settings, getContainerConfiguration()) {
            @Override
            protected String getIcon() {
                return BaseUrlConnectorStepPanel.this.getIcon();
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
        addOrReplace(panel);
    }

    private boolean isAiAlertVisible() {
        return getDetailsModel().getObjectType() != null && getDetailsModel().getObjectType().getApplication() != null
                && StringUtils.isNotEmpty(getDetailsModel().getObjectType().getApplication().getBaseApiEndpoint());
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
        return createStringResource("PageConnectorDevelopment.wizard.step.baseUrl");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.baseUrl.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.baseUrl.subText");
    }

    protected boolean checkMandatory(ItemWrapper wrapper) {
        if (isScim()) {
            if (QNameUtil.match(wrapper.getItemName(), SCIM_BASE_URL_ITEM_NAME)) {
                return true;
            }
        } else {
            if (QNameUtil.match(wrapper.getItemName(), BASE_ADDRESS_ITEM_NAME)) {
                return true;
            }
        }
        return wrapper.isMandatory();
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (isScim()) {
                if (scimItemNames().stream().anyMatch(name -> QNameUtil.match(wrapper.getItemName(), name))) {
                    return ItemVisibility.AUTO;
                }
            } else {
                if (QNameUtil.match(wrapper.getItemName(), BASE_ADDRESS_ITEM_NAME)) {
                    return ItemVisibility.AUTO;
                }
            }
            return ItemVisibility.HIDDEN;
        };
    }

    private List<ItemName> scimItemNames() {
        return List.of(SCIM_BASE_URL_ITEM_NAME);
    }

    private boolean isScim() {
        try {
            PrismPropertyWrapper<ConnDevIntegrationType> integrationType = getDetailsModel().getObjectWrapper().findProperty(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_INTEGRATION_TYPE));
            return ConnDevIntegrationType.SCIM.equals(integrationType.getValue().getRealValue());
        } catch (SchemaException e) {
            return false;
        }
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
    protected IModel<String> getNextLabelModel() {
        return null;
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
    public boolean isCompleted() {
        ItemName fieldToCheck = isScim() ? SCIM_BASE_URL_ITEM_NAME : BASE_ADDRESS_ITEM_NAME;
        return ConnectorDevelopmentWizardUtil.existTestingResourcePropertyValue(
                getDetailsModel(), getPanelType(), fieldToCheck);
    }
    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
