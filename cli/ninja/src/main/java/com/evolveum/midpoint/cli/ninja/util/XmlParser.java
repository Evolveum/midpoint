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

package com.evolveum.midpoint.cli.ninja.util;

import com.evolveum.midpoint.cli.common.ToolsUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.codehaus.staxmate.dom.DOMConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Viliam Repan (lazyman)
 */
public class XmlParser {

    private DOMConverter domConverter = new DOMConverter();

    public void parse(InputStream is, XmlObjectHandler handler) {
        XMLStreamReader stream;
        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            stream = xmlInputFactory.createXMLStreamReader(is);

            int serial = 1;

            Map<String, String> nsMap = new HashMap<String, String>();

            int eventType = stream.nextTag();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                if (!stream.getName().equals(ToolsUtils.C_OBJECTS)) {
                    parseObject(stream, handler, serial, nsMap);
                    return;
                }

                for (int i = 0; i < stream.getNamespaceCount(); i++) {
                    nsMap.put(stream.getNamespacePrefix(i), stream.getNamespaceURI(i));
                }
            } else {
                throw new XMLStreamException("StAX problem, shouldn't happen.");
            }

            while (stream.hasNext()) {
                eventType = stream.next();
                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    if (!parseObject(stream, handler, serial, nsMap)) {
                        break;
                    }
                    serial++;
                }
            }
        } catch (XMLStreamException ex) {
            //todo error handling
            ex.printStackTrace();
        }
    }

    private boolean parseObject(XMLStreamReader stream, XmlObjectHandler handler, int serial,
                                Map<String, String> nsMap) {
        XmlObjectMetadata metadata = new XmlObjectMetadata();
        metadata.setSerialNumber(serial);
        metadata.setStartLine(stream.getLocation().getLineNumber());

        com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType object = null;
        try {
            Document objectDoc = domConverter.buildDocument(stream);

            Element objectElement = ToolsUtils.getFirstChildElement(objectDoc);
            ToolsUtils.setNamespaceDeclarations(objectElement, nsMap);

            Unmarshaller unmarshaller = ToolsUtils.JAXB_CONTEXT.createUnmarshaller();
            Object obj = unmarshaller.unmarshal(objectElement);
            if (!(obj instanceof com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType)) {
                throw new XmlParserException("Xml object '" + obj.getClass().getName()
                        + "' doesn't represent 'ObjectType'");
            }
            object = (ObjectType) obj;
        } catch (JAXBException | XMLStreamException | XmlParserException ex) {
            metadata.setException(ex);
        }
        metadata.setEndLine(stream.getLocation().getLineNumber());

        return handler.handleObject(object, metadata);
    }
}
