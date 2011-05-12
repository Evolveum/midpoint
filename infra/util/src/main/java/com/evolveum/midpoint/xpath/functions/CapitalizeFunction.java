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

package com.evolveum.midpoint.xpath.functions;

import java.util.List;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 *
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class CapitalizeFunction implements XPathFunction {

    @Override
    public Object evaluate(List args) throws XPathFunctionException {
        if (null == args || args.size() != 1) {
            throw new XPathFunctionException("Wrong number of arguments");
        }

        Object argument = args.get(0);

        if (argument instanceof String) {
            return StringUtils.capitalize((String) argument);
        } else if (argument instanceof Node) {
            //NodeList nodes = (NodeList) argument;
            Node node = (Node)argument;
            return StringUtils.capitalize(node.getTextContent());
        } else {
            throw new XPathFunctionException("Wrong argument type, was " + argument.getClass().getName());
        }
    }
}
