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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
public final class RUtil {

    private static final QName CUSTOM_OBJECT = new QName("http://midpoint.evolveum.com/xml/ns/fake/sqlRepository-1.xsd", "sqlRepoObject");

    private RUtil() {
    }

    public static <T extends Objectable> void revive(PrismObject<T> object, Class<T> clazz, PrismContext prismContext)
            throws DtoTranslationException {
        try {
            object.revive(prismContext);
            prismContext.adopt(object, clazz);
        } catch (SchemaException ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static <T> T toJAXB(String value, Class<T> clazz, PrismContext prismContext)
            throws SchemaException, JAXBException {
        if (StringUtils.isEmpty(value)) {
            return null;
        }

        PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
        JAXBElement<T> element = jaxbProcessor.unmarshalElement(value, clazz);

        return element.getValue();
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

    public static <T> List<T> safeSetToList(Set<T> set) {
        if (set == null || set.isEmpty()) {
            return new ArrayList<T>();
        }

        List<T> list = new ArrayList<T>();
        list.addAll(set);

        return list;
    }

    public static RObjectReferenceType jaxbRefToRepo(ObjectReferenceType ref, ObjectType owner,
            PrismContext prismContext) {
        if (ref == null) {
            return null;
        }
        Validate.notNull(owner, "Owner of reference must not be null.");

        return jaxbRefToRepo(ref, owner.getOid(), prismContext);
    }

    public static RObjectReferenceType jaxbRefToRepo(ObjectReferenceType ref, String ownerId,
            PrismContext prismContext) {
        if (ref == null) {
            return null;
        }
//        Validate.notEmpty(ownerId, "Owner oid of reference must not be null.");

        RObjectReferenceType result = new RObjectReferenceType();
//        result.setOwnerOid(ownerId);
//        result.setOwnerId(0L);
        RObjectReferenceType.copyFromJAXB(ref, result, prismContext);

        return result;
    }

    public static ROperationResultType jaxbResultToRepo(RObjectType owner, OperationResultType result,
            PrismContext prismContext) throws DtoTranslationException {
        if (result == null) {
            return null;
        }

        ROperationResultType rResult = new ROperationResultType();
        ROperationResultType.copyFromJAXB(result, rResult, prismContext);

        rResult.setOwner(owner);

        return rResult;
    }

    public static Long getLongWrappedFromString(String text) {
        return StringUtils.isNotEmpty(text) && text.matches("[0-9]*") ? Long.parseLong(text) : null;
    }

    public static long getLongFromString(String text) {
        Long value = getLongWrappedFromString(text);
        return value != null ? value : 0;
    }
}
