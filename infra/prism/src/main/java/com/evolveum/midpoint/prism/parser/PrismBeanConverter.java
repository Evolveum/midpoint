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
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

public class PrismBeanConverter {
	
	private SchemaRegistry schemaRegistry;

	public PrismBeanConverter(SchemaRegistry schemaRegistry) {
		this.schemaRegistry = schemaRegistry;
	}

	public boolean canConvert(QName typeName) {
		return schemaRegistry.determineCompileTimeClass(typeName) != null; 
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
			Field field;
			try {
				field = classType.getDeclaredField(propName);
			} catch (NoSuchFieldException e) {
				throw new SchemaException("No field "+propName+" in class "+classType, e);
			}
			Method setter = findSetter(classType, field);
			Class<?> setterParamType = setter.getParameterTypes()[0];
			
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
				propTypeQname = XsdTypeMapper.getJavaToXsdMapping(setterParamType);
				if (propTypeQname == null) {
					throw new SchemaException("No mapping for class "+setterParamType+" while processing field "+field+" of "+classType);
				}
			}
			
			Object propValue;
			if (xsubnode instanceof PrimitiveXNode<?>) {
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
	
}
