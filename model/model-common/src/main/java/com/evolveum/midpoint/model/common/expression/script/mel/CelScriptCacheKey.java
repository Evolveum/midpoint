/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel;

import dev.cel.common.types.CelType;

import java.util.Map;
import java.util.Objects;

public class CelScriptCacheKey {

    private final String codeString;
    private final Map<String, CelType> variableTypeMap;
    private final CelType resultType;

    public CelScriptCacheKey(String codeString, Map<String, CelType> variableTypeMap, CelType resultType) {
        this.codeString = codeString;
        this.variableTypeMap = variableTypeMap;
        this.resultType = resultType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        CelScriptCacheKey that = (CelScriptCacheKey) o;
        return Objects.equals(codeString, that.codeString) && Objects.equals(variableTypeMap, that.variableTypeMap) && Objects.equals(resultType, that.resultType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codeString, variableTypeMap, resultType);
    }
}
