/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.marshaller;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.TypeDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import java.lang.reflect.*;
import java.util.*;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

/**
 * @author mederly
 */
public class PrismBeanInspector {

    @NotNull private PrismContext prismContext;

    public PrismBeanInspector(@NotNull PrismContext prismContext) {
        Validate.notNull(prismContext, "prismContext");
        this.prismContext = prismContext;
    }

    //region Caching mechanism (multiple dimensions)

    @FunctionalInterface
    interface Getter1<V, P1> {
        V get(P1 param1);
    }

    private <V, P1> V find1(Map<P1,V> cache, P1 param1, Getter1<V, P1> getter) {
        if (cache.containsKey(param1)) {
            return cache.get(param1);
        } else {
            V value = getter.get(param1);
            cache.put(param1, value);
            return value;
        }
    }

    @FunctionalInterface
    interface Getter2<V, P1, P2> {
        V get(P1 param1, P2 param2);
    }

    private <V, P1, P2> V find2(final Map<P1,Map<P2,V>> cache, final P1 param1, final P2 param2, final Getter2<V, P1, P2> getter) {
        Map<P2, V> cache2 = cache.computeIfAbsent(param1, k -> Collections.synchronizedMap(new HashMap<>()));
        return find1(cache2, param2, p -> getter.get(param1, p));
    }

    @FunctionalInterface
    interface Getter3<V, P1, P2, P3> {
        V get(P1 param1, P2 param2, P3 param3);
    }

    private <V, P1, P2, P3> V find3(final Map<P1,Map<P2,Map<P3,V>>> cache, final P1 param1, final P2 param2, final P3 param3, final Getter3<V, P1, P2, P3> getter) {
        Map<P2, Map<P3, V>> cache2 = cache.computeIfAbsent(param1, k -> Collections.synchronizedMap(new HashMap<>()));
        return find2(cache2, param2, param3, (p, q) -> getter.get(param1, p, q));
    }
    //endregion

    //region Individual inspection methods - cached versions

    private Map<Class<?>, String> determineNamespace = Collections.synchronizedMap(new HashMap<>());

    String determineNamespace(Class<?> paramType) {
        return find1(determineNamespace, paramType, this::determineNamespaceUncached);
    }

    private Map<Class<?>, QName> determineTypeForClass = Collections.synchronizedMap(new HashMap<>());

    QName determineTypeForClass(Class<?> paramType) {
        return find1(determineTypeForClass, paramType, PrismBeanInspector::determineTypeForClassUncached);
    }

    private Map<Field,Map<Method,Boolean>> isAttribute = Collections.synchronizedMap(new HashMap<>());

    boolean isAttribute(Field field, Method getter) {
        return find2(isAttribute, field, getter, this::isAttributeUncached);
    }

    private Map<Class,Map<String,Method>> findSetter = Collections.synchronizedMap(new HashMap<>());

    <T> Method findSetter(Class<T> beanClass, String fieldName) {
        //noinspection unchecked
        return find2(findSetter, beanClass, fieldName, (c, f) -> findSetterUncached(c, f));
    }

    private Map<Package,Class> getObjectFactoryClassPackage = Collections.synchronizedMap(new HashMap<>());
    Class getObjectFactoryClass(Package aPackage) {
        return find1(getObjectFactoryClassPackage, aPackage, p -> getObjectFactoryClassUncached(p));
    }

    private Map<String,Class> getObjectFactoryClassNamespace = Collections.synchronizedMap(new HashMap<>());
    Class getObjectFactoryClass(String namespaceUri) {
        return find1(getObjectFactoryClassNamespace, namespaceUri, s -> getObjectFactoryClassUncached(s));
    }

    private Map<Class<?>, List<String>> getPropOrder = Collections.synchronizedMap(new HashMap<>());

    List<String> getPropOrder(Class<?> beanClass) {
        return find1(getPropOrder, beanClass, this::getPropOrderUncached);
    }

    private Map<Class,Map<String,Method>> findElementMethodInObjectFactory = Collections.synchronizedMap(new HashMap<>());

    Method findElementMethodInObjectFactory(Class objectFactoryClass, String propName) {
        return find2(findElementMethodInObjectFactory, objectFactoryClass, propName,
                (c, p) -> findElementMethodInObjectFactoryUncached(c, p));
    }

    private Map<Class,Map<Method,Field>> lookupSubstitution = Collections.synchronizedMap(new HashMap<>());

    <T> Field lookupSubstitution(Class<T> beanClass, Method elementMethod) {
        return find2(lookupSubstitution, beanClass, elementMethod, this::lookupSubstitutionUncached);
    }

    private Map<Class,Map<String,String>> findEnumFieldName = Collections.synchronizedMap(new HashMap<>());

    <T> String findEnumFieldName(Class<T> classType, String primValue) {
        return find2(findEnumFieldName, classType, primValue, (c, v) -> findEnumFieldNameUncached(c, v));
    }

    private Map<Class,Map<String,String>> findEnumFieldValue = Collections.synchronizedMap(new HashMap<>());

    <T> String findEnumFieldValue(Class<T> classType, String toStringValue) {
        return find2(findEnumFieldValue, classType, toStringValue, (c, v) -> findEnumFieldValueUncached(c, v));
    }

    private Map<Field,Map<Class<?>,Map<String,QName>>> findTypeName = Collections.synchronizedMap(new HashMap<>());

    // Determines type for field/content combination. Field information is used only for simple XSD types.
    QName findTypeName(Field field, Class<?> contentClass, String defaultNamespacePlaceholder) {
        return find3(findTypeName, field, contentClass, defaultNamespacePlaceholder,
                this::findTypeNameUncached);
    }

    private Map<String,Map<Class<?>,Map<String,QName>>> findFieldElementQName = Collections.synchronizedMap(new HashMap<>());

    QName findFieldElementQName(String fieldName, Class<?> beanClass, String defaultNamespace) {
        return find3(findFieldElementQName, fieldName, beanClass, defaultNamespace,
                (fieldName1, beanClass1, defaultNamespace1) -> findFieldElementQNameUncached(fieldName1, beanClass1,
                        defaultNamespace1));
    }

    private Map<Class,Map<String,Method>> findPropertyGetter = Collections.synchronizedMap(new HashMap<>());

    public <T> Method findPropertyGetter(Class<T> beanClass, String propName) {
        return find2(findPropertyGetter, beanClass, propName, this::findPropertyGetterUncached);
    }

    private Map<Class,Map<String,Field>> findPropertyField = Collections.synchronizedMap(new HashMap<>());

    public <T> Field findPropertyField(Class<T> beanClass, String propName) {
        return find2(findPropertyField, beanClass, propName, this::findPropertyFieldUncached);
    }
    //endregion

    //region Uncached versions of the inspection methods

    private <T> Field findPropertyFieldUncached(Class<T> classType, String propName) {
        Field field = findPropertyFieldExactUncached(classType, propName);
        if (field != null) {
            return field;
        }
        // Fields for some reserved words are prefixed by underscore, so try also this.
        return findPropertyFieldExactUncached(classType, "_"+propName);
    }

    private <T> Field findPropertyFieldExactUncached(Class<T> classType, String propName) {
        for (Field field: classType.getDeclaredFields()) {
            XmlElement xmlElement = field.getAnnotation(XmlElement.class);
            if (xmlElement != null && xmlElement.name().equals(propName)) {
                return field;
            }
            XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);
            if (xmlAttribute != null && xmlAttribute.name().equals(propName)) {
                return field;
            }
        }
        try {
            return classType.getDeclaredField(propName);
        } catch (NoSuchFieldException e) {
            // nothing found
        }
        Class<? super T> superclass = classType.getSuperclass();
        if (superclass == null || Object.class.equals(superclass)) {
            return null;
        }
        return findPropertyField(superclass, propName);
    }

    private <T> Method findPropertyGetterUncached(Class<T> classType, String propName) {
        if (propName.startsWith("_")) {
            propName = propName.substring(1);
        }
        for (Method method: classType.getDeclaredMethods()) {
            XmlElement xmlElement = method.getAnnotation(XmlElement.class);
            if (xmlElement != null && xmlElement.name().equals(propName)) {
                return method;
            }
            XmlAttribute xmlAttribute = method.getAnnotation(XmlAttribute.class);
            if (xmlAttribute != null && xmlAttribute.name().equals(propName)) {
                return method;
            }
        }
        String getterName = "get"+ StringUtils.capitalize(propName);
        try {
            return classType.getDeclaredMethod(getterName);
        } catch (NoSuchMethodException e) {
            // nothing found
        }
        getterName = "is"+StringUtils.capitalize(propName);
        try {
            return classType.getDeclaredMethod(getterName);
        } catch (NoSuchMethodException e) {
            // nothing found
        }
        Class<? super T> superclass = classType.getSuperclass();
        if (superclass == null || superclass.equals(Object.class)) {
            return null;
        }
        return findPropertyGetter(superclass, propName);
    }

    private boolean isAttributeUncached(Field field, Method getter) {
        if (field == null && getter == null) {
            return false;
        } else {
            return field != null && field.isAnnotationPresent(XmlAttribute.class)
                    || getter != null && getter.isAnnotationPresent(XmlAttribute.class);
        }
    }

    private String determineNamespaceUncached(Class<?> beanClass) {
        XmlType xmlType = beanClass.getAnnotation(XmlType.class);
        if (xmlType == null) {
            return null;
        }

        String namespace = xmlType.namespace();
        if (BeanMarshaller.DEFAULT_PLACEHOLDER.equals(namespace)) {
            XmlSchema xmlSchema = beanClass.getPackage().getAnnotation(XmlSchema.class);
            namespace = xmlSchema.namespace();
        }
        if (StringUtils.isBlank(namespace) || BeanMarshaller.DEFAULT_PLACEHOLDER.equals(namespace)) {
            return null;
        }

        return namespace;
    }

    public static QName determineTypeForClassUncached(Class<?> beanClass) {
        XmlType xmlType = beanClass.getAnnotation(XmlType.class);
        if (xmlType == null) {
            return null;
        }

        String namespace = xmlType.namespace();
        if (BeanMarshaller.DEFAULT_PLACEHOLDER.equals(namespace)) {
            XmlSchema xmlSchema = beanClass.getPackage().getAnnotation(XmlSchema.class);
            namespace = xmlSchema.namespace();
        }
        if (StringUtils.isBlank(namespace) || BeanMarshaller.DEFAULT_PLACEHOLDER.equals(namespace)) {
            return null;
        }

        return new QName(namespace, xmlType.name());
    }

    private <T> Method findSetterUncached(Class<T> classType, String fieldName) {
        String setterName = getSetterName(fieldName);
        for(Method method: classType.getMethods()) {
            if (!method.getName().equals(setterName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                continue;
            }
            Class<?> setterType = parameterTypes[0];
            if (setterType.equals(Object.class) || Node.class.isAssignableFrom(setterType)) {
                // Leave for second pass, let's try find a better setter
                continue;
            }
            return method;
        }
        // Second pass
        for(Method method: classType.getMethods()) {
            if (!method.getName().equals(setterName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                continue;
            }
            return method;
        }
        return null;
    }

    private String getSetterName(String fieldName) {
        if (fieldName.startsWith("_")) {
            fieldName = fieldName.substring(1);
        }
        return "set"+StringUtils.capitalize(fieldName);
    }

    private Class getObjectFactoryClassUncached(Package pkg) {
        try {
            return Class.forName(pkg.getName()+".ObjectFactory");
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot find object factory class in package "+pkg.getName()+": "+e.getMessage(), e);
        }
    }

    private Class getObjectFactoryClassUncached(String namespaceUri) {
        SchemaDescription schemaDescription = prismContext.getSchemaRegistry().findSchemaDescriptionByNamespace(namespaceUri);
        if (schemaDescription == null) {
            throw new IllegalArgumentException("Cannot find object factory class for namespace "+namespaceUri+": unknown schema namespace");
        }
        Package compileTimeClassesPackage = schemaDescription.getCompileTimeClassesPackage();
        if (compileTimeClassesPackage == null) {
            throw new IllegalArgumentException("Cannot find object factory class for namespace "+namespaceUri+": not a compile-time schema");
        }
        return getObjectFactoryClassUncached(compileTimeClassesPackage);
    }

    private Method findElementMethodInObjectFactoryUncached(Class objectFactoryClass, String propName) {
        for (Method method: objectFactoryClass.getDeclaredMethods()) {
            XmlElementDecl xmlElementDecl = method.getAnnotation(XmlElementDecl.class);
            if (xmlElementDecl == null) {
                continue;
            }
            if (propName.equals(xmlElementDecl.name())) {
                return method;
            }
        }
        return null;
    }

    private Field lookupSubstitutionUncached(Class beanClass, Method elementMethodInObjectFactory) {
        XmlElementDecl xmlElementDecl = elementMethodInObjectFactory.getAnnotation(XmlElementDecl.class);
        if (xmlElementDecl == null) {
            return null;
        }
        final String substitutionHeadName = xmlElementDecl.substitutionHeadName();
        return findField(beanClass, field -> {
            XmlElementRef xmlElementRef = field.getAnnotation(XmlElementRef.class);
            return xmlElementRef != null && xmlElementRef.name().equals(substitutionHeadName);
        });
    }

    private Field findField(Class classType, Handler<Field> selector) {
        for (Field field: classType.getDeclaredFields()) {
            if (selector.handle(field)) {
                return field;
            }
        }
        Class superclass = classType.getSuperclass();
        if (superclass == null || superclass.equals(Object.class)) {
            return null;
        }
        return findField(superclass, selector);
    }

    private Method findMethod(Class classType, Handler<Method> selector) {
        for (Method field: classType.getDeclaredMethods()) {
            if (selector.handle(field)) {
                return field;
            }
        }
        Class superclass = classType.getSuperclass();
        if (superclass == null || superclass.equals(Object.class)) {
            return null;
        }
        return findMethod(superclass, selector);
    }

    private List<String> getPropOrderUncached(Class<?> beanClass) {
        List<String> propOrder;

        // Superclass first!
        Class superclass = beanClass.getSuperclass();
        if (superclass == null || superclass.equals(Object.class) || superclass.getAnnotation(XmlType.class) == null) {
            propOrder = new ArrayList<>();
        } else {
            propOrder = new ArrayList<>(getPropOrder(superclass));
        }

        XmlType xmlType = beanClass.getAnnotation(XmlType.class);
        if (xmlType == null) {
            throw new IllegalArgumentException("Cannot marshall "+beanClass+" it does not have @XmlType annotation");
        }

        String[] myPropOrder = xmlType.propOrder();
        for (String myProp: myPropOrder) {
            if (StringUtils.isNotBlank(myProp)) {
                // some properties starts with underscore..we don't want to serialize them with underscore, so remove it..
                if (myProp.startsWith("_")){
                    myProp = myProp.replace("_", "");
                }
                propOrder.add(myProp);
            }
        }

        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(XmlAttribute.class)) {
                propOrder.add(field.getName());
            }
        }

        Method[] methods = beanClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(XmlAttribute.class)) {
                propOrder.add(getPropertyNameFromGetter(method.getName()));
            }
        }

        return propOrder;
    }

    private <T> String findEnumFieldNameUncached(Class classType, T primValue){
        for (Field field: classType.getDeclaredFields()) {
            XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
            if (xmlEnumValue != null && xmlEnumValue.value().equals(primValue)) {
                return field.getName();
            }
        }
        return null;
    }

    private static String findEnumFieldValueUncached(Class classType, String toStringValue){
        for (Field field: classType.getDeclaredFields()) {
            XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
            if (xmlEnumValue != null && field.getName().equals(toStringValue)) {
                return xmlEnumValue.value();
            }
        }
        return null;
    }

    private String getPropertyNameFromGetter(String getterName) {
        if ((getterName.length() > 3) && getterName.startsWith("get") &&
                Character.isUpperCase(getterName.charAt(3))) {
            String propPart = getterName.substring(3);
            return StringUtils.uncapitalize(propPart);
        }
        return getterName;
    }

    private QName findTypeNameUncached(Field field, Class contentClass, String schemaNamespace) {
        if (RawType.class.equals(contentClass)) {
            // RawType is a meta-type. We do not really want to use field types of RawType class.
            return null;
        }
        if (field != null) {
            XmlSchemaType xmlSchemaType = field.getAnnotation(XmlSchemaType.class);
            if (xmlSchemaType != null) {
                return new QName(xmlSchemaType.namespace(), xmlSchemaType.name());
            }
        }
        QName typeName = XsdTypeMapper.getJavaToXsdMapping(contentClass);
        if (typeName != null) {
            return typeName;
        }
        // TODO the following code is similar to determineTypeForClass
        XmlType xmlType = (XmlType) contentClass.getAnnotation(XmlType.class);
        if (xmlType != null) {
            String propTypeLocalPart = xmlType.name();
            String propTypeNamespace = xmlType.namespace();
            if (propTypeNamespace.equals(BeanMarshaller.DEFAULT_PLACEHOLDER)) {
                PrismSchema schema = prismContext.getSchemaRegistry().findSchemaByCompileTimeClass(contentClass);
                if (schema != null && schema.getNamespace() != null) {
                    propTypeNamespace = schema.getNamespace();        // should be non-null for properly initialized schemas
                } else {
                    // schemaNamespace is only a poor indicator of required namespace (consider e.g. having c:UserType in apit:ObjectListType)
                    // so we use it only if we couldn't find anything else
                    propTypeNamespace = schemaNamespace;
                }
            }
            return new QName(propTypeNamespace, propTypeLocalPart);
        }
        return null;
    }

    private QName findFieldElementQNameUncached(String fieldName, Class beanClass, String defaultNamespace) {
        Field field;
        try {
            field = beanClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return new QName(defaultNamespace, fieldName);               // TODO implement this if needed (lookup the getter method instead of the field)
        }
        String realLocalName = fieldName;
        String realNamespace = defaultNamespace;
        XmlElement xmlElement = field.getAnnotation(XmlElement.class);
        if (xmlElement != null) {
            String name = xmlElement.name();
            if (!BeanMarshaller.DEFAULT_PLACEHOLDER.equals(name)) {
                realLocalName = name;
            }
            String namespace = xmlElement.namespace();
            if (!BeanMarshaller.DEFAULT_PLACEHOLDER.equals(namespace)) {
                realNamespace = namespace;
            }
        }
        return new QName(realNamespace, realLocalName);
    }
    //endregion

    //region Other
    public <T> Field findAnyField(Class<T> beanClass) {
        return findField(beanClass, field -> field.getAnnotation(XmlAnyElement.class) != null);
    }

    public <T> Method findAnyMethod(Class<T> beanClass) {
        return findMethod(beanClass, method -> method.getAnnotation(XmlAnyElement.class) != null);
    }

    // e.g. Collection<UserType> -> UserType
    @NotNull
    Type getTypeArgument(Type origType, String desc) {
        if (!(origType instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Not a parametrized type "+desc);
        }
        ParameterizedType parametrizedType = (ParameterizedType)origType;
        Type[] actualTypeArguments = parametrizedType.getActualTypeArguments();
        if (actualTypeArguments == null || actualTypeArguments.length == 0) {
            throw new IllegalArgumentException("No type arguments for getter "+desc);
        }
        if (actualTypeArguments.length > 1) {
            throw new IllegalArgumentException("Too many type arguments for getter for "+desc);
        }
        return actualTypeArguments[0];
    }

    @NotNull
    public Class getUpperBound(Type type, String desc) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof WildcardType) {
            WildcardType wildcard = ((WildcardType) type);
            if (wildcard.getUpperBounds().length != 1) {
                throw new IllegalArgumentException("Wrong number of upper bounds for " + type + " ("
                        + wildcard.getUpperBounds().length + "): " + desc);
            }
            Type upper = wildcard.getUpperBounds()[0];
            if (upper instanceof Class) {
                return (Class) upper;
            } else {
                throw new IllegalArgumentException("Upper bound for " + type + " is not a class, it is " + type + ": " + desc);
            }
        } else {
            throw new IllegalArgumentException(type + "is not a class nor wildcard type: " + type + ": " + desc);
        }
    }

    @NotNull
    public <T> Class<? extends T> findMatchingSubclass(Class<T> beanClass, Collection<QName> fields) throws SchemaException {
        SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
        TypeDefinition typeDef = schemaRegistry.findTypeDefinitionByCompileTimeClass(beanClass, TypeDefinition.class);
        if (typeDef == null) {
            throw new SchemaException("No type definition for " + beanClass);
        }
        List<TypeDefinition> subTypes = new ArrayList<>(typeDef.getStaticSubTypes());
        subTypes.sort(Comparator.comparing(TypeDefinition::getInstantiationOrder, nullsLast(naturalOrder())));
        TypeDefinition matchingDefinition = null;
        for (TypeDefinition subType : subTypes) {
            if (matchingDefinition != null && !Objects.equals(matchingDefinition.getInstantiationOrder(), subType.getInstantiationOrder())) {
                break;      // found something and went to lower orders -> we can stop searching
            }
            if (matches(subType, fields)) {
                if (matchingDefinition != null) {
                    throw new SchemaException("Couldn't unambiguously determine a subclass for " + beanClass
                            + " instantiation (fields: " + fields + "). Candidates: " + matchingDefinition + ", " + subType);
                }
                matchingDefinition = subType;
            }
        }
        if (matchingDefinition == null) {
            final int max = 5;
            throw new SchemaException("Couldn't find a subclass of " + beanClass + " that would contain fields " + fields + ". Considered "
                    + subTypes.subList(0, Math.min(subTypes.size(), max))
                    + (subTypes.size() >= max ? " (...)" : ""));
        }
        //noinspection unchecked
        Class<? extends T> compileTimeClass = (Class<? extends T>) matchingDefinition.getCompileTimeClass();
        if (compileTimeClass != null) {
            return compileTimeClass;
        } else {
            throw new SchemaException("No compile time class defined for " + matchingDefinition);
        }
    }

    private boolean matches(TypeDefinition type, Collection<QName> fields) {
        if (!(type instanceof ComplexTypeDefinition)) {
            return false;
        }
        ComplexTypeDefinition ctd = (ComplexTypeDefinition) type;
        return fields.stream().allMatch(field -> ctd.containsItemDefinition(field));
    }
    //endregion

}
