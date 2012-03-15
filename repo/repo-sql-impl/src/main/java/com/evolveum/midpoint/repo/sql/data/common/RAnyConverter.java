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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.w3c.dom.Element;

/**
 * @author lazyman
 */
class RAnyConverter {

    private static final Trace LOGGER = TraceManager.getTrace(RAnyConverter.class);
    private PrismContext prismContext;

    RAnyConverter(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    RValue convertToValue(Object object) throws SchemaException {
        if (!(object instanceof Element)) {
            throw new IllegalArgumentException("Can't convert to value.");
        }

        Element element = (Element) object;

//        XmlTypeConverter converter = new XmlTypeConverter();
//        Object javaValue = converter.toJavaValue(element);
//        LOGGER.info(">>>>>> value {}", new Object[]{javaValue});

        return null;
    }

    Object convertFromValue(RValue value) {
        return null;
    }
}
