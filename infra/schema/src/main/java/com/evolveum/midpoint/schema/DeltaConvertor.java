/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static com.evolveum.midpoint.prism.PrismObject.asPrismObject;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.prism.xml.ns._public.types_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * Converts between "XML" (i.e. XML/JSON/YAML/bean) and "native" (ObjectDelta and company) form.
 *
 * The "XML" form has two sub-forms: ObjectDeltaType (types-3) and ObjectModificationType (api-types-3).
 *
 * @author semancik
 */
public class DeltaConvertor {

    /**
     * Object delta: XML (api-types-3) -> native
     */
    @NotNull
    public static <T extends Objectable> ObjectDelta<T> createObjectDelta(
            ObjectModificationType objectModification, Class<T> type, PrismContext prismContext)
            throws SchemaException {
        Validate.notNull(prismContext, "No prismContext in DeltaConvertor.createObjectDelta call");
        PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
        if (objectDefinition == null) {
            throw new SchemaException("No object definition for class " + type);
        }
        return createObjectDelta(objectModification, objectDefinition);
    }

    /**
     * Object delta: XML (api-types-3) -> native
     */
    @NotNull
    public static <T extends Objectable> ObjectDelta<T> createObjectDelta(
            ObjectModificationType objectModification, PrismObjectDefinition<T> objDef) throws SchemaException {
        ObjectDelta<T> objectDelta = objDef.getPrismContext().deltaFactory().object()
                .create(objDef.getCompileTimeClass(), ChangeType.MODIFY);

        objectDelta.setOid(objectModification.getOid());
        for (ItemDeltaType propMod : objectModification.getItemDelta()) {
            objectDelta.addModification(createItemDelta(propMod, objDef));
        }

        return objectDelta;
    }

    /**
     * Object delta: XML -> native
     */
    @NotNull
    public static <T extends Objectable> ObjectDelta<T> createObjectDelta(
            @NotNull ObjectDeltaType deltaBean, PrismContext ignored)
            throws SchemaException {
        return createObjectDelta(deltaBean);
    }

    @Contract("null -> null; !null -> !null")
    public static <T extends Objectable> ObjectDelta<T> createObjectDeltaNullable(@Nullable ObjectDeltaType objectDeltaBean)
            throws SchemaException {
        return objectDeltaBean != null ?
                createObjectDelta(objectDeltaBean) : null;
    }

    /**
     * Object delta: XML -> native
     */
    @NotNull
    public static <T extends Objectable> ObjectDelta<T> createObjectDelta(@NotNull ObjectDeltaType objectDeltaBean)
            throws SchemaException {
        PrismContext prismContext = PrismContext.get();
        QName objectTypeName =
                requireNonNull(objectDeltaBean.getObjectType(),
                        () -> "No objectType specified");

        PrismObjectDefinition<T> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(objectTypeName);
        Class<T> objectJavaType = objDef.getCompileTimeClass();

        if (objectDeltaBean.getChangeType() == ChangeTypeType.ADD) {
            ObjectDelta<T> objectDelta = prismContext.deltaFactory().object().create(objectJavaType, ChangeType.ADD);
            objectDelta.setOid(objectDeltaBean.getOid());
            //noinspection unchecked
            objectDelta.setObjectToAdd((PrismObject<T>) asPrismObject(objectDeltaBean.getObjectToAdd()));
            return objectDelta;
        } else if (objectDeltaBean.getChangeType() == ChangeTypeType.MODIFY) {
            ObjectDelta<T> objectDelta = prismContext.deltaFactory().object().create(objectJavaType, ChangeType.MODIFY);
            objectDelta.setOid(objectDeltaBean.getOid());
            for (ItemDeltaType propMod : objectDeltaBean.getItemDelta()) {
                objectDelta.addModification(createItemDelta(propMod, objDef));
            }
            return objectDelta;
        } else if (objectDeltaBean.getChangeType() == ChangeTypeType.DELETE) {
            ObjectDelta<T> objectDelta = prismContext.deltaFactory().object().create(objectJavaType, ChangeType.DELETE);
            objectDelta.setOid(objectDeltaBean.getOid());
            return objectDelta;
        } else {
            throw new SchemaException("Unknown change type " + objectDeltaBean.getChangeType());
        }
    }

    /**
     * Object delta operation: XML -> native.
     */
    public static ObjectDeltaOperation<?> createObjectDeltaOperation(ObjectDeltaOperationType odoBean,
            PrismContext prismContext) throws SchemaException {
        ObjectDeltaOperation<?> odo = new ObjectDeltaOperation<>(
                createObjectDelta(odoBean.getObjectDelta(), prismContext));
        if (odoBean.getExecutionResult() != null) {
            odo.setExecutionResult(OperationResult.createOperationResult(odoBean.getExecutionResult()));
        }
        if (odoBean.getObjectName() != null) {
            odo.setObjectName(odoBean.getObjectName().toPolyString());
        }
        odo.setResourceOid(odoBean.getResourceOid());
        if (odoBean.getResourceName() != null) {
            odo.setObjectName(odoBean.getResourceName().toPolyString());
        }
        odo.setObjectOid(odoBean.getObjectOid());
        odo.setShadowIntent(odoBean.getShadowIntent());
        odo.setShadowKind(odoBean.getShadowKind());
        return odo;
    }

    /**
     * Object delta: XML (api-types-3) -> native (ItemDelta collection)
     */
    public static <T extends Objectable> Collection<? extends ItemDelta<?, ?>> toModifications(
            ObjectModificationType objectModification, Class<T> type, PrismContext prismContext)
            throws SchemaException {
        argCheck(prismContext != null, "No prismContext in DeltaConvertor.toModifications call");
        PrismObjectDefinition<T> objectDefinition =
                requireNonNull(prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type),
                        () -> "No object definition for class " + type);

        return toModifications(objectModification, objectDefinition);
    }

    /**
     * Object delta: XML (api-types-3) -> native (ItemDelta collection)
     */
    public static <T extends Objectable> Collection<? extends ItemDelta<?, ?>> toModifications(
            ObjectModificationType objectModification, PrismObjectDefinition<T> objDef) throws SchemaException {
        return toModifications(objectModification.getItemDelta(), objDef);
    }

    /**
     * Item deltas: XML -> native
     */
    public static <T extends Objectable> Collection<? extends ItemDelta<?, ?>> toModifications(
            Collection<ItemDeltaType> itemDeltaBeans, PrismObjectDefinition<T> objDef) throws SchemaException {
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        for (ItemDeltaType propMod : itemDeltaBeans) {
            modifications.add(createItemDelta(propMod, objDef));
        }
        return modifications;
    }

    /**
     * Object delta: native -> XML (api-types-3).
     * Only for MODIFY deltas.
     */
    public static <T extends Objectable> ObjectModificationType toObjectModificationType(ObjectDelta<T> delta) throws SchemaException {
        argCheck(delta.getChangeType() == ChangeType.MODIFY,
                "Cannot produce ObjectModificationType from delta of type %s", delta.getChangeType());
        ObjectModificationType modBean = new ObjectModificationType();
        modBean.setOid(delta.getOid());
        List<ItemDeltaType> propModBeans = modBean.getItemDelta();
        for (ItemDelta<?, ?> propDelta : delta.getModifications()) {
            Collection<ItemDeltaType> propPropModTypes;
            try {
                propPropModTypes = toItemDeltaTypes(propDelta);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " in " + delta, e);
            }
            propModBeans.addAll(propPropModTypes);
        }
        return modBean;
    }

    /**
     * Object delta: native -> XML.
     */
    @Contract("null -> null; !null -> !null")
    public static ObjectDeltaType toObjectDeltaType(ObjectDelta<?> objectDelta) throws SchemaException {
        return objectDelta != null ? toObjectDeltaType(objectDelta, null) : null;
    }

    /**
     * Object delta: native -> XML.
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    public static ObjectDeltaType toObjectDeltaType(ObjectDelta<?> objectDelta, DeltaConversionOptions options)
            throws SchemaException {
        if (objectDelta == null) {
            return null;
        }
        ObjectDeltaType objectDeltaBean = new ObjectDeltaType();
        objectDeltaBean.setChangeType(convertChangeType(objectDelta.getChangeType()));
        Class<? extends Objectable> javaType = objectDelta.getObjectTypeClass();
        PrismObjectDefinition<? extends Objectable> objDef =
                requireNonNull(PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(javaType),
                        () -> "Unknown compile time class: " + javaType);
        objectDeltaBean.setObjectType(objDef.getTypeName());
        objectDeltaBean.setOid(objectDelta.getOid());

        if (objectDelta.getChangeType() == ChangeType.ADD) {
            ObjectType objectBean = (ObjectType) PrismObject.asObjectable(objectDelta.getObjectToAdd());
            objectDeltaBean.setObjectToAdd(objectBean);
        } else if (objectDelta.getChangeType() == ChangeType.MODIFY) {
            for (ItemDelta<?, ?> propDelta : objectDelta.getModifications()) {
                Collection<ItemDeltaType> propPropModTypes;
                try {
                    propPropModTypes = toItemDeltaTypes(propDelta, options);
                } catch (SchemaException e) {
                    throw new SchemaException(e.getMessage() + " in " + objectDelta, e);
                }
                objectDeltaBean.getItemDelta().addAll(propPropModTypes);
            }
        } else if (objectDelta.getChangeType() == ChangeType.DELETE) {
            // Nothing to do
        } else {
            throw new SystemException("Unknown changeType " + objectDelta.getChangeType());
        }
        return objectDeltaBean;
    }

    /**
     * Object delta: native -> serialized XML.
     */
    public static String serializeDelta(
            ObjectDelta<? extends com.evolveum.prism.xml.ns._public.types_3.ObjectType> delta,
            DeltaConversionOptions options,
            @NotNull String language)
            throws SchemaException {
        ObjectDeltaType objectDeltaType = toObjectDeltaType(delta, options);
        return serializeDelta(objectDeltaType, options, language);
    }

    public static String serializeDelta(
            ObjectDeltaType objectDeltaType, DeltaConversionOptions options, @NotNull String language)
            throws SchemaException {
        SerializationOptions serializationOptions = new SerializationOptions()
                .skipTransient(true)
                .skipWhitespaces(true)
                .serializeReferenceNames(DeltaConversionOptions.isSerializeReferenceNames(options))
                .escapeInvalidCharacters(DeltaConversionOptions.isEscapeInvalidCharacters(options));
        return PrismContext.get().serializerFor(language)
                .options(serializationOptions)
                .serializeRealValue(objectDeltaType, SchemaConstants.T_OBJECT_DELTA);
    }

    /**
     * Object delta operation: native -> XML.
     */
    public static ObjectDeltaOperationType toObjectDeltaOperationType(ObjectDeltaOperation<?> objectDeltaOperation)
            throws SchemaException {
        return toObjectDeltaOperationType(objectDeltaOperation, null);
    }

    /**
     * Object delta operation: native -> XML.
     */
    public static ObjectDeltaOperationType toObjectDeltaOperationType(ObjectDeltaOperation<?> odo, DeltaConversionOptions options)
            throws SchemaException {
        ObjectDeltaOperationType odoBean = new ObjectDeltaOperationType();
        toObjectDeltaOperationType(odo, odoBean, options);
        return odoBean;
    }

    /**
     * Object delta operation: native -> XML (passed as parameter).
     * Quite dubious.
     */
    public static void toObjectDeltaOperationType(ObjectDeltaOperation<?> odo, ObjectDeltaOperationType odoBean,
            DeltaConversionOptions options) throws SchemaException {
        odoBean.setObjectDelta(DeltaConvertor.toObjectDeltaType(odo.getObjectDelta(), options));
        if (odo.getExecutionResult() != null) {
            odoBean.setExecutionResult(odo.getExecutionResult().createBeanReduced());
        }
        if (odo.getObjectName() != null) {
            odoBean.setObjectName(new PolyStringType(odo.getObjectName()));
        }
        odoBean.setResourceOid(odo.getResourceOid());
        if (odo.getResourceName() != null) {
            odoBean.setResourceName(new PolyStringType(odo.getResourceName()));
        }
        odoBean.setShadowKind(odo.getShadowKind());
        odoBean.setShadowIntent(odo.getShadowIntent());
        odoBean.setObjectOid(odo.getObjectOid());
    }

    private static ChangeTypeType convertChangeType(ChangeType changeType) {
        if (changeType == ChangeType.ADD) {
            return ChangeTypeType.ADD;
        }
        if (changeType == ChangeType.MODIFY) {
            return ChangeTypeType.MODIFY;
        }
        if (changeType == ChangeType.DELETE) {
            return ChangeTypeType.DELETE;
        }
        throw new SystemException("Unknown changeType " + changeType);
    }

    /**
     * Item delta: XML -> native.
     *
     * Creates delta from ItemDeltaType (XML). The values inside the ItemDeltaType are converted to java.
     * That's the reason this method needs schema and objectType (to locate the appropriate definitions).
     */
    public static <IV extends PrismValue, ID extends ItemDefinition<?>> ItemDelta<IV, ID> createItemDelta(ItemDeltaType propMod,
            Class<? extends Containerable> objectType, PrismContext prismContext) throws SchemaException {
        argCheck(prismContext != null, "No prismContext in DeltaConvertor.createItemDelta call");
        PrismContainerDefinition<? extends Containerable> objectDefinition = prismContext.getSchemaRegistry().
                findContainerDefinitionByCompileTimeClass(objectType);
        return createItemDelta(propMod, objectDefinition);
    }

    /**
     * Item delta: XML -> native.
     *
     * @param propMod The "XML" form.
     * @param rootPcd Root prism container definition. The root is where the delta path starts.
     */
    public static <IV extends PrismValue, ID extends ItemDefinition<?>> ItemDelta<IV, ID> createItemDelta(
            @NotNull ItemDeltaType propMod, @NotNull PrismContainerDefinition<?> rootPcd) throws SchemaException {
        return new ItemDeltaBeanToNativeConversion<IV, ID>(propMod, rootPcd)
                .convert();
    }

    /**
     * Item delta: native -> XML.
     */
    public static Collection<ItemDeltaType> toItemDeltaTypes(ItemDelta<?, ?> delta) throws SchemaException {
        return toItemDeltaTypes(delta, null);
    }

    /**
     * Item delta: native -> XML.
     */
    public static Collection<ItemDeltaType> toItemDeltaTypes(ItemDelta<?, ?> delta, DeltaConversionOptions options)
            throws SchemaException {
        if (InternalsConfig.consistencyChecks) {
            delta.checkConsistence();
        }
        if (!delta.isEmpty() && delta.getPrismContext() == null) {
            throw new IllegalStateException("Non-empty ItemDelta with no prismContext cannot be converted to ItemDeltaType.");
        }
        Collection<ItemDeltaType> deltaBeans = new ArrayList<>();
        ItemPathType pathBean = new ItemPathType(delta.getPath());
        if (delta.getValuesToReplace() != null) {
            ItemDeltaType mod = new ItemDeltaType();
            mod.setPath(pathBean);
            mod.setModificationType(ModificationTypeType.REPLACE);
            try {
                addModValues(delta, mod, delta.getValuesToReplace(), options);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting item " + delta.getElementName(), e);
            }
            addOldValues(delta, mod, delta.getEstimatedOldValues(), options);
            deltaBeans.add(mod);
        }
        if (delta.getValuesToAdd() != null) {
            ItemDeltaType mod = new ItemDeltaType();
            mod.setPath(pathBean);
            mod.setModificationType(ModificationTypeType.ADD);
            try {
                addModValues(delta, mod, delta.getValuesToAdd(), options);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting item " + delta.getElementName(), e);
            }
            addOldValues(delta, mod, delta.getEstimatedOldValues(), options);
            deltaBeans.add(mod);
        }
        if (delta.getValuesToDelete() != null) {
            ItemDeltaType mod = new ItemDeltaType();
            mod.setPath(pathBean);
            mod.setModificationType(ModificationTypeType.DELETE);
            try {
                addModValues(delta, mod, delta.getValuesToDelete(), options);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting item " + delta.getElementName(), e);
            }
            addOldValues(delta, mod, delta.getEstimatedOldValues(), options);
            deltaBeans.add(mod);
        }
        return deltaBeans;
    }

    // requires delta.prismContext to be set
    private static void addModValues(ItemDelta<?, ?> delta, ItemDeltaType mod, Collection<? extends PrismValue> values,
            DeltaConversionOptions options) throws SchemaException {
        addModOrOldValues(delta, values, options, mod.getValue());
    }

    // requires delta.prismContext to be set
    private static void addOldValues(ItemDelta<?, ?> delta, ItemDeltaType mod, Collection<? extends PrismValue> values,
            DeltaConversionOptions options) throws SchemaException {
        addModOrOldValues(delta, values, options, mod.getEstimatedOldValue());
    }

    private static void addModOrOldValues(ItemDelta<?, ?> delta, Collection<? extends PrismValue> values,
            DeltaConversionOptions options, @NotNull List<RawType> targetCollection) throws SchemaException {
        if (values != null && !values.isEmpty()) {
            for (PrismValue value : values) {
                XNode xnode = toXNode(delta, value, options);
                RawType modValue = new RawType(xnode, value.getPrismContext());
                targetCollection.add(modValue);
            }
        }
    }

    private static XNode toXNode(ItemDelta<?, ?> delta, @NotNull PrismValue value, DeltaConversionOptions options)
            throws SchemaException {
        var ret = delta.getPrismContext().xnodeSerializer()
                .definition(delta.getDefinition())
                .options(DeltaConversionOptions.isSerializeReferenceNames(options) ?
                        SerializationOptions.createSerializeReferenceNames() : null)
                .serialize(value)
                .getSubnode();
        ret.freeze();
        return ret;
    }

    /**
     * Object deltas: XML (api-types-3) -> native
     *
     * Dubious. Consider removal.
     */
    public static Collection<ObjectDelta<? extends com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType>>
    createObjectDeltas(ObjectDeltaListType deltaList, PrismContext prismContext)
            throws SchemaException {
        List<ObjectDelta<? extends com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType>> retval = new ArrayList<>();
        for (ObjectDeltaType deltaType : deltaList.getDelta()) {
            retval.add(createObjectDelta(deltaType, prismContext));
        }
        return retval;
    }
}
