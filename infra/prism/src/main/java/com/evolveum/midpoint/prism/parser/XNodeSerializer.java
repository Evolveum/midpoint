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

import com.evolveum.prism.xml.ns._public.query_2.SearchFilterType;
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
import com.evolveum.prism.xml.ns._public.types_2.EncryptedDataType;
import com.evolveum.prism.xml.ns._public.types_2.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_2.ProtectedDataType;
import com.evolveum.prism.xml.ns._public.types_2.SchemaDefinitionType;

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
            // HACK. Ugly hack. We need to make sure that the bean converter has a prism context.
            // If it does not then it cannot serialize any values and the subsequent calls may fail.
            // The bean converter usually has a context. The context may be missing if it was initialized
            // inside one of the JAXB getters/setters.
            // We need to get rid of JAXB entirelly to get rid of hacks like this
            PrismContext context = null;
            if (definition != null) {
                context = definition.getPrismContext();
            }
            if (context == null && itemValue.getParent() != null) {
                context = itemValue.getParent().getPrismContext();
            }
            if (context == null) {
                throw new SystemException("Cannot determine prism context when serializing "+itemValue);
            }
            beanConverter.setPrismContext(context);
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
            xmap.put(XNode.KEY_REFERENCE_DESCRIPTION, createPrimitiveXNode(description, DOMUtil.XSD_STRING));
        }
        SearchFilterType filter = value.getFilter();
        if (filter != null) {
            XNode xsubnode = filter.serializeToXNode(value.getPrismContext());
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
    //endregion

    //region Serializing properties - specific functionality
    private <T> XNode serializePropertyValue(PrismPropertyValue<T> value, PrismPropertyDefinition<T> definition) throws SchemaException {
        QName typeQName = definition.getTypeName();
        T realValue = value.getValue();
        if (realValue instanceof SchemaDefinitionType) {
            return serializeSchemaDefinition((SchemaDefinitionType)realValue);
        } else if (realValue instanceof ProtectedDataType<?>) {
            MapXNode xProtected = serializeProtectedDataType((ProtectedDataType<?>) realValue);
            if (definition.isDynamic()){
                xProtected.setExplicitTypeDeclaration(true);
                xProtected.setTypeQName(definition.getTypeName());
            }
            return xProtected;
        } else if (realValue instanceof PolyString) {
            return serializePolyString((PolyString) realValue);
        } else if (realValue instanceof ItemPathType){
            return serializeItemPathType((ItemPathType) realValue);
        } else if (beanConverter.canProcess(typeQName)) {
            return beanConverter.marshall(realValue);
        } else {
            // primitive value
            return createPrimitiveXNode(realValue, typeQName);
        }
    }

    private XNode serializeItemPathType(ItemPathType itemPath) {
        PrimitiveXNode<ItemPath> xprim = new PrimitiveXNode<ItemPath>();
        if (itemPath != null){
            ItemPath path = itemPath.getItemPath();
            xprim.setValue(path);
            xprim.setTypeQName(ItemPath.XSD_TYPE);
        }
        return xprim;
    }

    private XNode serializePolyString(PolyString realValue) {
        PrimitiveXNode<PolyString> xprim = new PrimitiveXNode<>();
        xprim.setValue(realValue);
        xprim.setTypeQName(PolyStringType.COMPLEX_TYPE);
        return xprim;
    }

    // TODO create more appropriate interface to be able to simply serialize ProtectedStringType instances
    public <T> MapXNode serializeProtectedDataType(ProtectedDataType<T> protectedType) throws SchemaException {
        MapXNode xmap = new MapXNode();
        if (protectedType.getEncryptedDataType() != null) {
            EncryptedDataType encryptedDataType = protectedType.getEncryptedDataType();
            MapXNode xEncryptedDataType = (MapXNode) beanConverter.marshall(encryptedDataType);
            xmap.put(ProtectedDataType.F_ENCRYPTED_DATA, xEncryptedDataType);
        } else if (protectedType.getClearValue() != null){
            QName type = XsdTypeMapper.toXsdType(protectedType.getClearValue().getClass());
            PrimitiveXNode xClearValue = createPrimitiveXNode(protectedType.getClearValue(), type);
            xmap.put(ProtectedDataType.F_CLEAR_VALUE, xClearValue);
        }
        // TODO: clearValue
        return xmap;
    }

    private XNode serializeSchemaDefinition(SchemaDefinitionType schemaDefinitionType) {
        SchemaXNode xschema = new SchemaXNode();
        xschema.setSchemaElement(schemaDefinitionType.getSchema());
        MapXNode xmap = new MapXNode();
        xmap.put(DOMUtil.XSD_SCHEMA_ELEMENT,xschema);
        return xmap;
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
