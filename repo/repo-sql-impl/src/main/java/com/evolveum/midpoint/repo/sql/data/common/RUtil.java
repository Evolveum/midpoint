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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
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

    public static <T> String toRepo(T value, PrismContext prismContext) throws JAXBException {
        if (value == null) {
            return null;
        }

        PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Marshaller.JAXB_FORMATTED_OUTPUT, false);

        if (value instanceof Objectable) {
            return jaxbProcessor.marshalToString((Objectable) value, properties);
        }

        return jaxbProcessor.marshalElementToString(new JAXBElement<Object>(new QName(SchemaConstants.NS_COMMON,
                "rUtilObject"), Object.class, value), properties);
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
//        result.setOwner(ownerId);
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

    public static RValue createRValue(Object object) throws DtoTranslationException {
        Validate.notNull(object, "Object must not be null.");

        RValue value;
        if ((object instanceof Element)) {// || XmlTypeConverter.canConvert(object.getClass())) {
            try {
                Object javaValue = null;//XmlTypeConverter.toJavaValue(object);
                System.out.println(javaValue.getClass());
                if (javaValue == null) {
                    throw new DtoTranslationException("Value converted from '" + object.toString() + "' is null.");
                }
                if (javaValue instanceof Long) {
                    RSimpleLongValue simple = new RSimpleLongValue();
                    simple.setValue((Long) javaValue);
                    value = simple;
                } else if (javaValue instanceof Integer) {
                    RSimpleLongValue simple = new RSimpleLongValue();
                    simple.setValue(new Long((Integer) javaValue));
                    value = simple;
                } else if (javaValue instanceof Date) {
                    RSimpleDateValue simple = new RSimpleDateValue();
                    simple.setValue((Date) javaValue);
                    value = simple;
                } else if (javaValue instanceof String) {
                    RSimpleStringValue simple = new RSimpleStringValue();
                    simple.setValue((String) javaValue);
                    value = simple;
                    //todo fix big strings - more than 255 bytes
                } else if (javaValue instanceof Element) {
                    value = createComplexValue((Element) javaValue);
                } else {
                    throw new DtoTranslationException("Can't translate type '" + object.getClass()
                            + "' to RValue. Value toString is:\n" + javaValue.toString());
                }
            } catch (DtoTranslationException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new DtoTranslationException(ex.getMessage(), ex);
            }
        } else {
            throw new DtoTranslationException("Unknown type '" + object.getClass()
                    + "', can't translate to RValue. Value toString is:\n" + object.toString());
        }

        if (object instanceof Element) {
            Element element = (Element) object;
            String namespace = element.getNamespaceURI();
            if (StringUtils.isEmpty(namespace)) {
                namespace = element.getOwnerDocument().getNamespaceURI();
            }
            value.setName(new QName(namespace, element.getLocalName()));
        }

        return value;
    }

    private static RComplexValue createComplexValue(Element element) {
        RComplexValue complex = new RComplexValue();
        complex.setValue(DOMUtil.printDom(element).toString());
        complex.setDom(true);

        return complex;
    }

    public static Long getLongWrappedFromString(String text) {
        return StringUtils.isNotEmpty(text) && text.matches("[0-9]*") ? Long.parseLong(text) : null;
    }

    public static long getLongFromString(String text) {
        Long value = getLongWrappedFromString(text);
        return value != null ? value : 0;
    }
}
