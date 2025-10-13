/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

public class ExecuteScriptArgument {


    private String argumentName;
    /**
     * NOTE! This may contain both Object (for single-value arguments) and Collection<Object> (for multi-value arguments).
     */
    private Object argumentValue;

    public ExecuteScriptArgument() {

    }

    public ExecuteScriptArgument(String name, Object value) {
        this.argumentName = name;
        this.argumentValue = value;
    }

    public String getArgumentName() {
        return argumentName;
    }

    public void setArgumentName(String argumentName) {
        this.argumentName = argumentName;
    }

    public Object getArgumentValue() {
        return argumentValue;
    }

    public void setArgumentValue(Object argumentValue) {
        this.argumentValue = argumentValue;
    }
}
