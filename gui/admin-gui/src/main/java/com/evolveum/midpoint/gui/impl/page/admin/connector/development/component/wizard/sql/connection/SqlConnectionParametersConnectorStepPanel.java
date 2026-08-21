/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.sql.connection;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
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
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * SQL connector "Connection" step: a single form collecting the JDBC connection
 * parameters (jdbcUrl, username, password and the optional advanced pool/timeout
 * settings) directly on the testing resource's connector configuration.
 */
@PanelType(name = "cdw-sql-connection-parameters")
@PanelInstance(identifier = "cdw-sql-connection-parameters",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.sqlConnectionParameters", icon = "fa fa-wrench"),
        containerPath = "empty")
public class SqlConnectionParametersConnectorStepPanel extends AbstractFormWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    public static final String PANEL_TYPE = "cdw-sql-connection-parameters";

    public static final ItemName JDBC_URL = ItemName.from("", "jdbcUrl");
    public static final ItemName USERNAME = ItemName.from("", "username");
    private static final ItemName PASSWORD = ItemName.from("", "password");
    private static final ItemName POOL_SIZE = ItemName.from("", "poolSize");
    private static final ItemName CONNECTION_TIMEOUT = ItemName.from("", "connectionTimeout");
    private static final ItemName IDLE_TIMEOUT = ItemName.from("", "idleTimeout");
    private static final ItemName VALIDATE_CONNECTION_ON_BORROW = ItemName.from("", "validateConnectionOnBorrow");
    private static final ItemName AUTO_DISCOVER_SCHEMA = ItemName.from("", "autoDiscoverSchema");
    private static final ItemName TEST_CONNECTION_QUERY = ItemName.from("", "testConnectionQuery");

    private static final List<ItemName> REQUIRED_FIELDS = List.of(JDBC_URL, USERNAME, PASSWORD);
    private static final List<ItemName> VISIBLE_FIELDS = List.of(
            JDBC_URL, USERNAME, PASSWORD, POOL_SIZE, CONNECTION_TIMEOUT, IDLE_TIMEOUT,
            VALIDATE_CONNECTION_ON_BORROW, AUTO_DISCOVER_SCHEMA, TEST_CONNECTION_QUERY);

    private static final ItemName DEVELOPMENT_MODE_ITEM_NAME = ItemName.from("", "developmentMode");
    private static final ItemPath CONNECTOR_CONFIGURATION_PROPERTIES = ItemPath.create("connectorConfiguration", SchemaConstants.ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME);
    private static final ItemPath PRODUCER_BUFFER_SIZE = ItemPath.create("connectorConfiguration", "producerBufferSize");

    private PrismContainerWrapper<? extends Containerable> containerWrapper;

    public SqlConnectionParametersConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected IModel<? extends PrismContainerWrapper> getContainerFormModel() {
        try {
            ObjectDetailsModels<ResourceType> objectDetailsModel =
                    ConnectorDevelopmentWizardUtil.getTestingResourceModel(getDetailsModel(), getPanelType());

            disableConnIdProducerProxy(objectDetailsModel);
            enableConnectorDevelopmentMode(objectDetailsModel);
            PrismPropertyWrapper<Object> stateProperty = objectDetailsModel.getObjectWrapper().findProperty(
                    ItemPath.create(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS));
            stateProperty.getValue().setRealValue(AvailabilityStatusType.DOWN);

            return PrismContainerWrapperModel.fromContainerWrapper(objectDetailsModel.getObjectWrapperModel(), CONNECTOR_CONFIGURATION_PROPERTIES);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private void enableConnectorDevelopmentMode(ObjectDetailsModels<ResourceType> objectDetailsModel) throws SchemaException {
        objectDetailsModel.getObjectWrapper().findProperty(CONNECTOR_CONFIGURATION_PROPERTIES.append(DEVELOPMENT_MODE_ITEM_NAME)).getValue().setRealValue(true);
    }

    private void disableConnIdProducerProxy(ObjectDetailsModels<ResourceType> objectDetailsModel) throws SchemaException {
        objectDetailsModel.getObjectWrapper().findProperty(PRODUCER_BUFFER_SIZE).getValue().setRealValue(0);
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
                return SqlConnectionParametersConnectorStepPanel.this.getIcon();
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

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    protected String getIcon() {
        return "fa fa-wrench";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.sqlConnectionParameters");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.sqlConnectionParameters.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.sqlConnectionParameters.subText");
    }

    protected boolean checkMandatory(ItemWrapper wrapper) {
        if (REQUIRED_FIELDS.stream().anyMatch(name -> QNameUtil.match(wrapper.getItemName(), name))) {
            return true;
        }
        return wrapper.isMandatory();
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (VISIBLE_FIELDS.stream().anyMatch(name -> QNameUtil.match(wrapper.getItemName(), name))) {
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
        return REQUIRED_FIELDS.stream().allMatch(
                fieldName -> ConnectorDevelopmentWizardUtil.existTestingResourcePropertyValue(
                        getDetailsModel(), getPanelType(), fieldName));
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
