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

package com.evolveum.midpoint.xpath;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;
import org.apache.commons.lang.Validate;

/**
 * 
 *
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class MidPointXPathFunctionResolver implements XPathFunctionResolver {

    Map<QName, XPathFunction>  map = new HashMap();

    @Override
    public XPathFunction resolveFunction(QName fname, int arity) {
        Validate.notNull("The function name cannot be null");
        Validate.isTrue(arity >= 0, "Provided negative value for function arity");

        return map.get(fname);
    }

    public void registerFunction(QName fname, XPathFunction function) {
        map.put(fname, function);
    }

}
