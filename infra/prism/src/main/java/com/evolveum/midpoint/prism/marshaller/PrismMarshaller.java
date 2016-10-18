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

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class PrismMarshaller {
	
	@NotNull private final PrismBeanConverter beanConverter;

	public PrismMarshaller(@NotNull PrismBeanConverter beanConverter) {
		this.beanConverter = beanConverter;
	}

	//region Public interface ======================================================================================

	/*
	 *  TODO reconsider what to return for empty items
	 *   1. null
	 *   2. Root(name, null)
	 *   3. Root(name, List())
	 *
	 *  TODO reconsider what to do if we have potentially multivalued property - whether to return list or not
	 */

	@NotNull
	RootXNode marshalItem(@NotNull Item<?, ?> item, QName itemName,
			ItemDefinition itemDefinition, SerializationContext context) throws SchemaException {

		QName realName = itemName != null ? itemName : item.getElementName();
		ItemDefinition realDefinition = itemDefinition != null ? itemDefinition : item.getDefinition();

		XNode content;
		if (item instanceof PrismObject) {
			content = marshalObjectContent((PrismObject) item, (PrismObjectDefinition) realDefinition, context);
		} else if (item.size() == 1) {
			content = marshalItemValue(item.getValue(0), realDefinition, context);
		} else {
			ListXNode xlist = new ListXNode();
			for (PrismValue val : item.getValues()) {
				xlist.add(marshalItemValue(val, realDefinition, context));
			}
			content = xlist;
		}
		return new RootXNode(realName, content);
	}

	RootXNode marshalItemValueAsRoot(@NotNull PrismValue value, @NotNull QName itemName, ItemDefinition itemDefinition,
			SerializationContext context) throws SchemaException {
		if (itemDefinition == null && value.getParent() != null) {
			itemDefinition = value.getParent().getDefinition();      // the definition may still be null here
		}
		XNode valueNode = marshalItemValue(value, itemDefinition, context);
		return new RootXNode(itemName, valueNode);
	}

	RootXNode marshalAnyData(@NotNull Object object, QName itemName, ItemDefinition itemDefinition, SerializationContext ctx) throws SchemaException {
		if (object instanceof Item) {
			return marshalItem((Item) object, itemName, itemDefinition, ctx);
		} else {
			Validate.notNull(itemName, "rootElementName must be specified for non-Item objects");
			XNode valueNode = beanConverter.marshall(object, ctx);		// TODO item definition!
			QName typeQName = JAXBUtil.getTypeQName(object.getClass());
			if (valueNode.getTypeQName() == null) {
				if (typeQName != null) {
					valueNode.setTypeQName(typeQName);
				} else {
					throw new SchemaException("No type QName for class " + object.getClass());
				}
			}
			return new RootXNode(itemName, valueNode);
		}
	}

	public boolean canSerialize(Object object) {
		if (object instanceof Item) {
			return true;
		} else {
			return beanConverter.canProcess(object.getClass());
		}
	}
    //endregion

	//region Implementation ======================================================================================

	private <O extends Objectable> MapXNode marshalObjectContent(@NotNull PrismObject<O> object, @NotNull PrismObjectDefinition<O> objectDefinition, SerializationContext ctx) throws SchemaException {
		MapXNode xmap = new MapXNode();
		if (object.getOid() != null) {
			xmap.put(XNode.KEY_OID, createPrimitiveXNodeStringAttr(object.getOid()));
		}
		if (object.getVersion() != null) {
			xmap.put(XNode.KEY_VERSION, createPrimitiveXNodeStringAttr(object.getVersion()));
		}
		marshalContainerValue(xmap, object.getValue(), objectDefinition, ctx);
		xmap.setTypeQName(objectDefinition.getTypeName());		// object should have the definition (?)
		return xmap;
	}

	@NotNull
    private <V extends PrismValue> XNode marshalItemValue(@NotNull PrismValue itemValue, ItemDefinition definition, SerializationContext ctx) throws SchemaException {
        XNode xnode;
        if (definition == null) {
            if (itemValue.getParent() != null) {
                definition = itemValue.getParent().getDefinition();
            }
        }
        if (definition == null && itemValue instanceof PrismPropertyValue) {
            return serializePropertyRawValue((PrismPropertyValue<?>) itemValue);
        } else if (itemValue instanceof PrismReferenceValue) {
            xnode = serializeReferenceValue((PrismReferenceValue)itemValue, (PrismReferenceDefinition) definition, ctx);
        } else if (itemValue instanceof PrismPropertyValue<?>) {
            xnode = serializePropertyValue((PrismPropertyValue<?>)itemValue, (PrismPropertyDefinition)definition);
        } else if (itemValue instanceof PrismContainerValue<?>) {
            xnode = marshalContainerValue((PrismContainerValue<?>)itemValue, (PrismContainerDefinition)definition, ctx);
        } else {
            throw new IllegalArgumentException("Unsupported value type "+itemValue.getClass());
        }
        if (definition != null && definition.isDynamic()) {
            xnode.setExplicitTypeDeclaration(true);
        }
        return xnode;
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
        if (containerVal.getConcreteType() != null) {
            xmap.setTypeQName(containerVal.getConcreteType());
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
					XNode xsubnode = marshalItem(item, null, null, ctx).getSubnode();
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
				XNode xsubnode = marshalItem(item, null, null, ctx).getSubnode();
				xmap.put(elementName, xsubnode);
			}
		}
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
            xmap.put(createReferenceQName(XNode.KEY_REFERENCE_FILTER, namespace), xsubnode);
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
    private <T> XNode serializePropertyValue(PrismPropertyValue<T> value, PrismPropertyDefinition<T> definition) throws SchemaException {
        QName typeQName = definition.getTypeName();
        T realValue = value.getValue();
        if (realValue instanceof PolyString) {
            return serializePolyString((PolyString) realValue);
        } else if (beanConverter.canProcess(typeQName)) {
            XNode xnode = beanConverter.marshall(realValue);
            if (realValue instanceof ProtectedDataType<?> && definition.isDynamic()) {          // why is this?
                xnode.setExplicitTypeDeclaration(true);
                xnode.setTypeQName(definition.getTypeName());
            }
            return xnode;
        } else {
            // primitive value
            return createPrimitiveXNode(realValue, typeQName);
        }
    }

    private XNode serializePolyString(PolyString realValue) {
        PrimitiveXNode<PolyString> xprim = new PrimitiveXNode<>();
        xprim.setValue(realValue, PolyStringType.COMPLEX_TYPE);
        return xprim;
    }

    private <T> XNode serializePropertyRawValue(PrismPropertyValue<T> value) throws SchemaException {
        Object rawElement = value.getRawElement();
        if (rawElement instanceof XNode) {
            return (XNode) rawElement;
        } else {
            T realValue = value.getValue();
            return createPrimitiveXNode(realValue, DOMUtil.XSD_STRING);
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

    private <T> PrimitiveXNode<T> createPrimitiveXNode(T val, QName type) {
        PrimitiveXNode<T> xprim = new PrimitiveXNode<T>();
        xprim.setValue(val, type);
        return xprim;
    }
    //endregion

}
