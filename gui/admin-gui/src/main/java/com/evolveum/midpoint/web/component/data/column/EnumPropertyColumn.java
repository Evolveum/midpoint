/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
                Enum en = (Enum) EnumPropertyColumn.super.getDataModel(rowModel).getObject();
                if (en == null) {
                    return null;
                }

                return translate(en);
            }
        };
    }

    protected String translate(Enum en) {
        return en.name();
    }
}
