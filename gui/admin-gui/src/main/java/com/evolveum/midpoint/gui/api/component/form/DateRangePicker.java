/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.form;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.model.Model;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DateRangePicker extends BasePanel<DateRange> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(DateRangePicker.class);

    private static final String ID_TEXT = "text";

    private IModel<DateRangePickerOptions> options;

    public DateRangePicker(String id, IModel<DateRange> model) {
        this(id, model, Model.of(new DateRangePickerOptions()));
    }

    public DateRangePicker(String id, IModel<DateRange> model, IModel<DateRangePickerOptions> options) {
        super(id, model);

        this.options = options;

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        checkComponentTag(tag, "div");
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "input-group"));

        TextField text = new TextField(ID_TEXT);
        text.setOutputMarkupId(true);
        add(text);
    }

    private TextField getText() {
        return (TextField) get(ID_TEXT);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        String options = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            options = mapper.writeValueAsString(this.options.getObject());
        } catch (Exception ex) {
            LOGGER.debug("Couldn't create toast", ex);
        }

        response.render(OnDomReadyHeaderItem.forScript("$(function() { $('#" + getText().getMarkupId() + "').daterangepicker(" + options + "); });"));
    }
}
