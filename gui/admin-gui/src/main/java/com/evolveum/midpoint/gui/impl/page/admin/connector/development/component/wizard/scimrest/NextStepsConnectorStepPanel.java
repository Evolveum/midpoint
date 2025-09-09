/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.util.List;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.impl.component.tile.EnumTileChoicePanel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator.WizardModelWithParentSteps;
import com.evolveum.midpoint.gui.impl.component.wizard.connectorgenerator.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass.ObjectClassConnectorStepPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-next-steps")
@PanelInstance(identifier = "cdw-next-steps",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.nextSteps", icon = "fa fa-wrench"),
        containerPath = "empty")
public class NextStepsConnectorStepPanel extends AbstractWizardStepPanel<ConnectorDevelopmentDetailsModel> implements WizardParentStep {

    private static final String PANEL_TYPE = "cdw-next-steps";

    private static final String ID_OBJECT_CLASS = "objectClass";
    private static final String ID_OBJECT_CLASS_NAME = "objectClassName";
    private static final String ID_OBJECT_CLASS_POSSIBILITIES = "objectClassPossibilities";
    private static final String ID_CONNECTOR_ACTION = "connectorAction";

    private LoadableModel<List<PrismContainerValueWrapper<ConnDevObjectClassInfoType>>> valuesModel;

    public NextStepsConnectorStepPanel(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper) {
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
                try {
                    PrismContainerWrapper<ConnDevObjectClassInfoType> container = getDetailsModel().getObjectWrapper().findContainer(
                            ItemPath.create(ConnectorDevelopmentType.F_CONNECTOR, ConnDevSchemaType.F_OBJECT_CLASS));

                    return container.getValues();
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private void initLayout() {
        add(AttributeAppender.replace("class", "col-12"));
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        ListView<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassPanel = new ListView<>(ID_OBJECT_CLASS, valuesModel) {
            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> listItem) {
                Label name = new Label(ID_OBJECT_CLASS_NAME, () -> listItem.getModelObject().getRealValue().getName());
                name.setOutputMarkupId(true);
                listItem.add(name);

                EnumTileChoicePanel<ObjectClassOperation> objectClassPanel = new EnumTileChoicePanel<>(ID_OBJECT_CLASS_POSSIBILITIES, ObjectClassOperation.class) {
                    @Override
                    protected String getDescriptionForTile(ObjectClassOperation type) {
                        return getString(type.getDescription());
                    }

                    @Override
                    protected void onTemplateChosePerformed(ObjectClassOperation view, AjaxRequestTarget target) {

                    }
                };
                objectClassPanel.setOutputMarkupId(true);
                listItem.add(objectClassPanel);
            }
        };
        objectClassPanel.setOutputMarkupId(true);
        add(objectClassPanel);

        EnumTileChoicePanel<ConnectorAction> connectorActionPanel = new EnumTileChoicePanel<>(ID_CONNECTOR_ACTION, ConnectorAction.class) {
            @Override
            protected String getDescriptionForTile(ConnectorAction type) {
                return getString(type.getDescription());
            }

            @Override
            protected void onTemplateChosePerformed(ConnectorAction action, AjaxRequestTarget target) {
                if (action == ConnectorAction.NEW_OBJECT_CLASS) {
                    createNewObjectClass(target);
                }
            }
        };
        connectorActionPanel.setOutputMarkupId(true);
        add(connectorActionPanel);
    }

    private void createNewObjectClass(AjaxRequestTarget target) {
        ObjectClassConnectorStepPanel step = new ObjectClassConnectorStepPanel(getHelper());
        WizardModel wizardModel = getWizard();
        wizardModel.addStepAfter(step, ObjectClassConnectorStepPanel.class);
        if (wizardModel instanceof WizardModelWithParentSteps wizardModelWithParentSteps) {
            wizardModelWithParentSteps.setActiveChildStepById(ObjectClassConnectorStepPanel.PANEL_TYPE);
        } else {
            wizardModel.setActiveStepById(ObjectClassConnectorStepPanel.PANEL_TYPE);
        }
        wizardModel.fireActiveStepChanged();
        target.add(getWizard().getPanel());
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageConnectorDevelopment.wizard.step.nextSteps");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.nextSteps.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageConnectorDevelopment.wizard.step.nextSteps.subText");
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

    public enum ObjectClassOperation implements TileEnum {

        SCHEMA(GuiStyleConstants.CLASS_ICON_RESOURCE_SCHEMA + " text-secondary bg-gray-100",
                "ObjectClassOperations.SCHEMA.description"),
        SEARCH("fa fa-search text-secondary bg-gray-100",
                "ObjectClassOperations.SEARCH.description"),
        CREATE("fa fa-circle-plus text-secondary bg-gray-100",
                "ObjectClassOperations.CREATE.description"),
        MODIFY("fa fa-pen-to-square text-secondary bg-gray-100",
                "ObjectClassOperations.MODIFY.description"),
        DELETE("fa fa-trash-can text-secondary bg-gray-100",
                "ObjectClassOperations.DELETE.description");

        private final String icon;
        private final String descriptionKey;

        ObjectClassOperation(String icon, String descriptionKey) {
            this.icon = icon;
            this.descriptionKey = descriptionKey;
        }

        @Override
        public String getIcon() {
            return icon;
        }

        @Override
        public String getDescription() {
            return descriptionKey;
        }
    }

    public enum ConnectorAction implements TileEnum {

        UPLOAD("fa-solid fa-gears bg-cyan-100 text-info",
                "ConnectorAction.UPLOAD.description"),
        NEW_OBJECT_CLASS("fa-solid fa-shapes bg-orange-100 text-warning",
                "ConnectorAction.NEW_OBJECT_CLASS.description"),
        ADD_ASSOCIATION("fa fa-code-compare bg-pink-100 text-pink",
                "ConnectorAction.ADD_ASSOCIATION.description");

        private final String icon;
        private final String descriptionKey;

        ConnectorAction(String icon, String descriptionKey) {
            this.icon = icon;
            this.descriptionKey = descriptionKey;
        }

        @Override
        public String getIcon() {
            return icon;
        }

        @Override
        public String getDescription() {
            return descriptionKey;
        }
    }
}
