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

package com.evolveum.midpoint.provisioning.conversion;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.provisioning.util.ShadowUtil;
import java.util.Arrays;
import java.util.Collection;
import javax.xml.namespace.QName;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import javax.xml.XMLConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author elek
 */
public class StringConverter implements Converter {

    private Collection<Class> supportedClasses;

    public StringConverter() {
        supportedClasses = Arrays.asList(new Class[]{String.class});
    }

    @Override
    public QName getXmlType() {
        return new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string");
    }

    @Override
    public Collection<Class> getJavaTypes() {
        return supportedClasses;
    }

    @Override
    public Object convertToJava(Node node) {
    	if (node==null) {
    		return null;
    	}
        return node.getTextContent();
    }

    @Override
    public Node convertToXML(QName qname, Object o) {
        Document d = ShadowUtil.getXmlDocument();
        Element e = d.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
        e.appendChild(d.createTextNode(Utils.cleanupUtf(o.toString())));
        return e;
    }
}
