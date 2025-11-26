/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.CloneStrategy;

import com.evolveum.midpoint.prism.PrismContainerValue;

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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
public abstract class EndpointsConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_PANEL = "panel";
    private static final String ID_RADIO = "radio";
    private static final String ID_NAME = "name";
    private static final String ID_OPERATION = "operation";
    private static final String ID_URI = "uri";

    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel;

    private LoadableModel<List<PrismContainerValueWrapper<ConnDevHttpEndpointType>>> valuesModel;

    public EndpointsConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel) {
        super(helper);
        this.objectClassModel = objectClassModel;
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
            protected List<PrismContainerValueWrapper<ConnDevHttpEndpointType>> load() {
                try {
                    PrismContainerWrapper<ConnDevObjectClassInfoType> container = getDetailsModel().getObjectWrapper().findContainer(
                            ItemPath.create(ConnectorDevelopmentType.F_APPLICATION, ConnDevApplicationInfoType.F_DETECTED_SCHEMA, ConnDevSchemaType.F_OBJECT_CLASS));
                    Optional<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassContainer = container.getValues().stream().filter(value ->
                                    StringUtils.equals(value.getRealValue().getName(), objectClassModel.getObject().getRealValue().getName()))
                            .findFirst();

                    if (objectClassContainer.isPresent()) {
                        try {
                            PrismContainerWrapper<ConnDevHttpEndpointType> endpointsContainer = objectClassContainer
                                    .get().findContainer(ConnDevObjectClassInfoType.F_ENDPOINT);
                            return endpointsContainer.getValues().stream()
                                    .filter(value -> value.getRealValue().getSuggestedUse().contains(getOperation()))
                                    .toList();
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return List.of();
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    protected abstract ConnDevHttpEndpointIntentType getOperation();

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        IModel<String> radioGroupModel = new IModel<>() {
            @Override
            public String getObject() {
                Optional<PrismContainerValueWrapper<ConnDevHttpEndpointType>> selected = valuesModel.getObject().stream()
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

        ListView<PrismContainerValueWrapper<ConnDevHttpEndpointType>> panel = new ListView<>(ID_PANEL, valuesModel) {
            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ConnDevHttpEndpointType>> listItem) {
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

                Label operation = new Label(ID_OPERATION, createStringResource(listItem.getModelObject().getRealValue().getOperation()));
                operation.setOutputMarkupId(true);
                listItem.add(operation);

                Label uri = new Label(ID_URI, () -> listItem.getModelObject().getRealValue().getUri());
                uri.setOutputMarkupId(true);
                listItem.add(uri);
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
        try {
            PrismContainerWrapper<ConnDevHttpEndpointType> container =
                    objectClassModel.getObject().findContainer(ConnDevObjectClassInfoType.F_ENDPOINT);

            List<PrismContainerValueWrapper<ConnDevHttpEndpointType>> valuesToAdd = valuesModel.getObject()
                    .stream().filter(PrismContainerValueWrapper::isSelected)
                    .map(value -> {
                        try {
                            PrismContainerValue<ConnDevHttpEndpointType> clone =
                                    value.getRealValue().asPrismContainerValue().cloneComplex(CloneStrategy.REUSE);
                            clone.removeItem(ConnDevHttpEndpointType.F_SUGGESTED_USE);
                            clone.asContainerable().suggestedUse(getOperation());

                            return (PrismContainerValueWrapper<ConnDevHttpEndpointType>) WebPrismUtil.createNewValueWrapper(
                                    container,
                                    clone,
                                    getPageBase(),
                                    getDetailsModel().createWrapperContext());
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            List<PrismContainerValueWrapper<ConnDevHttpEndpointType>> valuesToRemove = container.getValues().stream()
                    .filter(value -> value.getRealValue().getSuggestedUse().contains(getOperation()))
                    .toList();
            valuesToRemove.forEach(
                    value -> {
                        try {
                            container.remove(value, getDetailsModel().getPageAssignmentHolder());
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                    });

            valuesToAdd.forEach(value -> {
//                try {
//                    container.getItem().add(value.getRealValue().asPrismContainerValue());
                    container.getValues().add(value);
//                } catch (SchemaException e) {
//                    throw new RuntimeException(e);
//                }
            });

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

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
        if (ConnectorDevelopmentWizardUtil.existContainerValue(objectClassModel.getObject(), getScriptItemName())) {
            return true;
        }

        try {
            PrismContainerWrapper<ConnDevHttpEndpointType> container =
                    objectClassModel.getObject().findContainer(ConnDevObjectClassInfoType.F_ENDPOINT);

            return container.getValues().stream()
                    .anyMatch(value -> value.getRealValue() != null
                            && value.getRealValue().getSuggestedUse().contains(getOperation()));

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract ItemPath getScriptItemName();
}
