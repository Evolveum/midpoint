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

package com.evolveum.midpoint.common.jaxb;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DOMUtil;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.util.XMLMarshaller;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.File;
import java.io.InputStream;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class JAXBUtil {

    private static final transient Trace logger = TraceManager.getTrace(JAXBUtil.class);
    public static final String code_id = "$Id$";
    private static XMLMarshallerPool _marshallerPool = new XMLMarshallerPool();

    public static final ObjectType clone(ObjectType object) throws JAXBException {
        if (object == null) {
            return null;
        }
        ObjectFactory of = new ObjectFactory();
        JAXBElement<ObjectType> obj = of.createObject(object);
        obj = (JAXBElement<ObjectType>) JAXBUtil.unmarshal(JAXBUtil.marshal(obj));

        return obj.getValue();
    }

    public static final String marshal(Object xmlObject) throws JAXBException {
        XMLMarshaller m = _marshallerPool.checkout();
        String result = null;
        try {
            result = m.marshal(xmlObject);
        } finally {
            _marshallerPool.checkin(m);
        }
        return result;
    }

    public static final <T> String marshalWrap(T jaxbObject, QName elementQName) throws JAXBException {
        JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(), jaxbObject);
        return marshal(jaxbElement);
    }

    public static final String silentMarshal(Object xmlObject) {
        try {
            return marshal(xmlObject);
        } catch (JAXBException ex) {
            logger.debug("Failed to marshal object {}", xmlObject, ex);
            return null;
        }
    }

    public static final <T> String silentMarshalWrap(T jaxbObject, QName elementQName) {
        try {
            JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(), jaxbObject);
            return marshal(jaxbElement);
        } catch (JAXBException ex) {
            logger.debug("Failed to marshal object {}", jaxbObject, ex);
            return null;
        }
    }

    public static final void marshal(Object xmlObject, Element element) throws JAXBException {
        XMLMarshaller m = _marshallerPool.checkout();
        try {
            m.marshal(xmlObject, element);
        } finally {
            _marshallerPool.checkin(m);
        }
    }

    public static final void silentMarshal(Object xmlObject, Element element) {
        try {
            marshal(xmlObject, element);
        } catch (JAXBException ex) {
            logger.debug("Failed to marshal object {}", xmlObject, ex);
        }
    }

    public static final Object unmarshal(String xmlString) throws JAXBException {
        Object result = null;
        XMLMarshaller m = _marshallerPool.checkout();
        try {
            result = m.unmarshal(xmlString);
        } finally {
            _marshallerPool.checkin(m);
        }
        return result;
    }

    public static final Object unmarshal(InputStream input) throws JAXBException {
        Object result = null;
        XMLMarshaller m = _marshallerPool.checkout();
        try {
            result = m.unmarshal(input);
        } finally {
            _marshallerPool.checkin(m);
        }
        return result;
    }

    public static final Object silentUnmarshal(String xmlString) {
        try {
            return unmarshal(xmlString);
        } catch (JAXBException ex) {
            logger.debug("Failed to unmarshal xml string {}", xmlString, ex);
            return null;
        }
    }

    public static final Object silentUnmarshal(File file) {
        try {
            return unmarshal(file);
        } catch (JAXBException ex) {
            logger.debug("Failed to unmarshal file {}", file, ex);
            return null;
        }

    }

    public static final Object unmarshal(File file) throws JAXBException {
        Object result = null;
        XMLMarshaller m = _marshallerPool.checkout();
        try {
            result = m.unmarshal(file);
        } finally {
            _marshallerPool.checkin(m);
        }
        return result;
    }

    public static final <T> Element jaxbToDom(T jaxbObject, QName elementQName, Document doc) throws JAXBException {
        if (doc == null) {
            doc = DOMUtil.getDocument();
        }

        JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(), jaxbObject);
        Element element = doc.createElementNS(elementQName.getNamespaceURI(), elementQName.getLocalPart());

        marshal(jaxbElement, element);

        return (Element) element.getFirstChild();
    }

    public static <T> Element objectTypeToDom(T jaxbObject, Document doc) throws JAXBException {
        if (doc == null) {
            doc = DOMUtil.getDocument();
        }
        QName qname = SchemaConstants.getElementByObjectType(jaxbObject.getClass());
        if (qname == null) {
            throw new IllegalArgumentException("Cannot find element for class "+jaxbObject.getClass());
        }
        return jaxbToDom(jaxbObject,qname,doc);
    }
}
