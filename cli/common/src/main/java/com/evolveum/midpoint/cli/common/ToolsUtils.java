/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.cli.common;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.sun.org.apache.xml.internal.utils.XMLChar;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * @author lazyman
 */
public class ToolsUtils {

    public static final String DESCRIBE_PROPERTIES = "/describe.properties";

    public static final String PROP_VERSION = "version";

    public static final String LOGGER_SYS_OUT = "SYSOUT";
    public static final String LOGGER_SYS_ERR = "SYSERR";

    public static final QName C_OBJECTS = new QName(ModelClientUtil.NS_COMMON, "objects");
    public static final QName C_OBJECT = new QName(ModelClientUtil.NS_COMMON, "object");

    public static final JAXBContext JAXB_CONTEXT;

    static {
        try {
            JAXB_CONTEXT = ModelClientUtil.instantiateJaxbContext();
        } catch (JAXBException ex) {
            throw new IllegalStateException("Couldn't create jaxb context", ex);
        }
    }

    public static Element getFirstChildElement(Node parent) {
        if (parent == null || parent.getChildNodes() == null) {
            return null;
        }

        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node child = nodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) child;
            }
        }

        return null;
    }

    public static void serializeObject(Object object, Writer writer) throws JAXBException {
        if (object == null) {
            return;
        }

        Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);

        if (object instanceof ObjectType) {
            object = new JAXBElement(C_OBJECT, Object.class, object);
        }

        marshaller.marshal(object, writer);
    }

    public static String serializeObject(Object object) throws JAXBException {
        if (object == null) {
            return null;
        }

        Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        StringWriter sw = new StringWriter();
        marshaller.marshal(object, sw);
        return sw.toString();
    }

    public static void setNamespaceDeclarations(Element element, Map<String, String> rootNamespaceDeclarations) {
        for (Map.Entry<String, String> entry : rootNamespaceDeclarations.entrySet()) {
            setNamespaceDeclaration(element, entry.getKey(), entry.getValue());
        }
    }

    public static void setNamespaceDeclaration(Element element, String prefix, String namespaceUri) {
        Document doc = element.getOwnerDocument();
        NamedNodeMap attributes = element.getAttributes();
        Attr attr;
        if (prefix == null || prefix.isEmpty()) {
            // default namespace
            attr = doc.createAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE);
        } else {
            attr = doc.createAttributeNS(
                    XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix);
        }
        checkValidXmlChars(namespaceUri);
        attr.setValue(namespaceUri);
        attributes.setNamedItem(attr);
    }

    public static void checkValidXmlChars(String stringValue) {
        if (stringValue == null) {
            return;
        }
        for (int i = 0; i < stringValue.length(); i++) {
            if (!XMLChar.isValid(stringValue.charAt(i))) {
                throw new IllegalStateException("Invalid character with regards to XML (code "
                        + ((int) stringValue.charAt(i)) + ") in '"
                        + makeSafelyPrintable(stringValue, 200) + "'");
            }
        }
    }

    private static String makeSafelyPrintable(String text, int maxSize) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!XMLChar.isValid(c)) {
                sb.append('.');
            } else if (Character.isWhitespace(c)) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
            if (i == maxSize) {
                sb.append("...");
                break;
            }
        }
        return sb.toString();
    }

    public static String loadVersion() throws IOException {
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = ToolsUtils.class.getResourceAsStream(DESCRIBE_PROPERTIES);
            props.load(new InputStreamReader(is, "utf-8"));
        } finally {
            IOUtils.closeQuietly(is);
        }

        return props.getProperty(PROP_VERSION);
    }
}
