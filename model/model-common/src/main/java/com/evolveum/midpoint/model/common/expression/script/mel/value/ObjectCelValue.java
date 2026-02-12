/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import com.evolveum.midpoint.model.common.expression.script.mel.DynType;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.util.exception.SchemaException;

import dev.cel.common.types.CelType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Radovan Semancik
 */
public class ObjectCelValue<O extends Objectable> extends AbstractContainerValueCelValue<O> implements MidPointValueProducer<PrismObject<O>> {

    public static final String CEL_TYPE_NAME = PrismObject.class.getName();
    public static final CelType CEL_TYPE = new DynType(CEL_TYPE_NAME);

    private final PrismObject<O> object;

    ObjectCelValue(PrismObject<O> object) {
        super(object.getValue());
        this.object = object;
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    public static <O extends Objectable> ObjectCelValue<O> create(PrismObject<O> object) {
        return new ObjectCelValue<>(object);
    }

    public Object value() {
        return object;
    }

    public PrismObject<O> getObject() {
        return object;
    }

    @Override
    public PrismObject<O> getJavaValue() {
        return object;
    }

    @Override
    public int size() { return super.size() + 1; }

    public boolean isEmpty() {
        return false;
    }

    @Override
    public Object get(Object key) {
        if (PrismConstants.ATTRIBUTE_OID_LOCAL_NAME.equals(key)) {
            return object.getOid();
        } else {
            return super.get(key);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        if (PrismConstants.ATTRIBUTE_OID_LOCAL_NAME.equals(key)) {
            return true;
        } else {
            return super.containsKey(key);
        }
    }

    @Override
    public @NotNull Set<String> keySet() {
        Set<String> keys = super.keySet();
        keys.add(PrismConstants.ATTRIBUTE_OID_LOCAL_NAME);
        return keys;
    }

    @Override
    public @NotNull Collection<Object> values() {
        Collection<Object> values = super.values();
        values.add(object.getOid());
        return values;
    }
}
