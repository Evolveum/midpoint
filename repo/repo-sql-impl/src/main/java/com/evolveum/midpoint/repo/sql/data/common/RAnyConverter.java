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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
class RAnyConverter {

    private static final Trace LOGGER = TraceManager.getTrace(RAnyConverter.class);
    private PrismContext prismContext;
    private Document document;

    RAnyConverter(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    Set<RValue> convertToValue(Item item) throws SchemaException {
        Validate.notNull(item, "Object for converting must not be null.");

        Set<RValue> rValues = new HashSet<RValue>();

        ItemDefinition definition = item.getDefinition();

        RValue rValue = null;
        List<PrismValue> values = item.getValues();
        for (PrismValue value : values) {
            if (value instanceof PrismContainerValue) {
                rValue = new RClobValue();
            } else if (value instanceof PrismPropertyValue) {
                rValue = new RStringValue();
                //todo other types
            }

            rValue.setName(definition.getName());
            rValue.setType(definition.getTypeName());
            rValues.add(rValue);
        }

        XmlTypeConverter converter = new XmlTypeConverter();


//        Object javaValue = converter.toJavaValue(element);
//        LOGGER.info(">>>>>> value {}", new Object[]{javaValue});

        return rValues;
    }

    void convertFromValue(RValue value, PrismContainerValue parent) {
        Validate.notNull(value, "Value for converting must not be null.");
        Validate.notNull(parent, "Parent prism container value must not be null.");
//        Element element = createElement(value.getName());
//        element.setTextContent(value.get);

    }

    private Element createElement(QName name) {
        if (document == null) {
            document = DOMUtil.getDocument();
        }

        return DOMUtil.createElement(document, name);
    }
}
