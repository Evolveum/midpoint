/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serial;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.DateLabelComponent;

/**
 * @author honchar
 */
public class DateIntervalSearchPanel extends PopoverSearchPanel {

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<XMLGregorianCalendar> fromDateModel;
    private final IModel<XMLGregorianCalendar> toDateModel;

    public DateIntervalSearchPanel(String id, IModel<XMLGregorianCalendar> fromDateModel, IModel<XMLGregorianCalendar> toDateModel) {
        super(id);
        this.fromDateModel = fromDateModel;
        this.toDateModel = toDateModel;
    }

    @Override
    protected PopoverSearchPopupPanel createPopupPopoverPanel() {
        return new DateIntervalSearchPopupPanel(PopoverSearchPanel.ID_POPOVER_PANEL, fromDateModel, toDateModel) {

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
    public IModel<String> getTextValue() {
        return () -> {
            StringBuilder sb = new StringBuilder();
            if (fromDateModel != null && fromDateModel.getObject() != null) {
                sb.append(WebComponentUtil.getLocalizedDate(fromDateModel.getObject(), DateLabelComponent.SHORT_SHORT_STYLE));
            }
            if (sb.length() > 0 && toDateModel != null && toDateModel.getObject() != null) {
                sb.append("-");
            }
            if (toDateModel != null && toDateModel.getObject() != null) {
                sb.append(WebComponentUtil.getLocalizedDate(toDateModel.getObject(), DateLabelComponent.SHORT_SHORT_STYLE));
            }
            return sb.toString();
        };
    }

    protected boolean isInterval() {
        return true;
    }

}
