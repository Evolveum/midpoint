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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.tabs.IconPanelTab;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxTabbedPanel;
import com.evolveum.midpoint.web.component.util.SerializableConsumer;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Detail view for one {@link OperationLogEntry} shown in the log viewer drawer, see
 * {@link OperationCompoundLogPanel}.
 */
public class OperationLogEventDetailPanel extends BasePanel<ProvidedLogEntry> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_DETAILS_FRAGMENT = "detailsFragment";
    private static final String ID_PROTOCOL_UNAVAILABLE_FRAGMENT = "protocolUnavailableFragment";

    private static final String ID_BACK = "back";
    private static final String ID_EXPORT_ENTRY = "exportEntry";
    private static final String ID_FIELD_ROW = "fieldRow";
    private static final String ID_FIELD_LABEL = "fieldLabel";
    private static final String ID_FIELD_VALUE = "fieldValue";
    private static final String ID_TABS = "tabs";

    private static final String ID_DETAILS_MESSAGE = "detailsMessage";
    private static final String ID_STACKTRACE_LABEL = "stacktraceLabel";
    private static final String ID_STACKTRACE = "stacktrace";

    private static final String ID_PROTOCOL_UNAVAILABLE_TEXT = "protocolUnavailableText";

    private final SerializableConsumer<AjaxRequestTarget> onBack;

    public OperationLogEventDetailPanel(String id, IModel<ProvidedLogEntry> model, SerializableConsumer<AjaxRequestTarget> onBack) {
        super(id, model);
        this.onBack = onBack;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);

        add(createBackLink());
        add(createExportEntryLink());
        add(createFieldRows());
        add(createTabsPanel());
    }

    private @NotNull AjaxLink<Void> createBackLink() {
        return new AjaxLink<>(ID_BACK) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onBack.accept(target);
            }
        };
    }

    private @NotNull AjaxLink<Void> createExportEntryLink() {
        OperationLogEntry entry = getModelObject().entry();

        AjaxDownloadBehaviorFromStream download = new AjaxDownloadBehaviorFromStream() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected InputStream getInputStream() {
                return new ByteArrayInputStream(OperationLogJsonUtils.toJson(entry).getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public String getFileName() {
                return "connector-log-" + entry.getTraceId() + "-" + entry.getTimestamp().toEpochMilli() + ".json";
            }
        };
        download.setContentType("application/json");

        AjaxLink<Void> link = new AjaxLink<>(ID_EXPORT_ENTRY) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                download.initiate(target);
            }
        };
        link.add(download);
        return link;
    }

    private @NotNull ListView<Map.Entry<String, String>> createFieldRows() {
        OperationLogEntry entry = getModelObject().entry();
        return new ListView<>(ID_FIELD_ROW, () -> List.copyOf(getEventFields(entry).entrySet())) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Map.Entry<String, String>> item) {
                Map.Entry<String, String> row = item.getModelObject();
                item.add(createFieldLabel(row));
                item.add(createFieldValue(row));
            }
        };
    }

    private @NotNull Label createFieldLabel(Map.Entry<String, String> row) {
        return new Label(ID_FIELD_LABEL, createStringResource(row.getKey()));
    }

    private @NotNull Label createFieldValue(Map.Entry<String, String> row) {
        return new Label(ID_FIELD_VALUE, row.getValue());
    }

    private @NotNull Map<String, String> getEventFields(OperationLogEntry entry) {
        Map<String, String> map = new LinkedHashMap<>();
        addIfNotEmpty(map, "OperationLogPanel.field.timestamp", formatFullTimestamp(entry.getTimestamp()));
        addIfNotEmpty(map, "OperationLogPanel.field.severity", getString("OperationLogPanel.level." + entry.getLevel().name()));
        addIfNotEmpty(map, "OperationLogPanel.field.threadId", entry.getThreadName());
        addIfNotEmpty(map, "OperationLogPanel.field.class", entry.getFullyQualifiedClass());
        addIfNotEmpty(map, "OperationLogPanel.field.method", formatMethod(entry));
        return map;
    }

    private static void addIfNotEmpty(Map<String, String> map, String key, String value) {
        if (StringUtils.isNotEmpty(value)) {
            map.put(key, value);
        }
    }

    private static String formatMethod(OperationLogEntry entry) {
        if (entry.getMethod() != null && entry.getSourceFile() != null && entry.getLineNumber() != null) {
            return entry.getMethod() + "(" + entry.getSourceFile() + ":" + entry.getLineNumber() + ")";
        }
        return null;
    }

    private static String formatFullTimestamp(Instant timestamp) {
        if (timestamp == null) {
            return null;
        }
        return DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss").withZone(ZoneId.systemDefault()).format(timestamp);
    }

    private @NotNull AjaxTabbedPanel<ITab> createTabsPanel() {
        return new AjaxTabbedPanel<>(ID_TABS, buildTabs());
    }

    private @NotNull List<ITab> buildTabs() {
        OperationLogEntry entry = getModelObject().entry();
        OperationLogProvider provider = getModelObject().provider();

        List<ITab> tabs = new ArrayList<>();
        tabs.add(createDetailsTab(entry));

        if (entry.getProtocol() != null) {
            tabs.add(createProtocolTab(entry));
        } else if (provider != null && provider.isDebugModeEnabled()) {
            tabs.add(createProtocolUnavailableTab());
        }

        return tabs;
    }

    private @NotNull IconPanelTab createDetailsTab(OperationLogEntry entry) {
        return new IconPanelTab(createStringResource("OperationLogPanel.tab.details")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createDetailsTabContent(panelId, entry);
            }
        };
    }

    private @NotNull IconPanelTab createProtocolTab(OperationLogEntry entry) {
        return new IconPanelTab(createStringResource("OperationLogPanel.tab.protocol")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createProtocolTabContent(panelId, entry.getProtocol());
            }
        };
    }

    private @NotNull IconPanelTab createProtocolUnavailableTab() {
        return new IconPanelTab(createStringResource("OperationLogPanel.tab.protocol")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createProtocolUnavailableTabContent(panelId);
            }
        };
    }

    private @NotNull Fragment createDetailsTabContent(String id, OperationLogEntry entry) {
        Fragment fragment = new Fragment(id, ID_DETAILS_FRAGMENT, this);

        fragment.add(createDetailsMessageLabel(entry));
        fragment.add(createStacktraceLabel(entry));
        fragment.add(createStacktraceValue(entry));

        return fragment;
    }

    private @NotNull Label createDetailsMessageLabel(OperationLogEntry entry) {
        return new Label(ID_DETAILS_MESSAGE, entry.getMessage());
    }

    private @NotNull Label createStacktraceLabel(OperationLogEntry entry) {
        Label label = new Label(ID_STACKTRACE_LABEL, createStringResource("OperationLogPanel.field.stacktrace"));
        label.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(entry.getStacktrace())));
        return label;
    }

    private @NotNull Label createStacktraceValue(OperationLogEntry entry) {
        Label label = new Label(ID_STACKTRACE, entry.getStacktrace());
        label.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(entry.getStacktrace())));
        return label;
    }

    private @NotNull Fragment createProtocolUnavailableTabContent(String id) {
        Fragment fragment = new Fragment(id, ID_PROTOCOL_UNAVAILABLE_FRAGMENT, this);
        fragment.add(new Label(ID_PROTOCOL_UNAVAILABLE_TEXT, createStringResource("OperationLogPanel.protocol.developmentModeOnly")));
        return fragment;
    }

    /**
     * Resolves the "Protocol" tab content for one {@link OperationLogProtocol} implementation, mirroring
     * {@code ContainerWithStatusWidgetPanel.getDetails()}. {@link OperationLogProtocol} is intentionally not
     * sealed, so an unrecognized implementation falls back to an empty container instead of throwing.
     */
    private static @NotNull WebMarkupContainer createProtocolTabContent(String id, OperationLogProtocol protocol) {
        if (protocol instanceof SqlProtocol sqlProtocol) {
            return new SqlProtocolPanel(id, Model.of(sqlProtocol));
        } else if (protocol instanceof HttpProtocol httpProtocol) {
            return new HttpProtocolPanel(id, Model.of(httpProtocol));
        } else {
            return new WebMarkupContainer(id);
        }
    }
}
