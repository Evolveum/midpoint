/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.value;

import com.evolveum.midpoint.model.common.expression.script.cel.DynType;
import com.evolveum.midpoint.model.common.expression.script.cel.MidPointTypeProvider;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;

import dev.cel.common.types.CelType;
import dev.cel.common.values.CelValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Radovan Semancik
 */
public class ObjectCelValue<O extends Objectable> extends ContainerValueCelValue<O> {

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
}
