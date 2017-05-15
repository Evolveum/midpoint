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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author semancik
 *
 */
public class PrismMarshaller {
	
	@NotNull private final BeanMarshaller beanMarshaller;

	public PrismMarshaller(@NotNull BeanMarshaller beanMarshaller) {
		this.beanMarshaller = beanMarshaller;
	}

	//region Public interface ======================================================================================
	/*
	 *  These methods should not be called from the inside of marshaller. At entry, they invoke ItemInfo methods
	 *  to determine item name, definition and type to use. From inside marshalling process, these elements have
	 *  to be provided more-or-less by the caller.
	 */

	/**
	 * Marshals a given prism item (object, container, reference, property).
	 *
	 * @param item Item to be marshaled.
	 * @param itemName Name to give to the item in the marshaled form. Usually null (i.e. taken from the item itself).
	 * @param itemDefinition Definition to be used when parsing. Usually null (i.e. taken from the item itself).
	 * @param context Serialization context.
	 * @return Marshaled item.
	 */
	@NotNull
	RootXNode marshalItemAsRoot(@NotNull Item<?, ?> item, QName itemName,
			ItemDefinition itemDefinition, SerializationContext context) throws SchemaException {

		@NotNull QName realItemName = itemName != null ? itemName : item.getElementName();
		ItemDefinition realItemDefinition = itemDefinition != null ? itemDefinition : item.getDefinition();

		XNode content = marshalItemContent(item, realItemDefinition, context);
		if (realItemDefinition != null) {
			addTypeDefinitionIfNeeded(realItemName, realItemDefinition.getTypeName(), content);
		}
		return new RootXNode(realItemName, content);
	}

	/**
	 * Marshals a single PrismValue. For simplicity and compatibility with other interfaces, the result is always a RootXNode.
	 *
	 * @param value PrismValue to be marshaled.
	 * @param itemName Item name to be used. Optional. If omitted, it is derived from value and/or definition.
	 * @param itemDefinition Item definition to be used. Optional.
	 * @param context Serialization context.
	 * @return Marshaled prism value.
	 */
	@NotNull
	RootXNode marshalPrismValueAsRoot(@NotNull PrismValue value, QName itemName, ItemDefinition itemDefinition,
			SerializationContext context) throws SchemaException {
        ItemInfo itemInfo = ItemInfo.determineFromValue(value, itemName, itemDefinition, getSchemaRegistry());
		QName realItemName = itemInfo.getItemName();
		ItemDefinition realItemDefinition = itemInfo.getItemDefinition();
		QName realItemTypeName = itemInfo.getTypeName();

		if (realItemName == null) {
			throw new IllegalArgumentException("Couldn't determine item name from the prism value; cannot marshal to RootXNode");
		}

		XNode valueNode = marshalItemValue(value, realItemDefinition, realItemTypeName, context);
		addTypeDefinitionIfNeeded(realItemName, realItemTypeName, valueNode);
		return new RootXNode(realItemName, valueNode);
	}

	/**
	 * Marshals any data - prism item or real value.
	 *
	 * @param object Object to be marshaled.
	 * @param itemName Item name to be used. Optional. If omitted, it is derived from value and/or definition.
	 * @param itemDefinition Item definition to be used. Optional.
	 * @param context Serialization context.
	 * @return Marshaled object.
	 */
	@NotNull
	RootXNode marshalAnyData(@NotNull Object object, QName itemName, ItemDefinition itemDefinition, SerializationContext context) throws SchemaException {
		if (object instanceof Item) {
			return marshalItemAsRoot((Item) object, itemName, itemDefinition, context);
		}
		Validate.notNull(itemName, "itemName must be specified for non-Item objects");
		if (object instanceof Containerable) {
			return marshalPrismValueAsRoot(((Containerable) object).asPrismContainerValue(), itemName, null, context);
		} else if (beanMarshaller.canProcess(object.getClass())) {
			XNode valueNode = beanMarshaller.marshall(object, context);        // TODO item definition!
			QName typeName = JAXBUtil.getTypeQName(object.getClass());
			if (valueNode != null) {
				addTypeDefinitionIfNeeded(itemName, typeName, valueNode);
				if (valueNode.getTypeQName() == null && typeName == null) {
					throw new SchemaException("No type QName for class " + object.getClass());
				}
			} // TODO or else put type name at least to root? (but, can valueNode be null if object is not null?)
			return new RootXNode(itemName, valueNode);
		} else {
			throw new IllegalArgumentException("Couldn't serialize " + object);
		}
	}

	public boolean canSerialize(Object object) {
		if (object instanceof Item) {
			return true;
		} else {
			return beanMarshaller.canProcess(object.getClass());
		}
	}

    /*
	 *  TODO reconsider what to return for empty items
	 *   1. null
	 *   2. Root(name, null)
	 *   3. Root(name, List())
	 *
	 *  TODO reconsider what to do if we have potentially multivalued property - whether to return list or not
	 */

	//endregion

	//region Implementation ======================================================================================

	/**
	 * Marshals everything from the item except for the root node.
	 * Separated from marshalItemAsRoot in order to be reusable.
	 */
	@NotNull
	private XNode marshalItemContent(@NotNull Item<?, ?> item,
			ItemDefinition itemDefinition, SerializationContext context) throws SchemaException {
		if (item.size() == 1) {
			return marshalItemValue(item.getValue(0), itemDefinition, null, context);
		} else {
			ListXNode xlist = new ListXNode();
			for (PrismValue val : item.getValues()) {
				xlist.add(marshalItemValue(val, itemDefinition, null, context));
			}
			return xlist;
		}
	}

	@NotNull
	private <O extends Objectable> MapXNode marshalObjectContent(@NotNull PrismObject<O> object, @NotNull PrismObjectDefinition<O> objectDefinition, SerializationContext ctx) throws SchemaException {
		MapXNode xmap = new MapXNode();
		marshalContainerValue(xmap, object.getValue(), objectDefinition, ctx);
		xmap.setTypeQName(objectDefinition.getTypeName());		// object should have the definition (?)
		return xmap;
	}

	@SuppressWarnings("unchecked")
	@NotNull
    private XNode marshalItemValue(@NotNull PrismValue itemValue, @Nullable ItemDefinition definition,
			@Nullable QName typeName, SerializationContext ctx) throws SchemaException {
        XNode xnode;
        if (definition == null && typeName == null && itemValue instanceof PrismPropertyValue) {
            return serializePropertyRawValue((PrismPropertyValue<?>) itemValue);
        } else if (itemValue instanceof PrismReferenceValue) {
            xnode = serializeReferenceValue((PrismReferenceValue)itemValue, (PrismReferenceDefinition) definition, ctx);
        } else if (itemValue instanceof PrismPropertyValue<?>) {
            xnode = serializePropertyValue((PrismPropertyValue<?>)itemValue, (PrismPropertyDefinition) definition, typeName);
        } else if (itemValue instanceof PrismContainerValue<?>) {
            xnode = marshalContainerValue((PrismContainerValue<?>)itemValue, (PrismContainerDefinition) definition, ctx);
        } else {
            throw new IllegalArgumentException("Unsupported value type "+itemValue.getClass());
        }
        if (definition != null && definition.isDynamic() && isInstantiable(definition)) {
			if (xnode.getTypeQName() == null) {
				xnode.setTypeQName(definition.getTypeName());
			}
            xnode.setExplicitTypeDeclaration(true);
        }
        return xnode;
    }

    // TODO FIXME first of all, Extension definition should not be marked as dynamic
	private boolean isInstantiable(ItemDefinition definition) {
		if (definition.isAbstract()) {
			return false;
		}
		if (definition instanceof PrismContainerDefinition) {
			PrismContainerDefinition pcd = (PrismContainerDefinition) definition;
			ComplexTypeDefinition ctd = pcd.getComplexTypeDefinition();
			return ctd != null && ctd.getCompileTimeClass() != null;
		} else if (definition instanceof PrismPropertyDefinition) {
			PrismPropertyDefinition ppd = (PrismPropertyDefinition) definition;
			if (ppd.isAnyType()) {
				return false;
			}
			// TODO optimize
			return getSchemaRegistry().determineClassForType(ppd.getTypeName()) != null
					|| getSchemaRegistry().findTypeDefinitionByType(ppd.getTypeName(), TypeDefinition.class) != null;
		} else {
			return false;
		}
	}

	private <C extends Containerable> MapXNode marshalContainerValue(PrismContainerValue<C> containerVal, PrismContainerDefinition<C> containerDefinition, SerializationContext ctx) throws SchemaException {
		MapXNode xmap = new MapXNode();
		marshalContainerValue(xmap, containerVal, containerDefinition, ctx);
		return xmap;
	}
	
	private <C extends Containerable> void marshalContainerValue(MapXNode xmap, PrismContainerValue<C> containerVal, PrismContainerDefinition<C> containerDefinition, SerializationContext ctx) throws SchemaException {
		Long id = containerVal.getId();
		if (id != null) {
			xmap.put(XNode.KEY_CONTAINER_ID, createPrimitiveXNodeAttr(id, DOMUtil.XSD_LONG));
		}
		if (containerVal instanceof PrismObjectValue) {
			PrismObjectValue<?> objectVal = (PrismObjectValue<?>) containerVal;
			if (objectVal.getOid() != null) {
				xmap.put(XNode.KEY_OID, createPrimitiveXNodeStringAttr(objectVal.getOid()));
			}
			if (objectVal.getVersion() != null) {
				xmap.put(XNode.KEY_VERSION, createPrimitiveXNodeStringAttr(objectVal.getVersion()));
			}
		}

		// We put the explicit type name only if it's different from the parent one
		// (assuming this value is NOT serialized as a standalone one: in that case its
		// type must be marshaled in a special way).
		QName specificTypeName = getSpecificTypeName(containerVal);
        if (specificTypeName != null) {
            xmap.setTypeQName(specificTypeName);
            xmap.setExplicitTypeDeclaration(true);
        }

		Collection<QName> marshaledItems = new ArrayList<>();
		if (containerDefinition != null) {
			// We have to serialize in the definition order. Some data formats (XML) are
			// ordering-sensitive. We need to keep that ordering otherwise the resulting
			// document won't pass schema validation
			for (ItemDefinition itemDef: containerDefinition.getDefinitions()) {
				QName elementName = itemDef.getName();
				Item<?,?> item = containerVal.findItem(elementName);
				if (item != null) {
					XNode xsubnode = marshalItemContent(item, getItemDefinition(containerVal, item), ctx);
					xmap.put(elementName, xsubnode);
					marshaledItems.add(elementName);
				}
			}
		}
		// There are some cases when we do not have list of all elements in a container.
		// E.g. in run-time schema. Therefore we must also iterate over items and not just item definitions.
		if (containerVal.getItems() != null){
			for (Item<?,?> item : containerVal.getItems()) {
				QName elementName = item.getElementName();
				if (marshaledItems.contains(elementName)) {
					continue;
				}
				XNode xsubnode = marshalItemContent(item, getItemDefinition(containerVal, item), ctx);
				xmap.put(elementName, xsubnode);
			}
		}
	}

	private <C extends Containerable> ItemDefinition getItemDefinition(PrismContainerValue<C> cval, Item<?, ?> item) {
		if (item.getDefinition() != null) {
			return item.getDefinition();
		}
		ComplexTypeDefinition ctd = cval.getComplexTypeDefinition();
		if (ctd == null) {
			return null;
		}
		return ctd.findItemDefinition(item.getElementName());
	}

	// Returns type QName if it is different from parent's one and if it's suitable to be put to marshaled form
	private <C extends Containerable> QName getSpecificTypeName(PrismContainerValue<C> cval) {
		if (cval.getParent() == null) {
			return null;
		}
		ComplexTypeDefinition ctdValue = cval.getComplexTypeDefinition();
		ComplexTypeDefinition ctdParent = cval.getParent().getComplexTypeDefinition();
		QName typeValue = ctdValue != null ? ctdValue.getTypeName() : null;
		QName typeParent = ctdParent != null ? ctdParent.getTypeName() : null;

		if (typeValue == null || typeValue.equals(typeParent)) {
			return null;
		}
		if (ctdValue.getCompileTimeClass() == null) {
			// TODO.................
			return null;
		}
		return typeValue;
	}

	private XNode serializeReferenceValue(PrismReferenceValue value, PrismReferenceDefinition definition, SerializationContext ctx) throws SchemaException {
        MapXNode xmap = new MapXNode();
        boolean containsOid = false;
        String namespace = definition != null ? definition.getNamespace() : null;           // namespace for filter and description
        if (StringUtils.isNotBlank(value.getOid())){
            containsOid = true;
        	xmap.put(XNode.KEY_REFERENCE_OID, createPrimitiveXNodeStringAttr(value.getOid()));
        }
        QName relation = value.getRelation();
        if (relation != null) {
            xmap.put(XNode.KEY_REFERENCE_RELATION, createPrimitiveXNodeAttr(relation, DOMUtil.XSD_QNAME));
        }
        QName targetType = value.getTargetType();
        if (targetType != null) {
            xmap.put(XNode.KEY_REFERENCE_TYPE, createPrimitiveXNodeAttr(targetType, DOMUtil.XSD_QNAME));
        }
        String description = value.getDescription();
        if (description != null) {
            xmap.put(createReferenceQName(XNode.KEY_REFERENCE_DESCRIPTION, namespace), createPrimitiveXNode(description, DOMUtil.XSD_STRING));
        }
        SearchFilterType filter = value.getFilter();
        if (filter != null) {
            XNode xsubnode = filter.serializeToXNode();
			if (xsubnode != null) {
				xmap.put(createReferenceQName(XNode.KEY_REFERENCE_FILTER, namespace), xsubnode);
			}
        }
        EvaluationTimeType resolutionTime = value.getResolutionTime();
        if (resolutionTime != null) {
        	xmap.put(createReferenceQName(XNode.KEY_REFERENCE_RESOLUTION_TIME, namespace), 
        			createPrimitiveXNode(resolutionTime.value(), DOMUtil.XSD_STRING));
        }
        if (value.getTargetName() != null) {
            if (SerializationContext.isSerializeReferenceNames(ctx)) {
                XNode xsubnode = createPrimitiveXNode(value.getTargetName(), PolyStringType.COMPLEX_TYPE);
                xmap.put(createReferenceQName(XNode.KEY_REFERENCE_TARGET_NAME, namespace), xsubnode);
            } else {
                String commentValue = " " + value.getTargetName().getOrig() + " ";
                xmap.setComment(commentValue);
            }
        }

        boolean isComposite = false;
        if (definition != null) {
            isComposite = definition.isComposite();
        }
        if ((SerializationContext.isSerializeCompositeObjects(ctx) || isComposite || !containsOid) && value.getObject() != null) {
            XNode xobjnode = marshalObjectContent(value.getObject(), value.getObject().getDefinition(), ctx);
            xmap.put(createReferenceQName(XNode.KEY_REFERENCE_OBJECT, namespace), xobjnode);
        }

        return xmap;
    }

    // expects that qnames have null namespaces by default
    // namespace (second parameter) may be null if unknown
    private QName createReferenceQName(QName qname, String namespace) {
        if (namespace != null) {
            return new QName(namespace, qname.getLocalPart());
        } else {
            return qname;
        }
    }
    //endregion

    //region Serializing properties - specific functionality
    private <T> XNode serializePropertyValue(@NotNull PrismPropertyValue<T> value, PrismPropertyDefinition<T> definition, QName typeNameIfNoDefinition) throws SchemaException {
        @Nullable QName typeName = definition != null ? definition.getTypeName() : typeNameIfNoDefinition;
        ExpressionWrapper expression = value.getExpression();
        if (expression != null) {
        	// Store expression, not the value. In this case the value (if any) is 
        	// a transient product of the expression evaluation.
        	return createExpressionXNode(expression);
        }
        T realValue = value.getValue();
        if (realValue instanceof PolyString) {
            return serializePolyString((PolyString) realValue);
        } else if (beanMarshaller.canProcess(typeName)) {
            XNode xnode = beanMarshaller.marshall(realValue);
            if (realValue.getClass().getPackage() != null) {
				TypeDefinition typeDef = getSchemaRegistry()
						.findTypeDefinitionByCompileTimeClass(realValue.getClass(), TypeDefinition.class);
				if (xnode != null && typeDef != null && !QNameUtil.match(typeDef.getTypeName(), typeName)) {
					xnode.setTypeQName(typeDef.getTypeName());
					xnode.setExplicitTypeDeclaration(true);
				}
			}
			return xnode;
        } else {
            // primitive value
            return createPrimitiveXNode(realValue, typeName);
        }
    }

	private XNode serializePolyString(PolyString realValue) {
        PrimitiveXNode<PolyString> xprim = new PrimitiveXNode<>();
        xprim.setValue(realValue, PolyStringType.COMPLEX_TYPE);
        return xprim;
    }

    @NotNull
    private <T> XNode serializePropertyRawValue(PrismPropertyValue<T> value) throws SchemaException {
        XNode rawElement = value.getRawElement();
        if (rawElement != null) {
            return rawElement;
        }
        T realValue = value.getValue();
        if (realValue != null) {
            return createPrimitiveXNode(realValue, null);
        } else {
            throw new IllegalStateException("Neither real nor raw value present in " + value);
        }
    }

    private PrimitiveXNode<String> createPrimitiveXNodeStringAttr(String val) {
        return createPrimitiveXNodeAttr(val, DOMUtil.XSD_STRING);
    }

    private <T> PrimitiveXNode<T> createPrimitiveXNodeAttr(T val, QName type) {
        PrimitiveXNode<T> xprim = createPrimitiveXNode(val, type);
        xprim.setAttribute(true);
        return xprim;
    }

    @NotNull
    private <T> PrimitiveXNode<T> createPrimitiveXNode(T val, QName type) {
        PrimitiveXNode<T> xprim = new PrimitiveXNode<T>();
        xprim.setValue(val, type);
        return xprim;
    }
    
    @NotNull
    private XNode createExpressionXNode(@NotNull ExpressionWrapper expression) {
		return PrismUtil.serializeExpression(expression);
	}

	@NotNull
	private SchemaRegistry getSchemaRegistry() {
		return beanMarshaller.getPrismContext().getSchemaRegistry();
	}

	private void addTypeDefinitionIfNeeded(@NotNull QName itemName, QName typeName, @NotNull XNode valueNode) {
		if (valueNode.getTypeQName() != null && valueNode.isExplicitTypeDeclaration()) {
			return; // already set
		}
		if (typeName == null) {
			return;	// nothing to do, anyway
		}
		if (!getSchemaRegistry().hasImplicitTypeDefinition(itemName, typeName)
				&& (XmlTypeConverter.canConvert(typeName)
						|| getSchemaRegistry().findTypeDefinitionByType(typeName) != null)) {
			valueNode.setTypeQName(typeName);
			valueNode.setExplicitTypeDeclaration(true);
		}
	}

	//endregion

}
