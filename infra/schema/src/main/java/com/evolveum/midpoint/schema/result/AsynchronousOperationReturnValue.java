/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.result;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This may seems too simple and maybe pointless now. But we expect
 * that it may later evolve to something like future/promise.
 *
 * @author semancik
 */
public class AsynchronousOperationReturnValue<T> extends AsynchronousOperationResult {

    @Nullable private final T returnValue;

    public AsynchronousOperationReturnValue(@Nullable T returnValue, @NotNull OperationResult operationResult) {
        super(operationResult);
        this.returnValue = returnValue;
    }

    public @Nullable T getReturnValue() {
        return returnValue;
    }

    public static <T> AsynchronousOperationReturnValue<T> wrap(@Nullable T returnValue, @NotNull OperationResult result) {
        return new AsynchronousOperationReturnValue<>(returnValue, result);
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
