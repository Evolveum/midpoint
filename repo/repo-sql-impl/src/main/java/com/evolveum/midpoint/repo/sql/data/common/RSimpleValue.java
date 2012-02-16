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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

/**
 * @author lazyman
 */
public abstract class RSimpleValue<T> extends RValue {

    public abstract T getValue();

    public abstract void setValue(T object);

    @Override
    public Object toObject() throws DtoTranslationException {
        try {
            return XmlTypeConverter.toXsdElement(getValue(), getType(), getName(), DOMUtil.getDocument(), true);
        } catch (SchemaException ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    @Override
    public void insertValueFromElement(Element element) throws SchemaException {
        Validate.notNull(element, "Element must not be null.");

        setValue((T) XmlTypeConverter.toJavaValue(element));
    }
}
