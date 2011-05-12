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

package com.evolveum.midpoint.util;

import com.evolveum.midpoint.util.Variable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;

/**
 * 
 *
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class MapXPathVariableResolver implements XPathVariableResolver {

    private Map<QName, Object> variables = new HashMap<QName, Object>();

    public MapXPathVariableResolver() {
    }

    public MapXPathVariableResolver(Map<QName, Variable> variables) {
        Set<Entry<QName, Variable>> set = variables.entrySet();
        for (Entry<QName, Variable> entry : set) {
            this.variables.put(entry.getKey(), entry.getValue().getObject());
        }
    }

    public void addVariable(QName name, Object value) {
        variables.put(name, value);
    }

    @Override
    public Object resolveVariable(QName name) {
        Object retval = variables.get(name);
        return retval;
    }
}