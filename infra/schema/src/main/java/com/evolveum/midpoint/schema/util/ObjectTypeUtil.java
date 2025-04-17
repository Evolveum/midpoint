/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Objects;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.lazy.FlyweightClonedItem;
import com.evolveum.midpoint.prism.lazy.LazyXNodeBasedPrismValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

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
    public static <T> @NotNull Collection<T> getExtensionPropertyValuesNotNull(
            Containerable containerable, @NotNull QName propertyQname) {
        Collection<T> values = getExtensionPropertyValues(containerable, propertyQname);
        if (values == null) {
            return new ArrayList<>(0);
        } else {
            return values;
        }
    }

    public static <T> Collection<T> getExtensionPropertyValues(Containerable containerable, QName propertyQname) {
        if (containerable == null) {
            return null;
        }
        var extensionContainer = containerable.asPrismContainerValue().findContainer(ObjectType.F_EXTENSION);
        if (extensionContainer == null) {
            return null;
        }
        //noinspection unchecked
        PrismProperty<T> property = extensionContainer.findProperty(ItemName.fromQName(propertyQname));
        if (property == null) {
            return null;
        }
        return property.getRealValues();
    }

    public static Collection<Referencable> getExtensionReferenceValues(Containerable containerable, QName propertyQname) {
        if (containerable == null) {
            return null;
        }
        var extensionContainer = containerable.asPrismContainerValue().findContainer(ObjectType.F_EXTENSION);
        if (extensionContainer == null) {
            return null;
        }
        PrismReference property = extensionContainer.findReference(ItemName.fromQName(propertyQname));
        if (property == null) {
            return null;
        }
        Collection<Referencable> refs = new ArrayList<>(property.getValues().size());
        for (PrismReferenceValue refVal : property.getValues()) {
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
        return objectTypeType.getElementName().getLocalPart();
    }

    @Deprecated
    public static AssignmentType createAssignmentTo(@NotNull ObjectReferenceType ref, @Nullable PrismContext prismContext) {
        return createAssignmentTo(ref);
    }

    @Contract("null -> null; !null -> !null")
    public static AssignmentType createAssignmentToNullSafe(ObjectReferenceType ref) {
        return ref != null ? createAssignmentTo(ref) : null;
    }

    public static @NotNull AssignmentType createAssignmentTo(@NotNull ObjectReferenceType ref) {
        AssignmentType assignment = new AssignmentType();
        if (QNameUtil.match(ref.getType(), ResourceType.COMPLEX_TYPE)) {
            ConstructionType construction = new ConstructionType();
            construction.setResourceRef(ref);
            assignment.setConstruction(construction);
        } else {
            assignment.setTargetRef(ref);
        }
        return assignment;
    }

    @Deprecated
    public static <T extends ObjectType> AssignmentType createAssignmentTo(
            @NotNull PrismReferenceValue ref, @Nullable PrismContext ignored) {
        return createAssignmentTo(ref);
    }

    @NotNull
    public static <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull PrismReferenceValue ref) {
        ObjectReferenceType ort = new ObjectReferenceType();
        ort.setupReferenceValue(ref);
        return createAssignmentTo(ort);
    }

    @Deprecated
    public static <T extends ObjectType> AssignmentType createAssignmentTo(
            @NotNull String oid, @NotNull ObjectTypes type, @Nullable PrismContext ignored) {
        return createAssignmentTo(oid, type);
    }

    @NotNull
    public static <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull String oid, @NotNull ObjectTypes type) {
        return createAssignmentTo(createObjectRef(oid, type));
    }

    @NotNull
    public static <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull PrismObject<T> object,
            PrismContext prismContext) {
        return createAssignmentTo(object, prismContext.getDefaultRelation());
    }

    public static @NotNull <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull PrismObject<T> object) {
        return createAssignmentTo(object, PrismContext.get().getDefaultRelation());
    }

    @NotNull
    public static <T extends ObjectType> AssignmentType createAssignmentTo(@NotNull PrismObject<T> object, QName relation) {
        AssignmentType assignment = new AssignmentType();
        if (object.asObjectable() instanceof ResourceType) {
            ConstructionType construction = new ConstructionType();
            construction.setResourceRef(createObjectRef(object, relation));
            assignment.setConstruction(construction);
        } else {
            assignment.setTargetRef(createObjectRef(object, relation));
        }
        return assignment;
    }

    @NotNull
    public static AssignmentType createAssignmentWithConstruction(
            @NotNull PrismObject<ResourceType> object, ShadowKindType kind, String intent) {
        AssignmentType assignment = new AssignmentType();
        ConstructionType construction = new ConstructionType();
        construction.setResourceRef(createObjectRef(object));
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
        if (prv == null) {
            return null;
        }
        ObjectReferenceType ort = new ObjectReferenceType();
        ort.setupReferenceValue(prv);
        return ort;
    }

    public static ObjectReferenceType createObjectRef(Referencable value) {
        if (value == null) {
            return null;
        }
        ObjectReferenceType ort = new ObjectReferenceType();
        ort.setupReferenceValue(value.asReferenceValue());
        return ort;
    }

    public static ObjectReferenceType createObjectRef(ObjectType object) {
        if (object == null) {
            return null;
        }
        return createObjectRef(object, PrismContext.get().getDefaultRelation());
    }

    @Deprecated // keeping this as it was quite popular before
    public static ObjectReferenceType createObjectRef(ObjectType object, PrismContext ignored) {
        return createObjectRef(object);
    }

    /**
     * Creates a very basic (OID-only) reference for a given object. Useful e.g. to create references
     * to be used in search filters.
     */
    @Experimental
    public static ObjectReferenceType createOidOnlyObjectRef(ObjectType object) {
        return createObjectRef(object != null ? object.getOid() : null);
    }

    /**
     * @return OID-only object ref. Useful for search filters.
     */
    public static ObjectReferenceType createObjectRef(String oid) {
        if (oid == null) {
            return null;
        } else {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(oid);
            return ref;
        }
    }

    public static ObjectReferenceType createObjectRef(ObjectType objectType, QName relation) {
        if (objectType == null) {
            return null;
        }
        return createObjectRef(objectType.asPrismObject(), relation);
    }

    public static ObjectReferenceType createObjectRef(PrismObject<?> object) {
        return createObjectRef(object, PrismContext.get().getDefaultRelation());
    }

    @Deprecated
    public static ObjectReferenceType createObjectRef(PrismObject<?> object, PrismContext ignored) {
        return createObjectRef(object);
    }

    public static ObjectReferenceType createObjectRef(PrismObject<?> object, QName relation) {
        if (object == null) {
            return null;
        }
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(object.getOid());
        PrismObjectDefinition<?> definition = object.getDefinition();
        if (definition != null) {
            ref.setType(definition.getTypeName());
        }
        ref.setTargetName(object.asObjectable().getName());
        ref.setRelation(relation);
        return ref;
    }

    public static ObjectReferenceType createObjectRefWithFullObject(PrismObject<?> object) {
        if (object != null) {
            ObjectReferenceType ref = createObjectRef(object);
            ref.asReferenceValue().setObject(object);
            return ref;
        } else {
            return null;
        }
    }

    public static ObjectReferenceType createObjectRefWithFullObject(ObjectType object) {
        return createObjectRefWithFullObject(asPrismObject(object));
    }

    @Deprecated
    public static ObjectReferenceType createObjectRefWithFullObject(PrismObject<?> object, PrismContext ignored) {
        return createObjectRefWithFullObject(object);
    }

    //FIXME TODO temporary hack
    public static <T extends ObjectType> ObjectReferenceType createObjectRef(PrismObject<T> object, boolean nameAsDescription) {
        if (object == null) {
            return null;
        }
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(object.getOid());
        if (nameAsDescription) {
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
        ref.setRelation(refVal.getRelation());
        //noinspection unchecked
        PrismObject<T> object = refVal.getObject();
        if (object != null) {
            if (nameAsDescription) {
                ref.setDescription(object.getBusinessDisplayName());
            }
            PrismObjectDefinition<T> definition = object.getDefinition();
            if (definition != null) {
                ref.setType(definition.getTypeName());
            } else {
                ref.setType(refVal.getTargetType());
            }
            ref.setTargetName(PolyString.toPolyStringType(object.getName()));
        } else {
            ref.setType(refVal.getTargetType());
            PolyString targetName = refVal.getTargetName();
            ref.setTargetName(PolyString.toPolyStringType(targetName));
            if (nameAsDescription && targetName != null) {
                ref.setDescription(targetName.getOrig());
            }
        }
        return ref;
    }

    /**
     * This is to create reference clone without actually cloning. (Because the cloning copies
     * also the embedded object.)
     */
    public static ObjectReferenceType createObjectRefCopy(ObjectReferenceType ref) {
        if (ref == null) {
            return null;
        } else {
            ObjectReferenceType copy = new ObjectReferenceType();
            copy.setOid(ref.getOid());
            copy.setDescription(ref.getDescription());
            copy.setDocumentation(ref.getDocumentation());
            copy.setFilter(ref.getFilter());
            copy.setResolutionTime(ref.getResolutionTime());
            copy.setReferentialIntegrity(ref.getReferentialIntegrity());
            copy.setTargetName(ref.getTargetName());
            copy.setType(ref.getType());
            copy.setRelation(ref.getRelation());
            return copy;
        }
    }

    public static ObjectReferenceType createObjectRef(@NotNull String oid, @NotNull ObjectTypes type) {
        return createObjectRef(oid, null, type);
    }

    public static ObjectReferenceType createObjectRef(
            @NotNull String oid, @NotNull ObjectTypes type, @Nullable PrismObject<?> object) {
        var ref = createObjectRef(oid, null, type);
        ref.asReferenceValue().setObject(object);
        return ref;
    }

    @Contract("null, _ -> null; !null, _ -> !null")
    public static ObjectReferenceType createObjectRefNullSafe(@Nullable String oid, @NotNull ObjectTypes type) {
        return oid != null ?
                createObjectRef(oid, null, type) :
                null;
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
//                DOMUtil.fixNamespaceDeclarations(e);
//                return e;
//            }
//        }
//        return null;
    }

    public static void setXsdSchemaDefinition(PrismProperty<SchemaDefinitionType> definitionProperty, Element xsdElement) {
        SchemaDefinitionType schemaDefinition = new SchemaDefinitionType();
        schemaDefinition.setSchema(xsdElement);
        definitionProperty.setRealValue(schemaDefinition);
    }

    public static void assertConcreteType(Class<? extends Objectable> type) {
        // The abstract object types are enumerated here. It should be switched to some flag later on
        if (type.equals(ObjectType.class)) {
            throw new IllegalArgumentException("The type " + type.getName() + " is abstract");
        }
    }

    /**
     * Returns parent object, potentially traversing multiple parent links to get there.
     */
    @Nullable
    public static <O extends Objectable> O getParentObject(@NotNull Containerable containerable) {
        PrismObject<?> object = PrismValueUtil.getParentObject(containerable.asPrismContainerValue());
        //noinspection unchecked
        return object != null ? (O) object.asObjectable() : null;
    }

    /*
    TODO: This one is funny, it takes non-prism in and returns prism.
     It has a single caller, which uses the returned object as prism, but still...
     Only benefit of this method is better information, but try to use @Nullable getParentObject
     and check the return value if non-null is critical.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Deprecated
    public static <O extends Objectable> PrismObject<O> getParentObjectOld(Containerable containerable) {
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

    public static List<PrismReferenceValue> objectReferenceListToPrismReferenceValues(Collection<ObjectReferenceType> refList) {
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

    public static <O extends ObjectType> List<ObjectReferenceType> objectListToReferences(Collection<PrismObject<O>> objects) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (PrismObject<? extends ObjectType> object : objects) {
            rv.add(createObjectRef(object.asObjectable(), PrismContext.get().getDefaultRelation()));
        }
        return rv;
    }

    public static List<ObjectReferenceType> getAsObjectReferenceTypeList(PrismReference prismReference) throws SchemaException {
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (PrismReferenceValue prv : prismReference.getValues()) {
            if (prismReference == null || prismReference.getOid() == null) {
                continue;
            }
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
        @SuppressWarnings("unchecked") final Collection<ObjectDelta<? extends ObjectType>> deltas1 = (Collection) deltas;
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

    public static PolyStringType getDisplayName(Referencable ref) {
        if (ref == null) {
            return null;
        }

        PrismObject object = ref.asReferenceValue().getObject();
        if (object == null) {
            return getName(ref);
        }

        return getDisplayName(object);
    }

    public static PolyStringType getName(Referencable ref) {
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

    public static <T extends Objectable> PrismObject<T> asPrismObject(T objectable) {
        //noinspection unchecked
        return objectable != null ? objectable.asPrismObject() : null;
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

    public static <T> T getExtensionItemRealValue(@NotNull PrismObject<? extends ObjectType> object, @NotNull QName itemName) {
        PrismContainer<?> extension = object.getExtension();
        if (extension == null) {
            return null;
        }
        Item item = extension.findItem(ItemName.fromQName(itemName));
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
            value.acceptVisitor(createNormalizingVisitor(relationRegistry));
        }
    }

    public static void normalizeAllRelations(Item<?, ?> item, RelationRegistry relationRegistry) {
        if (item != null) {
            item.acceptVisitor(createNormalizingVisitor(relationRegistry));
        }
    }

    private static PrismVisitor createNormalizingVisitor(RelationRegistry relationRegistry) {
        return v -> {

            if (v instanceof PrismReferenceValue) {
                normalizeRelation((PrismReferenceValue) v, relationRegistry);
            }
            if (v instanceof PrismValue pv && LazyXNodeBasedPrismValue.isNotMaterialized(pv)) {
                return false;
            }
            return true;
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
        for (RelationDefinitionType relation : relationDefinitions) {
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

    public static <T extends Objectable> T asObjectable(PrismObject<T> prismObject) {
        return prismObject != null ? prismObject.asObjectable() : null;
    }

    public static <T extends Objectable> List<T> asObjectables(Collection<PrismObject<T>> objects) {
        return objects.stream().map(ObjectTypeUtil::asObjectable).collect(Collectors.toList());
    }

    public static boolean matchOnOid(ObjectReferenceType ref1, ObjectReferenceType ref2) {
        return ref1 != null && ref2 != null && ref1.getOid() != null && ref2.getOid() != null
                && ref1.getOid().equals(ref2.getOid());
    }

    public static void mergeExtension(PrismContainerValue<?> dstExtensionPcv, PrismContainerValue<?> srcExtensionPcv)
            throws SchemaException {
        for (Item<?, ?> srcExtensionItem : emptyIfNull(srcExtensionPcv.getItems())) {
            if (dstExtensionPcv.findItem(srcExtensionItem.getElementName()) == null) {
                dstExtensionPcv.add(srcExtensionItem.copy(), false);
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

    /**
     * Creates a localizable designation of an object - its type and name. See {@link #getDescriptiveName(Objectable)}.
     */
    public static LocalizableMessage createDisplayInformation(PrismObject<?> object, boolean startsWithUppercase) {
        if (object != null) {
            Objectable objectable = object.asObjectable();
            return new LocalizableMessageBuilder()
                    .key(SchemaConstants.OBJECT_SPECIFICATION_KEY)
                    .arg(createTypeDisplayInformation(objectable.getClass().getSimpleName(), startsWithUppercase))
                    .arg(getDescriptiveName(objectable))
                    .build();
        } else {
            return LocalizableMessageBuilder.buildFallbackMessage("?");
        }
    }

    /**
     * - User name is in the form of "full name (object name)".
     * - Names for other objects are either "detailed display name" ({@link #getDetailedDisplayName(Objectable)}) or plain name.
     */
    private static Object getDescriptiveName(Objectable object) {
        if (object instanceof UserType) {
            PolyStringType fullName = ((UserType) object).getFullName();
            if (fullName != null) {
                return new LocalizableMessageBuilder()
                        .key(SchemaConstants.USER_DESCRIPTIVE_NAME)
                        .arg(fullName)
                        .arg(object.getName())
                        .build();
            } else {
                return object.getName();
            }
        } else {
            String detailedDisplayName = getDetailedDisplayName(object);
            return StringUtils.isEmpty(detailedDisplayName) ? object.getName() : detailedDisplayName;
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
            return LocalizableMessageBuilder.buildFallbackMessage("?"); // should not really occur!
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

    public static <O extends ObjectType> XMLGregorianCalendar getLastTouchTimestamp(PrismObject<O> object) {
        return object != null ? ValueMetadataTypeUtil.getLastChangeTimestamp(object.asObjectable()) : null;
    }

    @NotNull
    public static List<Item<?, ?>> mapToExtensionItems(Map<QName, Object> values, PrismContainerDefinition<?> extensionDefinition,
            PrismContext prismContext) throws SchemaException {
        List<Item<?, ?>> extensionItems = new ArrayList<>();
        for (Map.Entry<QName, Object> entry : values.entrySet()) {
            ItemDefinition<Item<PrismValue, ItemDefinition<?>>> def = extensionDefinition != null
                    ? extensionDefinition.findItemDefinition(ItemName.fromQName(entry.getKey()))
                    : null;
            if (def == null) {
                //noinspection unchecked
                def = prismContext.getSchemaRegistry().findItemDefinitionByElementName(entry.getKey());     // a bit of hack here
                if (def == null) {
                    throw new SchemaException("No definition of " + entry.getKey() + " in the extension");
                }
            }
            Item<PrismValue, ItemDefinition<?>> extensionItem = def.instantiate();
            if (entry.getValue() != null) {
                if (entry.getValue() instanceof Collection) {
                    for (Object value : (Collection) entry.getValue()) {
                        addRealValue(extensionItem, value, prismContext);
                    }
                } else {
                    addRealValue(extensionItem, entry.getValue(), prismContext);
                }
            }
            extensionItems.add(extensionItem);
        }
        return extensionItems;
    }

    private static void addRealValue(Item<PrismValue, ItemDefinition<?>> extensionItem, Object value,
            PrismContext prismContext) throws SchemaException {
        if (value != null) {
            extensionItem.add(prismContext.itemFactory().createValue(value).clone());
        }
    }

    @NotNull
    public static ObjectQuery createManagerQuery(
            Class<? extends ObjectType> objectTypeClass, String orgOid, RelationRegistry relationRegistry) {
        Collection<QName> managerRelations = relationRegistry.getAllRelationsFor(RelationKindType.MANAGER);
        if (managerRelations.isEmpty()) {
            LOGGER.warn("No manager relation is defined");
            return PrismContext.get().queryFor(objectTypeClass).none().build();
        }

        List<PrismReferenceValue> referencesToFind = new ArrayList<>();
        for (QName managerRelation : managerRelations) {
            PrismReferenceValue parentOrgRefVal = PrismContext.get().itemFactory().createReferenceValue(orgOid, OrgType.COMPLEX_TYPE);
            parentOrgRefVal.setRelation(managerRelation);
            referencesToFind.add(parentOrgRefVal);
        }
        return PrismContext.get().queryFor(objectTypeClass)
                .item(ObjectType.F_PARENT_ORG_REF).ref(referencesToFind)
                .build();
    }

    public static <T extends Objectable> List<PrismObject<T>> keepDistinctObjects(Collection<PrismObject<T>> objects) {
        List<PrismObject<T>> rv = new ArrayList<>();
        Set<String> oids = new HashSet<>(objects.size());
        for (PrismObject<T> object : emptyIfNull(objects)) {
            if (object.getOid() == null) {
                throw new IllegalArgumentException("Unexpected OID-less object");
            } else if (!oids.contains(object.getOid())) {
                rv.add(object);
                oids.add(object.getOid());
            }
        }
        return rv;
    }

    public static List<ObjectReferenceType> keepDistinctReferences(Collection<ObjectReferenceType> references) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        Set<String> oids = new HashSet<>(references.size());
        for (ObjectReferenceType reference : emptyIfNull(references)) {
            if (reference.getOid() == null) {
                throw new IllegalArgumentException("Unexpected OID-less reference");
            } else if (!oids.contains(reference.getOid())) {
                rv.add(reference);
                oids.add(reference.getOid());
            }
        }
        return rv;
    }

    // BEWARE: Checks archetypeRef. This may be a problem during clockwork processing.
    public static <AH extends AssignmentHolderType> boolean hasArchetypeRef(PrismObject<AH> object, String oid) {
        return hasArchetypeRef(object.asObjectable(), oid);
    }

    // BEWARE: Checks archetypeRef. This may be a problem during clockwork processing.
    public static <AH extends AssignmentHolderType> boolean hasArchetypeRef(AH objectable, String oid) {
        for (ObjectReferenceType orgRef : objectable.getArchetypeRef()) {
            if (oid.equals(orgRef.getOid())) {
                return true;
            }
        }
        return false;
    }

    public static List<GuiObjectColumnType> orderCustomColumns(List<GuiObjectColumnType> customColumns) {
        if (customColumns == null || customColumns.size() == 0) {
            return new ArrayList<>();
        }
        List<GuiObjectColumnType> customColumnsList = new ArrayList<>(customColumns);
        List<String> previousColumnValues = new ArrayList<>();
        previousColumnValues.add(null);
        previousColumnValues.add("");

        Map<String, String> columnRefsMap = new HashMap<>();
        for (GuiObjectColumnType column : customColumns) {
            columnRefsMap.put(column.getName(), column.getPreviousColumn() == null ? "" : column.getPreviousColumn());
        }

        List<String> temp = new ArrayList<>();
        int index = 0;
        while (index < customColumns.size()) {
            int sortFrom = index;
            for (int i = index; i < customColumnsList.size(); i++) {
                GuiObjectColumnType column = customColumnsList.get(i);
                if (previousColumnValues.contains(column.getPreviousColumn()) ||
                        !columnRefsMap.containsKey(column.getPreviousColumn())) {
                    Collections.swap(customColumnsList, index, i);
                    index++;
                    temp.add(column.getName());
                }
            }
            if (temp.size() == 0) {
                temp.add(customColumnsList.get(index).getName());
                index++;
            }
            if (index - sortFrom > 1) {
                customColumnsList.subList(sortFrom, index - 1)
                        .sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()));
            }
            previousColumnValues.clear();
            previousColumnValues.addAll(temp);
            temp.clear();
        }
        return customColumnsList;
    }

    public static PrismContainerValue<ExtensionType> getExtensionContainerValue(Containerable containerable) {
        if (containerable == null) {
            return null;
        }
        PrismContainer extensionContainer = containerable.asPrismContainerValue().findContainer(ObjectType.F_EXTENSION);
        if (extensionContainer == null) {
            return null;
        }
        // note that here we create empty extension if there's none
        //noinspection unchecked
        return ((PrismContainerValue<ExtensionType>) extensionContainer.getValue());
    }

    public static @Nullable OperationResultStatusType getFetchStatus(@NotNull ObjectType object) {
        var fetchResult = object.getFetchResult();
        return fetchResult != null ? fetchResult.getStatus() : null;
    }

    public static boolean hasFetchError(@NotNull PrismObject<? extends ObjectType> object) {
        return OperationResultUtil.isError(
                getFetchStatus(object.asObjectable()));
    }

    public static void recordFetchError(ObjectType object, OperationResult result) {
        OperationResultType resultBean = result.createBeanReduced();
        assert OperationResultUtil.isError(resultBean.getStatus());
        object.setFetchResult(resultBean);
    }

    public static Collection<ObjectReferenceType> createObjectRefs(Collection<PrismReferenceValue> values) {
        return values.stream()
                .map(ObjectTypeUtil::createObjectRef)
                .collect(Collectors.toList());
    }

    /**
     * Returns display name for given object, e.g. fullName for a user, displayName for a role,
     * and more detailed description for a shadow. TODO where exactly does this method belong?
     */
    public static String getDetailedDisplayName(PrismObject<?> object) {
        return getDetailedDisplayName(
                asObjectable(object));
    }

    public static String getDetailedDisplayName(Objectable objectable) {
        if (objectable instanceof UserType) {
            return getOrig(((UserType) objectable).getFullName());
        } else if (objectable instanceof AbstractRoleType) {
            return getOrig(((AbstractRoleType) objectable).getDisplayName());
        } else if (objectable instanceof ShadowType) {
            ShadowType shadow = (ShadowType) objectable;
            String objectName = getOrig(shadow.getName());
            QName oc = shadow.getObjectClass();
            String ocName = oc != null ? oc.getLocalPart() : null;
            // This is dangerous (in general), as the PolyString localization information is lost.
            // But it's OK for shadows.
            return objectName + " (" + shadow.getKind() + " - " + shadow.getIntent() + " - " + ocName + ")";
        } else {
            return null;
        }
    }

    /**
     * Returns the type name for an object.
     * (This really belongs somewhere else, not here.)
     */
    @Contract("!null -> !null; null -> null")
    public static QName getObjectTypeName(ObjectType object) {
        if (object == null) {
            return null;
        }
        PrismObjectDefinition<?> objectDef = object.asPrismObject().getDefinition();
        if (objectDef != null) {
            return objectDef.getTypeName();
        }
        Class<? extends Objectable> clazz = object.asPrismObject().getCompileTimeClass();
        if (clazz == null) {
            throw new IllegalStateException("No compile-time class for " + object);
        }
        PrismObjectDefinition<?> defFromRegistry =
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
        if (defFromRegistry != null) {
            return defFromRegistry.getTypeName();
        } else {
            return ObjectType.COMPLEX_TYPE;
        }
    }

    /** A convenience variant of {@link #getObjectTypeName(ObjectType)}. */
    @Contract("!null -> !null; null -> null")
    public static QName getObjectTypeName(PrismObject<? extends ObjectType> object) {
        return getObjectTypeName(asObjectable(object));
    }

    public static boolean isIndestructible(@NotNull ObjectType object) {
        return Boolean.TRUE.equals(object.isIndestructible());
    }

    public static boolean isIndestructible(@NotNull PrismObject<? extends ObjectType> object) {
        return isIndestructible(object.asObjectable());
    }

    // Currently ignoring reference definition (target type limitations)
    public static Class<? extends ObjectType> getTargetClassFromReference(@NotNull ObjectReferenceType ref) {
        return getTargetClassFromReference(ref, ObjectType.class);
    }

    public static Class<? extends ObjectType> getTargetClassFromReference(
            @NotNull ObjectReferenceType ref, @NotNull Class<? extends ObjectType> defaultType) {
        var type = ref.getType();
        if (type != null) {
            return ObjectTypes.getObjectTypeClass(type);
        } else {
            return defaultType;
        }
    }

    public static String getOid(ObjectType object) {
        return object != null ? object.getOid() : null;
    }

    /**
     * Converts {@link PrismContainerValue} to {@link PrismObjectValue} based {@link ObjectType} as a workaround for MID-8522.
     *
     * TEMPORARY CODE
     */
    public static ObjectType fix(ObjectType objectable) {
        if (objectable == null) {
            return null;
        }
        PrismContainerValue<?> pcv = objectable.asPrismContainerValue();
        if (pcv instanceof PrismObjectValue) {
            return objectable;
        }

        PrismObjectValue<?> pov;
        try {
            pov = PrismContext.get().getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(objectable.getClass())
                    .instantiate()
                    .createNewValue();
            for (Item<?, ?> item : pcv.getItems()) {
                pov.add(item.clone());
            }
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when fixing " + objectable);
        }
        return (ObjectType) pov.asObjectable();
    }

    public static @Nullable ObjectType asObjectTypeIfPossible(@Nullable PrismValue value) {
        if (value == null) {
            return null;
        }
        // Intentionally not checking for instance-of PrismObjectValue, as some prism object values can come disguised
        // as PrismContainerValue instances. (Hopefully to be fixed some day.)
        Object realValue = value.getRealValueIfExists();
        return realValue instanceof ObjectType ? (ObjectType) realValue : null;
    }

    /**
     * Returns "identity collection" i.e. equivalent objects can be present there, differing in their identity.
     *
     * Throws an exception when a root object cannot be determined.
     */
    public static <C extends Containerable> Collection<PrismObject<? extends ObjectType>> getRootsForContainerables(
            @NotNull Collection<C> containerables) {
        return getRoots(
                containerables.stream()
                        .map(c -> c.asPrismContainerValue()));
    }

    /** Returns containerables that are not rooted in a {@link PrismObject}. */
    public static @NotNull <C extends Containerable> List<C> getDetachedContainerables(@NotNull Collection<C> containerables) {
        return containerables.stream()
                .filter(c -> !c.asPrismContainerValue().getRootValue().isObjectable())
                .collect(Collectors.toList());
    }

    /**
     * As {@link #getRootsForContainerables(Collection)} but for {@link ObjectReferenceType} values.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Collection<PrismObject<? extends ObjectType>> getRootsForReferences(
            @NotNull Collection<ObjectReferenceType> references) {
        return getRoots(
                references.stream()
                        .map(ref -> ref.asReferenceValue()));
    }

    /**
     * As {@link #getRootsForContainerables(Collection)} but for arbitrary prism values.
     */
    @SuppressWarnings("unused")
    public static Collection<PrismObject<? extends ObjectType>> getRootsForValues(@NotNull Collection<PrismValue> values) {
        return getRoots(values.stream());
    }

    private static <C extends Containerable> Collection<PrismObject<? extends ObjectType>> getRoots(Stream<PrismValue> values) {
        IdentityHashMap<PrismObject<? extends ObjectType>, Boolean> roots = new IdentityHashMap<>();
        values.forEach(value -> {
            var rootObject = asObjectTypeIfPossible(value.getRootValue());
            if (rootObject != null) {
                roots.put(rootObject.asPrismObject(), true);
            } else {
                throw new IllegalStateException("A value without root object: " + value);
            }
        });
        return roots.keySet();
    }

    public static boolean isObjectable(@NotNull Class<?> type) {
        return Objectable.class.isAssignableFrom(type);
    }

    public static <O extends ObjectType> PrismObjectValue<O> getValue(PrismObject<O> object) {
        return object != null ? object.getValue() : null;
    }

    public static void checkIn(@NotNull Containerable c, @NotNull Class<?> expectedRootType) {
        checkIn(c.asPrismContainerValue(), expectedRootType);
    }

    private static void checkIn(PrismValue value, @NotNull Class<?> expectedRootType) {
        var rootValue = value.getRootValue().getRealValueIfExists();
        if (rootValue == null || !expectedRootType.isAssignableFrom(rootValue.getClass())) {
            throw new IllegalStateException(
                    "Value not embedded within a %s but in '%s': %s"
                            .formatted(expectedRootType.getSimpleName(), MiscUtil.getValueWithClass(rootValue), value));
        }
    }

    public static List<ObjectReferenceType> getAssignedArchetypeRefs(@NotNull AssignmentHolderType object) {
        return getAssignedArchetypeRefStream(object)
                .toList();
    }

    public static boolean hasAssignedArchetype(@NotNull AssignmentHolderType object) {
        return !getAssignedArchetypeOids(object).isEmpty();
    }

    public static Set<String> getAssignedArchetypeOids(@NotNull AssignmentHolderType object) {
        return getAssignedArchetypeRefStream(object)
                .map(ref -> ref.getOid())
                .collect(Collectors.toSet());
    }

    private static Stream<ObjectReferenceType> getAssignedArchetypeRefStream(@NotNull AssignmentHolderType object) {
        return object.getAssignment().stream()
                .map(AssignmentType::getTargetRef)
                .filter(Objects::nonNull)
                .filter(ref -> QNameUtil.match(ArchetypeType.COMPLEX_TYPE, ref.getType()));
    }

    @FunctionalInterface
    private interface ExtensionItemRemover {
        // Removes item (known from the context) from the extension
        void removeFrom(PrismContainerValue<?> extension);
    }

    @FunctionalInterface
    private interface ExtensionItemCreator {
        // Creates item (known from the context) holding specified real values
        Item<?, ?> create(PrismContainerValue<?> extension, List<?> realValues) throws SchemaException;
    }

    public static void setExtensionPropertyRealValues(
            PrismContext ignored, PrismContainerValue<?> parent, ItemName propertyName, Object... values) throws SchemaException {
        setExtensionPropertyRealValues(parent, propertyName, values);
    }

    public static void setExtensionPropertyRealValues(
            PrismContainerValue<?> parent, ItemName propertyName, Object... values) throws SchemaException {
        setExtensionItemRealValues(parent,
                extension -> extension.removeProperty(propertyName),
                (extension, realValues) -> {
                    PrismProperty<Object> property = findPropertyDefinition(extension, propertyName)
                            .instantiate();
                    realValues.forEach(property::addRealValue);
                    return property;
                }, values);
    }

    private static @NotNull PrismPropertyDefinition<Object> findPropertyDefinition(
            PrismContainerValue<?> extension, ItemName propertyName) {
        if (extension.getDefinition() != null) {
            PrismPropertyDefinition<Object> definitionInExtension = extension.getDefinition().findPropertyDefinition(propertyName);
            if (definitionInExtension != null) {
                return definitionInExtension;
            }
        }
        //noinspection unchecked
        PrismPropertyDefinition<Object> globalDefinition = PrismContext.get().getSchemaRegistry()
                .findPropertyDefinitionByElementName(propertyName);
        if (globalDefinition != null) {
            return globalDefinition;
        }

        throw new IllegalStateException("Cannot determine definition for " + propertyName + " in " + extension + " nor globally");
    }

    public static void setExtensionContainerRealValues(
            PrismContext prismContext, PrismContainerValue<?> parent, ItemName containerName, Object... values) throws SchemaException {
        setExtensionItemRealValues(parent,
                extension -> extension.removeContainer(containerName),
                (extension, realValues) -> {
                    PrismContainer<Containerable> container = getContainerDefinition(prismContext, extension, containerName)
                            .instantiate();
                    for (Object realValue : realValues) {
                        //noinspection unchecked
                        container.add(((Containerable) realValue).asPrismContainerValue());
                    }
                    return container;
                }, values);
    }

    private static @NotNull PrismContainerDefinition<Containerable> getContainerDefinition(PrismContext prismContext,
            PrismContainerValue<?> extension, ItemName containerName) {
        if (extension.getDefinition() != null) {
            PrismContainerDefinition<Containerable> definitionInExtension =
                    extension.getDefinition().findContainerDefinition(containerName);
            if (definitionInExtension != null) {
                return definitionInExtension;
            }
        }
        PrismContainerDefinition<Containerable> globalDefinition = prismContext.getSchemaRegistry()
                .findContainerDefinitionByElementName(containerName);
        if (globalDefinition != null) {
            return globalDefinition;
        }

        throw new IllegalStateException("Cannot determine definition for " + containerName + " in " + extension + " nor globally");
    }

    private static void setExtensionItemRealValues(PrismContainerValue<?> parent, ExtensionItemRemover itemRemover,
            ExtensionItemCreator itemCreator, Object... values) throws SchemaException {

        // To cater for setExtensionPropertyRealValues(..., null)
        List<?> refinedValues = Arrays.stream(values)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        PrismContainerValue<?> extension;
        PrismContainer<Containerable> extensionContainer = parent.findContainer(ObjectType.F_EXTENSION);
        if (extensionContainer != null) {
            extension = extensionContainer.getValue();
        } else {
            if (refinedValues.isEmpty()) {
                return; // nothing to do here
            } else {
                extension = parent.findOrCreateContainer(ObjectType.F_EXTENSION).getValue();
            }
        }

        if (refinedValues.isEmpty()) {
            itemRemover.removeFrom(extension);
        } else {
            extension.addReplaceExisting(itemCreator.create(extension, refinedValues));
        }
    }

//    public static <T> T getExtensionItemRealValue(Containerable parent, ItemName name) {
//        return getExtensionItemRealValue(parent.asPrismContainerValue(), name);
//    }

    public static <T> T getExtensionItemRealValue(PrismContainerValue<?> parent, ItemPath path) {
        Item<?, ?> item = parent.findItem(ItemPath.create(ObjectType.F_EXTENSION, path));
        if (item != null) {
            //noinspection unchecked
            return (T) item.getRealValue();
        } else {
            return null;
        }
    }

    // TODO it would be better to return Set<String>
    public static List<String> getOids(List<? extends Objectable> objectables) {
        return objectables.stream()
                .map(Objectable::getOid)
                .collect(Collectors.toList());
    }

    // TODO it would be better to return Set<String>
    public static List<String> getOidsFromPrismObjects(List<? extends PrismObject<?>> prismObjects) {
        return prismObjects.stream()
                .map(PrismObject::getOid)
                .collect(Collectors.toList());
    }

    public static Set<String> getOidsFromRefs(Collection<? extends Referencable> refs) {
        return refs.stream()
                .map(ref -> ref.getOid())
                .collect(Collectors.toSet());
    }

    public static ObjectType getEmbeddedObjectBean(ObjectReferenceType ref) {
        if (ref == null) {
            return null;
        } else {
            return asObjectable(ref.getObject());
        }
    }

    public static AssignmentType getAssignment(ObjectType object, long id) {
        if (!(object instanceof AssignmentHolderType assignmentHolder)) {
            return null;
        }
        return assignmentHolder.getAssignment().stream()
                .filter(a -> a.getId() != null && a.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public static @NotNull AssignmentType getAssignmentRequired(ObjectType object, long id) {
        return stateNonNull(
                getAssignment(object, id),
                "No assignment with ID %d in %s", id, object);
    }

    /**
     * This method selects only really effective `effectiveMarkRef` values.
     *
     * @see MarkTypeUtil#isEffective(ObjectReferenceType)
     */
    public static @NotNull List<ObjectReferenceType> getReallyEffectiveMarkRefs(@NotNull ObjectType object) {
        return getReallyEffectiveMarkRefStream(object)
                .toList();
    }

    /** @see MarkTypeUtil#isEffective(ObjectReferenceType) */
    @SuppressWarnings("WeakerAccess")
    public static @NotNull Stream<ObjectReferenceType> getReallyEffectiveMarkRefStream(@NotNull ObjectType object) {
        return object.getEffectiveMarkRef().stream()
                .filter(MarkTypeUtil::isEffective);
    }

    public static boolean hasEffectiveMarkRef(@NotNull ObjectType object, @NotNull String markOid) {
        return getReallyEffectiveMarkRefStream(object)
                .anyMatch(ref -> markOid.equals(ref.getOid()));
    }

    /**
     * Extracts type class from the {@link Referencable}. Expected type is provided for cases when the type in reference
     * is missing or (for any reason) incorrect.
     *
     * Useful e.g. for resolving references.
     */
    public static <O extends ObjectType> @NotNull Class<O> getTypeClass(
            @NotNull Referencable ref, @NotNull Class<O> expectedType) throws SchemaException {
        QName typeName = ref.getType();
        if (typeName != null) {
            Class<?> typeClass = PrismContext.get().getSchemaRegistry().determineCompileTimeClass(typeName);
            if (typeClass != null) {
                if (expectedType.isAssignableFrom(typeClass)) {
                    //noinspection unchecked
                    return (Class<O>) typeClass;
                } else {
                    // Maybe the type in the reference is too generic?
                    // E.g. the reference is to FocusType, but we expect UserType.
                    // In such cases, we simply return the expected type.
                }
            }
        }
        return expectedType;
    }
}
