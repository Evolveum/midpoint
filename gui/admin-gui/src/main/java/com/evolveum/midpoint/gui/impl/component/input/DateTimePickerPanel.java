/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.gui.impl.component.input.converter.DateConverter;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import com.evolveum.midpoint.web.model.XmlGregorianCalendarModel;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;

/**
 * Panel for Date. Panel use date time picker. We can use Model of types Date or XMLGregorianCalendar.
 */
public class DateTimePickerPanel extends InputPanel {

    private static final String ID_CONTAINER = "container";
    private static final String ID_INPUT = "input";
    private static final String ID_ICON_CONTAINER = "iconContainer";

    private final DateTimePickerOptions dateTimePickerOptions = DateTimePickerOptions.of();

    public static DateTimePickerPanel createByDateModel(String id, IModel<Date> model) {
        return new DateTimePickerPanel(id, model);
    }

    public static DateTimePickerPanel createByXMLGregorianCalendarModel(String id, IModel<XMLGregorianCalendar> model) {
        return new DateTimePickerPanel(id, new XmlGregorianCalendarModel(model));
    }

    private DateTimePickerPanel(String id, IModel<Date> model) {
        super(id);
        initLayout(model);
    }

    private void initLayout(IModel<Date> model) {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER) {
            @Override
            public void renderHead(IHeaderResponse response) {
                super.renderHead(response);
                response.render(
                        OnDomReadyHeaderItem.forScript(
                                "MidPointTheme.initDateTimePicker("
                                        + getMarkupId() + ", "
                                        + dateTimePickerOptions.toJsConfiguration() + ");"));
            }
        };
        container.setOutputMarkupId(true);
        add(container);

        final TextField<Date> input = new TextField<>(ID_INPUT, model) {
            @Override
            protected IConverter<?> createConverter(Class<?> clazz)
            {
                if (Date.class.isAssignableFrom(clazz))
                {
                    return new DateConverter(dateTimePickerOptions.getDateTimeFormat());
                }
                return null;
            }
        };
        input.setType(Date.class);
        input.setOutputMarkupId(true);
        input.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        container.add(input);

        WebMarkupContainer iconContainer = new WebMarkupContainer(ID_ICON_CONTAINER);
        iconContainer.setOutputMarkupId(true);
        iconContainer.add(AttributeAppender.append("class", () -> isEnabled() ? "" : "disabled"));
        container.add(iconContainer);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        getBaseFormComponent().add(AttributeAppender.append("data-td-target", getContainerId()));
        get(createComponentPath(ID_CONTAINER, ID_ICON_CONTAINER)).add(AttributeAppender.append("data-td-target", getContainerId()));
    }

    private String getContainerId() {
        return "#" + get(ID_CONTAINER).getMarkupId();
    }

    @SuppressWarnings("unchecked")
    @Override
    public TextField<Date> getBaseFormComponent() {
        return (TextField<Date>) get(createComponentPath(ID_CONTAINER, ID_INPUT));
    }

    @Override
    protected void onAfterRender() {
        super.onAfterRender();
    }
}
