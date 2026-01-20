/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.prism.polystring.PolyString;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.*;

import java.util.Optional;

/**
 * @author Radovan Semancik
 */
public class MidPointTypeProvider implements CelTypeProvider {

    public static final String POLYSTRING_PACKAGE_NAME = PolyString.class.getTypeName();
    private static final String POLYSTRING_ORIG = PolyString.F_ORIG.getLocalPart();
    private static final String POLYSTRING_NORM = PolyString.F_NORM.getLocalPart();
    public static final CelType POLYSTRING_TYPE = createPolystringType();

    private final PrismContext prismContext;
    private ImmutableList<CelType> types;

    public MidPointTypeProvider(PrismContext prismContext) {
        this.prismContext = prismContext;
        types = ImmutableList.of(POLYSTRING_TYPE);
    }

    @Override
    public ImmutableCollection<CelType> types() {
        return types;
    }

    @Override
    public Optional<CelType> findType(String typeName) {
        return types.stream().filter(type -> type.name().equals(typeName)).findAny();
    }

    private static CelType createPolystringType() {
//        return OpaqueType.create(POLYSTRING_PACKAGE_NAME);
        ImmutableSet<String> fieldNames = ImmutableSet.of(POLYSTRING_ORIG, POLYSTRING_NORM);
        StructType.FieldResolver fieldResolver = fieldName -> {
            if (POLYSTRING_ORIG.equals(fieldName) || POLYSTRING_NORM.equals(fieldName)) {
                return Optional.of(SimpleType.STRING);
            } else {
                throw new IllegalStateException("Illegal request for polystring field " + fieldName);
            }
        };
        return StructType.create(POLYSTRING_PACKAGE_NAME, fieldNames, fieldResolver);
    }

}
