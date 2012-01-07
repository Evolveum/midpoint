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

package com.evolveum.midpoint.schema.processorFake;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * Only till schema will be divided into multiple parts
 *
 * @author lazyman
 */
@Deprecated
public class MidpointObject extends PropertyContainer {

    private Map<QName, Object> values = new HashMap<QName, Object>();

    public MidpointObject(QName qname) {
        super(qname);
    }

    public Object getValue(QName key) {
        return values.get(key);
    }

    public void setValue(QName key, Object value) {
        values.put(key, value);
    }
}
