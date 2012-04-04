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
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.prism.xml.ns._public.types_2.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.ModificationTypeType;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Methods that would belong to the ObjectType class but cannot go there because
 * of JAXB.
 * <p/>
 * There are also useful methods that would belong to other classes. But we
 * don't want to create new class for every method ... if this goes beyond a
 * reasonable degree, please refactor accordingly.
 *
 * @author Radovan Semancik
 */
public class ObjectTypeUtil {

    public static List<String> extractOids(List<? extends ObjectType> objects,
                                           List<? extends ObjectReferenceType> refs) {

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

    @Deprecated
    public static ItemDeltaType createPropertyModificationType(ModificationTypeType changeType, XPathHolder xpathType, Object element) {
        ItemDeltaType change = new ItemDeltaType();
        change.setValue(new ItemDeltaType.Value());
        change.setModificationType(changeType);
        if (xpathType == null) {
            // Default XPath is empty XPath, which means "."
            xpathType = new XPathHolder();
        }
        change.setPath(xpathType.toElement(SchemaConstants.NS_C, "path"));
        change.getValue().getAny().add(element);

        return change;
    }

    @Deprecated
    public static ItemDeltaType createPropertyModificationType(ModificationTypeType changeType,
    		XPathHolder xpath, QName propertyName, Object value) {
        Collection<Object> values = new ArrayList<Object>(1);
        values.add(value);
        return createPropertyModificationType(changeType, xpath, propertyName, values);
    }

    @Deprecated
    public static ItemDeltaType createPropertyModificationType(ModificationTypeType changeType,
    		XPathHolder xpath, PrismProperty property) {
        return createPropertyModificationType(changeType, xpath, property.getName(), property.getValues());
    }

    @Deprecated
    public static ItemDeltaType createPropertyModificationType(ModificationTypeType changeType,
    		XPathHolder xpath, QName propertyName, Collection<Object> values) {

        ItemDeltaType propertyChange = new ItemDeltaType();
        propertyChange.setModificationType(changeType);

        Document doc = DOMUtil.getDocument();

        if (xpath == null) {
            // Default XPath is empty XPath, which means "."
            xpath = new XPathHolder();
        }

        if (propertyName.getPrefix() == null) {
            // If the prefix was not specified, generate a random prefix
            // to avoid collisions with standard "nsXX" prefixes
            String prefix = GlobalDynamicNamespacePrefixMapper.getPreferredPrefix(propertyName.getNamespaceURI());
            propertyName = new QName(propertyName.getNamespaceURI(), propertyName.getLocalPart(), prefix);
        }

        propertyChange.setPath(xpath.toElement(SchemaConstants.NS_C, "path", doc));

        ItemDeltaType.Value jaxbValue = new ItemDeltaType.Value();
        if (values != null) {
            for (Object value : values) {
                if (value == null) {
                    // Empty value, that means empty element set. Nothing to do.
                    // This may be used e.g. for deleting all values (replacing by empty
                    // value)
                } else if (XmlTypeConverter.canConvert(value.getClass())) {
                    try {
                        Object e = XmlTypeConverter.toXsdElement(value, propertyName, doc);
                        jaxbValue.getAny().add(e);
                    } catch (SchemaException ex) {
                        throw new SystemException("Unexpected JAXB problem while converting " + propertyName + " : "
                                + ex.getMessage(), ex);
                    }
                } else if (value instanceof Element) {
                    // This may not be needed any more
                    jaxbValue.getAny().add((Element) value);
                } else {
                    throw new IllegalArgumentException("Unsupported value type " + value.getClass().getName());
                }
            }
        }

        propertyChange.setValue(jaxbValue);

        return propertyChange;
    }

    @Deprecated
    public static ObjectModificationType createModificationReplaceProperty(String oid, QName propertyName,
                                                                           Object propertyValue) {
        return createModificationReplaceProperty(oid, null, propertyName, propertyValue);
    }

    @Deprecated
    public static ObjectModificationType createModificationReplaceProperty(String oid, XPathHolder xpath, QName propertyName,
                                                                           Object propertyValue) {
        ObjectModificationType modification = new ObjectModificationType();
        modification.setOid(oid);
        List<ItemDeltaType> propertyModifications = modification.getModification();
        ItemDeltaType propertyModification = createPropertyModificationType(ModificationTypeType.REPLACE,
        		xpath, propertyName, propertyValue);
        propertyModifications.add(propertyModification);
        return modification;
    }

    public static String toShortString(ObjectType object) {
        if (object == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(getShortTypeName(object));
        builder.append(": ");
        builder.append(object.getName());
        builder.append("(OID:");
        builder.append(object.getOid());
        builder.append(")");

        return builder.toString();
    }

    public static String toShortString(AssignmentType assignment) {
        if (assignment == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("Assignment(");
        if (assignment.getAccountConstruction() != null) {
            sb.append("account");
            // TODO
        }
        if (assignment.getTarget() != null) {
            sb.append(toShortString(assignment.getTarget()));
        }
        if (assignment.getTargetRef() != null) {
            sb.append(toShortString(assignment.getTargetRef()));
        }
        sb.append(")");
        return sb.toString();
    }


    public static String dump(ObjectType object) {
    	if (object == null) {
    		return "null";
    	}
        return object.asPrismObject().dump();
    }

    public static Object toShortString(ObjectReferenceType objectRef) {
        if (objectRef == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("objectRef oid=").append(objectRef.getOid());
        if (objectRef.getType() != null) {
            sb.append(" type=").append(SchemaDebugUtil.prettyPrint(objectRef.getType()));
        }
        return sb.toString();
    }

	public static String getShortTypeName(ObjectType object) {
		return getShortTypeName(object.getClass());
	}
	
	public static String getShortTypeName(Class<? extends ObjectType> type) {
		ObjectTypes objectTypeType = ObjectTypes.getObjectType(type);
        if (objectTypeType != null) {
            return objectTypeType.getQName().getLocalPart();
        } else {
            return type.getSimpleName();
        }
	}

    
    public static ObjectReferenceType createObjectRef(ObjectType objectType) {
        return createObjectRef(objectType.asPrismObject());
    }

    public static <T extends ObjectType> ObjectReferenceType createObjectRef(PrismObject<T> object) {
        if (object == null) {
            return null;
        }
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(object.getOid());
        PrismObjectDefinition<T> definition = object.getDefinition();
        if (definition != null) {
            ref.setType(definition.getTypeName());
        }
        return ref;
    }
    
    public static ObjectReferenceType createObjectRef(String oid, ObjectTypes type) {
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(type, "Object type must not be null.");

        ObjectReferenceType reference = new ObjectReferenceType();
        reference.setType(type.getTypeQName());
        reference.setOid(oid);

        return reference;
    }


//    /**
//     * @param extension
//     * @return
//     */
//    public static String dump(Extension extension) {
//        if (extension == null) {
//            return "null";
//        }
//        StringBuilder sb = new StringBuilder();
//        for (Object e : extension.getAny()) {
//            try {
//                sb.append(JAXBUtil.serializeElementToString(e));
//            } catch (JAXBException e1) {
//                sb.append("[Unexpected exception: ");
//                sb.append(e1);
//                sb.append("]: ");
//                sb.append(e);
//            }
//            sb.append("\n");
//        }
//        return sb.toString();
//    }

    /**
     * Returns the &lt;xsd:schema&gt; element from the XmlSchemaType.
     */
    public static Element findXsdElement(XmlSchemaType xmlSchemaType) {
        if (xmlSchemaType == null) {
            return null;
        }
        PrismContainerValue<XmlSchemaType> xmlSchemaContainerValue = xmlSchemaType.asPrismContainerValue();
        return findXsdElement(xmlSchemaContainerValue);
    }
    
    public static Element findXsdElement(PrismContainer<XmlSchemaType> xmlSchemaContainer) {
    	return findXsdElement(xmlSchemaContainer.getValue());
    }
    
    public static Element findXsdElement(PrismContainerValue<XmlSchemaType> xmlSchemaContainerValue) {
        PrismProperty<Element> definitionProperty = xmlSchemaContainerValue.findProperty(XmlSchemaType.F_DEFINITION);
        if (definitionProperty == null) {
			return null;
		}
        Element definitionElement = definitionProperty.getValue().getValue();
        if (definitionElement == null) {
			return null;
		}
        List<Element> schemaElements = DOMUtil.listChildElements(definitionElement);
        for (Element e : schemaElements) {
            if (QNameUtil.compareQName(DOMUtil.XSD_SCHEMA_ELEMENT, e)) {
            	DOMUtil.fixNamespaceDeclarations(e);
                return e;
            }
        }
        return null;
    }
    
	public static void setXsdSchemaDefinition(PrismProperty<Element> definitionProperty, Element xsdElement) {
		Document document = xsdElement.getOwnerDocument();
		Element definitionElement = document.createElementNS(XmlSchemaType.F_DEFINITION.getNamespaceURI(),
				XmlSchemaType.F_DEFINITION.getLocalPart());
		definitionElement.appendChild(xsdElement);
		definitionProperty.setRealValue(definitionElement);
	}

    
//    /**
//     * common-1.xsd namespace is assumed
//     * single value and "replace" modification are assumed
//     */
//    public static <T> T getPropertyNewValue(ObjectModificationType objectChange, String pathSegment,
//                                            String propertyName, Class<T> propertyClass) throws SchemaException {
////		XPathSegment xpathSegment = new XPathSegment(new QName(SchemaConstants.NS_C,pathSegment));
////		List<XPathSegment> segmentlist = new ArrayList<XPathSegment>(1);
////		segmentlist.add(xpathSegment);
////		XPathHolder xpath = new XPathHolder(segmentlist);
//        XPathHolder xpath = createXPathHolder(new QName(SchemaConstants.NS_C, pathSegment));
//        return getPropertyNewValue(objectChange, xpath, new QName(SchemaConstants.NS_C, propertyName), propertyClass);
//    }

    public static XPathHolder createXPathHolder(QName property) {
        XPathSegment xpathSegment = new XPathSegment(property);
        List<XPathSegment> segmentlist = new ArrayList<XPathSegment>(1);
        segmentlist.add(xpathSegment);
        XPathHolder xpath = new XPathHolder(segmentlist);
        return xpath;
    }

//    /**
//     * single value and "replace" modification are assumed
//     */
//    public static <T> T getPropertyNewValue(ObjectModificationType objectChange, XPathHolder path,
//                                            QName propertyName, Class<T> propertyClass) throws SchemaException {
//        PropertyModificationType propertyModification = getPropertyModification(objectChange, path, propertyName);
//        if (propertyModification == null) {
//            return null;
//        }
//        if (!propertyModification.getModificationType().equals(PropertyModificationTypeType.replace) &&  
//        		!propertyModification.getModificationType().equals(PropertyModificationTypeType.add)) {
//        	// The "add" above is not entirely correct. TODO: fix it once we switch to deltas 
//            throw new IllegalStateException("Encoutered modify type " + propertyModification.getModificationType() + " while expecting " + PropertyModificationTypeType.replace + " during modification of property " + propertyName);
//        }
//        List<Object> valueElements = propertyModification.getValue().getAny();
//        if (valueElements.size() > 1) {
//            throw new IllegalStateException("Multiple values during modification of property " + propertyName + " while expeting just a single value");
//        }
//        return XmlTypeConverter.toJavaValue(valueElements.get(0), propertyClass);
//    }

//    public static PropertyModificationType getPropertyModification(ObjectModificationType objectChange, XPathHolder path,
//                                                                   QName propertyName) {
//        List<PropertyModificationType> propertyModifications = objectChange.getPropertyModification();
//        for (PropertyModificationType propertyModification : propertyModifications) {
//            XPathHolder propertyPath = new XPathHolder(propertyModification.getPath());
//            if (!path.equals(propertyPath)) {
//                if (propertyPath.isBelow(path)) {
//                    // this is an UGLY HACK
//                    // TODO: explain
//                    List<XPathSegment> tail = propertyPath.getTail(path);
//                    if (tail.isEmpty()) {
//                        continue;
//                    }
//                    XPathSegment propSegment = tail.get(0);
//                    if (!propertyName.equals(propSegment.getQName())) {
//                        continue;
//                    }
//                    PropertyModificationType newPropertyMod = new PropertyModificationType();
//                    Document doc = DOMUtil.getDocument();
//                    newPropertyMod.setPath(path.toElement(SchemaConstants.I_PROPERTY_CONTAINER_REFERENCE_PATH, doc));
//                    newPropertyMod.setModificationType(propertyModification.getModificationType());
//                    Element rootElement = DOMUtil.createElement(doc, propertyName);
//                    Element currentElement = rootElement;
//                    for (int i = 1; i < tail.size(); i++) {
//                        XPathSegment xPathSegment = tail.get(i);
//                        Element newElement = DOMUtil.createElement(doc, xPathSegment.getQName());
//                        currentElement.appendChild(newElement);
//                        currentElement = newElement;
//                    }
//                    for (Object valueJaxbElement : propertyModification.getValue().getAny()) {
//                        Element valueDomElement;
//                        try {
//                            valueDomElement = JAXBUtil.toDomElement(valueJaxbElement, doc, true, true, false);
//                        } catch (JAXBException e) {
//                            throw new IllegalStateException("Cannot convert element " + valueJaxbElement + " to DOM");
//                        }
//                        currentElement.appendChild(valueDomElement);
//                    }
//                    Value value = new Value();
//                    value.getAny().add(rootElement);
//                    newPropertyMod.setValue(value);
//                    return newPropertyMod;
//                } else {
//                    continue;
//                }
//            }
//            if (!propertyName.equals(getElementName(propertyModification))) {
//                continue;
//            }
//            return propertyModification;
//        }
//        return null;
//    }

    public static boolean isModificationOf(ItemDeltaType modification, QName elementName) {
        return isModificationOf(modification, elementName, null);
    }

    public static boolean isModificationOf(ItemDeltaType modification, QName elementName, XPathHolder path) {

        if (path == null && XPathHolder.isDefault(modification.getPath())) {
            return (elementName.equals(ObjectTypeUtil.getElementName(modification)));
        }
        if (path == null) {
            return false;
        }
        XPathHolder modPath = new XPathHolder(modification.getPath());
        if (path.equals(modPath)) {
            return (elementName.equals(ObjectTypeUtil.getElementName(modification)));
        }
        return false;
    }

    public static QName getElementName(ItemDeltaType propertyModification) {
        if (propertyModification.getValue() == null) {
            throw new IllegalArgumentException("Modification without value element");
        }
        if (propertyModification.getValue().getAny() == null || propertyModification.getValue().getAny().isEmpty()) {
            throw new IllegalArgumentException("Modification with empty value element");
        }
        return JAXBUtil.getElementQName(propertyModification.getValue().getAny().get(0));
    }

    public static boolean isEmpty(ObjectModificationType objectModification) {
        return (objectModification.getModification() == null) ||
                objectModification.getModification().isEmpty();
    }
    
    public static void assertConcreteType(Class<? extends Objectable> type) {
    	// The abstract object types are enumerated here. It should be switched to some flag later on
    	if (type.equals(ObjectType.class)) {
    		throw new IllegalArgumentException("The type "+type.getName()+" is abstract");
    	}
    }

}