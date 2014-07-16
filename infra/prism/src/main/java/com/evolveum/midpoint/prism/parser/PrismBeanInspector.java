/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.parser;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.Handler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
public class PrismBeanInspector {

    private PrismContext prismContext;

    public PrismBeanInspector(PrismContext prismContext) {
        Validate.notNull(prismContext, "prismContext");
        this.prismContext = prismContext;
    }

    //region Caching mechanism (multiple dimensions)

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

    interface Getter2<V, P1, P2> {
        V get(P1 param1, P2 param2);
    }

    private <V, P1, P2> V find2(final Map<P1,Map<P2,V>> cache, final P1 param1, final P2 param2, final Getter2<V, P1, P2> getter) {
        Map<P2, V> cache2 = cache.get(param1);
        if (cache2 == null) {
            cache2 = Collections.synchronizedMap(new HashMap());
            cache.put(param1, cache2);
        }
        return find1(cache2, param2, new Getter1<V, P2>() {
            @Override
            public V get(P2 p) {
                return getter.get(param1, p);
            }
        });
    }

    interface Getter3<V, P1, P2, P3> {
        V get(P1 param1, P2 param2, P3 param3);
    }

    private <V, P1, P2, P3> V find3(final Map<P1,Map<P2,Map<P3,V>>> cache, final P1 param1, final P2 param2, final P3 param3, final Getter3<V, P1, P2, P3> getter) {
        Map<P2, Map<P3, V>> cache2 = cache.get(param1);
        if (cache2 == null) {
            cache2 = Collections.synchronizedMap(new HashMap());
            cache.put(param1, cache2);
        }
        return find2(cache2, param2, param3, new Getter2<V, P2, P3>() {
            @Override
            public V get(P2 p, P3 q) {
                return getter.get(param1, p, q);
            }
        });
    }
    //endregion

    //region Individual inspection methods - cached versions

    private Map<Class<? extends Object>, String> _determineNamespace = Collections.synchronizedMap(new HashMap());

    String determineNamespace(Class<? extends Object> paramType) {
        return find1(_determineNamespace, paramType, new Getter1<String,Class<? extends Object>>() {
            @Override
            public String get(Class<? extends Object> paramType) {
                return determineNamespaceUncached(paramType);
            }
        });
    }

    private Map<Class<? extends Object>, QName> _determineTypeForClass = Collections.synchronizedMap(new HashMap());

    QName determineTypeForClass(Class<? extends Object> paramType) {
        return find1(_determineTypeForClass, paramType, new Getter1<QName,Class<? extends Object>>() {
            @Override
            public QName get(Class<? extends Object> paramType) {
                return determineTypeForClassUncached(paramType);
            }
        });
    }

    private Map<Field,Map<Method,Boolean>> _isAttribute = Collections.synchronizedMap(new HashMap());

    boolean isAttribute(Field field, Method getter) {
        return find2(_isAttribute, field, getter, new Getter2<Boolean,Field,Method>() {
            @Override
            public Boolean get(Field f, Method m) {
                return isAttributeUncached(f, m);
            }
        });
    }

    private Map<Class,Map<String,Method>> _findSetter = Collections.synchronizedMap(new HashMap());

    <T> Method findSetter(Class<T> beanClass, String fieldName) {
        return find2(_findSetter, beanClass, fieldName, new Getter2<Method,Class,String>() {
            @Override
            public Method get(Class c, String f) {
                return findSetterUncached(c, f);
            }
        });
    }

    private Map<Package,Class> _getObjectFactoryClass = Collections.synchronizedMap(new HashMap());
    Class getObjectFactoryClass(Package aPackage) {
        return find1(_getObjectFactoryClass, aPackage, new Getter1<Class,Package>() {
            @Override
            public Class get(Package p) {
                return getObjectFactoryClassUncached(p);
            }
        });
    }

    private Map<Class<? extends Object>, List<String>> _getPropOrder = Collections.synchronizedMap(new HashMap());

    List<String> getPropOrder(Class<? extends Object> beanClass) {
        return find1(_getPropOrder, beanClass, new Getter1<List<String>, Class<? extends Object>>() {
            @Override
            public List<String> get(Class<? extends Object> c) {
                return getPropOrderUncached(c);
            }
        });
    }

    private Map<Class,Map<String,Method>> _findElementMethodInObjectFactory = Collections.synchronizedMap(new HashMap());

    Method findElementMethodInObjectFactory(Class objectFactoryClass, String propName) {
        return find2(_findElementMethodInObjectFactory, objectFactoryClass, propName, new Getter2<Method,Class,String>() {
            @Override
            public Method get(Class c, String p) {
                return findElementMethodInObjectFactoryUncached(c, p);
            }
        });
    }

    private Map<Class,Map<Method,Field>> _lookupSubstitution = Collections.synchronizedMap(new HashMap());

    <T> Field lookupSubstitution(Class<T> beanClass, Method elementMethod) {
        return find2(_lookupSubstitution, beanClass, elementMethod, new Getter2<Field,Class,Method>() {
            @Override
            public Field get(Class c, Method m) {
                return lookupSubstitutionUncached(c, m);
            }
        });
    }

    private Map<Class,Map<String,String>> _findEnumFieldName = Collections.synchronizedMap(new HashMap());

    <T> String findEnumFieldName(Class<T> classType, String primValue) {
        return find2(_findEnumFieldName, classType, primValue, new Getter2<String,Class,String>() {
            @Override
            public String get(Class c, String v) {
                return findEnumFieldNameUncached(c, v);
            }
        });
    }

    private Map<Field,Map<Class<? extends Object>,Map<String,QName>>> _findFieldTypeName = Collections.synchronizedMap(new HashMap());

    QName findFieldTypeName(Field field, Class<? extends Object> beanClass, String defaultNamespacePlaceholder) {
        return find3(_findFieldTypeName, field, beanClass, defaultNamespacePlaceholder, new Getter3<QName,Field,Class<? extends Object>,String>() {
            @Override
            public QName get(Field field, Class<? extends Object> beanClass, String defaultNamespacePlaceholder) {
                return findFieldTypeNameUncached(field, beanClass, defaultNamespacePlaceholder);
            }
        });
    }

    private Map<String,Map<Class<? extends Object>,Map<String,QName>>> _findFieldElementQName = Collections.synchronizedMap(new HashMap());

    QName findFieldElementQName(String fieldName, Class<? extends Object> beanClass, String defaultNamespace) {
        return find3(_findFieldElementQName, fieldName, beanClass, defaultNamespace, new Getter3<QName, String, Class<? extends Object>, String>() {
            @Override
            public QName get(String fieldName, Class<? extends Object> beanClass, String defaultNamespace) {
                return findFieldElementQNameUncached(fieldName, beanClass, defaultNamespace);
            }
        });
    }

    private Map<Class,Map<String,Method>> _findPropertyGetter = Collections.synchronizedMap(new HashMap());

    public <T> Method findPropertyGetter(Class<T> beanClass, String propName) {
        return find2(_findPropertyGetter, beanClass, propName, new Getter2<Method,Class,String>() {
            @Override
            public Method get(Class param1, String param2) {
                return findPropertyGetterUncached(param1, param2);
            }
        });
    }

    private Map<Class,Map<String,Field>> _findPropertyField = Collections.synchronizedMap(new HashMap());

    public <T> Field findPropertyField(Class<T> beanClass, String propName) {
        return find2(_findPropertyField, beanClass, propName, new Getter2<Field,Class,String>() {
            @Override
            public Field get(Class param1, String param2) {
                return findPropertyFieldUncached(param1, param2);
            }
        });
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
            if (xmlElement != null && xmlElement.name() != null && xmlElement.name().equals(propName)) {
                return field;
            }
            XmlAttribute xmlAttribute = field.getAnnotation(XmlAttribute.class);
            if (xmlAttribute != null && xmlAttribute.name() != null && xmlAttribute.name().equals(propName)) {
                return field;
            }
        }
        try {
            return classType.getDeclaredField(propName);
        } catch (NoSuchFieldException e) {
            // nothing found
        }
        Class<? super T> superclass = classType.getSuperclass();
        if (superclass.equals(Object.class)) {
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
            if (xmlElement != null && xmlElement.name() != null && xmlElement.name().equals(propName)) {
                return method;
            }
            XmlAttribute xmlAttribute = method.getAnnotation(XmlAttribute.class);
            if (xmlAttribute != null && xmlAttribute.name() != null && xmlAttribute.name().equals(propName)) {
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
        if (superclass.equals(Object.class)) {
            return null;
        }
        return findPropertyGetter(superclass, propName);
    }

    private boolean isAttributeUncached(Field field, Method getter){
        if (field == null && getter == null){
            return false;
        }

        if (field != null && field.isAnnotationPresent(XmlAttribute.class)){
            return true;
        }

        if (getter != null && getter.isAnnotationPresent(XmlAttribute.class)){
            return true;
        }

        return false;
    }

    private String determineNamespaceUncached(Class<? extends Object> beanClass) {
        XmlType xmlType = beanClass.getAnnotation(XmlType.class);
        if (xmlType == null) {
            return null;
        }

        String namespace = xmlType.namespace();
        if (namespace == null || PrismBeanConverter.DEFAULT_PLACEHOLDER.equals(namespace)) {
            XmlSchema xmlSchema = beanClass.getPackage().getAnnotation(XmlSchema.class);
            namespace = xmlSchema.namespace();
        }
        if (StringUtils.isBlank(namespace) || PrismBeanConverter.DEFAULT_PLACEHOLDER.equals(namespace)) {
            return null;
        }

        return namespace;
    }

    private QName determineTypeForClassUncached(Class<? extends Object> beanClass) {
        XmlType xmlType = beanClass.getAnnotation(XmlType.class);
        if (xmlType == null) {
            return null;
        }

        String namespace = xmlType.namespace();
        if (namespace == null || PrismBeanConverter.DEFAULT_PLACEHOLDER.equals(namespace)) {
            XmlSchema xmlSchema = beanClass.getPackage().getAnnotation(XmlSchema.class);
            namespace = xmlSchema.namespace();
        }
        if (StringUtils.isBlank(namespace) || PrismBeanConverter.DEFAULT_PLACEHOLDER.equals(namespace)) {
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

    private Field lookupSubstitutionUncached(Class beanClass, Method elementMethod) {
        XmlElementDecl xmlElementDecl = elementMethod.getAnnotation(XmlElementDecl.class);
        if (xmlElementDecl == null) {
            return null;
        }
        final String substitutionHeadName = xmlElementDecl.substitutionHeadName();
        if (substitutionHeadName == null) {
            return null;
        }
        return findField(beanClass,new Handler<Field>() {
            @Override
            public boolean handle(Field field) {
                XmlElementRef xmlElementRef = field.getAnnotation(XmlElementRef.class);
                if (xmlElementRef == null) {
                    return false;
                }
                String name = xmlElementRef.name();
                if (name == null) {
                    return false;
                }
                return name.equals(substitutionHeadName);
            }
        });
    }

    private Field findField(Class classType, Handler<Field> selector) {
        for (Field field: classType.getDeclaredFields()) {
            if (selector.handle(field)) {
                return field;
            }
        }
        Class superclass = classType.getSuperclass();
        if (superclass.equals(Object.class)) {
            return null;
        }
        return findField(superclass, selector);
    }

    private List<String> getPropOrderUncached(Class<? extends Object> beanClass) {
        List<String> propOrder;

        // Superclass first!
        Class superclass = beanClass.getSuperclass();
        if (superclass.equals(Object.class) || superclass.getAnnotation(XmlType.class) == null) {
            propOrder = new ArrayList<>();
        } else {
            propOrder = new ArrayList<>(getPropOrder(superclass));
        }

        XmlType xmlType = beanClass.getAnnotation(XmlType.class);
        if (xmlType == null) {
            throw new IllegalArgumentException("Cannot marshall "+beanClass+" it does not have @XmlType annotation");
        }

        String[] myPropOrder = xmlType.propOrder();
        if (myPropOrder != null) {
            for (String myProp: myPropOrder) {
                if (StringUtils.isNotBlank(myProp)) {
                	// some properties starts with underscore..we don't want to serialize them with underscore, so remove it..
                	if (myProp.startsWith("_")){
                		myProp = myProp.replace("_", "");
                	}
                    propOrder.add(myProp);
                }
            }
        }

        Field[] fields = beanClass.getDeclaredFields();
        for (int i = 0; i< fields.length; i++){
            Field field = fields[i];
            if (field.isAnnotationPresent(XmlAttribute.class)){
                propOrder.add(field.getName());
            }
        }

        Method[] methods = beanClass.getDeclaredMethods();
        for (int i = 0; i< methods.length; i++){
            Method method = methods[i];
            if (method.isAnnotationPresent(XmlAttribute.class)){
//				System.out.println("methodName: " + method.getName());
                String propname = getPropertyNameFromGetter(method.getName());
                //StringUtils.uncapitalize(StringUtils.removeStart("get", method.getName()))
                propOrder.add(propname);
            }
        }

        return propOrder;
    }

    private <T> String findEnumFieldNameUncached(Class classType, T primValue){
        for (Field field: classType.getDeclaredFields()) {
            XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
            if (xmlEnumValue != null && xmlEnumValue.value() != null && xmlEnumValue.value().equals(primValue)) {
                return field.getName();
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

    private QName findFieldTypeNameUncached(Field field, Class fieldType, String schemaNamespace) {
        QName propTypeQname = null;
        XmlSchemaType xmlSchemaType = null;
        if (field != null) {
            xmlSchemaType = field.getAnnotation(XmlSchemaType.class);
        }
        if (xmlSchemaType != null) {
            String propTypeLocalPart = xmlSchemaType.name();
            if (propTypeLocalPart != null) {
                String propTypeNamespace = xmlSchemaType.namespace();
                if (propTypeNamespace == null) {
                    propTypeNamespace = XMLConstants.W3C_XML_SCHEMA_NS_URI;
                }
                propTypeQname = new QName(propTypeNamespace, propTypeLocalPart);
            }
        }
        if (propTypeQname == null) {
            propTypeQname = XsdTypeMapper.getJavaToXsdMapping(fieldType);
        }

        if (propTypeQname == null) {
            XmlType xmlType = (XmlType) fieldType.getAnnotation(XmlType.class);
            if (xmlType != null) {
                String propTypeLocalPart = xmlType.name();
                if (propTypeLocalPart != null) {
                    String propTypeNamespace = xmlType.namespace();
                    if (propTypeNamespace == null || propTypeNamespace.equals(PrismBeanConverter.DEFAULT_PLACEHOLDER)) {
                        if (prismContext != null) {     // hopefully this is always the case!
                            PrismSchema schema = prismContext.getSchemaRegistry().findSchemaByCompileTimeClass(fieldType);
                            if (schema != null && schema.getNamespace() != null) {
                                propTypeNamespace = schema.getNamespace();
                            }
                        }
                        if (propTypeNamespace == null) {
                            // schemaNamespace is only a poor indicator of required namespace (consider e.g. having c:UserType in apit:ObjectListType)
                            // so we use it only if we couldn't find anything else
                            propTypeNamespace = schemaNamespace;
                        }
                    }
                    propTypeQname = new QName(propTypeNamespace, propTypeLocalPart);
                }
            }
        }

        return propTypeQname;
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
            if (name != null && !PrismBeanConverter.DEFAULT_PLACEHOLDER.equals(name)) {
                realLocalName = name;
            }
            String namespace = xmlElement.namespace();
            if (namespace != null && !PrismBeanConverter.DEFAULT_PLACEHOLDER.equals(namespace)) {
                realNamespace = namespace;
            }
        }
        return new QName(realNamespace, realLocalName);
    }
    //endregion
}
