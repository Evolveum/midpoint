/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.functions;

/**
 * @author semancik
 *
 */
public class FunctionLibrary {

    private String variableName;
    private String namespace;
    private Object genericFunctions;

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Object getGenericFunctions() {
        return genericFunctions;
    }

    public void setGenericFunctions(Object genericFunctions) {
        this.genericFunctions = genericFunctions;
    }

    @Override
    public String toString() {
        return "FunctionLibrary{" +
                "variableName='" + variableName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", genericFunctions=" + genericFunctions +
                '}';
    }
}
