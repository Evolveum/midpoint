/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
