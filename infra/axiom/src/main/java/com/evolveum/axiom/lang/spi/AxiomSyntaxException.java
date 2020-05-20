/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.spi;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.google.common.base.Strings;

public class AxiomSyntaxException extends RuntimeException {


    private final SourceLocation source;

    public AxiomSyntaxException(@Nullable final SourceLocation source, String message, @Nullable final Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public AxiomSyntaxException(SourceLocation source2, String message) {
        this(source2, message, null);
    }

    public final Optional<SourceLocation> getSource() {
        return Optional.ofNullable(source);
    }

    public String getFormattedMessage() {
        final StringBuilder sb = new StringBuilder();
        if (source != null) {
            sb.append(source);
        }

        return sb.append(getMessage()).toString();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + ": " + getFormattedMessage();
    }

    public static void check(boolean test, SourceLocation source, String format, Object... args) {
        if(!test) {
            throw new AxiomSyntaxException(source, Strings.lenientFormat(format, args));
        }
    }


}
