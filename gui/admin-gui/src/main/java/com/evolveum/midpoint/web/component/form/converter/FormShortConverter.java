/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.component.form.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Vilo Repan
 */
@FacesConverter(value = "formShortConverter")
public class FormShortConverter extends FormIntegerConverter {

    public static final String CONVERTER_ID = "formShortConverter";

    public FormShortConverter() {
        super();
        init(Integer.valueOf(Short.MIN_VALUE), Integer.valueOf(Short.MAX_VALUE));
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        Integer number = (Integer) super.getAsObject(context, component, value);
        if (number == null) {
            return null;
        }

        return Short.valueOf(number.shortValue());
    }
}
