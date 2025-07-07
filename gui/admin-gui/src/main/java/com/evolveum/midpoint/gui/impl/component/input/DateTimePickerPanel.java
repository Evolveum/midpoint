/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.converter.DateConverter;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import com.evolveum.midpoint.web.model.XmlGregorianCalendarModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Panel for Date. Panel use date time picker. We can use Model of types Date or XMLGregorianCalendar.
 */
public class DateTimePickerPanel extends InputPanel {

    private static final String ID_CONTAINER = "container";
    private static final String ID_INPUT = "input";
    private static final String ID_ICON_CONTAINER = "iconContainer";
    private final static String ID_PICKER_STATUS = "pickerStatus";
    private final static String INVALID_FIELD_CLASS = "is-invalid";

    private final DateTimePickerOptions dateTimePickerOptions = DateTimePickerOptions.of();

    private boolean hasModalParent = false;

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

    @Override
    protected void onInitialize() {
        super.onInitialize();
        hasModalParent = WebComponentUtil.hasPopupableParent(this);
    }

    private String getDateTimePickerInitScript() {
        String config = hasModalParent
                ? dateTimePickerOptions.toJsConfiguration(getPageBase().getMainPopup().getMarkupId())
                : dateTimePickerOptions.toJsConfiguration();
        String messageOpen = getString("DateTimePickerPanel.pickerOpened");
        String messageClose = getString("DateTimePickerPanel.pickerClosed");
        return String.format("MidPointTheme.initDateTimePicker(%s, %s, '%s', '%s', '%s');",
                getMarkupId(), config, ID_PICKER_STATUS, messageOpen, messageClose);
    }

    private void initLayout(IModel<Date> model) {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER) {
            @Override
            public void renderHead(IHeaderResponse response) {
                super.renderHead(response);
                response.render(OnDomReadyHeaderItem.forScript(getDateTimePickerInitScript()));
            }
        };
        container.setOutputMarkupId(true);
        add(container);

        Label pickerStatus = new Label(ID_PICKER_STATUS, Model.of(""));
        pickerStatus.setOutputMarkupId(true);
        container.add(pickerStatus);

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
        input.add(new AjaxFormComponentUpdatingBehavior("change") {
            private boolean wasError = false;
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (wasError) {
                    wasError = false;
                    target.add(container);
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                wasError = true;
                target.add(container);
            }
        });
        input.add(AttributeAppender.append("class", () -> input.hasErrorMessage() ? INVALID_FIELD_CLASS : ""));
        input.add(AttributeAppender.replace("title", () ->
                getParentPage().getString("DateTimePickerPanel.dateFormatHint",
                        dateTimePickerOptions.getDateTimeFormat().stream().map(d -> "'" + d + "'").collect(Collectors.joining(", ")))));
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
