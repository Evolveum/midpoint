/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel.value;

import com.evolveum.midpoint.model.common.expression.script.cel.CelTypeMapper;
import com.evolveum.midpoint.model.common.expression.script.cel.DynType;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.exception.SchemaException;

import dev.cel.common.types.CelType;
import dev.cel.common.values.CelValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Radovan Semancik
 */
public class ContainerValueCelValue<C extends Containerable> extends AbstractContainerValueCelValue<C> implements MidPointValueProducer<PrismContainerValue<C>> {

    public static final String CEL_TYPE_NAME = PrismContainerValue.class.getName();
    public static final CelType CEL_TYPE = new DynType(CEL_TYPE_NAME);

    ContainerValueCelValue(PrismContainerValue<C> containerValue) {
        super(containerValue);
    }

    public static <C extends Containerable> ContainerValueCelValue<C> create(PrismContainerValue<C> containerValue) {
        return new ContainerValueCelValue<>(containerValue);
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    @Override
    public PrismContainerValue<C> getJavaValue() {
        return getContainerValue();
    }
}
