/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.conndev.SupportedAuthorization;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-testing-credentials")
@PanelInstance(identifier = "cdw-testing-credentials",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.testingCredentials", icon = "fa fa-wrench"),
        containerPath = "empty")
public class CredentialsConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String PANEL_TYPE = "cdw-testing-credentials";

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_PANEL = "panel";
    private static final String ID_RADIO = "radio";
    private static final String ID_NAME = "name";
    private static final String ID_FORM = "form";

    private LoadableModel<List<PrismContainerValueWrapper<ConnDevAuthInfoType>>> valuesModel;

    public CredentialsConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
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
                try {
                    PrismContainerWrapper<ConnDevAuthInfoType> container = getDetailsModel().getObjectWrapper().findContainer(
                            ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_AUTH));

                    List<PrismContainerValueWrapper<ConnDevAuthInfoType>> values = container.getValues();
                    if (values.size() == 1) {
                        values.get(0).setSelected(true);
                    }
                    return values;
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        IModel<String> radioGroupModel = new IModel<>() {
            @Override
            public String getObject() {
                Optional<PrismContainerValueWrapper<ConnDevAuthInfoType>> selected = valuesModel.getObject().stream()
                        .filter(PrismContainerValueWrapper::isSelected)
                        .findFirst();

                return selected.map(connDevAuthInfoTypePrismContainerValueWrapper -> connDevAuthInfoTypePrismContainerValueWrapper.getRealValue().getName())
                        .orElse(null);
            }

            @Override
            public void setObject(String object) {
                valuesModel.getObject().forEach(value -> value.setSelected(false));
                valuesModel.getObject().stream()
                        .filter(value -> StringUtils.equals(value.getRealValue().getName(), object))
                        .findFirst()
                        .ifPresent(value -> value.setSelected(true));
            }
        };
        RadioGroup<String> radioGroup = new RadioGroup<>(ID_RADIO_GROUP, radioGroupModel);
        radioGroup.setOutputMarkupId(true);
        add(radioGroup);

        ListView<PrismContainerValueWrapper<ConnDevAuthInfoType>> panel = new ListView<>(ID_PANEL, valuesModel) {
            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ConnDevAuthInfoType>> listItem) {
                if (listItem.getIndex() == valuesModel.getObject().size() - 1) {
                    listItem.add(AttributeAppender.append("class", "card-body py-2"));
                } else {
                    listItem.add(AttributeAppender.append("class", "card-header py-2"));
                }

                Radio<String> radio = new Radio<>(ID_RADIO, Model.of(listItem.getModelObject().getRealValue().getName()), radioGroup);
                radio.setOutputMarkupId(true);
                listItem.add(radio);

                Label name = new Label(ID_NAME, () -> listItem.getModelObject().getRealValue().getName());
                name.setOutputMarkupId(true);
                listItem.add(name);

                ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                        .visibilityHandler(getVisibilityHandler(listItem.getModelObject().getRealValue()))
                        .mandatoryHandler(getMandatoryHandler(listItem.getModelObject().getRealValue()))
                        .build();
                VerticalFormPanel formPanel = new VerticalFormPanel(ID_FORM, getContainerFormModel(), settings, getContainerConfiguration(getPanelType())) {

                    @Override
                    protected void onBeforeRender() {
                        super.onBeforeRender();
                        ((VerticalFormPrismContainerPanel)getSingleContainerPanel().getContainer().get("1"))
                                .getContainer().add(AttributeAppender.remove("class"));
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
                formPanel.setOutputMarkupId(true);
                formPanel.add(AttributeAppender.replace("class", "col-12"));
                formPanel.add(new VisibleBehaviour(() -> listItem.getModelObject().isSelected()));
                listItem.add(formPanel);

            }
        };
        panel.setOutputMarkupId(true);
        radioGroup.add(panel);

        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(get(ID_RADIO_GROUP));
            }
        });
    }

    private ItemMandatoryHandler getMandatoryHandler(ConnDevAuthInfoType authType) {
        List<ItemName> visibleItems = new ArrayList<>();
        try {
            PrismPropertyWrapper<ConnDevIntegrationType> integration =
                    getDetailsModel().getObjectWrapper().findProperty(
                            ItemPath.create(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_INTEGRATION_TYPE));
            visibleItems.addAll(SupportedAuthorization.attributesFor(integration.getValue().getRealValue(), authType.getType()));
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        return wrapper -> visibleItems.stream()
                .anyMatch(visibleItem -> StringUtils.equals(wrapper.getItemName().getLocalPart(), visibleItem.getLocalPart()));
    }

    private ItemVisibilityHandler getVisibilityHandler(ConnDevAuthInfoType authType) {
        List<ItemName> visibleItems = new ArrayList<>();
        try {
            PrismPropertyWrapper<ConnDevIntegrationType> integration =
                    getDetailsModel().getObjectWrapper().findProperty(
                            ItemPath.create(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_INTEGRATION_TYPE));
            visibleItems.addAll(SupportedAuthorization.attributesFor(integration.getValue().getRealValue(), authType.getType()));
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        return wrapper -> {
            if (visibleItems.stream().anyMatch(visibleItem -> StringUtils.equals(wrapper.getItemName().getLocalPart(), visibleItem.getLocalPart()))) {
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
        };

//        if (StringUtils.equals("Basic Authorization", authType)) {
//            return wrapper -> {
//                if (wrapper.getItemName().equals(ConnDevApplicationInfoType.F_APPLICATION_NAME)) {
//                return ItemVisibility.AUTO;
//            }
//                return ItemVisibility.HIDDEN;
//            };
//        }
//        if (StringUtils.equals("API Key Authorization", authType)) {
//            return wrapper -> {
//                if (wrapper.getItemName().equals(ConnDevApplicationInfoType.F_DESCRIPTION)) {
//                    return ItemVisibility.AUTO;
//                }
//                return ItemVisibility.HIDDEN;
//            };
//        }

//        if (StringUtils.equals("OAuth 2.0 Client Credentials", authType)) {
//            return wrapper -> {
//                if (wrapper.getItemName().equals(ConnDevApplicationInfoType.F_DEPLOYMENT_TYPE)) {
//                    return ItemVisibility.AUTO;
//                }
//                return ItemVisibility.HIDDEN;
//            };
//        }
//
//        if (StringUtils.equals("API Key in Header or Query", authType)) {
//            return wrapper -> {
//                if (wrapper.getItemName().equals(ConnDevApplicationInfoType.F_INTEGRATION_TYPE)) {
//                    return ItemVisibility.AUTO;
//                }
//                return ItemVisibility.HIDDEN;
//            };
//        }
//        return null;
    }

    private IModel<? extends PrismContainerWrapper> getContainerFormModel() {
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

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.testingCredentials");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.testingCredentials.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.testingCredentials.subText");
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
