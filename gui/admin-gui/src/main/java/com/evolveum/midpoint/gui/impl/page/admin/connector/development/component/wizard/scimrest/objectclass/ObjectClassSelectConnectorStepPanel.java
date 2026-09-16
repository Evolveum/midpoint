/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-select-object-class")
@PanelInstance(identifier = "cdw-select-object-class",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.selectObjectClass", icon = "fa fa-wrench"),
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
    private static final String ID_MORE_CLASSES_HINT = "moreClassesHint";

    private final IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel;
    private boolean showAllClasses;
    private LoadableModel<List<PrismContainerValueWrapper<ConnDevObjectClassInfoType>>> valuesModel;

    public ObjectClassSelectConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> valueModel) {
        super(helper);
        this.valueModel = valueModel;
        this.showAllClasses = false;
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
                List<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> candidates = getObjectClassCandidates();
                if (showAllClasses) {
                    return candidates;
                }
                // By default show only the classes with the highest confidence present in the list.
                // "More classes" reveals the ones with lower confidence.
                ConnDevRelevancyLevelType highest = highestRelevancy(candidates);
                return candidates.stream()
                        .filter(value -> {
                            ConnDevRelevancyLevelType relevancy = value.getRealValue().getRelevancy();
                            return relevancy == null // backwards compatibility (no confidence detected)
                                    || highest == null // nothing ranked at all
                                    || relevancy == highest; // top confidence level present
                        })
                        .toList();
            }
        };
    }

    /**
     * Detected object classes that are not yet saved into the connector's object class list.
     */
    private List<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> getObjectClassCandidates() {
        PrismContainerWrapper<ConnDevObjectClassInfoType> container;
        try {
            container = getDetailsModel().getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_APPLICATION,
                            ConnDevApplicationInfoType.F_DETECTED_SCHEMA,
                            ConnDevSchemaType.F_OBJECT_CLASS));
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        return container.getValues().stream()
                .filter(value ->
                        getDetailsModel().getObjectType().getConnector().getObjectClass().stream()
                                .noneMatch(savedObjectClass -> StringUtils.equals(savedObjectClass.getName(), value.getRealValue().getName())))
                .toList();
    }

    /**
     * Highest confidence level present among the candidates, or {@code null} when none of them
     * carries a relevancy (older detected schemas without confidence).
     */
    private static ConnDevRelevancyLevelType highestRelevancy(
            List<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> candidates) {
        // ConnDevRelevancyLevelType is declared low, medium, high, so the enum's natural order
        // (ordinal) already ranks confidence from lowest to highest.
        return candidates.stream()
                .map(value -> value.getRealValue().getRelevancy())
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * True when there are candidates with a confidence lower than the highest present, i.e. there is
     * something to reveal via "More classes".
     */
    private boolean hasLowerRelevancyClasses() {
        List<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> candidates = getObjectClassCandidates();
        ConnDevRelevancyLevelType highest = highestRelevancy(candidates);
        if (highest == null) {
            return false;
        }
        return candidates.stream()
                .map(value -> value.getRealValue().getRelevancy())
                .anyMatch(relevancy -> relevancy != null && relevancy.compareTo(highest) < 0);
    }

    /**
     * Whether the "More classes" affordance (button and hint banner) should be shown, i.e. we are
     * not already showing everything and there are lower-confidence classes to reveal.
     */
    private boolean isMoreClassesAvailable() {
        return !showAllClasses && hasLowerRelevancyClasses();
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        WebMarkupContainer moreClassesHint = new WebMarkupContainer(ID_MORE_CLASSES_HINT);
        moreClassesHint.setOutputMarkupId(true);
        moreClassesHint.setOutputMarkupPlaceholderTag(true);
        moreClassesHint.add(new VisibleBehaviour(this::isMoreClassesAvailable));
        add(moreClassesHint);

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
                showAllClasses = true;
                valuesModel.reset();
                target.add(radioGroup);
                target.add(this);
                target.add(moreClassesHint);
            }
        };

        moreClasses.showTitleAsLabel(true);
        moreClasses.setOutputMarkupId(true);
        moreClasses.setOutputMarkupPlaceholderTag(true);
        moreClasses.add(new VisibleBehaviour(this::isMoreClassesAvailable));
        add(moreClasses);

        String cssClass;
        if (valuesModel.getObject().size() == 1) {
            cssClass = "";
        } else if (valuesModel.getObject().size() == 2) {
            cssClass = "col-6";
        } else {
            cssClass = "col-4";
        }
        ListView<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> panel = new ListView<>(ID_PANEL, valuesModel) {
            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> listItem) {
                listItem.add(AttributeAppender.append("class", cssClass));

                Radio<String> radio = new Radio<>(ID_RADIO, Model.of(listItem.getModelObject().getRealValue().getName()), radioGroup);
                radio.setOutputMarkupId(true);
                listItem.add(radio);

                Label name = new Label(ID_NAME, () -> listItem.getModelObject().getRealValue().getName());
                name.setOutputMarkupId(true);
                listItem.add(name);

                Label description = new Label(ID_DESCRIPTION, () -> listItem.getModelObject().getRealValue().getDescription());
                description.setOutputMarkupId(true);
                listItem.add(description);

                listItem.add(AttributeAppender.append("style", "cursor: pointer;"));
                listItem.add(new AjaxEventBehavior("click") {
                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        radioGroupModel.setObject(listItem.getModelObject().getRealValue().getName());
                        target.add(radioGroup);
                    }
                });
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
        return createStringResource("PageConnectorDevelopment.wizard.step.selectObjectClass");
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
        return "col-12";
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
            valueModel.detach();
        }
        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();
        if (result != null && !result.isError()) {
//            ObjectClassConnectorStepPanel step = new ObjectClassConnectorStepPanel(getHelper());
//            WizardModel wizardModel = getWizard();
//            wizardModel.addStepBefore(step, ObjectClassConnectorStepPanel.class);
//            if (wizardModel instanceof WizardModelWithParentSteps wizardModelWithParentSteps) {
//                wizardModelWithParentSteps.setActiveChildStepById(ObjectClassConnectorStepPanel.PANEL_TYPE);
//            } else {
//                wizardModel.setActiveStepById(ObjectClassConnectorStepPanel.PANEL_TYPE);
//            }
//            wizardModel.fireActiveStepChanged();
//            target.add(getWizard().getPanel());
            super.onNextPerformed(target);
        } else {
            target.add(getFeedback());
        }
        return false;
    }

    @Override
    public boolean isCompleted() {
        if (valueModel.getObject() == null) {
            return false;
        }

        return valueModel.getObject().getStatus() != ValueStatus.ADDED;
    }

    @Override
    protected String getSubTextContainerCssClass() {
        return "text-secondary col-12 pb-4";
    }
}
