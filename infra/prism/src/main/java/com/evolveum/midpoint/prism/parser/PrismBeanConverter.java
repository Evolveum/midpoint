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
import java.util.Collection;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

public class PrismBeanConverter {
	
	public static final String DEFAULT_NAMESPACE_PLACEHOLDER = "##default";
	
	private SchemaRegistry schemaRegistry;

	public PrismBeanConverter(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}

	public boolean canConvert(QName typeName) {
		return schemaRegistry.determineCompileTimeClass(typeName) != null; 
	}
	
	public boolean canConvert(Class<?> clazz) {
		return clazz.getAnnotation(XmlType.class) != null;
	}
	
	public <T> T unmarshall(MapXNode xnode, QName typeQName) throws SchemaException {
		Class<T> classType = schemaRegistry.determineCompileTimeClass(typeQName);
		return unmarshall(xnode, classType);
	}
	
	public <T> T unmarshall(MapXNode xnode, Class<T> classType) throws SchemaException {
		T bean;
		try {
			bean = classType.newInstance();
		} catch (InstantiationException e) {
			throw new SystemException("Cannot instantiate bean of type "+classType+": "+e.getMessage(), e);
		} catch (IllegalAccessException e) {
			throw new SystemException("Cannot instantiate bean of type "+classType+": "+e.getMessage(), e);
		}
		for (Entry<QName,XNode> entry: xnode.entrySet()) {
			QName key = entry.getKey();
			XNode xsubnode = entry.getValue();
			String propName = key.getLocalPart();
			Field field = findPropertyField(classType, propName);
			if (field == null) {
				throw new SchemaException("No field "+propName+" in class "+classType);
			}
			Method setter = findSetter(classType, field);
			Class<?> setterParamType = setter.getParameterTypes()[0];
			
			Object propValue;
			if (xsubnode instanceof PrimitiveXNode<?>) {
				QName propTypeQname = findFieldTypeName(field, setterParamType);
				if (propTypeQname == null) {
					throw new SchemaException("No mapping for class "+setterParamType+" while processing field "+field+" of "+classType);
				}
				propValue = ((PrimitiveXNode<?>)xsubnode).getParsedValue(propTypeQname);
			} else if (xsubnode instanceof MapXNode) {
				propValue = unmarshall((MapXNode)xsubnode, setterParamType);
			} else {
				// TODO: list
				throw new IllegalArgumentException("Cannot parse "+xsubnode+" to a bean "+classType);
			}
			
			try {
				setter.invoke(bean, propValue);
			} catch (IllegalAccessException e) {
				throw new SystemException("Cannot invoke setter "+setter+" on bean of type "+classType+": "+e.getMessage(), e);
			} catch (IllegalArgumentException e) {
				throw new SystemException("Cannot invoke setter "+setter+" on bean of type "+classType+": "+e.getMessage(), e);
			} catch (InvocationTargetException e) {
				throw new SystemException("Cannot invoke setter "+setter+" on bean of type "+classType+": "+e.getMessage(), e);
			}
		}
		
		return bean;
	}
	
	private <T> Field findPropertyField(Class<T> classType, String propName) throws SchemaException {
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

	public <T> T unmarshallPrimitive(PrimitiveXNode<?> xprim, QName typeQName) throws SchemaException {
		Class<T> classType = schemaRegistry.determineCompileTimeClass(typeQName);
		return unmarshallPrimitive(xprim, classType);
	}
	
	public <T> T unmarshallPrimitive(PrimitiveXNode<?> xprim, Class<T> classType) throws SchemaException {
		if (!classType.isEnum()) {
			throw new SystemException("Cannot convert primitive value to non-enum bean of type "+classType);
		}
		// Assume string, maybe TODO extend later
		String primValue = (String) xprim.getParsedValue(DOMUtil.XSD_STRING);
		if (StringUtils.isBlank(primValue)) {
			return null;
		}
		primValue = StringUtils.trim(primValue);
		
		String javaEnumString = null;
		for (Field field: classType.getDeclaredFields()) {
			XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
			if (xmlEnumValue != null && xmlEnumValue.value() != null && xmlEnumValue.value().equals(primValue)) {
				javaEnumString = field.getName();
				break;
			}
		}
		
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
	
	public <T> MapXNode marshall(T bean) {
		if (bean == null) {
			return null;
		}
		
		MapXNode xmap = new MapXNode();
		
		Class<? extends Object> beanClass = bean.getClass();
		XmlType xmlType = beanClass.getAnnotation(XmlType.class);
		if (xmlType == null) {
			throw new IllegalArgumentException("Cannot marshall "+beanClass+" it does not have @XmlType annotation");
		}
		
		String namespace = xmlType.namespace();
		if (namespace == null || DEFAULT_NAMESPACE_PLACEHOLDER.equals(namespace)) {
			XmlSchema xmlSchema = beanClass.getPackage().getAnnotation(XmlSchema.class);
			namespace = xmlSchema.namespace();
		}
		if (StringUtils.isBlank(namespace) || DEFAULT_NAMESPACE_PLACEHOLDER.equals(namespace)) {
			throw new IllegalArgumentException("Cannot marshall "+beanClass+": cannot determine namespace ("+namespace+")");
		}
		
		String[] propOrder = xmlType.propOrder();
		for (String fieldName: propOrder) {
			QName elementName = new QName(namespace, fieldName);
			String getterName = getGetterName(fieldName);
			Method getter;
			try {
				getter = beanClass.getMethod(getterName);
			} catch (NoSuchMethodException e) {
				throw new IllegalStateException("No getter "+getterName+" for field "+fieldName+" in "+beanClass, e);
			} catch (SecurityException e) {
				throw new SystemException("Cannot accesss method "+getterName+" in "+beanClass+": "+e.getMessage(), e);
			}
			Object getterResult;
			try {
				getterResult = getter.invoke(bean);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SystemException("Cannot invoke method "+getterName+" in "+beanClass+": "+e.getMessage(), e);
			}
			
			if (getterResult == null) {
				continue;
			}
			
			Field field;
			try {
				field = beanClass.getDeclaredField(fieldName);
			} catch (NoSuchFieldException | SecurityException e) {
				throw new SystemException("Cannot accesss field "+fieldName+" in "+beanClass+": "+e.getMessage(), e);
			}
			
			if (getterResult instanceof Collection<?>) {
				Collection col = (Collection)getterResult;
				if (col.isEmpty()) {
					continue;
				}
				QName fieldTypeName = findFieldTypeName(field, col.iterator().next().getClass());
				ListXNode xlist = new ListXNode();
				for (Object element: col) {
					xlist.add(marshallValue(element, fieldTypeName));
				}
				xmap.put(elementName, xlist);
			} else {
				QName fieldTypeName = findFieldTypeName(field, getterResult.getClass());
				xmap.put(elementName, marshallValue(getterResult, fieldTypeName));
			}
		}
		
		return xmap;
	}

	private <T> XNode marshallValue(T value, QName fieldTypeName) {
		if (value == null) {
			return null;
		}
		if (canConvert(value.getClass())) {
			// This must be a bean
			return marshall(value);
		} else {
			// primitive value
			PrimitiveXNode<T> xprim = new PrimitiveXNode<T>();
			xprim.setValue(value);
			xprim.setTypeQName(fieldTypeName);
			return xprim;
		}
	}

	private String getGetterName(String fieldName) {
		return "get"+StringUtils.capitalize(fieldName);
	}
	
	private <T> Method findSetter(Class<T> classType, Field field) {
		String setterName = getSetterName(field);
		for(Method method: classType.getMethods()) {
			if (!method.getName().equals(setterName)) {
				continue;
			}
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes.length != 1) {
				continue;
			}
			Class<?> setterType = parameterTypes[0];
			// TODO: check for multiple setters?
			return method;
		}
		return null;
	}

	private String getSetterName(Field field) {
		return "set"+StringUtils.capitalize(field.getName());
	}

	private QName findFieldTypeName(Field field, Class fieldType) {
		QName propTypeQname = null;
		XmlSchemaType xmlSchemaType = field.getAnnotation(XmlSchemaType.class);
		if (xmlSchemaType != null) {
			String propTypeLocalPart = xmlSchemaType.name();
			if (propTypeLocalPart != null) {
				String propTypeNamespace = xmlSchemaType.namespace();
				if (propTypeNamespace == null) {
					propTypeNamespace = DOMUtil.W3C_XML_SCHEMA_XMLNS_URI;
				}
				propTypeQname = new QName(propTypeNamespace, propTypeLocalPart);
			}
		}
		if (propTypeQname == null) {
			propTypeQname = XsdTypeMapper.getJavaToXsdMapping(fieldType);
		}
		return propTypeQname;
	}

}
