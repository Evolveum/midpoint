/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.marshaller;

import static com.evolveum.midpoint.util.Checks.checkSchema;
import static com.evolveum.midpoint.util.Checks.checkSchemaNotNull;
import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xnode.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.impl.PrismContainerDefinitionImpl;
import com.evolveum.midpoint.prism.impl.PrismContainerValueImpl;
import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.impl.PrismPropertyImpl;
import com.evolveum.midpoint.prism.impl.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaRegistryImpl;
import com.evolveum.midpoint.prism.impl.util.PrismUtilInternal;
import com.evolveum.midpoint.prism.impl.xnode.IncompleteMarkerXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.ListXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.PrimitiveXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.SchemaXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ReferentialIntegrityType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;
import com.google.common.collect.ImmutableSet;


public class PrismUnmarshaller {

    private static final Trace LOGGER = TraceManager.getTrace(PrismUnmarshaller.class);

    private static final QName ARTIFICIAL_OBJECT_NAME = new QName(XMLConstants.NULL_NS_URI, "anObject");

    private static final Set<String> REFERENCE_PROPERTIES = ImmutableSet.of(
            XNodeImpl.KEY_REFERENCE_OID.getLocalPart(),
            XNodeImpl.KEY_REFERENCE_TYPE.getLocalPart(),
            XNodeImpl.KEY_REFERENCE_RELATION.getLocalPart(),
            XNodeImpl.KEY_REFERENCE_DESCRIPTION.getLocalPart(),
            XNodeImpl.KEY_REFERENCE_FILTER.getLocalPart(),
            XNodeImpl.KEY_REFERENCE_RESOLUTION_TIME.getLocalPart(),
            XNodeImpl.KEY_REFERENCE_TARGET_NAME.getLocalPart(),
            XNodeImpl.KEY_REFERENCE_OBJECT.getLocalPart(),
            XNodeImpl.KEY_REFERENCE_REFERENTIAL_INTEGRITY.getLocalPart());

    @NotNull private final PrismContext prismContext;
    @NotNull private final BeanUnmarshaller beanUnmarshaller;
    @NotNull private final SchemaRegistryImpl schemaRegistry;

    public PrismUnmarshaller(@NotNull PrismContext prismContext, @NotNull BeanUnmarshaller beanUnmarshaller,
            @NotNull SchemaRegistryImpl schemaRegistry) {
        this.prismContext = prismContext;
        this.beanUnmarshaller = beanUnmarshaller;
        this.schemaRegistry = schemaRegistry;
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
    <O extends Objectable> PrismObject<O> parseObject(@NotNull RootXNodeImpl root, ItemDefinition<?> itemDefinition, QName itemName,
            QName typeName, Class<?> typeClass, @NotNull ParsingContext pc) throws SchemaException {
        checkSchema(itemDefinition == null || itemDefinition instanceof PrismObjectDefinition,
                "Cannot parse object from element %s, the element does not define an object, it is defined as %s",itemName, itemDefinition);
        ItemInfo<?> itemInfo = ItemInfo.determine(itemDefinition,
                root.getRootElementName(), itemName, ARTIFICIAL_OBJECT_NAME,
                root.getTypeQName(), typeName,
                typeClass, PrismObjectDefinition.class, pc, schemaRegistry);

        XNodeImpl child = root.getSubnode();
        checkArgument(child instanceof MapXNodeImpl,
                "Cannot parse object from element %s, we need Map", child.getClass());
        ItemDefinition<?> itemDef = checkSchemaNotNull(itemInfo.getItemDefinition(),
                "Cannot parse object from element %s, there is no definition for that element", itemInfo.getItemName());
        checkSchema(itemDef instanceof PrismObjectDefinition,
                "Cannot parse object from element %s the element does not define an object, it is defined as %s", itemInfo.getItemName(), itemInfo.getItemDefinition());
        return (PrismObject<O>) parseItemInternal(child, itemInfo.getItemName(), itemDef, pc);
    }

    // TODO migrate to parseItem eventually
    @SuppressWarnings("unchecked")
    private <O extends Objectable> PrismObject<O> parseObject(MapXNodeImpl map, PrismObjectDefinition<O> objectDefinition,
            ParsingContext pc) throws SchemaException {
        ItemInfo<?> itemInfo = ItemInfo.determine(objectDefinition,
                null, null, ARTIFICIAL_OBJECT_NAME,
                map.getTypeQName(), null,
                null, PrismObjectDefinition.class, pc, schemaRegistry);
        return (PrismObject<O>) parseItemInternal(map, itemInfo.getItemName(), itemInfo.getItemDefinition(), pc);
    }

    Item<?, ?> parseItem(@NotNull RootXNodeImpl root,
            ItemDefinition<?> itemDefinition, QName itemName, QName typeName, Class<?> typeClass,
            @NotNull ParsingContext pc) throws SchemaException {

        ItemInfo<?> itemInfo = ItemInfo.determine(itemDefinition,
                root.getRootElementName(), itemName, ARTIFICIAL_OBJECT_NAME,
                root.getTypeQName(), typeName,
                typeClass, ItemDefinition.class, pc, schemaRegistry);
        ItemDefinition<?> realDefinition;
        if (itemInfo.getItemDefinition() == null && itemInfo.getComplexTypeDefinition() != null) {
            // Why we do not try to create dynamic definition from other (non-complex) type definitions?
            // (Most probably because we simply don't need it. Null is acceptable in that cases.)
            realDefinition = createDynamicDefinitionFromCtd(itemInfo.getItemName(), itemInfo.getComplexTypeDefinition());
        } else {
            realDefinition = itemInfo.getItemDefinition();
        }
        return parseItemInternal(root.getSubnode(), itemInfo.getItemName(), realDefinition, pc);
    }

    @NotNull
    private ItemDefinition<?> createDynamicDefinitionFromCtd(QName itemName, ComplexTypeDefinition typeDefinition) {
        QName typeName = typeDefinition.getTypeName();
        MutableItemDefinition<?> def;
        if (typeDefinition.isContainerMarker()) {
            // TODO what about objects?
            def = new PrismContainerDefinitionImpl<>(itemName, typeDefinition, prismContext);
        } else {
            def = new PrismPropertyDefinitionImpl<>(itemName, typeName, prismContext);
        }
        def.setDynamic(true);
        return def;
    }

    Object parseItemOrRealValue(@NotNull RootXNodeImpl root, ParsingContext pc) throws SchemaException {
        // is the type name explicitly specified? (if not, guess that we have a string)
        QName typeName = root.getTypeQName();
        if (typeName != null) {
            ItemDefinition<?> itemDefinition = schemaRegistry.findItemDefinitionByType(typeName);
            if (itemDefinition != null) {
                return parseItem(root, itemDefinition, null, null, null, pc);
            } else {
                return beanUnmarshaller.unmarshal(root, ((SchemaRegistry) schemaRegistry).determineCompileTimeClass(typeName), pc);
            }
        } else {
            // if type name is not known, we have to derive it from the element name
            QName itemName = root.getRootElementName();
            ItemDefinition<?> itemDefinition = checkSchemaNotNull(schemaRegistry.findItemDefinitionByElementName(itemName),
                    "Couldn't parse general object with no type name and unknown element name: %s", itemName);
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
    @NotNull
    private Item<?, ?> parseItemInternal(@NotNull XNodeImpl node,
            @NotNull QName itemName, ItemDefinition<?> itemDefinition, @NotNull ParsingContext pc) throws SchemaException {
        Validate.isTrue(!(node instanceof RootXNode));

        // TODO execute this only if in checked mode
        if (itemDefinition == null && node.getTypeQName() != null) {
            PrismContainerDefinition<?> pcd = schemaRegistry.findContainerDefinitionByType(node.getTypeQName());
            if (pcd != null) {
                throw new IllegalStateException("Node has an explicit type corresponding to container (" + pcd
                        + ") but parseItemInternal was called without definition: " + node.debugDump());
            }
        }

        if (itemDefinition == null || itemDefinition instanceof PrismPropertyDefinition) {
            return parseProperty(node, itemName, (PrismPropertyDefinition<?>) itemDefinition, pc);
        } else if (itemDefinition instanceof PrismContainerDefinition) {    // also objects go here
            return parseContainer(node, itemName, (PrismContainerDefinition<?>) itemDefinition, pc);
        } else if (itemDefinition instanceof PrismReferenceDefinition) {
            return parseReference(node, itemName, (PrismReferenceDefinition) itemDefinition, pc);
        } else {
            throw new IllegalArgumentException("Attempt to parse unknown definition type " + itemDefinition.getClass().getName());
        }
    }

    @NotNull
    private <C extends Containerable> PrismContainer<C> parseContainer(@NotNull XNodeImpl node, @NotNull QName itemName,
            @NotNull PrismContainerDefinition<C> containerDef, @NotNull ParsingContext pc) throws SchemaException {
        PrismContainer<C> container = containerDef.instantiate(itemName);
        if (node instanceof ListXNodeImpl) {
            ListXNodeImpl list = (ListXNodeImpl) node;
            if (containerDef instanceof PrismObject && list.size() > 1) {
                pc.warnOrThrow(LOGGER, "Multiple values for a PrismObject: " + node.debugDump());
                parseContainerValueToContainer(container, list.get(0), pc);
            } else {
                for (XNodeImpl subNode : list) {
                    parseContainerValueToContainer(container, subNode, pc);
                }
            }
        } else {
            parseContainerValueToContainer(container, node, pc);
        }
        return container;
    }

    private <C extends Containerable> void parseContainerValueToContainer(PrismContainer<C> container, XNodeImpl node,
            @NotNull ParsingContext pc) throws SchemaException {
        if (node instanceof IncompleteMarkerXNodeImpl) {
            container.setIncomplete(true);
        } else {
            container.add(parseContainerValue(node, container.getDefinition(), pc));
            if (node instanceof MapXNodeImpl && container instanceof PrismObject) {
                MapXNodeImpl map = (MapXNodeImpl) node;
                PrismObject<?> object = (PrismObject<?>) container;
                object.setOid(getOid(map));
                object.setVersion(getVersion(map));
            }
        }
    }

    private String getOid(MapXNodeImpl xmap) throws SchemaException {
        return xmap.getParsedPrimitiveValue(XNodeImpl.KEY_OID, DOMUtil.XSD_STRING);
    }

    private String getVersion(MapXNodeImpl xmap) throws SchemaException {
        return xmap.getParsedPrimitiveValue(XNodeImpl.KEY_VERSION, DOMUtil.XSD_STRING);
    }

    private Long getContainerId(MapXNodeImpl xmap, PrismContainerDefinition<?> containerDef) throws SchemaException {
        PrimitiveXNodeImpl<Object> maybeId = xmap.getPrimitive(XNodeImpl.KEY_CONTAINER_ID);
        if(isContainerId(XNodeImpl.KEY_CONTAINER_ID, maybeId, containerDef)) {
            return maybeId.getParsedValue(DOMUtil.XSD_LONG, Long.class);
        }
        return null;
    }

    private boolean isContainerId(QName itemName, XNodeImpl node, PrismContainerDefinition<?> parentDef) {
        if(node instanceof PrimitiveXNodeImpl<?> && QNameUtil.match(itemName, XNodeImpl.KEY_CONTAINER_ID)) {
            if(((PrimitiveXNodeImpl<?>)node).isAttribute()) {
                return true;
            }
            if(parentDef.isRuntimeSchema() && itemName.getNamespaceURI() != null) {
                return false;
            }
            if(idDef(parentDef) == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an item with name "id".
     *
     * @param containerDef
     * @return
     */
    private ItemDefinition<?> idDef(PrismContainerDefinition<?> containerDef) {
        if (containerDef == null) {
            return null;
        }
        return containerDef.findLocalItemDefinition(XNodeImpl.KEY_CONTAINER_ID);
    }

    private <C extends Containerable> PrismContainerValue<C> parseContainerValue(@NotNull XNodeImpl node,
            @NotNull PrismContainerDefinition<C> containerDef, @NotNull ParsingContext pc) throws SchemaException {
        PrismContainerValue<C> rv;
        if (node instanceof MapXNodeImpl) {
            rv = parseContainerValueFromMap((MapXNodeImpl) node, containerDef, pc);
        } else if (node instanceof PrimitiveXNodeImpl) {
            PrimitiveXNodeImpl<?> prim = (PrimitiveXNodeImpl<?>) node;
            if (prim.isEmpty()) {
                rv = containerDef.createValue();
            } else {
                pc.warnOrThrow(LOGGER, "Cannot parse container value from (non-empty) " + node);
                rv = containerDef.createValue();
            }
        } else {
            pc.warnOrThrow(LOGGER, "Cannot parse container value from " + node);
            rv = containerDef.createValue();
        }
        addMetadataIfPresent(rv, node, pc);
        return rv;
    }

    @NotNull
    private <C extends Containerable> PrismContainerValue<C> parseContainerValueFromMap(@NotNull MapXNodeImpl map,
            @NotNull PrismContainerDefinition<C> containerDef, @NotNull ParsingContext pc) throws SchemaException {

        ComplexTypeDefinition containerTypeDef = containerDef.getComplexTypeDefinition();


        PrismContainerValue<C> cval;
        if (containerDef instanceof PrismObjectDefinition) {
            //noinspection unchecked
            cval = ((PrismObjectDefinition) containerDef).createValue();
        } else {
            Long id = getContainerId(map, containerDef);
            // override container definition, if explicit type is specified
            if (map.getTypeQName() != null) {
                ComplexTypeDefinition explicitTypeDef = schemaRegistry.findComplexTypeDefinitionByType(map.getTypeQName());
                if (explicitTypeDef != null) {
                    if (containerTypeDef != null && explicitTypeDef.isAssignableFrom(containerTypeDef, schemaRegistry)) {
                        // Existing definition (CTD for PCD) is equal or more specific than the explicitly provided one.
                        // Let's then keep using the existing definition. It is not quite clean solution
                        // but there seem to exist serialized objects with generic xsi:type="c:ExtensionType" (MID-6474)
                        // or xsi:type="c:ShadowAttributesType" (MID-6394). Such abstract definitions could lead to
                        // parsing failures because of undefined items.
                        LOGGER.trace("Ignoring explicit type definition {} because equal or even more specific one is present: {}",
                                explicitTypeDef, containerTypeDef);
                    } else {
                        containerTypeDef = explicitTypeDef;
                    }
                } else {
                    pc.warnOrThrow(LOGGER, "Unknown type " + map.getTypeQName() + " in " + map);
                }
            }
            cval = new PrismContainerValueImpl<>(null, null, null, id, containerTypeDef, prismContext);
        }
        parseContainerChildren(cval, map, containerDef, containerTypeDef, pc);
        return cval;
    }

    private void parseContainerChildren(PrismContainerValue<?> cval, MapXNodeImpl map, PrismContainerDefinition<?> containerDef, ComplexTypeDefinition complexTypeDefinition, ParsingContext pc) throws SchemaException {
        for (Entry<QName, XNodeImpl> entry : map.entrySet()) {
            final QName itemName = entry.getKey();
            checkArgument(itemName != null, "Null item name while parsing %s", map.debugDumpLazily());

            if (isContainerId(itemName, entry.getValue(), containerDef)) {
                continue;
            }
            if (containerDef instanceof PrismObjectDefinition &&
                    (QNameUtil.match(itemName, XNodeImpl.KEY_OID) || QNameUtil.match(itemName, XNodeImpl.KEY_VERSION))) {
                continue;
            }

            ItemDefinition<?> itemDef = locateItemDefinition(itemName, complexTypeDefinition, entry.getValue());

            if (itemDef == null) {
                boolean shouldContinue = handleMissingDefinition(itemName, containerDef, complexTypeDefinition, pc, map);
                if(shouldContinue) {
                    continue;
                }

            }
            final Item<?, ?> item;
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
    }

    private boolean handleMissingDefinition(QName itemName, ItemDefinition<?> containerDef, TypeDefinition typeDefinition, ParsingContext pc, DebugDumpable object) throws SchemaException {
        SchemaMigration migration = determineSchemaMigration(typeDefinition, itemName);
        if (migration != null && pc.isCompat()) {
            if (migration.getOperation() == SchemaMigrationOperation.REMOVED) {
                String msg = "Item "+itemName+" was removed from the schema, skipped processing of that item";
                pc.warn(LOGGER, msg);
                return true;
            } else {
                pc.warnOrThrow(LOGGER, "Unsupported migration operation " + migration.getOperation() + " for item " + itemName + " (in "
                        + containerDef + ")" + "while parsing " + object.debugDump());
            }
        }
        boolean anyXsd = typeDefinition instanceof ComplexTypeDefinition && ((ComplexTypeDefinition) typeDefinition).isXsdAnyMarker();
        if (typeDefinition == null || typeDefinition.isRuntimeSchema() || anyXsd) {
            PrismSchema itemSchema = schemaRegistry.findSchemaByNamespace(itemName.getNamespaceURI());
            if (itemSchema != null) {
                // If we already have schema for this namespace then a missing element is
                // an error. We positively know that it is not in the schema.
                pc.warnOrThrow(LOGGER, "Item " + itemName + " has no definition (schema present, in "
                        + containerDef + ")" + "while parsing " + object.debugDump());
                // we can go along this item (at least show it in repository pages) - MID-3249
                // TODO make this configurable
            } else {
                // No definition for item, but the schema is runtime. the definition may come later.
                // Null is OK here. The item will be parsed as "raw"
            }
        } else {    // complex type definition is static
            pc.warnOrThrow(LOGGER, "Item " + itemName + " has no definition (in value "
                    + typeDefinition + ")" + "while parsing " + object.debugDump());
            return true;   // don't even attempt to parse it
        }
        return false;
    }

    private SchemaMigration determineSchemaMigration(TypeDefinition typeDefinition, QName itemName) {
        if (typeDefinition == null) {
            return null;
        }
        List<SchemaMigration> schemaMigrations = typeDefinition.getSchemaMigrations();
        if (schemaMigrations != null) {
            for (SchemaMigration schemaMigration : schemaMigrations) {
                if (QNameUtil.match(schemaMigration.getElementQName(), itemName)) {
                    return schemaMigration;
                }
            }
        }
        QName superTypeQName = typeDefinition.getSuperType();
        if (superTypeQName == null) {
            return null;
        }
        TypeDefinition superTypeDef = prismContext.getSchemaRegistry().findTypeDefinitionByType(superTypeQName);
        if (superTypeDef == null) {
            return null;
        }
        return determineSchemaMigration(superTypeDef, itemName);
    }

    @NotNull
    private <T> PrismProperty<T> parseProperty(@NotNull XNodeImpl node, @NotNull QName itemName,
            @Nullable PrismPropertyDefinition<T> itemDefinition, @NotNull ParsingContext pc) throws SchemaException {
        Validate.isTrue(!(node instanceof RootXNodeImpl));

        PrismProperty<T> property = itemDefinition != null ?
                itemDefinition.instantiate() :
                new PrismPropertyImpl<>(itemName, prismContext);

        if (node instanceof ListXNodeImpl && !node.isHeterogeneousList()) {
            ListXNodeImpl listNode = (ListXNodeImpl) node;
            checkSchema(itemDefinition == null || itemDefinition.isMultiValue() || listNode.size() <= 1,
                    "Attempt to store multiple values in single-valued property %s", itemName);
            for (XNodeImpl subNode : listNode) {
                if (subNode instanceof IncompleteMarkerXNodeImpl) {
                    property.setIncomplete(true);
                } else {
                    PrismPropertyValue<T> pval = parsePropertyValue(subNode, itemDefinition, pc);
                    addItemValueIfPossible(property, pval, pc);
                }
            }
        } else if (node instanceof MapXNodeImpl || node instanceof PrimitiveXNodeImpl || node.isHeterogeneousList()) {
            PrismPropertyValue<T> pval = parsePropertyValue(node, itemDefinition, pc);
            if (pval != null) {
                try {
                    property.add(pval);
                } catch (SchemaException e) {
                    if (pc.isCompat()) {
                        // Most probably the "apply definition" call while adding the value failed. This occurs for raw
                        // values with (somewhat) incorrect definitions being added. Overall, this is more a hack than serious
                        // solution, because we sometimes want to add static-schema-less property values. TODO investigate this.
                        ((PrismPropertyImpl<T>) property).addForced(pval);
                    } else {
                        throw e;
                    }
                }
            }
        } else if (node instanceof SchemaXNodeImpl) {
            SchemaDefinitionType schemaDefType = beanUnmarshaller.unmarshalSchemaDefinitionType((SchemaXNodeImpl) node);
            @SuppressWarnings("unchecked")
            PrismPropertyValue<T> val = (PrismPropertyValue<T>) new PrismPropertyValueImpl<>(schemaDefType);
            addItemValueIfPossible(property, val, pc);
        } else if (node instanceof IncompleteMarkerXNodeImpl) {
            property.setIncomplete(true);
        } else {
            throw new IllegalArgumentException("Cannot parse property from " + node);
        }
        return property;
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void addItemValueIfPossible(Item<V, D> item, V value, ParsingContext pc) throws SchemaException {
        if (value != null) {
            try {
                item.add(value);
            } catch (SchemaException e) {
                pc.warnOrThrow(LOGGER, "Couldn't add a value of " + value + " to the containing item: " + e.getMessage(), e);
            }
        }
    }

    // if definition == null or any AND node has type defined, this type must be non-containerable (fit into PPV)
    private <T> PrismPropertyValue<T> parsePropertyValue(@NotNull XNodeImpl node,
            @Nullable PrismPropertyDefinition<T> definition, @NotNull ParsingContext pc) throws SchemaException {
        QName typeFromDefinition = definition != null && !definition.isAnyType() ? definition.getTypeName() : null;
        QName typeName = ((SchemaRegistry) schemaRegistry).selectMoreSpecific(typeFromDefinition, node.getTypeQName());

        PrismPropertyValue<T> rv;
        if (typeName == null) {
            return createRawPrismPropertyValue(node);
        } else if (beanUnmarshaller.canProcess(typeName)) {
            Object unmarshalled = beanUnmarshaller.unmarshal(node, typeName, pc);
            T realValue = treatPolyStringAndRecompute(unmarshalled);
            if (!isValueAllowed(realValue, definition)) {
                pc.warnOrThrow(LOGGER, "Unknown (not allowed) value of type " + typeName + ". Value: " + realValue + ". Allowed values: " + definition.getAllowedValues());
                rv = null;
            } else if (realValue == null) {
                rv = deriveValueFromExpression(node);
            } else {
                PrismPropertyValueImpl<T> ppv = new PrismPropertyValueImpl<>(realValue);
                ppv.setPrismContext(prismContext);
                rv = ppv;
            }
        } else {
            pc.warnOrThrow(LOGGER, "Cannot parse as " + typeName + " because bean unmarshaller cannot process it (generated bean classes are missing?): " + node.debugDump());
            rv = createRawPrismPropertyValue(node);
        }
        addMetadataIfPresent(rv, node, pc);
        return rv;
    }

    private void addMetadataIfPresent(PrismValue prismValue, XNode node, @NotNull ParsingContext pc) throws SchemaException {
        if (prismValue != null && node instanceof MetadataAware) {
            parseMetadataNodes(prismValue, ((MetadataAware) node).getMetadataNodes(), pc);
        }
    }

    private void parseMetadataNodes(PrismValue prismValue, List<MapXNode> metadataNodes, ParsingContext pc) throws SchemaException {
        for (MapXNode metadataNode : metadataNodes) {
            PrismContainerValue pcv =
                    parseContainerValueFromMap((MapXNodeImpl) metadataNode, schemaRegistry.getValueMetadataDefinition(), pc);
            //noinspection unchecked
            prismValue.getValueMetadata().add(pcv);
        }
    }

    @Nullable
    private <T> PrismPropertyValue<T> deriveValueFromExpression(@NotNull XNodeImpl node) throws SchemaException {
        // Be careful here. Expression element can be legal sub-element of complex properties.
        // Therefore parse expression only if there is no legal value.
        ExpressionWrapper expression = PrismUtilInternal.parseExpression(node, prismContext);
        if (expression != null) {
            return new PrismPropertyValueImpl<>(null, prismContext, null, null, expression);
        } else {
            // There's no point in returning PPV(null) as it would soon fail on internal PP check.
            // We are probably recovering from an error in COMPAT mode here, so let's just skip this value.
            return null;
        }
    }

    // Postprocessing after returning from unmarshaller. It speaks bean language (e.g. PolyStringType, not PolyString).
    private <T> T treatPolyStringAndRecompute(Object bean) {
        Object rv;
        if (bean instanceof PolyStringType) {
            rv = ((PolyStringType) bean).toPolyString();
        } else {
            rv = bean;
        }
        PrismUtil.recomputeRealValue(rv, prismContext);
        //noinspection unchecked
        return (T) rv;
    }

    @NotNull
    private <T> PrismPropertyValue<T> createRawPrismPropertyValue(@NotNull XNodeImpl node) {
        return prismContext.itemFactory().createPropertyValue(node);
    }

    @Contract("_, null -> true")
    private <T> boolean isValueAllowed(T realValue, PrismPropertyDefinition<T> definition) {
        if (realValue instanceof Enum) {
            // Statically-defined enums have been already treated. Unless someone overrides the static schema,
            // reducing the set of allowed values. But let's declared this feature as "not supported yet")
            return true;
        } else if (definition == null || CollectionUtils.isEmpty(definition.getAllowedValues())) {
            return true;
        } else if (realValue == null) {
            return true; // TODO: ok?
        } else {
            return definition.getAllowedValues().stream()
                    .anyMatch(displayableValue -> realValue.equals(displayableValue.getValue()));
        }
    }

    @NotNull
    private PrismReference parseReference(@NotNull XNodeImpl node, @NotNull QName itemName,
            @NotNull PrismReferenceDefinition definition, @NotNull ParsingContext pc) throws SchemaException {
        PrismReference ref = definition.instantiate();
        if (node instanceof ListXNodeImpl) {
            for (XNodeImpl subNode : (ListXNodeImpl) node) {
                if (subNode instanceof IncompleteMarkerXNodeImpl) {
                    ref.setIncomplete(true);
                } else {
                    ref.add(parseReferenceValueFromXNode(subNode, definition, itemName, pc));
                }
            }
        } else if (node instanceof MapXNodeImpl) {
            ref.add(parseReferenceValueFromXNode(node, definition, itemName, pc));
        } else if (node instanceof PrimitiveXNodeImpl) {
            // empty
        } else if (node instanceof IncompleteMarkerXNodeImpl) {
            ref.setIncomplete(true);
        } else {
            throw new IllegalArgumentException("Cannot parse reference from " + node);
        }
        return ref;
    }

    @NotNull
    private PrismReferenceValue parseReferenceValueFromXNode(@NotNull XNodeImpl node,
            @NotNull PrismReferenceDefinition definition, @NotNull QName itemName, @NotNull ParsingContext pc) throws SchemaException {
        /*
         *  We distinguish between "real" references and composite objects by
         *  (1) looking at type QName of XNode passed (whether it's ObjectType or ObjectReferenceType)
         *  (2) comparing itemName and name from reference definition - e.g. linkRef vs. link
         */
        boolean isComposite;
        if (node.getTypeQName() != null) {
            QName typeName = node.getTypeQName();
            ItemDefinition<?> contentDefinition = schemaRegistry.findItemDefinitionByType(typeName);
            isComposite = contentDefinition instanceof PrismObjectDefinition;
        } else {
            isComposite = !QNameUtil.match(itemName, definition.getItemName());
        }

        PrismReferenceValue rv;
        if (isComposite) {
            rv = parseReferenceValueAsCompositeObject(node, definition, pc);  // This is a composite object (complete object stored inside reference)
        } else {
            // TODO fix this hack: for delta values of ObjectReferenceType we will not
            //  insist on having reference type (because the target definition could be such that it won't require it)
            boolean allowMissingRefTypesOverride = node.isExplicitTypeDeclaration();
            rv = parseReferenceValueAsReference(itemName, node, definition, pc, allowMissingRefTypesOverride);   // This is "real" reference (oid,  and nothing more)
        }

        addMetadataIfPresent(rv, node, pc);
        return rv;
    }

    private PrismReferenceValue parseReferenceValueAsReference(QName name, @NotNull XNodeImpl xnode, @NotNull PrismReferenceDefinition definition,
            @NotNull ParsingContext pc, boolean allowMissingRefTypesOverride) throws SchemaException {
        checkArgument(xnode instanceof MapXNodeImpl, "Cannot parse reference from %s", xnode);
        MapXNodeImpl map = (MapXNodeImpl) xnode;
        TypeDefinition typeDefinition = schemaRegistry.findTypeDefinitionByType(definition.getTypeName());
        String oid = map.getParsedPrimitiveValue(XNodeImpl.KEY_REFERENCE_OID, DOMUtil.XSD_STRING);
        PrismReferenceValue refVal = new PrismReferenceValueImpl(oid);

        QName targetType = map.getParsedPrimitiveValue(XNodeImpl.KEY_REFERENCE_TYPE, DOMUtil.XSD_QNAME);
        if (targetType == null) {
            if (!pc.isAllowMissingRefTypes() && !allowMissingRefTypesOverride) {
                targetType = checkSchemaNotNull(definition.getTargetTypeName(),
                        "Target type in reference %s not specified in reference nor in the schema", definition.getItemName());
            }
        } else {
            if (QNameUtil.noNamespace(targetType)) {
                targetType = ((SchemaRegistry) schemaRegistry).resolveUnqualifiedTypeName(targetType);
            }
            QName defTargetType = definition.getTargetTypeName();
            if (defTargetType != null) {
                checkSchema(prismContext.getSchemaRegistry().isAssignableFrom(defTargetType, targetType),
                        "Target type specified in reference %s (%s) does not match target type in schema (%s)", definition.getItemName(), targetType, defTargetType);
            }
        }
        PrismObjectDefinition<Objectable> objectDefinition = null;
        if (targetType != null) {
            objectDefinition = checkSchemaNotNull(schemaRegistry.findObjectDefinitionByType(targetType),
                    "No definition for type %s in reference",targetType);
            refVal.setTargetType(targetType);
        }

        QName relationAttribute = map.getParsedPrimitiveValue(XNodeImpl.KEY_REFERENCE_RELATION, DOMUtil.XSD_QNAME);
        refVal.setRelation(relationAttribute);

        refVal.setDescription(map.getParsedPrimitiveValue(XNodeImpl.KEY_REFERENCE_DESCRIPTION, DOMUtil.XSD_STRING));

        refVal.setFilter(parseFilter(map.get(XNodeImpl.KEY_REFERENCE_FILTER), pc));

        String resolutionTimeString = map.getParsedPrimitiveValue(XNodeImpl.KEY_REFERENCE_RESOLUTION_TIME, DOMUtil.XSD_STRING);
        if (resolutionTimeString != null) {
            EvaluationTimeType resolutionTime = EvaluationTimeType.fromValue(resolutionTimeString);
            refVal.setResolutionTime(resolutionTime);
        }

        String referentialIntegrityString = map.getParsedPrimitiveValue(XNodeImpl.KEY_REFERENCE_REFERENTIAL_INTEGRITY,
                DOMUtil.XSD_STRING);
        if (referentialIntegrityString != null) {
            refVal.setReferentialIntegrity(ReferentialIntegrityType.fromValue(referentialIntegrityString));
        }

        XNodeImpl xnodeForTargetName = map.get(XNodeImpl.KEY_REFERENCE_TARGET_NAME);
        if (xnodeForTargetName != null) {
            PolyStringType targetName = beanUnmarshaller.unmarshal(xnodeForTargetName, PolyStringType.class, pc);
            refVal.setTargetName(targetName);
        }

        XNodeImpl xrefObject = map.get(XNodeImpl.KEY_REFERENCE_OBJECT);
        if (xrefObject != null) {
            MapXNodeImpl objectMapNode = toObjectMapNode(xrefObject);
            checkSchemaNotNull(targetType, "Cannot parse object from %s without knowing its type", xrefObject);
            PrismObject<Objectable> object = parseObject(objectMapNode, objectDefinition, pc);
            setReferenceObject(refVal, object);
        }
        for (Entry<QName, XNodeImpl> entry : map.entrySet()) {
            QName itemName = entry.getKey();
            if (!isDefinedProperty(itemName, name.getNamespaceURI(), REFERENCE_PROPERTIES)) {
                handleMissingDefinition(itemName, definition, typeDefinition, pc, map);
            }
        }

        return refVal;
    }

    @SuppressWarnings({ "unused", "SameParameterValue" })
    private boolean isDefinedProperty(QName itemName, String parentNamespace, Set<String> properties) {
        // TODO: Namespace awarness is disabled, because some calls from parseItem use ItemDefinition.itemName
        // instead of used itemName
        /*
        String ns = itemName.getNamespaceURI();
        if(Strings.isNullOrEmpty(ns) || Objects.equals(parentNamespace,ns)) {
            return properties.contains(itemName.getLocalPart());
        }
        */
        return properties.contains(itemName.getLocalPart());
    }

    private MapXNodeImpl toObjectMapNode(XNodeImpl xNode) throws SchemaException {
        if (xNode instanceof MapXNodeImpl) {
            return (MapXNodeImpl) xNode;
        } else if (xNode instanceof PrimitiveXNode && xNode.isEmpty()) {
            return new MapXNodeImpl();
        } else {
            // TODO ...or warn
            throw new SchemaException("Cannot parse object from " + xNode);
        }
    }

    private void setReferenceObject(PrismReferenceValue refVal, PrismObject<Objectable> object) throws SchemaException {
        refVal.setObject(object);
        if (object.getOid() != null) {
            if (refVal.getOid() == null) {
                refVal.setOid(object.getOid());
            } else {
                checkSchema(refVal.getOid().equals(object.getOid()),
                        "OID in reference (%s) does not match OID in composite object (%s)", refVal.getOid(), object.getOid());
            }
        }
        QName objectTypeName = object.getDefinition().getTypeName();
        if (refVal.getTargetType() == null) {
            refVal.setTargetType(objectTypeName);
        } else {
            checkSchema(refVal.getTargetType().equals(objectTypeName),
                    "Target type in reference (%s) does not match type in composite object (%s)", refVal.getTargetType(), objectTypeName);
        }
    }

    private PrismReferenceValue parseReferenceValueAsCompositeObject(XNodeImpl node,
            PrismReferenceDefinition definition, ParsingContext pc) throws SchemaException {
        checkArgument(node instanceof MapXNodeImpl, "Cannot parse reference composite object from %s", node);
        MapXNodeImpl map = (MapXNodeImpl) node;
        QName targetTypeName = definition.getTargetTypeName();
        PrismObjectDefinition<Objectable> objectDefinition = null;
        if (map.getTypeQName() != null) {
            objectDefinition = schemaRegistry.findObjectDefinitionByType(map.getTypeQName());
        }
        if (objectDefinition == null && targetTypeName != null) {
            objectDefinition = schemaRegistry.findObjectDefinitionByType(targetTypeName);
        }
        checkSchemaNotNull(objectDefinition,
                "No object definition for composite object in reference element %s", definition.getCompositeObjectElementName());
        PrismObject<Objectable> compositeObject;
        try {
            compositeObject = parseObject(map, objectDefinition, pc);
        } catch (SchemaException e) {
            throw new SchemaException(e.getMessage() + " while parsing composite object in reference element "
                    + definition.getCompositeObjectElementName(), e);
        }

        PrismReferenceValue refVal = new PrismReferenceValueImpl();
        setReferenceObject(refVal, compositeObject);
        return refVal;
    }

    private SearchFilterType parseFilter(XNodeImpl xnode, ParsingContext pc) throws SchemaException {
        if (xnode == null) {
            return null;
        }
        if (xnode.isEmpty()) {
            return null;
        }
        return SearchFilterType.createFromParsedXNode(xnode, pc, prismContext);
    }

    private ItemDefinition<?> locateItemDefinition(@NotNull QName itemName, @Nullable ComplexTypeDefinition complexTypeDefinition,
            XNode xnode) {
        return schemaRegistry.locateItemDefinition(itemName, complexTypeDefinition, qName -> createDynamicItemDefinition(qName, xnode));
    }

    private ItemDefinition<?> createDynamicItemDefinition(QName itemName, XNode node)  {
        if (node == null) {
            return null;
        }
        QName typeName = node.getTypeQName();
        if (typeName == null) {
            if (node instanceof ListXNodeImpl) {
                // there may be type definitions in individual list members
                for (XNodeImpl subNode : ((ListXNodeImpl) node)) {
                    ItemDefinition<?> subdef = createDynamicItemDefinition(itemName, subNode);
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

        PrismPropertyDefinitionImpl <?> propDef = new PrismPropertyDefinitionImpl<>(itemName, typeName, prismContext);
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

    //TODO
    public <T extends Containerable> ItemDefinition<?> locateItemDefinition(
            @NotNull PrismContainerDefinition<T> containerDefinition, @NotNull QName itemName, @Nullable XNode xnode) {
        return locateItemDefinition(itemName, containerDefinition.getComplexTypeDefinition(), xnode);
    }
}
