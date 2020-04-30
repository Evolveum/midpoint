package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.antlr.AxiomBaseListener;
import com.evolveum.axiom.lang.antlr.AxiomParser.ArgumentContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.DoubleQuoteStringContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.IdentifierContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.StatementContext;
import com.evolveum.axiom.lang.api.AxiomStatementStreamListener;

public class AxiomAntlrAdapter extends AxiomBaseListener {

    private final AxiomIdentifierResolver statements;
    private final AxiomStatementStreamListener delegate;



    public AxiomAntlrAdapter(AxiomIdentifierResolver statements, AxiomStatementStreamListener delegate) {
        this.statements = statements;
        this.delegate = delegate;
    }



    @Override
    public void enterStatement(StatementContext ctx) {
        AxiomIdentifier identifier = statementIdentifier(ctx.identifier());
        delegate.startStatement(identifier);
        super.enterStatement(ctx);
    }

    @Override
    public void enterArgument(ArgumentContext ctx) {
        if (ctx.identifier() != null) {
            enterArgument(ctx.identifier());
        } else {
            enterStringArgument(ctx.identifier());
        }



        super.enterArgument(ctx);
    }

    private void enterStringArgument(IdentifierContext identifier) {
        // TODO Auto-generated method stub

    }

    private void enterArgument(IdentifierContext identifier) {
        delegate.argument(statementIdentifier(identifier));
    }

    private AxiomIdentifier statementIdentifier(IdentifierContext identifier) {
        String prefix = identifier.prefix().getText();
        String localName = identifier.localIdentifier().getText();
        return statements.resolveStatementIdentifier(prefix,localName);
    }

}
