/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.input.TextPanel;

/**
 * @author honchar
 */
public class DateIntervalSearchPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_DATE_TEXT_FIELD = "dateValueTextField";
    private static final String ID_SET_DATE_BUTTON = "setDateButton";
    private static final String ID_DATE_POPOVER_PANEL = "datePopoverPanel";
    private static final String ID_DATE_POPOVER_BODY = "datePopoverBody";
    private static final String ID_DATE_POPOVER = "datePopover";

    private IModel<XMLGregorianCalendar> fromDateModel;
    private IModel<XMLGregorianCalendar> toDateModel;

    public DateIntervalSearchPanel(String id, IModel<XMLGregorianCalendar> fromDateModel, IModel<XMLGregorianCalendar> toDateModel) {
        super(id);
        this.fromDateModel = fromDateModel;
        this.toDateModel = toDateModel;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        TextPanel<String> dateTextField = new TextPanel<String>(ID_DATE_TEXT_FIELD, this::getDateTextValue);
        dateTextField.setOutputMarkupId(true);
        dateTextField.add(AttributeAppender.append("title", this::getDateTextValue));
        dateTextField.setEnabled(false);
        add(dateTextField);

        AjaxButton setDateButton = new AjaxButton(ID_SET_DATE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                togglePopover(target, DateIntervalSearchPanel.this.get(ID_DATE_TEXT_FIELD),
                        DateIntervalSearchPanel.this.get(ID_DATE_POPOVER), 0);
            }
        };
        setDateButton.setOutputMarkupId(true);
        add(setDateButton);

        WebMarkupContainer popover = new WebMarkupContainer(ID_DATE_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        WebMarkupContainer popoverBody = new WebMarkupContainer(ID_DATE_POPOVER_BODY);
        popoverBody.setOutputMarkupId(true);
        popover.add(popoverBody);

        DateIntervalSearchPopupPanel dateIntervalSearchPopupPanel =
                new DateIntervalSearchPopupPanel(ID_DATE_POPOVER_PANEL, fromDateModel, toDateModel) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void confirmPerformed(AjaxRequestTarget target) {
                        target.add(DateIntervalSearchPanel.this);
                    }
                };
        dateIntervalSearchPopupPanel.setRenderBodyOnly(true);
        popoverBody.add(dateIntervalSearchPopupPanel);

    }

    public void togglePopover(AjaxRequestTarget target, Component button, Component popover, int paddingRight) {
        StringBuilder script = new StringBuilder();
        script.append("toggleSearchPopover('");
        script.append(button.getMarkupId()).append("','");
        script.append(popover.getMarkupId()).append("',");
        script.append(paddingRight).append(");");

        target.appendJavaScript(script.toString());
    }

    public String getDateTextValue() {
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
    }

}
