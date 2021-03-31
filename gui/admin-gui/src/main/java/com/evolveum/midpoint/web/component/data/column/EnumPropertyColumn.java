/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
public class EnumPropertyColumn<T> extends PropertyColumn<T, String> {

    private String unknownKey;
    private boolean nullAsUnknown;

    public EnumPropertyColumn(IModel<String> displayModel, String propertyExpression) {
        this(displayModel, propertyExpression, null);
    }

    public EnumPropertyColumn(IModel<String> displayModel, String propertyExpression, String unknownKey) {
        super(displayModel, propertyExpression);
        this.unknownKey = unknownKey;
        this.nullAsUnknown = unknownKey != null;
    }

    @Override
    public IModel<Object> getDataModel(final IModel<T> rowModel) {
        return new LoadableModel<>(false) {

            @Override
            protected String load() {
                Enum<?> en = (Enum<?>) EnumPropertyColumn.super.getDataModel(rowModel).getObject();
                if (en == null && !nullAsUnknown) {
                    return null;
                }

                return translate(en);
            }
        };
    }

    protected String translate(Enum<?> en) {
        String resourceKey = en == null ? unknownKey : en.getDeclaringClass().getSimpleName() + "." + en.name();
        StringResourceModel resourceModel = new StringResourceModel(resourceKey);
        return resourceModel.getString();
    }
}
