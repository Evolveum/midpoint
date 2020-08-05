/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.marshaller;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.impl.util.PrismUtilInternal;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.MetadataAware;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ReferentialIntegrityType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptySet;

/**
 * @author semancik
 *
 */
public class PrismMarshaller {

    private static final Trace LOGGER = TraceManager.getTrace(PrismMarshaller.class);

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
     * @param itemsToSkip Top-level items that should not be marshaled.
     * @return Marshaled item.
     */
    @NotNull
    RootXNodeImpl marshalItemAsRoot(@NotNull Item<?, ?> item, QName itemName,
            ItemDefinition itemDefinition, SerializationContext context,
            Collection<? extends QName> itemsToSkip) throws SchemaException {

        @NotNull QName realItemName = itemName != null ? itemName : item.getElementName();
        ItemDefinition realItemDefinition = itemDefinition != null ? itemDefinition : item.getDefinition();

        XNodeImpl content = marshalItemContent(item, realItemDefinition, context, itemsToSkip);
        if (realItemDefinition != null) {
            addTypeDefinitionIfNeeded(realItemName, realItemDefinition.getTypeName(), content);
        }
        return new RootXNodeImpl(realItemName, content);
    }

    /**
     * Marshals a single PrismValue. For simplicity and compatibility with other interfaces, the result is always a RootXNode.
     *
     * @param value PrismValue to be marshaled.
     * @param itemName Item name to be used. Optional. If omitted, it is derived from value and/or definition.
     * @param itemDefinition Item definition to be used. Optional.
     * @param context Serialization context.
     * @param itemsToSkip Top-level items that should not be marshaled.
     * @return Marshaled prism value.
     */
    @NotNull
    RootXNodeImpl marshalPrismValueAsRoot(@NotNull PrismValue value, QName itemName, ItemDefinition itemDefinition,
            SerializationContext context, Collection<? extends QName> itemsToSkip) throws SchemaException {
        ItemInfo itemInfo = ItemInfo.determineFromValue(value, itemName, itemDefinition, getSchemaRegistry());
        QName realItemName = itemInfo.getItemName();
        ItemDefinition realItemDefinition = itemInfo.getItemDefinition();
        QName realItemTypeName = itemInfo.getTypeName();

        if (realItemName == null) {
            throw new IllegalArgumentException("Couldn't determine item name from the prism value; cannot marshal to RootXNode");
        }

        XNodeImpl valueNode = marshalItemValue(value, realItemDefinition, realItemTypeName, context, itemsToSkip);
        addTypeDefinitionIfNeeded(realItemName, realItemTypeName, valueNode);
        return new RootXNodeImpl(realItemName, valueNode);
    }

    /**
     * Marshals any data - prism item or real value.
     *
     * @param object Object to be marshaled.
     * @param itemName Item name to be used. Optional. If omitted, it is derived from value and/or definition.
     * @param itemDefinition Item definition to be used. Optional.
     * @param context Serialization context.
     * @param itemsToSkip Top-level items that should not be marshaled.
     * @return Marshaled object.
     */
    @NotNull
    RootXNodeImpl marshalAnyData(@NotNull Object object, QName itemName, ItemDefinition itemDefinition,
            SerializationContext context, Collection<? extends QName> itemsToSkip) throws SchemaException {
        if (object instanceof Item) {
            return marshalItemAsRoot((Item) object, itemName, itemDefinition, context, itemsToSkip);
        }
        Validate.notNull(itemName, "itemName must be specified for non-Item objects");
        if (object instanceof Containerable) {
            return marshalPrismValueAsRoot(((Containerable) object).asPrismContainerValue(), itemName, null, context, itemsToSkip);
        } else if (beanMarshaller.canProcess(object.getClass())) {
            if (CollectionUtils.isNotEmpty(itemsToSkip)) {
                LOGGER.warn("Trying to skip marshalling items {} where not applicable: {}", itemsToSkip, object.getClass());
            }
            XNodeImpl valueNode = beanMarshaller.marshall(object, context);        // TODO item definition!
            QName typeName = JAXBUtil.getTypeQName(object.getClass());
            if (valueNode != null) {
                addTypeDefinitionIfNeeded(itemName, typeName, valueNode);
                if (valueNode.getTypeQName() == null && typeName == null) {
                    throw new SchemaException("No type QName for class " + object.getClass());
                }
            } // TODO or else put type name at least to root? (but, can valueNode be null if object is not null?)
            return new RootXNodeImpl(itemName, valueNode);
        } else {
            throw new IllegalArgumentException("Couldn't serialize " + object);
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
    private XNodeImpl marshalItemContent(@NotNull Item<?, ?> item,
            ItemDefinition itemDefinition, SerializationContext context,
            Collection<? extends QName> itemsToSkip) throws SchemaException {
        if (item.isEmpty() && item.isIncomplete()) {
            return new IncompleteMarkerXNodeImpl();
        } else if (item.size() == 1 && !item.isIncomplete()) {
            return marshalItemValue(item.getAnyValue(), itemDefinition, null, context, itemsToSkip);
        } else {
            ListXNodeImpl xlist = new ListXNodeImpl();
            for (PrismValue val : item.getValues()) {
                xlist.add(marshalItemValue(val, itemDefinition, null, context, itemsToSkip));
            }
            if (item.isIncomplete()) {
                xlist.add(new IncompleteMarkerXNodeImpl());
            }
            return xlist;
        }
    }

    @NotNull
    private <O extends Objectable> MapXNodeImpl marshalObjectContent(@NotNull PrismObject<O> object, @NotNull PrismObjectDefinition<O> objectDefinition, SerializationContext ctx) throws SchemaException {
        MapXNodeImpl xmap = new MapXNodeImpl();
        marshalContainerValue(xmap, object.getValue(), objectDefinition, ctx, null);
        xmap.setTypeQName(objectDefinition.getTypeName());        // object should have the definition (?)
        return xmap;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private XNodeImpl marshalItemValue(@NotNull PrismValue itemValue, @Nullable ItemDefinition definition,
            @Nullable QName typeName, SerializationContext ctx,
            Collection<? extends QName> itemsToSkip) throws SchemaException {
        XNodeImpl xnode;
        if (definition == null && typeName == null && itemValue instanceof PrismPropertyValue) {
            warnIfItemsToSkip(itemValue, itemsToSkip);
            xnode = serializePropertyRawValue((PrismPropertyValue<?>) itemValue);
        } else if (itemValue instanceof PrismReferenceValue) {
            warnIfItemsToSkip(itemValue, itemsToSkip);
            xnode = serializeReferenceValue((PrismReferenceValue)itemValue, (PrismReferenceDefinition) definition, ctx);
        } else if (itemValue instanceof PrismPropertyValue<?>) {
            warnIfItemsToSkip(itemValue, itemsToSkip);
            xnode = serializePropertyValue((PrismPropertyValue<?>)itemValue, (PrismPropertyDefinition) definition, typeName, ctx);
        } else if (itemValue instanceof PrismContainerValue<?>) {
            xnode = marshalContainerValue((PrismContainerValue<?>)itemValue, (PrismContainerDefinition) definition, ctx, itemsToSkip);
        } else {
            throw new IllegalArgumentException("Unsupported value type "+itemValue.getClass());
        }
        if (definition != null && (definition.isDynamic() || shouldPutTypeInExportMode(ctx, definition)) && isInstantiable(definition)) {
            if (xnode.getTypeQName() == null) {
                xnode.setTypeQName(definition.getTypeName());
            }
            xnode.setExplicitTypeDeclaration(true);
        }
        marshalValueMetadata(itemValue, xnode, ctx);
        return xnode;
    }

    private void marshalValueMetadata(PrismValue itemValue, XNodeImpl xnode, SerializationContext ctx) throws SchemaException {
        PrismContainer<?> valueMetadataContainer = itemValue.getValueMetadata();
        if (!valueMetadataContainer.isEmpty()) {
            if (xnode instanceof MetadataAware) {
                for (PrismContainerValue<?> valueMetadataValue : valueMetadataContainer.getValues()) {
                    MapXNode metadataNode = marshalContainerValue(valueMetadataValue,
                            getSchemaRegistry().getValueMetadataDefinition(), ctx, emptySet());
                    ((MetadataAware) xnode).addMetadataNode(metadataNode);
                }
            } else {
                throw new IllegalStateException("Couldn't marshal value metadata of " + itemValue + " to non-metadata-aware XNode: " + xnode);
            }
        }
    }

    private void warnIfItemsToSkip(@NotNull PrismValue itemValue, Collection<? extends QName> itemsToSkip) {
        if (CollectionUtils.isNotEmpty(itemsToSkip)) {
            LOGGER.warn("Trying to skip marshalling items {} where not applicable: {}", itemsToSkip, itemValue);
        }
    }

    private boolean shouldPutTypeInExportMode(SerializationContext ctx, ItemDefinition definition) {
        if (!SerializationContext.isSerializeForExport(ctx) || definition == null || !definition.isRuntimeSchema()) {
            return false;
        }
        QName itemName = definition.getItemName();
        if (StringUtils.isEmpty(itemName.getNamespaceURI())) {
            return true;            // ambiguous item name - let's put xsi:type, to be on the safe side
        }
        // we assume that all runtime elements which are part of the schema registry are retrievable by element name
        // (might not be the case for sub-items of custom extension containers! we hope providing xsi:type there will cause no harm)
        List<ItemDefinition> definitionsInRegistry = getSchemaRegistry()
                .findItemDefinitionsByElementName(itemName, ItemDefinition.class);
        return definitionsInRegistry.isEmpty();       // no definition in registry => xsi:type should be put
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

    @NotNull
    private MapXNodeImpl marshalContainerValue(PrismContainerValue<?> containerVal,
            PrismContainerDefinition<?> containerDefinition, SerializationContext ctx,
            Collection<? extends QName> itemsToSkip) throws SchemaException {
        MapXNodeImpl xmap = new MapXNodeImpl();
        marshalContainerValue(xmap, containerVal, containerDefinition, ctx, itemsToSkip);
        return xmap;
    }

    private void marshalContainerValue(MapXNodeImpl xmap, PrismContainerValue<?> containerVal,
            PrismContainerDefinition<?> containerDefinition, SerializationContext ctx,
            Collection<? extends QName> itemsToSkip) throws SchemaException {
        Long id = containerVal.getId();
        if (id != null) {
            xmap.put(XNodeImpl.KEY_CONTAINER_ID, createPrimitiveXNodeAttr(id, DOMUtil.XSD_LONG));
        }
        if (containerVal instanceof PrismObjectValue) {
            PrismObjectValue<?> objectVal = (PrismObjectValue<?>) containerVal;
            if (objectVal.getOid() != null) {
                xmap.put(XNodeImpl.KEY_OID, createPrimitiveXNodeStringAttr(objectVal.getOid()));
            }
            if (objectVal.getVersion() != null) {
                xmap.put(XNodeImpl.KEY_VERSION, createPrimitiveXNodeStringAttr(objectVal.getVersion()));
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
                ItemName elementName = itemDef.getItemName();
                Item<?,?> item = containerVal.findItem(elementName);
                if (item != null) {
                    XNodeImpl xsubnode;
                    if (shouldSkipItem(itemsToSkip, elementName, itemDef, ctx) && !item.hasNoValues()) {
                        xsubnode = new IncompleteMarkerXNodeImpl();
                    } else {
                        xsubnode = marshalItemContent(item, getItemDefinition(containerVal, item), ctx, null);
                    }
                    xmap.put(elementName, xsubnode);
                    marshaledItems.add(elementName);
                }
            }
        }
        // There are some cases when we do not have list of all elements in a container.
        // E.g. in run-time schema. Therefore we must also iterate over items and not just item definitions.
        for (Item<?,?> item : containerVal.getItems()) {
            QName elementName = item.getElementName();
            if (!marshaledItems.contains(elementName)) {
                XNodeImpl xsubnode;
                if (shouldSkipItem(itemsToSkip, elementName, item.getDefinition(), ctx) && !item.hasNoValues()) {
                    xsubnode = new IncompleteMarkerXNodeImpl();
                } else {
                    xsubnode = marshalItemContent(item, getItemDefinition(containerVal, item), ctx, null);
                }
                xmap.put(elementName, xsubnode);
            }
        }
    }

    private boolean shouldSkipItem(Collection<? extends QName> itemsToSkip, QName elementName, ItemDefinition<?> itemDef,
            SerializationContext ctx) {
        return QNameUtil.contains(itemsToSkip, elementName) ||
                ctx != null && ctx.getOptions() != null && ctx.getOptions().isSkipIndexOnly() && itemDef != null && itemDef.isIndexOnly();
    }

    private <C extends Containerable> ItemDefinition getItemDefinition(PrismContainerValue<C> cval, Item<?, ?> item) {
        if (item.getDefinition() != null) {
            return item.getDefinition();
        }
        ComplexTypeDefinition ctd = cval.getComplexTypeDefinition();
        if (ctd == null) {
            return null;
        }
        return ctd.findLocalItemDefinition(item.getElementName());
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

    private XNodeImpl serializeReferenceValue(PrismReferenceValue value, PrismReferenceDefinition definition, SerializationContext ctx) throws SchemaException {
        MapXNodeImpl xmap = new MapXNodeImpl();
        boolean containsOid;
        String namespace = definition != null ? definition.getNamespace() : null;           // namespace for filter and description
        if (StringUtils.isNotBlank(value.getOid())) {
            containsOid = true;
            xmap.put(XNodeImpl.KEY_REFERENCE_OID, createPrimitiveXNodeStringAttr(value.getOid()));
        } else {
            containsOid = false;
        }
        QName relation = value.getRelation();
        if (relation != null) {
            xmap.put(XNodeImpl.KEY_REFERENCE_RELATION, createPrimitiveXNodeAttr(relation, DOMUtil.XSD_QNAME));
        }
        QName targetType = value.getTargetType();
        if (targetType != null) {
            xmap.put(XNodeImpl.KEY_REFERENCE_TYPE, createPrimitiveXNodeAttr(targetType, DOMUtil.XSD_QNAME));
        }
        String description = value.getDescription();
        if (description != null) {
            xmap.put(createReferenceQName(XNodeImpl.KEY_REFERENCE_DESCRIPTION, namespace), createPrimitiveXNode(description, DOMUtil.XSD_STRING));
        }
        SearchFilterType filter = value.getFilter();
        if (filter != null) {
            XNodeImpl xsubnode = (XNodeImpl) filter.serializeToXNode(beanMarshaller.getPrismContext());
            if (xsubnode != null) {
                xmap.put(createReferenceQName(XNodeImpl.KEY_REFERENCE_FILTER, namespace), xsubnode);
            }
        }
        EvaluationTimeType resolutionTime = value.getResolutionTime();
        if (resolutionTime != null) {
            xmap.put(createReferenceQName(XNodeImpl.KEY_REFERENCE_RESOLUTION_TIME, namespace),
                    createPrimitiveXNode(resolutionTime.value(), DOMUtil.XSD_STRING));
        }
        ReferentialIntegrityType referentialIntegrity = value.getReferentialIntegrity();
        if (referentialIntegrity != null) {
            xmap.put(createReferenceQName(XNodeImpl.KEY_REFERENCE_REFERENTIAL_INTEGRITY, namespace),
                    createPrimitiveXNode(referentialIntegrity.value(), DOMUtil.XSD_STRING));
        }
        if (value.getTargetName() != null) {
            if (SerializationContext.isSerializeReferenceNames(ctx) ||
                    !containsOid && SerializationContext.isSerializeReferenceNamesForNullOids(ctx)) {
                XNodeImpl xsubnode = createPrimitiveXNode(value.getTargetName(), PolyStringType.COMPLEX_TYPE);
                xmap.put(createReferenceQName(XNodeImpl.KEY_REFERENCE_TARGET_NAME, namespace), xsubnode);
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
            //noinspection unchecked
            XNodeImpl xobjnode = marshalObjectContent(value.getObject(), value.getObject().getDefinition(), ctx);
            xmap.put(createReferenceQName(XNodeImpl.KEY_REFERENCE_OBJECT, namespace), xobjnode);
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
    @NotNull
    private <T> XNodeImpl serializePropertyValue(@NotNull PrismPropertyValue<T> value, PrismPropertyDefinition<T> definition,
            QName typeNameIfNoDefinition, SerializationContext ctx) throws SchemaException {
        @Nullable QName typeName = definition != null ? definition.getTypeName() : typeNameIfNoDefinition;
        ExpressionWrapper expression = value.getExpression();
        if (expression != null) {
            // Store expression, not the value. In this case the value (if any) is
            // a transient product of the expression evaluation.
            return createExpressionXNode(expression);
        }
        T realValue = value.getValue();
        if (realValue instanceof PolyString) {
            return beanMarshaller.marshalPolyString((PolyString) realValue);
        } else if (realValue instanceof PolyStringType) {   // should not occur ...
            return beanMarshaller.marshalPolyString(((PolyStringType) realValue).toPolyString());
        } else if (beanMarshaller.canProcess(typeName)) {
            XNodeImpl xnode = beanMarshaller.marshall(realValue, ctx);
            if (xnode == null) {
                // Marshaling attempt may process the expression in raw element
                expression = value.getExpression();
                if (expression != null) {
                    // Store expression, not the value. In this case the value (if any) is
                    // a transient product of the expression evaluation.
                    return createExpressionXNode(expression);
                }
                // HACK. Sometimes happens that we have raw value even with a definition
                // this is easy to work around
                if (value.isRaw()) {
                    xnode = ((XNodeImpl) value.getRawElement()).clone();
                } else {
                    throw new SchemaException("Cannot marshall property value "+value+": marshaller returned null");
                }
            }
            assert xnode != null;
            if (realValue != null && realValue.getClass().getPackage() != null) {
                TypeDefinition typeDef = getSchemaRegistry()
                        .findTypeDefinitionByCompileTimeClass(realValue.getClass(), TypeDefinition.class);
                if (typeDef != null && !QNameUtil.match(typeDef.getTypeName(), typeName)) {
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

    @NotNull
    private <T> XNodeImpl serializePropertyRawValue(PrismPropertyValue<T> value) {
        XNodeImpl rawElement = (XNodeImpl) value.getRawElement();
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

    private PrimitiveXNodeImpl<String> createPrimitiveXNodeStringAttr(String val) {
        return createPrimitiveXNodeAttr(val, DOMUtil.XSD_STRING);
    }

    private <T> PrimitiveXNodeImpl<T> createPrimitiveXNodeAttr(T val, QName type) {
        PrimitiveXNodeImpl<T> xprim = createPrimitiveXNode(val, type);
        xprim.setAttribute(true);
        return xprim;
    }

    @NotNull
    private <T> PrimitiveXNodeImpl<T> createPrimitiveXNode(T val, QName type) {
        PrimitiveXNodeImpl<T> xprim = new PrimitiveXNodeImpl<>();
        xprim.setValue(val, type);
        return xprim;
    }

    @NotNull
    private XNodeImpl createExpressionXNode(@NotNull ExpressionWrapper expression) throws SchemaException {
        return PrismUtilInternal.serializeExpression(expression, beanMarshaller);
    }

    @NotNull
    private SchemaRegistry getSchemaRegistry() {
        return beanMarshaller.getPrismContext().getSchemaRegistry();
    }

    private void addTypeDefinitionIfNeeded(@NotNull QName itemName, QName typeName, @NotNull XNodeImpl valueNode) {
        if (valueNode.getTypeQName() != null && valueNode.isExplicitTypeDeclaration()) {
            return; // already set
        }
        if (typeName == null) {
            return;    // nothing to do, anyway
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
