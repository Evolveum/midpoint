/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.model.common.expression.script.cel.value.ContainerValueCelValue;
import com.evolveum.midpoint.model.common.expression.script.cel.value.ObjectCelValue;
import com.evolveum.midpoint.model.common.expression.script.cel.value.ObjectReferenceCelValue;
import com.evolveum.midpoint.model.common.expression.script.cel.value.PolyStringCelValue;
import com.evolveum.midpoint.prism.PrismContext;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import dev.cel.common.types.*;

import java.util.Optional;

/**
 * @author Radovan Semancik
 */
public class MidPointTypeProvider implements CelTypeProvider {

    private final PrismContext prismContext;
    private ImmutableList<CelType> types;

    public MidPointTypeProvider(PrismContext prismContext) {
        this.prismContext = prismContext;
        types = ImmutableList.of(
                PolyStringCelValue.CEL_TYPE,
                ObjectReferenceCelValue.CEL_TYPE,
                ObjectCelValue.CEL_TYPE,
                ContainerValueCelValue.CEL_TYPE
        );
    }

    @Override
    public ImmutableCollection<CelType> types() {
        return types;
    }

    @Override
    public Optional<CelType> findType(String typeName) {
        return types.stream().filter(type -> type.name().equals(typeName)).findAny();
    }

}
