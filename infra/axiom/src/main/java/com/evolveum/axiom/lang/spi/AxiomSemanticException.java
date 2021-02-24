package com.evolveum.axiom.lang.spi;

import com.evolveum.concepts.SourceLocation;
import com.google.common.base.Strings;

public class AxiomSemanticException extends RuntimeException {

    private final SourceLocation source;

    public AxiomSemanticException(SourceLocation source, String message, Throwable cause) {
        super(source + ":" + message, cause);
        this.source = source;
    }

    public AxiomSemanticException(SourceLocation source, String message) {
        this(source, message, null);
    }

    public static void check(boolean check, SourceLocation start, String format, Object... arguments) {
        if(!check) {
            throw create(start, format, arguments);
        }
    }

    public static AxiomSemanticException create(SourceLocation start, String format, Object... arguments) {
        return new AxiomSemanticException(start, Strings.lenientFormat(format, arguments));
    }

}
