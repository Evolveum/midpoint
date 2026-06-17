/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;

/**
 * Panel that displays elapsed time based on provided start and optional end timestamps.
 *
 * <p>If the end timestamp is not provided, the panel shows continuously increasing
 * elapsed time (optionally auto-refreshed via Ajax). If the end timestamp is present,
 * it displays the final elapsed time along with the last refresh timestamp.</p>
 *
 * <p>Typical output examples:</p>
 * <ul>
 *     <li>Running: "Elapsed time: 1m 20s"</li>
 *     <li>Finished: "Last refresh: Mar 25, 2026, 10:45:00 AM • Elapsed time: 1m 43s"</li>
 * </ul>
 *
 * <p>The component supports optional auto-refresh to update the elapsed time dynamically.</p>
 */
public class TimerProgressPanel extends Panel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LABEL = "label";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm:ss a")
                    .withZone(ZoneId.systemDefault());

    private final IModel<XMLGregorianCalendar> startModel;
    private final @Nullable IModel<XMLGregorianCalendar> endModel;
    private final boolean autoRefreshEnabled;

    public TimerProgressPanel(String id, IModel<XMLGregorianCalendar> startModel) {
        this(id, startModel, null, false);
    }

    public TimerProgressPanel(String id, IModel<XMLGregorianCalendar> startModel, boolean autoRefreshEnabled) {
        this(id, startModel, null, autoRefreshEnabled);
    }

    public TimerProgressPanel(
            String id,
            IModel<XMLGregorianCalendar> startModel,
            @Nullable IModel<XMLGregorianCalendar> endModel) {
        this(id, startModel, endModel, false);
    }

    public TimerProgressPanel(
            String id,
            IModel<XMLGregorianCalendar> startModel,
            @Nullable IModel<XMLGregorianCalendar> endModel,
            boolean autoRefreshEnabled) {
        super(id);
        this.startModel = startModel;
        this.endModel = endModel;
        this.autoRefreshEnabled = autoRefreshEnabled;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        Label label = new Label(ID_LABEL, LoadableDetachableModel.of(this::createText));
        label.setOutputMarkupId(true);
        add(label);

        if (autoRefreshEnabled && isRunning()) {
            add(new AbstractAjaxTimerBehavior(Duration.ofSeconds(1)) {
                @Override
                protected void onTimer(AjaxRequestTarget target) {
                    if (!isRunning()) {
                        stop(target);
                    }
                    target.add(label);
                }
            });
        }
    }

    private boolean isRunning() {
        return startModel != null
                && startModel.getObject() != null
                && (endModel == null || endModel.getObject() == null);
    }

    private @NotNull String createText() {
        Instant start = toInstant(startModel != null ? startModel.getObject() : null);
        if (start == null) {
            return "";
        }

        XMLGregorianCalendar endCalendar = endModel != null ? endModel.getObject() : null;
        Instant end = toInstant(endCalendar);
        Instant effectiveEnd = end != null ? end : Instant.now();

        Duration duration = safeDurationBetween(start, effectiveEnd);
        String durationText = formatDuration(duration);

        if (end == null) {
            return translate("TimeProgressPanel.duration", durationText);
        }

        String date = formatTimestamp(endCalendar);
        return translate("TimeProgressPanel.lastRefreshAndDuration", new Object[] { date, durationText });
    }

    public static @Nullable String formatTimestamp(@Nullable XMLGregorianCalendar timestamp) {
        Instant instant = toInstant(timestamp);
        return instant != null ? DATE_TIME_FORMATTER.format(instant) : null;
    }

    private static @Nullable Instant toInstant(@Nullable XMLGregorianCalendar value) {
        return value != null ? value.toGregorianCalendar().toInstant() : null;
    }

    private static @NotNull Duration safeDurationBetween(@NotNull Instant start, @NotNull Instant end) {
        Duration duration = Duration.between(start, end);
        return duration.isNegative() ? Duration.ZERO : duration;
    }

    protected static @NotNull String formatDuration(@NotNull Duration duration) {
        long totalSeconds = duration.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return minutes + "m " + seconds + "s";
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        if (startModel != null) {
            startModel.detach();
        }
        if (endModel != null) {
            endModel.detach();
        }
    }
}
