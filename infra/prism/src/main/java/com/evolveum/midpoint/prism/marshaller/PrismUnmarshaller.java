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

import java.util.Collection;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.SchemaXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrismUnmarshaller {

    private static final Trace LOGGER = TraceManager.getTrace(PrismUnmarshaller.class);

    public static final QName ARTIFICIAL_OBJECT_NAME = new QName(XMLConstants.NULL_NS_URI, "anObject");

    @NotNull
	private PrismContext prismContext;

	public PrismUnmarshaller(@NotNull PrismContext prismContext) {
		this.prismContext = prismContext;
	}

    //region Public interface ========================================================

    /*
     *  Please note: methods in this section should NOT be called from inside of parsing process!
     *  It is to avoid repeatedly calling ItemInfo.determine, if at all possible.
     *  (An exception is only if we know we have the definition ... TODO ...)
     */
    @SuppressWarnings("unchecked")
    <O extends Objectable> PrismObject<O> parseObject(@NotNull RootXNode root, ItemDefinition<?> itemDefinition, QName itemName,
            QName typeName, Class<?> typeClass, @NotNull ParsingContext pc) throws SchemaException {
        ItemInfo itemInfo = ItemInfo.determine(itemDefinition,
                root.getRootElementName(), itemName, ARTIFICIAL_OBJECT_NAME,
                root.getTypeQName(), typeName,
                typeClass, PrismObjectDefinition.class, pc, getSchemaRegistry());

        XNode child = root.getSubnode();
        if (!(child instanceof MapXNode)) {
            throw new IllegalArgumentException("Cannot parse object from " + child.getClass().getSimpleName() + ", we need a map");
        }
        return (PrismObject<O>) (Item) parseItemInternal(child, itemInfo.getItemName(), itemInfo.getItemDefinition(), pc);
    }

    @SuppressWarnings("unchecked")
    <O extends Objectable> PrismObject<O> parseObject(MapXNode map, PrismObjectDefinition<O> objectDefinition, ParsingContext pc) throws SchemaException {
        ItemInfo itemInfo = ItemInfo.determine(objectDefinition,
                null, null, ARTIFICIAL_OBJECT_NAME,
                map.getTypeQName(), null,
                null, PrismObjectDefinition.class, pc, getSchemaRegistry());
        return (PrismObject<O>) (Item) parseItemInternal(map, itemInfo.getItemName(), itemInfo.getItemDefinition(), pc);
    }

    @SuppressWarnings("unchecked")
    Item<?, ?> parseItem(@NotNull RootXNode root,
            ItemDefinition<?> itemDefinition, QName itemName, QName typeName, Class<?> typeClass,
            @NotNull ParsingContext pc) throws SchemaException {

        ItemInfo itemInfo = ItemInfo.determine(itemDefinition,
                root.getRootElementName(), itemName, ARTIFICIAL_OBJECT_NAME,
                root.getTypeQName(), typeName,
                typeClass, ItemDefinition.class, pc, getSchemaRegistry());
        ItemDefinition realDefinition;
        if (itemInfo.getItemDefinition() == null && itemInfo.getComplexTypeDefinition() != null) {
            // let's create container definition dynamically
            PrismContainerDefinitionImpl pcd = new PrismContainerDefinitionImpl(itemInfo.getItemName(), itemInfo.getComplexTypeDefinition(),
                    prismContext);
            pcd.setDynamic(true);       // questionable
            realDefinition = pcd;
        } else {
            realDefinition = itemInfo.getItemDefinition();
        }
        return parseItemInternal(root.getSubnode(), itemInfo.getItemName(), realDefinition, pc);
    }

    Object parseItemOrRealValue(@NotNull RootXNode root, ParsingContext pc) throws SchemaException {
        // is the type name explicitly specified? (if not, guess that we have a string)
        QName typeName = root.getTypeQName();
        if (typeName != null) {
            ItemDefinition itemDefinition = getSchemaRegistry().findItemDefinitionByType(typeName);
            if (itemDefinition != null) {
                return parseItem(root, itemDefinition, null, null, null, pc);
            } else {
                return getBeanUnmarshaller().unmarshal(root, getSchemaRegistry().determineCompileTimeClass(typeName), pc);
            }
        } else {
            // if type name is not known, we have to derive it from the element name
            QName itemName = root.getRootElementName();
            ItemDefinition itemDefinition = getSchemaRegistry().findItemDefinitionByElementName(itemName);
            if (itemDefinition == null) {
                throw new SchemaException("Couldn't parse general object with no type name and unknown element name: " + itemName);
            }
            return parseItem(root, itemDefinition, itemName, null, null, pc);
        }
    }
    //endregion

    //region Private methods ========================================================

    // The situation of itemDefinition == null && node.typeName != null is allowed ONLY if the definition cannot be derived
    // from the typeName. E.g. if typeName is like xsd:string, xsd:boolean, etc. This rule is because we don't want to repeatedly
    // try to look for missing definitions here.
    //
    // So the caller is responsible for extracting information from node.typeQName - providing a definition if possible.
    @SuppressWarnings("unchecked")
    @NotNull
    private Item<?, ?> parseItemInternal(@NotNull XNode node,
            @NotNull QName itemName, ItemDefinition itemDefinition, @NotNull ParsingContext pc) throws SchemaException {
        Validate.isTrue(!(node instanceof RootXNode));

        // TODO execute this only if in checked mode
        if (itemDefinition == null && node.getTypeQName() != null) {
            PrismContainerDefinition<?> pcd = getSchemaRegistry().findContainerDefinitionByType(node.getTypeQName());
            if (pcd != null) {
                throw new IllegalStateException("Node has an explicit type corresponding to container (" + pcd
                        + ") but parseItemInternal was called without definition: " + node.debugDump());
            }
        }

        if (itemDefinition == null || itemDefinition instanceof PrismPropertyDefinition) {
            return parseProperty(node, itemName, (PrismPropertyDefinition) itemDefinition, pc);
        } else if (itemDefinition instanceof PrismContainerDefinition) {    // also objects go here
            return parseContainer(node, itemName, (PrismContainerDefinition<?>) itemDefinition, pc);
        } else if (itemDefinition instanceof PrismReferenceDefinition) {
            return parseReference(node, itemName, (PrismReferenceDefinition) itemDefinition, pc);
        } else {
            throw new IllegalArgumentException("Attempt to parse unknown definition type " + itemDefinition.getClass().getName());
        }
    }

    @NotNull
    private <C extends Containerable> PrismContainer<C> parseContainer(@NotNull XNode node, @NotNull QName itemName,
            @NotNull PrismContainerDefinition<C> containerDef, @NotNull ParsingContext pc) throws SchemaException {
        PrismContainer<C> container = containerDef.instantiate(itemName);
        if (node instanceof ListXNode) {
            for (XNode subNode : (ListXNode) node) {
                container.add(parseContainerValue(subNode, containerDef, pc));
            }
        } else {
            container.add(parseContainerValue(node, containerDef, pc));
            if (node instanceof MapXNode && container instanceof PrismObject) {
                MapXNode map = (MapXNode) node;
                PrismObject object = (PrismObject) container;
                object.setOid(getOid(map));
                object.setVersion(getVersion(map));
            }
        }
        return container;
    }

    private String getOid(MapXNode xmap) throws SchemaException {
        return xmap.getParsedPrimitiveValue(XNode.KEY_OID, DOMUtil.XSD_STRING);
    }

    private String getVersion(MapXNode xmap) throws SchemaException {
        return xmap.getParsedPrimitiveValue(XNode.KEY_VERSION, DOMUtil.XSD_STRING);
    }

    private Long getContainerId(MapXNode xmap) throws SchemaException {
        return xmap.getParsedPrimitiveValue(XNode.KEY_CONTAINER_ID, DOMUtil.XSD_LONG);
    }

    private <C extends Containerable> PrismContainerValue<C> parseContainerValue(@NotNull XNode node,
            @NotNull PrismContainerDefinition<C> containerDef, @NotNull ParsingContext pc) throws SchemaException {
        if (node instanceof MapXNode) {
            return parseContainerValueFromMap((MapXNode) node, containerDef, pc);
        } else if (node instanceof PrimitiveXNode) {
            PrimitiveXNode<?> prim = (PrimitiveXNode<?>) node;
            if (prim.isEmpty()) {
                return containerDef.createValue();
            } else {
                throw new IllegalArgumentException("Cannot parse container value from (non-empty) " + node);
            }
        } else {
            throw new IllegalArgumentException("Cannot parse container value from " + node);
        }
    }

    private <C extends Containerable> PrismContainerValue<C> parseContainerValueFromMap(@NotNull MapXNode map,
            @NotNull PrismContainerDefinition<C> containerDef, @NotNull ParsingContext pc) throws SchemaException {
        Long id = getContainerId(map);

        // override container definition, if explicit type is specified
        ComplexTypeDefinition complexTypeDefinition = containerDef.getComplexTypeDefinition();
        if (map.getTypeQName() != null) {
            ComplexTypeDefinition specificDef = getSchemaRegistry().findComplexTypeDefinitionByType(map.getTypeQName());
            if (specificDef != null) {
                complexTypeDefinition = specificDef;
            } else {
                pc.warnOrThrow(LOGGER, "Unknown type " + map.getTypeQName() + " in " + map);
            }
        }
        PrismContainerValue<C> cval = new PrismContainerValue<>(null, null, null, id, complexTypeDefinition, prismContext);
        for (Entry<QName, XNode> entry : map.entrySet()) {
            QName itemName = entry.getKey();
            if (itemName == null) {
                throw new IllegalArgumentException("Null item name while parsing " + map.debugDump());
            }
            if (QNameUtil.match(itemName, XNode.KEY_CONTAINER_ID)) {
                continue;
            }
            if (containerDef instanceof PrismObjectDefinition &&
                    (QNameUtil.match(itemName, XNode.KEY_OID) || QNameUtil.match(itemName, XNode.KEY_VERSION))) {
                continue;
            }
            ItemDefinition itemDef = locateItemDefinition(itemName, complexTypeDefinition, entry.getValue());

            if (itemDef == null) {
                if (complexTypeDefinition == null || complexTypeDefinition.isXsdAnyMarker() || complexTypeDefinition.isRuntimeSchema()) {
                    PrismSchema itemSchema = getSchemaRegistry().findSchemaByNamespace(itemName.getNamespaceURI());
                    if (itemSchema != null) {
                        // If we already have schema for this namespace then a missing element is
                        // an error. We positively know that it is not in the schema.
                        pc.warnOrThrow(LOGGER, "Item " + itemName + " has no definition (schema present, in container "
                                + containerDef + ")" + "while parsing " + map.debugDump());
                    } else {
                        // No definition for item, but the schema is runtime. the definition may come later.
                        // Null is OK here. The item will be parsed as "raw"
                    }
                } else {    // complex type definition is static
                    pc.warnOrThrow(LOGGER, "Item " + itemName + " has no definition (in container value "
                            + complexTypeDefinition + ")" + "while parsing " + map.debugDump());
                    continue;   // don't even attempt to parse it
                }
            }
            Item<?, ?> item;
            if (entry.getValue() == null) {
                if (itemDef != null) {
                    item = itemDef.instantiate();       // TODO or skip the creation altogether?
                } else {
                    item = null;
                }
            } else {
                item = parseItemInternal(entry.getValue(), itemName, itemDef, pc);
            }
            // Merge must be here, not just add. Some items (e.g. references) have alternative
            // names and representations and these cannot be processed as one map or list
            if (item != null) {
                cval.merge(item);
            }
        }
        return cval;
    }

    @NotNull
    private <T> PrismProperty<T> parseProperty(@NotNull XNode node, @NotNull QName itemName,
            @Nullable PrismPropertyDefinition<T> itemDefinition, @NotNull ParsingContext pc) throws SchemaException {
        Validate.isTrue(!(node instanceof RootXNode));

        PrismProperty<T> property = itemDefinition != null ?
                itemDefinition.instantiate() :
                new PrismProperty<T>(itemName, prismContext);

        if (node instanceof ListXNode) {
            ListXNode listNode = (ListXNode) node;
            if (itemDefinition != null && !itemDefinition.isMultiValue() && listNode.size() > 1) {
                throw new SchemaException("Attempt to store multiple values in single-valued property " + itemName);
            }
            for (XNode subNode : listNode) {
                PrismPropertyValue<T> pval = parsePropertyValue(subNode, itemDefinition, pc);
                if (pval != null) {
                    property.add(pval);
                }
            }
        } else if (node instanceof MapXNode || node instanceof PrimitiveXNode) {
            PrismPropertyValue<T> pval = parsePropertyValue(node, itemDefinition, pc);
            if (pval != null) {
                property.add(pval);
            }
        } else if (node instanceof SchemaXNode) {
            SchemaDefinitionType schemaDefType = getBeanUnmarshaller().unmarshalSchemaDefinitionType((SchemaXNode) node);
            @SuppressWarnings("unchecked")
            PrismPropertyValue<T> val = new PrismPropertyValue(schemaDefType);
            property.add(val);
        } else {
            throw new IllegalArgumentException("Cannot parse property from " + node);
        }
        return property;
    }

    // if definition == null or any AND node has type defined, this type must be non-containerable (fit into PPV)
    private <T> PrismPropertyValue<T> parsePropertyValue(@NotNull XNode node,
            @Nullable PrismPropertyDefinition<T> definition, @NotNull ParsingContext pc) throws SchemaException {

        if (definition == null || definition.isAnyType()) {
            if (node.getTypeQName() == null) {
                return PrismPropertyValue.createRaw(node);
            }
            // TODO FIX/TEST THIS UGLY HACK
            if (node instanceof PrimitiveXNode) {
                PrimitiveXNode prim = (PrimitiveXNode) node;
                prim.parseValue(node.getTypeQName(), pc.getEvaluationMode());
                if (prim.getValue() != null) {
                    return new PrismPropertyValue<>((T) prim.getValue());
                } else {
                    return null;
                }
            } else if (node instanceof MapXNode) {
                if (getBeanUnmarshaller().canProcess(node.getTypeQName())) {
                    T value = getBeanUnmarshaller().unmarshal((MapXNode) node, node.getTypeQName(), pc);
                    if (value instanceof Containerable) {
                        throw new IllegalStateException("Cannot store containerable into prism property: " + node.debugDump());
                    } else {
                        return new PrismPropertyValue<>(value);
                    }
                } else {
                    // TODO or should treat this elsewhere?
                    throw new IllegalStateException("Cannot parse as " + node.getTypeQName() + ": " + node.debugDump());
                }
            } else {
                throw new IllegalArgumentException("Unexpected node: " + node.debugDump());
            }
        }

        T realValue;
        if (node instanceof PrimitiveXNode<?>) {
            @SuppressWarnings("unchecked")
            PrimitiveXNode<T> primitiveNode = (PrimitiveXNode<T>) node;
            realValue = parsePropertyRealValueFromPrimitive(primitiveNode, definition, pc);
        } else if (node instanceof MapXNode) {
            realValue = parsePropertyRealValueFromMap((MapXNode) node, definition, pc);
        } else {
            throw new IllegalArgumentException("Cannot parse property value from " + node.debugDump());
        }
        return realValue != null ? new PrismPropertyValue<T>(realValue) : null;
    }

    private <T> T parsePropertyRealValueFromPrimitive(@NotNull PrimitiveXNode<T> primitiveNode,
            @NotNull PrismPropertyDefinition<T> definition, @NotNull ParsingContext pc) throws SchemaException {

        QName typeName = definition.getTypeName();
        T realValue;
        if (getBeanUnmarshaller().canProcess(typeName)) {
            // Primitive elements may also have complex Java representations (e.g. enums)
            realValue = getBeanUnmarshaller().unmarshalFromPrimitive(primitiveNode, typeName, pc);
        } else if (!DOMUtil.XSD_ANYTYPE.equals(typeName)) {
            try {
                realValue = primitiveNode.getParsedValue(typeName, pc.getEvaluationMode());
            } catch (SchemaException e) {
                pc.warnOrThrow(LOGGER, "Couldn't parse primitive value of type " + typeName + ". Value: " + primitiveNode.getStringValue()
                        + ".\nDefinition: " + definition.debugDump(), e);
                return null;
            }
        } else {
            realValue = (T) RawType.create(primitiveNode, prismContext);
        }

        if (!(realValue instanceof RawType) && !isValueAllowed(realValue, definition.getAllowedValues())) {
            pc.warnOrThrow(LOGGER, "Skipping unknown value of type " + typeName + ". Value: " + primitiveNode.getStringValue());
            return null;
        }

        if (realValue == null) {
            return null;
        } else if (realValue instanceof PolyStringType) {
            PolyStringType polyStringType = (PolyStringType) realValue;
            realValue = (T) new PolyString(polyStringType.getOrig(), polyStringType.getNorm());
        } else if (realValue instanceof String && typeName.equals(PolyStringType.COMPLEX_TYPE)) {
            String val = (String) realValue;
            realValue = (T) new PolyString(val);
        }

        PrismUtil.recomputeRealValue(realValue, prismContext);
        return realValue;
    }

    private <T> T parsePropertyRealValueFromMap(@NotNull MapXNode xmap, @NotNull PrismPropertyDefinition<T> propertyDefinition,
            @NotNull ParsingContext pc)
            throws SchemaException {
        QName typeName = propertyDefinition.getTypeName();
        if (getBeanUnmarshaller().canProcess(typeName)) {
            return getBeanUnmarshaller().unmarshal(xmap, typeName, pc);
        } else {
            if (propertyDefinition.isRuntimeSchema()) {
				throw new SchemaException("Complex run-time properties are not supported: type " + typeName + " from " + xmap);
			} else {
				throw new SystemException("Cannot parse compile-time property " + propertyDefinition.getName() + " type " + typeName + " from " + xmap);
			}
        }
    }

    private <T> boolean isValueAllowed(T realValue, Collection<? extends DisplayableValue<T>> collection) {
        if (CollectionUtils.isEmpty(collection)) {
            return true;
        }
        for (DisplayableValue<T> o : collection) {
            if (realValue.equals(o.getValue())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private PrismReference parseReference(@NotNull XNode node, @NotNull QName itemName,
            @NotNull PrismReferenceDefinition definition, @NotNull ParsingContext pc) throws SchemaException {
        PrismReference ref = definition.instantiate();
        if (node instanceof ListXNode) {
            ListXNode listNode = (ListXNode) node;
            if (!definition.isMultiValue() && listNode.size() > 1) {
                throw new SchemaException("Attempt to store multiple values in single-valued reference " + itemName);
            }
            for (XNode subNode : listNode) {
                ref.add(parseReferenceValueFromXNode(subNode, definition, itemName, pc));
            }
        } else if (node instanceof MapXNode) {
            ref.add(parseReferenceValueFromXNode(node, definition, itemName, pc));
        } else if (node instanceof PrimitiveXNode) {
            // empty
        } else {
            throw new IllegalArgumentException("Cannot parse reference from " + node);
        }
        return ref;
    }

    @NotNull
    private PrismReferenceValue parseReferenceValueFromXNode(@NotNull XNode node,
            @NotNull PrismReferenceDefinition definition, @NotNull QName itemName, @NotNull ParsingContext pc) throws SchemaException {
        /*
         *  We distinguish between "real" references and composite objects by
         *  (1) looking at type QName of XNode passed (whether it's ObjectType or ObjectReferenceType)
         *  (2) comparing itemName and name from reference definition - e.g. linkRef vs. link
         */
        boolean isComposite;
        if (node.getTypeQName() != null) {
            QName typeName = node.getTypeQName();
            ItemDefinition contentDefinition = getSchemaRegistry().findItemDefinitionByType(typeName);
            isComposite = contentDefinition instanceof PrismObjectDefinition;
        } else {
            isComposite = !QNameUtil.match(itemName, definition.getName());
        }

        if (isComposite) {
            return parseReferenceValueAsCompositeObject(node, definition, pc);  // This is a composite object (complete object stored inside reference)
        } else {
            return parseReferenceValueAsReference(node, definition, pc);   // This is "real" reference (oid,  and nothing more)
        }
    }

    private PrismReferenceValue parseReferenceValueAsReference(@NotNull XNode xnode, @NotNull PrismReferenceDefinition definition,
            @NotNull ParsingContext pc) throws SchemaException {
        if (!(xnode instanceof MapXNode)) {
            throw new IllegalArgumentException("Cannot parse reference from " + xnode);
        }
        MapXNode map = (MapXNode) xnode;

        String oid = map.getParsedPrimitiveValue(XNode.KEY_REFERENCE_OID, DOMUtil.XSD_STRING);
        PrismReferenceValue refVal = new PrismReferenceValue(oid);

        QName type = map.getParsedPrimitiveValue(XNode.KEY_REFERENCE_TYPE, DOMUtil.XSD_QNAME);
        if (type == null) {
			if (!pc.isAllowMissingRefTypes()) {
				type = definition.getTargetTypeName();
				if (type == null) {
					throw new SchemaException("Target type specified neither in reference nor in the schema");
				}
			}
        } else {
            if (QNameUtil.noNamespace(type)) {
                type = getSchemaRegistry().resolveUnqualifiedTypeName(type);
            }
            QName defTargetType = definition.getTargetTypeName();
            if (defTargetType != null) {
                if (!(prismContext.getSchemaRegistry().isAssignableFrom(defTargetType, type))) {
                    throw new SchemaException("Target type specified in reference (" + type
                            + ") does not match target type in schema (" + defTargetType + ")");
                }
            }
        }
		PrismObjectDefinition<Objectable> objectDefinition = null;
		if (type != null) {
			objectDefinition = getSchemaRegistry().findObjectDefinitionByType(type);
			if (objectDefinition == null) {
				throw new SchemaException("No definition for type " + type + " in reference");
			}
			refVal.setTargetType(type);
		}

        QName relationAttribute = map.getParsedPrimitiveValue(XNode.KEY_REFERENCE_RELATION, DOMUtil.XSD_QNAME);
        refVal.setRelation(relationAttribute);

        refVal.setDescription(map.getParsedPrimitiveValue(XNode.KEY_REFERENCE_DESCRIPTION, DOMUtil.XSD_STRING));

        refVal.setFilter(parseFilter(map.get(XNode.KEY_REFERENCE_FILTER), pc));
        
        String resolutionTimeString = map.getParsedPrimitiveValue(XNode.KEY_REFERENCE_RESOLUTION_TIME, DOMUtil.XSD_STRING);
        if (resolutionTimeString != null) {
        	EvaluationTimeType resolutionTime = EvaluationTimeType.fromValue(resolutionTimeString);
        	refVal.setResolutionTime(resolutionTime);
        }

        XNode xnodeForTargetName = map.get(XNode.KEY_REFERENCE_TARGET_NAME);
        if (xnodeForTargetName != null) {
            PolyStringType targetName = getBeanUnmarshaller().unmarshal(xnodeForTargetName, PolyStringType.class, pc);
            refVal.setTargetName(targetName);
        }

        XNode xrefObject = map.get(XNode.KEY_REFERENCE_OBJECT);
        if (xrefObject != null) {
            if (!(xrefObject instanceof MapXNode)) {
                throw new SchemaException("Cannot parse object from " + xrefObject);
            }
			if (type == null) {
				throw new SchemaException("Cannot parse object from " + xrefObject + " without knowing its type");
			}
            PrismObject<Objectable> object = parseObject((MapXNode) xrefObject, objectDefinition, pc);
            setReferenceObject(refVal, object);
        }

        return refVal;
    }

    private void setReferenceObject(PrismReferenceValue refVal, PrismObject<Objectable> object) throws SchemaException {
        refVal.setObject(object);
        if (object.getOid() != null) {
            if (refVal.getOid() == null) {
                refVal.setOid(object.getOid());
            } else {
                if (!refVal.getOid().equals(object.getOid())) {
                    throw new SchemaException("OID in reference (" + refVal.getOid() + ") does not match OID in composite object (" + object.getOid() + ")");
                }
            }
        }
        QName objectTypeName = object.getDefinition().getTypeName();
        if (refVal.getTargetType() == null) {
            refVal.setTargetType(objectTypeName);
        } else {
            if (!refVal.getTargetType().equals(objectTypeName)) {
                throw new SchemaException("Target type in reference (" + refVal.getTargetType() + ") does not match type in composite object (" + objectTypeName + ")");
            }
        }
    }

    private PrismReferenceValue parseReferenceValueAsCompositeObject(XNode node,
            PrismReferenceDefinition definition, ParsingContext pc) throws SchemaException {
        if (!(node instanceof MapXNode)) {
            throw new IllegalArgumentException("Cannot parse reference composite object from " + node);
        }
        MapXNode map = (MapXNode) node;
        QName targetTypeName = definition.getTargetTypeName();
        PrismObjectDefinition<Objectable> objectDefinition = null;
        if (map.getTypeQName() != null) {
            objectDefinition = getSchemaRegistry().findObjectDefinitionByType(map.getTypeQName());
        }
        if (objectDefinition == null && targetTypeName != null) {
            objectDefinition = getSchemaRegistry().findObjectDefinitionByType(targetTypeName);
        }
        if (objectDefinition == null) {
            throw new SchemaException("No object definition for composite object in reference element "
                    + definition.getCompositeObjectElementName());
        }

        PrismObject<Objectable> compositeObject;
        try {
            compositeObject = parseObject(map, objectDefinition, pc);
        } catch (SchemaException e) {
            throw new SchemaException(e.getMessage() + " while parsing composite object in reference element "
                    + definition.getCompositeObjectElementName(), e);
        }

        PrismReferenceValue refVal = new PrismReferenceValue();
        setReferenceObject(refVal, compositeObject);
        ((PrismReferenceDefinitionImpl) definition).setComposite(true);
        return refVal;
    }

    private SearchFilterType parseFilter(XNode xnode, ParsingContext pc) throws SchemaException {
        if (xnode == null) {
            return null;
        }
        if (xnode.isEmpty()) {
            return null;
        }
        return SearchFilterType.createFromXNode(xnode, prismContext);
    }

    private <T extends Containerable> ItemDefinition locateItemDefinition(@NotNull QName itemName,
            @Nullable ComplexTypeDefinition complexTypeDefinition,
            XNode xnode) throws SchemaException {
        return getSchemaRegistry()
                .locateItemDefinition(itemName, complexTypeDefinition, qName -> resolveDynamicItemDefinition(qName, xnode));
    }

    private ItemDefinition resolveDynamicItemDefinition(QName itemName, XNode node)  {
        if (node == null) {
            return null;
        }
        QName typeName = node.getTypeQName();
        if (typeName == null) {
            if (node instanceof ListXNode) {
                // there may be type definitions in individual list members
                for (XNode subNode : ((ListXNode) node)) {
                    ItemDefinition subdef = resolveDynamicItemDefinition(itemName, subNode);
                    // TODO: make this smarter, e.g. detect conflicting type definitions
                    if (subdef != null) {
                        return subdef;
                    }
                }
            }
        }
        if (typeName == null) {
            return null;
        }

        PrismPropertyDefinitionImpl propDef = new PrismPropertyDefinitionImpl(itemName, typeName, prismContext);
        Integer maxOccurs = node.getMaxOccurs();
        if (maxOccurs != null) {
            propDef.setMaxOccurs(maxOccurs);
        } else {
            // Make this multivalue by default, this is more "open"
            propDef.setMaxOccurs(-1);
        }
        propDef.setDynamic(true);
        return propDef;
    }

    //endregion


    //endregion
    private BeanUnmarshaller getBeanUnmarshaller() {
        return ((PrismContextImpl) prismContext).getBeanUnmarshaller();
    }

	private SchemaRegistry getSchemaRegistry() {
		return prismContext.getSchemaRegistry();
	}

	//TODO
    public <T extends Containerable> ItemDefinition locateItemDefinition(
            @NotNull PrismContainerDefinition<T> containerDefinition, @NotNull QName itemName, @Nullable XNode xnode)
            throws SchemaException {
        return locateItemDefinition(itemName, containerDefinition.getComplexTypeDefinition(), xnode);
    }


}
