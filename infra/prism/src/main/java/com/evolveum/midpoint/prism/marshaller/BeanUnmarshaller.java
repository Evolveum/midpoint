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
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.UnsupportedEncodingException;
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
	@NotNull private final Map<Class,PrimitiveUnmarshaller> specialPrimitiveUnmarshallers = new HashMap<>();
	@NotNull private final Map<Class,MapUnmarshaller> specialMapUnmarshallers = new HashMap<>();

	public BeanUnmarshaller(@NotNull PrismContext prismContext, @NotNull PrismBeanInspector inspector) {
		this.prismContext = prismContext;
		this.inspector = inspector;
		createSpecialUnmarshallerMaps();
	}

	@FunctionalInterface
	private interface PrimitiveUnmarshaller<T> {
		T unmarshal(PrimitiveXNode node, Class<T> beanClass, ParsingContext pc) throws SchemaException;
	}

	@FunctionalInterface
	private interface MapUnmarshaller<T> {
		T unmarshal(MapXNode node, Class<T> beanClass, ParsingContext pc) throws SchemaException;
	}

	private void add(Class<?> beanClass, PrimitiveUnmarshaller primitive, MapUnmarshaller map) {
		specialPrimitiveUnmarshallers.put(beanClass, primitive);
		specialMapUnmarshallers.put(beanClass, map);
	}

	private void createSpecialUnmarshallerMaps() {
		add(XmlAsStringType.class, this::unmarshalXmlAsStringFromPrimitive, this::unmarshalXmlAsStringFromMap);
		add(RawType.class, this::unmarshalRawType, this::unmarshalRawType);
		add(PolyString.class, this::unmarshalPolyStringFromPrimitive, this::unmarshalPolyStringFromMap);
		add(PolyStringType.class, this::unmarshalPolyStringFromPrimitive, this::unmarshalPolyStringFromMap);
		add(ItemPathType.class, this::unmarshalItemPath, this::notSupported);
		add(ProtectedStringType.class, this::unmarshalProtectedString, this::unmarshalProtectedString);
		add(ProtectedByteArrayType.class, this::unmarshalProtectedByteArray, this::unmarshalProtectedByteArray);
		add(SchemaDefinitionType.class, this::notSupported, this::unmarshalSchemaDefinitionType);
	}

	//region Main entry ==========================================================================
	/*
	 *  Preconditions:
	 *   1. typeName is processable by unmarshaller - i.e. it corresponds to simple or complex type NOT of containerable character
	 */

	@NotNull
	<T> T unmarshal(@NotNull XNode xnode, @NotNull QName typeQName, @NotNull ParsingContext pc) throws SchemaException {
		Class<T> classType = getSchemaRegistry().determineClassForType(typeQName);		// TODO use correct method!
		if (classType == null) {
			TypeDefinition td = getSchemaRegistry().findTypeDefinitionByType(typeQName);
			if (td instanceof SimpleTypeDefinition) {
				// most probably dynamically defined enum (TODO clarify)
				classType = (Class<T>) String.class;
			} else {
				throw new IllegalArgumentException("Couldn't unmarshal " + typeQName + ". Type definition = " + td);
			}
		}
		return unmarshal(xnode, classType, pc);
	}

	@NotNull
	<T> T unmarshal(@NotNull XNode xnode, @NotNull Class<T> beanClass, @NotNull ParsingContext pc) throws SchemaException {
		T value = unmarshalInternal(xnode, beanClass, pc);
		if (PrismContextImpl.isExtraValidation() && value != null) {
			Class<?> requested = ClassUtils.primitiveToWrapper(beanClass);
			Class<?> actual = ClassUtils.primitiveToWrapper(value.getClass());
			if (!requested.isAssignableFrom(actual)) {
				throw new AssertionError("Postcondition fail: unmarshal returned a value of " + value + " ("
						+ actual + ") which is not of requested type (" + requested + ")");
			}
		}
		return value;
	}

	private <T> T unmarshalInternal(@NotNull XNode xnode, @NotNull Class<T> beanClass, @NotNull ParsingContext pc) throws SchemaException {
		if (xnode instanceof RootXNode) {
			XNode subnode = ((RootXNode) xnode).getSubnode();
			if (subnode == null) {
				throw new IllegalStateException("Couldn't parse " + beanClass + " from a root node with a null content: " + xnode.debugDump());
			} else {
				return unmarshal(subnode, beanClass, pc);
			}
		} else if (!(xnode instanceof MapXNode) && !(xnode instanceof PrimitiveXNode)) {
			throw new IllegalStateException("Couldn't parse " + beanClass + " from non-map/non-primitive node: " + xnode.debugDump());
		}

		// only maps and primitives after this point

		if (xnode instanceof PrimitiveXNode) {
			PrimitiveXNode<T> prim = (PrimitiveXNode) xnode;
			if (XmlTypeConverter.canConvert(beanClass)) {
				QName xsdType = XsdTypeMapper.toXsdType(beanClass);
				Object parsedValue = prim.getParsedValue(xsdType, beanClass);
				return postConvertUnmarshal(parsedValue, pc);
			} else if (beanClass.isEnum()) {
				return unmarshalEnumFromPrimitive(prim, beanClass, pc);
			}
			@SuppressWarnings("unchecked")
			PrimitiveUnmarshaller<T> unmarshaller = specialPrimitiveUnmarshallers.get(beanClass);
			if (unmarshaller != null) {
				return unmarshaller.unmarshal(prim, beanClass, pc);
			} else if (prim.isEmpty()) {
				// Special case. Just return empty object
				try {
					return beanClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new SystemException("Cannot instantiate "+beanClass+": "+e.getMessage(), e);
				}
			} else {
				throw new SchemaException("Cannot convert primitive value to bean of type " + beanClass);
			}
		} else {
			@SuppressWarnings("unchecked")
			MapUnmarshaller<T> unmarshaller = specialMapUnmarshallers.get(beanClass);
			if (unmarshaller != null) {
				return unmarshaller.unmarshal((MapXNode) xnode, beanClass, pc);
			}
			return unmarshalFromMap((MapXNode) xnode, beanClass, pc);
		}
	}

	public boolean canProcess(QName typeName) {
		return ((PrismContextImpl) getPrismContext()).getBeanMarshaller().canProcess(typeName);
	}

	public boolean canProcess(Class<?> clazz) {
		return ((PrismContextImpl) getPrismContext()).getBeanMarshaller().canProcess(clazz);
	}
	//endregion


	private <T> T unmarshalFromMap(@NotNull MapXNode xmap, @NotNull Class<T> beanClass, @NotNull ParsingContext pc) throws SchemaException {

		if (Containerable.class.isAssignableFrom(beanClass)) {
			// This could have come from inside; note we MUST NOT parse this as PrismValue, because for objects we would lose oid/version
			@SuppressWarnings("unchecked")
			T value = (T) prismContext.parserFor(xmap.toRootXNode()).type(beanClass).parseRealValue();
			return value;
			//throw new IllegalArgumentException("Couldn't process Containerable: " + beanClass + " from " + xmap.debugDump());
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
		@SuppressWarnings("unchecked")
		Class<T> beanClass = (Class<T>) bean.getClass();
		for (Entry<QName, XNode> entry : xmap.entrySet()) {
			QName key = entry.getKey();
			if (keysToParse != null && !keysToParse.contains(key.getLocalPart())) {
				continue;
			}
			unmarshalMapEntry(bean, beanClass, entry.getKey(), entry.getValue(), xmap, pc);
		}
		return bean;
	}

	private <T> void unmarshalMapEntry(@NotNull T bean, @NotNull Class<T> beanClass,
			@NotNull QName key, @NotNull XNode node, @NotNull MapXNode containingMap, @NotNull ParsingContext pc) throws SchemaException {

		final String propName = key.getLocalPart();

		// this code is just to keep this method reasonably short
		PropertyAccessMechanism mechanism = new PropertyAccessMechanism();
		if (!mechanism.compute(bean, beanClass, propName, key, node, pc)) {
			return;
		}

		final String actualPropertyName = mechanism.actualPropertyName;
		final boolean storeAsRawType = mechanism.storeAsRawType;

		final Method getter = mechanism.getter;
		final Method setter = mechanism.setter;
		Class<?> paramType = mechanism.paramType;
		final boolean wrapInJaxbElement = mechanism.wrapInJaxbElement;

		if (Element.class.isAssignableFrom(paramType)) {
			throw new IllegalArgumentException("DOM not supported in field "+actualPropertyName+" in "+beanClass);
		}

		//check for subclasses???
		if (!storeAsRawType && node.getTypeQName() != null) {
			Class explicitParamType = getSchemaRegistry().determineClassForType(node.getTypeQName());
			if (explicitParamType != null) {
				paramType = explicitParamType;
			}
		}

		if (!(node instanceof ListXNode) && Object.class.equals(paramType) && !storeAsRawType) {
			throw new IllegalArgumentException("Object property (without @Raw) not supported in field "+actualPropertyName+" in "+beanClass);
		}

		String paramNamespace = inspector.determineNamespace(paramType);

		boolean problem = false;
		Object propValue = null;
		Collection<Object> propValues = null;
		if (node instanceof ListXNode) {
			ListXNode xlist = (ListXNode)node;
			if (setter != null) {
				try {
					propValue = convertSinglePropValue(node, actualPropertyName, paramType, storeAsRawType, beanClass, paramNamespace, pc);
				} catch (SchemaException e) {
					problem = processSchemaException(e, node, pc);
				}
			} else {
				// No setter, we have to use collection getter
				propValues = new ArrayList<>(xlist.size());
				for (XNode xsubsubnode: xlist) {
					try {
						propValues.add(convertSinglePropValue(xsubsubnode, actualPropertyName, paramType, storeAsRawType, beanClass, paramNamespace, pc));
					} catch (SchemaException e) {
						problem = processSchemaException(e, xsubsubnode, pc);
					}
				}
			}
		} else {
			try {
				propValue = convertSinglePropValue(node, actualPropertyName, paramType, storeAsRawType, beanClass, paramNamespace, pc);
			} catch (SchemaException e) {
				problem = processSchemaException(e, node, pc);
			}
		}

		if (setter != null) {
			Object value = null;
			try {
				value = prepareValueToBeStored(propValue, wrapInJaxbElement, mechanism.objectFactory, mechanism.elementFactoryMethod, propName, beanClass, pc);
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
				col.add(prepareValueToBeStored(propValue, wrapInJaxbElement, mechanism.objectFactory, mechanism.elementFactoryMethod, propName, beanClass, pc));
			} else if (propValues != null) {
				for (Object propVal: propValues) {
					col.add(prepareValueToBeStored(propVal, wrapInJaxbElement, mechanism.objectFactory, mechanism.elementFactoryMethod, propName, beanClass, pc));
				}
			} else if (!problem) {
				throw new IllegalStateException("Strange. Multival property "+propName+" in "+beanClass+" produced null values list, parsed from "+containingMap);
			}
			checkJaxbElementConsistence(col, pc);
		} else {
			throw new IllegalStateException("Uh? No setter nor getter.");
		}
	}

	private class PropertyAccessMechanism {

		Class<?> beanClass;

		// phase1
		String actualPropertyName;  	// This is the name of property we will really use. (Considering e.g. substitutions.)
		boolean storeAsRawType;			// Whether the data will be stored as RawType.
		Object objectFactory;			// JAXB object factory instance (e.g. xxxx.common-3.ObjectFactory).
		Method elementFactoryMethod;	// Method in object factory that creates a given JAXB element (e.g. createAsIs(value))

		// phase2
		Method getter, setter;			// Getter or setter that will be used to put a value (getter in case of collections)
		Class<?> paramType;				// Actual parameter type; unwrapped: Collection<X> -> X, JAXBElement<X> -> X
		boolean wrapInJaxbElement;		// If the paramType contained JAXBElement, i.e. if the value should be wrapped into it before using

		// returns true if the processing is to be continued;
		// false in case of using alternative way of unmarshalling (e.g. use of "any" method), or in case of error (in COMPAT mode)
		private <T> boolean compute(T bean, Class<T> beanClass, String propName, QName key, XNode node, ParsingContext pc)
				throws SchemaException {

			this.beanClass = beanClass;

			// phase1
			if (!computeActualPropertyName(bean, propName, key, node, pc)) {
				return false;
			}
			// phase2
			return computeGetterAndSetter(propName, pc);
		}

		// computes actualPropertyName + storeAsRawType
		// if necessary, fills-in also objectFactory + elementFactoryMethod
		private <T> boolean computeActualPropertyName(T bean, String propName, QName key, XNode node, ParsingContext pc)
				throws SchemaException {
			Field propertyField = inspector.findPropertyField(beanClass, propName);
			Method propertyGetter = null;
			if (propertyField == null) {
				propertyGetter = inspector.findPropertyGetter(beanClass, propName);
			}

			elementFactoryMethod = null;
			objectFactory = null;
			if (propertyField == null && propertyGetter == null) {
				// We have to try to find a more generic field, such as xsd:any or substitution element
				// check for global element definition first
				elementFactoryMethod = findElementFactoryMethod(propName);
				if (elementFactoryMethod != null) {
					// great - global element found, let's look up the field
					propertyField = inspector.lookupSubstitution(beanClass, elementFactoryMethod);
					if (propertyField == null) {
						propertyField = inspector.findAnyField(beanClass);        // Check for "any" field
						if (propertyField != null) {
							unmarshalToAnyUsingField(bean, propertyField, key, node, pc);
						} else {
							unmarshalToAnyUsingGetterIfExists(bean, key, node, pc, propName);
						}
						return false;
					}
				} else {
					unmarshalToAnyUsingGetterIfExists(bean, key, node, pc, propName);        // e.g. "getAny()"
					return false;
				}
			}

			// At this moment, property getter is the exact getter matching key.localPart (propName).
			// Property field may be either exact field matching key.localPart (propName), or more generic one (substitution, any).
			//noinspection ConstantConditions
			assert propertyGetter != null || propertyField != null;

			if (elementFactoryMethod != null) {
				storeAsRawType = elementFactoryMethod.getAnnotation(Raw.class) != null;
			} else if (propertyGetter != null) {
				storeAsRawType = propertyGetter.getAnnotation(Raw.class) != null;
			} else {
				storeAsRawType = propertyField.getAnnotation(Raw.class) != null;
			}

			if (propertyField != null) {
				actualPropertyName = propertyField.getName();
			} else {
				actualPropertyName = propName;
			}

			return true;
		}

		private Method findElementFactoryMethod(String propName) {
			Class objectFactoryClass = inspector.getObjectFactoryClass(beanClass.getPackage());
			objectFactory = instantiateObjectFactory(objectFactoryClass);
			return inspector.findElementMethodInObjectFactory(objectFactoryClass, propName);
		}

		private boolean computeGetterAndSetter(String propName, ParsingContext pc) throws SchemaException {
			setter = inspector.findSetter(beanClass, actualPropertyName);
			wrapInJaxbElement = false;
			paramType = null;
			if (setter == null) {
				// No setter. But if the property is multi-value we need to look
				// for a getter that returns a collection (Collection<Whatever>)
				getter = inspector.findPropertyGetter(beanClass, actualPropertyName);
				if (getter == null) {
					pc.warnOrThrow(LOGGER, "Cannot find setter or getter for field " + actualPropertyName + " in " + beanClass);
					return false;
				}
				computeParamTypeFromGetter(propName, getter.getReturnType());
			} else {
				getter = null;
				Class<?> setterType = setter.getParameterTypes()[0];
				computeParamTypeFromSetter(propName, setterType);
			}
			return true;
		}

		private void computeParamTypeFromSetter(String propName, Class<?> setterParamType) {
			if (JAXBElement.class.equals(setterParamType)) {
				//					TODO some handling for the returned generic parameter types
				Type[] genericTypes = setter.getGenericParameterTypes();
				if (genericTypes.length != 1) {
					throw new IllegalArgumentException("Too lazy to handle this.");
				}
				Type genericType = genericTypes[0];
				if (genericType instanceof ParameterizedType) {
					Type actualType = inspector.getTypeArgument(genericType, "add some description");
					if (actualType instanceof WildcardType) {
						if (elementFactoryMethod == null) {
							elementFactoryMethod = findElementFactoryMethod(propName);
						}
						// This is the case of Collection<JAXBElement<?>>
						// we need to extract the specific type from the factory method
						if (elementFactoryMethod == null) {
							throw new IllegalArgumentException(
									"Wildcard type in JAXBElement field specification and no factory method found for field "
											+ actualPropertyName + " in " + beanClass
											+ ", cannot determine collection type (inner type argument)");
						}
						Type factoryMethodGenericReturnType = elementFactoryMethod.getGenericReturnType();
						Type factoryMethodTypeArgument = inspector.getTypeArgument(factoryMethodGenericReturnType,
								"in factory method " + elementFactoryMethod + " return type for field " + actualPropertyName
										+ " in " + beanClass + ", cannot determine collection type");
						if (factoryMethodTypeArgument instanceof Class) {
							// This is the case of JAXBElement<Whatever>
							paramType = (Class<?>) factoryMethodTypeArgument;
							if (Object.class.equals(paramType) && !storeAsRawType) {
								throw new IllegalArgumentException("Factory method " + elementFactoryMethod
										+ " type argument is Object (without @Raw) for field " +
										actualPropertyName + " in " + beanClass + ", property " + propName);
							}
						} else {
							throw new IllegalArgumentException(
									"Cannot determine factory method return type, got " + factoryMethodTypeArgument
											+ " - for field " + actualPropertyName + " in " + beanClass
											+ ", cannot determine collection type (inner type argument)");
						}
					}
				}
				//					Class enclosing = paramType.getEnclosingClass();
				//					Class clazz = paramType.getClass();
				//					Class declaring = paramType.getDeclaringClass();
				wrapInJaxbElement = true;
			} else {
				paramType = setterParamType;
			}
		}

		private void computeParamTypeFromGetter(String propName, Class<?> getterReturnType) throws SchemaException {
			if (!Collection.class.isAssignableFrom(getterReturnType)) {
				throw new SchemaException("Cannot find getter for field " + actualPropertyName + " in " + beanClass
						+ " does not return collection, cannot use it to set value");
			}
			// getter.genericReturnType = Collection<...>
			Type typeArgument = inspector.getTypeArgument(getter.getGenericReturnType(),
					"for field " + actualPropertyName + " in " + beanClass + ", cannot determine collection type");
			if (typeArgument instanceof Class) {
				paramType = (Class<?>) typeArgument;	// ok, like Collection<AssignmentType>
			} else if (typeArgument instanceof ParameterizedType) {		// something more complex
				ParameterizedType paramTypeArgument = (ParameterizedType) typeArgument;
				Type rawTypeArgument = paramTypeArgument.getRawType();
				if (rawTypeArgument.equals(JAXBElement.class)) {
					// This is the case of Collection<JAXBElement<....>>
					wrapInJaxbElement = true;
					Type innerTypeArgument = inspector.getTypeArgument(typeArgument,
							"for field " + actualPropertyName + " in " + beanClass
									+ ", cannot determine collection type (inner type argument)");
					if (innerTypeArgument instanceof Class) {
						// This is the case of Collection<JAXBElement<Whatever>> (note that wrapInJaxbElement is now true)
						paramType = (Class<?>) innerTypeArgument;
					} else if (innerTypeArgument instanceof WildcardType) {
						// This is the case of Collection<JAXBElement<?>>
						// we need to extract the specific type from the factory method
						if (elementFactoryMethod == null) {
							elementFactoryMethod = findElementFactoryMethod(propName);
							if (elementFactoryMethod == null) {
								throw new IllegalArgumentException(
										"Wildcard type in JAXBElement field specification and no factory method found for field "
												+ actualPropertyName + " in " + beanClass
												+ ", cannot determine collection type (inner type argument)");
							}
						}
						// something like JAXBElement<AsIsExpressionEvaluatorType>
						Type factoryMethodGenericReturnType = elementFactoryMethod.getGenericReturnType();
						Type factoryMethodTypeArgument = inspector.getTypeArgument(factoryMethodGenericReturnType,
								"in factory method " + elementFactoryMethod + " return type for field " + actualPropertyName
										+ " in " + beanClass + ", cannot determine collection type");
						if (factoryMethodTypeArgument instanceof Class) {
							// This is the case of JAXBElement<Whatever>
							paramType = (Class<?>) factoryMethodTypeArgument;
							if (Object.class.equals(paramType) && !storeAsRawType) {
								throw new IllegalArgumentException("Factory method " + elementFactoryMethod
										+ " type argument is Object (and not @Raw) for field " +
										actualPropertyName + " in " + beanClass + ", property " + propName);
							}
						} else {
							throw new IllegalArgumentException(
									"Cannot determine factory method return type, got " + factoryMethodTypeArgument
											+ " - for field " + actualPropertyName + " in " + beanClass
											+ ", cannot determine collection type (inner type argument)");
						}
					} else {
						throw new IllegalArgumentException(
								"Ejha! " + innerTypeArgument + " " + innerTypeArgument.getClass() + " from "
										+ getterReturnType + " from " + actualPropertyName + " in " + propName + " "
										+ beanClass);
					}
				} else {
					// The case of Collection<Whatever<Something>>
					if (rawTypeArgument instanceof Class) {		// ??? rawTypeArgument is the 'Whatever' part
						paramType = (Class<?>) rawTypeArgument;
					} else {
						throw new IllegalArgumentException(
								"EH? Eh!? " + typeArgument + " " + typeArgument.getClass() + " from " + getterReturnType
										+ " from " + actualPropertyName + " in " + propName + " " + beanClass);
					}
				}
			} else {
				throw new IllegalArgumentException(
						"EH? " + typeArgument + " " + typeArgument.getClass() + " from " + getterReturnType + " from "
								+ actualPropertyName + " in " + propName + " " + beanClass);
			}
		}
	}

	private <T> void unmarshalToAnyUsingGetterIfExists(@NotNull T bean, @NotNull QName key, @NotNull XNode node,
			@NotNull ParsingContext pc, String propName) throws SchemaException {
		Method elementMethod = inspector.findAnyMethod(bean.getClass());
		if (elementMethod != null) {
			unmarshallToAnyUsingGetter(bean, elementMethod, key, node, pc);
		} else {
			pc.warnOrThrow(LOGGER, "No field "+propName+" in class "+bean.getClass()+" (and no element method in object factory too)");
		}
	}
//
//		if (prismContext != null && bean instanceof Revivable) {
//			((Revivable)bean).revive(prismContext);
//		}
//
//		return bean;
//	}
//
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

	private boolean processSchemaException(SchemaException e, XNode xsubnode, ParsingContext pc) throws SchemaException {
		if (pc.isStrict()) {
            throw e;
        } else {
            LoggingUtils.logException(LOGGER, "Couldn't parse part of the document. It will be ignored. Document part:\n{}", e, xsubnode);
			pc.warn("Couldn't parse part of the document. It will be ignored. Document part:\n" + xsubnode);
            return true;
        }
	}

	private <T,S> void unmarshallToAnyUsingGetter(T bean, Method getter, QName elementName, XNode xsubnode, ParsingContext pc) throws SchemaException{
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

	private <T,S> void unmarshalToAnyUsingField(T bean, Field anyField, QName elementName, XNode xsubnode, ParsingContext pc) throws SchemaException{
		Method getter = inspector.findPropertyGetter(bean.getClass(), anyField.getName());
		unmarshallToAnyUsingGetter(bean, getter, elementName, xsubnode, pc);
	}

    private Object instantiateObjectFactory(Class objectFactoryClass) {
        try {
            return objectFactoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot instantiate object factory class "+objectFactoryClass.getName()+": "+e.getMessage(), e);
        }
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
				PrismValue value = prismContext.parserFor(xsubnode.toRootXNode()).parseItemValue();	// TODO what about objects? oid/version will be lost here
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
			if (xsubnode instanceof PrimitiveXNode<?> || xsubnode instanceof MapXNode) {
				propValue = unmarshal(xsubnode, paramType, pc);
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

	private <T> T postConvertUnmarshal(Object parsedPrimValue, ParsingContext pc) {
		if (parsedPrimValue == null) {
			return null;
		}
		if (parsedPrimValue instanceof ItemPath) {
			return (T) new ItemPathType((ItemPath)parsedPrimValue);
		} else {
			return (T) parsedPrimValue;
		}
	}

    private SchemaDefinitionType unmarshalSchemaDefinitionType(MapXNode xmap, Class<?> beanClass, ParsingContext pc) throws SchemaException {
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
		return unmarshalSchemaDefinitionType((SchemaXNode) xsub);
    }

    SchemaDefinitionType unmarshalSchemaDefinitionType(SchemaXNode xsub) throws SchemaException{
        Element schemaElement = xsub.getSchemaElement();
        if (schemaElement == null) {
            throw new SchemaException("Empty schema in " + xsub);
        }
        SchemaDefinitionType schemaDefType = new SchemaDefinitionType();
        schemaDefType.setSchema(schemaElement);
        return schemaDefType;
    }

	@NotNull
	public PrismContext getPrismContext() {
		return prismContext;
	}

	@NotNull
	private SchemaRegistry getSchemaRegistry() {
		return prismContext.getSchemaRegistry();
	}


	//region Specific unmarshallers =========================================================

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

	private ItemPathType unmarshalItemPath(PrimitiveXNode<ItemPathType> primitiveXNode, Class beanClass, ParsingContext parsingContext)
			throws SchemaException {
		ItemPathType parsedValue = primitiveXNode.getParsedValue(ItemPathType.COMPLEX_TYPE, ItemPathType.class);
		return postConvertUnmarshal(parsedValue, parsingContext);
	}

	private Object unmarshalPolyStringFromPrimitive(PrimitiveXNode<?> node, Class<?> beanClass, ParsingContext parsingContext)
			throws SchemaException {
		Object value;
		if (node.isParsed()) {
			value = node.getValue();			// there can be e.g. PolyString there
		} else {
			value = ((PrimitiveXNode<String>) node).getParsedValue(DOMUtil.XSD_STRING, String.class);
		}
		return toCorrectPolyStringClass(value, beanClass, node);
	}

	private Object unmarshalPolyStringFromMap(MapXNode map, Class<?> beanClass, ParsingContext pc) throws SchemaException {
		String orig = map.getParsedPrimitiveValue(QNameUtil.nullNamespace(PolyString.F_ORIG), DOMUtil.XSD_STRING);
		if (orig == null) {
			throw new SchemaException("Null polystring orig in "+map);
		}
		String norm = map.getParsedPrimitiveValue(QNameUtil.nullNamespace(PolyString.F_NORM), DOMUtil.XSD_STRING);
		Object value = new PolyStringType(new PolyString(orig, norm));
		return toCorrectPolyStringClass(value, beanClass, map);
	}

	private Object toCorrectPolyStringClass(Object value, Class<?> beanClass, XNode node) {
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
			throw new IllegalStateException("Couldn't convert " + value + " to a PolyString; while parsing " + node.debugDump());
		}
		if (polyString != null && polyString.getNorm() == null) {
			// TODO should we always use default normalizer?
			polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
		}
		if (PolyString.class.equals(beanClass)) {
			return polyString;
		} else if (PolyStringType.class.equals(beanClass)) {
			return new PolyStringType(polyString);
		} else {
			throw new IllegalArgumentException("Wrong class for PolyString value: " + beanClass);
		}
	}

	private Object notSupported(XNode node, Class<?> beanClass, ParsingContext parsingContext) {
		// TODO what if compat mode?
		throw new IllegalArgumentException("The following couldn't be parsed as " + beanClass + ": " + node.debugDump());
	}

	private XmlAsStringType unmarshalXmlAsStringFromPrimitive(PrimitiveXNode node, Class<XmlAsStringType> beanClass, ParsingContext parsingContext) throws SchemaException {
		return new XmlAsStringType(((PrimitiveXNode<String>) node).getParsedValue(DOMUtil.XSD_STRING, String.class));
	}

	private XmlAsStringType unmarshalXmlAsStringFromMap(MapXNode map, Class<XmlAsStringType> beanClass, ParsingContext parsingContext) throws SchemaException {
		// reading a string represented a XML-style content
		// used e.g. when reading report templates (embedded XML)
		// A necessary condition: there may be only one map entry.
		if (map.size() > 1) {
			throw new SchemaException("Map with more than one item cannot be parsed as a string: " + map);
		} else if (map.isEmpty()) {
			return new XmlAsStringType();
		} else {
			Entry<QName, XNode> entry = map.entrySet().iterator().next();
			DomLexicalProcessor domParser = ((PrismContextImpl) prismContext).getParserDom();
			String value = domParser.write(entry.getValue(), entry.getKey(), null);
			return new XmlAsStringType(value);
		}
	}

	private RawType unmarshalRawType(XNode node, Class<RawType> beanClass, ParsingContext parsingContext) {
		// TODO We could probably try to parse the raw node content using information from explicit node type.
		return new RawType(node, prismContext);
	}

	private <T> T unmarshalEnumFromPrimitive(PrimitiveXNode prim, Class<T> beanClass, ParsingContext pc)
			throws SchemaException {

		String primValue = (String) prim.getParsedValue(DOMUtil.XSD_STRING, String.class);
		primValue = StringUtils.trim(primValue);
		if (StringUtils.isEmpty(primValue)) {
			return null;
		}

		String javaEnumString = inspector.findEnumFieldName(beanClass, primValue);
		if (javaEnumString == null) {
			for (Field field: beanClass.getDeclaredFields()) {
				if (field.getName().equals(primValue)) {
					javaEnumString = field.getName();
					break;
				}
			}
		}
		if (javaEnumString == null) {
			throw new SchemaException("Cannot find enum value for string '"+primValue+"' in "+beanClass);
		}

		@SuppressWarnings("unchecked")
		T bean = (T) Enum.valueOf((Class<Enum>)beanClass, javaEnumString);
		return bean;
	}

	private ProtectedStringType unmarshalProtectedString(MapXNode map, Class beanClass, ParsingContext pc) throws SchemaException {
		ProtectedStringType protectedType = new ProtectedStringType();
		XNodeProcessorUtil.parseProtectedType(protectedType, map, prismContext, pc);
		return protectedType;
	}

	private ProtectedStringType unmarshalProtectedString(PrimitiveXNode<String> prim, Class beanClass, ParsingContext pc) throws SchemaException {
		ProtectedStringType protectedType = new ProtectedStringType();
		protectedType.setClearValue(prim.getParsedValue(DOMUtil.XSD_STRING, String.class));
		return protectedType;
	}

	private ProtectedByteArrayType unmarshalProtectedByteArray(MapXNode map, Class beanClass, ParsingContext pc) throws SchemaException {
		ProtectedByteArrayType protectedType = new ProtectedByteArrayType();
		XNodeProcessorUtil.parseProtectedType(protectedType, map, prismContext, pc);
		return protectedType;
	}

	private ProtectedByteArrayType unmarshalProtectedByteArray(PrimitiveXNode<String> prim, Class beanClass, ParsingContext pc) throws SchemaException {
		ProtectedByteArrayType protectedType = new ProtectedByteArrayType();
		String stringValue = prim.getParsedValue(DOMUtil.XSD_STRING, String.class);
		if (stringValue == null) {
			return null;
		}
		try {
			protectedType.setClearValue(ArrayUtils.toObject(stringValue.getBytes("UTF-8")));
		} catch (UnsupportedEncodingException e) {
			throw new SystemException("UTF-8 encoding is not supported", e);
		}
		return protectedType;
	}
	//endregion
}
 