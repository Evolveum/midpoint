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

package com.evolveum.midpoint.common.jaxb.impl;

import com.evolveum.midpoint.common.jaxb.NamespaceEnum;
import com.evolveum.midpoint.xml.util.XMLMarshaller;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import com.evolveum.midpoint.xml.common.Pair;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Node;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class XMLMarshallerImpl extends Pair<Marshaller, Unmarshaller> implements XMLMarshaller {

    public XMLMarshallerImpl() throws JAXBException {
        super();

        StringBuilder b = new StringBuilder();
        b.append(ObjectFactory.class.getPackage().getName()).append(":");

        JAXBContext jctx = JAXBContext.newInstance(b.toString());
        setFirst(jctx.createMarshaller());
        getFirst().setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        getFirst().setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        getFirst().setProperty("com.sun.xml.bind.namespacePrefixMapper", new Mapper());

        setSecond(jctx.createUnmarshaller());
    }

    @Override
    public String marshal(Object xmlObject) throws JAXBException {
        StringWriter writer = new StringWriter();
        if (xmlObject != null) {
            getFirst().marshal(xmlObject, writer);
        }

        return writer.toString();
    }

    @Override
    public void marshal(Object xmlObject, Node node) throws JAXBException {
        if (xmlObject != null) {
            getFirst().marshal(xmlObject, node);
        }

    }

    @Override
    public Object unmarshal(String xmlString) throws JAXBException {
        if (xmlString == null) {
            return null;
        }

        Object result = null;
        xmlString = xmlString.trim();
        if (xmlString.startsWith("<") && xmlString.endsWith(">")) {
            try {
                result = unmarshal(IOUtils.toInputStream(xmlString, "utf-8"));
            } catch (IOException ex) {
                throw new JAXBException(ex);
            }
        }
        return result;
    }

    @Override
    public Object unmarshal(File file) throws JAXBException {
        FileReader in = null;
        try {
            if (file == null) {
                return null;
            }
            Object result = null;
            in = new FileReader(file);
            result = getSecond().unmarshal(in);
            return result;
        } catch (FileNotFoundException ex) {
            throw new JAXBException(ex);
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
            } catch (IOException ex) {
                throw new JAXBException(ex);
            }
        }
    }

    @Override
    public Object unmarshal(InputStream input) throws JAXBException {
        return getSecond().unmarshal(input);
    }

    private static class Mapper extends NamespacePrefixMapper {

        @Override
        public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
            NamespaceEnum[] values = NamespaceEnum.values();
            for (NamespaceEnum namespaceEnum : values) {
                if (namespaceEnum.getNamespace().equals(namespaceUri)) {
                    return namespaceEnum.getPrefix();
                }
            }

            return suggestion;
        }
    }
}
