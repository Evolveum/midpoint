/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.input.CheckPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-supported-auth")
@PanelInstance(identifier = "cdw-supported-auth",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.supportedAuthMethod", icon = "fa fa-wrench"),
        containerPath = "empty")
public class SupportedAuthMethodConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String PANEL_TYPE = "cdw-supported-auth";

    private static final String ID_PANEL = "panel";
    private static final String ID_CHECK = "check";
    private static final String ID_NAME = "name";
    private static final String ID_TYPE = "type";
    private static final String ID_DESCRIPTION = "description";

    private LoadableModel<List<PrismContainerValueWrapper<ConnDevAuthInfoType>>> valuesModel;

    public SupportedAuthMethodConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        createValuesModel();
        initLayout();
    }

    private void createValuesModel() {
        valuesModel = new LoadableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ConnDevAuthInfoType>> load() {
                PrismContainerWrapper<ConnDevAuthInfoType> container;
                try {
                    container = getDetailsModel().getObjectWrapper().findContainer(
                            ItemPath.create(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_AUTH));
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }

                List<PrismContainerValueWrapper<ConnDevAuthInfoType>> list = new ArrayList<>();
                try {
                    ConnDevAuthInfoType value = new ConnDevAuthInfoType().name("Bearer Token").type(ConnDevHttpAuthTypeType.BEARER).description("Used in APIs that require a simple token in the Authorization header");
                    list.add(WebPrismUtil.createNewValueWrapper(container, value.asPrismContainerValue(), getPageBase()));

                    value = new ConnDevAuthInfoType().name("Basic Auth").type(ConnDevHttpAuthTypeType.BASIC).description("Username and password encoded in the request header");
                    list.add(WebPrismUtil.createNewValueWrapper(container, value.asPrismContainerValue(), getPageBase()));

                    value = new ConnDevAuthInfoType().name("OAuth 2.0 Client Credentials").type(null).description("Token-based authentication using client ID and secret");
                    list.add(WebPrismUtil.createNewValueWrapper(container, value.asPrismContainerValue(), getPageBase()));

                    value = new ConnDevAuthInfoType().name("API Key in Header or Query").type(ConnDevHttpAuthTypeType.API_KEY).description("Single static key passed as a header or URL parameter");
                    list.add(WebPrismUtil.createNewValueWrapper(container, value.asPrismContainerValue(), getPageBase()));
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }

                return list;
            }
        };
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        ListView<PrismContainerValueWrapper<ConnDevAuthInfoType>> panel = new ListView<>(ID_PANEL, valuesModel) {
            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ConnDevAuthInfoType>> listItem) {
                if (listItem.getIndex() == valuesModel.getObject().size() - 1) {
                    listItem.add(AttributeAppender.append("class", "card-body py-2"));
                } else {
                    listItem.add(AttributeAppender.append("class", "card-header py-2"));
                }

                CheckPanel check = new CheckPanel(ID_CHECK, new PropertyModel<>(listItem.getModel(), "selected"));
                check.setOutputMarkupId(true);
                listItem.add(check);

                Label name = new Label(ID_NAME, () -> listItem.getModelObject().getRealValue().getName());
                name.setOutputMarkupId(true);
                listItem.add(name);

                Label type = new Label(ID_TYPE, () -> listItem.getModelObject().getRealValue().getType());
                type.setOutputMarkupId(true);
                listItem.add(type);

                Label description = new Label(ID_DESCRIPTION, () -> listItem.getModelObject().getRealValue().getDescription());
                description.setOutputMarkupId(true);
                listItem.add(description);
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.supportedAuthMethod");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.supportedAuthMethod.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.supportedAuthMethod.subText");
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        if (itemWrapper.getItemName().equals(ConnDevApplicationInfoType.F_APPLICATION_NAME)) {
            return true;
        }
        return itemWrapper.isMandatory();
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
        getPageBase().getPageParameters().remove(WizardModel.PARAM_STEP);
        if (getWizard().hasNext()) {
            try {
                PrismContainerWrapper<ConnDevAuthInfoType> container =
                        getDetailsModel().getObjectWrapper().findContainer(
                                ItemPath.create(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_AUTH));

                valuesModel.getObject().forEach(value -> {
                    try {
                        if (value.isSelected()) {
                            container.getItem().add(value.getRealValue().asPrismContainerValue());
                            container.getValues().add(value);
                        }
                        value.setSelected(false);
                    } catch (SchemaException e) {
                        throw new RuntimeException(e);
                    }
                });

            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }
        }
        return super.onNextPerformed(target);
    }
}
