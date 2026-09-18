/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.OperationLogCollapsedItem;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * The filterable list of {@link OperationLogEntry} rows shown in the log viewer drawer, see
 * {@link OperationCompoundLogPanel}.
 */
public class OperationLogListPanel extends BasePanel<OperationLogCollapsedItem> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_FILTER = "filter";
    private static final String ID_EXPORT_ALL = "exportAll";
    private static final String ID_EMPTY = "empty";
    private static final String ID_ENTRY = "entry";
    private static final String ID_LEVEL_BADGE = "levelBadge";
    private static final String ID_SOURCE = "source";
    private static final String ID_TIME = "time";
    private static final String ID_MESSAGE = "message";
    private static final String ID_DETAIL_LINK = "detailLink";

    private final WizardModelWithParentSteps wizardModel;
    private final OperationLogLevel selectedLevel;
    private final SerializableBiConsumer<OperationLogListAction, AjaxRequestTarget> onAction;

    public OperationLogListPanel(String id, IModel<OperationLogCollapsedItem> model, WizardModelWithParentSteps wizardModel,
            OperationLogLevel selectedLevel, SerializableBiConsumer<OperationLogListAction, AjaxRequestTarget> onAction) {
        super(id, model);
        this.wizardModel = wizardModel;
        this.selectedLevel = selectedLevel;
        this.onAction = onAction;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);

        add(createFilterToggle());
        add(createExportAllLink());
        add(createEmptyIndicator());
        add(createEntryList());
    }

    private @NotNull List<ProvidedLogEntry> getAllEntries() {
        List<ProvidedLogEntry> all = new ArrayList<>();
        for (OperationLogProvider provider : getModelObject().getProviders()) {
            for (OperationLogEntry entry : provider.getOperationLogEntries()) {
                all.add(new ProvidedLogEntry(provider, entry));
            }
        }
        return all;
    }

    private @NotNull List<ProvidedLogEntry> getFilteredEntries() {
        List<ProvidedLogEntry> all = getAllEntries();
        if (selectedLevel == null) {
            return all;
        }
        return all.stream().filter(providedEntry -> providedEntry.entry().getLevel() == selectedLevel).toList();
    }

    private @NotNull WebMarkupContainer createEmptyIndicator() {
        WebMarkupContainer empty = new WebMarkupContainer(ID_EMPTY);
        empty.add(new VisibleBehaviour(() -> getFilteredEntries().isEmpty()));
        return empty;
    }

    private @NotNull TogglePanel<OperationLogLevel> createFilterToggle() {
        TogglePanel<OperationLogLevel> toggle = new TogglePanel<>(ID_FILTER, this::buildToggleList) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<OperationLogLevel>> item) {
                onAction.accept(new OperationLogListAction.LevelSelected(item.getObject().getValue()), target);
            }

            @Override
            protected String getDefaultCssClass() {
                return "d-flex flex-row flex-wrap gap-2";
            }

            @Override
            protected String getButtonCssClass() {
                return "btn btn-sm rounded-pill border";
            }
        };
        toggle.setOutputMarkupId(true);
        return toggle;
    }

    private @NotNull List<Toggle<OperationLogLevel>> buildToggleList() {
        List<ProvidedLogEntry> all = getAllEntries();

        List<Toggle<OperationLogLevel>> list = new ArrayList<>();
        list.add(createToggle(null, "OperationLogPanel.filter.all", all.size()));
        for (OperationLogLevel level : OperationLogLevel.values()) {
            long count = all.stream().filter(providedEntry -> providedEntry.entry().getLevel() == level).count();
            list.add(createToggle(level, filterKey(level), count));
        }
        return list;
    }

    private @NotNull Toggle<OperationLogLevel> createToggle(OperationLogLevel level, String labelKey, long count) {
        Toggle<OperationLogLevel> toggle = new Toggle<>(null, getString(labelKey, count));
        toggle.setValue(level);
        toggle.setActive(Objects.equals(selectedLevel, level));
        return toggle;
    }

    private static @NotNull String filterKey(OperationLogLevel level) {
        return switch (level) {
            case TRACE -> "OperationLogPanel.filter.trace";
            case DEBUG -> "OperationLogPanel.filter.debug";
            case INFO -> "OperationLogPanel.filter.info";
            case WARN -> "OperationLogPanel.filter.warning";
            case ERROR -> "OperationLogPanel.filter.error";
        };
    }

    private @NotNull AjaxLink<Void> createExportAllLink() {
        AjaxDownloadBehaviorFromStream download = new AjaxDownloadBehaviorFromStream() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected InputStream getInputStream() {
                List<OperationLogEntry> entries = getAllEntries().stream().map(ProvidedLogEntry::entry).toList();
                return new ByteArrayInputStream(OperationLogJsonUtils.toJson(entries).getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public String getFileName() {
                String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
                return "connector-log-" + getActiveStepId() + "-" + timestamp + ".json";
            }
        };
        download.setContentType("application/json");

        AjaxLink<Void> link = new AjaxLink<>(ID_EXPORT_ALL) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                download.initiate(target);
            }
        };
        link.add(download);
        return link;
    }

    private @NotNull String getActiveStepId() {
        if (wizardModel != null && wizardModel.getActiveStep() != null
                && StringUtils.isNotEmpty(wizardModel.getActiveStep().getStepId())) {
            return wizardModel.getActiveStep().getStepId();
        }
        return "operationLog";
    }

    private @NotNull ListView<ProvidedLogEntry> createEntryList() {
        ListView<ProvidedLogEntry> list = new ListView<>(ID_ENTRY, this::getFilteredEntries) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ProvidedLogEntry> item) {
                OperationLogEntry entry = item.getModelObject().entry();

                item.add(createLevelBadge(entry));
                item.add(createSourceLabel(entry));
                item.add(createTimeLabel(entry));
                item.add(createMessageLabel(entry));
                item.add(createDetailLink(item.getModel()));
            }
        };
        list.setOutputMarkupId(true);
        return list;
    }

    private @NotNull Label createLevelBadge(OperationLogEntry entry) {
        Label levelBadge = new Label(ID_LEVEL_BADGE, createStringResource("OperationLogPanel.level." + entry.getLevel().name()));
        levelBadge.add(AttributeAppender.append("class", entry.getLevel().getCss()));
        return levelBadge;
    }

    private @NotNull Label createSourceLabel(OperationLogEntry entry) {
        return new Label(ID_SOURCE, entry.getSource());
    }

    private @NotNull Label createTimeLabel(OperationLogEntry entry) {
        return new Label(ID_TIME, formatTime(entry.getTimestamp()));
    }

    private @NotNull Label createMessageLabel(OperationLogEntry entry) {
        return new Label(ID_MESSAGE, entry.getMessage());
    }

    private @NotNull AjaxLink<Void> createDetailLink(IModel<ProvidedLogEntry> entryModel) {
        return new AjaxLink<>(ID_DETAIL_LINK) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onAction.accept(new OperationLogListAction.EntrySelected(entryModel.getObject()), target);
            }
        };
    }

    private static String formatTime(Instant timestamp) {
        if (timestamp == null) {
            return null;
        }
        return DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(timestamp);
    }
}
