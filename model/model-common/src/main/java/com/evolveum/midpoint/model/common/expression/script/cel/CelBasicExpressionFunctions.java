/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.script.cel.value.ContainerValueCelValue;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import dev.cel.common.types.CelType;
import dev.cel.common.values.CelValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class CelBasicExpressionFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(CelBasicExpressionFunctions.class);

    private final BasicExpressionFunctions implementation;

    public CelBasicExpressionFunctions(BasicExpressionFunctions implementation) {
        this.implementation = implementation;
    }

    public String lc(String orig) {
        return BasicExpressionFunctions.lc(orig);
    }

    public String uc(String orig) {
        return BasicExpressionFunctions.uc(orig);
    }

    public boolean contains(Object object, Object search) {
        return implementation.contains(object, search);
    }

    public boolean containsIgnoreCase(Object object, Object search) {
        return implementation.containsIgnoreCase(object, search);
    }

    public String trim(String orig) {
        return BasicExpressionFunctions.trim(orig);
    }

    public String norm(Object orig) {
        if (orig == null) {
            return null;
        }
        if (orig instanceof PolyString) {
            return implementation.norm((PolyString)orig);
        }
        if (orig instanceof String) {
            return implementation.norm((String)orig);
        }
        return implementation.norm(orig.toString());
    }

    public String concatNameO(Object arguments) {
        LOGGER.info("CCCCCCC: concatName {}", arguments);
        if (arguments == null) {
            return null;
        }
        if (arguments instanceof List) {
            return implementation.concatName(((List)arguments).toArray());
        } else {
            throw new IllegalArgumentException("Unsupported argument type "+arguments+" ("+arguments.getClass()+") in concatName");
        }
    }

    public String concatName(List<Object> components) {
        LOGGER.info("CCCCCCL: concatName {}", components);
        if (components == null) {
            return null;
        }
        return implementation.concatName(components.toArray());
    }

    public String stringify(Object whatever) {
        LOGGER.info("SSSSSS: stringify {}", whatever);
        return implementation.stringify(whatever);
    }

    public <T> @NotNull Collection<T> getExtensionPropertyValues(
            Containerable containerable, String namespace, @NotNull String localPart) {
        return implementation.getExtensionPropertyValues(containerable, namespace, localPart);
    }

//    public <T> @NotNull Collection<T> getExtensionPropertyValues(
//            ContainerValueCelValue containerCelValue, @NotNull groovy.namespace.QName propertyQname) {
////        TODO
//    }

    public <T> @Nullable T getExtensionPropertyValue(
            Containerable containerable, String namespace, @NotNull String localPart) throws SchemaException {
        return implementation.getExtensionPropertyValue(containerable, namespace, localPart);
    }

}
