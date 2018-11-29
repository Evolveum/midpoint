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
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;

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

	private static final Trace LOGGER = TraceManager.getTrace(ObjectTypeUtil.class);

	/**
	 * Never returns null. Returns empty collection instead.
	 */
	public static <T> Collection<T> getExtensionPropertyValuesNotNull(ObjectType objectType, QName propertyQname) {
		Collection<T> values = getExtensionPropertyValues(objectType, propertyQname);
		if (values == null) {
			return new ArrayList<>(0);
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
		PrismProperty<T> property = extensionContainer.findProperty(ItemName.fromQName(propertyQname));
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
		PrismReference property = extensionContainer.findReference(ItemName.fromQName(propertyQname));
		if (property == null) {
			return null;
		}
		Collection<Referencable> refs = new ArrayList<>(property.getValues().size());
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
        } else {
	        return getShortTypeName(object)
			        + ": "
			        + object.getName()
			        + " (OID:"
			        + object.getOid()
			        + ")";
        }
    }

    public static Object toShortStringLazy(ObjectType object) {
		return new Object() {
			@Override
			public String toString() {
				return toShortString(object);
			}
		};
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

	public static Object toShortString(PrismReferenceValue objectRef) {
		return toShortString(toObjectReferenceType(objectRef));
	}

	private static ObjectReferenceType toObjectReferenceType(PrismReferenceValue prv) {
		if (prv != null) {
			ObjectReferenceType ort = new ObjectReferenceType();
			ort.setupReferenceValue(prv);
			return ort;
		} else {
			return null;
		}
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
	public static <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull PrismObject<T> object,
			PrismContext prismContext) {
		return createAssignmentTo(object, prismContext.getDefaultRelation());
	}

	@NotNull
	public static <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull PrismObject<T> object, QName relation) {
		AssignmentType assignment = new AssignmentType(object.getPrismContext());
		if (object.asObjectable() instanceof ResourceType) {
			ConstructionType construction = new ConstructionType(object.getPrismContext());
			construction.setResourceRef(createObjectRef(object, relation));
			assignment.setConstruction(construction);
		} else {
			assignment.setTargetRef(createObjectRef(object, relation));
		}
		return assignment;
	}

	@NotNull
	public static AssignmentType createAssignmentWithConstruction(@NotNull PrismObject<ResourceType> object, ShadowKindType kind,
			String intent, PrismContext prismContext) {
		AssignmentType assignment = new AssignmentType(prismContext);
		ConstructionType construction = new ConstructionType(prismContext);
		construction.setResourceRef(createObjectRef(object, prismContext));
		construction.setKind(kind);
		construction.setIntent(intent);
		assignment.setConstruction(construction);
		return assignment;
	}

	@NotNull
	public static <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull T objectType, QName relation) {
		return createAssignmentTo((PrismObject<T>) objectType.asPrismObject(), relation);
	}

	public static ObjectReferenceType createObjectRef(PrismReferenceValue prv) {
		ObjectReferenceType ort = new ObjectReferenceType();
		ort.setupReferenceValue(prv);
		return ort;
	}

	public static ObjectReferenceType createObjectRefWithFullObject(ObjectType objectType, PrismContext prismContext) {
		if (objectType == null) {
			return null;
		}
        return createObjectRefWithFullObject(objectType.asPrismObject(), prismContext);
    }

	public static ObjectReferenceType createObjectRef(ObjectType object, PrismContext prismContext) {
		if (object == null) {
			return null;
		}
		return createObjectRef(object, prismContext.getDefaultRelation());
    }

	public static ObjectReferenceType createObjectRef(ObjectType objectType, QName relation) {
		if (objectType == null) {
			return null;
		}
        return createObjectRef(objectType.asPrismObject(), relation);
    }

    public static <T extends ObjectType> ObjectReferenceType createObjectRef(PrismObject<T> object, PrismContext prismContext) {
		if (object == null) {
			return null;
		}
        return createObjectRef(object, prismContext.getDefaultRelation());
    }

    public static <T extends ObjectType> ObjectReferenceType createObjectRef(PrismObject<T> object, QName relation) {
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
        ref.setRelation(relation);
        return ref;
    }

    public static <T extends ObjectType> ObjectReferenceType createObjectRefWithFullObject(PrismObject<T> object,
		    PrismContext prismContext) {
        if (object == null) {
            return null;
        }
        ObjectReferenceType ref = createObjectRef(object, prismContext);
        ref.asReferenceValue().setObject(object);
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

    public static List<String> objectReferenceListToOids(Collection<ObjectReferenceType> refList) {
        List<String> rv = new ArrayList<>();
        for (ObjectReferenceType ref : refList) {
            rv.add(ref.getOid());
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

	public static PolyStringType getName(ObjectReferenceType ref) {
    	if (ref == null) {
    		return null;
	    } else if (ref.asReferenceValue().getObject() != null && ref.asReferenceValue().getObject().getName() != null) {
    		return new PolyStringType(ref.asReferenceValue().getObject().getName());
	    } else {
    		return ref.getTargetName();
	    }
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
		Item item = extension.asPrismContainerValue().findItem(ItemName.fromQName(itemName));
    	return item != null ? (T) item.getRealValue() : null;
	}

	public static void normalizeRelation(ObjectReferenceType reference, RelationRegistry relationRegistry) {
    	if (reference != null) {
			reference.setRelation(relationRegistry.normalizeRelation(reference.getRelation()));
		}
	}

	public static void normalizeRelation(PrismReferenceValue reference, RelationRegistry relationRegistry) {
    	if (reference != null) {
			reference.setRelation(relationRegistry.normalizeRelation(reference.getRelation()));
		}
	}

	public static void normalizeAllRelations(PrismValue value, RelationRegistry relationRegistry) {
    	if (value != null) {
			value.accept(createNormalizingVisitor(relationRegistry));
		}
	}

	public static void normalizeAllRelations(Item<?, ?> item, RelationRegistry relationRegistry) {
    	if (item != null) {
			item.accept(createNormalizingVisitor(relationRegistry));
		}
	}

	private static Visitor createNormalizingVisitor(RelationRegistry relationRegistry) {
		return v -> {
			if (v instanceof PrismReferenceValue) {
				normalizeRelation((PrismReferenceValue) v, relationRegistry);
			}
		};
	}

	public static void normalizeFilter(ObjectFilter filter, RelationRegistry relationRegistry) {
		if (filter != null) {
			filter.accept(f -> {
				if (f instanceof RefFilter) {
					emptyIfNull(((RefFilter) f).getValues()).forEach(v -> normalizeRelation(v, relationRegistry));
				}
			});
		}
	}

	public static RelationDefinitionType findRelationDefinition(List<RelationDefinitionType> relationDefinitions, QName qname) {
		for (RelationDefinitionType relation: relationDefinitions) {
			if (QNameUtil.match(qname, relation.getRef())) {
				return relation;
			}
		}
		return null;
	}

	public static boolean referenceMatches(ObjectReferenceType ref, String targetOid, QName targetType, QName relation,
			PrismContext prismContext) {
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
			if (!prismContext.relationMatches(relation, ref.getRelation())) {
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

	public static boolean matchOnOid(ObjectReferenceType ref1, ObjectReferenceType ref2) {
		return ref1 != null && ref2 != null && ref1.getOid() != null && ref2.getOid() != null
				&& ref1.getOid().equals(ref2.getOid());
	}

	public static void mergeExtension(PrismContainerValue<?> dstExtensionContainerValue, PrismContainerValue<?> srcExtensionContainerValue) throws SchemaException {
		for (Item<?,?> srcExtensionItem: emptyIfNull(srcExtensionContainerValue.getItems())) {
			Item<?,?> magicItem = dstExtensionContainerValue.findItem(srcExtensionItem.getElementName());
			if (magicItem == null) {
				//noinspection unchecked
				dstExtensionContainerValue.add(srcExtensionItem.clone());
			}
		}
	}

	public static LocalizableMessage createTechnicalDisplayInformation(PrismObject<?> object, boolean startsWithUppercase) {
		if (object != null) {
			return new LocalizableMessageBuilder()
					.key(SchemaConstants.TECHNICAL_OBJECT_SPECIFICATION_KEY)
					.arg(createTypeDisplayInformation(object.asObjectable().getClass().getSimpleName(), startsWithUppercase))
					.arg(object.asObjectable().getName())
					.arg(object.getOid())
					.build();
		} else {
			return LocalizableMessageBuilder.buildFallbackMessage("?");          // should not really occur!
		}
	}

	public static LocalizableMessage createDisplayInformation(PrismObject<?> object, boolean startsWithUppercase) {
		if (object != null) {
			return new LocalizableMessageBuilder()
					.key(SchemaConstants.OBJECT_SPECIFICATION_KEY)
					.arg(createTypeDisplayInformation(object.asObjectable().getClass().getSimpleName(), startsWithUppercase))
					.arg(object.asObjectable().getName())
					.build();
		} else {
			return LocalizableMessageBuilder.buildFallbackMessage("?");          // should not really occur!
		}
	}

	public static LocalizableMessage createDisplayInformationWithPath(PrismObject<?> object, boolean startsWithUppercase, String path) {
		if (object != null) {
			return new LocalizableMessageBuilder()
					.key(SchemaConstants.OBJECT_SPECIFICATION_WITH_PATH_KEY)
					.arg(createTypeDisplayInformation(object.asObjectable().getClass().getSimpleName(), startsWithUppercase))
					.arg(object.asObjectable().getName())
					.arg(path)
					.build();
		} else {
			return LocalizableMessageBuilder.buildFallbackMessage("?");          // should not really occur!
		}
	}

	public static LocalizableMessage createTypeDisplayInformation(QName type, boolean startsWithUppercase) {
		return createTypeDisplayInformation(type != null ? type.getLocalPart() : null, startsWithUppercase);
	}

	public static LocalizableMessage createTypeDisplayInformation(String objectClassName, boolean startsWithUppercase) {
    	String prefix = startsWithUppercase ? SchemaConstants.OBJECT_TYPE_KEY_PREFIX : SchemaConstants.OBJECT_TYPE_LOWERCASE_KEY_PREFIX;
		return new LocalizableMessageBuilder()
						.key(prefix + objectClassName)
						.fallbackMessage(objectClassName)
						.build();
	}

	@NotNull
	@Deprecated
	public static <O extends ObjectType> Collection<String> getSubtypeValues(@NotNull PrismObject<O> object) {
		return FocusTypeUtil.determineSubTypes(object);
	}
	
	public static <O extends ObjectType> XMLGregorianCalendar getLastTouchTimestamp(PrismObject<O> object) {
		if (object == null) {
			return null;
		}
		MetadataType metadata = object.asObjectable().getMetadata();
		if (metadata == null) {
			return null;
		}
		XMLGregorianCalendar modifyTimestamp = metadata.getModifyTimestamp();
		if (modifyTimestamp != null) {
			return modifyTimestamp;
		}
		return metadata.getCreateTimestamp();
	}

	@NotNull
	public static List<Item<?, ?>> mapToExtensionItems(Map<QName, Object> values, PrismContainerDefinition<?> extensionDefinition,
			PrismContext prismContext) throws SchemaException {
		List<Item<?, ?>> extensionItems = new ArrayList<>();
		for (Map.Entry<QName, Object> entry : values.entrySet()) {
			ItemDefinition<Item<PrismValue, ItemDefinition>> def = extensionDefinition != null
					? extensionDefinition.findItemDefinition(ItemName.fromQName(entry.getKey()))
					: null;
			if (def == null) {
				//noinspection unchecked
				def = prismContext.getSchemaRegistry().findItemDefinitionByElementName(entry.getKey());     // a bit of hack here
				if (def == null) {
					throw new SchemaException("No definition of " + entry.getKey() + " in task extension");
				}
			}
			Item<PrismValue, ItemDefinition> extensionItem = def.instantiate();
			if (entry.getValue() != null) {
				if (entry.getValue() instanceof Collection) {
					for (Object value : (Collection) entry.getValue()) {
						addRealValue(extensionItem, value);
					}
				} else {
					addRealValue(extensionItem, entry.getValue());
				}
			}
			extensionItems.add(extensionItem);
		}
		return extensionItems;
	}

	private static void addRealValue(Item<PrismValue, ItemDefinition> extensionItem, Object value) throws SchemaException {
    	if (value != null) {
		    extensionItem.add(PrismValue.fromRealValue(value).clone());
	    }
	}

	@NotNull
	public static ObjectQuery createManagerQuery(Class<? extends ObjectType> objectTypeClass, String orgOid,
			RelationRegistry relationRegistry, PrismContext prismContext) {
	    Collection<QName> managerRelations = relationRegistry.getAllRelationsFor(RelationKindType.MANAGER);
		if (managerRelations.isEmpty()) {
			LOGGER.warn("No manager relation is defined");
			return QueryBuilder.queryFor(objectTypeClass, prismContext).none().build();
		}

		List<PrismReferenceValue> referencesToFind = new ArrayList<>();
	    for (QName managerRelation : managerRelations) {
	        PrismReferenceValue parentOrgRefVal = new PrismReferenceValueImpl(orgOid, OrgType.COMPLEX_TYPE);
	        parentOrgRefVal.setRelation(managerRelation);
	        referencesToFind.add(parentOrgRefVal);
	    }
	    return QueryBuilder.queryFor(objectTypeClass, prismContext)
	            .item(ObjectType.F_PARENT_ORG_REF).ref(referencesToFind)
	            .build();
	}
}
