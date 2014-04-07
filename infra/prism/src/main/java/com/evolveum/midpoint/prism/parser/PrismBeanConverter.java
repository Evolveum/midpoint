/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.prism.parser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.parser.util.XNodeProcessorUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.prism.xml.ns._public.query_2.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_2.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.ProtectedByteArrayType;
import com.evolveum.prism.xml.ns._public.types_2.ProtectedDataType;
import com.evolveum.prism.xml.ns._public.types_2.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_2.RawType;

public class PrismBeanConverter {
	
	public static final String DEFAULT_NAMESPACE_PLACEHOLDER = "##default";
	
	private PrismContext prismContext;

	public PrismBeanConverter(PrismContext prismContext) {
		this.prismContext = prismContext;
	}
	
	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	private SchemaRegistry getSchemaRegistry() {
		if (prismContext == null) {
			return null;
		}
		return prismContext.getSchemaRegistry();
	}

	public boolean canProcess(QName typeName) {
		return getSchemaRegistry().determineCompileTimeClass(typeName) != null; 
	}
	
	public boolean canProcess(Class<?> clazz) {
		return clazz.getAnnotation(XmlType.class) != null;
	}
	
	public <T> T unmarshall(MapXNode xnode, QName typeQName) throws SchemaException {
		Class<T> classType = getSchemaRegistry().determineCompileTimeClass(typeQName);
		return unmarshall(xnode, classType);
	}
	
	public <T> T unmarshall(MapXNode xnode, Class<T> beanClass) throws SchemaException {
        T bean;
        Set<String> keysToParse;          // only these keys will be parsed (null if all)
		if (SearchFilterType.class.isAssignableFrom(beanClass)) {
            keysToParse = Collections.singleton("condition");       // TODO fix this BRUTAL HACK - it is here because of c:ConditionalSearchFilterType
			bean = (T) unmarshalSearchFilterType(xnode, (Class<? extends SearchFilterType>) beanClass);
        } else {
            keysToParse = null;
            try {
                bean = beanClass.newInstance();
            } catch (InstantiationException e) {
                throw new SystemException("Cannot instantiate bean of type "+beanClass+": "+e.getMessage(), e);
            } catch (IllegalAccessException e) {
                throw new SystemException("Cannot instantiate bean of type "+beanClass+": "+e.getMessage(), e);
            }
        } 

		if (ProtectedDataType.class.isAssignableFrom(beanClass)){
			ProtectedDataType protectedDataType = null;
			if (bean instanceof ProtectedStringType){
				protectedDataType = new ProtectedStringType();
			} else if (bean instanceof ProtectedByteArrayType){
				protectedDataType = new ProtectedByteArrayType();
			} else{
				throw new SchemaException("Unexpected subtype of protected data type: " + bean.getClass());
			}
        	XNodeProcessorUtil.parseProtectedType(protectedDataType, xnode, prismContext);
        	return (T) protectedDataType;
    		
        }
		for (Entry<QName,XNode> entry: xnode.entrySet()) {
			QName key = entry.getKey();
            if (keysToParse != null && !keysToParse.contains(key.getLocalPart())) {
                continue;
            }
			XNode xsubnode = entry.getValue();
			String propName = key.getLocalPart();
			Field field = findPropertyField(beanClass, propName);
			Method propertyGetter = null;
			if (field == null) {
				propertyGetter = findPropertyGetter(beanClass, propName);
			}

			Method elementMethod = null;
			Object objectFactory = null;
			if (field == null && propertyGetter == null) {
				// We have to try to find a more generic field, such as xsd:any (TODO) or substitution element
				// check for global element definition first
				objectFactory = getObjectFactory(beanClass.getPackage());
				elementMethod = findElementMethodInObjectFactory(objectFactory, propName);
				if (elementMethod == null) {
					throw new SchemaException("No field "+propName+" in class "+beanClass+" (and no element method in object factory too)");
				}
				field = lookupSubstitution(beanClass, elementMethod);
				if (field == null) {
					throw new SchemaException("No field "+propName+" in class "+beanClass+" (and no suitable substitution too)");
				}
			}
			String fieldName;
			if (field != null) {
				fieldName = field.getName();
			} else {
				fieldName = propName;
			}
			
			Method setter = findSetter(beanClass, fieldName);
			Method getter = null;
			boolean wrapInJaxbElement = false;
			Class<?> paramType = null;
			if (setter == null) {
				// No setter. But if the property is multi-value we need to look
				// for a getter that returns a collection (Collection<Whatever>)
				getter = findPropertyGetter(beanClass, fieldName);
				if (getter == null) {
					throw new SchemaException("Cannot find setter or getter for field "+fieldName+" in "+beanClass);
				}
				Class<?> getterReturnType = getter.getReturnType();
				if (!Collection.class.isAssignableFrom(getterReturnType)) {
					throw new SchemaException("Cannot find getter for field "+fieldName+" in "+beanClass+" does not return collection, cannot use it to set value");
				}
				Type genericReturnType = getter.getGenericReturnType();
				Type typeArgument = getTypeArgument(genericReturnType, "for field "+fieldName+" in "+beanClass+", cannot determine collection type");
	//			System.out.println("type argument " + typeArgument);
				if (typeArgument instanceof Class) {
					paramType = (Class<?>) typeArgument;
				} else if (typeArgument instanceof ParameterizedType) {
					ParameterizedType paramTypeArgument = (ParameterizedType)typeArgument;
					Type rawTypeArgument = paramTypeArgument.getRawType();
					if (rawTypeArgument.equals(JAXBElement.class)) {
						// This is the case of Collection<JAXBElement<....>>
						wrapInJaxbElement = true;
						Type innerTypeArgument = getTypeArgument(typeArgument, "for field "+fieldName+" in "+beanClass+", cannot determine collection type (inner type argument)");
						if (innerTypeArgument instanceof Class) {
							// This is the case of Collection<JAXBElement<Whatever>>
							paramType = (Class<?>) innerTypeArgument;
						} else if (innerTypeArgument instanceof WildcardType) {
							// This is the case of Collection<JAXBElement<?>>
							// we need to exctract the specific type from the factory method
							if (elementMethod == null){
								throw new IllegalArgumentException("Wildcard type in JAXBElement field specification and no facotry method found for field "+fieldName+" in "+beanClass+", cannot determine collection type (inner type argument)");
							}
							Type factoryMethodGenericReturnType = elementMethod.getGenericReturnType();
							Type factoryMethodTypeArgument = getTypeArgument(factoryMethodGenericReturnType, "in factory method "+elementMethod+" return type for field "+fieldName+" in "+beanClass+", cannot determine collection type");
							if (factoryMethodTypeArgument instanceof Class) {
								// This is the case of JAXBElement<Whatever>
								paramType = (Class<?>) factoryMethodTypeArgument;
								if (Object.class.equals(paramType)) {
									throw new IllegalArgumentException("Factory method "+elementMethod+" type argument is Object for field "+
											fieldName+" in "+beanClass+", property "+propName);
								}
							} else {
								throw new IllegalArgumentException("Cannot determine factory method return type, got "+factoryMethodTypeArgument+" - for field "+fieldName+" in "+beanClass+", cannot determine collection type (inner type argument)");
							}
						} else {
							throw new IllegalArgumentException("Ejha! "+innerTypeArgument+" "+innerTypeArgument.getClass()+" from "+getterReturnType+" from "+fieldName+" in "+propName+" "+beanClass);
						}
					} else {
						// The case of Collection<Whatever<Something>>
						if (rawTypeArgument instanceof Class) {
							paramType = (Class<?>) rawTypeArgument;
						} else {
							throw new IllegalArgumentException("EH? Eh!? "+typeArgument+" "+typeArgument.getClass()+" from "+getterReturnType+" from "+fieldName+" in "+propName+" "+beanClass);
						}
					}
				} else {
					throw new IllegalArgumentException("EH? "+typeArgument+" "+typeArgument.getClass()+" from "+getterReturnType+" from "+fieldName+" in "+propName+" "+beanClass);
				}
			} else {
				Class<?> setterType = setter.getParameterTypes()[0];
				if (JAXBElement.class.equals(setterType)){
//					TODO some handling for the returned generic parameter types
					Type[] genericTypes = setter.getGenericParameterTypes();
					if (genericTypes.length != 1){
						throw new IllegalArgumentException("Too lazy to handle this.");
					}
					Type genericType = genericTypes[0];
					if (genericType instanceof ParameterizedType){
						Type actualType = getTypeArgument(genericType, "add some description");
						 if (actualType instanceof WildcardType) {
							 if (elementMethod == null) {
									objectFactory = getObjectFactory(beanClass.getPackage());
									elementMethod = findElementMethodInObjectFactory(objectFactory, propName);
								}
								// This is the case of Collection<JAXBElement<?>>
								// we need to exctract the specific type from the factory method
								if (elementMethod == null) {
									throw new IllegalArgumentException("Wildcard type in JAXBElement field specification and no facotry method found for field "+fieldName+" in "+beanClass+", cannot determine collection type (inner type argument)");
								}
								Type factoryMethodGenericReturnType = elementMethod.getGenericReturnType();
								Type factoryMethodTypeArgument = getTypeArgument(factoryMethodGenericReturnType, "in factory method "+elementMethod+" return type for field "+fieldName+" in "+beanClass+", cannot determine collection type");
								if (factoryMethodTypeArgument instanceof Class) {
									// This is the case of JAXBElement<Whatever>
									paramType = (Class<?>) factoryMethodTypeArgument;
									if (Object.class.equals(paramType)) {
										throw new IllegalArgumentException("Factory method "+elementMethod+" type argument is Object for field "+
												fieldName+" in "+beanClass+", property "+propName);
									}
								} else {
									throw new IllegalArgumentException("Cannot determine factory method return type, got "+factoryMethodTypeArgument+" - for field "+fieldName+" in "+beanClass+", cannot determine collection type (inner type argument)");
								}
						 }
					}
//					Class enclosing = paramType.getEnclosingClass();
//					Class clazz = paramType.getClass();
//					Class declaring = paramType.getDeclaringClass();
					wrapInJaxbElement = true;
				} else{
				paramType = setterType;
				}
			}
			
			if (Element.class.isAssignableFrom(paramType)) {
				// DOM!
				throw new IllegalArgumentException("DOM not supported in field "+fieldName+" in "+beanClass);
			}
			if (Object.class.equals(paramType)) {
				throw new IllegalArgumentException("Object property not supported in field "+fieldName+" in "+beanClass);
			}
			
			
						
			String paramNamespace = determineNamespace(paramType);
			
			//check for subclasses???
			if (xsubnode.getTypeQName()!= null){
				Class explicitParamType = getSchemaRegistry().determineCompileTimeClass(xsubnode.getTypeQName());
				if (explicitParamType != null && explicitParamType != null){
					paramType = explicitParamType; 
				}
			}
			
			Object propValue = null;
			Collection<Object> propValues = null;
			if (xsubnode instanceof ListXNode) {
				ListXNode xlist = (ListXNode)xsubnode;
				if (setter != null) {
					propValue = convertSinglePropValue(xsubnode, fieldName, paramType, beanClass, paramNamespace);
				} else {
					// No setter, we have to use collection getter
					propValues = new ArrayList<>(xlist.size());
					for(XNode xsubsubnode: xlist) {
						propValues.add(convertSinglePropValue(xsubsubnode, fieldName, paramType, beanClass, paramNamespace));
					}
				}
			} else {
				propValue = convertSinglePropValue(xsubnode, fieldName, paramType, beanClass, paramNamespace);
			}
			
			if (setter != null) {
				try {
					setter.invoke(bean, wrapInJaxb(propValue, wrapInJaxbElement, objectFactory, elementMethod, propName, beanClass));
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new SystemException("Cannot invoke setter "+setter+" on bean of type "+beanClass+": "+e.getMessage(), e);
				}
			} else if (getter != null) {
				Object getterReturn;
				Collection<Object> col;
				try {
					getterReturn = getter.invoke(bean);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new SystemException("Cannot invoke getter "+getter+" on bean of type "+beanClass+": "+e.getMessage(), e);
				}
				try {
					col = (Collection<Object>)getterReturn;
				} catch (ClassCastException e) {
					throw new SystemException("Getter "+getter+" on bean of type "+beanClass+" returned "+getterReturn+" instead of collection");
				}
				if (propValue != null) {
					col.add(wrapInJaxb(propValue, wrapInJaxbElement, objectFactory, elementMethod, propName, beanClass));
				} else if (propValues != null) {
					for (Object propVal: propValues) {
						col.add(wrapInJaxb(propVal, wrapInJaxbElement, objectFactory, elementMethod, propName, beanClass));
					}
				} else {
					throw new IllegalStateException("Strange. Multival property "+propName+" in "+beanClass+" produced null values list, parsed from "+xnode);
				}
			} else {
				throw new IllegalStateException("Uh?");
			}
		}
		
		if (prismContext != null && bean instanceof Revivable) {
			((Revivable)bean).revive(prismContext);
		}
		
		return bean;
	}

    // parses any subtype of SearchFilterType
	private <T extends SearchFilterType> T unmarshalSearchFilterType(MapXNode xmap, Class<T> beanClass) throws SchemaException {
		if (xmap == null) {
			return null;
		}
        T filterType;
        try {
            filterType = beanClass.newInstance();
        } catch (InstantiationException|IllegalAccessException e) {
            throw new SystemException("Cannot instantiate " + beanClass + ": " + e.getMessage(), e);
        }
        filterType.parseFromXNode(xmap);
		return filterType;
	}
	
	private XNode marshalSearchFilterType(SearchFilterType value) throws SchemaException {
		if (value == null) {
			return null;
		}
		return value.serializeToXNode(prismContext);
	}

	
	private Type getTypeArgument(Type origType, String desc) {
		if (!(origType instanceof ParameterizedType)) {
			throw new IllegalArgumentException("No a parametrized type "+desc);
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
	
	private <T> Object wrapInJaxb(Object propVal, boolean wrapInJaxbElement, Object objectFactory, Method factoryMehtod, String propName, Class beanClass) {
		if (wrapInJaxbElement) {
			if (factoryMehtod == null) {
				throw new IllegalArgumentException("Param type is JAXB element but no factory method found for it, property "+propName+" in "+beanClass);
			}
			try {
				return factoryMehtod.invoke(objectFactory, propVal);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SystemException("Unable to ivokeke factory method "+factoryMehtod+" on "+objectFactory.getClass()+" for property "+propName+" in "+beanClass);
			}
		} else {
			return propVal;
		}
	}

	private Method findElementMethodInObjectFactory(Object objectFactory, String propName) {
		Class<? extends Object> objectFactoryClass = objectFactory.getClass();
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
	
	private Field lookupSubstitution(Class beanClass, Method elementMethod) {
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


	private Object getObjectFactory(Package pkg) {
		Class objectFactoryClass;
		try {
			objectFactoryClass = Class.forName(pkg.getName()+".ObjectFactory");
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Cannot find object factory class in package "+pkg.getName()+": "+e.getMessage(), e);
		}
		try {
			return objectFactoryClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException("Cannot instantiate object factory class in package "+pkg.getName()+": "+e.getMessage(), e);
		}
	}
	
	private Object convertSinglePropValue(XNode xsubnode, String fieldName, Class paramType, Class classType, String schemaNamespace) throws SchemaException {
		Object propValue;
		if (paramType.equals(XNode.class)) {
			propValue = xsubnode;
		} else if (paramType.equals(RawType.class)) {
            propValue = new RawType(xsubnode);
        } else {
			if (xsubnode instanceof PrimitiveXNode<?>) {
				propValue = unmarshallPrimitive(((PrimitiveXNode<?>)xsubnode), paramType);
			} else if (xsubnode instanceof MapXNode) {
				propValue = unmarshall((MapXNode)xsubnode, paramType);
			} else if (xsubnode instanceof ListXNode) {
				ListXNode xlist = (ListXNode)xsubnode;
				if (xlist.size() > 1) {
					throw new SchemaException("Cannot set multi-value value to a single valued property "+fieldName+" of "+classType);
				} else {
					if (xlist.isEmpty()) {
						propValue = null;
					} else {
						propValue = xlist.get(0);
					}
				}
			} else {
				throw new IllegalArgumentException("Cannot parse "+xsubnode+" to a bean "+classType);
			}
		}
		return propValue;
	}

	private <T> Field findPropertyField(Class<T> classType, String propName) {
		Field field = findPropertyFieldExact(classType, propName);
		if (field != null) {
			return field;
		}
		// Fields for some reserved words are prefixed by underscore, so try also this.
		return findPropertyFieldExact(classType, "_"+propName);
	}
	
	private <T> Field findPropertyFieldExact(Class<T> classType, String propName) {
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
	
	private <T> Method findPropertyGetter(Class<T> classType, String propName) {
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
		String getterName = "get"+StringUtils.capitalize(propName);
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
	
	private List<String> getPropOrder(Class<? extends Object> beanClass) {
		List<String> propOrder;
		
		// Superclass first!
		Class superclass = beanClass.getSuperclass();
		if (superclass.equals(Object.class) || superclass.getAnnotation(XmlType.class) == null) {
			propOrder = new ArrayList<>();
		} else {
			propOrder = getPropOrder(superclass);
		}
		
		XmlType xmlType = beanClass.getAnnotation(XmlType.class);
		if (xmlType == null) {
			throw new IllegalArgumentException("Cannot marshall "+beanClass+" it does not have @XmlType annotation");
		}
		
		String[] myPropOrder = xmlType.propOrder();
		if (myPropOrder != null) {
			for (String myProp: myPropOrder) {
				if (!StringUtils.isBlank(myProp)) {
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

	
	public <T> T unmarshallPrimitive(PrimitiveXNode<?> xprim, QName typeQName) throws SchemaException {
		Class<T> classType = getSchemaRegistry().determineCompileTimeClass(typeQName);
		return unmarshallPrimitive(xprim, classType);
	}
	
	public <T> T unmarshallPrimitive(PrimitiveXNode<?> xprim, Class<T> classType) throws SchemaException {
		if (XmlTypeConverter.canConvert(classType)) {
			// Trivial case, direct conversion
			QName xsdType = XsdTypeMapper.toXsdType(classType);
			T primValue = postConvertUnmarshall(xprim.getParsedValue(xsdType));
			return primValue;
		}
		
		if (RawType.class.isAssignableFrom(classType)) {
			RawType rawType = new RawType(xprim);
			return (T) rawType;
		}
		
		if (ItemPathType.class.isAssignableFrom(classType)){
			QName typeQName = xprim.getTypeQName();
			if (typeQName == null) {
				typeQName = DOMUtil.XSD_STRING;
			}
			Object parsedValue = xprim.getParsedValue(ItemPathType.COMPLEX_TYPE);
			T primValue = postConvertUnmarshall(parsedValue);
//			ItemPath itemPath = new RawType();
//			rawType.setType(typeQName);
//			rawType.setValue(parsedValue);
			return (T) primValue;
		}
		
		if (SearchFilterType.class.isAssignableFrom(classType)){
			throw new SchemaException("Cannot unmarshall search filter from "+xprim);
		}
		
		if (xprim.isEmpty() && !classType.isEnum()) {
			// Special case. Just return empty object
			try {
				return classType.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new SystemException("Cannot instantiate "+classType+": "+e.getMessage(), e);
			}
		}
		
		if (!classType.isEnum()) {
			throw new SystemException("Cannot convert primitive value to non-enum bean of type "+classType);
		}
		// Assume string, maybe TODO extend later
		String primValue = (String) xprim.getParsedValue(DOMUtil.XSD_STRING);
		if (StringUtils.isBlank(primValue)) {
			return null;
		}
		primValue = StringUtils.trim(primValue);
		
		String javaEnumString = findEnumFieldName(classType, primValue);
//		for (Field field: classType.getDeclaredFields()) {
//			XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
//			if (xmlEnumValue != null && xmlEnumValue.value() != null && xmlEnumValue.value().equals(primValue)) {
//				javaEnumString = field.getName();
//				break;
//			}
//		}
		
		if (javaEnumString == null) {
			for (Field field: classType.getDeclaredFields()) {
				if (field.getName().equals(primValue)) {
					javaEnumString = field.getName();
					break;
				}
			}
		}
		
		if (javaEnumString == null) {
			throw new SchemaException("Cannot find enum value for string '"+primValue+"' in "+classType);
		}
		
		T bean = (T) Enum.valueOf((Class<Enum>)classType, javaEnumString);
		
		return bean;
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

    private <T> String findEnumFieldName(Class classType, T primValue){
		for (Field field: classType.getDeclaredFields()) {
			XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
			if (xmlEnumValue != null && xmlEnumValue.value() != null && xmlEnumValue.value().equals(primValue)) {
				return field.getName();
			}
		}
		return null;
	}
	
	private <T> T postConvertUnmarshall(Object parsedPrimValue) {
		if (parsedPrimValue == null) {
			return null;
		}
		if (parsedPrimValue instanceof ItemPath) {
			return (T) new ItemPathType((ItemPath)parsedPrimValue);
		} else {
			return (T) parsedPrimValue;
		}
	}

	public <T> XNode marshall(T bean) throws SchemaException {
		if (bean == null) {
			return null;
		}
		if (bean instanceof ObjectDeltaType){
		System.out.println("prism bean marshalling: " + bean);
		}
		MapXNode xmap = new MapXNode();
				
		Class<? extends Object> beanClass = bean.getClass();
		
		//check for enums
		if (beanClass.isEnum()){
			String enumValue = XNodeProcessorUtil.findEnumFieldValue(beanClass, bean);
			if (StringUtils.isEmpty(enumValue)){
				enumValue = bean.toString();
			}
			QName fieldTypeName = findFieldTypeName(null, beanClass, DEFAULT_NAMESPACE_PLACEHOLDER);
			return createPrimitiveXNode(enumValue, fieldTypeName, false);
//			return marshallValue(bean, fieldTypeName, false);
		}
		
		XmlType xmlType = beanClass.getAnnotation(XmlType.class);
		if (xmlType == null) {
			throw new IllegalArgumentException("Cannot marshall "+beanClass+" it does not have @XmlType annotation");
		}
		
		String namespace = determineNamespace(beanClass);
		if (namespace == null) {
			throw new IllegalArgumentException("Cannot determine namespace of "+beanClass);
		}
		
		List<String> propOrder = getPropOrder(beanClass);
		for (String fieldName: propOrder) {
			QName elementName = new QName(namespace, fieldName);
			Method getter = findPropertyGetter(beanClass, fieldName);
			if (getter == null) {
				throw new IllegalStateException("No getter for field "+fieldName+" in "+beanClass);
			}
			Object getterResult;
			try {
				getterResult = getter.invoke(bean);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SystemException("Cannot invoke method for field "+fieldName+" in "+beanClass+": "+e.getMessage(), e);
			}
			
			if (getterResult == null) {
				continue;
			}
			
			Field field = findPropertyField(beanClass, fieldName);
			boolean isAttribute = isAttribute(field, getter);
			
			if (getterResult instanceof Collection<?>) {
				Collection col = (Collection)getterResult;
				if (col.isEmpty()) {
					continue;
				}
				Iterator i = col.iterator();
				if (i == null) {
					// huh?!? .. but it really happens
					throw new IllegalArgumentException("Iterator of collection returned from "+getter+" is null");
				}
				Object getterResultValue = i.next();
				if (getterResultValue == null) {
					continue;
				}
				
				QName fieldTypeName = findFieldTypeName(field, getterResultValue.getClass(), namespace);
								
				ListXNode xlist = new ListXNode();
				boolean isJaxb = false;
				for (Object element: col) {
					Object elementToMarshall = element;
					if (element instanceof JAXBElement){
						if (((JAXBElement) element).getName() != null){
							elementName = ((JAXBElement) element).getName(); 
						}
						elementToMarshall = ((JAXBElement) element).getValue();
//						xmap.put(elementName, marshallValue(elementToMarshall, fieldTypeName, isAttribute));
//						isJaxb = true;
//						continue;
					} 
					XNode marshalled = marshallValue(elementToMarshall, fieldTypeName, isAttribute);
					setExplicitTypeDeclarationIfNeeded(getter, getterResultValue, marshalled, fieldTypeName);
					xlist.add(marshalled);
				}
//				if (!isJaxb){
					xmap.put(elementName, xlist);
//				}
			} else {
				QName fieldTypeName = findFieldTypeName(field, getterResult.getClass(), namespace);
				Object valueToMarshall = null;
				if (getterResult instanceof JAXBElement){
					valueToMarshall = ((JAXBElement) getterResult).getValue();
					elementName = ((JAXBElement) getterResult).getName();
				} else{
					valueToMarshall = getterResult;
				}
				XNode marshelled = marshallValue(valueToMarshall, fieldTypeName, isAttribute);
				if (!getter.getReturnType().equals(valueToMarshall.getClass()) && getter.getReturnType().isAssignableFrom(valueToMarshall.getClass())){
					if (prismContext != null){
					PrismObjectDefinition def = prismContext.getSchemaRegistry().determineDefinitionFromClass(valueToMarshall.getClass());
					if (def != null){
						QName type = def.getTypeName();
						System.out.println("setting type def: " + type);
						marshelled.setTypeQName(type);
						marshelled.setExplicitTypeDeclaration(true);
					}
					}
				}
				xmap.put(elementName, marshelled);
				
//				setExplicitTypeDeclarationIfNeeded(getter, valueToMarshall, xmap, fieldTypeName);
			}
		}
		
		return xmap;
	}
	
	public void revive(Object bean, final PrismContext prismContext) throws SchemaException {
		Handler<Object> visitor = new Handler<Object>() {
			@Override
			public boolean handle(Object o) {
				if (o instanceof Revivable) {
					try {
						((Revivable)o).revive(prismContext);
					} catch (SchemaException e) {
						throw new TunnelException(e);
					}
				}
				return true;
			}
		};
		try {
			visit(bean,visitor);
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
		
		XmlType xmlType = beanClass.getAnnotation(XmlType.class);
		if (xmlType == null) {
			// no @XmlType annotation, we are not interested to go any deeper
			return;
		}
		
		List<String> propOrder = getPropOrder(beanClass);
		for (String fieldName: propOrder) {
			Method getter = findPropertyGetter(beanClass, fieldName);
			if (getter == null) {
				throw new IllegalStateException("No getter for field "+fieldName+" in "+beanClass);
			}
			Object getterResult;
			try {
				getterResult = getter.invoke(bean);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SystemException("Cannot invoke method for field "+fieldName+" in "+beanClass+": "+e.getMessage(), e);
			}
			
			if (getterResult == null) {
				continue;
			}
			
			if (getterResult instanceof Collection<?>) {
				Collection col = (Collection)getterResult;
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
			elementToMarshall = ((JAXBElement) element).getValue();
		} 
		visit(elementToMarshall, handler);
	}

	private void setExplicitTypeDeclarationIfNeeded(Method getter, Object getterResult, XNode xmap, QName fieldTypeName){
		Class getterReturnType = getter.getReturnType();
		Class getterType = null;
		if (Collection.class.isAssignableFrom(getterReturnType)){
			Type genericReturnType = getter.getGenericReturnType();
			if (genericReturnType instanceof ParameterizedType){
				Type actualType = getTypeArgument(genericReturnType, "explicit type declaration");
				 
				if (actualType instanceof Class){
					getterType = (Class) actualType;
				}
			}
		} 
		if (getterType == null){
			getterType = getterReturnType;
		}
		Class getterResultReturnType = getterResult.getClass();
		if (getterType != getterResultReturnType && getterType.isAssignableFrom(getterResultReturnType)){
			xmap.setExplicitTypeDeclaration(true);
			xmap.setTypeQName(fieldTypeName);
		}
	}
	
	private boolean isAttribute(Field field, Method getter){
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
	private String determineNamespace(Class<? extends Object> beanClass) {
		XmlType xmlType = beanClass.getAnnotation(XmlType.class);
		if (xmlType == null) {
			return null;
		}
		
		String namespace = xmlType.namespace();
		if (namespace == null || DEFAULT_NAMESPACE_PLACEHOLDER.equals(namespace)) {
			XmlSchema xmlSchema = beanClass.getPackage().getAnnotation(XmlSchema.class);
			namespace = xmlSchema.namespace();
		}
		if (StringUtils.isBlank(namespace) || DEFAULT_NAMESPACE_PLACEHOLDER.equals(namespace)) {
			return null;
		}
		
		return namespace;
	}

	private <T> XNode marshallValue(T value, QName fieldTypeName, boolean isAttribute) throws SchemaException {
		if (value == null) {
			return null;
		}
		if (value instanceof ItemPathType){
			return marshalItemPath((ItemPathType) value);
		} else if (value instanceof SearchFilterType){
			return marshalSearchFilterType((SearchFilterType) value);
		} else if (value instanceof RawType){
			return marshalRawValue((RawType) value);
		} else		
		if (canProcess(value.getClass())) {
			// This must be a bean
			return marshall(value);
		} else {
			// primitive value
			return createPrimitiveXNode(value, fieldTypeName, isAttribute);
		}
	}
	
	private <T> PrimitiveXNode<T> createPrimitiveXNode(T value, QName fieldTypeName, boolean isAttribute){
		PrimitiveXNode<T> xprim = new PrimitiveXNode<T>();
		xprim.setValue(value);
		xprim.setTypeQName(fieldTypeName);
		xprim.setAttribute(isAttribute);
		return xprim;
	}

	private XNode marshalRawValue(RawType value) throws SchemaException {
        return value.serializeToXNode();
	}

	private XNode marshalItemPath(ItemPathType itemPath){
		PrimitiveXNode xprim = new PrimitiveXNode<>();
		ItemPath path = itemPath.getItemPath();
//		XPathHolder holder = new XPathHolder(path);
//		xprim.setValue(holder.getXPath());
		xprim.setValue(path);
		xprim.setTypeQName(ItemPathType.COMPLEX_TYPE);
		return xprim;
	}
	
	private <T> Method findSetter(Class<T> classType, String fieldName) {
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
	
	private String getPropertyNameFromGetter(String getterName) {
		if ((getterName.length() > 3) && getterName.startsWith("get") && 
				Character.isUpperCase(getterName.charAt(3))) {
			String propPart = getterName.substring(3);
			return StringUtils.uncapitalize(propPart);
		}
		return getterName;
	}

	private String getSetterName(String fieldName) {
		if (fieldName.startsWith("_")) {
			fieldName = fieldName.substring(1);
		}
		return "set"+StringUtils.capitalize(fieldName);
	}
	
	private QName findFieldTypeName(Field field, Class fieldType, String schemaNamespace) {
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
					if (propTypeNamespace == null || propTypeNamespace.equals(DEFAULT_NAMESPACE_PLACEHOLDER)) {
						propTypeNamespace = schemaNamespace;
					}
					propTypeQname = new QName(propTypeNamespace, propTypeLocalPart);
				}
			}	
		}
		
		return propTypeQname;
	}

}
 