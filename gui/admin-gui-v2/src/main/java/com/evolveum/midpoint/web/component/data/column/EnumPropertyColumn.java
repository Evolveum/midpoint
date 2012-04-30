/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.util.LoadableModel;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class EnumPropertyColumn<T> extends PropertyColumn<T> {

    public EnumPropertyColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

    public EnumPropertyColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
        super(displayModel, sortProperty, propertyExpression);
    }

    @Override
    protected IModel<?> createLabelModel(final IModel<T> rowModel) {
        return new LoadableModel<Object>(false) {

            @Override
            protected Object load() {
                Enum en = (Enum) EnumPropertyColumn.super.createLabelModel(rowModel).getObject();
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
