/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.input.DateTimePickerPanel;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.util.DateValidator;

public class DateIntervalSearchPopupPanel extends PopoverSearchPopupPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_DATE_FROM_LABEL = "fromLabel";
    private static final String ID_DATE_FROM_VALUE_CONTAINER = "fromValueContainer";
    private static final String ID_DATE_FROM_VALUE = "dateFromValue";
    private static final String ID_DATE_TO_VALUE = "dateToValue";
    private static final String ID_DATE_TO_CONTAINER = "toDateContainer";

    private IModel<XMLGregorianCalendar> fromDateModel;
    private IModel<XMLGregorianCalendar> toDateModel;

    public DateIntervalSearchPopupPanel(String id, Popover popover, IModel<XMLGregorianCalendar> fromDateModel, IModel<XMLGregorianCalendar> toDateModel) {
        super(id, popover);
        this.fromDateModel = fromDateModel;
        this.toDateModel = toDateModel;
    }

    @Override
    protected void customizationPopoverForm(MidpointForm popoverForm) {
        DateValidator validator = WebComponentUtil.getRangeValidator(popoverForm, SchemaConstants.PATH_ACTIVATION);

        WebMarkupContainer fromLabel = new WebMarkupContainer(ID_DATE_FROM_LABEL);
        fromLabel.add(new VisibleBehaviour(() -> isInterval()));
        popoverForm.add(fromLabel);

        WebMarkupContainer fromValueContainer = new WebMarkupContainer(ID_DATE_FROM_VALUE_CONTAINER);
        fromValueContainer.add(AttributeAppender.append("class", () -> {
            if (isInterval()) {
                return "col-10";
            }
            return "col-12";
        }));
        popoverForm.add(fromValueContainer);
        DateTimePickerPanel fromDatePanel = DateTimePickerPanel.createByXMLGregorianCalendarModel(ID_DATE_FROM_VALUE, fromDateModel);
        fromDatePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        fromDatePanel.getBaseFormComponent().add(AttributeAppender.append("aria-label", LocalizationUtil.translate("UserReportConfigPanel.dateFrom")));
        fromValueContainer.add(fromDatePanel);
        validator.setDateFrom(fromDatePanel.getBaseFormComponent());

        WebMarkupContainer toContainer = new WebMarkupContainer(ID_DATE_TO_CONTAINER);
        toContainer.add(new VisibleBehaviour(() -> isInterval()));
        popoverForm.add(toContainer);
        DateTimePickerPanel toDatePanel = DateTimePickerPanel.createByXMLGregorianCalendarModel(ID_DATE_TO_VALUE, toDateModel);
        toDatePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        toDatePanel.getBaseFormComponent().add(AttributeAppender.append("aria-label", LocalizationUtil.translate("UserReportConfigPanel.dateTo")));
        toContainer.add(toDatePanel);
        validator.setDateFrom(toDatePanel.getBaseFormComponent());
    }

    protected boolean isInterval() {
        return true;
    }
}
