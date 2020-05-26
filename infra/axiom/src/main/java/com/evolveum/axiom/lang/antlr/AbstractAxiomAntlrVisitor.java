/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.antlr;

import java.util.Optional;
import java.util.Set;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.antlr.AxiomParser.ArgumentContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.IdentifierContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.StatementContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.StringContext;
import com.evolveum.axiom.lang.api.AxiomItemStream;
import com.evolveum.axiom.lang.spi.SourceLocation;

public abstract class AbstractAxiomAntlrVisitor<T> extends AxiomBaseVisitor<T> {

    private final Optional<Set<AxiomIdentifier>> limit;
    private final String sourceName;

    public AbstractAxiomAntlrVisitor(String name, Set<AxiomIdentifier> limit) {
        this.sourceName = name;
        this.limit = Optional.ofNullable(limit);
    }

    private AxiomIdentifier statementIdentifier(IdentifierContext identifier) {
        String prefix = nullableText(identifier.prefix());
        String localName = identifier.localIdentifier().getText();
        return resolveItemName(prefix, localName);
    }


    protected abstract AxiomItemStream.Target delegate();
    protected abstract AxiomIdentifier resolveItemName(String prefix, String localName);
    protected abstract AxiomIdentifier resolveArgument(String prefix, String localName);

    private String nullableText(ParserRuleContext prefix) {
        return prefix != null ? prefix.getText() : "";
    }

    @Override
    public final T visitStatement(StatementContext ctx) {
        AxiomIdentifier identifier = statementIdentifier(ctx.identifier());
        if(canEmit(identifier)) {
            SourceLocation start = sourceLocation(ctx.identifier().start);
            delegate().startItem(identifier, start);

            ArgumentContext argument = ctx.argument();
            final Object value;
            final SourceLocation valueStart;
            if(argument != null) {
                value = convert(argument);
                valueStart = sourceLocation(argument.start);
            } else {
                value = null;
                valueStart = start;
            }
            delegate().startValue(value, valueStart);
            T ret = super.visitStatement(ctx);
            delegate().endValue(sourceLocation(ctx.stop));
            delegate().endItem(sourceLocation(ctx.stop));
            return ret;
        }
        return defaultResult();
    }

    private Object convert(ArgumentContext ctx) {
        if (ctx.identifier() != null) {
            return (convert(ctx.identifier()));
        } else {
            return (convert(ctx.string()));
        }
    }

    private boolean canEmit(AxiomIdentifier identifier) {
        if (limit.isPresent()) {
            return limit.get().contains(identifier);
        }
        return true;
    }

    @Override
    public final T visitArgument(ArgumentContext ctx) {

        return defaultResult();
    }

    private AxiomIdentifier convert(IdentifierContext argument) {
        return argumentIdentifier(argument);
    }

    private AxiomIdentifier argumentIdentifier(IdentifierContext identifier) {
        String prefix = nullableText(identifier.prefix());
        String localName = identifier.localIdentifier().getText();
        return resolveArgument(prefix, localName);
    }


    private SourceLocation sourceLocation(Token start) {
        return SourceLocation.from(sourceName, start.getLine(), start.getCharPositionInLine());
    }

    static String convert(StringContext string) {
        if(string.singleQuoteString() != null) {
            return convertSingleQuote(string.singleQuoteString().getText());
        }
        if(string.doubleQuoteString() != null) {
            return covertDoubleQuote(string.doubleQuoteString().getText());
        }
        return convertMultiline(string.multilineString().getText());
    }

    private static String convertSingleQuote(String text) {
        int stop = text.length();
        return text.substring(1, stop - 1);
    }

    private static String covertDoubleQuote(String text) {
        int stop = text.length();
        return text.substring(1, stop - 1);
    }

    private static String convertMultiline(String text) {
        return text.replace("\"\"\"", "");
    }

}
