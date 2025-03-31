/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serial;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.input.TextPanel;

/**
 * @author honchar
 */
public class DateIntervalSearchPanel extends PopoverSearchPanel {

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<XMLGregorianCalendar> fromDateModel;
    private final IModel<XMLGregorianCalendar> toDateModel;

    private final IModel<List<NamedIntervalPreset>> intervalPresets;

    private final IModel<NamedIntervalPreset> selectedIntervalPreset;

    public DateIntervalSearchPanel(
            String id,
            IModel<XMLGregorianCalendar> fromDateModel,
            IModel<XMLGregorianCalendar> toDateModel,
            IModel<List<NamedIntervalPreset>> intervalPresets,
            IModel<NamedIntervalPreset> selectedIntervalPreset) {

        super(id);
        this.fromDateModel = fromDateModel;
        this.toDateModel = toDateModel;
        this.intervalPresets = intervalPresets;
        this.selectedIntervalPreset = selectedIntervalPreset;
    }

    @Override
    protected PopoverSearchPopupPanel createPopupPopoverPanel(Popover popover) {
        return new DateIntervalSearchPopupPanel(
                PopoverSearchPanel.ID_POPOVER_PANEL, popover, fromDateModel, toDateModel, intervalPresets, selectedIntervalPreset) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void confirmPerformed(AjaxRequestTarget target) {
                target.add(DateIntervalSearchPanel.this);
            }

            @Override
            protected boolean isInterval() {
                return DateIntervalSearchPanel.this.isInterval();
            }

            @Override
            protected void removeSearchValue(AjaxRequestTarget target) {
                fromDateModel.setObject(null);
                toDateModel.setObject(null);
                target.add(this);
            }
        };
    }

    @Override
    public LoadableModel<String> getTextValue() {
        return new LoadableModel<>() {

            @Override
            protected String load() {
                if (selectedIntervalPreset != null) {
                    NamedIntervalPreset preset = selectedIntervalPreset.getObject();
                    if (preset != null) {
                        return LocalizationUtil.translateMessage(preset.text());
                    }
                }

                StringBuilder sb = new StringBuilder();
                if (fromDateModel != null && fromDateModel.getObject() != null) {
                    sb.append(WebComponentUtil.getLocalizedDate(fromDateModel.getObject(), DateLabelComponent.SHORT_SHORT_STYLE));
                }
                if (sb.length() > 0 && toDateModel != null && toDateModel.getObject() != null) {
                    sb.append(" - ");
                }
                if (toDateModel != null && toDateModel.getObject() != null) {
                    sb.append(WebComponentUtil.getLocalizedDate(toDateModel.getObject(), DateLabelComponent.SHORT_SHORT_STYLE));
                }
                return sb.toString();
            }
        };
    }

    protected boolean isInterval() {
        return true;
    }

    @Override
    protected TextPanel createTextPanel(String id, IModel model) {
        TextPanel panel = super.createTextPanel(id, model);
        panel.add(AttributeAppender.append("style", () -> isInterval() ? "width: 250px;" : null));

        return panel;
    }
}
