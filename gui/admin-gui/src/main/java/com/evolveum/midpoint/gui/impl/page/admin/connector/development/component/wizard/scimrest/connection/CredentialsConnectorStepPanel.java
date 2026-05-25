/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
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
    private static final String ID_CREDENTIALS_SECTION = "credentialsSection";
    private static final String ID_NO_SELECTION_MESSAGE = "noSelectionMessage";
    private static final String ID_NO_METHODS_MESSAGE = "noMethodsMessage";
    private static final String ID_FORM = "form";

    private LoadableModel<List<PrismContainerValueWrapper<ConnDevAuthInfoType>>> valuesModel;

    public CredentialsConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    public void init(WizardModel wizard) {
        super.init(wizard);
        createValuesModel();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
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
        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        IModel<String> radioGroupModel = new IModel<>() {
            @Override
            public String getObject() {
                Optional<PrismContainerValueWrapper<ConnDevAuthInfoType>> selected = valuesModel.getObject().stream()
                        .filter(PrismContainerValueWrapper::isSelected)
                        .findFirst();
                return selected.map(v -> v.getRealValue().getName()).orElse(null);
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

        IModel<ConnDevAuthInfoType> selectedAuthModel = () -> {
            String name = radioGroupModel.getObject();
            if (name == null) {
                return null;
            }
            return valuesModel.getObject().stream()
                    .filter(v -> StringUtils.equals(v.getRealValue().getName(), name))
                    .map(PrismContainerValueWrapper::getRealValue)
                    .findFirst().orElse(null);
        };

        Label noMethodsMessage = new Label(ID_NO_METHODS_MESSAGE, createStringResource("CredentialsConnectorStepPanel.noMethods"));
        noMethodsMessage.add(new VisibleBehaviour(() -> valuesModel.getObject().isEmpty()));
        add(noMethodsMessage);

        RadioGroup<String> radioGroup = new RadioGroup<>(ID_RADIO_GROUP, radioGroupModel);
        radioGroup.setOutputMarkupId(true);
        add(radioGroup);

        WebMarkupContainer credentialsSection = new WebMarkupContainer(ID_CREDENTIALS_SECTION);
        credentialsSection.setOutputMarkupId(true);
        add(credentialsSection);

        ListView<PrismContainerValueWrapper<ConnDevAuthInfoType>> panel = new ListView<>(ID_PANEL, valuesModel) {
            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ConnDevAuthInfoType>> listItem) {
                listItem.setOutputMarkupId(true);
                listItem.add(AttributeAppender.append("style", "cursor: pointer;"));
                listItem.add(new AjaxEventBehavior("click") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        radioGroupModel.setObject(listItem.getModelObject().getRealValue().getName());
                        target.add(radioGroup);
                        target.add(credentialsSection);
                    }
                });

                Radio<String> radio = new Radio<>(ID_RADIO, Model.of(listItem.getModelObject().getRealValue().getName()), radioGroup);
                radio.setOutputMarkupId(true);
                radio.add(new AjaxEventBehavior("click") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                    }

                    @Override
                    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                        super.updateAjaxAttributes(attributes);
                        attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.STOP);
                    }
                });
                listItem.add(radio);

                Label name = new Label(ID_NAME, () -> listItem.getModelObject().getRealValue().getName());
                name.setOutputMarkupId(true);
                listItem.add(name);
            }
        };
        panel.setOutputMarkupId(true);
        radioGroup.add(panel);

        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(radioGroup);
                target.add(credentialsSection);
            }
        });

        Label noSelectionMessage = new Label(ID_NO_SELECTION_MESSAGE, () -> {
            if (valuesModel.getObject().isEmpty()) {
                return createStringResource("CredentialsConnectorStepPanel.noCredentialsNeeded").getObject();
            }
            return createStringResource("CredentialsConnectorStepPanel.selectMethod").getObject();
        });
        noSelectionMessage.add(new VisibleBehaviour(() -> selectedAuthModel.getObject() == null));
        credentialsSection.add(noSelectionMessage);

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(wrapper -> {
                    ConnDevAuthInfoType authType = selectedAuthModel.getObject();
                    if (authType == null) {
                        return ItemVisibility.HIDDEN;
                    }
                    return getVisibleItemsFor(authType).stream()
                            .anyMatch(vi -> StringUtils.equals(wrapper.getItemName().getLocalPart(), vi.getLocalPart()))
                            ? ItemVisibility.AUTO : ItemVisibility.HIDDEN;
                })
                .mandatoryHandler(wrapper -> {
                    ConnDevAuthInfoType authType = selectedAuthModel.getObject();
                    if (authType == null) {
                        return false;
                    }
                    return getVisibleItemsFor(authType).stream()
                            .anyMatch(vi -> StringUtils.equals(wrapper.getItemName().getLocalPart(), vi.getLocalPart()));
                })
                .build();

        VerticalFormPanel formPanel = new VerticalFormPanel(ID_FORM, getContainerFormModel(), settings, getContainerConfiguration(getPanelType())) {

            @Override
            protected void onBeforeRender() {
                super.onBeforeRender();
                ((VerticalFormPrismContainerPanel) getSingleContainerPanel().getContainer().get("1"))
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
        formPanel.add(new VisibleBehaviour(() -> selectedAuthModel.getObject() != null));
        formPanel.add(AttributeAppender.replace("class", "col-12 gen-list-group"));
        credentialsSection.add(formPanel);
    }

    private List<ItemName> getVisibleItemsFor(ConnDevAuthInfoType authType) {
        try {
            PrismPropertyWrapper<ConnDevIntegrationType> integration =
                    getDetailsModel().getObjectWrapper().findProperty(
                            ItemPath.create(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_INTEGRATION_TYPE));
            return new ArrayList<>(SupportedAuthorization.attributesFor(integration.getValue().getRealValue(), authType.getType()));
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
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
        for (PrismContainerValueWrapper<ConnDevAuthInfoType> authValue : valuesModel.getObject()) {
            List<ItemName> visibleItems = new ArrayList<>();
            ConnDevAuthInfoType authType = authValue.getRealValue();
            try {
                PrismPropertyWrapper<ConnDevIntegrationType> integration =
                        getDetailsModel().getObjectWrapper().findProperty(
                                ItemPath.create(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_INTEGRATION_TYPE));
                visibleItems.addAll(SupportedAuthorization.attributesFor(integration.getValue().getRealValue(), authType.getType()));

                if (visibleItems.stream().allMatch(
                        visibleItem -> ConnectorDevelopmentWizardUtil.existTestingResourcePropertyValue(
                                getDetailsModel(), getPanelType(), visibleItem))) {
                    return true;
                }

            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
