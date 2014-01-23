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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public class XNodeProcessor {
	
	private PrismContext prismContext;
	
	public SchemaRegistry getSchemaRegistry() {
		return prismContext.getSchemaRegistry();
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public <O extends Objectable> PrismObject<O> parseObject(XNode xnode) throws SchemaException {
		if (xnode instanceof RootXNode) {
			return parseObject((RootXNode)xnode);
		} else if (xnode instanceof MapXNode) {
			return parseObject((MapXNode)xnode);
		} else {
			throw new IllegalArgumentException("Cannot parse object from "+xnode);
		}
	}
	
	public <O extends Objectable> PrismObject<O> parseObject(RootXNode rootXnode) throws SchemaException {
		QName rootElementName = rootXnode.getRootElementName();
		PrismObjectDefinition<O> objectDefinition = null;
		if (rootXnode.getTypeQName() != null) {
			objectDefinition = getSchemaRegistry().findObjectDefinitionByType(rootXnode.getTypeQName());
			if (objectDefinition == null) {
				throw new SchemaException("No object definition for type "+rootXnode.getTypeQName());
			}
		} else {
			objectDefinition = getSchemaRegistry().findObjectDefinitionByElementName(rootElementName);
			if (objectDefinition == null) {
				throw new SchemaException("No object definition for element name "+rootElementName);
			}
		}
		if (objectDefinition == null) {
			throw new SchemaException("Cannot locate object definition (unspecified reason)");
		}
		XNode subnode = rootXnode.getSubnode();
		if (!(subnode instanceof MapXNode)) {
			throw new IllegalArgumentException("Cannot parse object from "+subnode.getClass().getSimpleName()+", we need a map");
		}
		return parseObject((MapXNode)subnode, rootElementName, objectDefinition);
	}
	
	public <O extends Objectable> PrismObject<O> parseObject(MapXNode xnode, PrismObjectDefinition<O> objectDefinition) throws SchemaException {
		return parseObject(xnode, new QName(null, "object"), objectDefinition);
	}

	private <O extends Objectable> PrismObject<O> parseObject(MapXNode xnode, QName elementName, PrismObjectDefinition<O> objectDefinition) throws SchemaException {
		PrismObject<O> object = (PrismObject<O>) parsePrismContainer(xnode, elementName, objectDefinition);
		object.setOid(getOid(xnode));
		object.setVersion(getVersion(xnode));
		return object;
	}
	
	private <T> T getValue(MapXNode xmap, QName key, QName typeName) throws SchemaException {
		XNode xnode = xmap.get(key);
		if (xnode == null) {
			return null;
		}
		if (!(xnode instanceof PrimitiveXNode<?>)) {
			throw new SchemaException("Expected that field "+key+" will be primitive, but it is "+xnode.getDesc());
		}
		PrimitiveXNode<T> xprim = (PrimitiveXNode<T>)xnode;
		if (!xprim.isParsed()) {
			xprim.parseValue(typeName);
		}
		return xprim.getValue();
	}
	
	public <C extends Containerable> PrismContainer<C> parsePrismContainer(XNode xnode, QName elementName, 
			PrismContainerDefinition<C> containerDef) throws SchemaException {
		if (xnode instanceof MapXNode) {
			return parsePrismContainer((MapXNode)xnode, elementName, containerDef);
		} else if (xnode instanceof ListXNode) {
			PrismContainer<C> container = containerDef.instantiate(elementName);
			for (XNode xsubnode: (ListXNode)xnode) {
				parsePrismContainerValue(xsubnode, container);
			}
			return container;
		} else {
			throw new IllegalArgumentException("Cannot parse container from "+xnode);
		}
	}
	
	public <C extends Containerable> PrismContainer<C> parsePrismContainer(MapXNode xmap, QName elementName, 
			PrismContainerDefinition<C> containerDef) throws SchemaException {
		PrismContainer<C> container = containerDef.instantiate(elementName);
		parsePrismContainerValue(xmap, container);
		return container;
	}
	
	public <C extends Containerable> PrismContainerValue<C> parsePrismContainerValue(XNode xnode, PrismContainer<C> container)
			throws SchemaException {
		if (xnode instanceof MapXNode) {
			return parsePrismContainerValue((MapXNode)xnode, container);
		} else {
			throw new IllegalArgumentException("Cannot parse container value from "+xnode);
		}
	}

	public <C extends Containerable> PrismContainerValue<C> parsePrismContainerValue(MapXNode xmap, PrismContainer<C> container)
			throws SchemaException {
		Long id = getContainerId(xmap);
		PrismContainerValue<C> cval = new PrismContainerValue<C>(null, null, container, id);
		PrismContainerDefinition<C> containerDef = container.getDefinition();
		for (Entry<QName,XNode> xentry: xmap.entrySet()) {
			QName itemQName = xentry.getKey();
			if (QNameUtil.match(itemQName, XNode.KEY_CONTAINER_ID)) {
				continue;
			}
			ItemDefinition itemDef = locateItemDefinition(containerDef, itemQName, xentry.getValue());
			if (itemDef == null) {
				if (containerDef.isRuntimeSchema()) {
					// No definition for item, but the schema is runtime. the definition may come later.
					// Null is OK here.
				} else {
					throw new SchemaException("Item " + itemQName + " has no definition", itemQName);
				}
			}
			Item<?> item = parseItem(xentry.getValue(), itemQName, itemDef);
			cval.add(item);
		}
		return cval;
	}
	
	private <T extends Containerable> ItemDefinition locateItemDefinition(
			PrismContainerDefinition<T> containerDefinition, QName elementQName, XNode xnode)
			throws SchemaException {
		ItemDefinition def = containerDefinition.findItemDefinition(elementQName);
		if (def != null) {
			return def;
		}

		def = resolveDynamicItemDefinition(containerDefinition, elementQName, xnode);
		if (def != null) {
			return def;
		}

		if (containerDefinition.isRuntimeSchema()) {
			// Try to locate global definition in any of the schemas
			def = resolveGlobalItemDefinition(containerDefinition, elementQName, xnode);
		}
		return def;
	}
	
	private ItemDefinition resolveDynamicItemDefinition(ItemDefinition parentDefinition, QName elementName,
			XNode xnode) throws SchemaException {
		QName typeName = xnode.getTypeQName();
		// FIXME: now the definition assumes property, may also be property
		// container?
		if (typeName == null) {
			return null;
		}
		PrismPropertyDefinition propDef = new PrismPropertyDefinition(elementName, typeName, prismContext);
		Integer maxOccurs = xnode.getMaxOccurs();
		if (maxOccurs != null) {
			propDef.setMaxOccurs(maxOccurs);
		}
		propDef.setDynamic(true);
		return propDef;
	}
	
	private <T extends Containerable> ItemDefinition resolveGlobalItemDefinition(
			PrismContainerDefinition<T> containerDefinition, QName elementQName, XNode xnode)
			throws SchemaException {
		return prismContext.getSchemaRegistry().resolveGlobalItemDefinition(elementQName);
	}
	
	/**
	 * This gets definition of an unspecified type. It has to find the right
	 * method to call. Value elements have the same element name. They may be
	 * elements of a property or a container.
	 */
	public <V extends PrismValue> Item<V> parseItem(XNode xnode, QName itemName, ItemDefinition itemDef)
			throws SchemaException {
		if (itemDef == null) {
			// Assume property in a container with runtime definition
			return (Item<V>) parsePrismPropertyRaw(xnode, itemName);
		}
		if (itemDef instanceof PrismContainerDefinition) {
			return (Item<V>) parsePrismContainer(xnode, itemName, (PrismContainerDefinition<?>) itemDef);
		} else if (itemDef instanceof PrismPropertyDefinition) {
			return (Item<V>) parsePrismProperty(xnode, itemName, (PrismPropertyDefinition) itemDef);
		}
		if (itemDef instanceof PrismReferenceDefinition) {
			return (Item<V>) parsePrismReference(xnode, itemName, (PrismReferenceDefinition) itemDef);
		} else {
			throw new IllegalArgumentException("Attempt to parse unknown definition type " + itemDef.getClass().getName());
		}
	}
	
	public <T> PrismProperty<T> parsePrismProperty(XNode xnode, QName propName,
			PrismPropertyDefinition<T> propertyDefinition) throws SchemaException {
		if (xnode instanceof ListXNode) {
			return parsePrismProperty((ListXNode)xnode, propName, propertyDefinition);
		} else if (xnode instanceof MapXNode) {
			return parsePrismProperty((MapXNode)xnode, propName, propertyDefinition);
		} else if (xnode instanceof PrimitiveXNode<?>) {
			return parsePrismProperty((PrimitiveXNode)xnode, propName, propertyDefinition);
		} else {
			throw new IllegalArgumentException("Cannot parse property from " + xnode);
		}
	}
	
	public <T> PrismProperty<T> parsePrismProperty(ListXNode xlist, QName propName,
			PrismPropertyDefinition<T> propertyDefinition) throws SchemaException {
		if (xlist == null || xlist.isEmpty()) {
			return null;
		}
		PrismProperty<T> prop = propertyDefinition.instantiate(propName);

		if (!propertyDefinition.isMultiValue() && xlist.size() > 1) {
			throw new SchemaException("Attempt to store multiple values in single-valued property " + propName);
		}

		for (XNode xsubnode : xlist) {
			PrismPropertyValue<T> pval = parsePrismPropertyValue(xsubnode, prop);
			if (pval != null) {
				prop.add(pval);
			}
		}
		return prop;
	}
	
	public <T> PrismProperty<T> parsePrismProperty(MapXNode xmap, QName propName,
			PrismPropertyDefinition<T> propertyDefinition) throws SchemaException {
		PrismProperty<T> prop = propertyDefinition.instantiate(propName);
		PrismPropertyValue<T> pval = parsePrismPropertyValue(xmap, prop);
		if (pval != null) {
			prop.add(pval);
		}
		return prop;
	}
	
	public <T> PrismProperty<T> parsePrismProperty(PrimitiveXNode<T> xprim, QName propName,
			PrismPropertyDefinition<T> propertyDefinition) throws SchemaException {
		PrismProperty<T> prop = propertyDefinition.instantiate(propName);
		PrismPropertyValue<T> pval = parsePrismPropertyValue(xprim, prop);
		if (pval != null) {
			prop.add(pval);
		}
		return prop;
	}
	
	public <T> PrismPropertyValue<T> parsePrismPropertyValue(XNode xnode, PrismProperty<T> property) throws SchemaException {
		T realValue = parsePrismPropertyRealValue(xnode, property.getDefinition());
		if (realValue == null) {
			return null;
		}
		return new PrismPropertyValue<T>(realValue);
	}
	
	public <T> T parsePrismPropertyRealValue(XNode xnode, PrismPropertyDefinition<T> propertyDef) throws SchemaException {
		if (xnode instanceof PrimitiveXNode<?>) {
			return parsePrismPropertyRealValue((PrimitiveXNode<T>)xnode, propertyDef);
		} else if (xnode instanceof MapXNode) {
			return parsePrismPropertyRealValue((MapXNode)xnode, propertyDef);
		} else {
			throw new IllegalArgumentException("Cannot parse property value from "+xnode);
		}
	}
	
	public <T> T parsePrismPropertyRealValue(PrimitiveXNode<T> xprim, PrismPropertyDefinition<T> propertyDefinition) throws SchemaException {
		QName typeName = propertyDefinition.getTypeName();
		if (!xprim.isParsed()) {
			xprim.parseValue(typeName);
		}
		T realValue = xprim.getValue();
		if (realValue != null && realValue instanceof PolyStringType) {
			PolyStringType polyStringType = (PolyStringType)realValue;
			realValue = (T) new PolyString(polyStringType.getOrig(), polyStringType.getNorm());
		}
		if (realValue != null) {
			PrismUtil.recomputeRealValue(realValue, prismContext);
		}
		return realValue;
	}
	
	public <T> T parsePrismPropertyRealValue(MapXNode xmap, PrismPropertyDefinition<T> propertyDefinition) throws SchemaException {
		// TODO: polystring
		return null;
	}
	
	private <T> PrismProperty<T> parsePrismPropertyRaw(XNode xnode, QName itemName)
			throws SchemaException {
		if (xnode instanceof ListXNode) {
			return parsePrismPropertyRaw((ListXNode)xnode, itemName);
		} else {
			PrismProperty<T> property = new PrismProperty<T>(itemName);
			PrismPropertyValue<T> pval = PrismPropertyValue.createRaw(xnode);
			property.add(pval);
			return property;
		}
	}
	
	private <T> PrismProperty<T> parsePrismPropertyRaw(ListXNode xlist, QName itemName)
			throws SchemaException {
		PrismProperty<T> property = new PrismProperty<T>(itemName);
		for (XNode xsubnode : xlist) {
			PrismPropertyValue<T> pval = PrismPropertyValue.createRaw(xsubnode);
			property.add(pval);
		}
		return property;
	}
	
	public PrismReference parsePrismReference(XNode xnode, QName itemName,
			PrismReferenceDefinition referenceDefinition) throws SchemaException {
		if (xnode instanceof ListXNode) {
			return parsePrismReference((ListXNode)xnode, itemName, referenceDefinition);
		} else if (xnode instanceof MapXNode) {
			return parsePrismReference((MapXNode)xnode, itemName, referenceDefinition);
		} else {
			throw new IllegalArgumentException("Cannot parse reference from "+xnode);
		}
	}
	
	public PrismReference parsePrismReference(ListXNode xlist, QName itemName,
			PrismReferenceDefinition referenceDefinition) throws SchemaException {
		if (xlist == null || xlist.isEmpty()) {
			return null;
		}
		PrismReference ref = referenceDefinition.instantiate();

		if (!referenceDefinition.isMultiValue() && xlist.size() > 1) {
			throw new SchemaException("Attempt to store multiple values in single-valued reference " + itemName);
		}

		for (XNode subnode : xlist) {
			if (itemName.equals(referenceDefinition.getName())) {
				// This is "real" reference (oid type and nothing more)
				ref.add(parseReferenceValue(subnode));
			} else {
				// This is a composite object (complete object stored inside
				// reference)
				ref.add(parseReferenceAsCompositeObject(subnode, referenceDefinition));
			}
		}
		return ref;
	}

	public PrismReference parsePrismReference(MapXNode xmap, QName itemName,
			PrismReferenceDefinition referenceDefinition) throws SchemaException {
		PrismReference ref = referenceDefinition.instantiate();
		if (itemName.equals(referenceDefinition.getName())) {
			// This is "real" reference (oid type and nothing more)
			ref.add(parseReferenceValue(xmap));
		} else {
			// This is a composite object (complete object stored inside
			// reference)
			ref.add(parseReferenceAsCompositeObject(xmap, referenceDefinition));
		}
		return ref;
	}

	public PrismReferenceValue parseReferenceValue(XNode xnode) throws SchemaException {
		if (xnode instanceof MapXNode) {
			return parseReferenceValue((MapXNode)xnode);
		} else {
			throw new IllegalArgumentException("Cannot parse reference from "+xnode);
		}
	}
	
	public PrismReferenceValue parseReferenceValue(MapXNode xmap) throws SchemaException {
		String oid = getOid(xmap);
		PrismReferenceValue refVal = new PrismReferenceValue(oid);

		QName type = getValue(xmap, XNode.KEY_REFERENCE_TYPE, DOMUtil.XSD_QNAME);
		refVal.setTargetType(type);

		QName relationAttribute = getValue(xmap, XNode.KEY_REFERENCE_RELATION, DOMUtil.XSD_QNAME);
		refVal.setRelation(relationAttribute);

		refVal.setDescription((String) getValue(xmap, XNode.KEY_REFERENCE_DESCRIPTION, DOMUtil.XSD_STRING));

		refVal.setFilter(parseFilter(xmap.get(XNode.KEY_REFERENCE_FILTER)));

		return refVal;
	}

	private PrismReferenceValue parseReferenceAsCompositeObject(XNode xnode,
			PrismReferenceDefinition referenceDefinition) throws SchemaException {
		if (xnode instanceof MapXNode) {
			return parseReferenceAsCompositeObject((MapXNode)xnode, referenceDefinition);
		} else {
			throw new IllegalArgumentException("Cannot parse reference composite object from "+xnode);
		}
	}
	
	private PrismReferenceValue parseReferenceAsCompositeObject(MapXNode xmap,
			PrismReferenceDefinition referenceDefinition) throws SchemaException {
		QName targetTypeName = referenceDefinition.getTargetTypeName();
		PrismObjectDefinition<Objectable> objectDefinition = null;
		if (xmap.getTypeQName() != null) {
			objectDefinition = getSchemaRegistry().findObjectDefinitionByType(xmap.getTypeQName());
		}		
		if (objectDefinition == null && targetTypeName != null) {
			objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(targetTypeName);
		}
        if (objectDefinition == null) {
        	throw new SchemaException("No object definition for composite object in reference element "
					+ referenceDefinition.getCompositeObjectElementName());
        }

		PrismObject<Objectable> compositeObject = null;
		try {
                compositeObject = parseObject(xmap, objectDefinition);
		} catch (SchemaException e) {
			throw new SchemaException(e.getMessage() + " while parsing composite object in reference element "
					+ referenceDefinition.getCompositeObjectElementName(), e);
		}

		PrismReferenceValue refVal = new PrismReferenceValue();
		refVal.setObject(compositeObject);
		return refVal;
	}

	private ObjectFilter parseFilter(XNode xNode) {
		// TODO
		return null;
	}
	
	private String getOid(MapXNode xmap) throws SchemaException {
		return getValue(xmap, XNode.KEY_OID, DOMUtil.XSD_STRING);
	}
	
	private String getVersion(MapXNode xmap) throws SchemaException {
		return getValue(xmap, XNode.KEY_VERSION, DOMUtil.XSD_STRING);
	}

	private Long getContainerId(MapXNode xmap) throws SchemaException {
		return getValue(xmap, XNode.KEY_CONTAINER_ID, DOMUtil.XSD_LONG);
	}
}
