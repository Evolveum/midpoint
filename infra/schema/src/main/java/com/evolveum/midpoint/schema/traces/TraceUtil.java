/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.xml.bind.JAXBElement;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParamsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TraceType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.jetbrains.annotations.NotNull;

@Experimental
public class TraceUtil {

    @SuppressWarnings("unchecked")
    public static <T> T getTrace(OperationResultType result, Class<T> aClass) {
        for (TraceType trace : result.getTrace()) {
            if (aClass.isAssignableFrom(trace.getClass())) {
                return (T) trace;
            }
        }
        return null;
    }

    public static String getContext(OperationResultType opResult, String name) {
        if (opResult.getContext() != null) {
            for (EntryType e : opResult.getContext().getEntry()) {
                if (name.equals(e.getKey())) {
                    return dump(e.getEntryValue());
                }
            }
        }
        return "";
    }

    public static String getParameter(OperationResultType opResult, String name) {
        if (opResult.getParams() != null) {
            for (EntryType e : opResult.getParams().getEntry()) {
                if (name.equals(e.getKey())) {
                    return dump(e.getEntryValue());
                }
            }
        }
        return "";
    }

    public static List<JAXBElement<?>> selectByKey(ParamsType params, String key) {
        if (params != null) {
            return params.getEntry().stream()
                .filter(e -> key.equals(e.getKey()))
                .map(EntryType::getEntryValue)
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static @NotNull String getReturn(OperationResultType opResult, String name) {
        return String.join(", ", getReturnsAsStringList(opResult, name).toArray(new String[0]));
    }

    public static @NotNull List<String> getReturnsAsStringList(OperationResultType opResult, String name) {
        return asStringList(selectByKey(opResult.getReturns(), name));
    }

    public static @NotNull List<String> getParametersAsStringList(OperationResultType opResult, String name) {
        return asStringList(selectByKey(opResult.getParams(), name));
    }

    public static @NotNull List<String> asStringList(List<JAXBElement<?>> elements) {
        return elements.stream()
                .map(TraceUtil::dump)
                .collect(Collectors.toList());
    }

    public static String dump(JAXBElement<?> jaxb) {
        if (jaxb == null) {
            return "";
        }
        Object value = jaxb.getValue();
        if (value instanceof RawType) {
            return ((RawType) value).extractString();
        } else {
            return String.valueOf(value);
        }
    }

}
