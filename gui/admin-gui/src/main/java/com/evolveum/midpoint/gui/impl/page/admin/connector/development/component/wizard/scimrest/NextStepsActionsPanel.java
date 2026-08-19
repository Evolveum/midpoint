/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;

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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevExportConnectorResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevTestingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jetbrains.annotations.Nullable;

public class NextStepsActionsPanel extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(NextStepsActionsPanel.class);

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

    /**
     * This panel is created once and reused for the whole lifetime of the wizard (it lives inside the
     * cached summary step, see {@code AbstractWizardController#getSummaryPanel()}), so {@code onInitialize}
     * only runs on its first render and is not a reliable place to react to a just-finished export.
     * {@code onBeforeRender} runs on every render (including the AJAX response that switches back to this
     * panel), which is what we need here.
     *
     * If a connector export finished while the wizard was still on the waiting step, the result was
     * stashed on the details model (see {@link WaitingConnectorExportingStepPanel#onNextPerformed}),
     * because a download behavior attached to that step would have been invalidated by the step being
     * replaced by this panel in the same AJAX response. Now that this panel is (again) part of the
     * component tree, it is safe to attach the behavior and start the download.
     */
    @Override
    protected void onBeforeRender() {
        triggerPendingExportDownload();
        super.onBeforeRender();
    }

    private void triggerPendingExportDownload() {
        ConnDevExportConnectorResultType exportResult = detailsModel.getPendingExportResult();
        if (exportResult == null) {
            return;
        }
        detailsModel.setPendingExportResult(null);

        AjaxDownloadBehaviorFromStream downloadBehavior = new AjaxDownloadBehaviorFromStream() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected InputStream getInputStream() {
                try {
                    return detailsModel.getServiceLocator().getConnectorService().getExportedConnectorFileStream(
                            exportResult.getFileName(),
                            exportResult.getNodeRef().getOid(),
                            getPageBase().createSimpleTask("downloadExportedConnector"),
                            new OperationResult("downloadExportedConnector"));
                } catch (CommonException | IOException e) {
                    LOGGER.error("Couldn't download exported connector file", e);
                    return null;
                }
            }

            @Override
            public String getFileName() {
                return exportResult.getFileName();
            }
        };
        downloadBehavior.setContentType("application/java-archive");
        add(downloadBehavior);

        RequestCycle.get().find(AjaxRequestTarget.class)
                .ifPresent(downloadBehavior::initiate);
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
                    case EXPORT_CONNECTOR -> exportConnector(target);
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

    private void exportConnector(AjaxRequestTarget target) {
        controller.exportConnector(target);
    }

    public enum ConnectorAction implements TileEnum {

        CREATE_RESOURCE("fa fa-plus bg-teal-100 text-success",
                "ConnectorAction.CREATE_RESOURCE.description"),
        UPLOAD("fa-solid fa-gears bg-cyan-100 text-info",
                "ConnectorAction.UPLOAD.description"),
        EXPORT_CONNECTOR("fa-solid fa-download bg-purple-100 text-purple",
                "ConnectorAction.EXPORT_CONNECTOR.description"),
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
