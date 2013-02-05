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

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.data.common.any.*;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RSynchronizationSituationDescription;
import com.evolveum.midpoint.repo.sql.data.common.enums.RReferenceOwner;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationDescriptionType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.lang.reflect.Field;
import java.util.*;

/**
 * @author lazyman
 */
public final class RUtil {

    /**
     * This constant is used for mapping type for {@link javax.persistence.Lob} fields.
     */
    public static final String LOB_STRING_TYPE = "org.hibernate.type.MaterializedClobType";

    /**
     * This constant is used for {@link QName#localPart} column size in databases.
     */
    public static final int COLUMN_LENGTH_LOCALPART = 100;

    /**
     * This namespace is used for wrapping xml parts of objects during save to database.
     */
    public static final String NS_SQL_REPO = "http://midpoint.evolveum.com/xml/ns/fake/sqlRepository-1.xsd";
    public static final String SQL_REPO_OBJECTS = "sqlRepoObjects";
    public static final String SQL_REPO_OBJECT = "sqlRepoObject";
    public static final QName CUSTOM_OBJECT = new QName(NS_SQL_REPO, SQL_REPO_OBJECT);
    public static final QName CUSTOM_OBJECTS = new QName(NS_SQL_REPO, SQL_REPO_OBJECTS);

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
        PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
        if (List.class.isAssignableFrom(clazz)) {
            List<Element> objects = DOMUtil.getChildElements(root, CUSTOM_OBJECT);

            List list = new ArrayList();
            for (Element element : objects) {
                JAXBElement jaxbElement = jaxbProcessor.unmarshalElement(element, Object.class);
                list.add(jaxbElement.getValue());
            }
            return (T) list;
        } else if (Objectable.class.isAssignableFrom(clazz)) {
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

        Object valueForMarshall = value;
        PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
        if (value instanceof List) {
            List valueList = (List) value;
            if (valueList.isEmpty()) {
                return null;
            }

            Document document = DOMUtil.getDocument();
            Element root = DOMUtil.createElement(document, CUSTOM_OBJECTS);
            document.appendChild(root);

            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(Marshaller.JAXB_FORMATTED_OUTPUT, false);
            for (Object val : valueList) {
                jaxbProcessor.marshalElementToDom(new JAXBElement<Object>(CUSTOM_OBJECT, Object.class, val), root);
            }

            return DOMUtil.printDom(root, false, false).toString();
        }

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Marshaller.JAXB_FORMATTED_OUTPUT, false);
        return jaxbProcessor.marshalElementToString(new JAXBElement<Object>(CUSTOM_OBJECT, Object.class, valueForMarshall), properties);
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

    public static Set<RSynchronizationSituationDescription> listSyncSituationToSet(List<SynchronizationSituationDescriptionType> list) {
        Set<RSynchronizationSituationDescription> set = new HashSet<RSynchronizationSituationDescription>();
        if (list != null) {
            for (SynchronizationSituationDescriptionType str : list) {
                set.add(RSynchronizationSituationDescription.copyFromJAXB(str));
            }
        }

        return set;
    }

    public static List<SynchronizationSituationDescriptionType> safeSetSyncSituationToList(Set<RSynchronizationSituationDescription> set) {
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

    @Deprecated
    public static List<ObjectReferenceType> safeSetReferencesToList123(Set<REmbeddedReference> set, PrismContext prismContext) {
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

    public static List<ObjectReferenceType> safeSetReferencesToList(Set<RObjectReference> set, PrismContext prismContext) {
        if (set == null || set.isEmpty()) {
            return new ArrayList<ObjectReferenceType>();
        }

        List<ObjectReferenceType> list = new ArrayList<ObjectReferenceType>();
        for (RObjectReference str : set) {
            ObjectReferenceType ort = new ObjectReferenceType();
            RObjectReference.copyToJAXB(str, ort, prismContext);
            list.add(ort);
        }
        return list;
    }

    public static Set<RObjectReference> safeListReferenceToSet(List<ObjectReferenceType> list, PrismContext prismContext,
                                                               RContainer owner, RReferenceOwner refOwner) {
        Set<RObjectReference> set = new HashSet<RObjectReference>();
        if (list == null || list.isEmpty()) {
            return set;
        }

        for (ObjectReferenceType ref : list) {
            RObjectReference rRef = RUtil.jaxbRefToRepo(ref, prismContext, owner, refOwner);
            if (rRef != null) {
                set.add(rRef);
            }
        }
        return set;
    }

    public static RObjectReference jaxbRefToRepo(ObjectReferenceType reference, PrismContext prismContext,
                                                 RContainer owner, RReferenceOwner refOwner) {
        if (reference == null) {
            return null;
        }
        Validate.notNull(owner, "Owner of reference must not be null.");
        Validate.notNull(refOwner, "Reference owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RObjectReference repoRef = RReferenceOwner.createObjectReference(refOwner);
        repoRef.setOwner(owner);
        RObjectReference.copyFromJAXB(reference, repoRef, prismContext);

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

    public static Long getLongContainerIdFromString(String text) throws DtoTranslationException {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        if (!text.matches("[0-9]+")) {
            throw new DtoTranslationException("Couldn't create long id from '" + text + "'.");
        }

        return Long.parseLong(text);
    }

    public static String getStringFromLong(Long id) {
        if (id == null) {
            return null;
        }

        return id.toString();
    }

    /**
     * This method is used to override "hasIdentifierMapper" in EntityMetamodels of entities which have
     * composite id and class defined for it. It's workeround for bug as found in forum
     * https://forum.hibernate.org/viewtopic.php?t=978915&highlight=
     *
     * @param sessionFactory
     */
    public static void fixCompositeIDHandling(SessionFactory sessionFactory) {
        fixCompositeIdentifierInMetaModel(sessionFactory, RAnyContainer.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAnyClob.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAnyDate.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAnyString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAnyReference.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAnyLong.class);

        fixCompositeIdentifierInMetaModel(sessionFactory, RObjectReference.class);
        for (RReferenceOwner owner : RReferenceOwner.values()) {
            fixCompositeIdentifierInMetaModel(sessionFactory, owner.getClazz());
        }

        fixCompositeIdentifierInMetaModel(sessionFactory, RContainer.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAssignment.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RExclusion.class);
        for (RContainerType type : ClassMapper.getKnownTypes()) {
            fixCompositeIdentifierInMetaModel(sessionFactory, type.getClazz());
        }
    }

    private static void fixCompositeIdentifierInMetaModel(SessionFactory sessionFactory, Class clazz) {
        ClassMetadata classMetadata = sessionFactory.getClassMetadata(clazz);
        if (classMetadata instanceof AbstractEntityPersister) {
            AbstractEntityPersister persister = (AbstractEntityPersister) classMetadata;
            EntityMetamodel model = persister.getEntityMetamodel();
            IdentifierProperty identifier = model.getIdentifierProperty();

            try {
                Field field = IdentifierProperty.class.getDeclaredField("hasIdentifierMapper");
                field.setAccessible(true);
                field.set(identifier, true);
                field.setAccessible(false);
            } catch (Exception ex) {
                throw new SystemException("Attempt to fix entity meta model with hack failed, reason: "
                        + ex.getMessage(), ex);
            }
        }
    }
}
