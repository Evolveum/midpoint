package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.stmt.SourceLocation;
import com.google.common.base.Strings;

public class AxiomSemanticException extends RuntimeException {



    public AxiomSemanticException(String message, Throwable cause) {
        super(message, cause);
    }

    public AxiomSemanticException(String message) {
        super(message);
    }

    public AxiomSemanticException(Throwable cause) {
        super(cause);
    }

    public AxiomSemanticException(AxiomItemDefinition definition, String message) {
        super(definition.toString() + " " + message);
    }

    public static <V> V checkNotNull(V value, AxiomItemDefinition definition, String message) throws AxiomSemanticException {
        if(value == null) {
            throw new AxiomSemanticException(definition, message);
        }
        return value;
    }

    public static void check(boolean check, SourceLocation start, String format, Object... arguments) {
        if(!check) {
            throw new AxiomSemanticException(start + Strings.lenientFormat(format, arguments));
        }
        ;
    }

}
