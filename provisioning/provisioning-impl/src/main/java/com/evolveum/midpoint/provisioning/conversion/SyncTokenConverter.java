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

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.common.Pair;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Vilo Repan
 */
public class SyncTokenConverter implements Converter {

    private static final String PREFIX = "xsd";
    private static final Map<QName, Class> map = new HashMap<QName, Class>();
    private static final Collection types;

    static {
        map.put(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string"), String.class);
        map.put(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "integer"), Integer.class);
        map.put(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "double"), Double.class);
        map.put(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "boolean"), Boolean.class);
        map.put(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "float"), Float.class);
        map.put(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "long"), Long.class);
        map.put(new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "short"), Short.class);

        types = map.values();
    }

    //TODO: WHAT TO IMPLEMENT HERE????
    @Override
    public QName getXmlType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Class> getJavaTypes() {
        return types;
    }

    @Override
    public Object convertToJava(Node node) {
        Validate.notNull(node, "Couldn't convert null node.");
        
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            throw new IllegalArgumentException("Node '" + node.getLocalName() + "' is not element.");
        }
        Element element = (Element) node;
        String type = element.getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type");
        QName qname = createQName(element, type);

        Class clazz = map.get(qname);
        if (clazz == null) {
            throw new IllegalArgumentException("Can't convert node of type '" + type + "', unsupported.");
        }
        try {
            Constructor constructor = clazz.getConstructor(String.class);
            return constructor.newInstance(element.getTextContent());
        } catch (Exception ex) {
            throw new UnsupportedOperationException("Couldn't convert node '" + node.getLocalName() +
                    "', reason: couldn't create instance of class '" + clazz.getName() +
                    "', reason: " + ex.getMessage());
        }
    }

    @Override
    public Node convertToXML(QName name, Object o) {
        Validate.notNull(o, "Couldn't convert. Object can't be null");
        Validate.notNull(name, "Couldn't convert. QName can't be null.");
        
        Document document = DOMUtil.getDocument();
        Element syncToken = null;
        if (name.getNamespaceURI() != null && name.getPrefix() != null) {
            syncToken = document.createElementNS(name.getNamespaceURI(), name.getPrefix() + ":" + name.getLocalPart());
        } else {
            syncToken = document.createElement(name.getLocalPart());
        }

        Pair<String, String> pair = convertObject(o);
        if (pair == null) {
            throw new IllegalArgumentException("Couldn't transform object '" + o + "', unknown type.");
        }
        syncToken.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:type", pair.getFirst());
        syncToken.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + PREFIX, XMLConstants.W3C_XML_SCHEMA_NS_URI);
        syncToken.setTextContent(pair.getSecond());

        return syncToken;
    }

    private QName createQName(Element element, String type) {
        String namespace = null;
        String localName = null;

        int index = type.indexOf(":");
        if (index == -1) {
            namespace = element.getNamespaceURI();
            localName = type;
        } else {
            namespace = element.lookupNamespaceURI(type.substring(0, index));
            localName = type.substring(index + 1);
        }

        return new QName(namespace, localName);
    }

    private Pair<String, String> convertObject(Object o) {
        Set<Map.Entry<QName, Class>> set = map.entrySet();
        for (Map.Entry<QName, Class> entry : set) {
            if (entry.getValue().equals(o.getClass())) {
                return new Pair(PREFIX + ":" + entry.getKey().getLocalPart(), o.toString());
            }
        }

        return null;
    }
}
