/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.result;

/**
 * This may seems too simple and maybe pointless now. But we expect
 * that it may later evolve to something like future/promise.
 *
 * @author semancik
 */
public class AsynchronousOperationReturnValue<T> extends AsynchronousOperationResult {

    private T returnValue;

    public T getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(T returnValue) {
        this.returnValue = returnValue;
    }

    public static <T> AsynchronousOperationReturnValue<T> wrap(T returnValue, OperationResult result) {
        AsynchronousOperationReturnValue<T> ret = new AsynchronousOperationReturnValue<>();
        ret.setOperationResult(result);
        ret.setReturnValue(returnValue);
        return ret;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        super.shortDump(sb);
        sb.append(": ");
        sb.append(returnValue);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AsynchronousOperationReturnValue(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

}
