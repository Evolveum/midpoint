package com.evolveum.axiom.lang.impl;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

public class AxiomSyntaxException extends Exception {


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

}
