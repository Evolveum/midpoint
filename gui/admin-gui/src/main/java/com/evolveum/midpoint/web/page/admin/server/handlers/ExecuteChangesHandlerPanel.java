/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.web.page.admin.server.handlers.dto.ExecuteChangesHandlerDto;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class ExecuteChangesHandlerPanel extends QueryBasedHandlerPanel<ExecuteChangesHandlerDto> {
    private static final long serialVersionUID = 1L;

    private static final String ID_CHANGE_CONTAINER = "changeContainer";
    private static final String ID_CHANGE = "change";
    private static final String ID_OPTIONS_CONTAINER = "optionsContainer";
    private static final String ID_OPTIONS = "options";

    public ExecuteChangesHandlerPanel(String id, IModel<ExecuteChangesHandlerDto> model) {
        super(id, model);
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {
        WebMarkupContainer changeContainer = new WebMarkupContainer(ID_CHANGE_CONTAINER);
        TextArea change = new TextArea<>(ID_CHANGE, new PropertyModel<>(getModel(), ExecuteChangesHandlerDto.F_OBJECT_DELTA_XML));
        change.setEnabled(false);
        changeContainer.add(change);
        add(changeContainer);

        WebMarkupContainer optionsContainer = new WebMarkupContainer(ID_OPTIONS_CONTAINER);
        TextArea options = new TextArea<>(ID_OPTIONS, new PropertyModel<>(getModel(), ExecuteChangesHandlerDto.F_OPTIONS));
        options.setEnabled(false);
        optionsContainer.add(options);
        add(optionsContainer);
    }

}
