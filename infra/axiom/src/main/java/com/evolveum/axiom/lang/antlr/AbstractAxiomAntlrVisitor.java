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
import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.stream.AxiomItemStream;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.antlr.AxiomParser.ArgumentContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.IdentifierContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.ItemBodyContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.ItemContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.MetadataContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.StringContext;

public abstract class AbstractAxiomAntlrVisitor<T> extends AxiomBaseVisitor<T> {

    private final Optional<Set<AxiomName>> limit;
    private final String sourceName;

    private interface StartDelegate {
        void start(AxiomName identifier, SourceLocation location);
    }
    private interface EndDelegate {
        void end(SourceLocation location);
    }


    public AbstractAxiomAntlrVisitor(String name, Set<AxiomName> limit) {
        this.sourceName = name;
        this.limit = Optional.ofNullable(limit);
    }

    private AxiomName statementIdentifier(IdentifierContext identifier) {
        String prefix = nullableText(identifier.prefix());
        String localName = identifier.localIdentifier().getText();
        return resolveItemName(prefix, localName);
    }


    protected abstract AxiomItemStream.Target delegate();
    protected abstract AxiomName resolveItemName(String prefix, String localName);
    protected abstract AxiomName resolveArgument(String prefix, String localName);

    private String nullableText(ParserRuleContext prefix) {
        return prefix != null ? prefix.getText() : "";
    }


    @Override
    public T visitItem(ItemContext ctx) {
        AxiomName identifier = statementIdentifier(ctx.itemBody().identifier());
        return processItemBody(identifier, ctx.itemBody(), delegate()::startItem, delegate()::endItem);
    }

    public T processItemBody(AxiomName identifier, ItemBodyContext ctx, StartDelegate start, EndDelegate end) {
        if(canEmit(identifier)) {

            SourceLocation startLoc = sourceLocation(ctx.identifier().start);
            start.start(identifier, startLoc);

            ArgumentContext argument = ctx.value().argument();
            final Object value;
            final SourceLocation valueStart;

            if(argument != null) {
                value = convert(argument);
                valueStart = sourceLocation(argument.start);
            } else {
                value = null;
                valueStart = startLoc;
            }

            delegate().startValue(value, valueStart);
            T ret = visitItemBody(ctx);
            delegate().endValue(sourceLocation(ctx.stop));
            end.end(sourceLocation(ctx.stop));
            return ret;
        }
        return defaultResult();
    }


    @Override
    public T visitMetadata(MetadataContext ctx) {
        AxiomName identifier = statementIdentifier(ctx.itemBody().identifier());
        return processItemBody(identifier, ctx.itemBody(), delegate()::startInfra, delegate()::endInfra);
    }

    private Object convert(ArgumentContext ctx) {
        if (ctx.identifier() != null) {
            return (convert(ctx.identifier()));
        } else {
            return (convert(ctx.string()));
        }
    }

    private boolean canEmit(AxiomName identifier) {
        if (limit.isPresent()) {
            return limit.get().contains(identifier);
        }
        return true;
    }

    @Override
    public final T visitArgument(ArgumentContext ctx) {

        return defaultResult();
    }

    private AxiomName convert(IdentifierContext argument) {
        return argumentIdentifier(argument);
    }

    private AxiomName argumentIdentifier(IdentifierContext identifier) {
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
