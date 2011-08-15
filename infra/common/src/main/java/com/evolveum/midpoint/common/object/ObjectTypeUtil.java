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

package com.evolveum.midpoint.common.object;

import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Methods that would belong to the ObjectType class but cannot go there
 * because of JAXB.
 * 
 * There are also useful methods that would belong to other classes. But we don't
 * want to create new class for every method ... if this goes beyond a reasonable
 * degree, please refactor accordingly.
 * 
 * @author Radovan Semancik
 */
public class ObjectTypeUtil {

    static Random rnd = new Random();

    public static List<String> extractOids(List<? extends ObjectType> objects, List<? extends ObjectReferenceType> refs) {

        List<String> oids = new ArrayList<String>();

        if (objects != null) {
            for (ObjectType object : objects) {
                oids.add(object.getOid());
            }
        }

        if (refs != null) {
            for (ObjectReferenceType ref : refs) {
                oids.add(ref.getOid());
            }
        }

        return oids;
    }

    public static ObjectReferenceType findRef(String oid, List<ObjectReferenceType> refs) {
        for (ObjectReferenceType ref : refs) {
            if (ref.getOid().equals(oid)) {
                return ref;
            }
        }
        return null;
    }

//    TODO: refactor to use one code base for the method
    public static PropertyModificationType createPropertyModificationType(PropertyModificationTypeType changeType, XPathType xpathType, Node node) {
        PropertyModificationType change = new PropertyModificationType();
        change.setValue(new Value());
        change.setModificationType(changeType);
        change.setPath(xpathType.toElement(SchemaConstants.NS_C, "path"));
        change.getValue().getAny().add((Element) node);

        return change;
    }
//    public static PropertyModificationType createPropertyModificationType(PropertyModificationTypeType changetype, XPathType xpath, Element element) {
//        return createPropertyModificationType(changetype, xpath, new QName(element.getNamespaceURI(), element.getLocalName()), element.getTextContent());
//    }

    public static PropertyModificationType createPropertyModificationType(PropertyModificationTypeType changetype, XPathType xpath, QName property, Object value) {

        PropertyModificationType propertyChange = new PropertyModificationType();
        propertyChange.setModificationType(changetype);

        Document doc = DOMUtil.getDocument();

        if (xpath == null) {
            // Default XPath is empty XPath, which means "."
            xpath = new XPathType();
        }

        if (property.getPrefix() == null) {
            // If the prefix was not specified, generate a random prefix
            // to avoid collisions with standard "nsXX" prefixes
            String prefix = "ch" + rnd.nextInt(10000);
            property = new QName(property.getNamespaceURI(), property.getLocalPart(), prefix);
        }

        propertyChange.setPath(xpath.toElement(SchemaConstants.NS_C, "path", doc));

        Value jaxbValue = new Value();

        if (value==null) {
        	// Emtpy value, that means empty element set. Nothing to do.
        	// This may be used e.g. for deleting all values (replacing by empty value)
        } else if (XsdTypeConverter.canConvert(value.getClass())) {
        	Element e = doc.createElementNS(property.getNamespaceURI(), property.getLocalPart());
        	XsdTypeConverter.toXsdElement(value, e);
        	jaxbValue.getAny().add(e);
        	
        } else if (value.getClass().getPackage().equals(ObjectFactory.class.getPackage())) {
            // JAXB Object from a common schema
            Element e;
            try {
                e = JAXBUtil.jaxbToDom(value, property, doc);
            } catch (JAXBException ex) {
                // This should not happen
                throw new IllegalStateException(ex);
            }
            jaxbValue.getAny().add(e);
        } else if (value instanceof Element){
        	jaxbValue.getAny().add((Element) value);
        } else {
            throw new IllegalArgumentException("Unsupported value type " + value.getClass().getName());
        }

        propertyChange.setValue(jaxbValue);

        return propertyChange;
    }
    
    public static ObjectModificationType createModificationReplaceProperty(String oid, QName propertyName, Object propertyValue) {
		ObjectModificationType modification = new ObjectModificationType();
		modification.setOid(oid);
		List<PropertyModificationType> propertyModifications = modification.getPropertyModification();
		PropertyModificationType propertyModification = createPropertyModificationType(PropertyModificationTypeType.replace, null, propertyName, propertyValue);		
		propertyModifications.add(propertyModification);
		return modification;
	}
	
	public static String toShortString(ObjectType object) {
		if (object==null) {
			return "null";
		}
		return object.getClass().getSimpleName()+": "+object.getName()+"(OID:"+object.getOid()+")";
	}
	
	public static String dump(ObjectType object) {
		StringBuilder sb = new StringBuilder();
		sb.append(toShortString(object));
		sb.append("\n");
		Document doc = DOMUtil.getDocument();
		ObjectTypes objectTypeType = ObjectTypes.getObjectType(object.getClass());
		Element element;
		try {
			element = JAXBUtil.jaxbToDom(object, objectTypeType.getQName(), doc);
			sb.append(DOMUtil.serializeDOMToString(element));
		} catch (JAXBException e) {
			sb.append("Cannot serialize object to DOM: ");
			sb.append(e);
		}
		return sb.toString();
	}

	public static Object toShortString(ObjectReferenceType objectRef) {
		if (objectRef==null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("objectRef oid=").append(objectRef.getOid());
		if (objectRef.getType()!=null) {
			sb.append(" type=").append(objectRef.getType());
		}
		return sb.toString();
	}

	public static ObjectReferenceType createObjectRef(ObjectType objectType) {
		if (objectType==null) {
			return null;
		}
		ObjectReferenceType ref = new ObjectReferenceType();
		ref.setOid(objectType.getOid());
		ObjectTypes objectTypeType = ObjectTypes.getObjectType(objectType.getClass());
		if (objectTypeType!=null) {
			ref.setType(objectTypeType.getTypeQName());
		}
		return ref;
	}

	/**
	 * @param extension
	 * @return
	 */
	public static String dump(Extension extension) {
		if (extension==null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		for (Element e : extension.getAny()) {
			sb.append(DOMUtil.serializeDOMToString(e));
			sb.append("\n");
		}
		return sb.toString();
	}
}