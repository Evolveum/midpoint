/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.schema.result.OperationResult;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class CallableResult<T> implements Serializable {

    public static final String F_VALUE = "value";
    public static final String F_RESULT = "result";

    private T value;
    private OperationResult result;

    public CallableResult() {
    }

    public CallableResult(T value, OperationResult result) {
        this.result = result;
        this.value = value;
    }

    public OperationResult getResult() {
        return result;
    }

    public T getValue() {
        return value;
    }

    public void setResult(OperationResult result) {
        this.result = result;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
