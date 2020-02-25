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

/**
 * @author lazyman
 */
public class EnumPropertyColumn<T> extends PropertyColumn<T, String> {

    public EnumPropertyColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

    public EnumPropertyColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
        super(displayModel, sortProperty, propertyExpression);
    }

    @Override
    public IModel<Object> getDataModel(final IModel<T> rowModel) {
        return new LoadableModel<Object>(false) {

            @Override
            protected String load() {
                Enum<?> en = (Enum<?>) EnumPropertyColumn.super.getDataModel(rowModel).getObject();
                if (en == null) {
                    return null;
                }

                return translate(en);
            }
        };
    }

    protected String translate(Enum<?> en) {
        return en.name();
    }
}
