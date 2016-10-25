/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.prism.marshaller;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Analogous to PrismUnmarshaller, this class unmarshals atomic values from XNode tree structures.
 * Atomic values are values that can be used as property values (i.e. either simple types, or
 * beans that are not containerables).
 */
public class BeanUnmarshaller {

    private static final Trace LOGGER = TraceManager.getTrace(BeanUnmarshaller.class);

    @NotNull private final PrismBeanInspector inspector;
	@NotNull private final PrismContext prismContext;

	public BeanUnmarshaller(@NotNull PrismContext prismContext, @NotNull PrismBeanInspector inspector) {
		this.prismContext = prismContext;
		this.inspector = inspector;
	}

	@NotNull
	public PrismContext getPrismContext() {
		return prismContext;
	}

	@NotNull
	private SchemaRegistry getSchemaRegistry() {
		return prismContext.getSchemaRegistry();
	}


	<T> T unmarshal(@NotNull XNode xnode, @NotNull QName typeQName, @NotNull ParsingContext pc) throws SchemaException {
		Class<T> classType = getSchemaRegistry().determineCompileTimeClass(typeQName);		// TODO use correct method!
		return unmarshal(xnode, classType, pc);
	}

	<T> T unmarshal(@NotNull XNode xnode, @NotNull Class<T> beanClass, @NotNull ParsingContext pc) throws SchemaException {



		if (xnode instanceof PrimitiveXNode) {
			return unmarshalFromPrimitive((PrimitiveXNode<T>) xnode, beanClass, pc);
		} else if (xnode instanceof MapXNode) {
			return unmarshalFromMap((MapXNode) xnode, beanClass, pc);
		} else if (xnode instanceof RootXNode) {
			return unmarshal(((RootXNode) xnode).getSubnode(), beanClass, pc);
		} else {
			throw new IllegalStateException("Unexpected xnode " + xnode + ". Could not unmarshal value");
		}
	}

	private <T> T unmarshalFromMap(@NotNull MapXNode xmap, @NotNull Class<T> beanClass, @NotNull ParsingContext pc) throws SchemaException {
		if (PolyStringType.class.equals(beanClass)) {
			return (T) PolyStringType.unmarshal(xmap);
		} else if (ProtectedStringType.class.equals(beanClass)) {
			return (T) ProtectedStringType.unmarshal(xmap);
			ProtectedStringType protectedType = new ProtectedStringType();
			XNodeProcessorUtil.parseProtectedType(protectedType, xmap, prismContext, pc);
			return (T) protectedType;
		} else if (ProtectedByteArrayType.class.equals(beanClass)) {
			ProtectedByteArrayType protectedType = new ProtectedByteArrayType();
			XNodeProcessorUtil.parseProtectedType(protectedType, xmap, prismContext, pc);
			return (T) protectedType;
		} else if (SchemaDefinitionType.class.equals(beanClass)) {
			SchemaDefinitionType schemaDefType = unmarshalSchemaDefinitionType(xmap);
			return (T) schemaDefType;
		} else if (prismContext.getSchemaRegistry().determineDefinitionFromClass(beanClass) != null) {
			PrismObjectDefinition def = prismContext.getSchemaRegistry().determineDefinitionFromClass(beanClass);
			return (T) ((PrismContextImpl) prismContext).getPrismUnmarshaller().parseObject(xmap, def, pc).asObjectable();
		} else if (XmlAsStringType.class.equals(beanClass)) {
			// reading a string represented a XML-style content
			// used e.g. when reading report templates (embedded XML)
			// A necessary condition: there may be only one map entry.
			if (xmap.size() > 1) {
				throw new SchemaException("Map with more than one item cannot be parsed as a string: " + xmap);
			} else if (xmap.isEmpty()) {
				return (T) new XmlAsStringType();
			} else {
				Entry<QName, XNode> entry = xmap.entrySet().iterator().next();
				DomLexicalProcessor domParser = ((PrismContextImpl) prismContext).getParserDom();
				String value = domParser.write(entry.getValue(), entry.getKey(), null);
				return (T) new XmlAsStringType(value);
			}
		} else if (SearchFilterType.class.isAssignableFrom(beanClass)) {
			T bean = (T) unmarshalSearchFilterType(xmap, (Class<? extends SearchFilterType>) beanClass, pc);
			// TODO fix this BRUTAL HACK - it is here because of c:ConditionalSearchFilterType
			return unmarshalFromMapToBean(bean, xmap, Collections.singleton("condition"), pc);
		} else {
			T bean;
			try {
				bean = beanClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new SystemException("Cannot instantiate bean of type " + beanClass + ": " + e.getMessage(), e);
			}
			return unmarshalFromMapToBean(bean, xmap, null, pc);
		}
	}

	private <T> T unmarshalFromMapToBean(@NotNull T bean, @NotNull MapXNode xmap, @Nullable Collection<String> keysToParse, @NotNull ParsingContext pc) throws SchemaException {
		Class<T> beanClass = (Class<T>) bean.getClass();

		for (Entry<QName,XNode> entry: xmap.entrySet()) {
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
				// We have to try to find a more generic field, such as xsd:any or substitution element
				// check for global element definition first
                Class objectFactoryClass = inspector.getObjectFactoryClass(beanClass.getPackage());
				objectFactory = instantiateObjectFactory(objectFactoryClass);
				elementMethod = inspector.findElementMethodInObjectFactory(objectFactoryClass, propName);
				if (elementMethod == null) {
					// Check for "any" method
					elementMethod = inspector.findAnyMethod(beanClass);
					if (elementMethod == null) {
						String m = "No field "+propName+" in class "+beanClass+" (and no element method in object factory too)";
						if (pc.isCompat()) {
							pc.warn(LOGGER, m);
							continue;
						} else {
							throw new SchemaException(m);
						}
					}
					unmarshallToAny(bean, elementMethod, key, xsubnode, pc);
					continue;
					
				}
				field = inspector.lookupSubstitution(beanClass, elementMethod);
				if (field == null) {
					// Check for "any" field
					field = inspector.findAnyField(beanClass);
					if (field == null) {
						elementMethod = inspector.findAnyMethod(beanClass);
						if (elementMethod == null) {
							String m = "No field "+propName+" in class "+beanClass+" (and no element method in object factory too)";
							if (pc.isCompat()) {
								pc.warn(LOGGER, m);
								continue;
							} else {
								throw new SchemaException(m);
							}
						}
						unmarshallToAny(bean, elementMethod, key, xsubnode, pc);
						continue;
//						throw new SchemaException("No field "+propName+" in class "+beanClass+" (no suitable substitution and no 'any' field)");
					}
					unmarshallToAny(bean, field, key, xsubnode, pc);
					continue;
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
					String m = "Cannot find setter or getter for field " + fieldName + " in " + beanClass;
					if (pc.isCompat()) {
						pc.warn(LOGGER, m);
						continue;
					} else {
						throw new SchemaException(m);
					}
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
			
			//check for subclasses???
			if (!storeAsRawType && xsubnode != null && xsubnode.getTypeQName() != null) {
				Class explicitParamType = getSchemaRegistry().determineCompileTimeClass(xsubnode.getTypeQName());
				if (explicitParamType == null){
					explicitParamType = XsdTypeMapper.toJavaTypeIfKnown(xsubnode.getTypeQName());
				}
				
				if (explicitParamType != null){
					paramType = explicitParamType; 
				}
			}

			
			if (!(xsubnode instanceof ListXNode) && Object.class.equals(paramType) && !storeAsRawType) {
				throw new IllegalArgumentException("Object property (without @Raw) not supported in field "+fieldName+" in "+beanClass);
			}

			String paramNamespace = inspector.determineNamespace(paramType);
			
			
			boolean problem = false;
			Object propValue = null;
			Collection<Object> propValues = null;
			if (xsubnode instanceof ListXNode) {
				ListXNode xlist = (ListXNode)xsubnode;
				if (setter != null) {
					try {
						propValue = convertSinglePropValue(xsubnode, fieldName, paramType, storeAsRawType, beanClass, paramNamespace, pc);
					} catch (SchemaException e) {
						problem = processSchemaException(e, xsubnode, pc);
					}
				} else {
					// No setter, we have to use collection getter
					propValues = new ArrayList<>(xlist.size());
					for (XNode xsubsubnode: xlist) {
						try {
							propValues.add(convertSinglePropValue(xsubsubnode, fieldName, paramType, storeAsRawType, beanClass, paramNamespace, pc));
						} catch (SchemaException e) {
							problem = processSchemaException(e, xsubsubnode, pc);
						}
					}
				}
			} else {
				try {
					propValue = convertSinglePropValue(xsubnode, fieldName, paramType, storeAsRawType, beanClass, paramNamespace, pc);
				} catch (SchemaException e) {
					problem = processSchemaException(e, xsubnode, pc);
				}
			}
			
			if (setter != null) {
				Object value = null;
				try {
					value = prepareValueToBeStored(propValue, wrapInJaxbElement, objectFactory, elementMethod, propName, beanClass, pc);
					setter.invoke(bean, value);
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
					col.add(prepareValueToBeStored(propValue, wrapInJaxbElement, objectFactory, elementMethod, propName, beanClass, pc));
				} else if (propValues != null) {
					for (Object propVal: propValues) {
						col.add(prepareValueToBeStored(propVal, wrapInJaxbElement, objectFactory, elementMethod, propName, beanClass, pc));
					}
				} else if (!problem) {
					throw new IllegalStateException("Strange. Multival property "+propName+" in "+beanClass+" produced null values list, parsed from "+xmap);
				}
				checkJaxbElementConsistence(col, pc);
			} else {
				throw new IllegalStateException("Uh? No setter nor getter.");
			}
		}
		
		if (prismContext != null && bean instanceof Revivable) {
			((Revivable)bean).revive(prismContext);
		}
		
		return bean;
	}

	// Prepares value to be stored into the bean - e.g. converts PolyString->PolyStringType, wraps a value to JAXB if specified, ...
	private <T> Object prepareValueToBeStored(Object propVal, boolean wrapInJaxbElement, Object objectFactory, Method factoryMehtod, String propName,
			Class beanClass, ParsingContext pc) {

		if (propVal instanceof PolyString) {
			propVal = new PolyStringType((PolyString) propVal);
		}

		if (wrapInJaxbElement) {
			if (factoryMehtod == null) {
				throw new IllegalArgumentException("Param type is JAXB element but no factory method found for it, property "+propName+" in "+beanClass);
			}
			try {
				return factoryMehtod.invoke(objectFactory, propVal);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SystemException("Unable to invoke factory method "+factoryMehtod+" on "+objectFactory.getClass()+" for property "+propName+" in "+beanClass);
			}
		} else {
			return propVal;
		}
	}

	/*
	 *  We want to avoid this:
	 *    <expression>
     *      <script>
     *        <code>'up'</code>
     *      </script>
     *      <value>up</value>
     *    </expression>
     *
     *  Because it cannot be reasonably serialized in XNode (<value> gets changed to <script>).
	 */
	private void checkJaxbElementConsistence(Collection<Object> collection, ParsingContext pc) throws SchemaException {
		QName elementName = null;
		for (Object object : collection) {
			if (!(object instanceof JAXBElement)) {
				continue;
			}
			JAXBElement element = (JAXBElement) object;
			if (elementName == null) {
				elementName = element.getName();
			} else {
				if (!QNameUtil.match(elementName, element.getName())) {
					String m = "Mixing incompatible element names in one property: "
							+ elementName + " and " + element.getName();
					if (pc.isStrict()) {
						throw new SchemaException(m);
					} else {
						pc.warn(LOGGER, m);
					}
				}
			}
		}
	}

	protected boolean processSchemaException(SchemaException e, XNode xsubnode, ParsingContext pc) throws SchemaException {
		if (pc.isStrict()) {
            throw e;
        } else {
            LoggingUtils.logException(LOGGER, "Couldn't parse part of the document. It will be ignored. Document part:\n{}", e, xsubnode);
			pc.warn("Couldn't parse part of the document. It will be ignored. Document part:\n" + xsubnode);
            return true;
        }
	}

	private <T,S> void unmarshallToAny(T bean, Method getter, QName elementName, XNode xsubnode, ParsingContext pc) throws SchemaException{
		Class<T> beanClass = (Class<T>) bean.getClass();
		
		Class objectFactoryClass = inspector.getObjectFactoryClass(elementName.getNamespaceURI());
		Object objectFactory = instantiateObjectFactory(objectFactoryClass);
		Method elementFactoryMethod = inspector.findElementMethodInObjectFactory(objectFactoryClass, elementName.getLocalPart());
		Class<S> subBeanClass = (Class<S>) elementFactoryMethod.getParameterTypes()[0];

		if (xsubnode instanceof ListXNode){
			for (XNode xsubSubNode : ((ListXNode) xsubnode)){
				S subBean = unmarshal(xsubSubNode, subBeanClass, pc);
				unmarshallToAnyValue(bean, beanClass, subBean, objectFactoryClass, objectFactory, elementFactoryMethod, getter, pc);
			}
		} else{ 
			S subBean = unmarshal(xsubnode, subBeanClass, pc);
			unmarshallToAnyValue(bean, beanClass, subBean, objectFactoryClass, objectFactory, elementFactoryMethod, getter, pc);
		}
		
	}
	
	private <T, S> void unmarshallToAnyValue(T bean, Class beanClass, S subBean, Class objectFactoryClass, Object objectFactory,
			Method elementFactoryMethod, Method getter, ParsingContext pc) {
		
		
		JAXBElement<S> subBeanElement;
		try {
			subBeanElement = (JAXBElement<S>) elementFactoryMethod.invoke(objectFactory, subBean);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
			throw new IllegalArgumentException("Cannot invoke factory method "+elementFactoryMethod+" on "+objectFactoryClass+" with "+subBean+": "+e1, e1);
		}
		
		Collection<Object> col;
		Object getterReturn;
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
		col.add(subBeanElement != null ? subBeanElement.getValue() : subBeanElement);
	}
	
	private <T,S> void unmarshallToAny(T bean, Field anyField, QName elementName, XNode xsubnode, ParsingContext pc) throws SchemaException{
		Method getter = inspector.findPropertyGetter(bean.getClass(), anyField.getName());
		unmarshallToAny(bean, getter, elementName, xsubnode, pc);
	}
	
    private Object instantiateObjectFactory(Class objectFactoryClass) {
        try {
            return objectFactoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot instantiate object factory class "+objectFactoryClass.getName()+": "+e.getMessage(), e);
        }
    }

    // parses any subtype of SearchFilterType
	private <T extends SearchFilterType> T unmarshalSearchFilterType(MapXNode xmap, Class<T> beanClass, ParsingContext pc) throws SchemaException {
		if (xmap == null) {
			return null;
		}
        T filterType;
        try {
            filterType = beanClass.newInstance();
        } catch (InstantiationException|IllegalAccessException e) {
            throw new SystemException("Cannot instantiate " + beanClass + ": " + e.getMessage(), e);
        }
        filterType.parseFromXNode(xmap, prismContext);
		return filterType;
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

	private Object convertSinglePropValue(XNode xsubnode, String fieldName, Class paramType, boolean storeAsRawType,
			Class classType, String schemaNamespace, ParsingContext pc) throws SchemaException {
		Object propValue;
		if (xsubnode == null) {
			return null;
		} else if (paramType.equals(XNode.class)) {
			propValue = xsubnode;
		} else if (storeAsRawType || paramType.equals(RawType.class)) {
            RawType raw = new RawType(xsubnode, prismContext);
			// FIXME UGLY HACK: parse value if possible
			if (xsubnode.getTypeQName() != null) {
				PrismValue value = prismContext.parserFor(xsubnode.toRootXNode()).parseItemValue();
				if (value != null && !value.isRaw()) {
					raw = new RawType(value, prismContext);
				}
			}
			propValue = raw;
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
				propValue = unmarshalFromPrimitive(((PrimitiveXNode<?>)xsubnode), paramType, pc);
			} else if (xsubnode instanceof MapXNode) {
				propValue = unmarshalFromMap((MapXNode)xsubnode, paramType, pc);
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

	public <T> T unmarshalFromPrimitive(PrimitiveXNode<?> xprim, QName typeQName, ParsingContext pc) throws SchemaException {
		Class<T> classType = getSchemaRegistry().determineCompileTimeClass(typeQName);
		return unmarshalFromPrimitive(xprim, classType, pc);
	}
	
	private <T> T unmarshalFromPrimitive(PrimitiveXNode<?> xprim, Class<T> classType, ParsingContext pc) throws SchemaException {
        if (XmlAsStringType.class.equals(classType)) {
            return (T) new XmlAsStringType((String) xprim.getParsedValue(DOMUtil.XSD_STRING));
        }
		if (XmlTypeConverter.canConvert(classType)) {
			// Trivial case, direct conversion
			QName xsdType = XsdTypeMapper.toXsdType(classType);
			T primValue = postConvertUnmarshall(xprim.getParsedValue(xsdType), pc);
			return primValue;
		}
		
		if (RawType.class.isAssignableFrom(classType)) {
			RawType rawType = new RawType(xprim, prismContext);
			return (T) rawType;
		}

        if (PolyStringType.class.isAssignableFrom(classType)) {
			// TODO fixme this hack
            Object value = xprim.getParsedValue(DOMUtil.XSD_STRING);
			PolyString polyString;
			if (value instanceof String) {
				polyString = new PolyString((String) value);
			} else if (value instanceof PolyStringType) {
				polyString = ((PolyStringType) value).toPolyString();
			} else if (value instanceof PolyString) {
				polyString = (PolyString) value;		// TODO clone?
			} else if (value == null) {
				polyString = null;
			} else {
				throw new IllegalStateException("Couldn't convert " + value + " to a PolyString; while parsing " + xprim.debugDump());
			}
            if (polyString != null) {
				// TODO should we always use default normalizer?
				polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
			}
            return (T) new PolyStringType(polyString);
        }
		
		if (ItemPathType.class.isAssignableFrom(classType)){
			Object parsedValue = xprim.getParsedValue(ItemPathType.COMPLEX_TYPE);
			T primValue = postConvertUnmarshall(parsedValue, pc);
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
			throw new SchemaException("Cannot convert primitive value to non-enum bean of type " + classType);
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

	private <T> T postConvertUnmarshall(Object parsedPrimValue, ParsingContext pc) {
		if (parsedPrimValue == null) {
			return null;
		}
		if (parsedPrimValue instanceof ItemPath) {
			return (T) new ItemPathType((ItemPath)parsedPrimValue);
		} else {
			return (T) parsedPrimValue;
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

    private PolyStringType unmarshalPolyStringType(MapXNode xmap) throws SchemaException {
        String orig = xmap.getParsedPrimitiveValue(QNameUtil.nullNamespace(PolyString.F_ORIG), DOMUtil.XSD_STRING);
        if (orig == null) {
            throw new SchemaException("Null polystring orig in "+xmap);
        }
        String norm = xmap.getParsedPrimitiveValue(QNameUtil.nullNamespace(PolyString.F_NORM), DOMUtil.XSD_STRING);
        return new PolyStringType(new PolyString(orig, norm));
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

	public boolean canProcess(QName typeName) {
		return ((PrismContextImpl) getPrismContext()).getBeanMarshaller().canProcess(typeName);
	}
	public boolean canProcess(Class<?> clazz) {
		return ((PrismContextImpl) getPrismContext()).getBeanMarshaller().canProcess(clazz);
	}
	public QName determineTypeForClass(Class<?> clazz) {
		return inspector.determineTypeForClass(clazz);
	}

}
 