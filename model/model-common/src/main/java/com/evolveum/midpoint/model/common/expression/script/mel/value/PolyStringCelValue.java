/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.util.Map;
import java.util.Optional;

import com.evolveum.midpoint.model.common.expression.script.mel.MelComparable;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.CelType;
import dev.cel.common.types.SimpleType;
import dev.cel.common.types.StructType;

import com.evolveum.midpoint.prism.polystring.PolyString;

/**
 * @author Radovan Semancik
 */
public class PolyStringCelValue extends AbstractStructuredCelValue<String> implements MidPointValueProducer<PolyString>, MelComparable {

    public static final String POLYSTRING_PACKAGE_NAME = PolyString.class.getTypeName();
    private static final String F_ORIG = PolyString.F_ORIG.getLocalPart();
    private static final String F_NORM = PolyString.F_NORM.getLocalPart();
    public static final CelType CEL_TYPE = createCelType();

    private final PolyString polystring;

    PolyStringCelValue(PolyString polystring) {
        this.polystring = polystring;
    }

    public static PolyStringCelValue create(PolyString polystring) {
        return new PolyStringCelValue(polystring);
    }

    protected Map<String, String> createMapValue() {
        return Map.of(
                F_ORIG, polystring.getOrig(),
                F_NORM, polystring.getNorm()
        );
    }

    @Override
    public PolyString getJavaValue() {
        return polystring;
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    public PolyString getPolystring() {
        return polystring;
    }

    public String getOrig() {
        return polystring.getOrig();
    }

    public String getNorm() {
        return polystring.getNorm();
    }

    private static CelType createCelType() {
        ImmutableSet<String> fieldNames = ImmutableSet.of(F_ORIG, F_NORM);
        StructType.FieldResolver fieldResolver = fieldName -> {
            if (F_ORIG.equals(fieldName) || F_NORM.equals(fieldName)) {
                return Optional.of(SimpleType.STRING);
            } else {
                throw new IllegalStateException("Illegal request for polystring field " + fieldName);
            }
        };
        return StructType.create(POLYSTRING_PACKAGE_NAME, fieldNames, fieldResolver);
    }

    @Override
    public boolean melEquals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof PolyStringCelValue celPs) {
            other = celPs.getOrig();
        }
        if (other instanceof String str) {
            return getOrig().equals(str);
        }
        return false;
    }

}
