/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.marshaller;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BeanMarshaller implements SchemaRegistry.InvalidationListener {

    private static final Trace LOGGER = TraceManager.getTrace(BeanMarshaller.class);

    public static final String DEFAULT_PLACEHOLDER = "##default";

    @NotNull private final PrismBeanInspector inspector;
    @NotNull private final PrismContext prismContext;
    @NotNull private final Map<Class<?>,Marshaller> specialMarshallers = new HashMap<>();

    @NotNull private final Map<QName, Boolean> canProcessCache = new ConcurrentHashMap<>();

    @FunctionalInterface
    private interface Marshaller {
        XNodeImpl marshal(Object bean, SerializationContext sc) throws SchemaException;
    }

    private void createSpecialMarshallerMap() {
        specialMarshallers.put(XmlAsStringType.class, this::marshalXmlAsStringType);
        specialMarshallers.put(SchemaDefinitionType.class, this::marshalSchemaDefinition);
        specialMarshallers.put(ProtectedByteArrayType.class, this::marshalProtectedDataType);
        specialMarshallers.put(ProtectedStringType.class, this::marshalProtectedDataType);
        specialMarshallers.put(ItemPathType.class, this::marshalItemPathType);
        specialMarshallers.put(RawType.class, this::marshalRawType);
        specialMarshallers.put(PolyString.class, this::marshalPolyString);
        specialMarshallers.put(PolyStringType.class, this::marshalPolyString);
    }

    public BeanMarshaller(@NotNull PrismContext prismContext, @NotNull PrismBeanInspector inspector) {
        this.prismContext = prismContext;
        this.inspector = inspector;
        createSpecialMarshallerMap();
        getSchemaRegistry().registerInvalidationListener(this);
    }

    @Override
    public void invalidate() {
        canProcessCache.clear();
    }

    @Nullable
    public <T> XNodeImpl marshall(@Nullable T bean) throws SchemaException {
        return marshall(bean, null);
    }

    @Nullable
    public XNodeImpl marshall(@Nullable Object inputBean, @Nullable SerializationContext ctx) throws SchemaException {
        try {
            if (inputBean == null) {
                return null;
            }
            // Special hack (MID-5803) -- we should NEVER get PrismValue here; but not enough time to fix this right now
            Object bean;
            if (inputBean instanceof PrismValue) {
                bean = ((PrismValue) inputBean).getRealValue();
                if (bean == null) {
                    return null;
                }
            } else {
                bean = inputBean;
            }

            Marshaller marshaller = specialMarshallers.get(bean.getClass());
            if (marshaller != null) {
                return marshaller.marshal(bean, ctx);
            }

            if (bean instanceof Containerable) {
                return (XNodeImpl) prismContext.xnodeSerializer().context(ctx).serializeRealValue(bean, new QName("dummy"))
                        .getSubnode();
            } else if (bean instanceof Enum) {
                return marshalEnum((Enum<?>) bean, ctx);
            } else if (bean.getClass().getAnnotation(XmlType.class) != null) {
                return marshalXmlType(bean, ctx);
            } else if (bean instanceof Referencable) {
                // i.e. we are Referencable but not ObjectReferenceType (e.g. DefaultReferencableImpl)
                PrismReferenceValue referenceValue = ((Referencable) bean).asReferenceValue();
                XNodeImpl xnode = (XNodeImpl) prismContext.xnodeSerializer().context(ctx)
                        .serialize(referenceValue, new QName("dummy"))
                        .getSubnode();
                xnode.setTypeQName(ObjectUtils.defaultIfNull(prismContext.getDefaultReferenceTypeName(), ObjectReferenceType.COMPLEX_TYPE));
                xnode.setExplicitTypeDeclaration(true);     // probably not much correct, but...
                return xnode;
            } else if (bean instanceof RawType && ((RawType) bean).getXnode() != null) {
                return (XNodeImpl) ((RawType) bean).getXnode();
            } else {
                return marshalToPrimitive(bean, ctx);
            }
        } catch (Throwable t) {
            LOGGER.error("Couldn't marshal an object:\n{}", inputBean, t);
            throw t;
        }
    }

    private XNodeImpl marshalToPrimitive(Object bean, SerializationContext ctx) {
        return createPrimitiveXNode(bean, null, false);
    }

    private XNodeImpl marshalXmlType(Object bean, SerializationContext ctx) throws SchemaException {

        Class<?> beanClass = bean.getClass();
        ComplexTypeDefinition ctd = getSchemaRegistry()
                .findTypeDefinitionByCompileTimeClass(beanClass, ComplexTypeDefinition.class);

        Field valueField = XNodeProcessorUtil.findXmlValueField(beanClass);
        if (valueField != null) {
            return marshallBeanToPrimitive(bean, ctx, valueField);
        }

        if (ctd != null && ctd.isListMarker()) {
            return marshalHeterogeneousList(bean, ctx);
        } else {
            return marshalXmlTypeToMap(bean, ctx, ctd);
        }
    }

    /**
     * For cases when XSD complex type has a simple content. In that case the resulting class has @XmlValue annotation.
     */
    private <T> PrimitiveXNodeImpl<T> marshallBeanToPrimitive(Object bean, SerializationContext ctx, Field valueField) throws SchemaException {
        if (!valueField.isAccessible()) {
            valueField.setAccessible(true);
        }
        Object value;
        try {
            value = valueField.get(bean);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new SchemaException("Cannot get primitive value from field " + valueField.getName() + " of bean " + bean + ": "+e.getMessage(), e);
        }

        @SuppressWarnings("unchecked")
        PrimitiveXNodeImpl<T> xnode = new PrimitiveXNodeImpl<>((T) value);
        Class<?> fieldType = valueField.getType();
        QName xsdType = XsdTypeMapper.toXsdType(fieldType);
        xnode.setTypeQName(xsdType);
        return xnode;
    }

    private XNodeImpl marshalHeterogeneousList(Object bean, SerializationContext ctx) throws SchemaException {
        // structurally similar to a specific path through marshalXmlTypeToMap
        Class<?> beanClass = bean.getClass();
        QName propertyName = getHeterogeneousListPropertyName(beanClass);
        Method getter = inspector.findPropertyGetter(beanClass, propertyName.getLocalPart());
        Object getterResult = getValue(bean, getter, propertyName.getLocalPart());
        if (!(getterResult instanceof Collection)) {
            throw new IllegalStateException("Heterogeneous list property " + propertyName
                    + " does not contain a collection but " + MiscUtil.getObjectName(getterResult));
        }
        ListXNodeImpl xlist = new ListXNodeImpl();
        for (Object value : (Collection<?>) getterResult) {
            if (!(value instanceof JAXBElement)) {
                throw new IllegalStateException("Heterogeneous list contains a value that is not a JAXBElement: " + value);
            }
            JAXBElement<?> jaxbElement = (JAXBElement<?>) value;
            Object realValue = jaxbElement.getValue();
            if (realValue == null) {
                throw new IllegalStateException("Heterogeneous list contains a null value");        // TODO
            }
            QName typeName = inspector.determineTypeForClass(realValue.getClass());
            XNodeImpl marshaled = marshallValue(realValue, typeName, false, ctx);
            marshaled.setElementName(jaxbElement.getName());
            setExplicitTypeDeclarationIfNeededForHeteroList(marshaled, realValue);
            xlist.add(marshaled);
        }
        return xlist;
    }

    private XNodeImpl marshalXmlTypeToMap(Object bean, SerializationContext ctx, @Nullable ComplexTypeDefinition ctd) throws SchemaException {

        Class<?> beanClass = bean.getClass();
        MapXNodeImpl xmap;
        if (bean instanceof SearchFilterType) {
            // this hack is here because of c:ConditionalSearchFilterType - it is analogous to situation when unmarshalling this type (TODO: rework this in a nicer way)
            xmap = marshalSearchFilterType((SearchFilterType) bean);
            if (SearchFilterType.class.equals(bean.getClass())) {
                return xmap;        // nothing more to serialize; otherwise we continue, because in that case we deal with a subclass of SearchFilterType
            }
        } else {
            xmap = new MapXNodeImpl();
        }

        String namespace = inspector.determineNamespace(beanClass);
        if (namespace == null) {
            throw new IllegalArgumentException("Cannot determine namespace of "+beanClass);
        }

        List<String> propOrder = inspector.getPropOrder(beanClass);

        for (String fieldName: propOrder) {
            Method getter = inspector.findPropertyGetter(beanClass, fieldName);
            if (getter == null) {
                throw new IllegalStateException("No getter for field "+fieldName+" in "+beanClass);
            }
            Object getterResult = getValue(bean, getter, fieldName);
            if (getterResult == null) {
                continue;
            }

            Field field = inspector.findPropertyField(beanClass, fieldName);
            Map.Entry<QName,XNodeImpl> marshalled = marshallField(getterResult, field, fieldName, getter, namespace, ctd, beanClass, ctx);
            if (marshalled != null) {
                xmap.put(marshalled.getKey(), marshalled.getValue());
            }
        }
        return xmap;
    }

    private Map.Entry<QName, XNodeImpl> marshallField(Object getterResult,Field field, String fieldName, Method getter, String namespace, @Nullable ComplexTypeDefinition ctd, Class<?> beanClass, SerializationContext ctx) throws SchemaException {

            boolean isAttribute = inspector.isAttribute(field, getter);
            QName elementName = inspector.findFieldElementQName(fieldName, beanClass, namespace);
            ItemDefinition<?> propDef = ctd != null ? ctd.findLocalItemDefinition(elementName) : null;

            if (getterResult instanceof Collection<?>) {
                Collection<?> collection = (Collection<?>) getterResult;
                if (collection.isEmpty()) {
                    return null;
                }
                Object getterResultValue = collection.iterator().next();
                if (getterResultValue == null) {
                    return null;
                }
                // elementName will be determined from the first item on the list
                // TODO make sure it will be correct with respect to other items as well!
                if (getterResultValue instanceof JAXBElement && ((JAXBElement<?>) getterResultValue).getName() != null) {
                    elementName = ((JAXBElement<?>) getterResultValue).getName();
                    // We should update propDef with substitution if possible
                }

                ListXNodeImpl xlist = new ListXNodeImpl();
                for (Object value: collection) {
                    if (value == null) {
                        continue;
                    }
                    XNodeImpl marshaled = marshalSingleValue(value, field, fieldName, isAttribute, ctx, getter, propDef);
                    xlist.add(marshaled);
                }
                return new AbstractMap.SimpleEntry<>(elementName, xlist);
            }
            // Value is not collection
            if (getterResult instanceof JAXBElement) {
                elementName = ((JAXBElement<?>) getterResult).getName();
            }
            XNodeImpl marshaled = marshalSingleValue(getterResult, field, fieldName, isAttribute, ctx, getter, propDef);
            if (marshaled != null) {
                return new AbstractMap.SimpleEntry<>(elementName, marshaled);
            }
            return null;
    }

    private XNodeImpl marshalSingleValue(Object value, Field field, String namespace, boolean isAttribute, SerializationContext ctx, Method getter, ItemDefinition<?> propDef) throws SchemaException {
        Object valueToMarshal = value;
        if (value instanceof JAXBElement) {
            valueToMarshal = ((JAXBElement<?>) value).getValue();
        }
        QName typeName = inspector.findTypeName(field, valueToMarshal.getClass(), namespace);
        // note: fieldTypeName is used only for attribute values here (when constructing PrimitiveXNode)
        XNodeImpl marshaled = marshallValue(valueToMarshal, typeName, isAttribute, ctx);
        updateExplicitType(marshaled, valueToMarshal.getClass(), typeName, unwrappedReturnType(getter), propDef);
        return marshaled;
    }


    private Object getValue(Object bean, Method getter, String fieldOrPropertyName) {
        Object getterResult;
        try {
            getterResult = getter.invoke(bean);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new SystemException("Cannot invoke method for field/property "+fieldOrPropertyName+" in "+bean.getClass()+": "+e.getMessage(), e);
        }
        return getterResult;
    }

    private XNodeImpl marshalEnum(Enum<?> enumValue, SerializationContext ctx) {
        var enumClass = enumValue.getClass();
        String enumStringValue = inspector.findEnumFieldValue(enumClass, enumValue.toString());
        if (StringUtils.isEmpty(enumStringValue)){
            enumStringValue = enumValue.toString();
        }
        QName fieldTypeName = inspector.findTypeName(null, enumClass, DEFAULT_PLACEHOLDER);
        return createPrimitiveXNode(enumStringValue, fieldTypeName, false);

    }

    private XNodeImpl marshalXmlAsStringType(Object bean, SerializationContext sc) {
        PrimitiveXNodeImpl<String> xprim = new PrimitiveXNodeImpl<>();
        xprim.setValue(((XmlAsStringType) bean).getContentAsString(), DOMUtil.XSD_STRING);
        return xprim;
    }

    public void revive(Object bean, final PrismContext prismContext) throws SchemaException {
        Handler<Object> visitor = o -> {
            if (o instanceof Revivable) {
                try {
                    ((Revivable)o).revive(prismContext);
                } catch (SchemaException e) {
                    throw new TunnelException(e);
                }
            }
            return true;
        };
        try {
            visit(bean, visitor);
        } catch (TunnelException te) {
            SchemaException e = (SchemaException) te.getCause();
            throw e;
        }
    }

    public void visit(Object bean, Handler<Object> handler) {
        if (bean == null) {
            return;
        }

        Class<? extends Object> beanClass = bean.getClass();

        handler.handle(bean);

        if (beanClass.isEnum() || beanClass.isPrimitive()){
            //nothing more to do
            return;
        }

        // TODO: implement special handling for RawType, if necessary (it has no XmlType annotation any more)

        XmlType xmlType = beanClass.getAnnotation(XmlType.class);
        if (xmlType == null) {
            // no @XmlType annotation, we are not interested to go any deeper
            return;
        }

        List<String> propOrder = inspector.getPropOrder(beanClass);
        for (String fieldName: propOrder) {
            Method getter = inspector.findPropertyGetter(beanClass, fieldName);
            if (getter == null) {
                throw new IllegalStateException("No getter for field "+fieldName+" in "+beanClass);
            }
            Object getterResult = getValue(bean, getter, fieldName);

            if (getterResult == null) {
                continue;
            }

            if (getterResult instanceof Collection<?>) {
                Collection<?> col = (Collection<?>)getterResult;
                if (col.isEmpty()) {
                    continue;
                }

                for (Object element: col) {
                    visitValue(element, handler);

                }
            } else {
                visitValue(getterResult, handler);
            }
        }
    }

    private void visitValue(Object element, Handler<Object> handler) {
        Object elementToMarshall = element;
        if (element instanceof JAXBElement){
            elementToMarshall = ((JAXBElement<?>) element).getValue();
        }
        visit(elementToMarshall, handler);
    }

    private QName typeNameForClass(Class<?> jaxbClazz) {
        // Primitive types
        QName typeName = XsdTypeMapper.getJavaToXsdMapping(jaxbClazz);
        if (typeName != null) {
            return typeName;
        }
        return inspector.determineTypeForClass(jaxbClazz);
    }

    private void updateExplicitType(XNodeImpl marshaled, Class<? extends Object> valueClazz, QName valueType, Class<?> jaxbClazz,
            ItemDefinition<?> propDef) {
        if(marshaled == null || marshaled.isImmutable()) {
            // RawType now return immutables
            return;
        }
        if (valueType == null) {
            // the idea is not to overwrite pre-existing explicit type declaration with null value
            return;
        }

        if (marshaled.isExplicitTypeDeclaration()) {
            // Explicit type already set
            return;
        }

        if (marshaled.getTypeQName() == null && valueType != null) {
            marshaled.setTypeQName(valueType);
        }

        QName schemaType = schemaTypeForClass(propDef, jaxbClazz);
        if (Objects.equals(schemaType, valueType)) {
            return;
        }
        if (DOMUtil.XSD_ANYTYPE.equals(schemaType)) {
            // Field is anyType, lets record type to be safe.
            marshaled.setExplicitTypeDeclaration(true);
        }
        if (!valueClazz.equals(jaxbClazz) && jaxbClazz.isAssignableFrom(valueClazz)) {
            // It is subclass
            marshaled.setExplicitTypeDeclaration(true);
        }

    }

    private QName schemaTypeForClass(ItemDefinition<?> propDef, Class<?> jaxbClazz) {
        QName schemaType = propDef != null ? propDef.getTypeName() : null;
        if(schemaType != null) {
            return schemaType;
        }
        return typeNameForClass(jaxbClazz);
    }

    private void setExplicitTypeDeclarationIfNeededForHeteroList(XNodeImpl node, Object realValue) {
        QName elementName = node.getElementName();
        QName typeName = inspector.determineTypeForClass(realValue.getClass());
        if (typeName != null && !getSchemaRegistry().hasImplicitTypeDefinition(elementName, typeName)
                && getSchemaRegistry().findTypeDefinitionByType(typeName, TypeDefinition.class) != null) {
            node.setExplicitTypeDeclaration(true);
            node.setTypeQName(typeName);
        }
    }

    private Class<?> unwrappedReturnType(Method getter) {
        Class<?> getterType = getter.getReturnType();
        if (Collection.class.isAssignableFrom(getterType)) {
            Type genericReturnType = getter.getGenericReturnType();
            if (genericReturnType instanceof ParameterizedType) {
                Type actualType = inspector.getTypeArgument(genericReturnType, "explicit type declaration");
                if (actualType instanceof Class) {
                    getterType = (Class<?>) actualType;
                } else if (actualType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) actualType;
                    Type typeArgument = inspector.getTypeArgument(parameterizedType, "JAXBElement return type");
                    getterType = inspector.getUpperBound(typeArgument, "JAXBElement return type");
                }
            }
        }
        return getterType;
    }

    // bean should have only two features: "list" attribute and multivalued property into which we should store the elements
    @NotNull
    <T> QName getHeterogeneousListPropertyName(Class<T> beanClass) throws SchemaException {
        List<String> properties = inspector.getPropOrder(beanClass);
        if (!properties.contains(DOMUtil.IS_LIST_ATTRIBUTE_NAME)) {
            throw new SchemaException("Couldn't unmarshal heterogeneous list into class without '"
                    + DOMUtil.IS_LIST_ATTRIBUTE_NAME + "' attribute. Class: "
                    + beanClass.getName() + " has the following properties: " + properties);
        }
        if (properties.size() > 2) {
            throw new SchemaException("Couldn't unmarshal heterogeneous list into class with more than one property "
                    + "other than '" + DOMUtil.IS_LIST_ATTRIBUTE_NAME + "'. Class " + beanClass.getName()
                    + " has the following properties: " + properties);
        }
        String contentProperty = properties.stream()
                .filter(p -> !DOMUtil.IS_LIST_ATTRIBUTE_NAME.equals(p))
                .findFirst()
                .orElseThrow(() -> new SchemaException("Couldn't unmarshal heterogeneous list into class without "
                        + "content-holding property. Class: " + beanClass.getName() + "."));
        return new QName(inspector.determineNamespace(beanClass), contentProperty);
    }

    private <T> XNodeImpl marshallValue(T value, QName valueType, boolean isAttribute, SerializationContext ctx) throws SchemaException {
        if (value == null) {
            return null;
        }
        if (isAttribute) {
            // hoping the value fits into primitive!
            return createPrimitiveXNode(value, valueType, true);
        } else {
            return marshall(value, ctx);
        }
    }

    private <T> PrimitiveXNodeImpl<T> createPrimitiveXNode(T value, QName valueType, boolean isAttribute) {
        PrimitiveXNodeImpl<T> xprim = new PrimitiveXNodeImpl<>();
        xprim.setValue(value, valueType);
        xprim.setAttribute(isAttribute);
        return xprim;
    }

    private <T> PrimitiveXNodeImpl<T> createPrimitiveXNode(T val, QName type) {
        return createPrimitiveXNode(val, type, false);
    }

    private XNodeImpl marshalRawType(Object value, SerializationContext sc) throws SchemaException {
        return (XNodeImpl) ((RawType) value).serializeToXNode();
    }

    private XNodeImpl marshalPolyString(Object value, SerializationContext sc) throws SchemaException {
        if (value instanceof PolyString) {
            return marshalPolyString((PolyString) value);
        } else if (value instanceof PolyStringType) {
            return marshalPolyString(((PolyStringType) value).toPolyString());
        } else {
            throw new IllegalArgumentException("Not a PolyString nor PolyStringType: " + value);
        }
    }

    XNodeImpl marshalPolyString(PolyString realValue) throws SchemaException {
        if (realValue.isSimple()) {
            PrimitiveXNodeImpl<PolyString> xprim = new PrimitiveXNodeImpl<>();
            xprim.setValue(realValue, PolyStringType.COMPLEX_TYPE);
            return xprim;
        } else {
            MapXNodeImpl xmap = new MapXNodeImpl();

            PrimitiveXNodeImpl<String> xorig = new PrimitiveXNodeImpl<>();
            xorig.setValue(realValue.getOrig(), DOMUtil.XSD_STRING);
            xmap.put(PolyString.F_ORIG, xorig);

            PrimitiveXNodeImpl<String> xnorm = new PrimitiveXNodeImpl<>();
            xnorm.setValue(realValue.getNorm(), DOMUtil.XSD_STRING);
            xmap.put(PolyString.F_NORM, xnorm);

            PolyStringTranslationType translation = realValue.getTranslation();
            if (translation != null) {
                XNodeImpl xTranslation = marshall(translation);
                xmap.put(PolyString.F_TRANSLATION, xTranslation);
            }

            Map<String, String> lang = realValue.getLang();
            if (lang != null && !lang.isEmpty()) {
                XNodeImpl xTranslation = serializePolyStringLang(lang);
                xmap.put(PolyString.F_LANG, xTranslation);
            }

            return xmap;
        }
    }

    private XNodeImpl serializePolyStringLang(Map<String, String> lang) {
        MapXNodeImpl xmap = new MapXNodeImpl();
        for (Map.Entry<String, String> langEntry : lang.entrySet()) {
            PrimitiveXNodeImpl<String> xPrim = new PrimitiveXNodeImpl<>(langEntry.getValue());
            xmap.put(new QName(PolyString.F_LANG.getNamespaceURI(), langEntry.getKey()), xPrim);
        }
        return xmap;
    }

    private XNodeImpl marshalItemPathType(Object o, SerializationContext sc) {
        ItemPathType itemPath = (ItemPathType) o;
        PrimitiveXNodeImpl<ItemPathType> xprim = new PrimitiveXNodeImpl<>();
        if (itemPath != null) {
            xprim.setValue(itemPath, ItemPathType.COMPLEX_TYPE);
        }
        return xprim;
    }

    private XNodeImpl marshalSchemaDefinition(Object o, SerializationContext ctx) {
        SchemaDefinitionType schemaDefinitionType = (SchemaDefinitionType) o;
        SchemaXNodeImpl xschema = new SchemaXNodeImpl();
        xschema.setSchemaElement(schemaDefinitionType.getSchema());
        MapXNodeImpl xmap = new MapXNodeImpl();
        xmap.put(DOMUtil.XSD_SCHEMA_ELEMENT, xschema);
        return xmap;
    }

    // TODO create more appropriate interface to be able to simply serialize ProtectedStringType instances
    public <T> MapXNodeImpl marshalProtectedDataType(Object o, SerializationContext sc) throws SchemaException {
        @SuppressWarnings("unchecked")
        ProtectedDataType<T> protectedType = (ProtectedDataType<T>) o;
        MapXNodeImpl xmap = new MapXNodeImpl();
        if (protectedType.getEncryptedDataType() != null) {
            EncryptedDataType encryptedDataType = protectedType.getEncryptedDataType();
            MapXNodeImpl xEncryptedDataType = (MapXNodeImpl) marshall(encryptedDataType);
            xmap.put(ProtectedDataType.F_ENCRYPTED_DATA, xEncryptedDataType);
        } else if (protectedType.getHashedDataType() != null) {
            HashedDataType hashedDataType = protectedType.getHashedDataType();
            MapXNodeImpl xHashedDataType = (MapXNodeImpl) marshall(hashedDataType);
            xmap.put(ProtectedDataType.F_HASHED_DATA, xHashedDataType);
        } else if (protectedType.getClearValue() != null){
            QName type = XsdTypeMapper.toXsdType(protectedType.getClearValue().getClass());
            PrimitiveXNodeImpl<?> xClearValue = createPrimitiveXNode(protectedType.getClearValue(), type);
            xmap.put(ProtectedDataType.F_CLEAR_VALUE, xClearValue);
        }
        // TODO: clearValue
        return xmap;
    }


    //region Specific marshallers ==============================================================
    private MapXNodeImpl marshalSearchFilterType(SearchFilterType value) throws SchemaException {
        if (value == null) {
            return null;
        }
        return (MapXNodeImpl) value.serializeToXNode(prismContext);
    }

    //endregion

    @NotNull
    public PrismContext getPrismContext() {
        return prismContext;
    }

    private SchemaRegistry getSchemaRegistry() {
        return prismContext.getSchemaRegistry();
    }

    boolean canProcess(QName typeName) {
        Boolean cached = canProcessCache.get(typeName);
        if (cached != null) {
            return cached;
        } else {
            boolean computed = computeCanProcess(typeName);
            canProcessCache.put(typeName, computed);
            return computed;
        }
    }

    private boolean computeCanProcess(QName typeName) {
        Class<Object> clazz = getSchemaRegistry().determineClassForType(typeName);
        if (clazz != null && canProcess(clazz)) {
            return true;
        }
        TypeDefinition td = getSchemaRegistry().findTypeDefinitionByType(typeName);
        if (td instanceof SimpleTypeDefinition) {
            return true;            // most probably dynamic enum, at this point
        }
        return false;
    }

    public boolean canProcess(@NotNull Class<?> clazz) {
        return !Containerable.class.isAssignableFrom(clazz) &&
                (RawType.class.equals(clazz) || clazz.getAnnotation(XmlType.class) != null || XsdTypeMapper.getTypeFromClass(clazz) != null);
    }

    public QName determineTypeForClass(Class<?> clazz) {
        return inspector.determineTypeForClass(clazz);
    }

}


// TODO hacked, for now
//    private <T> String findEnumFieldValue(Class classType, Object bean){
//        String name = bean.toString();
//        for (Field field: classType.getDeclaredFields()) {
//            XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
//            if (xmlEnumValue != null && field.getName().equals(name)) {
//                return xmlEnumValue.value();
//            }
//        }
//        return null;
//    }
