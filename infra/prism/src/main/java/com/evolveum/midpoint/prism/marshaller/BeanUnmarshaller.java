/*
 * Copyright (c) 2010-2017 Evolveum
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
import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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

	/**
	 * TODO: decide if this method should be marked @NotNull.
	 * Basically the problem is with primitives. When parsed, they sometimes return null. The question is if it's correct.
	 */
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
		if (beanClass == null) {
			throw new IllegalStateException("No bean class for node: " + xnode.debugDump());
		}
		if (xnode instanceof RootXNode) {
			XNode subnode = ((RootXNode) xnode).getSubnode();
			if (subnode == null) {
				throw new IllegalStateException("Couldn't parse " + beanClass + " from a root node with a null content: " + xnode.debugDump());
			} else {
				return unmarshal(subnode, beanClass, pc);
			}
		} else if (!(xnode instanceof MapXNode) && !(xnode instanceof PrimitiveXNode) && !xnode.isHeterogeneousList()) {
			throw new IllegalStateException("Couldn't parse " + beanClass + " from non-map/non-primitive/non-hetero-list node: " + xnode.debugDump());
		}

		// only maps and primitives and heterogeneous lists after this point

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
			} else {
				return unmarshallPrimitive(prim, beanClass, pc);
			}
		} else {

			if (beanClass.getPackage() == null || beanClass.getPackage().getName().equals("java.lang")) {
				// We obviously have primitive data type, but we are asked to unmarshall from map xnode
				// NOTE: this may happen in XML when we have "empty" element, but it has some whitespace in it
				//       such as those troublesome newlines. This also happens if there is "empty" element
				//       but it contains an expression (so it is not PrimitiveXNode but MapXNode).
				// TODO: more robust implementation
				// TODO: look for "value" subnode with primitive value and try that.
				// This is most likely attempt to parse primitive value with dynamic expression.
				// Therefore just ignore entire map content.
				return null;
			}

			@SuppressWarnings("unchecked")
			MapUnmarshaller<T> unmarshaller = specialMapUnmarshallers.get(beanClass);
			if (xnode instanceof MapXNode && unmarshaller != null) {		// TODO: what about special unmarshaller + hetero list?
				return unmarshaller.unmarshal((MapXNode) xnode, beanClass, pc);
			}
			return unmarshalFromMapOrHeteroList(xnode, beanClass, pc);
		}
	}

	/**
	 * For cases when XSD complex type has a simple content. In that case the resulting class has @XmlValue annotation.
	 */
	private <T> T unmarshallPrimitive(PrimitiveXNode<T> prim, Class<T> beanClass, ParsingContext pc) throws SchemaException {
		if (prim.isEmpty()) {
			return instantiate(beanClass);		// Special case. Just return empty object
		}

		Field valueField = XNodeProcessorUtil.findXmlValueField(beanClass);

		if (valueField == null) {
			throw new SchemaException("Cannot convert primitive value to bean of type " + beanClass);
		}

		T instance = instantiate(beanClass);

		if (!valueField.isAccessible()) {
			valueField.setAccessible(true);
		}

		T value;
		if (prim.isParsed()) {
			value = prim.getValue();
		} else {
			Class<?> fieldType = valueField.getType();
			QName xsdType = XsdTypeMapper.toXsdType(fieldType);
			value = prim.getParsedValue(xsdType, (Class<T>) fieldType);
		}

		try {
			valueField.set(instance, value);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new SchemaException("Cannot set primitive value to field " + valueField.getName() + " of bean " + beanClass + ": "+e.getMessage(), e);
		}

		return instance;
	}

	boolean canProcess(QName typeName) {
		return getBeanMarshaller().canProcess(typeName);
	}

	boolean canProcess(Class<?> clazz) {
		return getBeanMarshaller().canProcess(clazz);
	}

	@NotNull
	private BeanMarshaller getBeanMarshaller() {
		return ((PrismContextImpl) getPrismContext()).getBeanMarshaller();
	}
	//endregion

	@NotNull
	private <T> T unmarshalFromMapOrHeteroList(@NotNull XNode mapOrList, @NotNull Class<T> beanClass, @NotNull ParsingContext pc) throws SchemaException {

		if (Containerable.class.isAssignableFrom(beanClass)) {
			// This could have come from inside; note we MUST NOT parse this as PrismValue, because for objects we would lose oid/version
			return prismContext.parserFor(mapOrList.toRootXNode()).type(beanClass).parseRealValue();
		} else if (SearchFilterType.class.isAssignableFrom(beanClass)) {
			if (mapOrList instanceof MapXNode) {
				T bean = (T) unmarshalSearchFilterType((MapXNode) mapOrList, (Class<? extends SearchFilterType>) beanClass, pc);
				// TODO fix this BRUTAL HACK - it is here because of c:ConditionalSearchFilterType
				return unmarshalFromMapOrHeteroListToBean(bean, mapOrList, Collections.singleton("condition"), pc);
			} else {
				throw new SchemaException("SearchFilterType is not supported in combination of heterogeneous list.");
			}
		} else {
			T bean = instantiate(beanClass);
			return unmarshalFromMapOrHeteroListToBean(bean, mapOrList, null, pc);
		}
	}

	private <T> T instantiate(@NotNull Class<T> beanClass) {
		T bean;
		try {
			bean = beanClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new SystemException("Cannot instantiate bean of type " + beanClass + ": " + e.getMessage(), e);
		}
		return bean;
	}

	@NotNull
	private <T> T unmarshalFromMapOrHeteroListToBean(@NotNull T bean, @NotNull XNode mapOrList, @Nullable Collection<String> keysToParse, @NotNull ParsingContext pc) throws SchemaException {
		@SuppressWarnings("unchecked")
		Class<T> beanClass = (Class<T>) bean.getClass();
		if (mapOrList instanceof MapXNode) {
			MapXNode map = (MapXNode) mapOrList;
			for (Entry<QName, XNode> entry : map.entrySet()) {
				QName key = entry.getKey();
				if (keysToParse != null && !keysToParse.contains(key.getLocalPart())) {
					continue;
				}
				if (entry.getValue() == null) {
					continue;
				}
				unmarshalEntry(bean, beanClass, entry.getKey(), entry.getValue(), mapOrList, false, pc);
			}
		} else if (mapOrList.isHeterogeneousList()) {
			QName keyQName = getBeanMarshaller().getHeterogeneousListPropertyName(beanClass);
			unmarshalEntry(bean, beanClass, keyQName, mapOrList, mapOrList, true, pc);
		} else {
			throw new IllegalStateException("Not a map nor heterogeneous list: " + mapOrList.debugDump());
		}
		return bean;
	}

	/**
	 * Parses either a map entry, or a fictitious heterogeneous list property.
	 *
	 * It makes sure that a 'key' property is inserted into 'bean' object, being sourced from 'node' structure.
	 * Node itself can be single-valued or multi-valued, corresponding to single or multi-valued 'key' property.
	 * ---
	 * A notable (and quite ugly) exception is processing of fictitious heterogeneous lists.
	 * In this case we have a ListXNode that should be interpreted as a MapXNode, inserting fictitious property
	 * named after abstract multivalued property in the parent bean.
	 *
	 * For example, when we have (embedded in ExecuteScriptType):
	 *   {
	 *     pipeline: *[
	 *       { element: search, ... },
	 *       { element: sequence, ... }
	 *     ]
	 *   }
	 *
	 * ...it should be, in fact, read as if it would be:
	 *
	 *   {
	 *     pipeline: {
	 *        scriptingExpression: [
	 *          { type: SearchExpressionType, ... },
	 *          { type: ExpressionSequenceType, ... }
	 *        ]
	 *     }
	 *   }
	 *
	 * (The only difference is in element names, which are missing in the latter snippet, but let's ignore that here.)
	 *
	 * Fictitious heterogeneous list entry here is "scriptingExpression", a property of pipeline (ExpressionPipelineType).
	 *
 	 * We have to create the following data structure (corresponding to latter snippet):
	 *
	 * instance of ExecuteScriptType:
	 *   scriptingExpression = instance of JAXBElement(pipeline, ExpressionPipelineType):			[1]
	 *     scriptingExpression = List of															[2]
	 *       - JAXBElement(search, SearchExpressionType)
	 *       - JAXBElement(sequence, ExpressionSequenceType)
	 *
	 * We in fact invoke this method twice with the same node (a two-entry list, marked as '*' in the first snippet):
	 * 1) bean=ExecuteScriptType, key=pipeline, node=HList(*), isHeteroListProperty=false
	 * 2) bean=ExpressionPipelineType, key=scriptingExpression, node=HList(*), isHeteroListProperty=true		<<<
	 *
	 * During the first call we fill in scriptingExpression (single value) in ExecuteScriptType [1]; during the second one
	 * we fill in scriptingExpression (multivalued) in ExpressionPipelineType [2].
	 *
	 * Now let's expand the sample.
	 *
	 * This XNode tree:
	 *   {
	 *     pipeline: *[
	 *       { element: search, type: RoleType, searchFilter: {...}, action: log },
	 *       { element: sequence, value: **[
	 *           { element: action, type: delete },
	 *           { element: action, type: assign, parameter: {...} },
	 *           { element: search, type: UserType }
	 *       ] }
	 *     ]
	 *   }
	 *
	 * Should be interpreted as:
	 *   {
	 *     pipeline: {
	 *       scriptingExpression: [
	 *          { type: SearchExpressionType, type: RoleType, searchFilter: {...}, action: log }
	 *          { type: ExpressionSequenceType, scriptingExpression: [
	 *                { type: ActionExpressionType, type: delete },
	 *                { type: ActionExpressionType, type: assign, parameter: {...} },
	 *                { type: SearchExpressionType, type: UserType }
	 *            ] }
	 *       ]
	 *     }
	 *   }
	 *
	 * Producing the following data:
	 *
	 * instance of ExecuteScriptType:
	 *   scriptingExpression = instance of JAXBElement(pipeline, ExpressionPipelineType):			[1]
	 *     scriptingExpression = List of															[2]
	 *       - JAXBElement(search, instance of SearchExpressionType):
	 *           type: RoleType,
	 *           searchFilter: (...),
	 *           action: log,
	 *       - JAXBElement(sequence, instance of ExpressionSequenceType):
	 *           scriptingExpression = List of
	 *             - JAXBElement(action, instance of ActionExpressionType):
	 *                 type: delete
	 *             - JAXBElement(action, instance of ActionExpressionType):
	 *                 type: assign
	 *                 parameter: (...),
	 *             - JAXBElement(search, instance of SearchExpressionType):
	 *                 type: UserType
	 *
	 * Invocations of this method will be:
	 *  1) bean=ExecuteScriptType, key=pipeline, node=HList(*), isHeteroListProperty=false
	 *  2) bean=ExpressionPipelineType, key=scriptingExpression, node=HList(*), isHeteroListProperty=true            <<<
	 *  3) bean=SearchExpressionType, key=type, node='type: c:RoleType', isHeteroListProperty=false
	 *  4) bean=SearchExpressionType, key=searchFilter, node=XNode(map:1 entries), isHeteroListProperty=false
	 *  5) bean=SearchExpressionType, key=action, node=XNode(map:1 entries), isHeteroListProperty=false
	 *  6) bean=ActionExpressionType, key=type, node='type: log', isHeteroListProperty=false
	 *  7) bean=ExpressionSequenceType, key=scriptingExpression, node=HList(**), isHeteroListProperty=true           <<<
	 *  8) bean=ActionExpressionType, key=type, node='type: delete', isHeteroListProperty=false
	 *  9) bean=ActionExpressionType, key=type, node='type: assign', isHeteroListProperty=false
	 * 10) bean=ActionExpressionType, key=parameter, node=XNode(map:2 entries), isHeteroListProperty=false
	 * 11) bean=ActionParameterValueType, key=name, node='name: role', isHeteroListProperty=false
	 * 12) bean=ActionParameterValueType, key=value, node='value: rome555c-7797-11e2-94a6-001e8c717e5b', isHeteroListProperty=false
	 * 13) bean=SearchExpressionType, key=type, node='type: UserType', isHeteroListProperty=false
	 *
	 * Here we have 2 calls with isHeteroListProperty=true; first for pipeline.scriptingExpression, second for
	 * sequence.scriptingExpression.
	 */
	private <T> void unmarshalEntry(@NotNull T bean, @NotNull Class<T> beanClass,
			@NotNull QName key, @NotNull XNode node, @NotNull XNode containingNode,
			boolean isHeteroListProperty, @NotNull ParsingContext pc) throws SchemaException {

		//System.out.println("bean=" + bean.getClass().getSimpleName() + ", key=" + key.getLocalPart() + ", node=" + node + ", isHeteroListProperty=" + isHeteroListProperty);
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
		final boolean wrapInJaxbElement = mechanism.wrapInJaxbElement;

		if (Element.class.isAssignableFrom(mechanism.paramType)) {
			throw new IllegalArgumentException("DOM not supported in field "+actualPropertyName+" in "+beanClass);
		}

		// The type T that is expected by the bean, i.e. either by
		//   - setMethod(T value), or
		//   - Collection<T> getMethod()
		// We use it to retrieve the correct value when parsing the node.
		// We might specialize it using the information derived from the node (to deal with inclusive polymorphism,
		// i.e. storing ExclusionPolicyConstraintType where AbstractPolicyConstraintType is expected).
		@NotNull Class<?> paramType;

		if (!storeAsRawType && !isHeteroListProperty) {
			Class<?> t = specializeParamType(node,  mechanism.paramType, pc);
			if (t == null) {	// indicates a problem
				return;
			} else {
				paramType = t;
			}
		} else {
			paramType = mechanism.paramType;
		}

		if (!(node instanceof ListXNode) && Object.class.equals(paramType) && !storeAsRawType) {
			throw new IllegalArgumentException("Object property (without @Raw) not supported in field "+actualPropertyName+" in "+beanClass);
		}

//		String paramNamespace = inspector.determineNamespace(paramType);

		boolean problem = false;
		Object propValue = null;
		Collection<Object> propValues = null;
		// For heterogeneous lists we have to create multi-valued fictitious property first. So we have to treat node as a map
		// (instead of list) and process it as a single value. Only when
		if (node instanceof ListXNode && (!node.isHeterogeneousList() || isHeteroListProperty)) {
			ListXNode xlist = (ListXNode) node;
			if (setter != null) {
				try {
					Object value = unmarshalSinglePropValue(node, actualPropertyName, paramType, storeAsRawType, beanClass, pc);
					if (wrapInJaxbElement) {
						propValue = wrapInJaxbElement(value, mechanism.objectFactory,
								mechanism.elementFactoryMethod, propName, beanClass, pc);
					} else {
						propValue = value;
					}
				} catch (SchemaException e) {
					problem = processSchemaException(e, node, pc);
				}
			} else {
				// No setter, we have to use collection getter
				propValues = new ArrayList<>(xlist.size());
				for (XNode xsubsubnode: xlist) {
					try {
						Object valueToAdd;
						Object value = unmarshalSinglePropValue(xsubsubnode, actualPropertyName, paramType, storeAsRawType, beanClass, pc);
						if (value != null) {
							if (isHeteroListProperty) {
								QName elementName = xsubsubnode.getElementName();
								if (elementName == null) {
									// TODO better error handling
									throw new SchemaException("Heterogeneous list with a no-elementName node: " + xsubsubnode);
								}
								Class valueClass = value.getClass();
								QName jaxbElementName;
								if (QNameUtil.hasNamespace(elementName)) {
									jaxbElementName = elementName;
								} else {
									// Approximate solution: find element in schema registry - check for type compatibility
									// in order to exclude accidental name matches (like c:expression/s:expression).
									Optional<ItemDefinition> itemDefOpt = getSchemaRegistry().findItemDefinitionsByElementName(elementName)
											.stream()
											.filter(def ->
													getSchemaRegistry().findTypeDefinitionsByType(def.getTypeName()).stream()
															.anyMatch(typeDef -> typeDef.getCompileTimeClass() != null
																	&& typeDef.getCompileTimeClass()
																	.isAssignableFrom(valueClass)))
											.findFirst();
									if (itemDefOpt.isPresent()) {
										jaxbElementName = itemDefOpt.get().getName();
									} else {
										LOGGER.warn("Heterogeneous list member with unknown element name '" + elementName + "': "  + value);
										jaxbElementName = elementName;        // unqualified
									}
								}
								@SuppressWarnings("unchecked")
								JAXBElement jaxbElement = new JAXBElement<>(jaxbElementName, valueClass, value);
								valueToAdd = jaxbElement;
							} else {
								if (wrapInJaxbElement) {
									valueToAdd = wrapInJaxbElement(value, mechanism.objectFactory,
											mechanism.elementFactoryMethod, propName, beanClass, pc);
								} else {
									valueToAdd = value;
								}
							}
							propValues.add(valueToAdd);
						}
					} catch (SchemaException e) {
						problem = processSchemaException(e, xsubsubnode, pc);
					}
				}
			}
		} else {
			try {
				propValue = unmarshalSinglePropValue(node, actualPropertyName, paramType, storeAsRawType, beanClass, pc);
				if (wrapInJaxbElement) {
					propValue = wrapInJaxbElement(propValue, mechanism.objectFactory,
							mechanism.elementFactoryMethod, propName, beanClass, pc);
				}
			} catch (SchemaException e) {
				problem = processSchemaException(e, node, pc);
			}
		}

		if (setter != null) {
			try {
				setter.invoke(bean, propValue);
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
				col.add(propValue);
			} else if (propValues != null) {
				for (Object propVal: propValues) {
					col.add(propVal);
				}
			} else if (!problem) {
				throw new IllegalStateException("Strange. Multival property "+propName+" in "+beanClass+" produced null values list, parsed from "+containingNode);
			}
			if (!isHeteroListProperty) {
				checkJaxbElementConsistence(col, pc);
			}
		} else {
			throw new IllegalStateException("Uh? No setter nor getter.");
		}
	}

	private Class<?> specializeParamType(@NotNull XNode node, @NotNull Class<?> expectedType, @NotNull ParsingContext pc)
			throws SchemaException {
		if (node.getTypeQName() != null) {
			Class explicitType = getSchemaRegistry().determineClassForType(node.getTypeQName());
			return explicitType != null && expectedType.isAssignableFrom(explicitType) ? explicitType : expectedType;
			// (if not assignable, we hope the adaptation will do it)
		} else if (node.getElementName() != null) {
			Collection<TypeDefinition> candidateTypes = getSchemaRegistry()
					.findTypeDefinitionsByElementName(node.getElementName(), TypeDefinition.class);
			List<TypeDefinition> suitableTypes = candidateTypes.stream()
					.filter(def -> def.getCompileTimeClass() != null && expectedType.isAssignableFrom(def.getCompileTimeClass()))
					.collect(Collectors.toList());
			if (suitableTypes.isEmpty()) {
				pc.warnOrThrow(LOGGER, "Couldn't derive suitable type based on element name ("
						+ node.getElementName() + "). Candidate types: " + candidateTypes + "; expected type: " + expectedType);
				return null;
			} else if (suitableTypes.size() > 1) {
				pc.warnOrThrow(LOGGER, "Couldn't derive single suitable type based on element name ("
						+ node.getElementName() + "). Suitable types: " + suitableTypes);
				return null;
			}
			return suitableTypes.get(0).getCompileTimeClass();
		} else {
			return expectedType;
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

			// Maybe this is not used in this context, because node.elementName is filled-in only for heterogeneous list
			// members - and they are iterated through elsewhere. Nevertheless, it is more safe to include it also here.
//			QName realElementName = getRealElementName(node, key, pc);
//			String realElementLocalName = realElementName.getLocalPart();

			elementFactoryMethod = null;
			objectFactory = null;
			if (propertyField == null && propertyGetter == null) {
				// We have to try to find a more generic field, such as xsd:any or substitution element
				// check for global element definition first
				elementFactoryMethod = findElementFactoryMethod(propName);			// realElementLocalName
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

	private QName getRealElementName(XNode node, QName key, ParsingContext pc) throws SchemaException {
		if (node.getElementName() == null) {
			return key;
		}
		String elementNS = node.getElementName().getNamespaceURI();
		String keyNS = key.getNamespaceURI();
		if (StringUtils.isNotEmpty(elementNS) && StringUtils.isNotEmpty(keyNS) && !elementNS.equals(keyNS)) {
			pc.warnOrThrow(LOGGER, "Namespaces for actual element (" + node.getElementName()
					+ ") and it's place in schema (" + key + " are different.");
			return key;		// fallback
		} else {
			return node.getElementName();
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
	private Object wrapInJaxbElement(Object propVal, Object objectFactory, Method factoryMethod, String propName,
			Class beanClass, ParsingContext pc) {
		if (factoryMethod == null) {
			throw new IllegalArgumentException("Param type is JAXB element but no factory method found for it, property "+propName+" in "+beanClass);
		}
		try {
			return factoryMethod.invoke(objectFactory, propVal);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new SystemException("Unable to invoke factory method "+factoryMethod+" on "+objectFactory.getClass()+" for property "+propName+" in "+beanClass);
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
		col.add(subBeanElement != null ? subBeanElement.getValue() : null);
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

	private Object unmarshalSinglePropValue(XNode xsubnode, String fieldName, Class paramType, boolean storeAsRawType,
			Class classType, ParsingContext pc) throws SchemaException {
		Object propValue;
		if (xsubnode == null) {
			propValue = null;
		} else if (paramType.equals(XNode.class)) {
			propValue = xsubnode;
		} else if (storeAsRawType || paramType.equals(RawType.class)) {
            RawType raw = new RawType(xsubnode, prismContext);
			// FIXME UGLY HACK: parse value if possible
			if (xsubnode.getTypeQName() != null) {
				PrismValue value = prismContext.parserFor(xsubnode.toRootXNode()).parseItemValue();	// TODO what about objects? oid/version will be lost here
				if (value != null && !value.isRaw()) {
					raw = new RawType(value, xsubnode.getTypeQName(), prismContext);
				}
			}
			propValue = raw;
        } else {
            // paramType is what we expect e.g. based on parent definition
            // but actual type (given by xsi:type/@typeDef) may be different, e.g. more specific
			// (although we already specialized paramType within the caller, we do it again here, because the subnode
			// used here might be a child of node used in the caller)
			paramType = specializeParamType(xsubnode, paramType, pc);
            if (paramType == null) {
            	return null;				// skipping this element in case of error
			}
			if (xsubnode instanceof PrimitiveXNode<?> || xsubnode instanceof MapXNode || xsubnode.isHeterogeneousList()) {
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
		if (propValue instanceof PolyString) {
			propValue = new PolyStringType((PolyString) propValue);
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
		T filterType = instantiate(beanClass);
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
		if (protectedType.isEmpty()) {
			// E.g. in case when the xmap is empty or if there are is just an expression
			return null;
		}
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
