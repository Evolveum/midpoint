/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import java.util.List;

import com.evolveum.midpoint.web.component.prism.ValueStatus;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentController;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-relationships")
@PanelInstance(identifier = "cdw-relationships",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.relationships", icon = "fa fa-wrench"),
        containerPath = "empty")
public class RelationshipsConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> implements WizardParentStep {

    public static final String PANEL_TYPE = "cdw-relationships";

    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_TABLE = "table";

    private IModel<List<PrismContainerValueWrapper<ConnDevRelationInfoType>>> relationshipModel;

    public RelationshipsConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
        super(helper);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        createValuesModel();
        initLayout();
    }

    private void createValuesModel() {
        relationshipModel = new LoadableDetachableModel<>() {
            @Override
            protected List<PrismContainerValueWrapper<ConnDevRelationInfoType>> load() {
                PrismContainerWrapperModel<ConnectorDevelopmentType, ConnDevRelationInfoType> model = PrismContainerWrapperModel.fromContainerWrapper(
                        getDetailsModel().getObjectWrapperModel(),
                        ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevConnectorType.F_RELATION));

                if (model.getObject() == null) {
                    return List.of();
                }
                return model.getObject().getValues().stream()
                        .filter(value -> value.getStatus() != ValueStatus.ADDED
                                && value.getStatus() != ValueStatus.DELETED)
                        .toList();
            }
        };
    }

    private void initLayout() {
        add(AttributeAppender.replace("class", "col-12"));
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        AjaxLink<?> addObjectClassButton = new AjaxLink<>(ID_ADD_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                ConnectorDevelopmentController controller = (ConnectorDevelopmentController) getWizard();
                controller.initNewRelationship(target);
            }
        };
        addObjectClassButton.setOutputMarkupId(true);
        add(addObjectClassButton);

        TileTablePanel<ConnectorRelationshipTile, PrismContainerValueWrapper<ConnDevRelationInfoType>> table
                = new TileTablePanel<>(ID_TABLE) {
            @Override
            protected ISortableDataProvider createProvider() {
                return new MultivalueContainerListDataProvider<>(
                        this,
                        Model.of(),
                        relationshipModel) {

                    @Override
                    protected List<PrismContainerValueWrapper<ConnDevRelationInfoType>> searchThroughList() {
                        return super.searchThroughList();
                    }
                };
            }

            @Override
            protected ConnectorRelationshipTile createTileObject(
                    PrismContainerValueWrapper<ConnDevRelationInfoType> object) {
                ConnDevRelationInfoType value = object.getRealValue();
                return new ConnectorRelationshipTile(value.getName(), object)
                        .description(value.getShortDescription())
                        .setSubject(value.getSubject())
                        .setSubjectAttribute(value.getSubjectAttribute())
                        .setObject(value.getObject())
                        .setObjectAttribute(value.getObjectAttribute());
            }

            @Override
            protected Component createTile(String id, IModel<ConnectorRelationshipTile> model) {
                return new ConnectorRelationshipTilePanel(id, model) {

                    @Override
                    protected void configureRelationshipPerformed(AjaxRequestTarget target) {

                    }

                    @Override
                    protected void deleteRelationshipPerformed(AjaxRequestTarget target) {

                    }
                };
            }

            @Override
            protected String getTileCssClasses() {
                return "col-6";
            }

            @Override
            protected boolean showFooter() {
                return relationshipModel.getObject().size() > 6;
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.relationships");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.relationships.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.relationships.subText");
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
    public VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    public boolean isStatusStep() {
        return true;
    }

}
