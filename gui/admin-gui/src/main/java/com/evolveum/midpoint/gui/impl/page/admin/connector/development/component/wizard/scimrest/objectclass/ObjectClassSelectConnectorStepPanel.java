/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serial;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator.WizardParentStep;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-select-object-class")
@PanelInstance(identifier = "cdw-select-object-class",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.objectClass", icon = "fa fa-wrench"),
        containerPath = "empty")
public class ObjectClassSelectConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String PANEL_TYPE = "cdw-select-object-class";

    public static final String OBJECT_CLASS_NAME = "cdw-select-object-class";

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_PANEL = "panel";
    private static final String ID_RADIO = "radio";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_MORE_OBJECT_CLASSES_BUTTON = "moreObjectClassesButton";

    private LoadableModel<List<PrismContainerValueWrapper<ConnDevObjectClassInfoType>>> valuesModel;

    public ObjectClassSelectConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
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
            protected List<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> load() {
                PrismContainerWrapper<ConnDevObjectClassInfoType> container;
                try {
                    container = getDetailsModel().getObjectWrapper().findContainer(
                            ItemPath.create(ConnectorDevelopmentType.F_APPLICATION,
                                    ConnDevApplicationInfoType.F_DETECTED_SCHEMA,
                                    ConnDevSchemaType.F_OBJECT_CLASS));
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }

                return container.getValues();
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
                return valuesModel.getObject().stream()
                        .filter(PrismContainerValueWrapper::isSelected)
                        .findFirst()
                        .map(containerValueWrapper -> containerValueWrapper.getRealValue().getName())
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

        AjaxIconButton moreClasses = new AjaxIconButton(ID_MORE_OBJECT_CLASSES_BUTTON,
                Model.of(GuiStyleConstants.CLASS_ICON_PREVIEW),
                createStringResource("ObjectClassSelectConnectorStepPanel.moreClasses")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
            }
        };

        moreClasses.showTitleAsLabel(true);
        add(moreClasses);

        ListView<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> panel = new ListView<>(ID_PANEL, valuesModel) {
            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> listItem) {
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

                Label description = new Label(ID_DESCRIPTION, () -> listItem.getModelObject().getRealValue().getDescription());
                description.setOutputMarkupId(true);
                listItem.add(description);
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

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.objectClass");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.selectObjectClass.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.selectObjectClass.subText");
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
        AtomicReference<String> objectClassName = new AtomicReference<>();

        try {
            PrismContainerWrapper<ConnDevObjectClassInfoType> parentWrapper =
                    getDetailsModel().getObjectWrapper().findContainer(
                            ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_OBJECT_CLASS));

            valuesModel.getObject().stream()
                    .filter(PrismContainerValueWrapper::isSelected)
                    .map(detectedValue -> {
                        try {
                            //noinspection unchecked
                            return (PrismContainerValueWrapper<ConnDevObjectClassInfoType>) getPageBase().createValueWrapper(
                                    parentWrapper, detectedValue.getRealValue().asPrismContainerValue().clone(), ValueStatus.ADDED, getDetailsModel().createWrapperContext());
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .findFirst()
                    .ifPresent(valueWrapper -> {
                        objectClassName.set(valueWrapper.getRealValue().getName());
                        try {
                            //noinspection unchecked
                            parentWrapper.getItem().add(valueWrapper.getRealValue().asPrismContainerValue());
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                        parentWrapper.getValues().add(valueWrapper);
                    });

        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        if (objectClassName.get() != null) {
            getHelper().putVariable(OBJECT_CLASS_NAME, objectClassName.get());
        }
        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();
        if (result != null && !result.isError()) {
            ObjectClassConnectorStepPanel step = new ObjectClassConnectorStepPanel(getHelper());
            WizardModel wizardModel = getWizard();
            wizardModel.addStepBefore(step, ObjectClassConnectorStepPanel.class);
            if (wizardModel instanceof WizardModelWithParentSteps wizardModelWithParentSteps) {
                wizardModelWithParentSteps.setActiveChildStepById(ObjectClassConnectorStepPanel.PANEL_TYPE);
            } else {
                wizardModel.setActiveStepById(ObjectClassConnectorStepPanel.PANEL_TYPE);
            }
            wizardModel.fireActiveStepChanged();
            target.add(getWizard().getPanel());
        } else {
            target.add(getFeedback());
        }
        return false;
    }
}
