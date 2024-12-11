/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import java.io.Serial;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class DeadlinePanel extends BasePanel<XMLGregorianCalendar> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_DEADLINE_ICON = "deadlineIcon";
    private static final String ID_DEADLINE_LABEL = "deadlineLabel";

    private static final int UNDEFINED_DEADLINE_DISTANCE_VALUE = -1;

    public enum DeadlineDistance {
        NORMAL(null, "text-success", "far fa-clock lh-n"),
        CLOSE_IN_TIME(10, "text-warning", "fa fa-warning lh-n"),
        CRITICALLY_CLOSE_IN_TIME(3, "text-danger", "fa fa-exclamation-circle lh-n"),
        PAST_OR_UNDEFINED(UNDEFINED_DEADLINE_DISTANCE_VALUE, "text-primary", "lh-n");

        private final Integer maxDayDistanceFromDeadline;
        private final String cssStyle;
        private final String iconCss;

        DeadlineDistance(Integer maxDayDistanceFromDeadline, String cssStyle, String iconCss) {
            this.maxDayDistanceFromDeadline = maxDayDistanceFromDeadline;
            this.cssStyle = cssStyle;
            this.iconCss = iconCss;
        }
    }

    public enum DeadlineLabelFormat {
        IN_DAYS_TEXT_FORMAT("DeadlinePanel.deadlineInDay",
                "DeadlinePanel.deadlineInDays"),        // e.g. "in 3 days", can be used when Deadline label precedes DeadlinePanel
        DAYS_REMAINING_TEXT_FORMAT("DeadlinePanel.dayRemaining",
                "DeadlinePanel.daysRemaining"), // e.g. "3 days remaining", clear message for separate DeadlinePanel usage
        FINISHED("DeadlinePanel.finishedOn", "DeadlinePanel.finishedOn"), // e.g. "finished on ..."
        DATE_FORMAT(null, null);                // e.g. usual date format

        private final String messageKeyForOneDay;
        private final String messageKeyForMoreDays;

        DeadlineLabelFormat(String messageKeyForOneDay, String messageKeyForMoreDays) {
            this.messageKeyForOneDay = messageKeyForOneDay;
            this.messageKeyForMoreDays = messageKeyForMoreDays;
        }
    }

    public DeadlinePanel(String id, IModel<XMLGregorianCalendar> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        IModel<String> deadlineLabelCssModel = createDeadlineLabelCssModel();
        String iconCss = getDeadlineIconCss();
        String styledIconCss = StringUtils.isNotEmpty(iconCss) ? iconCss + " " + deadlineLabelCssModel.getObject() : "";

        WebComponent deadlineIcon = new WebComponent(ID_DEADLINE_ICON);
        deadlineIcon.add(AttributeModifier.append("class", styledIconCss));
        deadlineIcon.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(iconCss)));
        add(deadlineIcon);

        Label deadlineLabel = new Label(ID_DEADLINE_LABEL, createLabelModel());
        deadlineLabel.add(AttributeModifier.append("class", deadlineLabelCssModel));
        add(deadlineLabel);
    }

    private IModel<String> createLabelModel() {
        int daysToDeadline = getDaysToDeadline();
        if (daysToDeadline < 0) {
            if (getModelObject() == null) {
                return Model.of("");
            } else {
                String formatedDate = WebComponentUtil.getLocalizedDate(getModelObject(), DateLabelComponent.SHORT_NOTIME_STYLE);
                return createStringResource(DeadlineLabelFormat.FINISHED.messageKeyForOneDay, formatedDate);
            }
        }

        String labelKey;
        var deadlineLabelFormat = getDeadlineLabelFormat();
        if (daysToDeadline == 1) {
            labelKey = deadlineLabelFormat.messageKeyForOneDay;
        } else {
            labelKey = deadlineLabelFormat.messageKeyForMoreDays;
        }

        if (StringUtils.isEmpty(labelKey)) {
            return Model.of(WebComponentUtil.getShortDateTimeFormattedValue(getModelObject(), getPageBase()));
        }
        return createStringResource(labelKey, daysToDeadline);
    }

    private IModel<String> createDeadlineLabelCssModel() {
        return new IModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return calculateDeadlineDistance().cssStyle;
            }
        };
    }

    private String getDeadlineIconCss() {
        return calculateDeadlineDistance().iconCss;
    }

    private DeadlineDistance calculateDeadlineDistance() {
        int daysToDeadline = getDaysToDeadline();
        if (daysToDeadline < 0) {
            return DeadlineDistance.PAST_OR_UNDEFINED;
        }
        if (daysToDeadline <= DeadlineDistance.CRITICALLY_CLOSE_IN_TIME.maxDayDistanceFromDeadline) {
            return DeadlineDistance.CRITICALLY_CLOSE_IN_TIME;
        } else if (daysToDeadline <= DeadlineDistance.CLOSE_IN_TIME.maxDayDistanceFromDeadline) {
            return DeadlineDistance.CLOSE_IN_TIME;
        }
        return DeadlineDistance.NORMAL;
    }

    private int getDaysToDeadline() {
        if (getModelObject() == null) {
            return UNDEFINED_DEADLINE_DISTANCE_VALUE;
        }
        XMLGregorianCalendar deadline = getModelObject();
        LocalDate deadlineLocalDate = LocalDate.of(
                deadline.getYear(),
                deadline.getMonth(),
                deadline.getDay());
        LocalDate now = LocalDate.now();

        return (int) ChronoUnit.DAYS.between(now, deadlineLocalDate);
    }

    protected DeadlineLabelFormat getDeadlineLabelFormat() {
        return DeadlineLabelFormat.IN_DAYS_TEXT_FORMAT;
    }

}
