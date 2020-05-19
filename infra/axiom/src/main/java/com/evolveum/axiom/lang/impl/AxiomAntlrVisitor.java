/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.Optional;
import java.util.Set;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.antlr.AxiomBaseVisitor;
import com.evolveum.axiom.lang.antlr.AxiomParser.ArgumentContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.IdentifierContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.PrefixContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.StatementContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.StringContext;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.spi.AxiomStatementStreamListener;
import com.evolveum.axiom.lang.spi.SourceLocation;
import com.google.common.base.Strings;

public class AxiomAntlrVisitor<T> extends AxiomBaseVisitor<T> {

    private final AxiomIdentifierResolver statements;
    private final AxiomIdentifierResolver arguments;
    private final AxiomStatementStreamListener delegate;
    private final Optional<Set<AxiomIdentifier>> limit;
    private final String sourceName;

    public AxiomAntlrVisitor(String name, AxiomIdentifierResolver statements, AxiomIdentifierResolver arguments, AxiomStatementStreamListener delegate,
            Set<AxiomIdentifier> limit) {
        this.sourceName = name;
        this.statements = statements;
        this.arguments = arguments;
        this.delegate = delegate;
        this.limit = Optional.ofNullable(limit);
    }

    private AxiomIdentifier statementIdentifier(IdentifierContext identifier) {
        String prefix = nullableText(identifier.prefix());
        String localName = identifier.localIdentifier().getText();
        return statements.resolveIdentifier(prefix, localName);
    }

    private String nullableText(ParserRuleContext prefix) {
        return prefix != null ? prefix.getText() : null;
    }

    @Override
    public T visitStatement(StatementContext ctx) {
        AxiomIdentifier identifier = statementIdentifier(ctx.identifier());
        if(canEmit(identifier)) {
            delegate.startStatement(identifier, sourceLocation(ctx.identifier().start));
            T ret = super.visitStatement(ctx);
            delegate.endStatement(sourceLocation(ctx.stop));
            return ret;
        }
        return defaultResult();
    }

    private boolean canEmit(AxiomIdentifier identifier) {
        if (limit.isPresent()) {
            return limit.get().contains(identifier);
        }
        return true;
    }

    @Override
    public T visitArgument(ArgumentContext ctx) {
        if (ctx.identifier() != null) {
            delegate.argument(convert(ctx.identifier()), sourceLocation(ctx.start));
        } else {
            delegate.argument(convert(ctx.string()), sourceLocation(ctx.start));
        }
        return defaultResult();
    }

    private AxiomIdentifier convert(IdentifierContext argument) {
        return argumentIdentifier(argument);
    }

    private AxiomIdentifier argumentIdentifier(IdentifierContext identifier) {
        String prefix = nullableText(identifier.prefix());
        String localName = identifier.localIdentifier().getText();
        return arguments.resolveIdentifier(prefix, localName);
    }



    private int sourceLine(ParserRuleContext node) {
        return node.start.getLine();
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
