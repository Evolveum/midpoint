/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.antlr;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import com.evolveum.axiom.api.AxiomPrefixedName;

import com.evolveum.axiom.api.stream.AxiomStreamTarget;
import com.evolveum.axiom.lang.antlr.AxiomParser.ArgumentContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.InfraNameContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.ItemContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.ItemNameContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.ItemValueContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.PrefixedNameContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.StringContext;
import com.evolveum.concepts.SourceLocation;


public abstract class AbstractAxiomAntlrVisitor<T> extends AxiomBaseVisitor<T> {

    private final String sourceName;

    private interface Itemable {

        void start(AxiomParser.PrefixedNameContext name, SourceLocation location);

        void end(SourceLocation location);
    }

    private final Itemable item = new Itemable() {

        @Override
        public void start(AxiomParser.PrefixedNameContext name, SourceLocation location) {
            delegate().startItem(name, location);
        }

        @Override
        public void end(SourceLocation location) {
            delegate().endItem(location);
        }
    };

    private final Itemable infra = new Itemable() {

        @Override
        public void start(AxiomParser.PrefixedNameContext name, SourceLocation location) {
            delegate().startInfra(name, location);
        }

        @Override
        public void end(SourceLocation location) {
            delegate().endInfra(location);
        }
    };


    public AbstractAxiomAntlrVisitor(String name) {
        this.sourceName = name;
    }

    protected abstract AxiomStreamTarget<AxiomParser.PrefixedNameContext, AxiomParser.ArgumentContext> delegate();

    private String nullableText(ParserRuleContext prefix) {
        return prefix != null ? prefix.getText() : "";
    }

    private Itemable startItem(ItemNameContext itemName, SourceLocation loc) {
        InfraNameContext infraName = itemName.infraName();
        PrefixedNameContext name;
        Itemable processor;
        if(infraName != null) {
            name = infraName.prefixedName();
            processor = infra;
        } else {
            name = (itemName.dataName().prefixedName());
            processor = item;
        }
        processor.start(name, loc);
        return processor;
    }



    @Override
    public T visitItem(ItemContext ctx) {
        SourceLocation startLoc = sourceLocation(ctx.itemName().start);
        Itemable processor = startItem(ctx.itemName(), startLoc);
        T ret = super.visitItem(ctx);
        processor.end(sourceLocation(ctx.stop));
        return ret;
    }

    @Override
    public T visitItemValue(ItemValueContext ctx) {
        ArgumentContext argument = ctx.argument();
        final SourceLocation valueStart;

        if(argument != null) {
            valueStart = sourceLocation(argument.start);
        } else {
            valueStart = sourceLocation(ctx.start);
        }
        delegate().startValue(argument, valueStart);
        T ret = super.visitItemValue(ctx);
        delegate().endValue(sourceLocation(ctx.stop));
        return ret;
    }

    private Object convert(ArgumentContext ctx) {
        if (ctx.prefixedName() != null) {
            return (convert(ctx.prefixedName()));
        } else {
            return (convert(ctx.string()));
        }
    }

    @Override
    public final T visitArgument(ArgumentContext ctx) {
        return defaultResult();
    }

    private AxiomPrefixedName convert(PrefixedNameContext value) {
        String prefix = nullableText(value.prefix());
        String localName = value.localName().getText();
        return AxiomPrefixedName.from(prefix, localName);
    }


    private SourceLocation sourceLocation(Token start) {
        return SourceLocation.from(sourceName, start.getLine(), start.getCharPositionInLine());
    }

    static String convertToString(ArgumentContext context) {
       if(context.prefixedName() != null) {
           return context.prefixedName().getText();
       }
       return convert(context.string());
    }

    static String convert(StringContext string) {
        if(string.singleQuoteString() != null) {
            return AxiomAntlrLiterals.convertSingleQuote(string.singleQuoteString().getText());
        }
        if(string.doubleQuoteString() != null) {
            return AxiomAntlrLiterals.convertDoubleQuote(string.doubleQuoteString().getText());
        }
        return AxiomAntlrLiterals.convertMultiline(string.multilineString().getText());
    }



}
