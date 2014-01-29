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

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class XNodeSerializer {
	
	private PrismBeanConverter beanConverter;
	private boolean serializeCompositeObjects = false;
	
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

	
	public <O extends Objectable> RootXNode serializeObject(PrismObject<O> object) throws SchemaException {
		RootXNode xroot = new RootXNode();
		xroot.setSubnode(serializeObjectContent(object));
		xroot.setTypeQName(object.getDefinition().getTypeName());
		xroot.setRootElementName(object.getElementName());
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
		for (Item<?> item: containerVal.getItems()) {
			QName elementName = item.getElementName();
			XNode xsubnode = serializeItem(item);
			xmap.put(elementName, xsubnode);
		}
	}
	
	private <V extends PrismValue> XNode serializeItem(Item<V> item) throws SchemaException {
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
	
	private <V extends PrismValue> XNode serializeItemValue(V itemValue, ItemDefinition definition) throws SchemaException {
		XNode xnode;
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
		return xnode;
	}
	
	private XNode serializeReferenceValue(PrismReferenceValue value, PrismReferenceDefinition definition) throws SchemaException {
		MapXNode xmap = new MapXNode();
		xmap.put(XNode.KEY_REFERENCE_OID, createPrimitiveXNodeStringAttr(value.getOid()));
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
			xmap.put(XNode.KEY_REFERENCE_DESCRIPTION, createPrimitiveXNode(description, DOMUtil.XSD_STRING));
		}
		ObjectFilter filter = value.getFilter();
		if (filter != null) {
			XNode xsubnode = QueryConvertor.serializeFilter(filter);
			xmap.put(XNode.KEY_REFERENCE_FILTER, xsubnode);
		}
		
		boolean isComposite = false;
		if (definition != null) {
			isComposite = definition.isComposite();
		}
		if ((serializeCompositeObjects || isComposite) && value.getObject() != null) {
			XNode xobjnode = serializeObjectContent(value.getObject());
			xmap.put(XNode.KEY_REFERENCE_OBJECT, xobjnode);
		}
		
		return xmap;
	}
		
	private <T> XNode serializePropertyValue(PrismPropertyValue<T> value, PrismPropertyDefinition<T> definition) throws SchemaException {
		QName typeQName = definition.getTypeName();
		T realValue = value.getValue();
		if (beanConverter.canConvert(typeQName)) {
			return beanConverter.marshall(realValue);
		} else {
			// primitive value
			return createPrimitiveXNode(realValue, typeQName);
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
	
}
