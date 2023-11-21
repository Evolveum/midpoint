/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import com.evolveum.midpoint.provisioning.impl.LazilyInitializableMixin;
import com.evolveum.midpoint.provisioning.ucf.api.UcfErrorState;
import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/** Error state of lazy-initializable objects, see {@link InitializationState} and {@link LazilyInitializableMixin}. */
public abstract class ErrorState implements Serializable {

    public static Ok ok() {
        return Ok.INSTANCE;
    }

    public static Error error(@NotNull Throwable exception) {
        return new Error(exception);
    }

    static NotApplicable notApplicable() {
        return NotApplicable.INSTANCE;
    }

    public static @NotNull ErrorState fromUcfErrorState(@NotNull UcfErrorState ucf) {
        if (ucf.isSuccess()) {
            return ok();
        } else {
            return error(ucf.getException());
        }
    }

    public boolean isOk() {
        return this instanceof Ok;
    }

    public boolean isError() {
        return this instanceof Error;
    }

    public boolean isNotApplicable() {
        return this instanceof NotApplicable;
    }

    public @Nullable Throwable getException() {
        return null;
    }

    /**
     * No restrictions for further processing are known (yet). After successful initialization,
     * the object can be used for full processing.
     */
    public static class Ok extends ErrorState {

        private static final Ok INSTANCE = new Ok();

        @Override
        public String toString() {
            return "OK";
        }
    }

    /**
     * The object is in error state. The further processing is limited. Usually,
     * only actions related to statistics keeping and error reporting should be done.
     */
    public static class Error extends ErrorState {

        @NotNull private final Throwable exception;

        private Error(@NotNull Throwable exception) {
            this.exception = exception;
        }

        public static Error of(@NotNull Throwable exception) {
            return new Error(exception);
        }

        public @NotNull Throwable getException() {
            return exception;
        }

        @Override
        public String toString() {
            return "ERROR: " + MiscUtil.getClassWithMessage(exception);
        }
    }

    /**
     * The object is without errors, but it is nevertheless not applicable for further processing.
     * (From the higher level view it is simply not relevant. From the lower level view it may miss some
     * crucial information - like a repository shadow.)
     *
     * Usually this state means that it should be passed further only for statistics-keeping purposes.
     */
    public static class NotApplicable extends ErrorState {

        private static final NotApplicable INSTANCE = new NotApplicable();

        @Override
        public String toString() {
            return "NOT APPLICABLE";
        }
    }
}
