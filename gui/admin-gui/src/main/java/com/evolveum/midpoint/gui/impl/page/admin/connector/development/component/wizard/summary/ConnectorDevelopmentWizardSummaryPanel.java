/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardParentStep;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentController;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.ConnectorDevelopmentWizardUtil;

import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.NextStepsActionsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection.BaseUrlConnectorStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.connection.EndpointConnectorStepPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationTypeType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author lskublik
 */
@PanelType(name = "cdw-summary")
@PanelInstance(identifier = "cdw-summary",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.summary", icon = "fa fa-wrench"),
        containerPath = "empty")
public class ConnectorDevelopmentWizardSummaryPanel extends WizardStepPanel implements WizardParentStep {

    private static final String PANEL_TYPE = "cdw-summary";

    private static final String ID_READ_ONLY_PROGRESS_BAR = "readOnlyProgressBar";
    private static final String ID_READ_ONLY_FINISH = "readOnlyFinish";

    private static final String ID_STATUS_WIDGETS = "statusWidgets";
    private static final String ID_STATUS_WIDGET = "statusWidget";

    private static final String ID_NUMBER_OF_OBJECTS_WIDGETS = "numberOfObjectsWidgets";
    private static final String ID_NUMBER_OF_OBJECTS_WIDGET = "numberOfObjectsWidget";

    private static final String ID_CONNECTOR_ACTION = "connectorAction";

    private final ConnectorDevelopmentDetailsModel detailsModel;

    public ConnectorDevelopmentWizardSummaryPanel(ConnectorDevelopmentDetailsModel detailsModel) {
        this.detailsModel = detailsModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        IModel<List<ProgressBar>> progressBarModel = getProgressBarModel();
        ProgressBarPanel progressBar = new ProgressBarPanel(ID_READ_ONLY_PROGRESS_BAR, progressBarModel);
        progressBar.setOutputMarkupId(true);
        progressBar.add(new VisibleBehaviour(() -> !ConnectorDevelopmentWizardUtil.isInitObjectClassSearchAllOperationComplete(detailsModel)));
        add(progressBar);

        WebMarkupContainer readOnlyFinish = new WebMarkupContainer(ID_READ_ONLY_FINISH);
        readOnlyFinish.setOutputMarkupId(true);
        readOnlyFinish.add(new VisibleBehaviour(() -> ConnectorDevelopmentWizardUtil.isInitObjectClassSearchAllOperationComplete(detailsModel)));
        add(readOnlyFinish);

        ListView<ContainerWithStatusWidgetDto> containersWidgets = new ListView<>(ID_STATUS_WIDGETS, createStatusWidgetsModel()) {
            @Override
            protected void populateItem(ListItem<ContainerWithStatusWidgetDto> item) {
                ContainerWithStatusWidgetPanel widget = new ContainerWithStatusWidgetPanel(ID_STATUS_WIDGET, item.getModel());
                widget.setOutputMarkupId(true);
                item.add(widget);
            }
        };
        containersWidgets.setOutputMarkupId(true);
        add(containersWidgets);

        ListView<NumberOfObjectsWidgetDto> numberOfObjectsWidgets = new ListView<>(ID_NUMBER_OF_OBJECTS_WIDGETS, createNumberOfObjectsWidgetsModel()) {
            @Override
            protected void populateItem(ListItem<NumberOfObjectsWidgetDto> item) {
                NumberOfObjectsWidgetPanel widget = new NumberOfObjectsWidgetPanel(ID_NUMBER_OF_OBJECTS_WIDGET, item.getModel());
                widget.setOutputMarkupId(true);
                item.add(widget);
            }
        };
        numberOfObjectsWidgets.setOutputMarkupId(true);
        add(numberOfObjectsWidgets);

        NextStepsActionsPanel connectorActionPanel = new NextStepsActionsPanel(
                ID_CONNECTOR_ACTION, detailsModel, (ConnectorDevelopmentController) getWizard());
        connectorActionPanel.setOutputMarkupId(true);
        add(connectorActionPanel);
    }

    private IModel<List<NumberOfObjectsWidgetDto>> createNumberOfObjectsWidgetsModel() {
        return () -> {
            List<NumberOfObjectsWidgetDto> list =  new ArrayList<>();

            list.add(new NumberOfObjectsWidgetDto(
                    "fa fa-layer-group",
                    createStringResource("ConnectorDevelopmentWizardSummaryPanel.objectClasses"),
                    () -> detailsModel.getObjectType().getConnector().getObjectClass().size(),
                    createStringResource("ConnectorDevelopmentWizardSummaryPanel.objectClassesButton")) {

                @Override
                public void openListOfObjects(AjaxRequestTarget target) {
                    ConnectorDevelopmentController controller = (ConnectorDevelopmentController) getWizard();
                    controller.showObjectClassesPanel(target);
                }
            });

            list.add(new NumberOfObjectsWidgetDto(
                    "fa fa-sitemap",
                    createStringResource("ConnectorDevelopmentWizardSummaryPanel.relationship"),
                    () -> detailsModel.getObjectType().getConnector().getRelation().size(),
                    createStringResource("ConnectorDevelopmentWizardSummaryPanel.relationshipButton")) {

                @Override
                public void openListOfObjects(AjaxRequestTarget target) {
                    ConnectorDevelopmentController controller = (ConnectorDevelopmentController) getWizard();
                    controller.showRelationshipsPanel(target);
                }
            });

            return list;
        };
    }

    private IModel<List<ContainerWithStatusWidgetDto>> createStatusWidgetsModel() {
        return () -> {
            List<ContainerWithStatusWidgetDto> list =  new ArrayList<>();

            list.add(new ContainerWithStatusWidgetDto(
                    "fa fa-file-lines",
                    createStringResource("ConnectorDevelopmentWizardSummaryPanel.basicInformation"),
                    Model.of("bg-gray-200"),
                    this::getNameOfConnector,
                    createMapForBasicInformationWidgetModel()) {
                @Override
                public void editPerformed(AjaxRequestTarget target) {
                    ConnectorDevelopmentController controller = (ConnectorDevelopmentController) getWizard();
                    controller.editBasicInformation(target);
                }

                @Override
                public boolean isEditButtonVisible() {
                    return ConnectorDevelopmentWizardUtil.isBasicSettingsComplete(detailsModel.getObjectWrapper());
                }
            });

            list.add(new ContainerWithStatusWidgetDto(
                    "fa fa-shield-halved",
                    createStringResource("ConnectorDevelopmentWizardSummaryPanel.connection"),
                    () -> ConnectorDevelopmentWizardUtil.isConnectionComplete(detailsModel, null) ?
                            "bg-green-100 text-success" : "bg-red-100 text-danger",
                    () -> ConnectorDevelopmentWizardUtil.isConnectionComplete(detailsModel, null) ?
                            createStringResource("ConnectorDevelopmentWizardSummaryPanel.connected").getString() :
                            createStringResource("ConnectorDevelopmentWizardSummaryPanel.unconnected").getString(),
                    createMapForConnectionWidgetModel()) {
                @Override
                public void editPerformed(AjaxRequestTarget target) {
                    ConnectorDevelopmentController controller = (ConnectorDevelopmentController) getWizard();
                    controller.editConnection(target);
                }

                @Override
                public boolean isEditButtonVisible() {
                    return ConnectorDevelopmentWizardUtil.isConnectionComplete(detailsModel, null);
                }
            }.setStatusCssIcon(() -> ConnectorDevelopmentWizardUtil.isConnectionComplete(detailsModel, null) ?
                    "fa fa-circle-check" : "fa fa-circle-xmark"));

            list.add(new ContainerWithStatusWidgetDto(
                    "fa fa-book",
                    createStringResource("ConnectorDevelopmentWizardSummaryPanel.documentation"),
                    Model.of("bg-green-100 text-success"),
                    createStringResource("ConnectorDevelopmentWizardSummaryPanel.goodCoverage"),
                    createMapForDocumentationWidgetModel()) {
                @Override
                public void editPerformed(AjaxRequestTarget target) {
                    ConnectorDevelopmentController controller = (ConnectorDevelopmentController) getWizard();
                    controller.editDocumentation(target);
                }

                @Override
                public boolean isEditButtonVisible() {
                    return ConnectorDevelopmentWizardUtil.existContainerValue(
                            detailsModel.getObjectWrapper(), ConnectorDevelopmentType.F_DOCUMENTATION_SOURCE);
                }
            }.setStatusCssIcon(Model.of("fa fa-thumbs-up")));

            return list;
        };
    }

    private String getNameOfConnector() {
        PolyStringType name = detailsModel.getObjectType().getConnector().getDisplayName();
        if (name == null) {
            name = detailsModel.getObjectType().getApplication().getApplicationName();
        }
        return LocalizationUtil.translatePolyString(name);
    }

    private @NotNull IModel<NumericWidgetDetailsDto> createMapForDocumentationWidgetModel() {
        return new LoadableDetachableModel<>() {

            @Override
            protected NumericWidgetDetailsDto load() {
                Map<IModel<String>, IModel<Integer>> values = new LinkedHashMap<>();
                values.put(
                        createStringResource("ConnectorDevelopmentWizardSummaryPanel.files"),
                        Model.of(0)); //TODO fix it
                values.put(
                        createStringResource("ConnectorDevelopmentWizardSummaryPanel.links"),
                        () -> detailsModel.getObjectType().getDocumentationSource().stream()
                                .filter(documentationValue -> StringUtils.isNotEmpty(documentationValue.getUri()))
                                .toList()
                                .size());
                return new NumericWidgetDetailsDto(values);
            }
        };
    }

    private @NotNull IModel<StringValuesWidgetDetailsDto> createMapForConnectionWidgetModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected StringValuesWidgetDetailsDto load() {
                Map<IModel<String>, IModel<String>> values = new LinkedHashMap<>();
                values.put(
                        createStringResource("ConnectorDevelopmentWizardSummaryPanel.baseUrl"),
                        defineValueModel((String) ConnectorDevelopmentWizardUtil.getTestingResourcePropertyValue(
                                detailsModel, null, BaseUrlConnectorStepPanel.PROPERTY_ITEM_NAME)));
                values.put(
                        createStringResource("ConnectorDevelopmentWizardSummaryPanel.testEndpoint"),
                        defineValueModel((String) ConnectorDevelopmentWizardUtil.getTestingResourcePropertyValue(
                                detailsModel, null, EndpointConnectorStepPanel.PROPERTY_ITEM_NAME)));
                values.put(
                        createStringResource("ConnectorDevelopmentWizardSummaryPanel.authMethod"),
                        defineValueModel("Basic auth")); //TODO fix it
                return new StringValuesWidgetDetailsDto(values);
            }
        };
    }

    private @NotNull IModel<StringValuesWidgetDetailsDto> createMapForBasicInformationWidgetModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected StringValuesWidgetDetailsDto load() {
                Map<IModel<String>, IModel<String>> values = new LinkedHashMap<>();
                values.put(
                        createStringResource("ConnDevConnectorType.version"),
                        defineValueModel(detailsModel.getObjectType().getConnector().getVersion()));
                values.put(
                        createStringResource("ConnDevApplicationInfoType.integrationType"),
                        defineValueModel(detailsModel.getObjectType().getApplication().getIntegrationType()));
                values.put(
                        createStringResource("ConnDevApplicationInfoType.deploymentType"),
                        defineValueModel(detailsModel.getObjectType().getApplication().getDeploymentType()));
                return new StringValuesWidgetDetailsDto(values);
            }
        };
    }

    private IModel<String> defineValueModel(String value) {
        if (StringUtils.isEmpty(value)) {
            return createStringResource("ConnectorDevelopmentWizardSummaryPanel.unknown");
        }
        return Model.of(value);
    }

    private IModel<String> defineValueModel(Enum value) {
        if (value == null) {
            return createStringResource("ConnectorDevelopmentWizardSummaryPanel.unknown");
        }
        return createStringResource(value);
    }

    private @NotNull IModel<List<ProgressBar>> getProgressBarModel() {
        return () -> {
            if (!ConnectorDevelopmentWizardUtil.isBasicSettingsComplete(detailsModel.getObjectWrapper())) {
                return createProgressBar(0, "ConnectorDevelopmentWizardSummaryPanel.4toReadOnly");
            }

            if (!ConnectorDevelopmentWizardUtil.isConnectionComplete(detailsModel, null)) {
                return createProgressBar(25, "ConnectorDevelopmentWizardSummaryPanel.3toReadOnly");
            }

            if (!ConnectorDevelopmentWizardUtil.isInitObjectClassSchemaOperationComplete(detailsModel)) {
                return createProgressBar(50, "ConnectorDevelopmentWizardSummaryPanel.2toReadOnly");
            }

            if (!ConnectorDevelopmentWizardUtil.isInitObjectClassSearchAllOperationComplete(detailsModel)) {
                return createProgressBar(75, "ConnectorDevelopmentWizardSummaryPanel.1toReadOnly");
            }

            return createProgressBar(100, "ConnectorDevelopmentWizardSummaryPanel.0toReadOnly");
        };
    }

    private static @NotNull List<ProgressBar> createProgressBar(double value, String messageKey) {
        return List.of(
                new ProgressBar(
                        value,
                        ProgressBar.State.PRIMARY,
                        com.evolveum.midpoint.schema.util.LocalizationUtil.toLocalizableMessage(
                                com.evolveum.midpoint.schema.util.LocalizationUtil.createForKey(messageKey))));
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }
}
