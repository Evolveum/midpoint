/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemNameUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

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
    private static final ItemName PROPERTY_ITEM_NAME = ItemName.from("", "baseAddress");

    public BaseUrlConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        try {
            PrismReferenceWrapper<Referencable> resource = getDetailsModel().getObjectWrapper().findReference(
                    ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));

            ObjectDetailsModels<ResourceType> objectDetailsModel =
                    resource.getValue().getNewObjectModel(getContainerConfiguration(PANEL_TYPE), getPageBase(), new OperationResult("getResourceModel"));

            ItemPath path = ItemPath.create("connectorConfiguration", SchemaConstants.ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME);
            return PrismContainerWrapperModel.fromContainerWrapper(objectDetailsModel.getObjectWrapperModel(), path);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        try {
            PrismPropertyValueWrapper<Object> suggestedValue = getDetailsModel().getObjectWrapper().findProperty(
                    ItemPath.create(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_BASE_API_ENDPOINT)).getValue();
            if (StringUtils.isNotEmpty((String) suggestedValue.getRealValue())) {
                PrismPropertyValueWrapper<String> configurationValue = (PrismPropertyValueWrapper<String>) getContainerFormModel().getObject().findProperty(PROPERTY_ITEM_NAME).getValue();
                if (StringUtils.isEmpty(configurationValue.getRealValue())) {
                    configurationValue.setRealValue((String) suggestedValue.getRealValue());
                }
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
        if (QNameUtil.match(wrapper.getItemName(), PROPERTY_ITEM_NAME)) {
            return true;
        }
        return wrapper.isMandatory();
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (QNameUtil.match(wrapper.getItemName(), PROPERTY_ITEM_NAME)) {
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
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
}
