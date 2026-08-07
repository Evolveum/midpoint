/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.util.*;

import com.evolveum.midpoint.model.common.expression.script.mel.MelException;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import com.evolveum.prism.xml.ns._public.types_3.ObjectType;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.*;
import dev.cel.common.values.NullValue;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 */
public class ObjectDeltaCelValue<O extends ObjectType> extends AbstractStructuredCelValue<Object> implements MidPointValueProducer<ObjectDelta<O>> {

    public static final String OBJECT_DELTA_PACKAGE_NAME = ObjectDeltaType.class.getTypeName();
    private static final String F_OID = "oid";
//    private static final String F_OID = ObjectDeltaType.F_OID.getLocalPart();
    private static final String F_CHANGE_TYPE = ObjectDeltaType.F_CHANGE_TYPE.getLocalPart();
    private static final String F_OBJECT_TYPE = ObjectDeltaType.F_OBJECT_TYPE.getLocalPart();
    private static final String F_OBJECT_TO_ADD = ObjectDeltaType.F_OBJECT_TO_ADD.getLocalPart();
    private static final String F_ITEM_DELTA = ObjectDeltaType.F_ITEM_DETLA.getLocalPart();
    public static final CelType CEL_TYPE = createCelType();

    private final ObjectDelta<O> objectDelta;
    private QName objectTypeQName;

    ObjectDeltaCelValue(ObjectDelta<O> objectDelta) {
        this.objectDelta = objectDelta;
    }

    public static <O extends ObjectType> ObjectDeltaCelValue<O> create(ObjectDeltaType objectDeltaType) {
        ObjectDeltaCelValue<O> celVal = null;
        try {
            celVal = create(DeltaConvertor.createObjectDelta(objectDeltaType, PrismContext.get()));
        } catch (SchemaException e) {
            throw new MelException(e.getMessage(), e);
        }
        celVal.objectTypeQName = objectDeltaType.getObjectType();
        return celVal;
    }

    public static <O extends ObjectType> ObjectDeltaCelValue<O> create(ObjectDelta<O> objectDelta) {
        return new ObjectDeltaCelValue<O>(objectDelta);
    }

    protected Map<String, Object> createMapValue() {
        Map<String, Object> value = new HashMap<>();
        value.put(F_OID, wrap(objectDelta.getOid()));
        value.put(F_CHANGE_TYPE, wrapChangeType(objectDelta.getChangeType()));
        value.put(F_OBJECT_TYPE, wrapQName(determineType(objectDelta.getObjectTypeClass())));
        value.put(F_OBJECT_TO_ADD, wrapPrismObject(objectDelta.getObjectToAdd()));
        value.put(F_ITEM_DELTA, wrapItemDeltas(objectDelta.getModifications()));
        return value;
    }

    private QName determineType(Class<O> objectTypeClass) {
        if (objectTypeQName != null) {
            PrismObjectDefinition<O> objDef =
                    PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(objectTypeClass);
            objectTypeQName = objDef.getTypeName();
        }
        return objectTypeQName;
    }

    private Object wrapItemDeltas(Collection<? extends ItemDelta<?, ?>> itemDeltas) {
        if (itemDeltas == null) {
            return NullValue.NULL_VALUE;
        }
        return itemDeltas.stream().map(this::wrapItemDelta).toList();
    }

    private Object wrapItemDelta(ItemDelta<?, ?> itemDelta) {
        return itemDelta == null ? NullValue.NULL_VALUE : ItemDeltaCelValue.create(itemDelta);
    }

    private Object wrapChangeType(ChangeType changeType) {
        return changeType == null ? NullValue.NULL_VALUE : ChangeType.toChangeTypeType(changeType).value();
    }

    @Override
    public ObjectDelta<O> getJavaValue() {
        return objectDelta;
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    private static CelType createCelType() {
        final ImmutableSet<String> fieldNames = ImmutableSet.of(F_OID, F_CHANGE_TYPE, F_OBJECT_TYPE,
                F_OBJECT_TO_ADD, F_ITEM_DELTA);
        StructType.FieldResolver fieldResolver = fieldName -> {
            if (F_OID.equals(fieldName) || F_CHANGE_TYPE.equals(fieldName)) {
                return Optional.of(NullableType.create(SimpleType.STRING));
            } else if (F_OBJECT_TYPE.equals(fieldName)) {
                return Optional.of(NullableType.create(QNameCelValue.CEL_TYPE));
            } else if (F_OBJECT_TO_ADD.equals(fieldName)) {
                return Optional.of(NullableType.create(ObjectCelValue.CEL_TYPE));
            } else if (F_ITEM_DELTA.equals(fieldName)) {
                return Optional.of(ListType.create(ItemDeltaCelValue.CEL_TYPE));
            } else {
                throw new IllegalStateException("Illegal request for ObjectDeltaOperation field " + fieldName);
            }
        };
        return StructType.create(OBJECT_DELTA_PACKAGE_NAME, fieldNames, fieldResolver);
    }

}
