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
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.Map.Entry;

public class PrismUnmarshaller {

    private static final Trace LOGGER = TraceManager.getTrace(PrismUnmarshaller.class);

    private static final QName ARTIFICIAL_OBJECT_NAME = new QName(XMLConstants.NULL_NS_URI, "anObject");

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
     *
     *  TODO migrate to parseItem eventually (now we treat objects in parseItemInternal!)
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

    // TODO migrate to parseItem eventually
    @SuppressWarnings("unchecked")
    private <O extends Objectable> PrismObject<O> parseObject(MapXNode map, PrismObjectDefinition<O> objectDefinition,
            ParsingContext pc) throws SchemaException {
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
            QName actualTypeName = itemInfo.getComplexTypeDefinition().getTypeName();
            if (getSchemaRegistry().isContainer(actualTypeName)) {      // TODO what about objects?
                PrismContainerDefinitionImpl def = new PrismContainerDefinitionImpl(itemInfo.getItemName(),
                        itemInfo.getComplexTypeDefinition(), prismContext);
                def.setDynamic(true);
                realDefinition = def;
            } else {
                PrismPropertyDefinitionImpl def = new PrismPropertyDefinitionImpl(itemInfo.getItemName(), actualTypeName, prismContext);
                def.setDynamic(true);
                realDefinition = def;
            }
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
            ListXNode list = (ListXNode) node;
            if (containerDef instanceof PrismObject && list.size() > 1) {
                pc.warnOrThrow(LOGGER, "Multiple values for a PrismObject: " + node.debugDump());
                parseContainerValueToContainer(container, list.get(0), pc);
            } else {
                for (XNode subNode : list) {
                    parseContainerValueToContainer(container, subNode, pc);
                }
            }
        } else {
            parseContainerValueToContainer(container, node, pc);
        }
        return container;
    }

    private <C extends Containerable> void parseContainerValueToContainer(PrismContainer<C> container, XNode node,
            @NotNull ParsingContext pc) throws SchemaException {
        container.add(parseContainerValue(node, container.getDefinition(), pc));
        if (node instanceof MapXNode && container instanceof PrismObject) {
            MapXNode map = (MapXNode) node;
            PrismObject object = (PrismObject) container;
            object.setOid(getOid(map));
            object.setVersion(getVersion(map));
        }
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
                pc.warnOrThrow(LOGGER, "Cannot parse container value from (non-empty) " + node);
                return containerDef.createValue();
            }
        } else {
            pc.warnOrThrow(LOGGER, "Cannot parse container value from " + node);
            return containerDef.createValue();
        }
    }

    private <C extends Containerable> PrismContainerValue<C> parseContainerValueFromMap(@NotNull MapXNode map,
            @NotNull PrismContainerDefinition<C> containerDef, @NotNull ParsingContext pc) throws SchemaException {
        Long id = getContainerId(map);

        ComplexTypeDefinition complexTypeDefinition = containerDef.getComplexTypeDefinition();

        PrismContainerValue<C> cval;
        if (containerDef instanceof PrismObjectDefinition) {
            cval = ((PrismObjectDefinition) containerDef).createValue();
        } else {
            // override container definition, if explicit type is specified
            if (map.getTypeQName() != null) {
                ComplexTypeDefinition specificDef = getSchemaRegistry().findComplexTypeDefinitionByType(map.getTypeQName());
                if (specificDef != null) {
                    complexTypeDefinition = specificDef;
                } else {
                    pc.warnOrThrow(LOGGER, "Unknown type " + map.getTypeQName() + " in " + map);
                }
            }
            cval = new PrismContainerValue<>(null, null, null, id, complexTypeDefinition, prismContext);
        }
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
                        // we can go along this item (at least show it in repository pages) - MID-3249
                        // TODO make this configurable
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
                new PrismProperty<>(itemName, prismContext);

        if (node instanceof ListXNode && !node.isHeterogeneousList()) {
            ListXNode listNode = (ListXNode) node;
            if (itemDefinition != null && !itemDefinition.isMultiValue() && listNode.size() > 1) {
                throw new SchemaException("Attempt to store multiple values in single-valued property " + itemName);
            }
            for (XNode subNode : listNode) {
                PrismPropertyValue<T> pval = parsePropertyValue(subNode, itemDefinition, pc);
                addItemValueIfPossible(property, pval, pc);
            }
        } else if (node instanceof MapXNode || node instanceof PrimitiveXNode || node.isHeterogeneousList()) {
            PrismPropertyValue<T> pval = parsePropertyValue(node, itemDefinition, pc);
            if (pval != null) {
                property.add(pval);
            }
        } else if (node instanceof SchemaXNode) {
            SchemaDefinitionType schemaDefType = getBeanUnmarshaller().unmarshalSchemaDefinitionType((SchemaXNode) node);
            @SuppressWarnings("unchecked")
            PrismPropertyValue<T> val = new PrismPropertyValue(schemaDefType);
			addItemValueIfPossible(property, val, pc);
        } else {
            throw new IllegalArgumentException("Cannot parse property from " + node);
        }
        return property;
    }

	private <V extends PrismValue, D extends ItemDefinition> void addItemValueIfPossible(Item<V, D> item, V value, ParsingContext pc) throws SchemaException {
		if (value != null) {
			try {
				item.add(value);
			} catch (SchemaException e) {
				pc.warnOrThrow(LOGGER, "Couldn't add a value of " + value + " to the containing item: " + e.getMessage(), e);
			}
		}
	}

	// if definition == null or any AND node has type defined, this type must be non-containerable (fit into PPV)
    private <T> PrismPropertyValue<T> parsePropertyValue(@NotNull XNode node,
            @Nullable PrismPropertyDefinition<T> definition, @NotNull ParsingContext pc) throws SchemaException {
        QName typeFromDefinition = definition != null && !definition.isAnyType() ? definition.getTypeName() : null;
        QName typeName =
                getSchemaRegistry().areComparable(typeFromDefinition, node.getTypeQName()) ?
                        getSchemaRegistry().selectMoreSpecific(typeFromDefinition, node.getTypeQName()) : null;
        if (typeName == null) {
			return createRawPrismPropertyValue(node);
        } else if (getBeanUnmarshaller().canProcess(typeName)) {
            T realValue = getBeanUnmarshaller().unmarshal(node, typeName, pc);
            // Postprocessing after returning from unmarshaller. It speaks bean language (e.g. PolyStringType, not PolyString).
            // It also doesn't know about prism-specific things like allowed values, etc.
            if (realValue instanceof PolyStringType) {
                @SuppressWarnings("unchecked")
                T valueT = (T) ((PolyStringType) realValue).toPolyString();
                realValue = valueT;
            }
            PrismUtil.recomputeRealValue(realValue, prismContext);
            if (!isValueAllowed(realValue, definition)) {
                pc.warnOrThrow(LOGGER, "Unknown (not allowed) value of type " + typeName + ". Value: " + realValue + ". Allowed values: " + definition.getAllowedValues());
                return null;
            }
            if (realValue == null) {
            	// Be careful here. Expression element can be legal sub-element of complex properties.
            	// Therefore parse expression only if there is no legal value.
            	ExpressionWrapper expression = PrismUtil.parseExpression(node, prismContext);
            	if (expression != null) {
            		PrismPropertyValue<T> ppv = new PrismPropertyValue<>(null, prismContext, null, null, expression);
            		return ppv;

            	}
            }
            PrismPropertyValue<T> ppv = new PrismPropertyValue<>(realValue);
            ppv.setPrismContext(prismContext);
            return ppv;
        } else {
        	pc.warnOrThrow(LOGGER, "Cannot parse as " + typeName + ": " + node.debugDump());
			return createRawPrismPropertyValue(node);
        }
    }

	@NotNull
	private <T> PrismPropertyValue<T> createRawPrismPropertyValue(@NotNull XNode node) {
		PrismPropertyValue<T> ppv = PrismPropertyValue.createRaw(node);
		ppv.setPrismContext(prismContext);
		return ppv;
	}

	private <T> boolean isValueAllowed(T realValue, PrismPropertyDefinition<T> definition) throws SchemaException {
        if (definition == null || CollectionUtils.isEmpty(definition.getAllowedValues())) {
            return true;
        }
        if (realValue == null) {
            return true;        // TODO: ok?
        }
        String serializedForm;
        if (realValue instanceof Enum) {
            PrimitiveXNode<String> prim = (PrimitiveXNode<String>) getBeanMarshaller().marshall(realValue);
            serializedForm = prim.getValue();
        } else {
            serializedForm = null;
        }

        return definition.getAllowedValues().stream()
                .anyMatch(displayableValue ->
                        realValue.equals(displayableValue.getValue())
                        || serializedForm != null && serializedForm.equals(displayableValue.getValue())
                );
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
            // TODO fix this hack: for delta values of ObjectReferenceType we will not
            // insist on having reference type (because the target definition could be such that it won't require it)
            boolean allowMissingRefTypesOverride = node.isExplicitTypeDeclaration();
            return parseReferenceValueAsReference(node, definition, pc, allowMissingRefTypesOverride);   // This is "real" reference (oid,  and nothing more)
        }
    }

    private PrismReferenceValue parseReferenceValueAsReference(@NotNull XNode xnode, @NotNull PrismReferenceDefinition definition,
            @NotNull ParsingContext pc, boolean allowMissingRefTypesOverride) throws SchemaException {
        if (!(xnode instanceof MapXNode)) {
            throw new IllegalArgumentException("Cannot parse reference from " + xnode);
        }
        MapXNode map = (MapXNode) xnode;

        String oid = map.getParsedPrimitiveValue(XNode.KEY_REFERENCE_OID, DOMUtil.XSD_STRING);
        PrismReferenceValue refVal = new PrismReferenceValue(oid);

        QName type = map.getParsedPrimitiveValue(XNode.KEY_REFERENCE_TYPE, DOMUtil.XSD_QNAME);
        if (type == null) {
			if (!pc.isAllowMissingRefTypes() && !allowMissingRefTypesOverride) {
				type = definition.getTargetTypeName();
				if (type == null) {
					throw new SchemaException("Target type in reference " + definition.getName() +
							" not specified in reference nor in the schema");
				}
			}
        } else {
            if (QNameUtil.noNamespace(type)) {
                type = getSchemaRegistry().resolveUnqualifiedTypeName(type);
            }
            QName defTargetType = definition.getTargetTypeName();
            if (defTargetType != null) {
                if (!(prismContext.getSchemaRegistry().isAssignableFrom(defTargetType, type))) {
                    throw new SchemaException("Target type specified in reference " + definition.getName() +
                    		" (" + type + ") does not match target type in schema (" + defTargetType + ")");
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

        refVal.setFilter(parseFilter(map.get(XNode.KEY_REFERENCE_FILTER)));

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
        ((PrismReferenceDefinitionImpl) definition).setComposite(true);             // TODO why do we modify the definition? is that safe?
        return refVal;
    }

    private SearchFilterType parseFilter(XNode xnode) throws SchemaException {
        if (xnode == null) {
            return null;
        }
        if (xnode.isEmpty()) {
            return null;
        }
        return SearchFilterType.createFromXNode(xnode, prismContext);
    }

    private ItemDefinition locateItemDefinition(@NotNull QName itemName,
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

    private BeanMarshaller getBeanMarshaller() {
        return ((PrismContextImpl) prismContext).getBeanMarshaller();
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
