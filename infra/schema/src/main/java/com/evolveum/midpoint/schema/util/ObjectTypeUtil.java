/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.marshaller.XPathHolder;
import com.evolveum.midpoint.prism.marshaller.XPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

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
	
	public static Collection<Referencable> getExtensionReferenceValues(ObjectType objectType, QName propertyQname) {
		PrismObject<? extends ObjectType> object = objectType.asPrismObject();
		PrismContainer<Containerable> extensionContainer = object.findContainer(ObjectType.F_EXTENSION);
		if (extensionContainer == null) {
			return null;
		}
		PrismReference property = extensionContainer.findReference(propertyQname);
		if (property == null) {
			return null;
		}
		Collection<Referencable> refs = new ArrayList<Referencable>(property.getValues().size());
		for (PrismReferenceValue refVal : property.getValues()){
			refs.add(refVal.asReferencable());
		}
		return refs;
	}
    

    public static ObjectReferenceType findRef(String oid, List<ObjectReferenceType> refs) {
        for (ObjectReferenceType ref : refs) {
            if (ref.getOid().equals(oid)) {
                return ref;
            }
        }
        return null;
    }

    public static String toShortString(PrismObject<? extends ObjectType> object) {
        return toShortString(object != null ? object.asObjectable() : null);
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
		return toShortString(objectRef, false);
	}

	public static Object toShortString(ObjectReferenceType objectRef, boolean withName) {
        if (objectRef == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("objectRef oid=").append(objectRef.getOid());
		if (withName && objectRef.getTargetName() != null) {
			sb.append(" name='").append(objectRef.getTargetName()).append("'");
		}
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

	@NotNull
	public static <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull ObjectReferenceType ref, @Nullable PrismContext prismContext) {
		AssignmentType assignment = new AssignmentType(prismContext);
		if (QNameUtil.match(ref.getType(), ResourceType.COMPLEX_TYPE)) {
			ConstructionType construction = new ConstructionType();
			construction.setResourceRef(ref);
			assignment.setConstruction(construction);
		} else {
			assignment.setTargetRef(ref);
		}
		return assignment;
	}

	@NotNull
	public static <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull PrismReferenceValue ref, @Nullable PrismContext prismContext) {
		ObjectReferenceType ort = new ObjectReferenceType();
		ort.setupReferenceValue(ref);
		return createAssignmentTo(ort, prismContext);
	}

	@NotNull
	public static <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull String oid, @NotNull ObjectTypes type, @Nullable PrismContext prismContext) {
		return createAssignmentTo(createObjectRef(oid, type), prismContext);
	}

	@NotNull
	public static <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull PrismObject<T> object) {
		AssignmentType assignment = new AssignmentType(object.getPrismContext());
		if (object.asObjectable() instanceof ResourceType) {
			ConstructionType construction = new ConstructionType(object.getPrismContext());
			construction.setResourceRef(createObjectRef(object));
			assignment.setConstruction(construction);
		} else {
			assignment.setTargetRef(createObjectRef(object));
		}
		return assignment;
	}

	public static ObjectReferenceType createObjectRef(PrismReferenceValue prv) {
		ObjectReferenceType ort = new ObjectReferenceType();
		ort.setupReferenceValue(prv);
		return ort;
	}

	public static ObjectReferenceType createObjectRef(ObjectType objectType) {
		if (objectType == null) {
			return null;
		}
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
        ref.setTargetName(object.asObjectable().getName());
        return ref;
    }
    
    //FIXME TODO temporary hack 
    public static <T extends ObjectType> ObjectReferenceType createObjectRef(PrismObject<T> object, boolean nameAsDescription) {
        if (object == null) {
            return null;
        }
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(object.getOid());
        if (nameAsDescription){
        	ref.setDescription(object.getBusinessDisplayName());
        }
        PrismObjectDefinition<T> definition = object.getDefinition();
        if (definition != null) {
            ref.setType(definition.getTypeName());
        }
        return ref;
    }
    
    public static <T extends ObjectType> ObjectReferenceType createObjectRef(PrismReferenceValue refVal, boolean nameAsDescription) {
        if (refVal == null) {
            return null;
        }
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(refVal.getOid());
        PrismObject<T> object = refVal.getObject();
        if (object != null) {
	        if (nameAsDescription) {
	        	ref.setDescription(object.getBusinessDisplayName());
	        }
	        PrismObjectDefinition<T> definition = object.getDefinition();
	        if (definition != null) {
	            ref.setType(definition.getTypeName());
	        }
	        ref.setTargetName(PolyString.toPolyStringType(object.getName()));
        } else {
        	ref.setType(refVal.getTargetType());
        	ref.setTargetName(PolyString.toPolyStringType(refVal.getTargetName()));
        	if (nameAsDescription && refVal.getTargetName() != null) {
	        	ref.setDescription(refVal.getTargetName().getOrig());
	        }
        }
        return ref;
    }
    
    public static ObjectReferenceType createObjectRef(String oid, ObjectTypes type) {
       return createObjectRef(oid, null, type);
    }
    
    public static ObjectReferenceType createObjectRef(String oid, PolyStringType name, ObjectTypes type) {
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(type, "Object type must not be null.");

        ObjectReferenceType reference = new ObjectReferenceType();
        reference.setType(type.getTypeQName());
        reference.setOid(oid);
        reference.setTargetName(name);

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

    public static PrismObject getParentObject(Containerable containerable) {
        if (containerable == null) {
            return null;
        }
        PrismContainerable<? extends Containerable> parent1 = containerable.asPrismContainerValue().getParent();
        if (parent1 == null) {
            return null;
        }
        if (!(parent1 instanceof PrismContainer)) {
            throw new IllegalArgumentException("Parent of " + containerable + " is not a PrismContainer. It is " + parent1.getClass());
        }
        PrismValue parent2 = ((PrismContainer) parent1).getParent();
        if (parent2 == null) {
            return null;
        }
        if (!(parent2 instanceof PrismContainerValue)) {
            throw new IllegalArgumentException("Grandparent of " + containerable + " is not a PrismContainerValue. It is " + parent2.getClass());
        }
        Itemable parent3 = parent2.getParent();
        if (parent3 == null) {
            return null;
        }
        if (!(parent3 instanceof PrismObject)) {
            throw new IllegalArgumentException("Grandgrandparent of " + containerable + " is not a PrismObject. It is " + parent3.getClass());
        }
        return (PrismObject) parent3;
    }

    public static List<PrismReferenceValue> objectReferenceListToPrismReferenceValues(Collection<ObjectReferenceType> refList) throws SchemaException {
        List<PrismReferenceValue> rv = new ArrayList<>();
        for (ObjectReferenceType ref : refList) {
            rv.add(ref.asReferenceValue());
        }
        return rv;
    }

    public static List<ObjectReferenceType> getAsObjectReferenceTypeList(PrismReference prismReference) throws SchemaException {
		List<ObjectReferenceType> rv = new ArrayList<>();
		for (PrismReferenceValue prv : prismReference.getValues()) {
			rv.add(createObjectRef(prv.clone()));
		}
		return rv;
	}

	public static List<String> referenceValueListToOidList(Collection<PrismReferenceValue> referenceValues) {
		List<String> oids = new ArrayList<>(referenceValues.size());
		for (PrismReferenceValue referenceValue : referenceValues) {
			oids.add(referenceValue.getOid());
		}
		return oids;
	}

	public static Objectable getObjectFromReference(ObjectReferenceType ref) {
		if (ref == null) {
			return null;
		}
		if (ref.asReferenceValue().getObject() == null) {
			return null;
		}
		return ref.asReferenceValue().getObject().asObjectable();
	}

	public static PrismObject<?> getPrismObjectFromReference(ObjectReferenceType ref) {
		if (ref == null) {
			return null;
		}
		return ref.asReferenceValue().getObject();
	}

	public static List<ObjectDelta<? extends ObjectType>> toDeltaList(ObjectDelta<?> delta) {
		@SuppressWarnings("unchecked")
		ObjectDelta<? extends ObjectType> objectDelta = (ObjectDelta<? extends ObjectType>) delta;
		return Collections.<ObjectDelta<? extends ObjectType>>singletonList(objectDelta);
	}

	// Hack: because DeltaBuilder cannot provide ObjectDelta<? extends ObjectType> (it is from schema)
	public static Collection<ObjectDelta<? extends ObjectType>> cast(Collection<ObjectDelta<?>> deltas) {
		@SuppressWarnings("unchecked")
		final Collection<ObjectDelta<? extends ObjectType>> deltas1 = (Collection) deltas;
		return deltas1;
	}

	public static PolyStringType getDisplayName(PrismObject<?> object) {
    	return object != null ? getDisplayName((ObjectType) object.asObjectable()) : null;
	}

	public static PolyStringType getDisplayName(ObjectType object) {
    	if (object instanceof AbstractRoleType) {
    		return ((AbstractRoleType) object).getDisplayName();
		} else if (object instanceof UserType) {
    		return ((UserType) object).getFullName();
		} else {
    		return null;
		}
	}

	public static PolyStringType getDisplayName(ObjectReferenceType ref) {
		return ref != null ? getDisplayName(ref.asReferenceValue().getObject()) : null;
	}

	public static ObjectType toObjectable(PrismObject object) {
    	return object != null ? (ObjectType) object.asObjectable() : null;
	}

	public static boolean containsOid(Collection<ObjectReferenceType> values, @NotNull String oid) {
		return values.stream().anyMatch(v -> oid.equals(v.getOid()));
	}

	@SuppressWarnings("unchecked")
	public static <T> T getExtensionItemRealValue(@Nullable ExtensionType extension, @NotNull QName itemName) {
    	if (extension == null) {
    		return null;
		}
		Item item = extension.asPrismContainerValue().findItem(itemName);
    	return item != null ? (T) item.getRealValue() : null;
	}

	public static QName normalizeRelation(QName name) {
    	if (name == null) {
    		return SchemaConstants.ORG_DEFAULT;
		} else {
    		return QNameUtil.setNamespaceIfMissing(name, SchemaConstants.NS_ORG, SchemaConstants.PREFIX_NS_ORG);
		}
	}

	public static void normalizeRelation(ObjectReferenceType reference) {
    	if (reference != null) {
			reference.setRelation(normalizeRelation(reference.getRelation()));
		}
	}

	public static void normalizeRelation(PrismReferenceValue reference) {
    	if (reference != null) {
			reference.setRelation(normalizeRelation(reference.getRelation()));
		}
	}

	public static void normalizeAllRelations(PrismValue value) {
    	if (value != null) {
			value.accept(createNormalizingVisitor());
		}
	}

	public static void normalizeAllRelations(Item<?,?> item) {
    	if (item != null) {
			item.accept(createNormalizingVisitor());
		}
	}

	private static Visitor createNormalizingVisitor() {
		return v -> {
			if (v instanceof PrismReferenceValue) {
				normalizeRelation((PrismReferenceValue) v);
			}
		};
	}

	public static void normalizeFilter(ObjectFilter filter) {
		if (filter != null) {
			filter.accept(f -> {
				if (f instanceof RefFilter) {
					emptyIfNull(((RefFilter) f).getValues()).forEach(v -> normalizeRelation(v));
				}
			});
		}
	}

	// This is not the right place for this. But let's leave it here for now.
	// See MID-3581
	public static boolean isDelegationRelation(QName relation) {
		return QNameUtil.match(relation, SchemaConstants.ORG_DEPUTY);
	}

	// This is not the right place for this. But let's leave it here for now.
	// See MID-3581
	public static boolean isMembershipRelation(QName relation) {
		return isDefaultRelation(relation)
				|| isManagerRelation(relation)
				|| QNameUtil.match(relation, SchemaConstants.ORG_META);
	}

	// This is not the right place for this. But let's leave it here for now.
	// See MID-3581
	public static boolean isManagerRelation(QName relation) {
		return QNameUtil.match(relation, SchemaConstants.ORG_MANAGER);
	}

	// This is not the right place for this. But let's leave it here for now.
	// See MID-3581
	public static boolean isDefaultRelation(QName relation) {
		return relation == null || QNameUtil.match(relation, SchemaConstants.ORG_DEFAULT);
	}
	
	// We want to make this configurable in the future MID-3581
	public static boolean processRelationOnLogin(QName relation) {
		return isMembershipRelation(relation) || isDelegationRelation(relation);
	}
	
	// We want to make this configurable in the future MID-3581
	public static boolean processRelationOnRecompute(QName relation) {
		return !QNameUtil.match(relation, SchemaConstants.ORG_APPROVER) && !QNameUtil.match(relation, SchemaConstants.ORG_OWNER);
	}

	public static boolean relationMatches(QName relationQuery, QName relation) {
		return QNameUtil.match(relationQuery, PrismConstants.Q_ANY) || relationsEquivalent(relationQuery, relation);
	}

	public static boolean relationsEquivalent(QName relation1, QName relation2) {
		if (ObjectTypeUtil.isDefaultRelation(relation1)) {
			return ObjectTypeUtil.isDefaultRelation(relation2);
		} else {
			return QNameUtil.match(relation1, relation2);
		}
	}
	
	public static boolean referenceMatches(ObjectReferenceType ref, String targetOid, QName targetType, QName relation) {
		if (ref == null) {
			return false;
		}
		if (targetOid != null) {
			if (!targetOid.equals(ref.getOid())) {
				return false;
			}
		}
		if (targetType != null) {
			if (!QNameUtil.match(ref.getType(), targetType)) {
				return false;
			}
		}
		if (relation != null) {
			if (!relationMatches(relation, ref.getRelation())) {
				return false;
			}
		}
		return true;
	}

	public static OrderConstraintsType getConstraintFor(List<OrderConstraintsType> constraints, QName relation) {
		return CollectionUtils.emptyIfNull(constraints).stream()
				.filter(c -> QNameUtil.match(c.getRelation(), relation))		// intentionally not using default/null equivalence here
				.findFirst().orElse(null);
	}

	public static <T extends Objectable> T asObjectable(PrismObject<T> prismObject) {
    	return prismObject != null ? prismObject.asObjectable() : null;
	}

}