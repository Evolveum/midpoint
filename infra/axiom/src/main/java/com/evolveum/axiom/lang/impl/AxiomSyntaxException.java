/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.google.common.base.Strings;

public class AxiomSyntaxException extends RuntimeException {


    private final String source;
    private final int line;
    private final int charPositionInLine;

    public AxiomSyntaxException(@Nullable final String source, final int line,
            final int charPositionInLine, final String message) {
        this(source, line, charPositionInLine, message, null);
    }

    public AxiomSyntaxException(@Nullable final String source, final int line,
            final int charPositionInLine, final String message, @Nullable final Throwable cause) {
        super(message, cause);
        this.source = source;
        this.line = line;
        this.charPositionInLine = charPositionInLine;
    }

    public final Optional<String> getSource() {
        return Optional.ofNullable(source);
    }

    public final int getLine() {
        return line;
    }

    public final int getCharPositionInLine() {
        return charPositionInLine;
    }

    public String getFormattedMessage() {
        final StringBuilder sb = new StringBuilder(getMessage());
        if (source != null) {
            sb.append(" in source ");
            sb.append(source);
        }
        if (line != 0) {
            sb.append(" on line ");
            sb.append(line);
            if (charPositionInLine != 0) {
                sb.append(" character ");
                sb.append(charPositionInLine);
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + ": " + getFormattedMessage();
    }

    public static void check(boolean test, String source, int line, int posInLine, String format, Object... args) {
        if(!test) {
            throw new AxiomSyntaxException(source, line, posInLine, Strings.lenientFormat(format, args));
        }

    }

}
