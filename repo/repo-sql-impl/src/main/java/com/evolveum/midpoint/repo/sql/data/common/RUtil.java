/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationDescriptionType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
public final class RUtil {

    public static final String NS_SQL_REPO = "http://midpoint.evolveum.com/xml/ns/fake/sqlRepository-1.xsd";
    static final QName CUSTOM_OBJECT = new QName(NS_SQL_REPO, "sqlRepoObject");

    private RUtil() {
    }

    public static <T extends Objectable> void revive(Objectable object, PrismContext prismContext)
            throws DtoTranslationException {
        try {
            prismContext.adopt(object);
        } catch (SchemaException ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static <T> T toJAXB(String value, Class<T> clazz, PrismContext prismContext) throws SchemaException,
            JAXBException {
        return toJAXB(null, null, value, clazz, prismContext);
    }

    public static <T> T toJAXB(Class<?> parentClass, ItemPath path, String value,
            Class<T> clazz, PrismContext prismContext) throws SchemaException, JAXBException {
        if (StringUtils.isEmpty(value)) {
            return null;
        }

        Document document = DOMUtil.parseDocument(value);
        Element root = document.getDocumentElement();

        PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
        if (Objectable.class.isAssignableFrom(clazz)) {
            if (root == null) {
                return null;
            }
            PrismObject object = domProcessor.parseObject(root);
            return (T) object.asObjectable();
        } else if (Containerable.class.isAssignableFrom(clazz)) {
            Element firstChild = getFirstSubElement(root);
            if (firstChild == null) {
                return null;
            }
            SchemaRegistry registry = prismContext.getSchemaRegistry();
            PrismContainerDefinition parentDefinition = registry.determineDefinitionFromClass(parentClass);
            PrismContainerDefinition definition = parentDefinition.findContainerDefinition(path);

            PrismContainer container = domProcessor.parsePrismContainer(firstChild, definition);
            return (T) container.getValue().asContainerable(clazz);
        }

        PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
        JAXBElement<T> element = jaxbProcessor.unmarshalElement(root, clazz);
        return element.getValue();
    }

    private static Element getFirstSubElement(Element parent) {
        if (parent == null) {
            return null;
        }

        NodeList list = parent.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return (Element) list.item(i);
            }
        }

        return null;
    }

    public static <T> String toRepo(T value, PrismContext prismContext) throws SchemaException, JAXBException {
        if (value == null) {
            return null;
        }

        PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
        if (value instanceof Objectable) {
            return domProcessor.serializeObjectToString(((Objectable) value).asPrismObject());
        }

        if (value instanceof Containerable) {
            return domProcessor.serializeObjectToString(((Containerable) value).asPrismContainerValue(),
                    createFakeParentElement());
        }

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        return prismContext.getPrismJaxbProcessor().marshalElementToString(
                new JAXBElement<Object>(CUSTOM_OBJECT, Object.class, value), properties);
    }

    private static Element createFakeParentElement() {
        return DOMUtil.createElement(DOMUtil.getDocument(), CUSTOM_OBJECT);
    }

    public static <T> Set<T> listToSet(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return new HashSet<T>(list);
    }

    public static Set<RPolyString> listPolyToSet(List<PolyStringType> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        Set<RPolyString> set = new HashSet<RPolyString>();
        for (PolyStringType str : list) {
            set.add(RPolyString.copyFromJAXB(str));
        }
        return set;
    }

    public static List<PolyStringType> safeSetPolyToList(Set<RPolyString> set) {
        if (set == null || set.isEmpty()) {
            return new ArrayList<PolyStringType>();
        }

        List<PolyStringType> list = new ArrayList<PolyStringType>();
        for (RPolyString str : set) {
            list.add(RPolyString.copyToJAXB(str));
        }
        return list;
    }
    
    public static List<ObjectReferenceType> safeSetReferencesToList(Set<REmbeddedReference> set, PrismContext prismContext) {
        if (set == null || set.isEmpty()) {
            return new ArrayList<ObjectReferenceType>();
        }

        List<ObjectReferenceType> list = new ArrayList<ObjectReferenceType>();
        for (REmbeddedReference str : set) {
        	ObjectReferenceType ort = new ObjectReferenceType();
        	REmbeddedReference.copyToJAXB(str, ort, prismContext);
            list.add(ort);
        }
        return list;
    }
    
    public static Set<RSynchronizationSituationDescription> listSyncSituationToSet(List<SynchronizationSituationDescriptionType> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        Set<RSynchronizationSituationDescription> set = new HashSet<RSynchronizationSituationDescription>();
        for (SynchronizationSituationDescriptionType str : list) {
            set.add(RSynchronizationSituationDescription.copyFromJAXB(str));
        }
        return set;
    }

    public static List<SynchronizationSituationDescriptionType> safeSetSyncSituationToList(Set<RSynchronizationSituationDescription> set) {
        if (set == null || set.isEmpty()) {
            return new ArrayList<SynchronizationSituationDescriptionType>();
        }

        List<SynchronizationSituationDescriptionType> list = new ArrayList<SynchronizationSituationDescriptionType>();
        for (RSynchronizationSituationDescription str : set) {
            list.add(RSynchronizationSituationDescription.copyToJAXB(str));
        }
        return list;
    }

    public static <T> List<T> safeSetToList(Set<T> set) {
        if (set == null || set.isEmpty()) {
            return new ArrayList<T>();
        }

        List<T> list = new ArrayList<T>();
        list.addAll(set);

        return list;
    }

    public static RObjectReference jaxbRefToRepo(ObjectReferenceType ref, RContainer owner,
            PrismContext prismContext) {
        if (ref == null) {
            return null;
        }
        Validate.notNull(owner, "Owner of reference must not be null.");

        RObjectReference repoRef = new RObjectReference();
        repoRef.setOwner(owner);
        RObjectReference.copyFromJAXB(ref, repoRef, prismContext);

        return repoRef;
    }

    public static REmbeddedReference jaxbRefToEmbeddedRepoRef(ObjectReferenceType jaxb, PrismContext prismContext) {
        if (jaxb == null) {
            return null;
        }
        REmbeddedReference ref = new REmbeddedReference();
        REmbeddedReference.copyFromJAXB(jaxb, ref, prismContext);

        return ref;
    }

    public static Long getLongWrappedFromString(String text) {
        return StringUtils.isNotEmpty(text) && text.matches("[0-9]*") ? Long.parseLong(text) : null;
    }

    public static long getLongFromString(String text) {
        Long value = getLongWrappedFromString(text);
        return value != null ? value : 0;
    }

    public static String getStringFromLong(Long id) {
        if (id == null) {
            return null;
        }

        return id.toString();
    }
}
