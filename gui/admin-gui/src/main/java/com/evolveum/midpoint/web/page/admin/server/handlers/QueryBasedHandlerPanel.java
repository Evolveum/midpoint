/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.QueryBasedHandlerDto;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class QueryBasedHandlerPanel<D extends QueryBasedHandlerDto> extends BasePanel<D> {
    private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_TYPE_CONTAINER = "objectTypeContainer";
    private static final String ID_OBJECT_TYPE = "objectType";
    private static final String ID_OBJECT_QUERY_CONTAINER = "objectQueryContainer";
    private static final String ID_OBJECT_QUERY = "objectQuery";

    public QueryBasedHandlerPanel(String id, IModel<D> model) {
        super(id, model);
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {
        WebMarkupContainer objectTypeContainer = new WebMarkupContainer(ID_OBJECT_TYPE_CONTAINER);
        Label objectType = new Label(ID_OBJECT_TYPE, new IModel<String>() {
            @Override
            public String getObject() {
//                final String key = getModelObject().getObjectTypeKey();
//                return key != null ? getString(key) : null;
                return null;
            }
        });
        objectTypeContainer.add(objectType);
        add(objectTypeContainer);

        WebMarkupContainer objectQueryContainer = new WebMarkupContainer(ID_OBJECT_QUERY_CONTAINER);
        TextArea objectQuery = new TextArea<>(ID_OBJECT_QUERY, new PropertyModel<>(getModel(), QueryBasedHandlerDto.F_OBJECT_QUERY));
        objectQuery.setEnabled(false);
        objectQueryContainer.add(objectQuery);
        add(objectQueryContainer);
    }

}
