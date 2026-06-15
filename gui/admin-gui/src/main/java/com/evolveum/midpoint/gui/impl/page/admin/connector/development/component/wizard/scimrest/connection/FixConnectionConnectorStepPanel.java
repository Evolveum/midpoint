/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
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
import com.evolveum.midpoint.smart.api.conndev.ConnectorDevelopmentArtifacts;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.reports.component.SimpleAceEditorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "cdw-fix-connection")
@PanelInstance(identifier = "cdw-fix-connection",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.fixConnection", icon = "fa fa-wrench"),
        containerPath = "empty")
public class FixConnectionConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    public static final String PANEL_TYPE = "cdw-fix-connection";

    private static final String CREDENTIALS_PANEL_TYPE = "cdw-testing-credentials";

    private static final String ID_BASE_URL = "baseUrl";
    private static final String ID_ENDPOINT = "endpoint";
    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_AUTH_PANEL = "authPanel";
    private static final String ID_RADIO = "radio";
    private static final String ID_NAME = "name";
    private static final String ID_CREDENTIALS_SECTION = "credentialsSection";
    private static final String ID_CREDENTIALS_FORM = "credentialsForm";
    private static final String ID_NO_METHODS_MESSAGE = "noMethodsMessage";
    private static final String ID_NO_SELECTION_MESSAGE = "noSelectionMessage";
    private static final String ID_AUTH_SCRIPT_SECTION = "authScriptSection";
    private static final String ID_AUTH_SCRIPT_PANEL = "authScriptPanel";

    private IModel<String> baseUrlModel;
    private IModel<String> endpointModel;
    private LoadableModel<List<PrismContainerValueWrapper<ConnDevAuthInfoType>>> authValuesModel;
    private LoadableModel<ConnDevArtifactType> authScriptModel;

    public FixConnectionConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    public void init(WizardModel wizard) {
        super.init(wizard);
        initModels();
    }

    private void initModels() {
        baseUrlModel = new LoadableModel<>(false) {
            @Override
            protected String load() {
                ItemName urlField = ConnectorDevelopmentWizardUtil.isScim(getDetailsModel())
                        ? BaseUrlConnectorStepPanel.SCIM_BASE_URL_ITEM_NAME
                        : BaseUrlConnectorStepPanel.BASE_ADDRESS_ITEM_NAME;
                Object val = ConnectorDevelopmentWizardUtil.getTestingResourcePropertyValue(
                        getDetailsModel(), BaseUrlConnectorStepPanel.PANEL_TYPE, urlField);
                return val instanceof String s ? s : "";
            }
        };

        endpointModel = new LoadableModel<>(false) {
            @Override
            protected String load() {
                Object val = ConnectorDevelopmentWizardUtil.getTestingResourcePropertyValue(
                        getDetailsModel(), EndpointConnectorStepPanel.PANEL_TYPE, EndpointConnectorStepPanel.PROPERTY_ITEM_NAME);
                return val instanceof String s ? s : "";
            }
        };

        authValuesModel = new LoadableModel<>() {
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

        authScriptModel = new LoadableModel<>(false) {
            @Override
            protected ConnDevArtifactType load() {
                if (!ConnectorDevelopmentWizardUtil.existScript(getDetailsModel(),
                        ConnectorDevelopmentArtifacts.KnownArtifactType.AUTHENTICATION_CUSTOMIZATION, null)) {
                    return null;
                }
                try {
                    PrismContainerValueWrapper<ConnDevArtifactType> wrapper = ConnectorDevelopmentWizardUtil.getScript(
                            getDetailsModel(), ConnectorDevelopmentArtifacts.KnownArtifactType.AUTHENTICATION_CUSTOMIZATION, null);
                    if (wrapper == null) {
                        return null;
                    }
                    ConnDevArtifactType artifact = wrapper.getRealValue().clone();
                    Task task = getPageBase().createSimpleTask("loadAuthScript");
                    String content = getDetailsModel().getConnectorDevelopmentOperation()
                            .getArtifactContent(artifact, task, task.getResult());
                    artifact.setContent(content);
                    return artifact;
                } catch (IOException e) {
                    return null;
                }
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        TextField<String> baseUrlField = new TextField<>(ID_BASE_URL, baseUrlModel);
        baseUrlField.setOutputMarkupId(true);
        baseUrlField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        add(baseUrlField);

        TextField<String> endpointField = new TextField<>(ID_ENDPOINT, endpointModel);
        endpointField.setOutputMarkupId(true);
        endpointField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        add(endpointField);

        IModel<String> radioGroupModel = createRadioGroupModel();
        IModel<ConnDevAuthInfoType> selectedAuthModel = () -> {
            String name = radioGroupModel.getObject();
            if (name == null) {
                return null;
            }
            return authValuesModel.getObject().stream()
                    .filter(v -> StringUtils.equals(v.getRealValue().getName(), name))
                    .map(PrismContainerValueWrapper::getRealValue)
                    .findFirst().orElse(null);
        };

        Label noMethodsMessage = new Label(ID_NO_METHODS_MESSAGE, createStringResource("CredentialsConnectorStepPanel.noMethods"));
        noMethodsMessage.add(new VisibleBehaviour(() -> authValuesModel.getObject().isEmpty()));
        add(noMethodsMessage);

        RadioGroup<String> radioGroup = new RadioGroup<>(ID_RADIO_GROUP, radioGroupModel);
        radioGroup.setOutputMarkupId(true);
        add(radioGroup);

        WebMarkupContainer credentialsSection = new WebMarkupContainer(ID_CREDENTIALS_SECTION);
        credentialsSection.setOutputMarkupId(true);
        add(credentialsSection);

        ListView<PrismContainerValueWrapper<ConnDevAuthInfoType>> authPanel = new ListView<>(ID_AUTH_PANEL, authValuesModel) {
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
        authPanel.setOutputMarkupId(true);
        radioGroup.add(authPanel);

        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(radioGroup);
                target.add(credentialsSection);
            }
        });

        Label noSelectionMessage = new Label(ID_NO_SELECTION_MESSAGE, () -> {
            if (authValuesModel.getObject().isEmpty()) {
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
                    return ConnectorDevelopmentWizardUtil.getVisibleAuthorizationAttributes(getDetailsModel(), authType).stream()
                            .anyMatch(vi -> StringUtils.equals(wrapper.getItemName().getLocalPart(), vi.getLocalPart()))
                            ? ItemVisibility.AUTO : ItemVisibility.HIDDEN;
                })
                .mandatoryHandler(wrapper -> false)
                .build();

        VerticalFormPanel credentialsForm = new VerticalFormPanel(ID_CREDENTIALS_FORM, getCredentialsFormModel(), settings, getContainerConfiguration(CREDENTIALS_PANEL_TYPE)) {
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
        credentialsForm.setOutputMarkupId(true);
        credentialsForm.add(new VisibleBehaviour(() -> selectedAuthModel.getObject() != null));
        credentialsForm.add(AttributeAppender.replace("class", "col-12 gen-list-group"));
        credentialsSection.add(credentialsForm);

        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        WebMarkupContainer authScriptSection = new WebMarkupContainer(ID_AUTH_SCRIPT_SECTION);
        authScriptSection.setOutputMarkupId(true);
        add(authScriptSection);

        IModel<String> editorModel = new IModel<>() {
            @Override
            public String getObject() {
                ConnDevArtifactType artifact = authScriptModel.getObject();
                return artifact != null ? artifact.getContent() : "";
            }

            @Override
            public void setObject(String content) {
                ConnDevArtifactType artifact = authScriptModel.getObject();
                if (artifact == null) {
                    artifact = ConnectorDevelopmentArtifacts.authenticationScript();
                    authScriptModel.setObject(artifact);
                }
                artifact.setContent(content);
            }
        };

        SimpleAceEditorPanel authScriptPanel = new SimpleAceEditorPanel(ID_AUTH_SCRIPT_PANEL, editorModel, 400) {
            @Override
            protected AceEditor createEditor(String id, IModel<String> model, int minSize) {
                AceEditor editor = new AceEditor(id, model);
                editor.setReadonly(false);
                editor.setMinHeight(minSize);
                editor.setHeight(400);
                editor.setResizeToMaxHeight(false);
                editor.setMode(AceEditor.Mode.GROOVY);
                add(editor);
                return editor;
            }
        };
        ((AceEditor) authScriptPanel.getBaseFormComponent()).setConvertEmptyInputStringToNull(false);
        authScriptPanel.add(AttributeAppender.append("class", "d-flex flex-column w-100 border rounded"));
        authScriptSection.add(authScriptPanel);
    }

    private IModel<String> createRadioGroupModel() {
        return new IModel<>() {
            @Override
            public String getObject() {
                Optional<PrismContainerValueWrapper<ConnDevAuthInfoType>> selected = authValuesModel.getObject().stream()
                        .filter(PrismContainerValueWrapper::isSelected)
                        .findFirst();
                return selected.map(v -> v.getRealValue().getName()).orElse(null);
            }

            @Override
            public void setObject(String object) {
                authValuesModel.getObject().forEach(value -> value.setSelected(false));
                authValuesModel.getObject().stream()
                        .filter(value -> StringUtils.equals(value.getRealValue().getName(), object))
                        .findFirst()
                        .ifPresent(value -> value.setSelected(true));
            }
        };
    }

    private IModel<? extends PrismContainerWrapper> getCredentialsFormModel() {
        try {
            PrismReferenceWrapper<Referencable> resource = getDetailsModel().getObjectWrapper().findReference(
                    ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));
            ObjectDetailsModels<ResourceType> objectDetailsModel =
                    resource.getValue().getNewObjectModel(getContainerConfiguration(CREDENTIALS_PANEL_TYPE), getPageBase(), new OperationResult("getResourceModel"));
            ItemPath path = ItemPath.create("connectorConfiguration", SchemaConstants.ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME);
            return PrismContainerWrapperModel.fromContainerWrapper(objectDetailsModel.getObjectWrapperModel(), path);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean saveAuthScript(AjaxRequestTarget target) {
        ConnDevArtifactType artifact = authScriptModel.getObject();
        if (artifact == null || StringUtils.isBlank(artifact.getContent())) {
            return true;
        }
        Task task = getPageBase().createSimpleTask("saveAuthScript");
        try {
            ConnDevArtifactType script = artifact.clone();
            WebPrismUtil.cleanupEmptyContainerValue(script.asPrismContainerValue());
            getDetailsModel().getConnectorDevelopmentOperation().saveAuthenticationScript(script, task, task.getResult());
        } catch (IOException | CommonException e) {
            throw new RuntimeException(e);
        }
        if (task.getResult() == null || task.getResult().isError()) {
            target.add(getFeedback());
            return false;
        }
        return true;
    }

    private void testConnection(AjaxRequestTarget target) {
        String url = endpointModel.getObject();
        String baseUrl = baseUrlModel.getObject();

        try {
            if (StringUtils.isNotEmpty(baseUrl)) {
                ItemName urlField = ConnectorDevelopmentWizardUtil.isScim(getDetailsModel())
                        ? BaseUrlConnectorStepPanel.SCIM_BASE_URL_ITEM_NAME
                        : BaseUrlConnectorStepPanel.BASE_ADDRESS_ITEM_NAME;
                ConnectorDevelopmentWizardUtil.setTestingResourcePropertyValue(getDetailsModel(), PANEL_TYPE, urlField, baseUrl);
            }
            if (StringUtils.isNotEmpty(url)) {
                ConnectorDevelopmentWizardUtil.setTestingResourcePropertyValue(
                        getDetailsModel(), PANEL_TYPE, EndpointConnectorStepPanel.PROPERTY_ITEM_NAME, url);
            }
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        if (!saveAuthScript(target)) {
            return;
        }

        OperationResult saveResult = getHelper().onSaveObjectPerformed(target);
        if (saveResult != null && !saveResult.isError()) {
            getDetailsModel().reloadPrismObjectByOid();
            super.onNextPerformed(target);
        } else {
            target.add(getFeedback());
        }
        target.add(getButtonContainer());
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        testConnection(target);
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("FixConnectionConnectorStepPanel.submit");
    }

    @Override
    public IModel<Boolean> isStepVisible() {
        return () -> {
            if (getWizard() instanceof WizardModelWithParentSteps wizardModel) {
                return wizardModel.isStepWithError(PANEL_TYPE);
            }
            return false;
        };
    }

    @Override
    public boolean isCompleted() {
        return ConnectorDevelopmentWizardUtil.isConnectionComplete(getDetailsModel());
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.fixConnection");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.fixConnection.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.fixConnection.subText");
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
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
