/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import java.io.Serial;
import java.util.List;

import org.apache.commons.lang3.Strings;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

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
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.HelpInfoPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.HelpTab;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorWizardHelpTopics;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-select-relationship")
@PanelInstance(identifier = "cdw-select-relationship",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.selectRelationship", icon = "fa fa-wrench"),
        containerPath = "empty")
public class RelationshipSelectConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> {

    private static final String PANEL_TYPE = "cdw-select-relationship";

    public static final String RELATIONSHIP_NAME = "cdw-select-relationship";

    private static final int MAX_RECOMMENDED = 5;

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_PANEL = "panel";
    private static final String ID_ROW = "row";
    private static final String ID_BROWSE_MORE_BUTTON = "browseMoreButton";
    private static final String ID_COUNTER = "counter";

    private final IModel<PrismContainerValueWrapper<ConnDevRelationInfoType>> valueModel;

    /**
     * State of selecting one of the detected relationships, shared with the popup with the other relationships.
     */
    private final LoadableModel<RelationshipSelectionState> stateModel = LoadableModel.create(this::createState, false);

    public RelationshipSelectConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevRelationInfoType>> valueModel) {
        super(helper);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }


    private RelationshipSelectionState createState() {
        List<ConnDevObjectClassInfoType> objectClasses = getConfiguredObjectClasses();
        List<ConnDevRelationInfoType> connectorRelations = getConnectorRelations();
        List<RelationshipDto> relationships = getDetectedRelations().stream()
                .filter(relation -> hasConfiguredObjectClass(relation, objectClasses))
                .filter(relation -> !isInConnector(relation, connectorRelations))
                .map(relation -> RelationshipDto.create(relation, objectClasses))
                .sorted(new RelationshipConfigurationComparator())
                .toList();
        return new RelationshipSelectionState(
                relationships.stream().limit(MAX_RECOMMENDED).toList(),
                relationships.stream().skip(MAX_RECOMMENDED).toList());
    }

    private List<ConnDevRelationInfoType> getDetectedRelations() {
        try {
            PrismContainerWrapper<ConnDevRelationInfoType> relationsContainer = getDetailsModel().getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_APPLICATION,
                            ConnDevApplicationInfoType.F_DETECTED_SCHEMA,
                            ConnDevSchemaType.F_RELATION));
            return relationsContainer.getValues().stream()
                    .map(PrismContainerValueWrapper::getRealValue)
                    .toList();
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ConnDevObjectClassInfoType> getConfiguredObjectClasses() {
        try {
            PrismContainerWrapper<ConnDevObjectClassInfoType> objectClassesContainer = getDetailsModel().getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR,
                            ConnDevConnectorType.F_OBJECT_CLASS));
            return objectClassesContainer.getValues().stream()
                    .map(PrismContainerValueWrapper::getRealValue)
                    .toList();
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ConnDevRelationInfoType> getConnectorRelations() {
        return getConnectorRelationsContainer().getValues().stream()
                .map(PrismContainerValueWrapper::getRealValue)
                .toList();
    }

    private PrismContainerWrapper<ConnDevRelationInfoType> getConnectorRelationsContainer() {
        try {
            return getDetailsModel().getObjectWrapper().findContainer(
                    ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_RELATION));
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasConfiguredObjectClass(ConnDevRelationInfoType relation, List<ConnDevObjectClassInfoType> objectClasses) {
        return objectClasses.stream().anyMatch(objectClass ->
                Strings.CS.equals(relation.getSubject(), objectClass.getName())
                        || Strings.CS.equals(relation.getObject(), objectClass.getName()));
    }

    private boolean isInConnector(ConnDevRelationInfoType relation, List<ConnDevRelationInfoType> connectorRelations) {
        return connectorRelations.stream().anyMatch(connectorRelation ->
                Strings.CS.equals(relation.getSubject(), connectorRelation.getSubject())
                        && Strings.CS.equals(relation.getSubjectAttribute(), connectorRelation.getSubjectAttribute())
                        && Strings.CS.equals(relation.getObject(), connectorRelation.getObject())
                        && Strings.CS.equals(relation.getObjectAttribute(), connectorRelation.getObjectAttribute()));
    }

    private void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-2 col-12 gen-step-title"));
        getSubtextLabel().add(AttributeAppender.replace("class", "border-bottom pb-4 d-inline-block w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex align-items-center flex-nowrap flex-row mt-4 gap-2 wizard-actions-strip col-12"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));
        getSubmit().add(AttributeAppender.replace("class", "btn btn-primary"));

        RadioGroup<RelationshipDto> radioGroup = new RadioGroup<>(ID_RADIO_GROUP,
                LambdaModel.of(stateModel, RelationshipSelectionState::getSelectedRelationship, RelationshipSelectionState::setSelectedRelationship));
        radioGroup.setOutputMarkupId(true);
        add(radioGroup);

        ListView<RelationshipDto> panel = new ListView<>(ID_PANEL, () -> stateModel.getObject().getRecommendedRelationships()) {
            @Override
            protected void populateItem(ListItem<RelationshipDto> listItem) {
                listItem.add(createRelationshipRowPanel(listItem.getModel()));
            }
        };
        panel.setOutputMarkupId(true);
        radioGroup.add(panel);

        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget object) {
            }
        });

        add(createBrowseMoreButton());
        add(createCounter());
    }

    private RelationshipRowPanel createRelationshipRowPanel(IModel<RelationshipDto> model) {
        return new RelationshipRowPanel(ID_ROW, model);
    }

    private AjaxButton createBrowseMoreButton() {
        AjaxButton browseMore = new AjaxButton(ID_BROWSE_MORE_BUTTON,
                () -> createStringResource("RelationshipSelectConnectorStepPanel.browseMore", stateModel.getObject().getOtherRelationships().size()).getString()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showBrowseRelationshipsPopup(target);
            }
        };
        browseMore.add(new VisibleBehaviour(() -> !stateModel.getObject().getOtherRelationships().isEmpty()));
        return browseMore;
    }

    private Label createCounter() {
        Label counter = new Label(ID_COUNTER, () -> {
            int count = stateModel.getObject().getRecommendedRelationships().size();
            return createStringResource("CountToolbar.label", 1, count, count).getString();
        });
        counter.add(new VisibleBehaviour(() -> !stateModel.getObject().getRecommendedRelationships().isEmpty()));
        return counter;
    }

    private void showBrowseRelationshipsPopup(AjaxRequestTarget target) {
        BrowseRelationshipsPopup popup = new BrowseRelationshipsPopup(getPageBase().getMainPopupBodyId(), stateModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void continuePerformed(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                onNextPerformed(target);
            }
        };
        getPageBase().showMainPopup(popup, target);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.selectRelationship");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.selectRelationship.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.selectRelationship.subText");
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
        return true;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("RelationshipSelectConnectorStepPanel.generateScripts");
    }

    @Override
    protected String getSubmitIcon() {
        return "fa fa-gears";
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        super.onSubmitPerformed(target);
        onNextPerformed(target);
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        RelationshipDto selected = stateModel.getObject().getSelectedRelationship();
        if (selected == null) {
            showNoSelectedRelationshipPopup(target);
            return false;
        }

        PrismContainerValueWrapper<ConnDevRelationInfoType> addedRelationship = addRelationshipToConnector(selected);
        String relationshipName = addedRelationship.getRealValue().getName();
        if (relationshipName != null) {
            getHelper().putVariable(RELATIONSHIP_NAME, relationshipName);
            valueModel.detach();
        }
        OperationResult result = getHelper().onSaveObjectPerformed(target);
        getDetailsModel().getConnectorDevelopmentOperation();
        if (result != null && !result.isError()) {
            stateModel.reset();
            super.onNextPerformed(target);
        } else {
            removeRelationshipFromConnector(addedRelationship);
            getHelper().removeVariable(RELATIONSHIP_NAME);
            target.add(getFeedback());
        }
        return false;
    }

    private PrismContainerValueWrapper<ConnDevRelationInfoType> addRelationshipToConnector(RelationshipDto relationship) {
        PrismContainerWrapper<ConnDevRelationInfoType> relationsContainer = getConnectorRelationsContainer();
        try {
            //noinspection unchecked
            PrismContainerValueWrapper<ConnDevRelationInfoType> valueWrapper =
                    (PrismContainerValueWrapper<ConnDevRelationInfoType>) getPageBase().createValueWrapper(
                            relationsContainer, relationship.getRelation().asPrismContainerValue().clone(), ValueStatus.ADDED, getDetailsModel().createWrapperContext());
            //noinspection unchecked
            relationsContainer.getItem().add(valueWrapper.getRealValue().asPrismContainerValue());
            relationsContainer.getValues().add(valueWrapper);
            return valueWrapper;
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeRelationshipFromConnector(PrismContainerValueWrapper<ConnDevRelationInfoType> valueWrapper) {
        PrismContainerWrapper<ConnDevRelationInfoType> relationsContainer = getConnectorRelationsContainer();
        relationsContainer.getItem().remove(valueWrapper.getRealValue().asPrismContainerValue());
        relationsContainer.getValues().remove(valueWrapper);
    }

    private void showNoSelectedRelationshipPopup(AjaxRequestTarget target) {
        HelpInfoPanel popup = new HelpInfoPanel(
                getPageBase().getMainPopupBodyId(),
                createStringResource("RelationshipSelectConnectorStepPanel.select.required")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("RelationshipSelectConnectorStepPanel.select.title");
            }

            @Override
            protected @NotNull Label initLabel(IModel<String> messageModel) {
                Label label = super.initLabel(messageModel);
                label.add(AttributeModifier.append("class", "alert alert-warning"));
                return label;
            }

            @Override
            public int getHeight() {
                return 180;
            }
        };
        getPageBase().showMainPopup(popup, target);
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

    @Override
    protected List<HelpTab> computeHelpTabs() {
        return ConnectorDevelopmentWizardUtil.helpTabs(getDetailsModel(), ConnectorWizardHelpTopics.RELATIONSHIP_SELECTION, null);
    }
}
