/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-endpoint-path")
@PanelInstance(identifier = "cdw-endpoint-path",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.endpointPath", icon = "fa fa-wrench"),
        containerPath = "empty")
public class EndpointConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String PANEL_TYPE = "cdw-endpoint-path";

    public static final ItemName PROPERTY_ITEM_NAME = ItemName.from("", "restTestEndpoint");

    private static final String MANUAL_OPTION = "__manual__";

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_PANEL = "panel";
    private static final String ID_RADIO = "radio";
    private static final String ID_NAME = "name";
    private static final String ID_CONNECTED_BADGE = "connectedBadge";
    private static final String ID_MANUAL_ITEM = "manualItem";
    private static final String ID_MANUAL_RADIO = "manualRadio";
    private static final String ID_MANUAL_URL = "manualUrl";
    private static final String ID_MANUAL_BADGE = "manualBadge";

    private LoadableModel<List<ConnDevHttpEndpointType>> endpointsModel;
    private IModel<String> selectedModel;
    private IModel<String> manualUrlModel;
    private IModel<String> connectedUrl;

    private AjaxIconButton actionButton;

    public EndpointConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    public void init(WizardModel wizard) {
        super.init(wizard);

        endpointsModel = new LoadableModel<>() {
            @Override
            protected List<ConnDevHttpEndpointType> load() {
                try {
                    PrismContainerWrapper<ConnDevHttpEndpointType> container = getDetailsModel().getObjectWrapper().findContainer(
                            ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_SUGGESTED_ENDPOINT));
                    if (container == null || container.getValues().isEmpty()) {
                        return Collections.emptyList();
                    }
                    return container.getValues().stream()
                            .map(v -> v.getRealValue())
                            .collect(Collectors.toList());
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        selectedModel = new IModel<>() {
            private String selected = null;

            @Override
            public String getObject() {
                if (selected == null) {
                    List<ConnDevHttpEndpointType> endpoints = endpointsModel.getObject();
                    selected = endpoints.isEmpty() ? MANUAL_OPTION : endpoints.get(0).getUri();
                }
                return selected;
            }

            @Override
            public void setObject(String s) {
                selected = s;
            }
        };

        manualUrlModel = Model.of("");
        connectedUrl = Model.of((String) null);

        Object existingUrlObj = ConnectorDevelopmentWizardUtil.getTestingResourcePropertyValue(
                getDetailsModel(), PANEL_TYPE, PROPERTY_ITEM_NAME);
        String existingUrl = existingUrlObj instanceof String s ? s : null;
        if (StringUtils.isNotEmpty(existingUrl)) {
            boolean matchesSuggested = endpointsModel.getObject().stream()
                    .anyMatch(e -> existingUrl.equals(e.getUri()));
            if (matchesSuggested) {
                selectedModel.setObject(existingUrl);
            } else {
                selectedModel.setObject(MANUAL_OPTION);
                manualUrlModel.setObject(existingUrl);
            }
            if (isCompleted()) {
                connectedUrl.setObject(existingUrl);
            }
        }
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

        RadioGroup<String> radioGroup = new RadioGroup<>(ID_RADIO_GROUP, selectedModel);
        radioGroup.setOutputMarkupId(true);
        add(radioGroup);

        ListView<ConnDevHttpEndpointType> panel = new ListView<>(ID_PANEL, endpointsModel) {
            @Override
            protected void populateItem(ListItem<ConnDevHttpEndpointType> item) {
                ConnDevHttpEndpointType endpoint = item.getModelObject();

                Radio<String> radio = new Radio<>(ID_RADIO, Model.of(endpoint.getUri()), radioGroup);
                radio.setOutputMarkupId(true);
                item.add(radio);

                item.add(new Label(ID_NAME, StringUtils.defaultString(endpoint.getUri())));

                WebMarkupContainer badge = new WebMarkupContainer(ID_CONNECTED_BADGE);
                badge.setOutputMarkupPlaceholderTag(true);
                badge.add(new VisibleBehaviour(() ->
                        StringUtils.equals(endpoint.getUri(), connectedUrl.getObject())));
                item.add(badge);

                item.add(AttributeAppender.append("style", "cursor: pointer;"));
                item.add(new AjaxEventBehavior("click") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        selectedModel.setObject(endpoint.getUri());
                        target.add(radioGroup);
                        target.add(getButtonContainer());
                    }
                });
            }
        };
        panel.setOutputMarkupId(true);
        radioGroup.add(panel);

        WebMarkupContainer manualItem = new WebMarkupContainer(ID_MANUAL_ITEM);
        manualItem.setOutputMarkupId(true);
        manualItem.add(AttributeAppender.append("style", "cursor: pointer;"));
        manualItem.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                selectedModel.setObject(MANUAL_OPTION);
                target.add(radioGroup);
                target.add(getButtonContainer());
            }
        });
        radioGroup.add(manualItem);

        Radio<String> manualRadio = new Radio<>(ID_MANUAL_RADIO, Model.of(MANUAL_OPTION), radioGroup);
        manualRadio.setOutputMarkupId(true);
        manualItem.add(manualRadio);

        TextField<String> manualUrl = new TextField<>(ID_MANUAL_URL, manualUrlModel);
        manualUrl.setOutputMarkupId(true);
        manualUrl.setOutputMarkupPlaceholderTag(true);
        manualUrl.add(AttributeAppender.append("onclick", "event.stopPropagation()"));
        manualUrl.add(new VisibleBehaviour(() -> MANUAL_OPTION.equals(selectedModel.getObject())));
        manualUrl.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // model updated automatically
            }
        });
        manualItem.add(manualUrl);

        WebMarkupContainer manualBadge = new WebMarkupContainer(ID_MANUAL_BADGE);
        manualBadge.setOutputMarkupPlaceholderTag(true);
        manualBadge.add(new VisibleBehaviour(() ->
                MANUAL_OPTION.equals(selectedModel.getObject())
                        && StringUtils.isNotEmpty(connectedUrl.getObject())
                        && connectedUrl.getObject().equals(manualUrlModel.getObject())));
        manualItem.add(manualBadge);

        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(radioGroup);
                target.add(getButtonContainer());
            }
        });
    }

    private String getSelectedUrl() {
        return MANUAL_OPTION.equals(selectedModel.getObject())
                ? manualUrlModel.getObject()
                : selectedModel.getObject();
    }

    private boolean isCurrentUrlConnected() {
        String url = getSelectedUrl();
        return StringUtils.isNotEmpty(url) && url.equals(connectedUrl.getObject());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setRestTestEndpoint(String url) throws SchemaException {
        ObjectDetailsModels<ResourceType> resourceModel = ConnectorDevelopmentWizardUtil.getTestingResourceModel(getDetailsModel(), PANEL_TYPE);
        ItemPath path = ItemPath.create("connectorConfiguration", SchemaConstants.ICF_CONFIGURATION_PROPERTIES_LOCAL_NAME, PROPERTY_ITEM_NAME);
        PrismPropertyWrapper prop = resourceModel.getObjectWrapper().findProperty(path);
        if (prop != null && !prop.getValues().isEmpty()) {
            ((PrismPropertyValueWrapper) prop.getValue()).setRealValue(url);
        }
    }

    private OperationResult runResourceTest() throws SchemaException {
        PrismReferenceWrapper<Referencable> resource = getDetailsModel().getObjectWrapper().findReference(
                ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));
        String resourceOid = resource.getValue().getRealValue().getOid();

        Task task = getPageBase().createSimpleTask("testResource");
        ConnectorDevelopmentWizardUtil.enableConnectorLogCapture(task);
        OperationResult result = task.getResult();
        try {
            getPageBase().getModelService().testResource(resourceOid, task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
        }
        result.computeStatus();
        return result;
    }

    private void testConnection(AjaxRequestTarget target) {
        String url = getSelectedUrl();
        if (StringUtils.isEmpty(url)) {
            target.add(getFeedback());
            return;
        }
        try {
            setRestTestEndpoint(url);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        OperationResult saveResult = getHelper().onSaveObjectPerformed(target);
        if (saveResult != null && !saveResult.isError()) {
            try {
                OperationResult testResult = runResourceTest();
                if (!testResult.isError()) {
                    connectedUrl.setObject(url);
                    target.add(get(ID_RADIO_GROUP));
                } else {
                    getPageBase().showResult(testResult);
                }
                target.add(getFeedback());
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }
        } else {
            target.add(getFeedback());
        }
        target.add(getButtonContainer());
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        if (isCurrentUrlConnected()) {
            super.onNextPerformed(target);
            return false;
        }
        testConnection(target);
        return false;
    }

    @Override
    protected void initCustomButtons(RepeatingView customButtons) {
        IModel<String> iconModel = () -> isCurrentUrlConnected()
                ? "fa fa-arrow-right"
                : "fa fa-tower-broadcast";

        IModel<String> labelModel = () -> isCurrentUrlConnected()
                ? getString("EndpointConnectorStepPanel.continue")
                : getString("EndpointConnectorStepPanel.submit");

        actionButton = new AjaxIconButton(customButtons.newChildId(), iconModel, labelModel) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onNextPerformed(target);
            }
        };
        actionButton.showTitleAsLabel(true);
        actionButton.setOutputMarkupId(true);
        actionButton.add(AttributeAppender.replace("class", "btn btn-primary"));
        actionButton.add(AttributeAppender.append("onclick",
                "if(!this.classList.contains('connected-btn')){"
                + "this.innerHTML='<i class=\"fa fa-spinner fa-spin mr-1\"></i>"
                + getString("EndpointConnectorStepPanel.connecting") + "';"
                + "this.style.pointerEvents='none';}"));
        customButtons.add(actionButton);
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.endpointPath");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.endpointPath.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.endpointPath.subText");
    }

    @Override
    public boolean isCompleted() {
        return ConnectorDevelopmentWizardUtil.isConnectionComplete(getDetailsModel(), PANEL_TYPE);
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
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
