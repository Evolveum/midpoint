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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.Raw;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.prism.parser.util.XNodeProcessorUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.SchemaXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EncryptedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedByteArrayType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.XmlAsStringType;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class PrismBeanConverter {

    private static final Trace LOGGER = TraceManager.getTrace(PrismBeanConverter.class);

    public static final String DEFAULT_PLACEHOLDER = "##default";

    private PrismBeanInspector inspector;
	
	private PrismContext prismContext;

	public PrismBeanConverter(PrismContext prismContext) {
		this.prismContext = prismContext;
        this.inspector = new PrismBeanInspector(prismContext);
	}
	
	public PrismContext getPrismContext() {
		return prismContext;
	}

//	public void setPrismContext(PrismContext prismContext) {
//		this.prismContext = prismContext;
//	}

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
		return RawType.class.equals(clazz) || clazz.getAnnotation(XmlType.class) != null;
	}
	
	public <T> T unmarshall(MapXNode xnode, QName typeQName) throws SchemaException {
		Class<T> classType = getSchemaRegistry().determineCompileTimeClass(typeQName);
		return unmarshall(xnode, classType);
	}

	public <T> T unmarshall(MapXNode xnode, Class<T> beanClass) throws SchemaException {

        if (PolyStringType.class.equals(beanClass)) {
            PolyString polyString = unmarshalPolyString(xnode);
            return (T) polyString;
        } else if (ProtectedStringType.class.equals(beanClass)) {
            ProtectedStringType protectedType = new ProtectedStringType();
            XNodeProcessorUtil.parseProtectedType(protectedType, xnode, prismContext);
            return (T) protectedType;
        } else if (ProtectedByteArrayType.class.equals(beanClass)) {
            ProtectedByteArrayType protectedType = new ProtectedByteArrayType();
            XNodeProcessorUtil.parseProtectedType(protectedType, xnode, prismContext);
            return (T) protectedType;
        } else if (SchemaDefinitionType.class.equals(beanClass)) {
            SchemaDefinitionType schemaDefType = unmarshalSchemaDefinitionType(xnode);
            return (T) schemaDefType;
        } else if (prismContext.getSchemaRegistry().determineDefinitionFromClass(beanClass) != null) {
			return (T) prismContext.getXnodeProcessor().parseObject(xnode).asObjectable();			
		} else if (XmlAsStringType.class.equals(beanClass)) {
            // reading a string represented a XML-style content
            // used e.g. when reading report templates (embedded XML)
            // A necessary condition: there may be only one map entry.
            if (xnode.size() > 1) {
                throw new SchemaException("Map with more than one item cannot be parsed as a string: " + xnode);
            } else if (xnode.isEmpty()) {
                return (T) new XmlAsStringType();
            } else {
                Map.Entry<QName,XNode> entry = xnode.entrySet().iterator().next();
                DomParser domParser = prismContext.getParserDom();
                String value = domParser.serializeToString(entry.getValue(), entry.getKey());
                return (T) new XmlAsStringType(value);
            }
        }
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
			Field field = inspector.findPropertyField(beanClass, propName);
			Method propertyGetter = null;
			if (field == null) {
				propertyGetter = inspector.findPropertyGetter(beanClass, propName);
			}

			Method elementMethod = null;
			Object objectFactory = null;
			if (field == null && propertyGetter == null) {
				// We have to try to find a more generic field, such as xsd:any (TODO) or substitution element
				// check for global element definition first
                Class objectFactoryClass = inspector.getObjectFactoryClass(beanClass.getPackage());
				objectFactory = instantiateObjectFactory(objectFactoryClass);
				elementMethod = inspector.findElementMethodInObjectFactory(objectFactoryClass, propName);
				if (elementMethod == null) {
					throw new SchemaException("No field "+propName+" in class "+beanClass+" (and no element method in object factory too)");
				}
				field = inspector.lookupSubstitution(beanClass, elementMethod);
				if (field == null) {
					throw new SchemaException("No field "+propName+" in class "+beanClass+" (and no suitable substitution too)");
				}
			}

            boolean storeAsRawType;
            if (elementMethod != null) {
                storeAsRawType = elementMethod.getAnnotation(Raw.class) != null;
            } else if (propertyGetter != null) {
                storeAsRawType = propertyGetter.getAnnotation(Raw.class) != null;
            } else {
                storeAsRawType = field.getAnnotation(Raw.class) != null;
            }

			String fieldName;
			if (field != null) {
				fieldName = field.getName();
			} else {
				fieldName = propName;
			}
			
			Method setter = inspector.findSetter(beanClass, fieldName);
			Method getter = null;
			boolean wrapInJaxbElement = false;
			Class<?> paramType = null;
			if (setter == null) {
				// No setter. But if the property is multi-value we need to look
				// for a getter that returns a collection (Collection<Whatever>)
				getter = inspector.findPropertyGetter(beanClass, fieldName);
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
                                // TODO: TEMPORARY CODE!!!!!!!!!! fix in 3.1 [med]
                                Class objectFactoryClass = inspector.getObjectFactoryClass(beanClass.getPackage());
                                objectFactory = instantiateObjectFactory(objectFactoryClass);
                                elementMethod = inspector.findElementMethodInObjectFactory(objectFactoryClass, propName);
                                if (elementMethod == null) {
                                    throw new IllegalArgumentException("Wildcard type in JAXBElement field specification and no factory method found for field "+fieldName+" in "+beanClass+", cannot determine collection type (inner type argument)");
                                }
							}
							Type factoryMethodGenericReturnType = elementMethod.getGenericReturnType();
							Type factoryMethodTypeArgument = getTypeArgument(factoryMethodGenericReturnType, "in factory method "+elementMethod+" return type for field "+fieldName+" in "+beanClass+", cannot determine collection type");
							if (factoryMethodTypeArgument instanceof Class) {
								// This is the case of JAXBElement<Whatever>
								paramType = (Class<?>) factoryMethodTypeArgument;
								if (Object.class.equals(paramType) && !storeAsRawType) {
									throw new IllegalArgumentException("Factory method "+elementMethod+" type argument is Object (and not @Raw) for field "+
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
									Class objectFactoryClass = inspector.getObjectFactoryClass(beanClass.getPackage());
                                    objectFactory = instantiateObjectFactory(objectFactoryClass);
									elementMethod = inspector.findElementMethodInObjectFactory(objectFactoryClass, propName);
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
									if (Object.class.equals(paramType) && !storeAsRawType) {
										throw new IllegalArgumentException("Factory method "+elementMethod+" type argument is Object (without @Raw) for field "+
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
				} else {
                    paramType = setterType;
				}
			}
			
			if (Element.class.isAssignableFrom(paramType)) {
				// DOM!
				throw new IllegalArgumentException("DOM not supported in field "+fieldName+" in "+beanClass);
			}
			if (Object.class.equals(paramType) && !storeAsRawType) {
				throw new IllegalArgumentException("Object property (without @Raw) not supported in field "+fieldName+" in "+beanClass);
			}

			String paramNamespace = inspector.determineNamespace(paramType);
			
			//check for subclasses???
			if (!storeAsRawType && xsubnode.getTypeQName() != null) {
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
					propValue = convertSinglePropValue(xsubnode, fieldName, paramType, storeAsRawType, beanClass, paramNamespace);
				} else {
					// No setter, we have to use collection getter
					propValues = new ArrayList<>(xlist.size());
					for(XNode xsubsubnode: xlist) {
						propValues.add(convertSinglePropValue(xsubsubnode, fieldName, paramType, storeAsRawType, beanClass, paramNamespace));
					}
				}
			} else {
				propValue = convertSinglePropValue(xsubnode, fieldName, paramType, storeAsRawType, beanClass, paramNamespace);
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
				throw new IllegalStateException("Uh? No setter nor getter.");
			}
		}
		
		if (prismContext != null && bean instanceof Revivable) {
			((Revivable)bean).revive(prismContext);
		}
		
		return bean;
	}

    private Object instantiateObjectFactory(Class objectFactoryClass) {
        try {
            return objectFactoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot instantiate object factory class "+objectFactoryClass.getName()+": "+e.getMessage(), e);
        }
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
	
	private MapXNode marshalSearchFilterType(SearchFilterType value) throws SchemaException {
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

	private Object convertSinglePropValue(XNode xsubnode, String fieldName, Class paramType, boolean storeAsRawType, Class classType, String schemaNamespace) throws SchemaException {
		Object propValue;
		if (paramType.equals(XNode.class)) {
			propValue = xsubnode;
		} else if (storeAsRawType || paramType.equals(RawType.class)) {
            propValue = new RawType(xsubnode, prismContext);
        } else {
            // paramType is what we expect e.g. based on parent definition
            // but actual type (given by xsi:type/@typeDef) may be different, e.g. more specific
            if (xsubnode.getTypeQName() != null) {
                Class explicitParamType = getSchemaRegistry().determineCompileTimeClass(xsubnode.getTypeQName());
                if (explicitParamType != null) {
                    paramType = explicitParamType;
                } else {
                    // TODO or throw an exception?
                    LOGGER.warn("Unknown type name: " + xsubnode.getTypeQName() + ", ignoring it.");
                }
            }
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

	public <T> T unmarshallPrimitive(PrimitiveXNode<?> xprim, QName typeQName) throws SchemaException {
		Class<T> classType = getSchemaRegistry().determineCompileTimeClass(typeQName);
		return unmarshallPrimitive(xprim, classType);
	}
	
	private <T> T unmarshallPrimitive(PrimitiveXNode<?> xprim, Class<T> classType) throws SchemaException {
        if (XmlAsStringType.class.equals(classType)) {
            return (T) new XmlAsStringType((String) xprim.getParsedValue(DOMUtil.XSD_STRING));
        }
		if (XmlTypeConverter.canConvert(classType)) {
			// Trivial case, direct conversion
			QName xsdType = XsdTypeMapper.toXsdType(classType);
			T primValue = postConvertUnmarshall(xprim.getParsedValue(xsdType));
			return primValue;
		}
		
		if (RawType.class.isAssignableFrom(classType)) {
			RawType rawType = new RawType(xprim, prismContext);
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
		
		String javaEnumString = inspector.findEnumFieldName(classType, primValue);
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
        if (bean instanceof SchemaDefinitionType) {
            return marshalSchemaDefinition((SchemaDefinitionType) bean);
        } else if (bean instanceof ProtectedDataType<?>) {
            MapXNode xProtected = marshalProtectedDataType((ProtectedDataType<?>) bean);
            return xProtected;
        } else if (bean instanceof ItemPathType){
            return marshalItemPathType((ItemPathType) bean);
        } else if (bean instanceof RawType) {
            return marshalRawValue((RawType) bean);
        } else if (bean instanceof XmlAsStringType) {
            return marshalXmlAsStringType((XmlAsStringType) bean);
        } else if (prismContext != null && prismContext.getSchemaRegistry().determineDefinitionFromClass(bean.getClass()) != null){
        	return prismContext.getXnodeProcessor().serializeObject(((Objectable)bean).asPrismObject()).getSubnode();
        }

        // Note: SearchFilterType is treated below

        Class<? extends Object> beanClass = bean.getClass();

        //check for enums
        if (beanClass.isEnum()){
            String enumValue = XNodeProcessorUtil.findEnumFieldValue(beanClass, bean);
            if (StringUtils.isEmpty(enumValue)){
                enumValue = bean.toString();
            }
            QName fieldTypeName = inspector.findFieldTypeName(null, beanClass, DEFAULT_PLACEHOLDER);
            return createPrimitiveXNode(enumValue, fieldTypeName, false);
        }

        MapXNode xmap;
        if (bean instanceof SearchFilterType) {
            // this hack is here because of c:ConditionalSearchFilterType - it is analogous to situation when unmarshalling this type (TODO: rework this in a nicer way)
            xmap = marshalSearchFilterType((SearchFilterType) bean);
            if (SearchFilterType.class.equals(bean.getClass())) {
                return xmap;        // nothing more to serialize; otherwise we continue, because in that case we deal with a subclass of SearchFilterType
            }
        } else {
            xmap = new MapXNode();
        }

		XmlType xmlType = beanClass.getAnnotation(XmlType.class);
		if (xmlType == null) {
			throw new IllegalArgumentException("Cannot marshall "+beanClass+" it does not have @XmlType annotation");
		}
		
		String namespace = inspector.determineNamespace(beanClass);
		if (namespace == null) {
			throw new IllegalArgumentException("Cannot determine namespace of "+beanClass);
		}
		
		List<String> propOrder = inspector.getPropOrder(beanClass);
		for (String fieldName: propOrder) {
			QName elementName = inspector.findFieldElementQName(fieldName, beanClass, namespace);
			Method getter = inspector.findPropertyGetter(beanClass, fieldName);
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
			
			Field field = inspector.findPropertyField(beanClass, fieldName);
			boolean isAttribute = inspector.isAttribute(field, getter);
			
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
				
				QName fieldTypeName = inspector.findFieldTypeName(field, getterResultValue.getClass(), namespace);
								
				ListXNode xlist = new ListXNode();
				for (Object element: col) {
					Object elementToMarshall = element;
					if (element instanceof JAXBElement){
						if (((JAXBElement) element).getName() != null){
							elementName = ((JAXBElement) element).getName(); 
						}
						elementToMarshall = ((JAXBElement) element).getValue();
					} 
					XNode marshalled = marshallValue(elementToMarshall, fieldTypeName, isAttribute);

                    // Brutal hack - made here just to make scripts (bulk actions) functional while not breaking anything else
                    // Fix it in 3.1. [med]
                    if (fieldTypeName == null && element instanceof JAXBElement && marshalled != null) {
                        QName typeName = inspector.determineTypeForClass(elementToMarshall.getClass());
                        if (typeName != null) {
                            marshalled.setExplicitTypeDeclaration(true);
                            marshalled.setTypeQName(typeName);
                        }
                    }
                    else {
                    // end of hack

                        setExplicitTypeDeclarationIfNeeded(getter, getterResultValue, marshalled, fieldTypeName);
                    }
					xlist.add(marshalled);
				}
                xmap.put(elementName, xlist);
			} else {
				QName fieldTypeName = inspector.findFieldTypeName(field, getterResult.getClass(), namespace);
				Object valueToMarshall = null;
				if (getterResult instanceof JAXBElement){
					valueToMarshall = ((JAXBElement) getterResult).getValue();
					elementName = ((JAXBElement) getterResult).getName();
				} else{
					valueToMarshall = getterResult;
				}
				XNode marshelled = marshallValue(valueToMarshall, fieldTypeName, isAttribute);
				if (!getter.getReturnType().equals(valueToMarshall.getClass()) && getter.getReturnType().isAssignableFrom(valueToMarshall.getClass())){
					if (prismContext != null) {
                        PrismObjectDefinition def = prismContext.getSchemaRegistry().determineDefinitionFromClass(valueToMarshall.getClass());
                        if (def != null){
                            QName type = def.getTypeName();
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

    private XNode marshalXmlAsStringType(XmlAsStringType bean) {
        PrimitiveXNode xprim = new PrimitiveXNode<>();
        xprim.setValue(bean.getContentAsString());
        xprim.setTypeQName(DOMUtil.XSD_STRING);
        return xprim;
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
	
	private <T> XNode marshallValue(T value, QName fieldTypeName, boolean isAttribute) throws SchemaException {
		if (value == null) {
			return null;
		}
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

    private <T> PrimitiveXNode<T> createPrimitiveXNode(T val, QName type) {
        return createPrimitiveXNode(val, type, false);
    }

    private XNode marshalRawValue(RawType value) throws SchemaException {
        return value.serializeToXNode();
	}

    private XNode marshalItemPathType(ItemPathType itemPath) {
        PrimitiveXNode<ItemPath> xprim = new PrimitiveXNode<ItemPath>();
        if (itemPath != null){
            ItemPath path = itemPath.getItemPath();
            xprim.setValue(path);
            xprim.setTypeQName(ItemPathType.COMPLEX_TYPE);
        }
        return xprim;
    }

    private XNode marshalSchemaDefinition(SchemaDefinitionType schemaDefinitionType) {
        SchemaXNode xschema = new SchemaXNode();
        xschema.setSchemaElement(schemaDefinitionType.getSchema());
        MapXNode xmap = new MapXNode();
        xmap.put(DOMUtil.XSD_SCHEMA_ELEMENT, xschema);
        return xmap;
    }

    // TODO create more appropriate interface to be able to simply serialize ProtectedStringType instances
    public <T> MapXNode marshalProtectedDataType(ProtectedDataType<T> protectedType) throws SchemaException {
        MapXNode xmap = new MapXNode();
        if (protectedType.getEncryptedDataType() != null) {
            EncryptedDataType encryptedDataType = protectedType.getEncryptedDataType();
            MapXNode xEncryptedDataType = (MapXNode) marshall(encryptedDataType);
            xmap.put(ProtectedDataType.F_ENCRYPTED_DATA, xEncryptedDataType);
        } else if (protectedType.getClearValue() != null){
            QName type = XsdTypeMapper.toXsdType(protectedType.getClearValue().getClass());
            PrimitiveXNode xClearValue = createPrimitiveXNode(protectedType.getClearValue(), type);
            xmap.put(ProtectedDataType.F_CLEAR_VALUE, xClearValue);
        }
        // TODO: clearValue
        return xmap;
    }

    private PolyString unmarshalPolyString(MapXNode xmap) throws SchemaException {
        String orig = xmap.getParsedPrimitiveValue(QNameUtil.nullNamespace(PolyString.F_ORIG), DOMUtil.XSD_STRING);
        if (orig == null) {
            throw new SchemaException("Null polystring orig in "+xmap);
        }
        String norm = xmap.getParsedPrimitiveValue(QNameUtil.nullNamespace(PolyString.F_NORM), DOMUtil.XSD_STRING);
        return new PolyString(orig, norm);
    }

    private SchemaDefinitionType unmarshalSchemaDefinitionType(MapXNode xmap) throws SchemaException {
        Entry<QName, XNode> subEntry = xmap.getSingleSubEntry("schema element");
        if (subEntry == null) {
            return null;
        }
        XNode xsub = subEntry.getValue();
        if (xsub == null) {
            return null;
        }
        if (!(xsub instanceof SchemaXNode)) {
            throw new SchemaException("Cannot parse schema from "+xsub);
        }
//		Element schemaElement = ((SchemaXNode)xsub).getSchemaElement();
//		if (schemaElement == null) {
//			throw new SchemaException("Empty schema in "+xsub);
//		}
        SchemaDefinitionType schemaDefType = unmarshalSchemaDefinitionType((SchemaXNode) xsub);
//		new SchemaDefinitionType();
//		schemaDefType.setSchema(schemaElement);
        return schemaDefType;
    }

    public SchemaDefinitionType unmarshalSchemaDefinitionType(SchemaXNode xsub) throws SchemaException{
        Element schemaElement = ((SchemaXNode)xsub).getSchemaElement();
        if (schemaElement == null) {
            throw new SchemaException("Empty schema in "+xsub);
        }
        SchemaDefinitionType schemaDefType = new SchemaDefinitionType();
        schemaDefType.setSchema(schemaElement);
        return schemaDefType;
    }

}
 