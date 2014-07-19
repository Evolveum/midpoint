/*
 * Copyright (c) 2010-2014 Evolveum
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.SchemaXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.prism.xml.ns._public.types_3.EncryptedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * @author semancik
 *
 */
public class XNodeSerializer {
	
	private PrismBeanConverter beanConverter;
	private boolean serializeCompositeObjects = false;

    // TODO think out where to put this key
    public static final String USER_DATA_KEY_COMMENT = XNodeSerializer.class.getName()+".comment";
	
	public XNodeSerializer(PrismBeanConverter beanConverter) {
		super();
		this.beanConverter = beanConverter;
	}

	public boolean isSerializeCompositeObjects() {
		return serializeCompositeObjects;
	}

	public void setSerializeCompositeObjects(boolean serializeCompositeObjects) {
		this.serializeCompositeObjects = serializeCompositeObjects;
	}

    //region Serializing objects
	public <O extends Objectable> RootXNode serializeObject(PrismObject<O> object) throws SchemaException {
		RootXNode xroot = new RootXNode();
		xroot.setSubnode(serializeObjectContent(object));
		xroot.setTypeQName(object.getDefinition().getTypeName());
		QName elementName = object.getElementName();
		xroot.setRootElementName(elementName);
		return xroot;
	}
	
	private <O extends Objectable> MapXNode serializeObjectContent(PrismObject<O> object) throws SchemaException {
		MapXNode xmap = new MapXNode();
		if (object.getOid() != null) {
			xmap.put(XNode.KEY_OID, createPrimitiveXNodeStringAttr(object.getOid()));
		}
		if (object.getVersion() != null) {
			xmap.put(XNode.KEY_VERSION, createPrimitiveXNodeStringAttr(object.getVersion()));
		}
		PrismObjectDefinition<O> objectDefinition = object.getDefinition();
		serializeContainerValue(xmap, object.getValue(), objectDefinition);
		return xmap;
	}
    //endregion

    //region Serializing (any) items
    public <V extends PrismValue> XNode serializeItem(Item<V> item) throws SchemaException {
        ListXNode xlist = new ListXNode();
        List<V> values = item.getValues();
        ItemDefinition definition = item.getDefinition();

        for (V val: values) {
            XNode xsubnode = serializeItemValue(val, definition);
            xlist.add(xsubnode);
        }

        boolean asList;
        if (definition != null) {
            asList = definition.isMultiValue();
        } else {
            asList = values.size() > 1;
        }

        if (asList) {
            return xlist;
        } else {
            if (xlist.isEmpty()) {
                return null;
            } else {
                return xlist.iterator().next();
            }
        }
    }

//    public <T> RootXNode serializePropertyValueAsRoot(PrismPropertyValue<T> propval, QName elementName) throws SchemaException {
//        Validate.notNull(propval, "Property value to be serialized cannot be null");
//        Validate.notNull(propval.getParent(), "Property value to be serialized must have a parent");
//        // maybe this condition could be relaxed in the future
//        Validate.notNull(propval.getParent().getDefinition(), "Property value to be serialized must have a parent with a definition");
//
//        ItemDefinition definition = propval.getParent().getDefinition();
//        XNode valueNode = serializeItemValue(propval, definition);
//        return new RootXNode(elementName, valueNode);
//    }

    public RootXNode serializeItemValueAsRoot(PrismValue value, QName elementName) throws SchemaException {
        Validate.notNull(value, "Item value to be serialized cannot be null");
        Validate.notNull(value.getParent(), "Item value to be serialized must have a parent");
        Validate.notNull(elementName, "Element name cannot be null");
        return serializeItemValueAsRootInternal(value, elementName);
    }

    // element name may be null (it is then derived from the definition, if the definition exists)
    private RootXNode serializeItemValueAsRootInternal(PrismValue value, QName elementName) throws SchemaException {
        ItemDefinition definition = value.getParent().getDefinition();      // the definition may be null here
        XNode valueNode = serializeItemValue(value, definition);
        if (elementName == null) {
            if (definition == null) {
                throw new IllegalStateException("The name of element to be serialized couldn't be determined, as there's no definition");
            }
            elementName = definition.getName();
        }
        return new RootXNode(elementName, valueNode);
    }

    public <V extends PrismValue> RootXNode serializeItemAsRoot(Item<V> item) throws SchemaException {
        Validate.notNull(item.getDefinition(), "Item without a definition");
        XNode valueNode = serializeItem(item);
        return new RootXNode(item.getDefinition().getName(), valueNode);
    }

    // definition may be null
    public <V extends PrismValue> XNode serializeItemValue(V itemValue, ItemDefinition definition) throws SchemaException {
        XNode xnode;
        if (definition == null) {
            if (itemValue.getParent() != null) {
                definition = itemValue.getParent().getDefinition();
            }
        }
        if (definition == null){
            return serializePropertyRawValue((PrismPropertyValue<?>) itemValue);
        }
        if (beanConverter.getPrismContext() == null) {
            throw new IllegalStateException("No prismContext in beanConverter!");
        }
        if (itemValue instanceof PrismReferenceValue) {
            xnode = serializeReferenceValue((PrismReferenceValue)itemValue, (PrismReferenceDefinition) definition);
        } else if (itemValue instanceof PrismPropertyValue<?>) {
            xnode = serializePropertyValue((PrismPropertyValue<?>)itemValue, (PrismPropertyDefinition)definition);
        } else if (itemValue instanceof PrismContainerValue<?>) {
            xnode = serializeContainerValue((PrismContainerValue<?>)itemValue, (PrismContainerDefinition)definition);
        } else {
            throw new IllegalArgumentException("Unsupported value type "+itemValue.getClass());
        }
        if (definition.isDynamic()) {
            xnode.setExplicitTypeDeclaration(true);
        }
        Object commentValue = itemValue.getUserData(USER_DATA_KEY_COMMENT);
        if (commentValue != null) {
            xnode.setComment(commentValue.toString());
        }
//		System.out.println("item value serialization: \n" + xnode.debugDump());
        return xnode;
    }
    //endregion

    //region Serializing containers - specific functionality
//	public <C extends Containerable> RootXNode serializeContainerValueAsRoot(PrismContainerValue<C> containerVal) throws SchemaException {
//        Validate.notNull(containerVal);
//		return serializeItemValueAsRootInternal(containerVal, null);
//	}
//
//    public <C extends Containerable> RootXNode serializeContainerValueAsRoot(PrismContainerValue<C> containerVal, QName elementName) throws SchemaException {
//        Validate.notNull(containerVal);
//        Validate.notNull(elementName);
//        return serializeItemValueAsRootInternal(containerVal, elementName);
//    }

    private <C extends Containerable> MapXNode serializeContainerValue(PrismContainerValue<C> containerVal, PrismContainerDefinition<C> containerDefinition) throws SchemaException {
		MapXNode xmap = new MapXNode();
		serializeContainerValue(xmap, containerVal, containerDefinition);
		return xmap;
	}
	
	private <C extends Containerable> void serializeContainerValue(MapXNode xmap, PrismContainerValue<C> containerVal, PrismContainerDefinition<C> containerDefinition) throws SchemaException {
		Long id = containerVal.getId();
		if (id != null) {
			xmap.put(XNode.KEY_CONTAINER_ID, createPrimitiveXNodeAttr(id, DOMUtil.XSD_LONG));
		}
        if (containerVal.getConcreteType() != null) {
            xmap.setTypeQName(containerVal.getConcreteType());
        }

		Collection<QName> serializedItems = new ArrayList<>();
		if (containerDefinition != null) {
			// We have to serialize in the definition order. Some data formats (XML) are
			// ordering-sensitive. We need to keep that ordering otherwise the resulting
			// document won't pass schema validation
			for (ItemDefinition itemDef: containerDefinition.getDefinitions()) {
				QName elementName = itemDef.getName();
				Item<?> item = containerVal.findItem(elementName);
				if (item != null) {
					XNode xsubnode = serializeItem(item);
					xmap.put(elementName, xsubnode);
					serializedItems.add(elementName);
				}
			}
		}
		// There are some cases when we do not have list of all elements in a container.
		// E.g. in run-time schema. Therefore we must also iterate over items and not just item definitions.
		if (containerVal.getItems() != null){
			for (Item<?> item : containerVal.getItems()) {
				QName elementName = item.getElementName();
				if (serializedItems.contains(elementName)) {
					continue;
				}
				XNode xsubnode = serializeItem(item);
				xmap.put(elementName, xsubnode);
			}
		}
	}
    //endregion

    //region Serializing references - specific functionality
    private XNode serializeReferenceValue(PrismReferenceValue value, PrismReferenceDefinition definition) throws SchemaException {
        MapXNode xmap = new MapXNode();
        String namespace = definition != null ? definition.getNamespace() : null;           // namespace for filter and description
        if (StringUtils.isNotBlank(value.getOid())){
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

        boolean isComposite = false;
        if (definition != null) {
            isComposite = definition.isComposite();
        }
        if ((serializeCompositeObjects || isComposite) && value.getObject() != null) {
            XNode xobjnode = serializeObjectContent(value.getObject());
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
        xprim.setValue(realValue);
        xprim.setTypeQName(PolyStringType.COMPLEX_TYPE);
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
        xprim.setValue(val);
        xprim.setTypeQName(type);
        return xprim;
    }
    //endregion

}
