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

import com.evolveum.midpoint.api.exceptions.MidPointException;
import com.evolveum.midpoint.provisioning.util.ShadowUtil;
import java.util.Arrays;
import java.util.Collection;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Convert from/to Jaxb POJO-s.
 *
 * @author elek
 */
public class JAXBConverter implements Converter {

    private Document d;

    private JAXBContext context;

    private Class type;

    private String namespace;

    public JAXBConverter(String namespace, Class type) {
        this.namespace = namespace;
        this.type = type;
        try {
            d = ShadowUtil.getXmlDocument();
            context = JAXBContext.newInstance(type, Container.class);
        } catch (JAXBException ex) {
            throw new MidPointException("Error on creating JAXBContext", ex);
        }
    }

    @Override
    public QName getXmlType() {
        return new QName(namespace, type.getSimpleName());
    }

    @Override
    public Collection<Class> getJavaTypes() {
        return Arrays.asList(new Class[]{type});
    }

    @Override
    public Object convertToJava(Node node) {
        try {
            Object o = context.createUnmarshaller().unmarshal(node);
            if (o instanceof JAXBElement) {
                return ((JAXBElement) o).getValue();
            } else {
                return o;
            }
        } catch (JAXBException ex) {
            throw new MidPointException("Error on convertion object " + node, ex);
        }

    }

    @Override
    public Node convertToXML(QName name, Object o) {
        try {
            Node n = d.createElement("container");
            context.createMarshaller().marshal(new Container(o), n);
            //first child is from <container> Node, the sedond is from the Container object
            return n.getFirstChild().getFirstChild();
        } catch (JAXBException ex) {
            throw new MidPointException("Error on convertion object " + o, ex);
        }
    }

    @XmlRootElement
    public static class Container {

        private Object child;

        public Container() {
        }

        public Container(Object child) {
            this.child = child;
        }

        public Object getChild() {
            return child;
        }

        public void setChild(Object child) {
            this.child = child;
        }
    }
}
