/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.tile.EnumTileChoicePanel;
import com.evolveum.midpoint.gui.impl.duplication.DuplicationProcessHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentController;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevTestingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.jetbrains.annotations.Nullable;

public class NextStepsActionsPanel extends BasePanel {

    private static final String ID_CONNECTOR_ACTION = "connectorAction";

    private final ConnectorDevelopmentDetailsModel detailsModel;
    private final ConnectorDevelopmentController controller;

    public NextStepsActionsPanel(String id, ConnectorDevelopmentDetailsModel detailsModel, ConnectorDevelopmentController controller) {
        super(id);
        this.detailsModel = detailsModel;
        this.controller = controller;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayer();
    }

    private void initLayer() {
        EnumTileChoicePanel<ConnectorAction> connectorActionPanel = new EnumTileChoicePanel<>(ID_CONNECTOR_ACTION, ConnectorAction.class) {
            @Override
            protected String getDescriptionForTile(ConnectorAction type) {
                return getString(type.getDescription());
            }

            @Override
            protected void onTemplateChosePerformed(ConnectorAction action, AjaxRequestTarget target) {
                switch (action) {
                    case NEW_OBJECT_CLASS -> createNewObjectClass(target);
                    case ADD_RELATIONSHIP -> createNewRelationship(target);
                    case CREATE_RESOURCE -> {
                        ResourceCreationPopup popup = new ResourceCreationPopup(getPageBase().getMainPopupBodyId()) {
                            @Override
                            protected void createNewResource(boolean useConfiguration) {
                                try {
                                    PrismReferenceWrapper<Referencable> resourceRef = detailsModel.getObjectWrapper().findReference(
                                            ItemPath.create(ConnectorDevelopmentType.F_TESTING, ConnDevTestingType.F_TESTING_RESOURCE));
                                    @Nullable PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(resourceRef.getValue().getRealValue(), getPageBase());
                                    if (resource != null) {
                                        PrismObject<ResourceType> newResource = DuplicationProcessHelper.duplicateObjectDefault(resource);
                                        newResource.findOrCreateProperty(ResourceType.F_NAME).setRealValue(null);
                                        newResource.findOrCreateContainer(ResourceType.F_SCHEMA).clear();
                                        newResource.findOrCreateProperty(ResourceType.F_LIFECYCLE_STATE).setRealValue(SchemaConstants.LIFECYCLE_PROPOSED);
                                        if (!useConfiguration) {
                                            ItemPath path = ItemPath.create("connectorConfiguration");
                                            newResource.findOrCreateContainer(path).clear();
                                        }
                                        DetailsPageUtil.dispatchToObjectDetailsPage(newResource, true, true, getPageBase());
                                    }
                                } catch (SchemaException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        };
                        getPageBase().showMainPopup(popup, target);
                    }
                }
            }
        };
        connectorActionPanel.setOutputMarkupId(true);
        add(connectorActionPanel);
    }

    private void createNewRelationship(AjaxRequestTarget target) {
        controller.initNewRelationship(target);
    }

    private void createNewObjectClass(AjaxRequestTarget target) {
        controller.initNewObjectClass(target);
    }

    public enum ConnectorAction implements TileEnum {

        CREATE_RESOURCE("fa fa-plus bg-teal-100 text-success",
                "ConnectorAction.CREATE_RESOURCE.description"),
        UPLOAD("fa-solid fa-gears bg-cyan-100 text-info",
                "ConnectorAction.UPLOAD.description"),
        NEW_OBJECT_CLASS("fa-solid fa-shapes bg-orange-100 text-warning",
                "ConnectorAction.NEW_OBJECT_CLASS.description"),
        ADD_RELATIONSHIP("fa fa-code-compare bg-pink-100 text-pink",
                "ConnectorAction.ADD_RELATIONSHIP.description");

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
