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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.parser.XPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

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
	
	/**
	 * Never returns null. Returns empty collection instead.
	 */
	public static <T> Collection<T> getExtensionPropertyValuesNotNull(ObjectType objectType, QName propertyQname) {
		Collection<T> values = getExtensionPropertyValues(objectType, propertyQname);
		if (values == null) {
			return new ArrayList<T>(0);
		} else {
			return values;
		}
	}
	
	public static <T> Collection<T> getExtensionPropertyValues(ObjectType objectType, QName propertyQname) {
		PrismObject<? extends ObjectType> object = objectType.asPrismObject();
		PrismContainer<Containerable> extensionContainer = object.findContainer(ObjectType.F_EXTENSION);
		if (extensionContainer == null) {
			return null;
		}
		PrismProperty<T> property = extensionContainer.findProperty(propertyQname);
		if (property == null) {
			return null;
		}
		return property.getRealValues();
	}
    

    public static ObjectReferenceType findRef(String oid, List<ObjectReferenceType> refs) {
        for (ObjectReferenceType ref : refs) {
            if (ref.getOid().equals(oid)) {
                return ref;
            }
        }
        return null;
    }

    public static String toShortString(ObjectType object) {
        if (object == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(getShortTypeName(object));
        builder.append(": ");
        builder.append(object.getName());
        builder.append(" (OID:");
        builder.append(object.getOid());
        builder.append(")");

        return builder.toString();
    }

    public static String toShortString(AssignmentType assignment) {
        if (assignment == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("Assignment(");
        if (assignment.getConstruction() != null) {
            sb.append("construction");
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
        return object.asPrismObject().debugDump();
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
        PrismProperty<SchemaDefinitionType> definitionProperty = xmlSchemaContainerValue.findProperty(XmlSchemaType.F_DEFINITION);
        if (definitionProperty == null) {
			return null;
		}
        SchemaDefinitionType schemaDefinition = definitionProperty.getValue().getValue();
        if (schemaDefinition == null) {
			return null;
		}
        
        return schemaDefinition.getSchema();
        
//        List<Element> schemaElements = DOMUtil.listChildElements(definitionElement);
//        for (Element e : schemaElements) {
//            if (QNameUtil.compareQName(DOMUtil.XSD_SCHEMA_ELEMENT, e)) {
//            	DOMUtil.fixNamespaceDeclarations(e);
//                return e;
//            }
//        }
//        return null;
    }
    
	public static void setXsdSchemaDefinition(PrismProperty<SchemaDefinitionType> definitionProperty, Element xsdElement) {
		
//		Document document = xsdElement.getOwnerDocument();
//		Element definitionElement = document.createElementNS(XmlSchemaType.F_DEFINITION.getNamespaceURI(),
//				XmlSchemaType.F_DEFINITION.getLocalPart());
//		definitionElement.appendChild(xsdElement);
//		SchemaDefinitionType schemaDefinition = definitionProperty.getValue().getValue();
//		schemaDefinition.setSchema(definitionElement);
		SchemaDefinitionType schemaDefinition = new SchemaDefinitionType();
		schemaDefinition.setSchema(xsdElement);
		definitionProperty.setRealValue(schemaDefinition);
	}

    public static XPathHolder createXPathHolder(QName property) {
        XPathSegment xpathSegment = new XPathSegment(property);
        List<XPathSegment> segmentlist = new ArrayList<XPathSegment>(1);
        segmentlist.add(xpathSegment);
        XPathHolder xpath = new XPathHolder(segmentlist);
        return xpath;
    }

    public static boolean isModificationOf(ItemDeltaType modification, QName elementName) {
        return isModificationOf(modification, elementName, null);
    }

    //TODO: refactor after new schema
    public static boolean isModificationOf(ItemDeltaType modification, QName elementName, ItemPathType path) {

//        if (path == null && XPathHolder.isDefault(modification.getPath())) {
//            return (elementName.equals(ObjectTypeUtil.getElementName(modification)));
//        }
    	
    	ItemPathType modificationPath = modification.getPath();
    	if (ItemPathUtil.isDefault(modificationPath)){
    		throw new IllegalArgumentException("Path in the delta must not be null");
    	}
//    	  if (path == null && ItemPathUtil.isDefault(modificationPath)) {
//            return (elementName.equals(getElementName(modification)));
//        }
    	
        if (path == null) {
            return false;
        }
//        XPathHolder modPath = new XPathHolder(modification.getPath());
        ItemPath full = new ItemPath(path.getItemPath(), elementName);
        ItemPathType fullPath = new ItemPathType(full);
        return fullPath.equivalent(modificationPath);
//        if (fullPath.equals(modificationPath)) {
//            return (elementName.equals(getElementName(modification)));
//        }
//        return false;
    }

//    public static QName getElementName(ItemDeltaType propertyModification) {
//        if (propertyModification.getValue() == null) {
//            throw new IllegalArgumentException("Modification without value element");
//        }
//        if (propertyModification.getValue().getContent() == null || propertyModification.getValue().getContent().isEmpty()) {
//            throw new IllegalArgumentException("Modification with empty value element");
//        }
//        return JAXBUtil.getElementQName(propertyModification.getValue().getContent().get(0));
//    }

//    public static boolean isEmpty(ObjectModificationType objectModification) {
//        return (objectModification.getItemDelta() == null) ||
//                objectModification.getItemDelta().isEmpty();
//    }
    
    public static void assertConcreteType(Class<? extends Objectable> type) {
    	// The abstract object types are enumerated here. It should be switched to some flag later on
    	if (type.equals(ObjectType.class)) {
    		throw new IllegalArgumentException("The type "+type.getName()+" is abstract");
    	}
    }


}