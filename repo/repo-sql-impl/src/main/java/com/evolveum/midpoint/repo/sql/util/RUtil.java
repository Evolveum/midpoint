/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.repo.sql.data.audit.RObjectDeltaOperation;
import com.evolveum.midpoint.repo.sql.data.common.OperationResult;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RSynchronizationSituationDescription;
import com.evolveum.midpoint.repo.sql.data.common.any.*;
import com.evolveum.midpoint.repo.sql.data.common.container.*;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LocalizedMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ParamsType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.codec.digest.DigestUtils;
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
     * {@link org.hibernate.type.MaterializedClobType} was not working properly with PostgreSQL,
     * causing TEXT types (clobs) to be saved not in table row but somewhere else and it always
     * messed up UTF-8 encoding
     */
    public static final String LOB_STRING_TYPE = "org.hibernate.type.StringClobType";

    public static final int COLUMN_LENGTH_QNAME = 200;

    public static final String QNAME_DELIMITER = "#";

    /**
     * This constant is used for oid column size in database.
     */
    public static final int COLUMN_LENGTH_OID = 36;

    /**
     * This namespace is used for wrapping xml parts of objects during save to database.
     */
    public static final String NS_SQL_REPO = "http://midpoint.evolveum.com/xml/ns/fake/sqlRepository-1.xsd";
    public static final String SQL_REPO_OBJECTS = "sqlRepoObjects";
    public static final String SQL_REPO_OBJECT = "sqlRepoObject";
    public static final QName CUSTOM_OBJECT = new QName(NS_SQL_REPO, SQL_REPO_OBJECT);
    public static final QName CUSTOM_OBJECTS = new QName(NS_SQL_REPO, SQL_REPO_OBJECTS);

    private static final Trace LOGGER = TraceManager.getTrace(RUtil.class);

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

    public static <T> T toJAXB(String value, Class<T> clazz, QName type, PrismContext prismContext) throws SchemaException,
            JAXBException {
        return toJAXB(null, null, value, clazz, type, prismContext);
    }

    public static <T> T toJAXB(Class<?> parentClass, ItemPath path, String value,
                               Class<T> clazz, PrismContext prismContext) throws SchemaException, JAXBException {
        return toJAXB(parentClass, path, value, clazz, null, prismContext);
    }

    public static <T> T toJAXB(Class<?> parentClass, ItemPath path, String value,
                               Class<T> clazz, QName complexType, PrismContext prismContext) throws SchemaException, JAXBException {
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
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Parsing:\n{}", DOMUtil.serializeDOMToString(root));
            }
            PrismObject object = domProcessor.parseObject(root);
            return (T) object.asObjectable();
        } else if (Containerable.class.isAssignableFrom(clazz)) {
            Element firstChild = getFirstSubElement(root);
            if (firstChild == null) {
                return null;
            }
            SchemaRegistry registry = prismContext.getSchemaRegistry();


            PrismContainerDefinition definition = null;
            if (parentClass != null) {
                PrismContainerDefinition parentDefinition = registry.determineDefinitionFromClass(parentClass);
                definition = parentDefinition.findContainerDefinition(path);
            } else if (complexType != null) {
                definition = registry.findContainerDefinitionByType(complexType);
            } else {
                definition = registry.determineDefinitionFromClass(clazz);
            }

            if (definition == null) {
                throw new SchemaException("Could not find definition for " + QNameUtil.getNodeQName(firstChild));
            }

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

    public static <T> List<T> safeSetToList(Set<T> set) {
        if (set == null || set.isEmpty()) {
            return new ArrayList<T>();
        }

        List<T> list = new ArrayList<T>();
        list.addAll(set);

        return list;
    }

    public static List<ObjectReferenceType> safeSetReferencesToList(Set<? extends RObjectReference> set, PrismContext prismContext) {
        List<ObjectReferenceType> list = new ArrayList<ObjectReferenceType>();

        if (set == null || set.isEmpty()) {
            return list;
        }

        for (RObjectReference str : set) {
            ObjectReferenceType ort = new ObjectReferenceType();
            RObjectReference.copyToJAXB(str, ort, prismContext);
            list.add(ort);
        }
        return list;
    }

    public static Set safeListReferenceToSet(List<ObjectReferenceType> list, PrismContext prismContext,
                                             RObject owner, RReferenceOwner refOwner) {
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
                                                 RObject owner, RReferenceOwner refOwner) {
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

    public static Integer getIntegerFromString(String val) {
        if (val == null || !val.matches("[0-9]+")) {
            return null;
        }

        return Integer.parseInt(val);
    }

    /**
     * This method is used to override "hasIdentifierMapper" in EntityMetaModels of entities which have
     * composite id and class defined for it. It's workaround for bug as found in forum
     * https://forum.hibernate.org/viewtopic.php?t=978915&highlight=
     *
     * @param sessionFactory
     */
    public static void fixCompositeIDHandling(SessionFactory sessionFactory) {
        fixCompositeIdentifierInMetaModel(sessionFactory, RObjectDeltaOperation.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RSynchronizationSituationDescription.class);

        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtDate.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtPolyString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtReference.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtLong.class);

        fixCompositeIdentifierInMetaModel(sessionFactory, RAssignmentExtension.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtDate.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtPolyString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtReference.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtLong.class);

        fixCompositeIdentifierInMetaModel(sessionFactory, RObjectReference.class);
        for (RReferenceOwner owner : RReferenceOwner.values()) {
            fixCompositeIdentifierInMetaModel(sessionFactory, owner.getClazz());
        }

        fixCompositeIdentifierInMetaModel(sessionFactory, RAssignmentReference.class);
        for (RCReferenceOwner owner : RCReferenceOwner.values()) {
            fixCompositeIdentifierInMetaModel(sessionFactory, owner.getClazz());
        }

        fixCompositeIdentifierInMetaModel(sessionFactory, RAssignment.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAuthorization.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RExclusion.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RTrigger.class);
        for (RObjectType type : ClassMapper.getKnownTypes()) {
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

    public static void copyResultToJAXB(OperationResult repo, OperationResultType jaxb, PrismContext prismContext) throws
            DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        jaxb.setDetails(repo.getDetails());
        jaxb.setMessage(repo.getMessage());
        jaxb.setMessageCode(repo.getMessageCode());
        jaxb.setOperation(repo.getOperation());
        if (repo.getStatus() != null) {
            jaxb.setStatus(repo.getStatus().getSchemaValue());
        }
        jaxb.setToken(repo.getToken());

        try {
            jaxb.setLocalizedMessage(RUtil.toJAXB(OperationResultType.class, new ItemPath(
                    OperationResultType.F_LOCALIZED_MESSAGE), repo.getLocalizedMessage(), LocalizedMessageType.class,
                    prismContext));
            jaxb.setParams(RUtil.toJAXB(OperationResultType.class, new ItemPath(OperationResultType.F_PARAMS),
                    repo.getParams(), ParamsType.class, prismContext));

            jaxb.setContext(RUtil.toJAXB(OperationResultType.class, new ItemPath(OperationResultType.F_CONTEXT),
                    repo.getContext(), ParamsType.class, prismContext));

            jaxb.setReturns(RUtil.toJAXB(OperationResultType.class, new ItemPath(OperationResultType.F_RETURNS),
                    repo.getReturns(), ParamsType.class, prismContext));


            if (StringUtils.isNotEmpty(repo.getPartialResults())) {
                OperationResultType result = RUtil.toJAXB(repo.getPartialResults(), OperationResultType.class,
                        prismContext);
                jaxb.getPartialResults().addAll(result.getPartialResults());
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static void copyResultFromJAXB(OperationResultType jaxb, OperationResult repo, PrismContext prismContext)
            throws DtoTranslationException {
        Validate.notNull(jaxb, "JAXB object must not be null.");
        Validate.notNull(repo, "Repo object must not be null.");

        repo.setDetails(jaxb.getDetails());
        repo.setMessage(jaxb.getMessage());
        repo.setMessageCode(jaxb.getMessageCode());
        repo.setOperation(jaxb.getOperation());
        repo.setStatus(getRepoEnumValue(jaxb.getStatus(), ROperationResultStatus.class));
        repo.setToken(jaxb.getToken());

        try {
            repo.setLocalizedMessage(RUtil.toRepo(jaxb.getLocalizedMessage(), prismContext));
            repo.setParams(RUtil.toRepo(jaxb.getParams(), prismContext));
            repo.setContext(RUtil.toRepo(jaxb.getContext(), prismContext));
            repo.setReturns(RUtil.toRepo(jaxb.getReturns(), prismContext));

            if (!jaxb.getPartialResults().isEmpty()) {
                OperationResultType result = new OperationResultType();
                result.getPartialResults().addAll(jaxb.getPartialResults());
                repo.setPartialResults(RUtil.toRepo(result, prismContext));
            }
        } catch (Exception ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static String computeChecksum(Object... objects) {
        StringBuilder builder = new StringBuilder();
        for (Object object : objects) {
            if (object == null) {
                continue;
            }

            builder.append(object.toString());
        }

        return DigestUtils.md5Hex(builder.toString());
    }

    public static <T extends SchemaEnum> T getRepoEnumValue(Object object, Class<T> type) {
        if (object == null) {
            return null;
        }
        Object[] values = type.getEnumConstants();
        for (Object value : values) {
            T schemaEnum = (T) value;
            if (schemaEnum.getSchemaValue().equals(object)) {
                return schemaEnum;
            }
        }

        throw new IllegalArgumentException("Unknown value '" + object
                + "' of type '" + object.getClass() + "', can't translate to '" + type + "'.");
    }

    public static String qnameToString(QName qname) {
        StringBuilder sb = new StringBuilder();
        if (qname != null) {
            sb.append(qname.getNamespaceURI());
        }
        sb.append(QNAME_DELIMITER);
        if (qname != null) {
            sb.append(qname.getLocalPart());
        }

        return sb.toString();
    }

    public static QName stringToQName(String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        int index = text.lastIndexOf(QNAME_DELIMITER);
        String namespace = StringUtils.left(text, index);
        String localPart = StringUtils.right(text, text.length() - index - 1);

        if (StringUtils.isEmpty(localPart)) {
            return null;
        }

        return new QName(namespace, localPart);
    }

    public static Long toLong(Short s) {
        if (s == null) {
            return null;
        }

        return s.longValue();
    }

    public static Short toShort(Long l) {
        if (l == null) {
            return null;
        }

        if (l > Short.MAX_VALUE || l < Short.MIN_VALUE) {
            throw new IllegalArgumentException("Couldn't cast value to short " + l);
        }

        return l.shortValue();
    }

    public static String getDebugString(RObject object) {
        StringBuilder sb = new StringBuilder();
        if (object.getName() != null) {
            sb.append(object.getName().getOrig());
        } else {
            sb.append("null");
        }
        sb.append('(').append(object.getOid()).append(')');

        return sb.toString();
    }

    public static String getTableName(Class hqlType) {
        MidPointNamingStrategy namingStrategy = new MidPointNamingStrategy();
        return namingStrategy.classToTableName(hqlType.getSimpleName());
    }
}
