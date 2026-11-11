/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.form.ToggleCheckBoxPanel;
import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.button.ReloadableButton;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevExportConnectorResultType;

/**
 * Popup exporting the developed connector in its current state as a package of the selected type to the selected destination.
 */
public class ExportConnectorPopup extends BasePanel<ConnectorExportOptions> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ExportConnectorPopup.class);

    private static final String CLASS_DOT = ExportConnectorPopup.class.getName() + ".";
    private static final String OP_EXPORT_CONNECTOR = CLASS_DOT + "exportConnector";
    private static final String OP_LOAD_EXPORT_STATUS = CLASS_DOT + "loadExportStatus";
    private static final String OP_DOWNLOAD_EXPORTED_CONNECTOR = CLASS_DOT + "downloadExportedConnector";

    private static final Duration STATUS_REFRESH_INTERVAL = Duration.ofSeconds(1);

    private static final String ID_TYPE = "type";
    private static final String ID_TYPES = "types";
    private static final String ID_TYPE_RADIO = "typeRadio";
    private static final String ID_TYPE_ICON = "typeIcon";
    private static final String ID_TYPE_LABEL = "typeLabel";
    private static final String ID_DESTINATION_SECTION = "destinationSection";
    private static final String ID_DESTINATION = "destination";
    private static final String ID_DESTINATIONS = "destinations";
    private static final String ID_DESTINATION_RADIO = "destinationRadio";
    private static final String ID_DESTINATION_LABEL = "destinationLabel";
    private static final String ID_DESTINATION_DESCRIPTION = "destinationDescription";
    private static final String ID_OPTIONAL_SECTION = "optionalSection";
    private static final String ID_OPTIONAL_TITLE = "optionalTitle";
    private static final String ID_CHANGE_VERSION = "changeVersion";
    private static final String ID_VERSION_SECTION = "versionSection";
    private static final String ID_VERSION = "version";
    private static final String ID_VERSION_CHANGE = "versionChange";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CANCEL = "cancel";
    private static final String ID_EXPORT = "export";

    private final ConnectorDevelopmentDetailsModel detailsModel;

    private Fragment footer;

    public ExportConnectorPopup(String id, IModel<ConnectorExportOptions> model, ConnectorDevelopmentDetailsModel detailsModel) {
        super(id, model);
        this.detailsModel = detailsModel;
    }

    @Override
    public int getWidth() {
        return 660;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("ExportConnectorPopup.title");
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);

        add(createTypeChoice());
        add(createDestinationSection());
        add(createOptionalSection());
        FeedbackAlerts feedback = createFeedbackPanel();
        add(feedback);

        footer = createFooter(feedback);
    }

    private Component createTypeChoice() {
        RadioGroup<ConnectorExportFormat> typeChoice = new RadioGroup<>(
                ID_TYPE, LambdaModel.of(getModel(), ConnectorExportOptions::getFormat, ConnectorExportOptions::setFormat));
        typeChoice.add(createFormatChangeBehavior());

        typeChoice.add(new ListView<>(ID_TYPES, Arrays.asList(ConnectorExportFormat.values())) {

            @Override
            protected void populateItem(ListItem<ConnectorExportFormat> item) {
                item.add(new Radio<>(ID_TYPE_RADIO, item.getModel()));

                WebMarkupContainer icon = new WebMarkupContainer(ID_TYPE_ICON);
                icon.add(AttributeAppender.append("class", item.getModelObject().getIcon()));
                item.add(icon);

                item.add(new Label(ID_TYPE_LABEL, () -> LocalizationUtil.translateEnum(item.getModelObject())));
            }
        });
        return typeChoice;
    }

    private Component createDestinationSection() {
        WebMarkupContainer section = new WebMarkupContainer(ID_DESTINATION_SECTION);
        section.add(new VisibleBehaviour(() -> getModelObject().getFormat() != null));

        RadioGroup<ConnectorExportDestination> destinationChoice = new RadioGroup<>(ID_DESTINATION,
                LambdaModel.of(getModel(), ConnectorExportOptions::getDestination, ConnectorExportOptions::setDestination));
        destinationChoice.add(createPopupRefreshBehavior());
        section.add(destinationChoice);

        destinationChoice.add(new ListView<>(ID_DESTINATIONS, Arrays.asList(ConnectorExportDestination.values())) {

            @Override
            protected void populateItem(ListItem<ConnectorExportDestination> item) {
                item.add(AttributeAppender.append("class", () -> isSupported(item.getModelObject()) ? null : "opacity-50"));

                Radio<ConnectorExportDestination> radio = new Radio<>(ID_DESTINATION_RADIO, item.getModel());
                radio.add(new EnableBehaviour(() -> isSupported(item.getModelObject())));
                item.add(radio);
                item.add(new Label(ID_DESTINATION_LABEL, () -> LocalizationUtil.translateEnum(item.getModelObject())));
                item.add(new Label(ID_DESTINATION_DESCRIPTION, createStringResource(item.getModelObject().getDescriptionKey())));
            }
        });
        return section;
    }

    private Component createOptionalSection() {
        WebMarkupContainer section = new WebMarkupContainer(ID_OPTIONAL_SECTION);
        section.add(new VisibleBehaviour(() -> getModelObject().getDestination() != null));

        WebMarkupContainer optionalTitle = new WebMarkupContainer(ID_OPTIONAL_TITLE);
        optionalTitle.add(new VisibleBehaviour(() -> !isSavedToConnectorDirectory()));
        section.add(optionalTitle);

        WebMarkupContainer versionSection = createVersionSection();

        ToggleCheckBoxPanel changeVersion = new ToggleCheckBoxPanel(ID_CHANGE_VERSION,
                LambdaModel.of(getModel(), ConnectorExportOptions::isVersionChangeEnabled, ConnectorExportOptions::setVersionChangeEnabled),
                createStringResource("ExportConnectorPopup.changeVersion"),
                Model.of()) {

            @Override
            protected @NotNull Map<String, Object> createSwitchOptions() {
                return Map.of("size", "sm");
            }

            @Override
            public String getContainerCssClass() {
                return super.getContainerCssClass() + " align-items-center flex-grow-1";
            }

            @Override
            protected void onToggle(AjaxRequestTarget target) {
                target.add(versionSection);
            }
        };
        changeVersion.add(new VisibleBehaviour(() -> !isSavedToConnectorDirectory()));
        section.add(changeVersion);
        section.add(versionSection);
        return section;
    }

    private WebMarkupContainer createVersionSection() {
        WebMarkupContainer versionSection = new WebMarkupContainer(ID_VERSION_SECTION);
        versionSection.setOutputMarkupPlaceholderTag(true);
        versionSection.add(new VisibleBehaviour(this::isNewVersionUsed));
        versionSection.add(AttributeAppender.append("class", () -> isSavedToConnectorDirectory() ? null : "border-top"));

        TextField<String> version = new TextField<>(
                ID_VERSION, LambdaModel.of(getModel(), ConnectorExportOptions::getNewVersion, ConnectorExportOptions::setNewVersion));
        version.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(versionSection, footer);
            }
        });
        versionSection.add(version);

        versionSection.add(new Label(ID_VERSION_CHANGE, () -> StringUtils.isNotBlank(getModelObject().getNewVersion())
                ? createStringResource("ExportConnectorPopup.versionChange", getCurrentVersion(), getModelObject().getNewVersion()).getString()
                : createStringResource("ExportConnectorPopup.versionChange.empty", getCurrentVersion()).getString()));
        return versionSection;
    }

    private FeedbackAlerts createFeedbackPanel() {
        FeedbackAlerts feedbackPanel = new FeedbackAlerts(ID_FEEDBACK);
        feedbackPanel.setOutputMarkupId(true);
        return feedbackPanel;
    }

    private Fragment createFooter(FeedbackAlerts feedback) {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.setOutputMarkupId(true);
        footer.add(createCancelButton());
        footer.add(createExportButton(feedback));
        return footer;
    }

    private AjaxButton createCancelButton() {
        return new AjaxButton(ID_CANCEL, createStringResource("Button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
    }

    private ReloadableButton createExportButton(FeedbackAlerts feedback) {
        ReloadableButton export = new ReloadableButton(ID_EXPORT, getPageBase(), createStringResource("ExportConnectorPopup.export")) {

            @Override
            protected String getCreatedTaskOid(AjaxRequestTarget target) {
                return submitExport(feedback, target);
            }

            @Override
            protected Duration getReloadInterval() {
                return STATUS_REFRESH_INTERVAL;
            }

            @Override
            protected void onTaskClosed(String taskOid, AjaxRequestTarget target) {
                exportTaskFinished(taskOid, feedback, target);
            }

            @Override
            protected void onTaskFailed(OperationResult taskResult, AjaxRequestTarget target) {
                exportTaskFinished(getRunningTaskOid(), feedback, target);
            }

            @Override
            protected void refresh(AjaxRequestTarget target) {
                target.add(this);
            }

            @Override
            protected String getIconCssClass() {
                return "fa fa-download";
            }

            @Override
            protected String getButtonCssClass() {
                return "btn btn-primary";
            }
        };
        export.add(new EnableBehaviour(this::isExportEnabled));
        return export;
    }

    private AjaxFormChoiceComponentUpdatingBehavior createPopupRefreshBehavior() {
        return new AjaxFormChoiceComponentUpdatingBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(ExportConnectorPopup.this, footer);
            }
        };
    }

    private AjaxFormChoiceComponentUpdatingBehavior createFormatChangeBehavior() {
        return new AjaxFormChoiceComponentUpdatingBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ConnectorExportDestination destination = getModelObject().getDestination();
                if (destination != null && !isSupported(destination)) {
                    getModelObject().setDestination(null);
                }
                target.add(ExportConnectorPopup.this, footer);
            }
        };
    }

    private boolean isSupported(ConnectorExportDestination destination) {
        return destination.supports(getModelObject().getFormat());
    }

    private boolean isExportEnabled() {
        return getModelObject().getFormat() != null
                && getModelObject().getDestination() != null
                && (!isSavedToConnectorDirectory() || StringUtils.isNotBlank(getModelObject().getNewVersion()));
    }

    private boolean isSavedToConnectorDirectory() {
        return getModelObject().getDestination() == ConnectorExportDestination.CONNECTOR_DIRECTORY;
    }

    private boolean isNewVersionUsed() {
        return getModelObject().isVersionChangeEnabled() || isSavedToConnectorDirectory();
    }

    private String submitExport(FeedbackAlerts feedback, AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OP_EXPORT_CONNECTOR);
        String version = isNewVersionUsed() ? StringUtils.trimToNull(getModelObject().getNewVersion()) : null;
        try {
            return switch (getModelObject().getFormat()) {
                case JAR -> detailsModel.getConnectorDevelopmentOperation()
                        .submitExportConnector(version, isSavedToConnectorDirectory(), task, task.getResult());
                case ZIP -> detailsModel.getConnectorDevelopmentOperation().submitUploadConnector(version, task, task.getResult());
            };
        } catch (RuntimeException e) {
            LOGGER.error("Couldn't submit connector export", e);
            exportFailed(null, feedback, target);
            return null;
        }
    }

    private void exportTaskFinished(String taskOid, FeedbackAlerts feedback, AjaxRequestTarget target) {
        StatusInfo<ConnDevExportConnectorResultType> status;
        try {
            status = loadExportStatus(taskOid);
        } catch (CommonException e) {
            LOGGER.error("Couldn't load status of connector export", e);
            exportFailed(null, feedback, target);
            return;
        }

        if (!status.isComplete()) {
            exportFailed(status.getMessage(), feedback, target);
            return;
        }

        getPageBase().hideMainPopup(target);
        if (isSavedToConnectorDirectory()) {
            showExportSuccess(getString("ExportConnectorPopup.savedToConnectorDirectory"), target);
        } else {
            downloadExportedConnector(status.getResult(), target);
            showExportSuccess(getString("ExportConnectorPopup.downloaded"), target);
        }
    }

    private void showExportSuccess(String message, AjaxRequestTarget target) {
        new Toast()
                .success()
                .title(message)
                .icon("fas fa-circle-check")
                .autohide(true)
                .delay(5_000)
                .show(target);
    }

    private void downloadExportedConnector(ConnDevExportConnectorResultType result, AjaxRequestTarget target) {
        PageBase page = getPageBase();
        AjaxDownloadBehaviorFromStream download = new AjaxDownloadBehaviorFromStream() {

            @Override
            protected InputStream getInputStream() {
                Task task = page.createSimpleTask(OP_DOWNLOAD_EXPORTED_CONNECTOR);
                try {
                    return page.getConnectorService().getExportedConnectorFileStream(
                            result.getFileName(), result.getNodeRef().getOid(), task, task.getResult());
                } catch (CommonException | IOException e) {
                    LOGGER.error("Couldn't download exported connector file", e);
                    return null;
                }
            }
        };
        download.setFileName(result.getFileName());
        download.setContentType(result.getContentType());
        page.add(download);
        download.initiate(target);
    }

    private StatusInfo<ConnDevExportConnectorResultType> loadExportStatus(String taskOid) throws CommonException {
        Task task = getPageBase().createSimpleTask(OP_LOAD_EXPORT_STATUS);
        return switch (getModelObject().getFormat()) {
            case JAR -> getPageBase().getConnectorService().getExportConnectorStatus(taskOid, task, task.getResult());
            case ZIP -> getPageBase().getConnectorService().getUploadConnectorStatus(taskOid, task, task.getResult());
        };
    }

    private void exportFailed(LocalizableMessage message, FeedbackAlerts feedback, AjaxRequestTarget target) {
        feedback.error(message != null
                ? LocalizationUtil.translateMessage(message)
                : getString("ExportConnectorPopup.exportFailed"));
        target.add(feedback);
    }

    private String getCurrentVersion() {
        return detailsModel.getObjectType().getConnector().getVersion();
    }
}
