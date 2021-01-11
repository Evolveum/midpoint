package com.evolveum.axiom.lang.antlr;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import com.evolveum.axiom.lang.antlr.query.AxiomQueryLexer;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser;
import com.evolveum.axiom.lang.antlr.query.AxiomQueryParser.FilterContext;

public class AxiomQuerySource {

    private final FilterContext root;

    public AxiomQuerySource(FilterContext root) {
        this.root = root;
    }

    public static final AxiomQuerySource from(String query) {
        CodePointCharStream stream = CharStreams.fromString(query);
        AxiomQueryLexer lexer = new AxiomQueryLexer(stream);
        AxiomQueryParser parser = new AxiomQueryParser(new CommonTokenStream(lexer));
        FilterContext root = parser.filter();
        return new AxiomQuerySource(root);
    }

    public FilterContext root() {
        return root;
    }
}
